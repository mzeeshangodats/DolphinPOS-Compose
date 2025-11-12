package com.retail.dolphinpos.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.retail.dolphinpos.data.entities.transaction.CreateOrderTransactionEntity
import com.retail.dolphinpos.data.entities.transaction.PaymentMethod

@Dao
interface CreateOrderTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: CreateOrderTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<CreateOrderTransactionEntity>): List<Long>

    @Query("SELECT * FROM create_order_transaction ORDER BY created_at DESC")
    suspend fun getAllTransactions(): List<CreateOrderTransactionEntity>

    @Query("SELECT * FROM create_order_transaction WHERE id = :transactionId")
    suspend fun getTransactionById(transactionId: Long): CreateOrderTransactionEntity?

    @Query("SELECT * FROM create_order_transaction WHERE order_no = :orderNo ORDER BY created_at DESC")
    suspend fun getTransactionsByOrderNo(orderNo: String): List<CreateOrderTransactionEntity>

    @Query("SELECT * FROM create_order_transaction WHERE store_id = :storeId ORDER BY created_at DESC")
    suspend fun getTransactionsByStoreId(storeId: Int): List<CreateOrderTransactionEntity>

    @Query("SELECT * FROM create_order_transaction WHERE location_id = :locationId ORDER BY created_at DESC")
    suspend fun getTransactionsByLocationId(locationId: Int): List<CreateOrderTransactionEntity>

    @Query("SELECT * FROM create_order_transaction WHERE user_id = :userId ORDER BY created_at DESC")
    suspend fun getTransactionsByUserId(userId: Int): List<CreateOrderTransactionEntity>

    @Query("SELECT * FROM create_order_transaction WHERE batchNo = :batchNo ORDER BY created_at DESC")
    suspend fun getTransactionsByBatchNo(batchNo: String): List<CreateOrderTransactionEntity>

    @Query("SELECT * FROM create_order_transaction WHERE invoice_no = :invoiceNo")
    suspend fun getTransactionByInvoiceNo(invoiceNo: String): CreateOrderTransactionEntity?

    @Query("SELECT * FROM create_order_transaction WHERE status = :status ORDER BY created_at DESC")
    suspend fun getTransactionsByStatus(status: String): List<CreateOrderTransactionEntity>

    @Query("SELECT * FROM create_order_transaction WHERE payment_method = :paymentMethod ORDER BY created_at DESC")
    suspend fun getTransactionsByPaymentMethod(paymentMethod: PaymentMethod): List<CreateOrderTransactionEntity>

    @Update
    suspend fun updateTransaction(transaction: CreateOrderTransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: CreateOrderTransactionEntity)

    @Query("DELETE FROM create_order_transaction WHERE id = :transactionId")
    suspend fun deleteTransactionById(transactionId: Long)

    @Query("DELETE FROM create_order_transaction WHERE order_no = :orderNo")
    suspend fun deleteTransactionsByOrderNo(orderNo: String)
}

