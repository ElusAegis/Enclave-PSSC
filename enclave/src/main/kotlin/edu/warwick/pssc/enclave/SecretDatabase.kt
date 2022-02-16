package edu.warwick.pssc.enclave

import edu.warwick.pssc.conclave.DataDiscloseCondition
import edu.warwick.pssc.conclave.SecretData
import java.util.*

class SecretDatabase {

    private val storage = HashMap<UUID, Pair<SecretData, DataDiscloseCondition<*>>>()

    fun storeData(data: SecretData, discloseCondition: DataDiscloseCondition<Any>): UUID {
        val secretDataId = UUID.randomUUID()
        storage[secretDataId] = Pair(data, discloseCondition)

        return secretDataId
    }

    @Suppress("unused")
    fun remove(secretDataId: UUID) {
        // Check that secretDataId is valid
        if (storage.containsKey(secretDataId))
            throw IllegalArgumentException("SecretDataId is not valid")

        storage.remove(secretDataId)
    }

    /**
     * An explicit method to get data with security bound to prevent accidental data disclosure
     */
    fun getDataIfAuthorised(
        secretDataId: UUID,
        authorised: Boolean,
    ): SecretData {
        if (!authorised) {
            throw IllegalArgumentException("Requested data is not authorised!")
        }

        if (!storage.containsKey(secretDataId))
            throw IllegalArgumentException("SecretDataId is not valid")

        return storage[secretDataId]!!.first
    }

    fun getCondition(secretDataId: UUID): DataDiscloseCondition<*> {
        // Check that secretDataId is valid
        if (!storage.containsKey(secretDataId))
            throw IllegalArgumentException("SecretDataId is not valid")

        return storage[secretDataId]!!.second
    }
}
