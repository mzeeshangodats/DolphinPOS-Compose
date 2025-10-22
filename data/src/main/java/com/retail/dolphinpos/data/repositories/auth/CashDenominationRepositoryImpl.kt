package com.retail.dolphinpos.data.repositories.auth

import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.data.mapper.UserMapper
import com.retail.dolphinpos.domain.model.auth.batch.Batch
import com.retail.dolphinpos.domain.repositories.auth.CashDenominationRepository

class CashDenominationRepositoryImpl(
    private val userDao: UserDao
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

}