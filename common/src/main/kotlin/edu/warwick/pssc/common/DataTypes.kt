package edu.warwick.pssc.conclave

import edu.warwick.pssc.conclave.common.AddressTypeSerializer
import edu.warwick.pssc.conclave.common.BigIntegerSerializer
import edu.warwick.pssc.conclave.common.TypeReferenceSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.NumericType
import org.web3j.abi.datatypes.Type
import java.math.BigInteger

@Serializable
sealed class DataDiscloseCondition <in CheckInputType> {
    abstract fun check(checkInput: CheckInputType): Boolean
}

typealias EthPublicKey = BigInteger
typealias EthPrivateKey = BigInteger

@Serializable
enum class Comparator {
    EQUAL,
    NOT_EQUAL,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    IGNORE
}

typealias ValidationRule = Triple<@Polymorphic Type<@Contextual Any>, Comparator, Int> // Expects a type, a comparator, and an index in output list

typealias ValidationCNF = AndList<OrList<ValidationRule>>


@Serializable
data class PublicKeyDiscloseCondition(
    val publicKeys: List<@Serializable(with = BigIntegerSerializer::class) EthPublicKey>
) : DataDiscloseCondition<EthPublicKey>() {

    override fun check(checkInput: EthPublicKey): Boolean {
        return publicKeys.contains(checkInput)
    }
}

@Serializable
data class EthContractDiscloseCondition(
    @Serializable(with = AddressTypeSerializer::class)
    val contractAddress: Address,
    val functionName: String,
    val inputData: List<@Polymorphic Type<@Contextual Any>?>, // Where the value is null, we substitute with the requesters address in function call
    val outputDataType: List<@Serializable(with= TypeReferenceSerializer::class) TypeReference<Type<@Contextual Any>>>,
    val validationsCNF: ValidationCNF // We allow mixing in the validation rules for the output data by indexing the data they should reference
) : DataDiscloseCondition<List<Type<Any>>>() {
    override fun check(checkInput: List<Type<Any>>): Boolean {
        var andValid = true
        validationsCNF.forEach { OrList ->
            var orValid = false
            OrList.forEach { validationRule ->
                val outputIndex = validationRule.third

                val realValue = checkInput.getOrElse(outputIndex) {
                    throw IllegalArgumentException("Validation rule references output index $outputIndex, but only ${checkInput.size} outputs were provided")
                }

                val expectedValue = validationRule.first
                val comparator = validationRule.second

                val comparisonValid = when (comparator) {
                    Comparator.EQUAL -> realValue == expectedValue
                    Comparator.NOT_EQUAL -> realValue != expectedValue
                    Comparator.IGNORE -> true
                    else -> {
                        if (expectedValue is NumericType && realValue is NumericType) {
                            val expectedValueBigInt = (expectedValue as NumericType).value
                            val realValueBigInt = (realValue as NumericType).value
                            when (comparator) {
                                Comparator.GREATER_THAN -> realValueBigInt > expectedValueBigInt
                                Comparator.GREATER_THAN_OR_EQUAL -> realValueBigInt >= expectedValueBigInt
                                Comparator.LESS_THAN -> realValueBigInt < expectedValueBigInt
                                Comparator.LESS_THAN_OR_EQUAL -> realValueBigInt <= expectedValueBigInt
                                else -> throw IllegalArgumentException("Comparator $comparator is not supported")
                            }
                        } else {
                            throw IllegalArgumentException("Comparator $comparator is not supported for types ${realValue::class.simpleName} and ${expectedValue::class.simpleName}")
                        }
                    }
                }
                orValid = orValid || comparisonValid
            }
            andValid = andValid && orValid
        }
        return andValid
    }
}

typealias AndList<T> = List<T>
typealias OrList<T> = List<T>

@Suppress("Unused")
@Serializable
data class EthMultiContractDiscloseCondition(
    val cnf: AndList<OrList<EthContractDiscloseCondition>>
)

typealias SecretData = ByteArray
