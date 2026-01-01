package com.retail.dolphinpos.work_manager

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.retail.dolphinpos.data.repositories.order.OrderRepositoryImpl
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class OrderSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val orderRepository: OrderRepositoryImpl
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Get all unsynced local orders (orderSource = 'local' AND isSynced = false)
            val unsyncedOrders = orderRepository.getUnsyncedLocalOrders()
            
            Log.d("OrderSyncWorker", "Found ${unsyncedOrders.size} unsynced orders")
            
            for (order in unsyncedOrders) {
                try {
                    // Double-check order is not already synced (to prevent duplicate API calls)
                    if (order.isSynced || order.syncStatus == com.retail.dolphinpos.data.entities.order.OrderSyncStatus.SYNCED) {
                        Log.d("OrderSyncWorker", "Order ${order.orderNumber} is already synced, skipping")
                        continue
                    }
                    
                    // Sync order to server
                    // On success, OrderRepository automatically updates isSynced = true and status = "completed"
                    orderRepository.syncOrderToServer(order).onSuccess { response ->
                        Log.d("OrderSyncWorker", "Successfully synced order ${order.orderNumber}. Response: ${response.message}")
                    }.onFailure { e ->
                        Log.e("OrderSyncWorker", "Failed to sync order ${order.orderNumber}: ${e.message}")
                    }
                } catch (e: Exception) {
                    Log.e("OrderSyncWorker", "Error syncing order ${order.orderNumber}: ${e.message}")
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("OrderSyncWorker", "Error in doWork: ${e.message}")
            Result.retry()
        }
    }
}

