package com.retail.dolphinpos.work_manager

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.retail.dolphinpos.domain.repositories.product.ProductRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ProductSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val productRepository: ProductRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Get all unsynced product IDs
            val unsyncedProductIds = productRepository.getUnsyncedProducts()
            
            Log.d("ProductSyncWorker", "Found ${unsyncedProductIds.size} unsynced products")
            
            for (productId in unsyncedProductIds) {
                try {
                    // Sync product to server
                    // On success, ProductRepository automatically updates isSynced = true
                    productRepository.syncProductToServer(productId).onSuccess { serverProductId ->
                        Log.d("ProductSyncWorker", "Successfully synced product ID $productId. Server ID: $serverProductId")
                    }.onFailure { e ->
                        Log.e("ProductSyncWorker", "Failed to sync product ID $productId: ${e.message}")
                    }
                } catch (e: Exception) {
                    Log.e("ProductSyncWorker", "Error syncing product ID $productId: ${e.message}")
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("ProductSyncWorker", "Error in doWork: ${e.message}")
            Result.retry()
        }
    }
}

