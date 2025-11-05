package com.retail.dolphinpos.data.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.data.service.ApiService
import com.retail.dolphinpos.domain.model.auth.select_registers.request.VerifyRegisterRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class RegisterVerificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val apiService: ApiService,
    private val preferenceManager: PreferenceManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Check if user is logged in and has an occupied register
            if (!preferenceManager.isLogin() || !preferenceManager.getRegister()) {
                Log.d("RegisterVerificationWorker", "User not logged in or register not occupied")
                return Result.success()
            }

            val storeId = preferenceManager.getStoreID()
            val locationId = preferenceManager.getOccupiedLocationID()
            val storeRegisterId = preferenceManager.getOccupiedRegisterID()

            if (storeId == 0 || locationId == 0 || storeRegisterId == 0) {
                Log.d("RegisterVerificationWorker", "Invalid register information")
                return Result.success()
            }

            // Verify register status
            val verifyRequest = VerifyRegisterRequest(
                storeId = storeId,
                locationId = locationId,
                storeRegisterId = storeRegisterId
            )

            val response = apiService.verifyStoreRegister(verifyRequest)

            // Check if status is "occupied"
            val status = response.data.status.lowercase()
            if (status != "occupied") {
                Log.w("RegisterVerificationWorker", "Register status is not occupied: $status")

                preferenceManager.setRegister(false)
                
                // Set flag to navigate to PinCodeScreen
                preferenceManager.setForceRegisterSelection(true)
                
                return Result.success()
            }

            Log.d("RegisterVerificationWorker", "Register status verified: $status")
            Result.success()
        } catch (e: Exception) {
            Log.e("RegisterVerificationWorker", "Error verifying register: ${e.message}")
            // Don't retry on network errors - will check again in next cycle
            Result.success()
        }
    }
}

