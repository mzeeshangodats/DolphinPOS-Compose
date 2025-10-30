package com.retail.dolphinpos.data.repositories.auth

import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.data.mapper.UserMapper
import com.retail.dolphinpos.data.service.ApiService
import com.retail.dolphinpos.data.util.getErrorMessage
import com.retail.dolphinpos.domain.model.auth.batch.Batch
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
            val response = apiService.batchOpen(batchOpenRequest)
            Result.success(response)
        } catch (e: HttpException) {
            // Parse error message from HTTP response
            val errorMessage = e.getErrorMessage()
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
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