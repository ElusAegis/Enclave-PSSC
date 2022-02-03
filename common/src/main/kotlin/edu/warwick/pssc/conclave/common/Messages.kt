@file:Suppress("EXPERIMENTAL_API_USAGE")

package edu.warwick.pssc.conclave.common

import edu.warwick.pssc.conclave.DataDiscloseCondition
import edu.warwick.pssc.conclave.SecretData
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import org.web3j.crypto.Sign
import java.util.*

@Serializable
sealed class Message {
    /**
     * Serialize message to a byte array
     */
    fun encodeToByteArray() = ProtoBuf.encodeToByteArray(serializer(), this)

    /**
     * Deserialize message from a byte array
     */
    companion object { fun ByteArray.deserializeMail() = ProtoBuf.decodeFromByteArray(serializer(), this) }
}

object SecretDataSubmission {
    /**
     * Message sent from the Client to the Host with Secret Data.
     */
    @Serializable
    data class Submission(
        val data: SecretData,
        val condition: DataDiscloseCondition,
    ) : Message()

    /**
     * Response after a successful Secret Data submission.
     * @param dataReferenceId of the submitted data for future reference.
     */
    @Serializable
    data class Response(
        @Serializable(with = UUIDSerializer::class)
        val dataReferenceId: UUID
    ) : Message()
}


/**
 * Enclave response status for SignIn request.
 * @param message is an optional message, describing what has happened.
 */
@Serializable
data class ErrorMessage (
    val message : String? = null
) : Message()


object SecretDataRequest {

    /**
     * Message sent from the Client to the Host to request the Host to disclose the Secret Data.
     */
    @Serializable
    data class Request(
        @Serializable(with = UUIDSerializer::class)
        val dataReferenceId: UUID,
        @Serializable(with = SignatureSerializer::class)
        val dataReferenceIdSignature: Sign.SignatureData
    ) : Message()


    @Serializable
    data class Response(
        val data: SecretData
    ) : Message()
}



