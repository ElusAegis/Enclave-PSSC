package edu.warwick.pssc.oracle

import edu.warwick.pssc.conclave.BlockInfo
import edu.warwick.pssc.conclave.common.ErrorMessage
import edu.warwick.pssc.conclave.common.OracleBlock
import edu.warwick.pssc.conclave.common.OracleEthDataCall
import edu.warwick.pssc.conclave.common.SuccessMessage
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.http.HttpService
import java.math.BigInteger

class EthOracleService(apiUrl: String, private val defaultFromAddress: Address, private val serviceProvider: Oracle.ServiceProvider): IOracleService {

    private val logger: Logger = LogManager.getLogger()
    private val web3 = Web3j.build(HttpService(apiUrl))

    private val ENCLAVE_POLLING_INTERVAL_MS = 2000L


    init {
        val web3ClientVersion = web3.web3ClientVersion().send()
        val nodeSynced = !web3.ethSyncing().send().isSyncing
        if (!nodeSynced) {
            logger.error("Node is not synced.")
            throw IllegalStateException("Node is not synced.")
        } else {
            logger.info("Successfully connected to node running ${web3ClientVersion.web3ClientVersion}")
        }

        // Launch a coroutine
    }

    suspend fun getNewRequestsFromEnclave() {
        // Continuously poll the Enclave API for new messages
        while (true) {
            val (message, topic) = serviceProvider.getEnclave().pollMessageWithTopic(ENCLAVE_POLLING_INTERVAL_MS)

            logger.debug("Received message from enclave.")

            try {
                when (message) {
                    is OracleEthDataCall.Request -> {
                        logger.info("Received OracleEthDataCall Request from enclave.")
                        val replyMessage = try {
                            val returnValue = doEthDataCall(message.contractAddress, message.functionName, message.inputData, message.outputDataType)
                            OracleEthDataCall.Response(true, returnValue)
                        } catch (e: Exception) {
                            logger.error("Failed to process OracleEthDataCall request. Error occurred. ${e.message}")
                            OracleEthDataCall.Response(false, emptyList())
                        }

                        serviceProvider.getEnclave().sendMail(replyMessage, topic)
                        logger.info("Sent OracleEthDataCall Response to enclave.")

                    }
                    is OracleBlock.CurrentResponse -> {} // This message is handled by the other thread
                    is SuccessMessage -> {} // This message is handled by the other thread TODO - have multiple enclave connections to have no overlap
                    is ErrorMessage -> { logger.error("Received ErrorMessage from enclave. ${message.message}") }
                    else -> {
                        throw RuntimeException("Incorrect response message from the Enclave, got ${message::class.simpleName}")
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to process message from the Enclave.", e.message)
            }
        }
    }


    fun getLatestBlock(): BlockInfo {
        val latestBlock = web3.ethGetBlockByNumber(DefaultBlockParameter.valueOf("latest"), false).send().block
        return BlockInfo(latestBlock.number, latestBlock.hash)
    }

    fun getNextBlock(currentBlockNumber: BigInteger): BlockInfo {
        val nextBlockNumber = currentBlockNumber.add(BigInteger.ONE)
        val nextBlock = web3.ethGetBlockByNumber(DefaultBlockParameter.valueOf(nextBlockNumber), false).send().block
        return BlockInfo(nextBlock.number, nextBlock.hash)
    }



    fun doEthDataCall(
        contractAddress: Address,
        functionName: String,
        inputData: List<Type<Any>>,
        outputDataType: List<TypeReference<Type<Any>>>,
        fromAddress: Address = defaultFromAddress
    ): List<Type<Any>> {
        val function = Function(functionName, inputData, outputDataType)
        val encodedFunction = FunctionEncoder.encode(function)
        val callTransaction =
            Transaction.createEthCallTransaction(fromAddress.value, contractAddress.value, encodedFunction)

        val response = web3.ethCall(callTransaction, DefaultBlockParameter.valueOf("latest")).send()
        if (response.isReverted) {
            logger.error("Eth call reverted: ${response.revertReason}")
            throw IllegalStateException("Eth call reverted: ${response.error.message}")
        }
        val decodedResult = FunctionReturnDecoder.decode(response.value, function.outputParameters)

        // Check the results
        if (decodedResult.size != outputDataType.size) {
            logger.error("Eth call returned incorrect result size.")
            throw IllegalStateException("Eth call returned incorrect result size.")
        }

        for (i in decodedResult.indices) {
            if (decodedResult[i].javaClass != outputDataType[i].type.javaClass) {
                logger.error("Eth call returned incorrect result type.")
                throw IllegalStateException("Eth call returned incorrect result type.")
            }
        }

        return decodedResult
    }
}
