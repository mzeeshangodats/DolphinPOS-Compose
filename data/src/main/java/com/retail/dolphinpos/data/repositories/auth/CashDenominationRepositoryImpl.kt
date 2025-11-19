package com.retail.dolphinpos.data.repositories.auth

import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.data.mapper.UserMapper
import com.retail.dolphinpos.data.service.ApiService
import com.retail.dolphinpos.data.util.safeApiCallResult
import com.retail.dolphinpos.data.util.getErrorMessage
import com.retail.dolphinpos.data.util.parseErrorResponse
import com.retail.dolphinpos.domain.model.auth.batch.Batch
import com.retail.dolphinpos.domain.model.auth.batch.BatchDetails
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
            // Check for 422 status code - return failure with HttpException as cause
            if (e.code() == 422) {
                val errorMessage = e.getErrorMessage()
                Result.failure(Exception(errorMessage).apply {
                    // Preserve the 422 status code by setting the cause (HttpException)
                    initCause(e)
                })
            } else {
                // For other HttpException status codes, use handleApiErrorResult logic
                val errorResponse = e.parseErrorResponse<BatchOpenResponse>()
                val errorMessage = errorResponse?.message ?: "Batch open failed"
                Result.failure(Exception(errorMessage).apply {
                    initCause(e)
                })
            }
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