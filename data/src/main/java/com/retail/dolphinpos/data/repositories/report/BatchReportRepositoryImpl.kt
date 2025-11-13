package com.retail.dolphinpos.data.repositories.report

import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.data.mapper.UserMapper
import com.retail.dolphinpos.data.service.ApiService
import com.retail.dolphinpos.data.util.safeApiCall
import com.retail.dolphinpos.data.util.safeApiCallResult
import com.retail.dolphinpos.domain.model.auth.batch.Batch
import com.retail.dolphinpos.domain.model.auth.cash_denomination.BatchCloseRequest
import com.retail.dolphinpos.domain.model.auth.cash_denomination.BatchCloseResponse
import com.retail.dolphinpos.domain.model.report.BatchReport
import com.retail.dolphinpos.domain.repositories.report.BatchReportRepository

class BatchReportRepositoryImpl(
    private val apiService: ApiService,
    private val userDao: UserDao
) : BatchReportRepository {

    override suspend fun getBatchDetails(): Batch {
        val batchEntity = userDao.getBatchDetails()
        return UserMapper.toBatchDetails(batchEntity)
    }

    override suspend fun getBatchReport(batchNo: String): BatchReport {
        return safeApiCall(
            apiCall = { apiService.getBatchReport(batchNo) },
            defaultResponse = {
                BatchReport(
                    data = com.retail.dolphinpos.domain.model.report.BatchReportData(
                        batchNo = "",
                        closed = null,
                        closedBy = 0,
                        closingCashAmount = 0.0,
                        closingTime = null,
                        createdAt = null,
                        id = 0,
                        locationId = 0,
                        openTime = null,
                        opened = null,
                        openedBy = 0,
                        payInCard = 0,
                        payInCash = 0,
                        payOutCard = 0,
                        payOutCash = 0,
                        startingCashAmount = 0.0,
                        status = null,
                        storeId = 0,
                        storeRegisterId = 0,
                        totalAbandonOrders = 0,
                        totalAmount = null,
                        totalCardAmount = null,
                        totalCashAmount = null,
                        totalCashDiscount = null,
                        totalDiscount = null,
                        totalOnlineSales = null,
                        totalPayIn = 0,
                        totalPayOut = 0,
                        totalRewardDiscount = null,
                        totalSales = 0,
                        totalTax = null,
                        totalTip = 0,
                        totalTipCard = 0,
                        totalTipCash = 0,
                        totalTransactions = 0,
                        updatedAt = null
                    )
                )
            }
        )
    }

    override suspend fun batchClose(batchNo: String, batchCloseRequest: BatchCloseRequest): Result<BatchCloseResponse> {
        return safeApiCallResult<BatchCloseResponse>(
            apiCall = { apiService.batchClose(batchNo, batchCloseRequest) },
            defaultMessage = "Batch close failed",
            messageExtractor = { errorResponse: BatchCloseResponse -> errorResponse.message }
        )
    }
}

