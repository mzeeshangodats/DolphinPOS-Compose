package com.retail.dolphinpos.domain.repositories.auth

import com.retail.dolphinpos.domain.model.auth.batch.Batch

interface CashDenominationRepository {

    suspend fun insertBatchIntoLocalDB(batch: Batch)

    suspend fun getBatchDetails(): Batch

}