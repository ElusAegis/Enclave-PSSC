package edu.warwick.pssc.enclave

import edu.warwick.pssc.conclave.DataDiscloseCondition
import edu.warwick.pssc.conclave.SecretData
import java.util.*

class SecretDatabase {

    private val storage = HashMap<UUID, Pair<SecretData, DataDiscloseCondition>>()

    fun storeData(data: SecretData, discloseCondition: DataDiscloseCondition): UUID {
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
     * Get data from the database by secretDataId
     */
    fun getData(secretDataId: UUID): Pair<SecretData, DataDiscloseCondition> {
        // Check that secretDataId is valid
        if (!storage.containsKey(secretDataId))
            throw IllegalArgumentException("SecretDataId is not valid")

        return storage[secretDataId]!!
    }
}
