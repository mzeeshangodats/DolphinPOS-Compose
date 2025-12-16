package com.retail.dolphinpos.work_manager

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.retail.dolphinpos.domain.repositories.home.HomeRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class CustomerSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val homeRepository: HomeRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Get all unsynced customer IDs
            val unsyncedCustomerIds = homeRepository.getUnsyncedCustomers()
            
            Log.d("CustomerSyncWorker", "Found ${unsyncedCustomerIds.size} unsynced customers")
            
            for (customerId in unsyncedCustomerIds) {
                try {
                    // Sync customer to server
                    // On success, HomeRepository automatically updates isSynced = true
                    homeRepository.syncCustomerToServer(customerId).onSuccess { response ->
                        Log.d("CustomerSyncWorker", "Successfully synced customer ID $customerId. Response: ${response.message}")
                    }.onFailure { e ->
                        Log.e("CustomerSyncWorker", "Failed to sync customer ID $customerId: ${e.message}")
                    }
                } catch (e: Exception) {
                    Log.e("CustomerSyncWorker", "Error syncing customer ID $customerId: ${e.message}")
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("CustomerSyncWorker", "Error in doWork: ${e.message}")
            Result.retry()
        }
    }
}

