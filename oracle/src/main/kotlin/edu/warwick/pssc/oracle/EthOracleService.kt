package edu.warwick.pssc.oracle

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

class EthOracleService(apiUrl: String,
                       private val defaultFromAddress: Address,
                       private val logger: Logger = LogManager.getLogger()): IOracleService {

    private val web3 = Web3j.build(HttpService(apiUrl))

    init {
        val web3ClientVersion = web3.web3ClientVersion().send()
        val nodeSynced = !web3.ethSyncing().send().isSyncing
        if (!nodeSynced) {
            logger.error("Node is not synced.")
            throw IllegalStateException("Node is not synced.")
        } else {
            logger.info("Successfully connected to node running ${web3ClientVersion.web3ClientVersion}")
        }
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
        return FunctionReturnDecoder.decode(response.value, function.outputParameters)
    }
}
