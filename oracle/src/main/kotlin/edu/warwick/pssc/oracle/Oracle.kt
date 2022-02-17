package edu.warwick.pssc.oracle

import com.r3.conclave.client.EnclaveClient
import com.r3.conclave.client.web.WebEnclaveTransport
import com.r3.conclave.common.EnclaveConstraint
import edu.warwick.pssc.common.connection.sendAndWaitForMail
import edu.warwick.pssc.conclave.common.ErrorMessage
import edu.warwick.pssc.conclave.common.MessageSerializer.decodeMessage
import edu.warwick.pssc.conclave.common.MessageSerializer.encodeMessage
import edu.warwick.pssc.conclave.common.OracleEthDataCall
import edu.warwick.pssc.conclave.common.OracleRegistration
import org.apache.logging.log4j.LogManager
import org.web3j.abi.datatypes.Address
import kotlin.system.exitProcess

class Oracle(apiUrl: String, enclaveUrl: String, enclaveConstraint: String, oracleKey: String) {

    private val logger = LogManager.getLogger()

    private val oracle = EthOracleService(apiUrl, Address("0"),  logger)

    private val enclave: EnclaveClient = EnclaveClient(EnclaveConstraint.parse(enclaveConstraint))

    init {

        val enclaveTransport = WebEnclaveTransport(enclaveUrl)

        try {
            enclave.start(enclaveTransport)
        } catch (e: Exception) {
            logger.error("Could not connect to enclave.", e.message)
            exitProcess(1)
        }

        logger.info("Connected to enclave.")

        registerWithEnclave(oracleKey)
        logger.info("Successfully signed into the enclave.")
    }

    private fun registerWithEnclave(key: String) {
        val registrationByteMessage = OracleRegistration.Request(key).encodeMessage()
        val responseMail = enclave.sendAndWaitForMail(registrationByteMessage, "oracle-registration")

        try {
            val response = responseMail.bodyAsBytes.decodeMessage()

            when (response) {
                is OracleRegistration.SuccessResponse -> {} // Registered successfully
                is OracleRegistration.AlreadyRegisteredResponse -> {
                    logger.warn("Oracle with such key already registered with enclave. We have updated the routing hint.")
                }
                is ErrorMessage -> {
                    throw RuntimeException("Error registering with enclave: ${response.message}")

                }
                else -> {
                    throw RuntimeException("Incorrect response message from the Enclave, got ${response::class.simpleName}")
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to register with the Enclave.", e.message)
            exitProcess(1)
        }
    }

    fun run() {
        // Continuously poll the Enclave API for new messages
        while (true) {
            val mail = enclave.pollMail()
            // If message is null, it means there are no messages to process
            // Sleep for 2 seconds and try again
            if (mail == null) {
                Thread.sleep(2000)
                continue
            }

            logger.debug("Received message from enclave.")

            try {
                val message = mail.bodyAsBytes.decodeMessage()

                when (message) {
                    is OracleEthDataCall.Request -> {
                        logger.info("Received OracleEthDataCall Request from enclave.")
                        val replyMessage = try {
                            val returnValue = oracle.doEthDataCall(message.contractAddress, message.functionName, message.inputData, message.outputDataType)
                             OracleEthDataCall.Response(returnValue)
                        } catch (e: Exception) {
                            logger.error("Failed to process OracleEthDataCall request. ${e.message}")
                            ErrorMessage("Failed to process OracleEthDataCall request: ${e.message}")
                        }

                        enclave.sendMail(mail.topic, replyMessage.encodeMessage(), null)
                        logger.info("Sent OracleEthDataCall Response to enclave.")

                    }
                    else -> {
                        throw RuntimeException("Incorrect response message from the Enclave, got ${message::class.simpleName}")
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to process message from the Enclave.", e.message)
            }
        }
    }

    /**
     * The connection is established during initialization.
     * Thus, we can safely close it here
     */
    private fun shutdown() {
        (enclave.transport as WebEnclaveTransport).close()
    }


    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            val client = Oracle(apiUrl = args[0], enclaveUrl = args[1], enclaveConstraint = args[2], oracleKey = args[3])

            try {
                client.run()
            } catch (e: Exception) {
                // Do a safe shutdown, as we have already established connection to the enclave
                client.shutdown()
            }
        }
    }
}