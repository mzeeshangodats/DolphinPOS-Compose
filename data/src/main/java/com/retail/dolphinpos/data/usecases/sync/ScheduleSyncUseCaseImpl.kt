package com.retail.dolphinpos.data.usecases.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.retail.dolphinpos.domain.usecases.sync.ScheduleSyncUseCase
import com.retail.dolphinpos.data.work_manager.PosSyncWorker
import javax.inject.Inject

class ScheduleSyncUseCaseImpl @Inject constructor() : ScheduleSyncUseCase {
    
    override fun scheduleSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<PosSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "pos_sync_worker",
                ExistingWorkPolicy.KEEP,
                syncRequest
            )
    }
}

