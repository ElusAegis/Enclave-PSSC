package edu.warwick.pssc.enclave

import java.security.PublicKey

class OracleManager {

    data class ConnectionData(val publicKey: PublicKey, val routingHint: String?)

    private val oracles : MutableMap<String, ConnectionData> = mutableMapOf()

    fun registerOracle(key : String, oracleConnection : ConnectionData): Boolean {
        val newRegistration = !(key in oracles.keys)

        oracles[key] = oracleConnection

        return newRegistration
    }

    fun getOracles() = oracles.entries

}