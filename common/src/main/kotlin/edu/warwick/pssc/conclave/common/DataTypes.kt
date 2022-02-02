package edu.warwick.pssc.conclave

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigInteger

@Serializable
sealed class DataDiscloseCondition

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BigInteger::class)
class BigIntegerSerializer : KSerializer<BigInteger> {
    override val descriptor = PrimitiveSerialDescriptor("BigInteger", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: BigInteger) = encoder.encodeString(value.toString(16))
    override fun deserialize(decoder: Decoder) = decoder.decodeString().toBigInteger(16)
}

typealias EthPublicKey = BigInteger
typealias EthPrivateKey = BigInteger



@Serializable
data class PublicKeyDiscloseCondition(val publicKeys: List<@Serializable(with = BigIntegerSerializer::class) EthPublicKey>):
    DataDiscloseCondition()

typealias SecretData = ByteArray
