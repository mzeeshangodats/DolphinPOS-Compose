package com.retail.dolphinpos.data.repositories.report

import com.retail.dolphinpos.common.network.NetworkMonitor
import com.retail.dolphinpos.data.dao.BatchReportDao
import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.data.mapper.BatchReportMapper
import com.retail.dolphinpos.data.mapper.UserMapper
import com.retail.dolphinpos.data.service.ApiService
import com.retail.dolphinpos.data.util.safeApiCallResult
import com.retail.dolphinpos.domain.model.auth.batch.Batch
import com.retail.dolphinpos.domain.model.auth.cash_denomination.BatchCloseRequest
import com.retail.dolphinpos.domain.model.auth.cash_denomination.BatchCloseResponse
import com.retail.dolphinpos.domain.model.report.batch_history.BatchReportHistoryData
import com.retail.dolphinpos.domain.model.report.batch_history.BatchReportHistoryResponse
import com.retail.dolphinpos.domain.model.report.batch_report.BatchReport
import com.retail.dolphinpos.domain.model.report.batch_report.BatchReportData
import com.retail.dolphinpos.domain.repositories.report.BatchReportRepository
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Locale

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
            } catch (e: HttpException) {
                // Handle 404 error - don't fall back to local, let it propagate so ViewModel can handle navigation
                if (e.code() == 404) {
                    throw e
                }
                // For other HTTP errors, try to get from local database
                val localEntity = batchReportDao.getBatchReportByBatchNo(batchNo)
                if (localEntity != null) {
                    BatchReport(
                        data = BatchReportMapper.toBatchReportData(localEntity)
                    )
                } else {
                    // Return empty/default response if nothing found
                    BatchReport(
                        data = BatchReportData(
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
                        data = BatchReportData(
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
                    data = BatchReportData(
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

    override suspend fun getBatchHistory(
        startDate: String,
        endDate: String,
        status: String,
        storeId: Int,
        page: Int,
        limit: Int,
        paginate: Boolean,
        orderBy: String,
        order: String,
        keyword: String?
    ): Result<List<BatchReportHistoryData>> {
        return try {
            // Check if internet is available
            if (networkMonitor.isNetworkAvailable()) {
                // Try to get data from API
                val response = apiService.getBatchReportHistory(
                    startDate = startDate,
                    endDate = endDate,
                    status = status,
                    storeId = storeId,
                    page = page,
                    limit = limit,
                    paginate = paginate,
                    orderBy = orderBy,
                    order = order,
                    keyword = keyword
                )
                
                if (response.isSuccessful && response.body() != null) {
                    val batchHistoryResponse = response.body()!!
                    val batchHistoryList = batchHistoryResponse.data
                    
                    // Save to local database
                    saveBatchHistoryToLocalDB(batchHistoryList, storeId)
                    
                    Result.success(batchHistoryList)
                } else {
                    // If API call fails, try to get from local database
                    loadBatchHistoryFromLocalDB(storeId, startDate, endDate)
                }
            } else {
                // No internet - get from local database
                loadBatchHistoryFromLocalDB(storeId, startDate, endDate)
            }
        } catch (e: Exception) {
            // If API call fails, try to get from local database
            loadBatchHistoryFromLocalDB(storeId, startDate, endDate)
        }
    }

    private suspend fun saveBatchHistoryToLocalDB(
        batchHistoryList: List<BatchReportHistoryData>,
        storeId: Int
    ) {
        try {
            // Delete existing batch history for this store
            userDao.deleteBatchHistoryByStoreId(storeId)
            
            // Convert and save batch history entities
            val batchHistoryEntities = batchHistoryList.map { batchHistory ->
                com.retail.dolphinpos.data.entities.user.BatchHistoryEntity(
                    id = batchHistory.id,
                    batchNo = batchHistory.batchNo,
                    startingCashAmount = batchHistory.startingCashAmount,
                    closingCashAmount = when (val amount = batchHistory.closingCashAmount) {
                        is Number -> amount.toDouble()
                        is String -> amount.toDoubleOrNull()
                        else -> null
                    },
                    openTime = batchHistory.openTime,
                    closingTime = when (val time = batchHistory.closingTime) {
                        is String -> time
                        else -> null
                    },
                    status = batchHistory.status,
                    storeId = batchHistory.storeId,
                    locationId = batchHistory.locationId,
                    storeRegisterId = batchHistory.storeRegisterId,
                    openedBy = batchHistory.openedBy,
                    closedBy = when (val by = batchHistory.closedBy) {
                        is Number -> by.toInt()
                        is String -> by.toIntOrNull()
                        else -> null
                    },
                    totalSales = when (val sales = batchHistory.totalSales) {
                        is String -> sales
                        is Number -> sales.toString()
                        else -> null
                    },
                    totalTax = when (val tax = batchHistory.totalTax) {
                        is String -> tax
                        is Number -> tax.toString()
                        else -> null
                    },
                    totalDiscount = batchHistory.totalDiscount,
                    totalTransactions = batchHistory.totalTransactions,
                    createdAt = batchHistory.createdAt,
                    updatedAt = batchHistory.updatedAt
                )
            }
            
            // Insert batch history entities
            userDao.insertBatchHistoryList(batchHistoryEntities)
        } catch (e: Exception) {
            android.util.Log.e("BatchReportRepositoryImpl", "Error saving batch history to local DB: ${e.message}")
        }
    }

    private suspend fun loadBatchHistoryFromLocalDB(
        storeId: Int,
        startDate: String?,
        endDate: String?
    ): Result<List<BatchReportHistoryData>> {
        return try {
            val batchHistoryEntities = if (startDate != null && endDate != null) {
                try {
                    userDao.getBatchHistoryByStoreIdAndDateRange(storeId, startDate, endDate)
                } catch (e: Exception) {
                    android.util.Log.e("BatchReportRepositoryImpl", "Error parsing dates: ${e.message}")
                    userDao.getBatchHistoryByStoreId(storeId)
                }
            } else {
                userDao.getBatchHistoryByStoreId(storeId)
            }
            
            // Convert entities to domain models
            val batchHistoryList = batchHistoryEntities.map { entity ->
                BatchReportHistoryData(
                    batchNo = entity.batchNo,
                    closed = "", // Not stored in entity, use empty string as default
                    closedBy = entity.closedBy ?: 0,
                    closingCashAmount = entity.closingCashAmount ?: 0.0,
                    closingTime = entity.closingTime ?: "",
                    createdAt = entity.createdAt,
                    id = entity.id,
                    locationId = entity.locationId,
                    openTime = entity.openTime,
                    opened = com.retail.dolphinpos.domain.model.report.batch_report.Opened(
                        name = ""
                    ),
                    openedBy = entity.openedBy,
                    startingCashAmount = entity.startingCashAmount,
                    status = entity.status,
                    storeId = entity.storeId,
                    storeRegisterId = entity.storeRegisterId,
                    totalDiscount = entity.totalDiscount ?: "0",
                    totalSales = entity.totalSales ?: "0",
                    totalTax = entity.totalTax ?: "0",
                    totalTransactions = entity.totalTransactions,
                    updatedAt = entity.updatedAt
                )
            }
            
            Result.success(batchHistoryList)
        } catch (e: Exception) {
            android.util.Log.e("BatchReportRepositoryImpl", "Error loading batch history from local DB: ${e.message}")
            Result.failure(e)
        }
    }
}

