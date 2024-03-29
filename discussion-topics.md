# Research Topic 

0xc7AD46e0b8a400Bb3C915120d284AafbA8fc4735

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

### Multithreading Enclave

If we multithread we would be able to make the enclave wait and then pick up from the check point that we have specified. But this increases the complexity of the implemintation. Alternitively, we can just have a signle thread and code implemintation for checkpoints. 

### How do we wait for Oracle data?

Encalves are a state machine, meaning we have no option to check the status unless we trust host. But we do not want to do so. 

Additionally, how do we decide if all active Oracles have answered the challenge, rather then the time was dspead up ond only Host controlled Oracles answered it

## Constraints

## Replay attacks until correct 

What we’ve just shown is an example of a simulation. Note that in a world where time runs only forward and nobody can trick me with a time machine, the hat-based protocol is correct and sound, meaning that after E^2 rounds I should be convinced (with all but negligible probability) that the graph really is colorable and that Google is putting valid inputs into the protocol.

What we’ve just shown is that if time doesn’t run only forward — specifically, if Google can ‘rewind’ my view of time — then they can fake a valid protocol run even if they have no information at all about the actual graph coloring.

But we should calculate how many rounds it takes to get a correct protocol run without any information about the constraints.

### Signatures in Ethereum

We can omit using the public key in a sending message due to the paper bellow

This describes the alogorithm to perform signature verification
https://www.secg.org/sec1-v2.pdf - section 4.6.1
https://en.wikipedia.org/wiki/Elliptic_Curve_Digital_Signature_Algorithm

org/web3j/crypto/Sign.java

## On-chain constraints

Ethereum Library we have used - web3j, does not allow to load the contracts dynamically, which means we can not interact with any arbitrary contract and can rather only use the predefined contracts, for which we have created Contract Wrappers

Alternative is to use a JS application which would perform data queries to the blockchain and pass it back to the Java program to respond to the enclave. We would use sockets to implement this also will later enable us to have a lot more functionality for which data sources we can use for Enclaves 

## Data Reader

### Loggers

we have added logger form log4j, being aware of its issues with the vulnerability

## Oracles

## Data Providers 

### How to represent data constraints in the system?

We have used CNF form to allow an arbitrary comparison for a single cell of a contract function output. Yet we also need to extend this to comparing all function outputs in general as well as for different contracts.
We achive the latter as a simplification - we need all contracts. Here wehave a tradeoff between the functionality and usability and type safeness. We could enforse stricter constraints with less functionality. But instead we have decided to allow more fluid verifications

## Blockchain of Time

When producing the epoch list, we could use either externally available data, such as Eth block hash. Which guarantees, that the data becomes available only every 15 seconds. Alternitively we can trust nodes to maintain their own counters. Which can be prone to issues of nodes going out of sync and over time speeding block production. This is a very dangerous behaviour which we do not want to allow. This would mean that nodes that move epochs faster will benefit more. Thus we will reserve to using block hashes that we know appear only every fix period of seconds. Alternative is a VDFs or anything else

## Out of scope of this implemintation

### Missinformation

The node who shares the secret might share the different secret to the participants, we could use https://eprint.iacr.org/2021/339.pdf to control it 