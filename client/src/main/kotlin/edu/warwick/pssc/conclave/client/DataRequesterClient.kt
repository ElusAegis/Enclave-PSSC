package edu.warwick.pssc.conclave.client

import com.r3.conclave.client.EnclaveClient
import com.r3.conclave.client.web.WebEnclaveTransport
import com.r3.conclave.common.EnclaveConstraint
import com.r3.conclave.common.EnclaveException
import edu.warwick.pssc.conclave.EthPrivateKey
import edu.warwick.pssc.conclave.client.connection.sendAndWaitForMail
import edu.warwick.pssc.conclave.common.ErrorMessage
import edu.warwick.pssc.conclave.common.Message.Companion.deserializeMail
import edu.warwick.pssc.conclave.common.SecretDataRequest
import edu.warwick.pssc.conclave.common.toByteArray
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Sign
import picocli.CommandLine
import java.util.*
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
class DataRequesterClient : Callable<Void?> {

    private val logger = LogManager.getLogger()


    @CommandLine.Option(names = ["-i", "--id"],
        required = true,
        interactive = true,
        description = ["The id of the secret data to query."],
        converter = [UUIDConverter::class])
    private var dataReferenceId: UUID? = null

    @CommandLine.Option(
        names = ["-prv", "--private-key"],
        required = true,
        description = ["The private key to prove ownership of pubic key."],
        converter = [EthPublicKeyConverter::class]
    )
    private var privateKey: EthPrivateKey? = null

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


        val signingKeyPair = ECKeyPair.create(privateKey!!)

        // We sign over the requested data id
        // We do not protect against the replay attack at the moment, as Enclave can not serve as a source of randomness
        // And any randomness produced by the client can be forged by the enclave host
        val dataReferenceIdSignature = Sign.signMessage(dataReferenceId!!.toByteArray(), signingKeyPair)


        val mail = SecretDataRequest.Request(
            dataReferenceId!!,
            dataReferenceIdSignature
        )

        val mailBytes = mail.encodeToByteArray()

        val responseMail = enclaveClient.sendAndWaitForMail(mailBytes)

        try  {
            when (val reply = responseMail.bodyAsBytes.deserializeMail()) {
                is SecretDataRequest.Response -> {
                    val secretData = ProtoBuf.decodeFromByteArray<String>(reply.data)
                    logger.log(Level.INFO, "Successfully got secret data: $secretData")
                }
                is ErrorMessage -> {
                    throw EnclaveException(reply.message)
                }
                else -> {
                    throw EnclaveException("Unknown response from enclave.")
                }
            }
        } catch (e: Exception) {
            logger.log(Level.WARN, "Error getting secret data: from enclave: ${e.message}")
        }

        webEnclaveTransport.close()
        logger.log(Level.INFO, "Enclave client exiting.")
        return null
    }




    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val exitCode = CommandLine(DataRequesterClient()).execute(*args)
            exitProcess(exitCode)
        }
    }



}