package edu.warwick.pssc.enclave.modules

import com.r3.conclave.enclave.EnclavePostOffice
import edu.warwick.pssc.conclave.BlockInfo
import edu.warwick.pssc.conclave.OracleKey
import edu.warwick.pssc.conclave.common.OracleRegistration
import edu.warwick.pssc.enclave.ConnectionData
import edu.warwick.pssc.enclave.PSSCEnclave
import java.security.PublicKey

/**
 * We provide a link to the enclave to enable triggering event when a specific action happens
 */
class OracleManager(val messageBroker: PSSCEnclave.MessageBroker) {

    /**
     * A percent of Oracles needs to agree on a block to proceed to next epoch
     */
    private val REQUIED_CONSENSUS = 0.5

    /**
     * The number of blocks any oracle can miss before he will be kicked out from the oracle list
     */
    private val ALLOWED_MISSED_BLOCKS = 1000 // We can calculate max timeout by MAX_MISSED_BLOCK_VOTES * 15 seconds (which is average ethereum block time in seconds)

    data class BlockVoting(var missedBlocks: Int, var nextVotedBlock: BlockInfo?) { companion object { val initial = BlockVoting(0, null) } }

    data class Oracle(var connection: ConnectionData, val voting: BlockVoting)

    /**
     * We allow the first oracle to provide the Current Block information
     * This can be done by the host on startup
     * Other Oracles will rad this block information and, if it is correct, proceed building on it
     */
    var currentBlock: BlockInfo? = null
        private set

    /**
     * Map that counts votes for the next block to be built
     */
    private val nextBlockVotes = mutableMapOf<BlockInfo, Int>()

    private val oracles : MutableMap<OracleKey, Oracle> = mutableMapOf()

    /**
     * We only register new oracles at the end of current epoch to disable them form influencing the current requests
     */
    private val pendingOracleRegistrations : MutableMap<OracleKey, Pair<Oracle, EnclavePostOffice>> = mutableMapOf()

    private fun executeNextEpoch(nextBlock: BlockInfo) {
        currentBlock = nextBlock
        messageBroker.logInfo("New Epoch with block $currentBlock")

        oracles.entries.forEach {
            val oracle = it.value
            if (oracle.voting.nextVotedBlock != currentBlock) {
                oracle.voting.missedBlocks += 1
            }

            it.setValue(oracle)
        }

        // DeRegister oracles that have missed too many blocks
        oracles.entries.filter {
            it.value.voting.missedBlocks > ALLOWED_MISSED_BLOCKS
        }.forEach {
            messageBroker.logInfo("DeRegistered Oracle ${it.key}.")
            messageBroker.sendMessage(OracleRegistration.DeRegistrationMessage, it.value.connection.publicKey, it.value.connection.routingHint)
            oracles.remove(it.key)
        }

        // Register new oracles
        pendingOracleRegistrations.entries.forEach {
            messageBroker.logInfo("Registered Oracle ${it.key}.")
            messageBroker.sendMessage(OracleRegistration.Success, it.value.second, it.value.first.connection.routingHint)
            oracles[it.key] = it.value.first
        }
        pendingOracleRegistrations.clear()

        messageBroker.executeRequestManagerEpoch()
    }


    fun receiveBlockVote(key: OracleKey, nextBlock: BlockInfo) {

        val oracle = oracles[key] ?: throw IllegalArgumentException("Oracle with key $key not found")

        if (currentBlock == null) {
            messageBroker.logInfo("Received genesis block from $key for block number ${nextBlock.blockNumber}")
            // If the Enclave has just started, we accept the first submission as the current block
            currentBlock = nextBlock
        } else {
            messageBroker.logInfo("Received block vote from $key for block number ${nextBlock.blockNumber}")
            oracle.voting.nextVotedBlock = nextBlock

            nextBlockVotes[nextBlock] = nextBlockVotes.getOrDefault(nextBlock, 0) + 1

            if (nextBlockVotes.getOrDefault(nextBlock, 0) >= oracles.size * REQUIED_CONSENSUS) {
                executeNextEpoch(nextBlock)
            }
        }


    }


    /**
     * We only fully register oracle at the end of each epoch
     * This is to prevent oracle from influencing the current requests
     * For now oracle will be added to the waiting list
     */
    fun registerOracle(key : OracleKey, oracleConnection : ConnectionData, postOffice: EnclavePostOffice): Boolean {
        if (oracles.isEmpty()) {
            oracles[key] = Oracle(oracleConnection, BlockVoting.initial)
            messageBroker.logInfo("Registered First Oracle $key")
            messageBroker.sendMessage(OracleRegistration.Success, postOffice, oracleConnection.routingHint)
            return true
        }

        val newRegistration = key !in oracles.keys

        if (newRegistration) {
            messageBroker.logInfo("Accepted Oracle $key registration request")
            pendingOracleRegistrations[key] = Pair(Oracle(oracleConnection, BlockVoting.initial), postOffice)
        } else {
            messageBroker.logInfo("Rejected Oracle $key registration request. Oracle already registered")
            oracles[key]!!.connection = oracleConnection // If the oracle is already registered, we update the connection with the new information
        }


        return newRegistration
    }

    fun getOracleId(key: PublicKey): OracleKey? {
        return oracles.entries.find { it.value.connection.publicKey == key }?.key
    }



    fun getOracleWithConnections() = oracles.entries.map { Pair(it.key, it.value.connection) }

}