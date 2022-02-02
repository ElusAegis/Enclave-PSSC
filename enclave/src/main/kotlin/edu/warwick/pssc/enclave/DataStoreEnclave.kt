package edu.warwick.pssc.enclave

import com.r3.conclave.enclave.Enclave
import com.r3.conclave.mail.EnclaveMail
import edu.warwick.pssc.conclave.common.SecretDataSubmissionResponse
import java.util.*

/**
 * Enclave that accepts data submissions from the Clients,
 * Stores data with a rule on who can access it.
 * Whenever any client would like to access data,
 * they provide proof that he can access it and the Enclave returns it
 */
class DataStoreEnclave : Enclave() {

    val dataDatabase : SecretDatabase = SecretDatabase()

    override fun onStartup() {
        logInfo("Waiting for Data Submissions and requests!")
    }

    override fun receiveFromUntrustedHost(bytes: ByteArray): ByteArray? {
        TODO("Not Supported right now")
    }

    override fun receiveMail(mail: EnclaveMail, routingHint: String?) {
        logInfo("Received mail!")
        val response = SecretDataSubmissionResponse(UUID.randomUUID())
        val encryptedResponse = postOffice(mail).encryptMail(response.encodeToByteArray())

        logInfo("Sending response to ${mail.authenticatedSender}")

        postMail(encryptedResponse, routingHint)
    }

    /**
     * Logging function for Enclave
     */
    private fun logInfo(vararg messages: Any) {
        messages.forEach { println("[ENCLAVE] $it") }
    }
}