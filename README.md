# Private State Smart Contracts

## About the project

This project has been developed by Artem Grigor as a final work of his degree. The project explores ways of how to add privacy to decentralised applications. 

In particular this directory contains working code for Access Management System that can be integrated with decentralised networks such as Ethereum and will provide access to the data only if specific constraints are fulfilled. Please see the final report for more details on the project or reach out to Artem Grigor at Artem.Grigor@warwick.ac.uk 

## Running the project

Here we provide a guide on how to run our project, and we will walk through a demo of the projects features.  

### Requirements

1. [Docker](https://www.docker.com)
2. [Java 11](https://www.oracle.com/uk/java/technologies/javase/jdk11-archive-downloads.html)
3. [Conclave SDK v1.2.1](https://www.conclave.net) added to the projects directory
4. [Gradle 7](https://docs.gradle.org/current/userguide/installation.html)
5. [IntelliJ IDEA](https://www.jetbrains.com/idea/) is highly recommended
6. Port 8080 free on your machine

### Initial Start Up

#### Enclave

First, we need to start up the Enclave. We will start it in a Mock mode so that the application will be able to run on non SGX CPUs. 

If you want to start it in a secure mode, please refer to the documentation available on Conclave website to configure your environment in a suitable way. 

If you are using IntelliJ IDEA, you can run the **Start Mock Enclave** configuration.

Alternatively, on macOS and Linux execute in a terminal of your choice

            `./gradlew run --args="--sealed.state.file='runtime/conclave-storage'" -q --console=plain `  

On Windows execute
        
            `./gradlew.bat run --args="--sealed.state.file='runtime/conclave-storage'" -q --console=plain `

**NB!** We have not tested that the application works correctly on Windows.  

Once you see `Assessed security level at ... is INSECURE` in your terminal the startup is complete.

#### Oracle

To start an oracle with IntelliJ IDEA, run the **Start Oracle** configuration.

Alternatively, on macOS and Linux execute in a terminal of your choice

            `./gradlew oracle:run --args="'https://rinkeby.infura.io/v3/2366571028a04e72b47a0bbd64001a37' 'http://localhost:8080' 'S:0000000000000000000000000000000000000000000000000000000000000000 PROD:1 SEC:INSECURE' 'ed07ee0c'" -q --console=plain `  

On Windows execute

            `./gradlew.bat oracle:run --args="'https://rinkeby.infura.io/v3/2366571028a04e72b47a0bbd64001a37' 'http://localhost:8080' 'S:0000000000000000000000000000000000000000000000000000000000000000 PROD:1 SEC:INSECURE' 'ed07ee0c'" -q --console=plain `


Please note that here we have provided our private Ethereum RPC, which might be deleted by us in the near future. To make sure this works, replace the RPC in the command with our own. You can create it [here](https://infura.io).  

Once you see `... INFO Submitted block ... to the Enclave` in your terminal the startup is complete.


### Demo Overview


In the report as one of the examples for using our project we explore token gated secret data. That is access to data is allowed only to someone who has a specific token. This is what we have implemented in our prototype. Please follow the instructions bellow to start the application and try it for yourself. 

### Submit Secret Data

To submit secret data, start up the Data Submitter application. 

This can be done by running **ERC20 Data Submitter Interactive** configuration in the IntelliJ IDEA.

Alternatively, on macOS and Linux execute in a terminal of your choice

            `./gradlew client:runERC20Submitter -q --console=plain`  

On Windows execute

            `./gradlew.bat client:runERC20Submitter -q --console=plain`

The command line will then prompt you to input a secret. Provide any string of literals.

Then you will be prompted for an Ethereum Token address. The application currently works on the Ethereum testnet Rinkeby. We recommend using the [`0xc7AD46e0b8a400Bb3C915120d284AafbA8fc4735`](https://rinkeby.etherscan.io/token/0xc7AD46e0b8a400Bb3C915120d284AafbA8fc4735?a=0x424706da31e53a4859e560db7ed908d6534973c0) address.

You will get back a response saying `... INFO Successfully submitted secret data to enclave. Data Reference ID is ...`. Copy the data reference id and save it. We will use it later.

### Request Secret Data

To request secret data, start up the Data Requester application.

This can be done by running **Start Data  Requester ERC20 Interactive** configuration in the IntelliJ IDEA.

Alternatively, on macOS and Linux execute in a terminal of your choice

            `./gradlew client:runERC20Requestor -q --console=plain`  

On Windows execute

            `./gradlew.bat client:runERC20Requestor -q --console=plain`
`
The command line will then prompt you to input a data reference id from earlier. Paste what you have received in the Data Submitter application.

Then you will be prompted for an Ethereum private key. Please input the private key that has the token which you have specified as a Data Submitter. For our recommended token use the `e5e426a40532c54dedb5bccd0a2493688778d32a457979124c035be9786024e8` PK which corresponds to the [`0x424706Da31E53a4859e560DB7ed908d6534973C0`](https://rinkeby.etherscan.io/address/0x424706Da31E53a4859e560DB7ed908d6534973C0) address. 

As a response you should see the secret data you have originally provided. In case the system does not work, please try to restart all components. 

You can additionally examine the output of both Enclave and Oracle to see what is going on inside them.