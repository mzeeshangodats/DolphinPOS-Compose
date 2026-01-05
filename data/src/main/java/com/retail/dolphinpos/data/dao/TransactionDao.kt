package com.retail.dolphinpos.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.retail.dolphinpos.data.entities.transaction.PaymentMethod
import com.retail.dolphinpos.data.entities.transaction.TransactionEntity

/**
 * Transaction DAO for offline-first transaction storage
 * 
 * DUPLICATE PREVENTION STRATEGY:
 * - invoice_no is the PRIMARY KEY (globally unique identifier)
 * - All insert operations use OnConflictStrategy.REPLACE (UPSERT behavior)
 * - When a transaction with the same invoice_no is inserted, it REPLACES the existing record
 * - This ensures:
 *   1. Multiple API calls with the same transaction don't create duplicates
 *   2. Local transactions are updated when synced from server
 *   3. Server transactions update local records when fetched
 * 
 * UPSERT BEHAVIOR:
 * - INSERT OR REPLACE: If invoice_no exists, update the record; otherwise, insert new record
 * - Works for both single and batch operations
 * - Safe to call multiple times with the same transaction data
 */
@Dao
interface TransactionDao {

    /**
     * UPSERT: Insert transaction or replace if invoice_no already exists
     * 
     * DUPLICATE PREVENTION: Uses invoice_no as PRIMARY KEY
     * - If invoice_no exists: Updates the existing record
     * - If invoice_no doesn't exist: Inserts new record
     * 
     * Safe to call multiple times with the same invoice_no
     * 
     * @return The rowId (SQLite internal ID, not used for lookups - use invoice_no instead)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    /**
     * UPSERT: Insert multiple transactions or replace if invoice_no already exists
     * 
     * DUPLICATE PREVENTION: Each transaction uses invoice_no as PRIMARY KEY
     * - Transactions with existing invoice_no are updated
     * - Transactions with new invoice_no are inserted
     * 
     * Safe to call multiple times with overlapping transaction lists
     * Used when fetching transactions from API (TransactionActivityScreen)
     * 
     * @return List of rowIds (SQLite internal IDs, not used for lookups - use invoice_no instead)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long>

    @Query("SELECT * FROM transactions ORDER BY created_at DESC")
    suspend fun getAllTransactions(): List<TransactionEntity>

    /**
     * Get transaction by invoice_no (PRIMARY KEY)
     * 
     * Uses invoice_no as the lookup key (PRIMARY KEY lookup is fastest)
     */
    @Query("SELECT * FROM transactions WHERE invoice_no = :invoiceNo")
    suspend fun getTransactionByInvoiceNo(invoiceNo: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE order_no = :orderNo ORDER BY created_at DESC")
    suspend fun getTransactionsByOrderNo(orderNo: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE order_id = :orderId ORDER BY created_at DESC")
    suspend fun getTransactionsByOrderId(orderId: Int): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE store_id = :storeId ORDER BY created_at DESC")
    suspend fun getTransactionsByStoreId(storeId: Int): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE location_id = :locationId ORDER BY created_at DESC")
    suspend fun getTransactionsByLocationId(locationId: Int): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE user_id = :userId ORDER BY created_at DESC")
    suspend fun getTransactionsByUserId(userId: Int): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE batch_no = :batchNo ORDER BY created_at DESC")
    suspend fun getTransactionsByBatchNo(batchNo: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE status = :status ORDER BY created_at DESC")
    suspend fun getTransactionsByStatus(status: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE payment_method = :paymentMethod ORDER BY created_at DESC")
    suspend fun getTransactionsByPaymentMethod(paymentMethod: PaymentMethod): List<TransactionEntity>

    /**
     * Update transaction by invoice_no (PRIMARY KEY)
     * 
     * Note: Prefer using insertTransaction() with OnConflictStrategy.REPLACE for UPSERT behavior
     */
    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    /**
     * Delete transaction by invoice_no (PRIMARY KEY)
     */
    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    /**
     * Delete transaction by invoice_no (PRIMARY KEY)
     * 
     * @param invoiceNo The invoice number (PRIMARY KEY) of the transaction to delete
     */
    @Query("DELETE FROM transactions WHERE invoice_no = :invoiceNo")
    suspend fun deleteTransactionByInvoiceNo(invoiceNo: String)

    @Query("DELETE FROM transactions WHERE order_no = :orderNo")
    suspend fun deleteTransactionsByOrderNo(orderNo: String)
}
