package com.retail.dolphinpos.data.repositories.report

import com.retail.dolphinpos.common.network.NetworkMonitor
import com.retail.dolphinpos.data.dao.BatchReportDao
import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.data.mapper.BatchReportMapper
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
    private val userDao: UserDao,
    private val batchReportDao: BatchReportDao,
    private val networkMonitor: NetworkMonitor
) : BatchReportRepository {

    override suspend fun getBatchDetails(): Batch {
        val batchEntity = userDao.getBatchDetails()
        return UserMapper.toBatchDetails(batchEntity)
    }

    override suspend fun getBatchReport(batchNo: String): BatchReport {
        // Check if internet is available
        if (networkMonitor.isNetworkAvailable()) {
            // Try to get data from API
            return try {
                val apiResponse = apiService.getBatchReport(batchNo)
                
                // Save to local database
                if (apiResponse.data != null) {
                    // First, check if any batch report exists and delete all existing batch reports
                    val existingBatchReports = batchReportDao.getAllBatchReports()
                    if (existingBatchReports.isNotEmpty()) {
                        batchReportDao.deleteAllBatchReports()
                    }
                    
                    // Then insert the new batch report
                    val batchReportEntity = BatchReportMapper.toBatchReportEntity(apiResponse.data)
                    batchReportDao.insertBatchReport(batchReportEntity)
                }
                
                apiResponse
            } catch (e: Exception) {
                // If API call fails, try to get from local database
                val localEntity = batchReportDao.getBatchReportByBatchNo(batchNo)
                if (localEntity != null) {
                    BatchReport(
                        data = BatchReportMapper.toBatchReportData(localEntity)
                    )
                } else {
                    // Return empty/default response if nothing found
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
            }
        } else {
            // No internet - get from local database
            val localEntity = batchReportDao.getBatchReportByBatchNo(batchNo)
            return if (localEntity != null) {
                BatchReport(
                    data = BatchReportMapper.toBatchReportData(localEntity)
                )
            } else {
                // Return empty/default response if nothing found
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
        }
    }

    override suspend fun batchClose(batchNo: String, batchCloseRequest: BatchCloseRequest): Result<BatchCloseResponse> {
        return safeApiCallResult<BatchCloseResponse>(
            apiCall = { apiService.batchClose(batchNo, batchCloseRequest) },
            defaultMessage = "Batch close failed",
            messageExtractor = { errorResponse: BatchCloseResponse -> errorResponse.message }
        )
    }
}

