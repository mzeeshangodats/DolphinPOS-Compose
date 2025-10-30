package com.retail.dolphinpos.data.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.retail.dolphinpos.data.repositories.order.PendingOrderRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class OrderSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val pendingOrderRepository: PendingOrderRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val unsyncedOrders = pendingOrderRepository.getUnsyncedOrders()
            
            Log.d("OrderSyncWorker", "Found ${unsyncedOrders.size} unsynced orders")
            
            for (order in unsyncedOrders) {
                try {
                    pendingOrderRepository.syncOrderToServer(order).onSuccess {
                        Log.d("OrderSyncWorker", "Successfully synced order ${order.orderNo}")
                    }.onFailure { e ->
                        Log.e("OrderSyncWorker", "Failed to sync order ${order.orderNo}: ${e.message}")
                    }
                } catch (e: Exception) {
                    Log.e("OrderSyncWorker", "Error syncing order ${order.orderNo}: ${e.message}")
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("OrderSyncWorker", "Error in doWork: ${e.message}")
            Result.retry()
        }
    }
}

