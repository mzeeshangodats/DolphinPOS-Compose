package com.retail.dolphinpos.domain.repositories.report

import com.retail.dolphinpos.domain.model.auth.batch.Batch
import com.retail.dolphinpos.domain.model.auth.cash_denomination.BatchCloseRequest
import com.retail.dolphinpos.domain.model.auth.cash_denomination.BatchCloseResponse
import com.retail.dolphinpos.domain.model.report.BatchReport

interface BatchReportRepository {
    suspend fun getBatchDetails(): Batch
    suspend fun getBatchReport(batchNo: String): BatchReport
    suspend fun batchClose(batchNo: String, batchCloseRequest: BatchCloseRequest): Result<BatchCloseResponse>
}

