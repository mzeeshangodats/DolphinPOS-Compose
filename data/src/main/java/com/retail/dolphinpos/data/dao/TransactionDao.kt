package com.retail.dolphinpos.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.retail.dolphinpos.data.entities.transaction.PaymentMethod
import com.retail.dolphinpos.data.entities.transaction.TransactionEntity

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long>

    @Query("SELECT * FROM transactions ORDER BY created_at DESC")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    suspend fun getTransactionById(transactionId: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE order_no = :orderNo ORDER BY created_at DESC")
    suspend fun getTransactionsByOrderId(orderNo: Int): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE store_id = :storeId ORDER BY created_at DESC")
    suspend fun getTransactionsByStoreId(storeId: Int): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE location_id = :locationId ORDER BY created_at DESC")
    suspend fun getTransactionsByLocationId(locationId: Int): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE user_id = :userId ORDER BY created_at DESC")
    suspend fun getTransactionsByUserId(userId: Int): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE batchNo = :batchNo ORDER BY created_at DESC")
    suspend fun getTransactionsByBatchId(batchNo: Int): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE invoice_no = :invoiceNo")
    suspend fun getTransactionByInvoiceNo(invoiceNo: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE status = :status ORDER BY created_at DESC")
    suspend fun getTransactionsByStatus(status: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE payment_method = :paymentMethod ORDER BY created_at DESC")
    suspend fun getTransactionsByPaymentMethod(paymentMethod: PaymentMethod): List<TransactionEntity>

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransactionById(transactionId: Long)

    @Query("DELETE FROM transactions WHERE order_no = :orderNo")
    suspend fun deleteTransactionsByOrderId(orderNo: Int)
}

