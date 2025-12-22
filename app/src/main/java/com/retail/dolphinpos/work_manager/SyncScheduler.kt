package com.retail.dolphinpos.work_manager

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.retail.dolphinpos.data.work_manager.PosSyncWorker

/**
 * Utility class for scheduling POS sync work
 */
object SyncScheduler {
    
    private const val UNIQUE_WORK_NAME = "pos_sync_worker"
    
    /**
     * Schedule sync work to run immediately when network is available
     * Use ExistingWorkPolicy.KEEP to avoid duplicate work requests
     */
    fun scheduleSyncNow(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<PosSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP, // If work already enqueued, keep it
                syncRequest
            )
    }
}

