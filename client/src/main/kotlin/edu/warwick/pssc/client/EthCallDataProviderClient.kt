package edu.warwick.pssc.client

import com.r3.conclave.client.EnclaveClient
import com.r3.conclave.client.web.WebEnclaveTransport
import com.r3.conclave.common.EnclaveConstraint
import edu.warwick.pssc.common.connection.sendAndWaitForMail
import edu.warwick.pssc.conclave.AddressPlaceholder
import edu.warwick.pssc.conclave.Comparator
import edu.warwick.pssc.conclave.DataDiscloseCondition
import edu.warwick.pssc.conclave.EthContractDiscloseCondition
import edu.warwick.pssc.conclave.common.ErrorMessage
import edu.warwick.pssc.conclave.common.MessageSerializer.decodeMessage
import edu.warwick.pssc.conclave.common.MessageSerializer.encodeMessage
import edu.warwick.pssc.conclave.common.SecretDataSubmission
import edu.warwick.pssc.conclave.common.web3jSerializationModule
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint256
import picocli.CommandLine
import java.util.concurrent.Callable
import kotlin.system.exitProcess


/**
 * Submit Data Client
 * Specify the ERC20 Token Address to check if requester has tokens of that address
 */
@CommandLine.Command(
    name = "contract-submission",
    mixinStandardHelpOptions = true,
    description = ["Client that can add secret data to the SecretDataEnclave based on ERC20 tokens."]
)
class EthCallDataProviderClient : Callable<Void?> {

    private val logger = LogManager.getLogger()
    @OptIn(ExperimentalSerializationApi::class)
    private val serializer = ProtoBuf { serializersModule = web3jSerializationModule }

    @CommandLine.Option(
        names = ["-s", "--secret"],
        required = true,
        interactive = true,
        description = ["The secret to submit to the enclave."])
    private var secretData: String? = null

    @CommandLine.Option(
        names = ["-t", "--token"],
        required = true,
        interactive = false, // TODO: Make interactive
        description = ["ERC20 Contract Address to check tokens of."],
        converter = [AddressConverter::class]
    )
    private var address: Address? = null


    @CommandLine.Option(
        names = ["-u", "--url"],
        required = false,
        description = ["URL of the web host running the enclave."]
    )
    private var url: String = "localhost:8080"

    @CommandLine.Option(
        names = ["-c", "--constraint"],
        required = true,
        description = ["Enclave constraint which determines the enclave's identity and whether it's acceptable to use."],
        converter = [EnclaveConstraintConverter::class]
    )
    private var constraint: EnclaveConstraint? = null

    @OptIn(ExperimentalSerializationApi::class)
    @Throws(Exception::class)
    override fun call(): Void? {
        /*A new private key is generated. Enclave Client is created using this private key and constraint.
        A corresponding public key will be used by the enclave to encrypt data to be sent to this client*/
        val enclaveClient = EnclaveClient(constraint!!)

        val webEnclaveTransport = WebEnclaveTransport(url)

        try {

            enclaveClient.start(webEnclaveTransport)
        } catch (e: Exception) {
            logger.error("Could not connect to enclave.", e.message)
            exitProcess(1)
        }


        val encodedSecretData = serializer.encodeToByteArray(secretData)
        val submission = SecretDataSubmission.Submission(
            encodedSecretData,
            EthContractDiscloseCondition(
                address!!,
                "balanceOf",
                listOf(AddressPlaceholder) as List<Type<Any>>,
                listOf(TypeReference.create(Uint256::class.java)) as List<TypeReference<Type<Any>>>,
                listOf(listOf(Triple(Uint256(0), Comparator.GREATER_THAN, 0))) as List<List<Triple<Type<Any>, Comparator, Int>>>
            ) as DataDiscloseCondition<Any>
        )

        val mailBytes = submission.encodeMessage()

        val responseMail = enclaveClient.sendAndWaitForMail(mailBytes)

        try  {
            when (val reply = responseMail.bodyAsBytes.decodeMessage()) {
                is SecretDataSubmission.Response -> {
                    logger.log(Level.INFO, "Successfully submitted secret data to enclave. Data Reference ID is ${reply.dataReferenceId}")
                }
                is ErrorMessage -> {
                    throw Exception(reply.message)
                }
                else -> {
                    throw Exception("Unknown response from enclave.")
                }
            }
        } catch (e: Exception) {
            logger.log(Level.WARN, "Error submitting secret data to enclave.")
        }

        webEnclaveTransport.close()
        logger.log(Level.INFO, "Enclave client exiting.")
        return null
    }



    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val exitCode = CommandLine(EthCallDataProviderClient()).execute(*args)
            exitProcess(exitCode)
        }
    }

}