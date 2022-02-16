package edu.warwick.pssc.enclave

import edu.warwick.pssc.conclave.EthPublicKey
import org.web3j.crypto.Sign

class DataRequestValidator {


    companion object {

        /**
         * Get the key that has signed the data reference id.
         */
        fun getSignedPublicKey(
            dataSignedOver: ByteArray,
            signature: Sign.SignatureData
        ): EthPublicKey {

            val signedKey = Sign.signedMessageToKey(dataSignedOver, signature)

            return signedKey
        }
    }

}



