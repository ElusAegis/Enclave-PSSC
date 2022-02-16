package edu.warwick.pssc.enclave

import java.security.PublicKey

class OracleManager {
    private val oracles : MutableSet<PublicKey> = mutableSetOf()

    fun registerOracle(oracle : PublicKey): Boolean {
        if (oracle in oracles) {
            return false
        }
        oracles.add(oracle)
        return true
    }

    fun getOracles() : Set<PublicKey> {
        return oracles
    }

}