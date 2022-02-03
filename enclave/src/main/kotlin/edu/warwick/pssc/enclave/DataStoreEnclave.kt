package edu.warwick.pssc.enclave

import com.r3.conclave.enclave.Enclave
import com.r3.conclave.mail.EnclaveMail
import edu.warwick.pssc.conclave.PublicKeyDiscloseCondition
import edu.warwick.pssc.conclave.common.*
import edu.warwick.pssc.conclave.common.Message.Companion.deserializeMail
import org.web3j.crypto.Sign
import java.security.SignatureException

/**
 * Enclave that accepts data submissions from the Clients,
 * Stores data with a rule on who can access it.
 * Whenever any client would like to access data,
 * they provide proof that he can access it and the Enclave returns it
 */
@Suppress("unused")
class DataStoreEnclave : Enclave() {

    private val dataDatabase : SecretDatabase = SecretDatabase()

    override fun onStartup() {
        logInfo("Waiting for Data Submissions and requests!")
    }

    override fun receiveFromUntrustedHost(bytes: ByteArray): ByteArray? {
        TODO("Not Supported right now")
    }

    override fun receiveMail(mail: EnclaveMail, routingHint: String?) {
        logInfo("Received mail!")
        logInfo("Mail Topic: ${mail.topic}")

        val message = mail.bodyAsBytes.deserializeMail()

        when (message) {
            is SecretDataSubmission.Submission -> processDataSubmission(message, mail, routingHint)
            is SecretDataRequest.Request -> processDataRequestRequest(message, mail, routingHint)
            else -> logInfo("Received an unknown message!")
        }

        logInfo("Finished processing mail!")
    }

    private fun processDataRequestRequest(
        message: SecretDataRequest.Request,
        mail: EnclaveMail,
        routingHint: String?
    ) {
        logInfo("Processing Data Disclosure Request!")
        logInfo("Requested Data Id: ${message.dataReferenceId}")

        var responseMessage: Message

        try {
            val (data, condition) = dataDatabase.getData(message.dataReferenceId)

            when (condition) {
                is PublicKeyDiscloseCondition -> {
                    val signedData = message.dataReferenceId.toByteArray()

                    val signedKey = Sign.signedMessageToKey(signedData, message.dataReferenceIdSignature)

                    require(condition.publicKeys.contains(signedKey)) {
                        "Not authorised to access this data"
                    }
                }
                else -> {
                    throw IllegalArgumentException("Unknown condition type")
                }
            }

            responseMessage = SecretDataRequest.Response(data)



        } catch (e: IllegalArgumentException) {
            responseMessage = ErrorMessage("Could not fetch data requested!")
        } catch (e: SignatureException) {
            responseMessage = ErrorMessage("Could not verify signature of data request!")
        }

        val responseMessageBytes = responseMessage.encodeToByteArray()

        val responseMail = postOffice(mail).encryptMail(responseMessageBytes)
        postMail(responseMail, routingHint)
    }

    private fun processDataSubmission(
        message: SecretDataSubmission.Submission,
        mail: EnclaveMail,
        routingHint: String?
    ) {
        logInfo("Received a Secret Data Submission!")
        // Store secret data in database
        val storedDataId = dataDatabase.storeData(message.data, message.condition)

        // Send back the stored data id
        val response = SecretDataSubmission.Response(storedDataId)
        val responseBytes = response.encodeToByteArray()
        val responseMail = postOffice(mail).encryptMail(responseBytes)
        postMail(responseMail, routingHint)
        logInfo("Sent back the stored data id!")
    }

    /**
     * Logging function for Enclave
     */
    private fun logInfo(vararg messages: Any) {
        messages.forEach { println("[ENCLAVE] $it") }
    }
}