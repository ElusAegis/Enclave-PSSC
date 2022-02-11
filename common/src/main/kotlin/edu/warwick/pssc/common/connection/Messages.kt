@file:Suppress("EXPERIMENTAL_API_USAGE")

package edu.warwick.pssc.conclave.common

import edu.warwick.pssc.conclave.DataDiscloseCondition
import edu.warwick.pssc.conclave.SecretData
import kotlinx.serialization.Contextual
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Type
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
    companion object { fun ByteArray.deserializeMessage() = ProtoBuf.decodeFromByteArray(serializer(), this) }
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

object OracleRegistration {
    /**
     * Message sent from the Oracle to the Host to register as an Oracle.
     */
    @Serializable
    object Request : Message()

    /**
     * Response after a successful Oracle registration.
     */
    @Serializable
    object SuccessResponse: Message()
}

object OracleEthDataCall {

    /**
     * Enclave Data Request sent to an Oracle to get the data.
     */
    @Serializable
    data class Request(
        @Serializable(with = UUIDSerializer::class)
        val dataCallId: UUID,
        @Serializable(with = AddressTypeSerializer::class)
        val contractAddress: Address,
        val inputData: List<@Polymorphic Type<@Contextual Any>>,
        val outputDataType: @Serializable(with=TypeReferenceSerializer::class) TypeReference<Type<@Contextual Any>>,
    ) : Message()

    /**
     * Oracle response with data.
     */
    @Serializable
    data class Response(
        @Serializable(with = UUIDSerializer::class)
        val dataCallId: UUID,
        val outputData: @Polymorphic Type<@Contextual Any>
    ) : Message()

}



