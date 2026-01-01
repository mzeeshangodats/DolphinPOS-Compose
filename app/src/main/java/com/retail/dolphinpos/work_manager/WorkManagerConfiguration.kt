package com.retail.dolphinpos.work_manager

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

object WorkManagerConfiguration {

    fun initializeWorkManager(context: Context) {
        // Get Hilt entry point to access HiltWorkerFactory
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WorkManagerEntryPoint::class.java
        )

        val configuration = Configuration.Builder()
            .setWorkerFactory(entryPoint.workerFactory())
            .build()

        WorkManager.initialize(context, configuration)
        
        // Enqueue periodic sync work when internet is available
        enqueueOrderSyncWork(context)
        enqueueCustomerSyncWork(context)
        enqueueProductSyncWork(context)
        enqueueTimeSlotSyncWork(context)
        // Note: PosSyncWorker is scheduled via SyncScheduler when needed (not periodic)
        // It uses OneTimeWorkRequest with NetworkType.CONNECTED constraint
    }

    private fun enqueueOrderSyncWork(context: Context) {
        enqueuePeriodicSyncWork<OrderSyncWorker>(
            context = context,
            tag = "ORDER_SYNC"
        )
    }

    private fun enqueueCustomerSyncWork(context: Context) {
        enqueuePeriodicSyncWork<CustomerSyncWorker>(
            context = context,
            tag = "CUSTOMER_SYNC"
        )
    }

    private fun enqueueProductSyncWork(context: Context) {
        enqueuePeriodicSyncWork<ProductSyncWorker>(
            context = context,
            tag = "PRODUCT_SYNC"
        )
    }

    private fun enqueueTimeSlotSyncWork(context: Context) {
        enqueuePeriodicSyncWork<TimeSlotSyncWorker>(
            context = context,
            tag = "TIME_SLOT_SYNC"
        )
    }

    /**
     * Generalizes periodic sync work enqueueing for all sync workers
     * @param context Application context
     * @param tag Unique tag for the work request
     * @param repeatInterval Repeat interval (default: 15 minutes)
     * @param flexInterval Flex interval (default: 5 minutes)
     */
    private inline fun <reified T : CoroutineWorker> enqueuePeriodicSyncWork(
        context: Context,
        tag: String,
        repeatInterval: Long = 15,
        repeatIntervalUnit: TimeUnit = TimeUnit.MINUTES,
        flexInterval: Long = 5,
        flexIntervalUnit: TimeUnit = TimeUnit.MINUTES
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<T>(
            repeatInterval, repeatIntervalUnit,
            flexInterval, flexIntervalUnit
        )
            .setConstraints(constraints)
            .addTag(tag)
            .build()

        WorkManager.getInstance(context).enqueue(syncRequest)
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkManagerEntryPoint {
    fun workerFactory(): HiltWorkerFactory
}

