package com.retail.dolphinpos.data.work

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
        enqueueBatchSyncWork(context)
        enqueueOrderSyncWork(context)
        enqueueRegisterVerificationWork(context)
        enqueueTimeSlotSyncWork(context)
    }

    private fun enqueueBatchSyncWork(context: Context) {
        enqueuePeriodicSyncWork<BatchSyncWorker>(
            context = context,
            tag = "BATCH_SYNC"
        )
    }

    private fun enqueueOrderSyncWork(context: Context) {
        enqueuePeriodicSyncWork<OrderSyncWorker>(
            context = context,
            tag = "ORDER_SYNC"
        )
    }

    private fun enqueueRegisterVerificationWork(context: Context) {
        enqueuePeriodicSyncWork<RegisterVerificationWorker>(
            context = context,
            tag = "REGISTER_VERIFICATION"
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

