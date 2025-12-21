package com.retail.dolphinpos.data.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.retail.dolphinpos.data.dao.BatchDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Coordinator worker that schedules batch and order sync using unified chained flow.
 * 
 * CRITICAL OPERATIONS:
 * 1. Schedules BatchSyncWorker (handles both START and CLOSE sync)
 * 2. Chains OrderSyncWorker after BatchSyncWorker completes
 * 3. Uses unified chained flow: BatchSyncWorker → OrderSyncWorker
 * 
 * This worker is used for:
 * - Periodic sync (every 15 minutes)
 * - Network restored sync
 * - Manual sync triggers
 * 
 * IMPORTANT: Orders are NEVER synced unless batch START is synced.
 * The unified BatchSyncWorker handles all batch sync operations.
 */
@HiltWorker
class BatchSyncCoordinatorWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val batchDao: BatchDao
) : CoroutineWorker(context, params) {
    
    companion object {
        const val TAG = "BatchSyncCoordinatorWorker"
        const val BATCH_ID_KEY = "batch_id"
        
        /**
         * Schedules unified batch and order sync chain.
         * BatchSyncWorker handles both START and CLOSE sync, then OrderSyncWorker syncs orders.
         * Can be called from anywhere to trigger sync.
         */
        fun scheduleBatchAndOrderSync(
            workManager: WorkManager
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            // BatchSyncWorker handles all batch sync operations (no input data needed)
            val batchSyncWork = OneTimeWorkRequestBuilder<BatchSyncWorker>()
                .setConstraints(constraints)
                .build()
            
            // OrderSyncWorker runs after batch sync completes
            val orderSyncWork = OneTimeWorkRequestBuilder<OrderSyncWorker>()
                .setConstraints(constraints)
                .build()
            
            // Chain: BatchSyncWorker → OrderSyncWorker
            workManager.beginUniqueWork(
                "batch_order_sync",
                ExistingWorkPolicy.KEEP, // Keep existing work if already enqueued
                batchSyncWork
            )
                .then(orderSyncWork)
                .enqueue()
        }
    }
    
    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting batch sync coordinator")
            
            // Schedule unified batch and order sync chain
            // BatchSyncWorker will handle all batch sync operations (START and CLOSE)
            // OrderSyncWorker will sync orders after batch sync completes
            val workManager = WorkManager.getInstance(applicationContext)
            scheduleBatchAndOrderSync(workManager)
            
            Log.d(TAG, "Scheduled unified batch and order sync chain")
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in batch sync coordinator: ${e.message}", e)
            // Retry on failure (WorkManager will use exponential backoff)
            Result.retry()
        }
    }
    
}

