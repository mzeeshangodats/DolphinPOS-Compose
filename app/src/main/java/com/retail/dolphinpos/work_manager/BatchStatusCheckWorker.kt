package com.retail.dolphinpos.work_manager

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.domain.model.auth.cash_denomination.BatchCloseRequest
import com.retail.dolphinpos.domain.repositories.report.BatchReportRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BatchStatusCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val batchReportRepository: BatchReportRepository,
    private val preferenceManager: PreferenceManager
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "BatchStatusCheckWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            // Check if user is logged in and batch is open
            if (!preferenceManager.isLogin() || !preferenceManager.isBatchOpen()) {
                Log.d(TAG, "User not logged in or batch is not open")
                return Result.success()
            }

            val batchNo = preferenceManager.getBatchNo()
            if (batchNo.isEmpty()) {
                Log.d(TAG, "No batch number found")
                return Result.success()
            }

            // Check batch status from API
            val batchReport = batchReportRepository.getBatchReport(batchNo)
            val batchStatus = batchReport.data.status?.lowercase()

            Log.d(TAG, "Batch status: $batchStatus for batch: $batchNo")

            // If batch status is closed, call batchClose API
            if (batchStatus == "closed") {
                Log.d(TAG, "Batch status is closed, calling batchClose API")
                
                val userId = preferenceManager.getUserID()
                val storeId = preferenceManager.getStoreID()
                val locationId = preferenceManager.getOccupiedLocationID()
                val closingCashAmount = batchReport.data.closingCashAmount

                val batchCloseRequest = BatchCloseRequest(
                    cashierId = userId,
                    closedBy = userId,
                    closingCashAmount = closingCashAmount,
                    locationId = locationId,
                    orders = emptyList(), // Empty list as per existing implementation
                    paxBatchNo = "", // Empty string as per existing implementation
                    storeId = storeId
                )

                val result = batchReportRepository.batchClose(batchNo, batchCloseRequest)

                result.onSuccess { response ->
                    Log.d(TAG, "Batch closed successfully: ${response.message}")
                    // Update batch status in preferences
                    preferenceManager.setBatchStatus("closed")
                }.onFailure { exception ->
                    Log.e(TAG, "Failed to close batch: ${exception.message}")
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking batch status: ${e.message}", e)
            // Don't retry on errors - will check again in next cycle
            Result.success()
        }
    }
}

