package com.retail.dolphinpos.work_manager

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.data.service.ApiService
import com.retail.dolphinpos.data.util.safeApiCallResult
import com.retail.dolphinpos.domain.model.auth.clock_in_out.ClockInOutRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TimeSlotSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val userDao: UserDao,
    private val apiService: ApiService
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val unsyncedTimeSlots = userDao.getPendingTimeSlots()
            
            Log.d("TimeSlotSyncWorker", "Found ${unsyncedTimeSlots.size} unsynced time slots")
            
            for (timeSlot in unsyncedTimeSlots) {
                try {
                    val request = ClockInOutRequest(
                        slug = timeSlot.slug,
                        storeId = timeSlot.storeId,
                        time = timeSlot.time,
                        userId = timeSlot.userId
                    )
                    
                    val result = safeApiCallResult(
                        apiCall = { apiService.clockInOut(request) },
                        defaultMessage = "Clock In/Out sync failed",
                        messageExtractor = { errorResponse -> errorResponse.message }
                    )
                    
                    result.onSuccess {
                        // Mark as synced if successful
                        userDao.markTimeSlotSynced(timeSlot.id)
                        Log.d("TimeSlotSyncWorker", "Successfully synced time slot ${timeSlot.id} (${timeSlot.slug})")
                    }.onFailure { e ->
                        Log.e("TimeSlotSyncWorker", "Failed to sync time slot ${timeSlot.id}: ${e.message}")
                        // Don't mark as synced, will retry in next cycle
                    }
                } catch (e: Exception) {
                    Log.e("TimeSlotSyncWorker", "Error syncing time slot ${timeSlot.id}: ${e.message}")
                    // Don't mark as synced, will retry in next cycle
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("TimeSlotSyncWorker", "Error in doWork: ${e.message}")
            // Retry if there's a general error
            Result.retry()
        }
    }
}

