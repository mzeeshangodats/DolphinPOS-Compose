package com.retail.dolphinpos.domain.repositories.auth

import com.retail.dolphinpos.domain.model.auth.batch.Batch
import com.retail.dolphinpos.domain.model.auth.cash_denomination.BatchOpenRequest
import com.retail.dolphinpos.domain.model.auth.cash_denomination.BatchOpenResponse

interface CashDenominationRepository {

    suspend fun insertBatchIntoLocalDB(batch: Batch)

    suspend fun getBatchDetails(): Batch

    suspend fun batchOpen(batchOpenRequest: BatchOpenRequest): Result<BatchOpenResponse>

    suspend fun markBatchAsSynced(batchNo: String)

}