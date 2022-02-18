package edu.warwick.pssc.enclave

import com.r3.conclave.enclave.Enclave
import com.r3.conclave.enclave.EnclavePostOffice
import com.r3.conclave.mail.EnclaveMail
import edu.warwick.pssc.conclave.AddressPlaceholder
import edu.warwick.pssc.conclave.EthContractDiscloseCondition
import edu.warwick.pssc.conclave.PublicKeyDiscloseCondition
import edu.warwick.pssc.conclave.common.*
import edu.warwick.pssc.conclave.common.MessageSerializer.decodeMessage
import edu.warwick.pssc.conclave.common.MessageSerializer.encodeMessage
import edu.warwick.pssc.enclave.modules.DataRequestValidator
import edu.warwick.pssc.enclave.modules.OracleManager
import edu.warwick.pssc.enclave.modules.RequestManager
import edu.warwick.pssc.enclave.modules.SecretDatabase
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Type
import org.web3j.crypto.Keys.getAddress
import java.security.PublicKey
import java.security.SignatureException
import java.util.*

/**
 * Enclave that accepts data submissions from the Clients,
 * Stores data with a rule on who can access it.
 * Whenever any client would like to access data,
 * they provide proof that he can access it and the Enclave returns it
 */
@Suppress("unused")
class PSSCEnclave : Enclave() {

    private val requestManager = RequestManager(this.MessageBroker())
    private val oracleManager = OracleManager(this.MessageBroker())

    private val database : SecretDatabase = SecretDatabase()

    override fun onStartup() {
        MessageBroker().logInfo("Waiting for Data Submissions and Requests!")
    }

    override fun receiveFromUntrustedHost(bytes: ByteArray): ByteArray? {
        throw UnsupportedOperationException("Receiving from untrusted host is not supported")
    }

    override fun receiveMail(mail: EnclaveMail, routingHint: String?) {

        val message = mail.bodyAsBytes.decodeMessage()

        MessageBroker().logInfo("Received message ${message::class.simpleName}! Mail Topic: ${mail.topic} RoutingHint: $routingHint")

        val response: Message? = try {
            when (message) {
                is SecretDataSubmission.Submission -> processDataSubmission(message)
                is SecretDataRequest.Request -> processDataRequestRequest(message, mail, routingHint)
                is OracleRegistration.Request -> processOracleRegistrationRequest(message, mail, routingHint)
                is OracleEthDataCall.Response -> processOracleDataCallResponse(message, mail)
                is OracleBlock.CurrentRequest -> processOracleCurrentBlockRequest()
                is OracleBlock.NextSubmission -> processOracleNextBlockSubmission(message, mail)
                else -> {
                    MessageBroker().logInfo("Received an unknown message!")
                    throw EnclaveStateException("Received an unknown message!")
                } // Add processing for error messages from oracles
            }
        } catch (e: EnclaveStateException) {
            ErrorMessage(e.message)
        }

        if (response != null) {
            MessageBroker().logInfo("Sending response of type ${response::class.simpleName}!")
            val responseBytes = response.encodeMessage()
            val encryptedMail = postOffice(mail).encryptMail(responseBytes)
            postMail(encryptedMail, routingHint)
        }
    }

    private fun processOracleNextBlockSubmission(
        message: OracleBlock.NextSubmission,
        mail: EnclaveMail
    ): Message {
        val oracleId = oracleManager.getOracleId(mail.authenticatedSender)
            ?: throw EnclaveStateException("The message sender is not an authenticated Oracle!")

        try {
            oracleManager.receiveBlockVote(oracleId, message.block)
        } catch (e: java.lang.IllegalArgumentException) {
            throw EnclaveStateException("The message sender is not an authenticated Oracle!")
        }

        return SuccessMessage
    }

    private fun processOracleCurrentBlockRequest(
    ): Message {
        return OracleBlock.CurrentResponse(oracleManager.currentBlock)
    }

    private fun processOracleDataCallResponse(
        message: OracleEthDataCall.Response,
        mail: EnclaveMail
    ): Message {
        MessageBroker().logInfo("Processing Oracle Data Call Response!")
        val requestId = UUID.fromString(mail.topic) // We maintain the ID of the request in the topic of the mail

        val oracleId = oracleManager.getOracleId(mail.authenticatedSender)
            ?: throw EnclaveStateException("The message sender is not an authenticated Oracle!")

        // Because of serialisation limitations, we do the following to conform to a rule that if failed, oracleResponseData is null
        val oracleResponseData = if (message.success) message.outputData else null

        // Register the response and if enough Oracles have responded for consensus, get the value that was requested
        val (trustedOutput, requestedDataId, requester) = try {
            requestManager.registerResponse(requestId, oracleId, oracleResponseData)
        } catch (e: java.lang.IllegalArgumentException) {
            throw EnclaveStateException("There is no pending request with the $requestId id!")
        } catch (e: java.lang.IllegalStateException) {
            // Not enough Oracles have responded, yet
            // We should wait for more outputs from Oracles
            // Tell the oracle that we have successfully received the data
            return SuccessMessage
        }

        if (trustedOutput == null) {
            // This means most Oracles could not get requested data
            // Notify requester that the request has failed
            MessageBroker().sendMessage(ErrorMessage("Oracles could not perform the request successfully"), requester.requestPostOffice, requester.routingHint)
            return SuccessMessage
        }


        val condition = try {
            database.getCondition(requestedDataId) as EthContractDiscloseCondition
        } catch (e: Exception) {
            // Notify the requester that the request has failed, even though data has been obtained
            MessageBroker().sendMessage(ErrorMessage("Could not get condition for data id $requestedDataId"), requester.requestPostOffice, requester.routingHint)
            return SuccessMessage // Oracle has successfully delivered the data requested
        }

        val authorised = condition.check(trustedOutput)

        val messageToRequester = try {
            getData(requestedDataId, authorised)
        } catch (e: EnclaveStateException) {
            ErrorMessage(e.message)
        }

        MessageBroker().sendMessage(messageToRequester, requester.requestPostOffice, requester.routingHint)
        return SuccessMessage
    }

    private fun processOracleRegistrationRequest(
        message: OracleRegistration.Request,
        mail: EnclaveMail,
        routingHint: String?
    ): Message? {
        MessageBroker().logInfo("Processing Oracle Registration Request!")

        val oracleKey = message.key

        val oracleRegistrationAccepted = oracleManager.registerOracle(oracleKey, ConnectionData(mail.authenticatedSender, routingHint), postOffice(mail))

        return if (!oracleRegistrationAccepted) {
            OracleRegistration.AlreadyRegisteredResponse
        } else {
            return null // We have added the waiting list. Once the new epoch is started, the waiting list will be processed
        }
    }

    private fun processDataRequestRequest(
        message: SecretDataRequest.Request,
        mail: EnclaveMail,
        routingHint: String?
    ): Message? {
        MessageBroker().logInfo("Processing Data Disclosure Request!")
        MessageBroker().logInfo("Requested Data Id: ${message.dataReferenceId}")

        val condition = try { database.getCondition(message.dataReferenceId) }
        catch (e: IllegalArgumentException) {
            throw EnclaveStateException("Provided data ID does not exist!")
        }

        val validatedRequesterKey = try {
            DataRequestValidator.getSignedPublicKey(
                message.dataReferenceId.toByteArray(),
                message.dataReferenceIdSignature
            )
        } catch (e: SignatureException) {
            throw EnclaveStateException("Could not verify signature provided!")
        }

        val authorised = when (condition) {
            is PublicKeyDiscloseCondition ->
                condition.check(validatedRequesterKey)
            is EthContractDiscloseCondition -> {

                @Suppress("UNCHECKED_CAST")
                val requesterAddress = Address(getAddress(validatedRequesterKey)) as Type<Any>
                val inputData = condition.inputData.map { item ->
                    // Replace all empty positions in the input data with the Address of the requester
                    if (item.javaClass == AddressPlaceholder::class.java) {
                        requesterAddress
                    } else {
                        item
                    }
                }

                val requestToOracles = OracleEthDataCall.Request(
                    condition.contractAddress,
                    condition.functionName,
                    inputData,
                    condition.outputDataType
                )

                val oracles = oracleManager.getOracleWithConnections()
                val requester = RequestManager.RequesterDetails(routingHint, postOffice(mail), validatedRequesterKey)

                val ethRequestId = requestManager.addRequest(message.dataReferenceId, requester, oracles.size)

                if (oracles.isEmpty())
                    throw EnclaveStateException("Could not check the data request as no oracles are online!")

                oracles.forEach { (oracleKey, oracleConnection) ->
                    MessageBroker().logInfo("Calling Oracle: $oracleKey...")
                    val encryptedMail = postOffice(oracleConnection.publicKey, ethRequestId.toString()).encryptMail(requestToOracles.encodeMessage())
                    postMail(encryptedMail, oracleConnection.routingHint)
                }





                // By this point we have sent out all requests to the oracles, so we can wait for the responses
                // will trigger a new execution of the "receiveMail" method, which will process the response
                // That is why we finish the execution here and make the requester wait until we obtain the information
                return null
            }
            else -> throw IllegalArgumentException("Unknown condition type!")
        }

        return getData(message.dataReferenceId, authorised)
    }

    private fun processDataSubmission(
        message: SecretDataSubmission.Submission
    ): Message {
        MessageBroker().logInfo("Received a Secret Data Submission!")
        // Store secret data in database
        val storedDataId = database.storeData(message.data, message.condition)

        // Send back the stored data id
        return SecretDataSubmission.Response(storedDataId)
    }


    private fun getData(dataId: UUID, authorised: Boolean): Message {
        try {
            return SecretDataRequest.Response(database.getDataIfAuthorised(dataId, authorised))
        } catch (e: IllegalArgumentException) {
            throw EnclaveStateException("Could not get the requested data!")
        }
    }




    inner class MessageBroker {
        
        /**
         * Logging function for Enclave
         */
        fun logInfo(vararg messages: Any) {
            messages.forEach { println("[ENCLAVE] $it") }
        }

        fun executeRequestManagerEpoch() {
            requestManager.executeEpoch()
        }

        fun sendMessage(message: Message, pkey: PublicKey, routingHint: String? = null) = sendMessage(message, postOffice(pkey), routingHint)

        fun sendMessage(message: Message, postOffice: EnclavePostOffice, routingHint: String? = null) {
            val encryptedMail = postOffice.encryptMail(message.encodeMessage())
            postMail(encryptedMail, routingHint)
        }
    }

}