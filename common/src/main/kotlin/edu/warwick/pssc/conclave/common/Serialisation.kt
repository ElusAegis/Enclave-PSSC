package edu.warwick.pssc.conclave.common

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import org.web3j.crypto.Sign
import java.math.BigInteger
import java.util.*

class UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)


    override fun serialize(encoder: Encoder, value: UUID) =
        encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): UUID =
        UUID.fromString(decoder.decodeString())
}

fun UUID.toByteArray() = ByteArray(16) {
    if (it < 8) {
        (this.leastSignificantBits shr (it * 8)).toByte()
    } else {
        (this.mostSignificantBits shr ((it - 8) * 8)).toByte()
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BigInteger::class)
class BigIntegerSerializer : KSerializer<BigInteger> {
    override val descriptor = PrimitiveSerialDescriptor("BigInteger", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: BigInteger) = encoder.encodeString(value.toString(16))
    override fun deserialize(decoder: Decoder) = decoder.decodeString().toBigInteger(16)
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = Sign.SignatureData::class)
class SignatureSerializer : KSerializer<Sign.SignatureData> {
    override val descriptor = buildClassSerialDescriptor("EthSignatureData") {
        element<ByteArray>("r")
        element<ByteArray>("s")
        element<ByteArray>("v")
    }

    override fun serialize(encoder: Encoder, value: Sign.SignatureData) = encoder.encodeStructure(descriptor) {
        encodeSerializableElement(descriptor, 0, ByteArraySerializer(), value.r)
        encodeSerializableElement(descriptor, 1, ByteArraySerializer(), value.s)
        encodeSerializableElement(descriptor, 2, ByteArraySerializer(), value.v)
    }

    override fun deserialize(decoder: Decoder): Sign.SignatureData = decoder.decodeStructure(descriptor) {
        var r: ByteArray? = null
        var s: ByteArray? = null
        var v: ByteArray? = null

        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                0 -> r = decodeSerializableElement(descriptor, 0, ByteArraySerializer())
                1 -> s = decodeSerializableElement(descriptor, 1, ByteArraySerializer())
                2 -> v = decodeSerializableElement(descriptor, 2, ByteArraySerializer())
                CompositeDecoder.DECODE_DONE -> break
                else -> throw SerializationException("Unknown index $index")
            }
        }

        Sign.SignatureData(v, r, s)
    }
}