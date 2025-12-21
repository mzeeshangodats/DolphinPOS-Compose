package com.retail.dolphinpos.data.workers

import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.retail.dolphinpos.data.dao.BatchDao
import com.retail.dolphinpos.data.dao.OrderDao
import com.retail.dolphinpos.data.entities.user.BatchSyncStatus
import com.retail.dolphinpos.data.repositories.order.OrderRepositoryImpl
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import retrofit2.HttpException

/**
 * WorkManager worker that syncs orders to the backend.
 * 
 * CRITICAL OPERATIONS:
 * 1. Only syncs orders for batches that are SYNCED (batch exists on backend)
 * 2. Skips orders for batches with SYNC_PENDING or SYNC_FAILED status
 * 3. Updates order isSynced flag after successful sync
 * 
 * IMPORTANT: This worker should be chained AFTER BatchSyncWorker.
 * Orders must never be synced before their batch exists on backend.
 */
@HiltWorker
class OrderSyncWorker @AssistedInject constructor(
    @Assisted context: android.content.Context,
    @Assisted params: WorkerParameters,
    private val orderDao: OrderDao,
    private val batchDao: BatchDao,
    private val orderRepository: OrderRepositoryImpl
) : CoroutineWorker(context, params) {
    
    companion object {
        const val TAG = "OrderSyncWorker"
        const val BATCH_ID_KEY = "batch_id"
    }
    
    override suspend fun doWork(): Result {
        // Get batchId from input data (if provided from chained worker)
        // When chained after BatchSyncWorker, WorkManager merges output data
        val batchId = inputData.getString(BATCH_ID_KEY)
        
        return try {
            if (batchId != null && batchId.isNotEmpty()) {
                // Sync orders for a specific batch (from BatchSyncWorker chain)
                syncOrdersForBatch(batchId)
            } else {
                // Sync all orders for batches eligible for order sync
                // Only syncs orders for batches with START_SYNCED, CLOSE_PENDING, or CLOSE_SYNCED
                syncAllPendingOrders()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync orders: ${e.message}", e)
            Result.retry()
        }
    }
    
    /**
     * Syncs orders for a specific batch.
     * Only processes orders if batch start is synced (START_SYNCED or later, but not FAILED).
     */
    private suspend fun syncOrdersForBatch(batchId: String): Result {
        // Verify batch start is synced before syncing orders
        val batch = batchDao.getBatchById(batchId)
            ?: return Result.failure(workDataOf("error" to "Batch not found: $batchId"))
        
        // Orders can only be synced if batch start is synced
        if (!batch.canSyncOrders()) {
            Log.w(TAG, "Batch $batchId cannot sync orders (syncStatus: ${batch.syncStatus}), skipping order sync")
            return Result.success(workDataOf("skipped" to "Batch start not synced"))
        }
        
        // Get unsynced orders for this batch
        val unsyncedOrders = orderDao.getUnsyncedOrdersByBatchId(batchId)
        
        if (unsyncedOrders.isEmpty()) {
            Log.d(TAG, "No unsynced orders for batch $batchId")
            return Result.success()
        }
        
        Log.d(TAG, "Syncing ${unsyncedOrders.size} orders for batch $batchId")
        
        var successCount = 0
        var failureCount = 0
        
        for (order in unsyncedOrders) {
            try {
                // Use OrderRepository's syncOrderToServer method which handles:
                // 1. Conversion from OrderEntity to CreateOrderRequest
                // 2. API call
                // 3. Updating order as synced
                orderRepository.syncOrderToServer(order).fold(
                    onSuccess = {
                        successCount++
                        Log.d(TAG, "Synced order ${order.orderNumber}")
                    },
                    onFailure = { exception ->
                        failureCount++
                        Log.e(TAG, "Failed to sync order ${order.orderNumber}: ${exception.message}", exception)
                        // Continue with next order (don't fail entire batch)
                        // Orders that fail will be retried on next worker run
                    }
                )
            } catch (e: Exception) {
                failureCount++
                Log.e(TAG, "Exception syncing order ${order.orderNumber}: ${e.message}", e)
            }
        }
        
        Log.d(TAG, "Order sync complete: $successCount succeeded, $failureCount failed")
        
        // Return success even if some orders failed
        // Failed orders will be retried on next worker run
        return Result.success(
            workDataOf(
                "success_count" to successCount,
                "failure_count" to failureCount
            )
        )
    }
    
    /**
     * Syncs all unsynced orders for batches eligible for order sync.
     * Only syncs orders for batches with START_SYNCED, CLOSE_PENDING, or CLOSE_SYNCED status.
     * Orders are NEVER synced if batch start is not synced.
     */
    private suspend fun syncAllPendingOrders(): Result {
        // Get batches eligible for order sync
        val eligibleBatches = batchDao.getBatchesEligibleForOrderSync()
        val eligibleBatchIds = eligibleBatches.map { it.batchId }.toSet()
        
        // Get all unsynced local orders
        val allUnsyncedOrders = orderDao.getUnsyncedLocalOrders()
        
        // Filter to only include orders for eligible batches
        val unsyncedOrders = allUnsyncedOrders.filter { it.batchId in eligibleBatchIds }
        
        if (unsyncedOrders.isEmpty()) {
            Log.d(TAG, "No unsynced orders for synced batches")
            return Result.success()
        }
        
        Log.d(TAG, "Syncing ${unsyncedOrders.size} orders across all synced batches")
        
        // Sync all orders (they're already filtered to only include SYNCED batches)
        var totalSuccess = 0
        var totalFailure = 0
        
        for (order in unsyncedOrders) {
            try {
                // Use OrderRepository's syncOrderToServer method which handles:
                // 1. Conversion from OrderEntity to CreateOrderRequest
                // 2. API call
                // 3. Updating order as synced
                orderRepository.syncOrderToServer(order).fold(
                    onSuccess = {
                        totalSuccess++
                        Log.d(TAG, "Synced order ${order.orderNumber}")
                    },
                    onFailure = { exception ->
                        totalFailure++
                        Log.e(TAG, "Failed to sync order ${order.orderNumber}: ${exception.message}", exception)
                        // Continue with next order
                    }
                )
            } catch (e: Exception) {
                totalFailure++
                Log.e(TAG, "Exception syncing order ${order.orderNumber}: ${e.message}", e)
            }
        }
        
        Log.d(TAG, "Order sync complete: $totalSuccess succeeded, $totalFailure failed")
        
        return Result.success(
            workDataOf(
                "total_success" to totalSuccess,
                "total_failure" to totalFailure
            )
        )
    }
    
}
