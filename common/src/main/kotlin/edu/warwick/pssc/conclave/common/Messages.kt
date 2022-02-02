@file:Suppress("EXPERIMENTAL_API_USAGE")

package edu.warwick.pssc.conclave.common

import edu.warwick.pssc.conclave.DataDiscloseCondition
import edu.warwick.pssc.conclave.SecretData
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
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



/**
 * Message sent from the Client to the Host with Secret Data.
 */
@Serializable
data class SecretDataSubmission(
    val data: SecretData,
    val condition: DataDiscloseCondition,
) : Message()

/**
 * Response after a successful Secret Data submission.
 * @param referenceId of the submitted data for future reference.
 */
@Serializable
data class SecretDataSubmissionResponse(
    @Serializable(with = UUIDSerializer::class)
    val referenceId: UUID
) : Message()


/**
 * Enclave response status for SignIn request.
 * @param message is an optional message, describing what has happened.
 */
@Serializable
data class ErrorResponse (
    val message : String? = null
) : Message()


/**
 * Message sent from the Client to the Host to request the Host to disclose the Secret Data.
 */
@Serializable
data class SecretDataRequest(
    @Serializable(with = UUIDSerializer::class)
    val referenceId: UUID
) : Message()

@Serializable
data class SecretDataDisclosure(
    val data: SecretData
) : Message()


