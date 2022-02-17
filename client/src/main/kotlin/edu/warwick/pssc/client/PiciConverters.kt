package edu.warwick.pssc.client

import com.r3.conclave.common.EnclaveConstraint
import edu.warwick.pssc.conclave.EthPublicKey
import org.web3j.abi.datatypes.Address
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

class AddressConverter: CommandLine.ITypeConverter<Address> {
    override fun convert(value: String): Address {
        return Address(value.slice(2 until value.length))
    }
}