package edu.warwick.pssc.conclave.client

import com.r3.conclave.common.EnclaveConstraint
import edu.warwick.pssc.conclave.EthPublicKey
import picocli.CommandLine
import java.math.BigInteger
import java.util.*

class EnclaveConstraintConverter : CommandLine.ITypeConverter<EnclaveConstraint?> {
    override fun convert(value: String): EnclaveConstraint {
        return EnclaveConstraint.parse(value)
    }
}

class EthPublicKeyConverter : CommandLine.ITypeConverter<EthPublicKey> {
    override fun convert(value: String?): EthPublicKey {
        return BigInteger(value, 16)
    }

}

class UUIDConverter: CommandLine.ITypeConverter<UUID> {
    override fun convert(value: String): UUID {
        return UUID.fromString(value)
    }
}