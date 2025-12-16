package com.retail.dolphinpos.data.repositories.auth

import com.google.gson.Gson
import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.data.mapper.UserMapper
import com.retail.dolphinpos.data.service.ApiService
import com.retail.dolphinpos.data.util.safeApiCallResult
import com.retail.dolphinpos.data.util.getErrorMessage
import com.retail.dolphinpos.data.util.parseErrorResponse
import com.retail.dolphinpos.domain.model.auth.batch.Batch
import com.retail.dolphinpos.domain.model.auth.batch.BatchDetails
import com.retail.dolphinpos.domain.model.auth.cash_denomination.BatchErrorResponse
import com.retail.dolphinpos.domain.model.auth.cash_denomination.BatchOpenRequest
import com.retail.dolphinpos.domain.model.auth.cash_denomination.BatchOpenResponse
import com.retail.dolphinpos.domain.repositories.auth.CashDenominationRepository
import retrofit2.HttpException

class CashDenominationRepositoryImpl(
    private val userDao: UserDao,
    private val apiService: ApiService
) : CashDenominationRepository {

    override suspend fun insertBatchIntoLocalDB(batch: Batch) {
        try {
            // First, check if any batch exists and delete all existing batches
            val existingBatches = userDao.getAllBatches()
            if (existingBatches.isNotEmpty()) {
                userDao.deleteAllBatches()
            }
            
            // Then insert the new batch
            userDao.insertBatchDetails(
                UserMapper.toBatchEntity(
                    batch
                )
            )
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun getBatchDetails(): Batch {
        val batchEntities = userDao.getBatchDetails()
        return UserMapper.toBatchDetails(batchEntities)
    }

    override suspend fun batchOpen(batchOpenRequest: BatchOpenRequest): Result<BatchOpenResponse> {
        return try {
            // Try API call directly to catch HttpException with status code
            Result.success(apiService.batchOpen(batchOpenRequest))
        } catch (e: HttpException) {
            // Parse structured error response
            val errorMessage = try {
                val errorBody = e.response()?.errorBody()?.string()
                if (errorBody != null) {
                    val gson = Gson()
                    val errorResponse: BatchErrorResponse? = 
                        gson.fromJson(errorBody, BatchErrorResponse::class.java)
                    
                    errorResponse?.let { error ->
                        // Prioritize field-specific error messages
                        val fieldErrors = buildString {
                            error.errors?.let { errors ->
                                if (errors.startingCashAmount != null) append("${errors.startingCashAmount}\n")
                                if (errors.batchNo != null) append("${errors.batchNo}\n")
                                if (errors.storeId != null) append("${errors.storeId}\n")
                                if (errors.userId != null) append("${errors.userId}\n")
                                if (errors.locationId != null) append("${errors.locationId}\n")
                                if (errors.storeRegisterId != null) append("${errors.storeRegisterId}\n")
                            }
                        }.trim()
                        
                        // If we have specific field errors, use those; otherwise use the main message
                        if (fieldErrors.isNotEmpty()) {
                            fieldErrors
                        } else {
                            error.message ?: "Batch open failed"
                        }
                    } ?: "Batch open failed"
                } else {
                    "Batch open failed"
                }
            } catch (parseException: Exception) {
                "Batch open failed"
            }
            
            Result.failure(Exception(errorMessage).apply {
                // Preserve the HttpException by setting it as cause
                initCause(e)
            })
        } catch (e: java.io.IOException) {
            // Re-throw IOException so it can be handled by the caller
            throw e
        } catch (e: Exception) {
            // For other exceptions, wrap in Result.failure
            Result.failure(e)
        }
    }

    override suspend fun markBatchAsSynced(batchNo: String) {
        try {
            val batchEntity = userDao.getBatchByBatchNo(batchNo)
            batchEntity?.let {
                val updatedBatch = it.copy(isSynced = true)
                userDao.updateBatch(updatedBatch)
            }
        } catch (e: Exception) {
            throw e
        }
    }

}