package edu.warwick.pssc.enclave.modules

import com.r3.conclave.enclave.EnclavePostOffice
import edu.warwick.pssc.conclave.EthPublicKey
import edu.warwick.pssc.conclave.OracleKey
import edu.warwick.pssc.conclave.common.ErrorMessage
import edu.warwick.pssc.enclave.PSSCEnclave
import org.web3j.abi.datatypes.Type
import java.util.*

class RequestManager(val messageBroker: PSSCEnclave.MessageBroker) {

    /**
     * Minimal % of Oracles that need to respond with the same value from the total amount of Oracles notified for the request to be valid
     */
    private val MIN_ORACLE_TURNOUT = 0.5


    /**
     * Each request is alive for one epoch, and we allow new Oracles to join the request for next epoch after their registration
     * This protects us from malicious duplicate submissions
     */
    data class OracleSubmissions (
        val notifiedOracles: Int,
        val submissions: MutableMap<String, List<Type<Any>>?>
    )

    data class RequesterDetails (
        val routingHint: String?,
        val requestPostOffice: EnclavePostOffice,
        val verifiedPublicKey: EthPublicKey,
    )

    data class PendingDataRequest(val dataId: UUID, val requester: RequesterDetails, val oracleSubmissions: OracleSubmissions)

    /** Map between the Topic of the request sent to Oracle to the UUID of the */
    private val pendingDataRequests : MutableMap<UUID, PendingDataRequest> = mutableMapOf()


    /**
     * Once epoch is executed, all still pending requests are considered invalid
     * Thus we notify the requester that the request has been invalidated
     * and remove it from the pending requests map
     */
    fun executeEpoch() {
        pendingDataRequests.forEach { (_, request) ->
            request.requester.apply {
                messageBroker.sendMessage(ErrorMessage("Insufficient amount of oracles has responded to the request"), requestPostOffice, routingHint)
            }
        }
    }

    /**
     * If request has received sufficient submissions, we check if there is consensus among them (one value has more than MIN_CONSENSUS proportion of votes)
     * And if sufficient amount of Oracles have submitted information
     * If so, we check the condition and return data.
     * If not, we invalidate the request and send response to the requester
     */
    private fun executeRequest(request: PendingDataRequest): List<Type<Any>>? {
        // Check if there is consensus among the oracles
        val oracleSubmissions = request.oracleSubmissions.submissions
        val oracleVotes = oracleSubmissions.entries
            .groupBy { it.value }
            .mapValues { it.value.count() }

        val (topVotedValue, voteCount) = oracleVotes.entries.maxByOrNull { it.value }?.toPair() ?: Pair(null, 0)

        if (voteCount < request.oracleSubmissions.notifiedOracles * MIN_ORACLE_TURNOUT)
            throw IllegalStateException("Not sufficient amount of Oracles has responded to the query.")

        return topVotedValue
    }

    fun registerResponse(requestId: UUID, oracle: OracleKey, data: List<Type<Any>>?): Triple<List<Type<Any>>?, UUID, RequesterDetails> {
        pendingDataRequests[requestId].apply {
            if (this == null)
                throw java.lang.IllegalArgumentException("Request with id $requestId does not exist.")

            oracleSubmissions.submissions[oracle] = data

            // Try to evaluate the request
            // If insufficient amount of oracles have submitted, we keep waiting
            // If there is consensus on the data, we return the data
            return Triple(executeRequest(this), this.dataId, this.requester)
        }
    }

    fun addRequest(dataReferenceId: UUID, requester: RequesterDetails, notifiedOracles: Int): UUID {
        val requestId = UUID.randomUUID()
        pendingDataRequests[requestId] = PendingDataRequest(dataReferenceId, requester, OracleSubmissions(notifiedOracles, mutableMapOf()))
        return requestId
    }

}

