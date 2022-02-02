package edu.warwick.pssc.conclave.client

import com.r3.conclave.client.EnclaveClient
import com.r3.conclave.client.web.WebEnclaveTransport
import com.r3.conclave.common.EnclaveConstraint
import edu.warwick.pssc.conclave.EthPublicKey
import edu.warwick.pssc.conclave.PublicKeyDiscloseCondition
import edu.warwick.pssc.conclave.common.ErrorResponse
import edu.warwick.pssc.conclave.common.Message
import edu.warwick.pssc.conclave.common.SecretDataSubmission
import edu.warwick.pssc.conclave.common.SecretDataSubmissionResponse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import picocli.CommandLine
import java.math.BigInteger
import java.util.concurrent.Callable
import kotlin.system.exitProcess

const val INTERACTIVE = false

/**
 * Submit Data Client
 * Takes a string and a list of ethereum public keys and submits it to the enclave as a constraint.
 */
@CommandLine.Command(
    name = "data-submission-client",
    mixinStandardHelpOptions = true,
    description = ["Simple client that can add secret data to the SecretDataEnclave."]
)
class Client : Callable<Void?> {

    @CommandLine.Option(names = ["-s", "--secret"], 
        required = true,
        description = ["The secret to submit to the enclave."],
        interactive = INTERACTIVE)
    private var secretData: String? = null

    @CommandLine.Parameters(description = ["List of Public Keys to allow to access secret data. Use HEX encoding."],
        converter = [EthPublicKeyConverter::class])
    private var publicKeys: List<EthPublicKey> = ArrayList()

    @CommandLine.Option(
        names = ["-u", "--url"],
        required = false,
        interactive = INTERACTIVE,
        description = ["URL of the web host running the enclave."]
    )
    private var url: String = "localhost:8080"

    @CommandLine.Option(
        names = ["-c", "--constraint"],
        required = true,
        interactive = INTERACTIVE,
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

        enclaveClient.start(webEnclaveTransport)

        val encodedSecretData = ProtoBuf.encodeToByteArray(secretData)
        val mail = SecretDataSubmission(encodedSecretData, PublicKeyDiscloseCondition(publicKeys))
        val mailBytes = mail.encodeToByteArray()
        var responseMail = enclaveClient.sendMail(mailBytes)
        if (responseMail == null) {
            do {
                Thread.sleep(2000)
                //Poll for reply to enclave
                responseMail = enclaveClient.pollMail()
            } while (responseMail == null)
        }

        val replyBytes = responseMail.bodyAsBytes

        try  {
            when (val reply = ProtoBuf.decodeFromByteArray(Message.serializer(), replyBytes)) {
                is SecretDataSubmissionResponse -> {
                    println("Successfully submitted secret data to enclave. Data Reference ID is ${reply.referenceId}")
                }
                is ErrorResponse -> {
                    throw Exception(reply.message)
                }
                else -> {
                    throw Exception("Unknown response from enclave.")
                }
            }
        } catch (e: Exception) {
            println("Error submitting secret data to enclave.")
        }

        webEnclaveTransport.close()
        println("Enclave client exiting.")
        return null
    }

    private class EnclaveConstraintConverter : CommandLine.ITypeConverter<EnclaveConstraint?> {
        override fun convert(value: String): EnclaveConstraint {
            return EnclaveConstraint.parse(value)
        }
    }

    private class EthPublicKeyConverter : CommandLine.ITypeConverter<EthPublicKey> {
        override fun convert(value: String?): EthPublicKey {
            return BigInteger(value, 16)
        }

    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val exitCode = CommandLine(Client()).execute(*args)
            exitProcess(exitCode)
        }
    }

}