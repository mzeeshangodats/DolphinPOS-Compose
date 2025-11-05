package com.retail.dolphinpos.data.repositories.auth

import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.data.mapper.UserMapper
import com.retail.dolphinpos.data.service.ApiService
import com.retail.dolphinpos.data.util.safeApiCallResult
import com.retail.dolphinpos.domain.model.auth.batch.Batch
import com.retail.dolphinpos.domain.model.auth.batch.BatchDetails
import com.retail.dolphinpos.domain.model.auth.cash_denomination.BatchOpenRequest
import com.retail.dolphinpos.domain.model.auth.cash_denomination.BatchOpenResponse
import com.retail.dolphinpos.domain.repositories.auth.CashDenominationRepository

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
        return safeApiCallResult(
            apiCall = { apiService.batchOpen(batchOpenRequest) },
            defaultMessage = "Batch open failed",
            messageExtractor = { errorResponse -> errorResponse.message }
        )
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