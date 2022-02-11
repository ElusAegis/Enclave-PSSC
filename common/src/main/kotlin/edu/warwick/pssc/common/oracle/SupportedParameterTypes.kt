//@file:UseSerializers(BigIntegerSerializer::class)
//
//package edu.warwick.pssc.common.oracle
//
//import edu.warwick.pssc.conclave.common.BigIntegerSerializer
//import kotlinx.serialization.Serializable
//import kotlinx.serialization.UseSerializers
//import org.web3j.abi.datatypes.Address
//import org.web3j.abi.datatypes.Bool
//import org.web3j.abi.datatypes.generated.*
//import java.math.BigInteger
//
//
//
//@Serializable
//sealed class SupportedEthereumTypes<Web3jType: Type<*>> {
//
//    abstract fun convert(): Web3jType
//
//    @Serializable
//    data class SerialisationBool(val value: Boolean) : SupportedEthereumTypes<Bool>() {
//        override fun convert(): Bool {
//            return Bool(this.value)
//        }
//    }
//
//    @Serializable
//    data class SerialisationUint8(val value: BigInteger) : SupportedEthereumTypes<Uint8>() {
//        override fun convert(): Uint8 {
//            return Uint8(value)
//        }
//    }
//
//    //uint16 serialisation
//    @Serializable
//    data class SerialisationUint16(val value: BigInteger) : SupportedEthereumTypes<Uint16>() {
//        override fun convert(): Uint16 {
//            return Uint16(value)
//        }
//    }
//
//    //uint32 serialisation
//    @Serializable
//    data class SerialisationUint32(val value: BigInteger) : SupportedEthereumTypes<Uint32>() {
//        override fun convert(): Uint32 {
//            return Uint32(value)
//        }
//    }
//
//    //uint64 serialisation
//    @Serializable
//    data class SerialisationUint64(val value: BigInteger) : SupportedEthereumTypes<Uint64>() {
//        override fun convert(): Uint64 {
//            return Uint64(value)
//        }
//    }
//
//    // uint128 serialisation
//    @Serializable
//    data class SerialisationUint128(val value: BigInteger) : SupportedEthereumTypes<Uint128>() {
//        override fun convert(): Uint128 {
//            return Uint128(value)
//        }
//    }
//
//    @Serializable
//    data class SerialisationUint256(val value: BigInteger) : SupportedEthereumTypes<Uint256>() {
//        override fun convert(): Uint256 {
//            return Uint256(value)
//        }
//    }
//
//
//    // int8 serialisation
//    @Serializable
//    data class SerialisationInt8(val value: BigInteger) : SupportedEthereumTypes<Int8>() {
//        override fun convert(): Int8 {
//            return Int8(value)
//        }
//    }
//
//    // int16 serialisation
//    @Serializable
//    data class SerialisationInt16(val value: BigInteger) : SupportedEthereumTypes<Int16>() {
//        override fun convert(): Int16 {
//            return Int16(value)
//        }
//    }
//
//    // int32 serialisation
//    @Serializable
//    data class SerialisationInt32(val value: BigInteger) : SupportedEthereumTypes<Int32>() {
//        override fun convert(): Int32 {
//            return Int32(value)
//        }
//    }
//
//    // int64 serialisation
//    @Serializable
//    data class SerialisationInt64(val value: BigInteger) : SupportedEthereumTypes<Int64>() {
//        override fun convert(): Int64 {
//            return Int64(value)
//        }
//    }
//
//    // int128 serialisation
//    @Serializable
//    data class SerialisationInt128(val value: BigInteger) : SupportedEthereumTypes<Int128>() {
//        override fun convert(): Int128 {
//            return Int128(value)
//        }
//    }
//
//    // int256 serialisation
//    @Serializable
//    data class SerialisationInt256(val value: BigInteger) : SupportedEthereumTypes<Int256>() {
//        override fun convert(): Int256 {
//            return Int256(value)
//        }
//    }
//
//    // address serialisation
//    @Serializable
//    data class SerialisationAddress(val hexValue: String) : SupportedEthereumTypes<Address>() {
//        override fun convert(): Address {
//            return Address(hexValue)
//        }
//    }
//
//    // bytes1 serialisation
//    @Serializable
//    data class SerialisationBytes1(@Suppress("ArrayInDataClass") val byteArray: ByteArray) : SupportedEthereumTypes<Bytes1>() {
//        override fun convert(): Bytes1 {
//            return Bytes1(byteArray)
//        }
//    }
//
//    // bytes32 serialisation
//    @Serializable
//    data class SerialisationBytes32(@Suppress("ArrayInDataClass") val byteArray: ByteArray) : SupportedEthereumTypes<Bytes32>() {
//        override fun convert(): Bytes32 {
//            return Bytes32(byteArray)
//        }
//    }
//}