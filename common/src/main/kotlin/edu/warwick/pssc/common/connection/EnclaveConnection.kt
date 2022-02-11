package edu.warwick.pssc.common.connection

import com.r3.conclave.client.EnclaveClient
import com.r3.conclave.mail.EnclaveMail

/**
 * Waits for a mail to be received from the enclave after a mail has been sent.
 */
fun EnclaveClient.sendAndWaitForMail(
    mailBytes: ByteArray,
    topic: String = "default",
): EnclaveMail {
    var responseMail = sendMail(topic, mailBytes, null)
    if (responseMail == null) {
        do {
            Thread.sleep(2000)
            //Poll for reply to enclave
            responseMail = pollMail()
        } while (responseMail == null)
    }
    return responseMail
}