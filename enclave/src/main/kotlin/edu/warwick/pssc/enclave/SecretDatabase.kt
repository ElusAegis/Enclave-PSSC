package edu.warwick.pssc.enclave

import edu.warwick.pssc.conclave.DataDiscloseCondition
import edu.warwick.pssc.conclave.SecretData
import java.util.*
import kotlin.collections.HashMap

class SecretDatabase {

    private val storage = HashMap<UUID, Pair<SecretData, DataDiscloseCondition>>()

    fun add(data: SecretData, discloseCondition: DataDiscloseCondition): UUID {
        val secretDataId = UUID.randomUUID()
        storage[secretDataId] = Pair(data, discloseCondition)

        return secretDataId
    }

    fun remove(secretDataId: UUID) {
        // Check that secretDataId is valid
        if (storage.containsKey(secretDataId))
            throw IllegalArgumentException("SecretDataId is not valid")

        storage.remove(secretDataId)
    }

    fun updateDiscloseCondition(secretDataId: UUID, discloseCondition: DataDiscloseCondition) {
        // Check that secretDataId is valid
        if (storage.containsKey(secretDataId))
            throw IllegalArgumentException("SecretDataId is not valid")

        storage[secretDataId] = Pair(storage[secretDataId]!!.first, discloseCondition)
    }

    fun updateData(secretDataId: UUID, data: SecretData) {
        // Check that secretDataId is valid
        if (storage.containsKey(secretDataId))
            throw IllegalArgumentException("SecretDataId is not valid")

        storage[secretDataId] = Pair(data, storage[secretDataId]!!.second)
    }
}
