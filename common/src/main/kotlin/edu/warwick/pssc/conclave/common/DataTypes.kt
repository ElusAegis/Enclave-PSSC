package edu.warwick.pssc.conclave

import edu.warwick.pssc.conclave.common.BigIntegerSerializer
import kotlinx.serialization.Serializable
import java.math.BigInteger

@Serializable
sealed class DataDiscloseCondition

typealias EthPublicKey = BigInteger
typealias EthPrivateKey = BigInteger



@Serializable
data class PublicKeyDiscloseCondition(val publicKeys: List<@Serializable(with = BigIntegerSerializer::class) EthPublicKey>):
    DataDiscloseCondition()

typealias SecretData = ByteArray
