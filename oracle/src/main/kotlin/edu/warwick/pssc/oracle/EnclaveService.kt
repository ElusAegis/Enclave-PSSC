package edu.warwick.pssc.oracle

import com.r3.conclave.client.EnclaveClient
import com.r3.conclave.client.web.WebEnclaveTransport
import com.r3.conclave.common.EnclaveConstraint
import edu.warwick.pssc.common.connection.sendAndWaitForMail
import edu.warwick.pssc.conclave.OracleKey
import edu.warwick.pssc.conclave.common.ErrorMessage
import edu.warwick.pssc.conclave.common.Message
import edu.warwick.pssc.conclave.common.MessageSerializer.decodeMessage
import edu.warwick.pssc.conclave.common.MessageSerializer.encodeMessage
import edu.warwick.pssc.conclave.common.OracleRegistration
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.logging.log4j.LogManager
import kotlin.system.exitProcess

class EnclaveService(enclaveUrl: String, rawEnclaveConstraint: String, oracleKey: OracleKey) {

    // Protect from concurrently sending mail to the Enclave
    val mutex = Mutex()

    private val logger = LogManager.getLogger()

    private val enclaveInstance : EnclaveClient

    init {
        val enclaveConstraint = EnclaveConstraint.parse(rawEnclaveConstraint)
        enclaveInstance = EnclaveClient(enclaveConstraint)
        val enclaveTransport = WebEnclaveTransport(enclaveUrl)

        try {
            enclaveInstance.start(enclaveTransport)
        } catch (e: Exception) {
            logger.error("Could not connect to enclave.", e.message)
            exitProcess(1)
        }

        logger.info("Connected to enclave.")

        registerWithEnclave(oracleKey)
        logger.info("Successfully signed into the enclave.")
    }

    private fun registerWithEnclave(key: String) {

        val response = runBlocking {
            sendAndWaitForMail(OracleRegistration.Request(key), "oracle-registration")
        }


        try {

            when (response) {
                is OracleRegistration.Success -> {} // Registered successfully
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

    suspend fun sendMail(message: Message, topic: String = "default", envelope : ByteArray? = null) {
        mutex.withLock {
            enclaveInstance.sendMail(topic, message.encodeMessage(), envelope)
        }
    }

    suspend fun sendAndWaitForMail(message: Message, topic: String = "default"): Message {
        mutex.withLock {
            val registrationByteMessage = message.encodeMessage()
            val responseMail = enclaveInstance.sendAndWaitForMail(registrationByteMessage, topic)
            return responseMail.bodyAsBytes.decodeMessage()
        }
    }

    suspend fun pollMessage(timeout: Long): Message = pollMessageWithTopic(timeout).first

    suspend fun pollMessageWithTopic(timeout: Long) : Pair<Message, String> {
        while (true) {
            val response = enclaveInstance.pollMail()
            if (response != null) {
                return Pair(response.bodyAsBytes.decodeMessage(), response.topic)
            }
            // If mail is null, it means there are no messages to process
            // Sleep for timeout seconds and try again
            delay(timeout)
        }
    }

    fun stop() {
        (enclaveInstance.transport as WebEnclaveTransport).close()
    }
}