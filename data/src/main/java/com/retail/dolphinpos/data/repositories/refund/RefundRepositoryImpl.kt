package com.retail.dolphinpos.data.repositories.refund

import com.google.gson.Gson
import com.retail.dolphinpos.common.network.NetworkMonitor
import com.retail.dolphinpos.data.dao.OrderDao
import com.retail.dolphinpos.data.dao.ProductsDao
import com.retail.dolphinpos.data.dao.SyncCommandDao
import com.retail.dolphinpos.data.mapper.RefundMapper
import com.retail.dolphinpos.data.entities.refund.RefundEntity
import com.retail.dolphinpos.data.entities.refund.RefundStatus
import com.retail.dolphinpos.data.entities.sync.CommandType
import com.retail.dolphinpos.data.entities.sync.CommandStatus
import com.retail.dolphinpos.data.entities.sync.SyncCommandEntity
import com.google.gson.reflect.TypeToken
import com.retail.dolphinpos.domain.model.order.Order
import com.retail.dolphinpos.domain.model.order.OrderRefundStatus
import com.retail.dolphinpos.domain.model.refund.Refund
import com.retail.dolphinpos.domain.model.refund.RefundRequest
import com.retail.dolphinpos.domain.model.refund.RefundStatus as DomainRefundStatus
import com.retail.dolphinpos.domain.repositories.refund.RefundRepository
import com.retail.dolphinpos.domain.usecases.sync.ScheduleSyncUseCase
import java.util.UUID
import javax.inject.Inject

class RefundRepositoryImpl @Inject constructor(
    private val localDataSource: RefundLocalDataSource,
    private val remoteDataSource: RefundRemoteDataSource,
    private val orderDao: OrderDao,
    private val productsDao: ProductsDao,
    private val syncCommandDao: SyncCommandDao,
    private val networkMonitor: NetworkMonitor,
    private val scheduleSyncUseCase: ScheduleSyncUseCase,
    private val gson: Gson
) : RefundRepository {
    
    override suspend fun createRefund(request: RefundRequest, refundAmount: Double, refundedItems: List<com.retail.dolphinpos.domain.model.refund.RefundedItem>): Result<Refund> {
        return try {
            // Get order to validate
            val order = orderDao.getOrderById(request.orderId)
                ?: return Result.failure(IllegalArgumentException("Order not found"))
            
            // Generate unique refund ID
            val refundId = generateRefundId(order.storeId, order.locationId)
            
            // Create refund entity (will be mapped to domain model)
            val refundEntity = RefundEntity(
                refundId = refundId,
                orderId = request.orderId,
                orderNo = order.orderNumber,
                refundType = when (request.refundType) {
                    com.retail.dolphinpos.domain.model.refund.RefundType.FULL -> 
                        com.retail.dolphinpos.data.entities.refund.RefundType.FULL
                    com.retail.dolphinpos.domain.model.refund.RefundType.PARTIAL -> 
                        com.retail.dolphinpos.data.entities.refund.RefundType.PARTIAL
                },
                refundAmount = refundAmount,
                refundedItems = gson.toJson(refundedItems),
                paymentMethod = com.retail.dolphinpos.data.entities.transaction.PaymentMethod.fromString(request.paymentMethod),
                refundStatus = RefundStatus.PENDING,
                storeId = order.storeId,
                locationId = order.locationId,
                userId = order.userId,
                batchNo = order.batchNo,
                reason = request.reason
            )
            
            // Insert refund locally
            val insertedId = localDataSource.insertRefund(refundEntity)
            val insertedRefund = localDataSource.getRefundById(insertedId)
                ?: return Result.failure(IllegalStateException("Failed to retrieve inserted refund"))
            
            // Convert to domain model
            val refund = RefundMapper.toRefund(insertedRefund)
            
            // Try to sync if online
            if (networkMonitor.isNetworkAvailable()) {
                syncRefundToServer(refund).getOrNull()?.let { syncedRefund ->
                    return Result.success(syncedRefund)
                }
            } else {
                // Create sync command for offline refund
                val sequence = syncCommandDao.getNextSequence()
                val command = SyncCommandEntity(
                    sequence = sequence,
                    type = CommandType.CREATE_REFUND,
                    batchId = order.batchNo,
                    orderId = refundId, // Use refundId as orderId field for refund commands
                    status = CommandStatus.PENDING,
                    attempts = 0,
                    lastError = null,
                    createdAt = System.currentTimeMillis(),
                    idempotencyKey = refundId
                )
                syncCommandDao.insertCommand(command)
            }
            
            Result.success(refund)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getRefundById(refundId: Long): Refund? {
        return localDataSource.getRefundById(refundId)?.let { RefundMapper.toRefund(it) }
    }
    
    override suspend fun getRefundByRefundId(refundId: String): Refund? {
        return localDataSource.getRefundByRefundId(refundId)?.let { RefundMapper.toRefund(it) }
    }
    
    override suspend fun getRefundsByOrderId(orderId: Long): List<Refund> {
        return localDataSource.getRefundsByOrderId(orderId).map { RefundMapper.toRefund(it) }
    }
    
    override suspend fun getPendingRefunds(): List<Refund> {
        return localDataSource.getPendingRefunds().map { RefundMapper.toRefund(it) }
    }
    
    override suspend fun syncRefundToServer(refund: Refund): Result<Refund> {
        return try {
            if (!networkMonitor.isNetworkAvailable()) {
                return Result.failure(IllegalStateException("Network not available"))
            }
            
            // Get order to get server ID
            val order = orderDao.getOrderByOrderNumber(refund.orderNo)
                ?: return Result.failure(IllegalArgumentException("Order not found"))
            
            val serverOrderId = order.serverId
                ?: return Result.failure(IllegalStateException("Order not synced to server yet"))
            
            // Create API request
            val request = com.retail.dolphinpos.domain.model.refund.CreateRefundRequest(
                orderId = serverOrderId.toInt(),
                refundType = refund.refundType.name,
                refundAmount = refund.refundAmount,
                refundedItems = refund.refundedItems.map { item ->
                    com.retail.dolphinpos.domain.model.refund.RefundedItemRequest(
                        productId = item.productId,
                        productVariantId = item.productVariantId,
                        quantity = item.quantity
                    )
                },
                paymentMethod = refund.paymentMethod,
                reason = refund.reason
            )
            
            // Call API
            val response = remoteDataSource.createRefund(request)
            
            val refundData = response.refund
            if (response.success && refundData != null) {
                // Update local refund with server ID and status
                val updatedRefund = refund.copy(
                    serverId = refundData.id,
                    refundStatus = DomainRefundStatus.SYNCED
                )
                
                val updatedEntity = RefundMapper.toRefundEntity(updatedRefund, gson)
                localDataSource.updateRefund(updatedEntity)
                
                Result.success(updatedRefund)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateRefundStatus(refundId: String, status: DomainRefundStatus, serverId: Int?) {
        val entity = localDataSource.getRefundByRefundId(refundId)
            ?: return
        
        val updatedEntity = entity.copy(
            refundStatus = when (status) {
                DomainRefundStatus.PENDING -> RefundStatus.PENDING
                DomainRefundStatus.SYNCED -> RefundStatus.SYNCED
            },
            serverId = serverId ?: entity.serverId,
            updatedAt = System.currentTimeMillis()
        )
        
        localDataSource.updateRefund(updatedEntity)
    }
    
    override suspend fun getTotalRefundedAmountForOrder(orderId: Long): Double {
        return localDataSource.getTotalRefundedAmountForOrder(orderId)
    }
    
    override suspend fun getOrderById(orderId: Long): Order? {
        val orderEntity = orderDao.getOrderById(orderId) ?: return null
        
        // Parse order items
        val orderItems: List<com.retail.dolphinpos.domain.model.home.create_order.CheckOutOrderItem> = try {
            val type = object : com.google.gson.reflect.TypeToken<List<com.retail.dolphinpos.domain.model.home.create_order.CheckOutOrderItem>>() {}.type
            gson.fromJson<List<com.retail.dolphinpos.domain.model.home.create_order.CheckOutOrderItem>>(orderEntity.items, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        
        return Order(
            id = orderEntity.id,
            orderNumber = orderEntity.orderNumber,
            orderItems = orderItems,
            subTotal = orderEntity.subTotal,
            total = orderEntity.total,
            taxValue = orderEntity.taxValue,
            discountAmount = orderEntity.discountAmount,
            cashDiscountAmount = orderEntity.cashDiscountAmount,
            rewardDiscount = orderEntity.rewardDiscount,
            totalRefundedAmount = orderEntity.totalRefundedAmount,
            refundStatus = try {
                OrderRefundStatus.valueOf(orderEntity.refundStatus)
            } catch (e: Exception) {
                OrderRefundStatus.NONE
            },
            isVoid = orderEntity.isVoid
        )
    }
    
    override suspend fun updateOrderRefundStatus(orderId: Long, totalRefundedAmount: Double, refundStatus: OrderRefundStatus) {
        val orderEntity = orderDao.getOrderById(orderId) ?: return
        
        val updatedOrder = orderEntity.copy(
            totalRefundedAmount = totalRefundedAmount,
            refundStatus = refundStatus.name,
            updatedAt = System.currentTimeMillis()
        )
        
        orderDao.updateOrder(updatedOrder)
    }
    
    override suspend fun restoreInventory(productId: Int?, productVariantId: Int?, quantity: Int) {
        if (productVariantId != null) {
            // Restore variant quantity (negative quantity to add)
            productsDao.deductVariantQuantity(productVariantId, -quantity)
        } else if (productId != null) {
            // Restore product quantity (negative quantity to add)
            productsDao.deductProductQuantity(productId, -quantity)
        }
    }
    
    private fun generateRefundId(storeId: Int, locationId: Int): String {
        val timestamp = System.currentTimeMillis()
        return "REFUND_S${storeId}L${locationId}-$timestamp"
    }
}

