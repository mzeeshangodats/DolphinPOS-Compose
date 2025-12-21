package com.retail.dolphinpos.data.repositories.batch

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkContinuation
import androidx.work.WorkManager
import com.retail.dolphinpos.data.dao.BatchDao
import com.retail.dolphinpos.data.entities.user.BatchEntity
import com.retail.dolphinpos.data.entities.user.BatchLifecycleStatus
import com.retail.dolphinpos.data.entities.user.BatchSyncStatus
import com.retail.dolphinpos.data.mapper.UserMapper
import com.retail.dolphinpos.data.workers.BatchSyncCoordinatorWorker
import com.retail.dolphinpos.data.workers.BatchSyncWorker
import com.retail.dolphinpos.domain.model.auth.batch.BatchWithSyncStatus
import com.retail.dolphinpos.domain.repositories.batch.BatchRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of BatchRepository.
 * 
 * CRITICAL DESIGN DECISIONS:
 * - All batch operations work offline immediately
 * - Batch sync happens asynchronously via WorkManager
 * - Uses UUID for batchId to ensure uniqueness and idempotent retries
 * - Only one active batch can exist per register at a time
 */
@Singleton
class BatchRepositoryImpl @Inject constructor(
    private val batchDao: BatchDao,
    private val workManager: WorkManager,
    private val context: Context
) : BatchRepository {
    
    override suspend fun getActiveBatch(registerId: Int): BatchWithSyncStatus? {
        return batchDao.getActiveBatch(registerId)?.let { 
            UserMapper.toBatchWithSyncStatus(it)
        }
    }
    
    /**
     * Starts a new batch locally. This is an OFFLINE-FIRST operation.
     * 
     * CRITICAL: This method does NOT make any network calls.
     * - Batch is created immediately in local database
     * - WorkManager is scheduled for async sync (does not block)
     * - Works completely offline - network status is irrelevant
     * 
     * If this method throws a network-related exception, it's a bug.
     */
    override suspend fun startBatch(
        userId: Int,
        storeId: Int,
        registerId: Int,
        locationId: Int,
        startingCashAmount: Double,
        batchNo: String?
    ): BatchWithSyncStatus {
        // Generate batch UUID locally (single source of truth)
        val batchId = UUID.randomUUID().toString()
        
        // Generate batch number if not provided
        val finalBatchNo = batchNo ?: generateBatchNumber(registerId)
        
        // Close previous active batch if exists
        val previousBatch = batchDao.getActiveBatch(registerId)
        if (previousBatch != null) {
            // Close previous batch (assuming it should be closed with current cash)
            // In production, you might want to pass closing cash amount as parameter
            val closingCashAmount = startingCashAmount // Default to same as starting cash
            batchDao.closeBatch(
                batchId = previousBatch.batchId,
                closedAt = System.currentTimeMillis(),
                closingCashAmount = closingCashAmount
            )
            // Note: closeBatch() automatically updates syncStatus to CLOSE_PENDING if batch was START_SYNCED
        }
        
        // Create new batch with START_PENDING status
        // This ensures it works offline immediately
        val newBatch = BatchEntity(
            batchId = batchId,
            batchNo = finalBatchNo,
            userId = userId,
            storeId = storeId,
            registerId = registerId,
            locationId = locationId,
            startingCashAmount = startingCashAmount,
            openedAt = System.currentTimeMillis(),
            lifecycleStatus = BatchLifecycleStatus.OPEN,
            syncStatus = BatchSyncStatus.START_PENDING
        )
        
        // Insert batch locally (this is the single source of truth)
        // This is a pure database operation - no network involved
        batchDao.insertBatch(newBatch)
        
        // Schedule unified batch and order sync
        // BatchSyncWorker will handle batch start sync, then OrderSyncWorker will sync orders
        scheduleBatchAndOrderSync()
        
        // Convert to domain model before returning
        return UserMapper.toBatchWithSyncStatus(newBatch)
    }
    
    override suspend fun closeBatch(
        batchId: String,
        closingCashAmount: Double
    ): Boolean {
        val batch = batchDao.getBatchById(batchId) ?: return false
        
        // Check if batch is already closed (lifecycle status)
        if (batch.lifecycleStatus == BatchLifecycleStatus.CLOSED) {
            return false
        }
        
        // Close the batch locally (offline-first)
        // This updates lifecycleStatus to CLOSED and syncStatus to CLOSE_PENDING if batch was START_SYNCED
        batchDao.closeBatch(
            batchId = batchId,
            closedAt = System.currentTimeMillis(),
            closingCashAmount = closingCashAmount
        )
        
        // Schedule unified batch and order sync
        // BatchSyncWorker will handle batch close sync, then OrderSyncWorker will sync orders
        scheduleBatchAndOrderSync()
        
        return true
    }
    
    override suspend fun getBatchById(batchId: String): BatchWithSyncStatus? {
        return batchDao.getBatchById(batchId)?.let {
            UserMapper.toBatchWithSyncStatus(it)
        }
    }
    
    override suspend fun getAllBatchesByRegister(registerId: Int): List<BatchWithSyncStatus> {
        return batchDao.getAllBatchesByRegister(registerId).map {
            UserMapper.toBatchWithSyncStatus(it)
        }
    }
    
    override suspend fun hasActiveBatch(registerId: Int): Boolean {
        return batchDao.hasActiveBatch(registerId)
    }
    
    /**
     * Schedules unified batch and order sync chain.
     * 
     * CRITICAL: OrderSyncWorker only runs AFTER BatchSyncWorker completes.
     * This ensures orders are never synced before batch start is synced.
     * 
     * Uses WorkManager with network constraint to ensure sync happens
     * when network is available.
     * 
     * This uses the shared scheduling method from BatchSyncCoordinatorWorker
     * to ensure consistent behavior across the app.
     */
    private fun scheduleBatchAndOrderSync() {
        // Use the shared scheduling method from BatchSyncCoordinatorWorker
        // This ensures consistent chaining logic (BatchSyncWorker → OrderSyncWorker)
        BatchSyncCoordinatorWorker.scheduleBatchAndOrderSync(workManager)
    }
    
    /**
     * Generates a batch number locally.
     * Format: REG{registerId}-{timestamp}
     * 
     * In production, server might assign batch number, but we need
     * a local identifier for offline operation.
     */
    private fun generateBatchNumber(registerId: Int): String {
        val timestamp = System.currentTimeMillis()
        return "REG$registerId-$timestamp"
    }
}
