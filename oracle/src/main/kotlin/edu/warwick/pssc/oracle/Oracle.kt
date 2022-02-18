package edu.warwick.pssc.oracle

import edu.warwick.pssc.conclave.OracleKey
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.web3j.abi.datatypes.Address

class Oracle(apiUrl: String, enclaveUrl: String, enclaveConstraint: String, oracleKey: OracleKey) {

    private val enclave = EnclaveService(enclaveUrl, enclaveConstraint, oracleKey)
    private val oracle = EthOracleService(apiUrl, Address("0"), ServiceProvider())
    private val blockSync = BlockSyncService(ServiceProvider())

    inner class ServiceProvider {
        fun getOracle(): EthOracleService {
            return oracle
        }
        fun getEnclave(): EnclaveService {
            return enclave
        }
    }

    private fun run() {
        runBlocking {
            launch {
                blockSync.syncWithEnclave()
            }
            launch {
                oracle.getNewRequestsFromEnclave()
            }
        }
    }


    /**
     * The connection is established during initialization.
     * Thus, we can safely close it here
     */
    private fun shutdown() {
        enclave.stop()
    }


    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            val client = Oracle(apiUrl = args[0], enclaveUrl = args[1], enclaveConstraint = args[2], oracleKey = args[3])

            try {
                client.run()
            } catch (e: Exception) {
                // Do a safe shutdown, as we have already established connection to the enclave
                client.shutdown()
                throw e
            }
        }
    }
}