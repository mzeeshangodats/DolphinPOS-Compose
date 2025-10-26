package com.retail.dolphinpos.data.work

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

object BatchSyncWorkerConfiguration {

    fun initializeWorkManager(context: Context) {
        // Get Hilt entry point to access HiltWorkerFactory
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            BatchSyncWorkerEntryPoint::class.java
        )

        val configuration = Configuration.Builder()
            .setWorkerFactory(entryPoint.batchSyncWorkerFactory())
            .build()

        WorkManager.initialize(context, configuration)
        
        // Enqueue periodic sync work when internet is available
        enqueueBatchSyncWork(context)
        enqueueOrderSyncWork(context)
    }

    private fun enqueueBatchSyncWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Use periodic work with shorter interval for faster sync
        val syncRequest = PeriodicWorkRequestBuilder<BatchSyncWorker>(
            15, TimeUnit.MINUTES, // Repeat every 15 minutes
            5, TimeUnit.MINUTES // With a 5-minute flex interval
        )
            .setConstraints(constraints)
            .addTag("BATCH_SYNC")
            .build()

        WorkManager.getInstance(context).enqueue(syncRequest)
    }

    private fun enqueueOrderSyncWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Use periodic work with shorter interval for faster sync
        val syncRequest = PeriodicWorkRequestBuilder<OrderSyncWorker>(
            15, TimeUnit.MINUTES, // Repeat every 15 minutes
            5, TimeUnit.MINUTES // With a 5-minute flex interval
        )
            .setConstraints(constraints)
            .addTag("ORDER_SYNC")
            .build()

        WorkManager.getInstance(context).enqueue(syncRequest)
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BatchSyncWorkerEntryPoint {
    fun batchSyncWorkerFactory(): HiltWorkerFactory
}

