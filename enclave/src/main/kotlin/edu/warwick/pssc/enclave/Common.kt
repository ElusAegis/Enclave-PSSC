package edu.warwick.pssc.enclave

import java.security.PublicKey

data class ConnectionData(val publicKey: PublicKey, val routingHint: String?)

data class EnclaveStateException(override val message: String): Exception()