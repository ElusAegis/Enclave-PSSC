package edu.warwick.pssc.oracle

import edu.warwick.pssc.conclave.common.ErrorMessage
import edu.warwick.pssc.conclave.common.OracleBlock
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.apache.logging.log4j.LogManager
import java.math.BigInteger

class BlockSyncService(val serviceProvider: Oracle.ServiceProvider) {

    private val TIMEOUT_SECONDS = 10L

    private val logger = LogManager.getLogger()

    suspend fun syncWithEnclave() = coroutineScope {
        kotlin.run {
            while (true) {
                getLatestBlockInEnclave()
                delay(TIMEOUT_SECONDS * 1000)
            }
        }
    }

    /**
     * Get the latest block that the Enclave has confirmed
     */
    private suspend fun getLatestBlockInEnclave() {
        val reply = serviceProvider.getEnclave().sendAndWaitForMail(OracleBlock.CurrentRequest)

        when (reply) {
            is OracleBlock.CurrentResponse -> {
                val enclaveCurrentBlock = reply.block
                if (enclaveCurrentBlock == null) {
                    // We are the first to provide block into the Enclave
                    // Provide it with the latest block we know of
                    logger.info("No block in Enclave, providing first block")
                    submitBlockToEnclave(null)
                } else {
                    logger.info("Enclave has latest block ${enclaveCurrentBlock.blockNumber}")
                    val latestBlock = serviceProvider.getOracle().getLatestBlock()

                    if (latestBlock.blockNumber > enclaveCurrentBlock.blockNumber){
                        logger.info("New block available in Oracle, submitting block to Enclave")
                        submitBlockToEnclave(enclaveCurrentBlock.blockNumber)
                    } else {
                        logger.info("No new block available in Oracle")
                    }
                }
            }
            else -> throw Exception("Unexpected response from Enclave: $reply")
        }
    }

    /**
     * We submit the following block to the Enclave
     * If the blockNumber is null, we submit the latest block we know of
     */
    private suspend fun submitBlockToEnclave(blockNumber: BigInteger?) {
        val nextBlock = if (blockNumber == null) {
            serviceProvider.getOracle().getLatestBlock()
        } else {
            serviceProvider.getOracle().getNextBlock(blockNumber)
        }

        val response = serviceProvider.getEnclave().sendAndWaitForMail(OracleBlock.NextSubmission(nextBlock))

        if (response is ErrorMessage) {
            logger.error("Our block submission was rejected by the Enclave: ${response.message}")
        } else {
            logger.info("Submitted block ${nextBlock.blockNumber} to the Enclave")
        }
    }
}