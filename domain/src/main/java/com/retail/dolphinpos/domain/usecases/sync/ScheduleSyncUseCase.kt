package com.retail.dolphinpos.domain.usecases.sync

import android.content.Context

/**
 * Use case for scheduling POS sync work
 * This abstracts the WorkManager scheduling logic
 */
interface ScheduleSyncUseCase {
    fun scheduleSync(context: Context)
}

