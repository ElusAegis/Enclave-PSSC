@file:OptIn(ExperimentalSerializationApi::class)

package edu.warwick.pssc.conclave.common


import edu.warwick.pssc.conclave.AddressPlaceholder
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.protobuf.ProtoBuf
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.*
import org.web3j.abi.datatypes.generated.*
import org.web3j.crypto.Sign
import java.math.BigInteger
import java.util.*
import kotlin.reflect.KClass


object MessageSerializer {

    private val serializer = ProtoBuf { serializersModule = web3jSerializationModule }

    fun Message.encodeMessage(): ByteArray {
        return serializer.encodeToByteArray(Message.serializer(), this)
    }

    fun ByteArray.decodeMessage(): Message {
        return serializer.decodeFromByteArray(Message.serializer(), this)
    }
}

val web3jSerializationModule = SerializersModule {

    polymorphic(Type::class) {
        subclass(Address::class, AddressTypeSerializer())
        subclass(Bool::class, BoolTypeSerializer())
        subclass(AddressPlaceholder::class, AddressPlaceholder.serializer())
        NumericTypeSerializer.types.forEach {
            subclass(it, NumericTypeSerializer(it.java))
        }
    }
}


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

class AddressTypeSerializer : KSerializer<Address> {
    override val descriptor = PrimitiveSerialDescriptor("Address", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Address) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder) = Address(decoder.decodeString())
}

class BoolTypeSerializer : KSerializer<Bool> {
    override val descriptor = PrimitiveSerialDescriptor("Bool", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Bool) = encoder.encodeBoolean(value.value)

    override fun deserialize(decoder: Decoder) = Bool(decoder.decodeBoolean())
}

class NumericTypeSerializer<T: NumericType>(private val tClass: Class<T>) : KSerializer<T> {

    override val descriptor = buildClassSerialDescriptor("Numeric${tClass.simpleName}") {
        element("value", BigIntegerSerializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: T) {
        return encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, BigIntegerSerializer(), value.value)
        }
    }

    override fun deserialize(decoder: Decoder) = decoder.decodeStructure(descriptor) {
        var bi: BigInteger? = null
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                0 -> bi = decodeSerializableElement(descriptor, 0, BigIntegerSerializer())
                CompositeDecoder.DECODE_DONE -> break
                else -> throw SerializationException("Unknown index $index")
            }
        }

        @Suppress("UNCHECKED_CAST")
        tClass.getConstructor(BigInteger::class.java).newInstance(bi) as T
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        val types : List<KClass<IntType>> = listOf(
            Uint8::class,
            Uint16::class,
            Uint32::class,
            Uint64::class,
            Uint128::class,
            Uint256::class,
            Int8::class,
            Int16::class,
            Int32::class,
            Int64::class,
            Int128::class,
            Int256::class,
        ) as List<KClass<IntType>>
    }
}

object TypeReferenceSerializer: KSerializer<TypeReference<Type<Any>>> {
    override val descriptor = PrimitiveSerialDescriptor("TypeReference", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: TypeReference<Type<Any>>) {
        val typeName = value.classType.canonicalName
        encoder.encodeString(typeName)
    }

    override fun deserialize(decoder: Decoder): TypeReference<Type<Any>> {
        val typeName = decoder.decodeString()
        try {
            @Suppress("UNCHECKED_CAST") // We know this is safe
            val type = Class.forName(typeName) as Class<Type<Any>>
            return TypeReference.create(type)
        } catch (e: ClassNotFoundException) {
            throw IllegalArgumentException("Could not find Web3j class for provided $typeName")
        }
    }
}