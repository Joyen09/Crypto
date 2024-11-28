# ScroogeCoin Transaction Handler

## Project Overview
This project implements a transaction handler for ScroogeCoin, a simplified version of Bitcoin's transaction model. It includes mechanisms for validating transactions and handling transaction fees in a cryptocurrency system.

## Features
- Transaction validation based on UTXO (Unspent Transaction Output) model
- Double-spending prevention
- Digital signature verification
- Transaction fee calculation
- Maximum fee optimization algorithm

## Core Components
1. **TxHandler**: Basic transaction validation and processing
   - Validates individual transactions
   - Handles multiple transactions in epochs
   - Maintains UTXO pool

2. **MaxFeeTxHandler**: Extended handler with fee optimization
   - Inherits basic validation from TxHandler
   - Implements dynamic programming for maximum fee calculation
   - Optimizes transaction selection based on fees

## Technical Details
- Language: Java
- Key Algorithms:
  - UTXO-based transaction validation
  - Digital signature verification using SHA256withRSA
  - Dynamic programming for fee optimization

## Requirements
- Java Development Kit (JDK) 8 or higher
- Basic understanding of cryptocurrency transactions
- Knowledge of UTXO model

## Usage
```java
// Create a new transaction handler
UTXOPool utxoPool = new UTXOPool();
TxHandler txHandler = new TxHandler(utxoPool);

// Validate a single transaction
boolean isValid = txHandler.isValidTx(transaction);

// Process multiple transactions
Transaction[] validTxs = txHandler.handleTxs(possibleTxs);

// For maximum fee optimization
MaxFeeTxHandler maxFeeTxHandler = new MaxFeeTxHandler(utxoPool);
Transaction[] optimalTxs = maxFeeTxHandler.handleTxs(possibleTxs);
```

## Implementation Details
1. **Transaction Validation Criteria**:
   - All outputs claimed by the transaction are in the current UTXO pool
   - The signatures on each input of the transaction are valid
   - No UTXO is claimed multiple times
   - All output values are non-negative
   - The sum of input values is greater than or equal to the sum of output values

2. **Fee Optimization**:
   - Uses dynamic programming to find optimal transaction set
   - Considers transaction dependencies
   - Maximizes total transaction fees while maintaining validity

## Contributing
Feel free to submit issues and enhancement requests.

## License
This project is part of a cryptocurrency course assignment. Please check with your institution regarding sharing and usage rights.

## Acknowledgments
- Based on the Bitcoin transaction model
- Developed as part of cryptocurrency technology education
