package com.retail.dolphinpos.domain.repositories.transaction

import com.retail.dolphinpos.domain.model.transaction.Transaction
import com.retail.dolphinpos.domain.model.transaction.TransactionResponse

interface TransactionRepository {
    suspend fun getTransactionsFromApi(
        storeId: Int,
        locationId: Int? = null,
        page: Int = 1,
        limit: Int = 10
    ): TransactionResponse

    suspend fun getTransactionsFromLocal(storeId: Int): List<Transaction>
    
    suspend fun saveTransactionsToLocal(transactions: List<Transaction>)
    
    suspend fun fetchAndSaveTransactionsFromApi(
        storeId: Int,
        locationId: Int? = null,
        page: Int = 1,
        limit: Int = 10
    ): List<Transaction>
    
    suspend fun saveTransactionToLocal(transaction: Transaction): Long
    
    suspend fun updateTransaction(transaction: Transaction)
    
    suspend fun getUnsyncedTransactions(): List<Transaction>
    
    suspend fun syncTransactionToServer(transaction: Transaction): Result<Unit>
}

