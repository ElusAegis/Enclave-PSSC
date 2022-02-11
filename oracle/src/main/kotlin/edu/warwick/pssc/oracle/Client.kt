package edu.warwick.pssc.oracle

import com.r3.conclave.client.EnclaveClient
import com.r3.conclave.client.web.WebEnclaveTransport
import com.r3.conclave.common.EnclaveConstraint
import edu.warwick.pssc.common.connection.sendAndWaitForMail
import edu.warwick.pssc.conclave.common.ErrorMessage
import edu.warwick.pssc.conclave.common.Message.Companion.deserializeMessage
import edu.warwick.pssc.conclave.common.OracleEthDataCall
import edu.warwick.pssc.conclave.common.OracleRegistration
import org.apache.logging.log4j.LogManager
import kotlin.system.exitProcess

class Client(apiUrl: String, enclaveUrl: String, enclaveConstraint: String) {

    private val logger = LogManager.getLogger()

    private val oracle = ApiOracleService(apiUrl)

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

        registerWithEnclave()
        logger.info("Successfully registered with enclave.")
    }

    fun registerWithEnclave() {
        val registrationByteMessage = OracleRegistration.Request.encodeToByteArray()
        val responseMail = enclave.sendAndWaitForMail(registrationByteMessage)

        try {
            val response = responseMail.bodyAsBytes.deserializeMessage()

            when (response) {
                is OracleRegistration.SuccessResponse -> {} // Registered successfully
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
                val response = mail.bodyAsBytes.deserializeMessage()

                when (response) {
                    is OracleEthDataCall.Request -> {
                        logger.info("Received OracleEthDataCall.Request from enclave.")
                        logger.info("Do not process it for now ")   // TODO: Process it
                    }
                    else -> {
                        throw RuntimeException("Incorrect response message from the Enclave, got ${response::class.simpleName}")
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
    fun shutdown() {
        (enclave.transport as WebEnclaveTransport).close()
    }


    companion object fun main(args: Array<String>) {


        val client = Client(apiUrl = args[0], enclaveUrl = args[1], enclaveConstraint = args[2])

        try {
            client.run()
        } catch (e: Exception) {
            // Do a safe shutdown, as we have already established connection to the enclave
            client.shutdown()
        }
    }

}