@file:Suppress("EXPERIMENTAL_API_USAGE")

package edu.warwick.pssc.conclave.common

import edu.warwick.pssc.conclave.BlockInfo
import edu.warwick.pssc.conclave.DataDiscloseCondition
import edu.warwick.pssc.conclave.SecretData
import kotlinx.serialization.Contextual
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Type
import org.web3j.crypto.Sign
import java.util.*

@Serializable
sealed class Message

object SecretDataSubmission {
    /**
     * Message sent from the Client to the Host with Secret Data.
     */
    @Serializable
    data class Submission(
        val data: SecretData,
        val condition: DataDiscloseCondition<@Contextual Any>,
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

/**
 * Message confirming that the action was sussessful.
 */
@Serializable
object SuccessMessage : Message()


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
    data class Request(val key: String) : Message()

    /**
     * Response after a successful Oracle registration.
     */
    @Serializable
    object Success: Message()

    /**
     * Response if the Oracle is already registered.
     */
    @Serializable
    object AlreadyRegisteredResponse: Message()

    /**
     * DeRegistration response if Oracle has been inactive for a while
     */
    @Serializable
    object DeRegistrationMessage: Message()
}

object OracleEthDataCall {

    /**
     * Enclave Data Request sent to an Oracle to get the data.
     */
    @Serializable
    data class Request(
        @Serializable(with = AddressTypeSerializer::class)
        val contractAddress: Address,
        val functionName: String,
        val inputData: List<@Polymorphic Type<@Contextual Any>>,
        val outputDataType: List<@Serializable(with=TypeReferenceSerializer::class) TypeReference<Type<@Contextual Any>>>,
    ) : Message()

    /**
     * Oracle response with data.
     */
    @Serializable
    data class Response(
        val success: Boolean,
        val outputData: List<@Polymorphic Type<@Contextual Any>>
    ) : Message()

}

object OracleBlock {
    /**
     * Find what is the latest block known to the enclave.
     */
    @Serializable
    object CurrentRequest : Message()

    /**
     * Response with the latest block known to the enclave.
     */
    @Serializable
    data class CurrentResponse(
        val block: BlockInfo? // If the block is null, the enclave has not yet received any blocks. One is prompted to send the latest block known.
    ) : Message()

    /**
     * Submit next block to the enclave.
     */
    @Serializable
    data class NextSubmission(
        val block: BlockInfo
    ) : Message()
}


