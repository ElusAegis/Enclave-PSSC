# Research Topic 

1. How to protect about data request replay, so that the Enclave could not reverse enclave to a previous state, replay the event horison, and then request all the request again.
   1. Use Oracles to sign true random messages from inside the enclave to outside oracle to prevent replay.

## Enclaves

### Replay protection for deleted data:

**How to delete data from the enclave so that the host could not revert the enclave and read it**


3. Why can not store a state of the Enclave as a hash of (hash previous state + hash of new message + random), and communicate this hash every time to the Client? And client would include this new hash in his message, 
   which we will check to control for the replay attacks?
   We have many connected clients, and updating one state will invalidate all the other states, we would need to allow to have past lookups
   One way to protect is to build a blockchain of states, that prevent the Encalve from replaying the same messages, further than by 1 position. The downside is that if there comes the restart, all the data will be lost.
   So it is not justified unless we want to prevent any data restarts.
4. One way to enforce the global state of the enclave is to require double message sign-ins. First, we challenge the client with the state of enclave, and he responds with the message + state of the encalve. If the state has changed while the client was sending the message, the client can try again.
    This enforses quick challenge complition time and the fact that all messages can be placed into a chain, which is impossible to reorder.

As a result, we can not guarantee 100% replay protected data deletion, thus we will remove this as a feature. Instead, we will introduce pay for storage model, where the more you pay, the longer you data is stored in the Enclave.
But there will be no guaranteed deletion of data, as well as change to data.

### Replay protection for data disclosure messages

Use the hash of data to make it data specific

But - we can not return an hash of the data as its id as some hashes are well known, and it will prevent secrecy of the data.

We could do `hash(random)` to reduce information spilage. 

We could generate id as `hash(hash(data) + random)`

This way we protect the common hash attack as well as prevent replay for the requests for different data - we only prove id to get specific data.

Important to sign the requested data id to prevent the replay attack

We might consider encrypting the data with the Ethereum Public Key to protect it further against replay attacks

## Constraints

### Signatures in Ethereum

We can omit using the public key in a sending message due to the paper bellow

This describes the alogorithm to perform signature verification
https://www.secg.org/sec1-v2.pdf - section 4.6.1
https://en.wikipedia.org/wiki/Elliptic_Curve_Digital_Signature_Algorithm

org/web3j/crypto/Sign.java

## On-chain constraints

Ethereum Library we have used - web3j, does not allow to load the contracts dynamically, which means we can not interact with any arbitrary contract and can rather only use the predefined contracts, for which we have created Contract Wrappers

Alternative is to use a JS application which would perform data queries to the blockchain and pass it back to the Java program to respond to the enclave. We would use sockets to implement this also will later enable us to have a lot more functionality for which data sources we can use for Enclaves 

## Clients

### Loggers

we have added logger form log4j, being aware of its issues with the vulnerability

##