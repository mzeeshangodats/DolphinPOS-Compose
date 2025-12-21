package com.retail.dolphinpos.domain.repositories.batch

import com.retail.dolphinpos.domain.model.auth.batch.BatchSyncStatus
import com.retail.dolphinpos.domain.model.auth.batch.BatchWithSyncStatus

/**
 * Repository interface for batch management operations.
 * 
 * Handles offline-first batch management where:
 * - Batch state is stored locally as single source of truth
 * - Batch sync happens asynchronously via WorkManager
 * - Orders always attach to the currently active local batch
 */
interface BatchRepository {
    
    /**
     * Gets the currently active batch for a register.
     * Returns null if no active batch exists.
     * 
     * This is the single source of truth for which batch orders should attach to.
     */
    suspend fun getActiveBatch(registerId: Int): BatchWithSyncStatus?
    
    /**
     * Starts a new batch locally.
     * 
     * IMPORTANT: This operation:
     * 1. Creates batch immediately in local database (works offline)
     * 2. Closes previous active batch if exists
     * 3. Schedules BatchStartWorker to sync to backend
     * 4. Returns the new batch (with UUID)
     * 
     * @param userId User ID starting the batch
     * @param storeId Store ID
     * @param registerId Register ID
     * @param locationId Location ID
     * @param startingCashAmount Starting cash amount
     * @param batchNo Optional batch number (if not provided, will be generated)
     * 
     * @return The created BatchWithSyncStatus with UUID batchId
     */
    suspend fun startBatch(
        userId: Int,
        storeId: Int,
        registerId: Int,
        locationId: Int,
        startingCashAmount: Double,
        batchNo: String? = null
    ): BatchWithSyncStatus
    
    /**
     * Closes the active batch.
     * 
     * @param batchId UUID of the batch to close
     * @param closingCashAmount Closing cash amount
     * 
     * @return True if batch was closed successfully, false if batch not found or already closed
     */
    suspend fun closeBatch(
        batchId: String,
        closingCashAmount: Double
    ): Boolean
    
    /**
     * Gets batch by UUID
     */
    suspend fun getBatchById(batchId: String): BatchWithSyncStatus?
    
    /**
     * Gets all batches for a register
     */
    suspend fun getAllBatchesByRegister(registerId: Int): List<BatchWithSyncStatus>
    
    /**
     * Checks if there's an active batch for the register
     */
    suspend fun hasActiveBatch(registerId: Int): Boolean
}
