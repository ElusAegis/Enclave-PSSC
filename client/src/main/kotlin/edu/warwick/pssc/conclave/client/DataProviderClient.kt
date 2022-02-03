package edu.warwick.pssc.conclave.client

import com.r3.conclave.client.EnclaveClient
import com.r3.conclave.client.web.WebEnclaveTransport
import com.r3.conclave.common.EnclaveConstraint
import edu.warwick.pssc.conclave.EthPublicKey
import edu.warwick.pssc.conclave.PublicKeyDiscloseCondition
import edu.warwick.pssc.conclave.client.connection.sendAndWaitForMail
import edu.warwick.pssc.conclave.common.ErrorMessage
import edu.warwick.pssc.conclave.common.Message.Companion.deserializeMail
import edu.warwick.pssc.conclave.common.SecretDataSubmission
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import picocli.CommandLine
import java.util.concurrent.Callable
import kotlin.system.exitProcess


/**
 * Submit Data Client
 * Takes a string and a list of ethereum public keys and submits it to the enclave as a constraint.
 */
@CommandLine.Command(
    name = "data-submission-client",
    mixinStandardHelpOptions = true,
    description = ["Simple client that can add secret data to the SecretDataEnclave."]
)
class DataProviderClient : Callable<Void?> {

    private val logger = LogManager.getLogger()

    @CommandLine.Option(names = ["-s", "--secret"],
        required = true,
        interactive = true,
        description = ["The secret to submit to the enclave."])
    private var secretData: String? = null

    @CommandLine.Parameters(description = ["List of Public Keys to allow to access secret data. Use HEX encoding."],
        converter = [EthPublicKeyConverter::class])
    private var publicKeys: List<EthPublicKey> = ArrayList()

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


        val encodedSecretData = ProtoBuf.encodeToByteArray(secretData)
        val mail = SecretDataSubmission.Submission(
            encodedSecretData,
            PublicKeyDiscloseCondition(publicKeys)
        )
        val mailBytes = mail.encodeToByteArray()
        val responseMail = enclaveClient.sendAndWaitForMail(mailBytes)

        try  {
            when (val reply = responseMail.bodyAsBytes.deserializeMail()) {
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
            val exitCode = CommandLine(DataProviderClient()).execute(*args)
            exitProcess(exitCode)
        }
    }

}