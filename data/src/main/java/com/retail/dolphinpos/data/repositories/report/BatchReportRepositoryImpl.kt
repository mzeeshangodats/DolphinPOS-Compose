package com.retail.dolphinpos.data.repositories.report

import android.util.Log
import com.retail.dolphinpos.common.network.NetworkMonitor
import com.retail.dolphinpos.data.dao.BatchDao
import com.retail.dolphinpos.data.dao.BatchReportDao
import com.retail.dolphinpos.data.dao.HoldCartDao
import com.retail.dolphinpos.data.dao.OrderDao
import com.retail.dolphinpos.data.dao.UserDao
import com.retail.dolphinpos.data.entities.order.OrderEntity
import com.retail.dolphinpos.data.entities.user.BatchEntity
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
import com.retail.dolphinpos.domain.model.report.batch_report.Closed
import com.retail.dolphinpos.domain.model.report.batch_report.Opened
import com.retail.dolphinpos.domain.repositories.report.BatchReportRepository
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Locale

class BatchReportRepositoryImpl(
    private val apiService: ApiService,
    private val userDao: UserDao,
    private val batchReportDao: BatchReportDao,
    private val batchDao: BatchDao,
    private val orderDao: OrderDao,
    private val holdCartDao: HoldCartDao,
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
                // For other HTTP errors, try to get from local database or calculate from orders
                val localEntity = batchReportDao.getBatchReportByBatchNo(batchNo)
                if (localEntity != null) {
                    BatchReport(
                        data = BatchReportMapper.toBatchReportData(localEntity)
                    )
                } else {
                    // Try to calculate from local orders
                    try {
                        val calculatedReport = calculateBatchReportFromLocalOrders(batchNo)
                        BatchReport(data = calculatedReport)
                    } catch (e: Exception) {
                        Log.e("BatchReportRepositoryImpl", "Error calculating batch report from local orders: ${e.message}", e)
                        BatchReport(data = createEmptyBatchReportData())
                    }
                }
            } catch (e: Exception) {
                // If API call fails, try to get from local database or calculate from orders
                val localEntity = batchReportDao.getBatchReportByBatchNo(batchNo)
                if (localEntity != null) {
                    BatchReport(
                        data = BatchReportMapper.toBatchReportData(localEntity)
                    )
                } else {
                    // Try to calculate from local orders
                    try {
                        val calculatedReport = calculateBatchReportFromLocalOrders(batchNo)
                        BatchReport(data = calculatedReport)
                    } catch (calcException: Exception) {
                        Log.e("BatchReportRepositoryImpl", "Error calculating batch report from local orders: ${calcException.message}", calcException)
                        BatchReport(data = createEmptyBatchReportData())
                    }
                }
            }
        } else {
            // No internet - try to get from cached batch report first
            val localEntity = batchReportDao.getBatchReportByBatchNo(batchNo)
            if (localEntity != null) {
                return BatchReport(
                    data = BatchReportMapper.toBatchReportData(localEntity)
                )
            }
            
            // No cached report - calculate from local orders
            return try {
                val calculatedReport = calculateBatchReportFromLocalOrders(batchNo)
                BatchReport(data = calculatedReport)
            } catch (e: Exception) {
                Log.e("BatchReportRepositoryImpl", "Error calculating batch report from local orders: ${e.message}", e)
                // Return empty/default response if calculation fails
                BatchReport(
                    data = createEmptyBatchReportData()
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

    /**
     * Calculates batch report from local orders and batch data.
     * This is used when offline to display real-time batch report data.
     */
    private suspend fun calculateBatchReportFromLocalOrders(batchNo: String): BatchReportData {
        // Get batch entity by batchNo
        val batchEntity = batchDao.getBatchByBatchNo(batchNo)
            ?: throw IllegalStateException("Batch not found for batchNo: $batchNo")

        // Get all orders for this batch
        val orders = orderDao.getOrdersByBatchId(batchEntity.batchId)
        
        // Filter out void orders
        val validOrders = orders.filter { !it.isVoid }

        // Calculate aggregated values from orders
        val totalTransactions = validOrders.size
        val totalSales = validOrders.sumOf { it.total }
        val totalTax = validOrders.sumOf { it.taxValue }
        val totalDiscount = validOrders.sumOf { it.discountAmount }
        val totalCashDiscount = validOrders.sumOf { it.cashDiscountAmount }
        val totalRewardDiscount = validOrders.sumOf { it.rewardDiscount }
        
        // Calculate payment method totals
        var totalCashAmount = 0.0
        var totalCardAmount = 0.0
        var totalOnlineSales = 0.0
        
        validOrders.forEach { order ->
            when (order.paymentMethod.lowercase()) {
                "cash" -> totalCashAmount += order.total
                "card", "credit_card", "debit_card" -> totalCardAmount += order.total
                "online" -> totalOnlineSales += order.total
            }
        }

        // Get abandoned carts count (hold carts)
        val totalAbandonOrders = if (batchEntity.storeId != null && batchEntity.registerId != null) {
            // Note: HoldCartDao requires userId, but we can approximate with batch userId
            // If userId is not available, we'll use 0 as fallback
            try {
                holdCartDao.getHoldCartCount(
                    userId = batchEntity.userId ?: 0,
                    storeId = batchEntity.storeId,
                    registerId = batchEntity.registerId
                )
            } catch (e: Exception) {
                Log.w("BatchReportRepositoryImpl", "Could not get hold cart count: ${e.message}")
                0
            }
        } else {
            0
        }

        // Format dates
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val openTime = dateFormat.format(java.util.Date(batchEntity.startedAt))
        val closingTime = batchEntity.closedAt?.let { dateFormat.format(java.util.Date(it)) }
        val createdAt = dateFormat.format(java.util.Date(batchEntity.startedAt))
        val updatedAt = batchEntity.closedAt?.let { dateFormat.format(java.util.Date(it)) } ?: createdAt

        // Determine status
        val status = if (batchEntity.closedAt != null) "closed" else "open"

        return BatchReportData(
            batchNo = batchEntity.batchNo,
            closed = if (batchEntity.closedAt != null) Closed(name = "") else null,
            closedBy = 0, // Not stored in BatchEntity
            closingCashAmount = batchEntity.closingCashAmount ?: 0.0,
            closingTime = closingTime,
            createdAt = createdAt,
            id = 0, // Local batch doesn't have server ID
            locationId = batchEntity.locationId ?: 0,
            openTime = openTime,
            opened = Opened(name = ""), // Name would come from user lookup
            openedBy = batchEntity.userId ?: 0,
            payInCard = 0, // Not tracked in orders
            payInCash = 0, // Not tracked in orders
            payOutCard = 0, // Not tracked in orders
            payOutCash = 0, // Not tracked in orders
            startingCashAmount = batchEntity.startingCashAmount,
            status = status,
            storeId = batchEntity.storeId ?: 0,
            storeRegisterId = batchEntity.registerId ?: 0,
            totalAbandonOrders = totalAbandonOrders,
            totalAmount = totalSales.toString(),
            totalCardAmount = totalCardAmount.toString(),
            totalCashAmount = totalCashAmount.toString(),
            totalCashDiscount = totalCashDiscount.toString(),
            totalDiscount = totalDiscount.toString(),
            totalOnlineSales = totalOnlineSales.toString(),
            totalPayIn = 0, // Not tracked in orders
            totalPayOut = 0, // Not tracked in orders
            totalRewardDiscount = totalRewardDiscount.toString(),
            totalSales = totalSales,
            totalTax = totalTax.toString(),
            totalTip = 0, // Not tracked in orders
            totalTipCard = 0, // Not tracked in orders
            totalTipCash = 0, // Not tracked in orders
            totalTransactions = totalTransactions,
            updatedAt = updatedAt
        )
    }

    /**
     * Creates an empty BatchReportData with default values.
     */
    private fun createEmptyBatchReportData(): BatchReportData {
        return BatchReportData(
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
    }
}

