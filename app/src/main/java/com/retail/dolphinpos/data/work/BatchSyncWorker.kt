package com.retail.dolphinpos.data.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.data.mapper.UserMapper
import com.retail.dolphinpos.data.service.ApiService
import com.retail.dolphinpos.domain.model.auth.cash_denomination.BatchOpenRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BatchSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val userDao: UserDao,
    private val apiService: ApiService,
    private val preferenceManager: PreferenceManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Get all unsynced batches
            val unsyncedBatches = userDao.getUnsyncedBatches()
            
            if (unsyncedBatches.isEmpty()) {
                Log.d("BatchSyncWorker", "No unsynced batches found")
                return Result.success()
            }
            
            Log.d("BatchSyncWorker", "Found ${unsyncedBatches.size} unsynced batches")
            
            // Sync each batch
            for (batchEntity in unsyncedBatches) {
                try {
                    val batchOpenRequest = BatchOpenRequest(
                        storeId = batchEntity.storeId ?: 0,
                        cashierId = batchEntity.userId ?: 0,
                        storeRegisterId = batchEntity.registerId,
                        startingCashAmount = batchEntity.startingCashAmount
                    )
                    
                    val response = apiService.batchOpen(batchOpenRequest)
                    
                    if (response.message != null || response.data != null) {
                        // Mark batch as synced
                        val updatedBatch = batchEntity.copy(isSynced = true)
                        userDao.updateBatch(updatedBatch)
                        Log.d("BatchSyncWorker", "Batch ${batchEntity.batchNo} synced successfully")
                    }
                } catch (e: Exception) {
                    Log.e("BatchSyncWorker", "Failed to sync batch ${batchEntity.batchNo}: ${e.message}")
                    // Continue with next batch
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("BatchSyncWorker", "Error in BatchSyncWorker: ${e.message}")
            Result.retry()
        }
    }
}

