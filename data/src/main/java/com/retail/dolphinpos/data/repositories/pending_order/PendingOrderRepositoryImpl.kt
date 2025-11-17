package com.retail.dolphinpos.data.repositories.pending_order

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.retail.dolphinpos.data.dao.PendingOrderDao
import com.retail.dolphinpos.data.service.ApiService
import com.retail.dolphinpos.data.util.safeApiCallResult
import com.retail.dolphinpos.data.entities.order.PendingOrderEntity
import com.retail.dolphinpos.domain.model.home.create_order.CreateOrderRequest
import com.retail.dolphinpos.domain.model.home.create_order.CreateOrderResponse
import com.retail.dolphinpos.domain.model.order.PendingOrder

class PendingOrderRepositoryImpl(
    private val pendingOrderDao: PendingOrderDao,
    private val apiService: ApiService,
    private val gson: Gson
) {

    suspend fun saveOrderToLocal(orderRequest: CreateOrderRequest): Long {
        val orderEntity = PendingOrderEntity(
            orderNumber = orderRequest.orderNumber ?: "",
            invoiceNo = orderRequest.invoiceNo,
            customerId = orderRequest.customerId,
            storeId = orderRequest.storeId,
            locationId = orderRequest.locationId,
            storeRegisterId = orderRequest.storeRegisterId,
            batchNo = orderRequest.batchNo,
            paymentMethod = orderRequest.paymentMethod,
            isRedeemed = orderRequest.isRedeemed,
            source = orderRequest.source,
            redeemPoints = orderRequest.redeemPoints,
            items = gson.toJson(orderRequest.items),
            subTotal = orderRequest.subTotal,
            total = orderRequest.total,
            applyTax = orderRequest.applyTax,
            taxValue = orderRequest.taxValue,
            discountAmount = orderRequest.discountAmount,
            cashDiscountAmount = orderRequest.cashDiscountAmount,
            rewardDiscount = orderRequest.rewardDiscount,
            discountIds = orderRequest.discountIds?.let { gson.toJson(it) },
            transactionId = orderRequest.transactionId,
            transactions = orderRequest.transactions?.let { gson.toJson(it) },
            cardDetails = orderRequest.cardDetails?.let { gson.toJson(it) },
            userId = orderRequest.userId,
            voidReason = orderRequest.voidReason,
            isVoid = orderRequest.isVoid,
            isSynced = false,
            taxDetails = orderRequest.taxDetails?.let { gson.toJson(it) },
            taxExempt = orderRequest.taxExempt
        )
        return pendingOrderDao.insertPendingOrder(orderEntity)
    }

    suspend fun syncOrderToServer(order: PendingOrderEntity): Result<CreateOrderResponse> {
        return try {
            val orderRequest = convertToCreateOrderRequest(order)
            val result = safeApiCallResult(
                apiCall = { apiService.createOrder(orderRequest) },
                defaultMessage = "Order creation failed",
                messageExtractor = { errorResponse -> errorResponse.message }
            )
            
            // Mark as synced only if successful
            result.onSuccess { response ->
                val updatedOrder = order.copy(isSynced = true)
                pendingOrderDao.updatePendingOrder(updatedOrder)
            }
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUnsyncedOrders(): List<PendingOrderEntity> {
        return pendingOrderDao.getUnsyncedOrders()
    }

    suspend fun getOrderById(orderId: Long): PendingOrderEntity? {
        return pendingOrderDao.getOrderById(orderId)
    }

    suspend fun getLastPendingOrder(): PendingOrderEntity? {
        return pendingOrderDao.getLastPendingOrder()
    }
    
    suspend fun getLastPendingOrderRegardlessOfSync(): PendingOrderEntity? {
        return pendingOrderDao.getLastPendingOrderRegardlessOfSync()
    }

    suspend fun deleteOrder(orderId: Long) {
        val order = pendingOrderDao.getOrderById(orderId)
        order?.let { pendingOrderDao.deletePendingOrder(it) }
    }

    private fun convertToCreateOrderRequest(entity: PendingOrderEntity): CreateOrderRequest {
        val itemsType = object : TypeToken<List<com.retail.dolphinpos.domain.model.home.create_order.CheckOutOrderItem>>() {}.type
        val discountIdsType = object : TypeToken<List<Int>>() {}.type
        val transactionsType = object : TypeToken<List<com.retail.dolphinpos.domain.model.home.create_order.CheckoutSplitPaymentTransactions>>() {}.type
        val cardDetailsType = object : TypeToken<com.retail.dolphinpos.domain.model.home.create_order.CardDetails>() {}.type
        val taxDetailsType = object : TypeToken<List<com.retail.dolphinpos.domain.model.TaxDetail>>() {}.type

        return CreateOrderRequest(
            orderNumber = entity.orderNumber,
            invoiceNo = entity.invoiceNo,
            customerId = entity.customerId,
            storeId = entity.storeId,
            locationId = entity.locationId,
            storeRegisterId = entity.storeRegisterId,
            batchNo = entity.batchNo,
            paymentMethod = entity.paymentMethod,
            isRedeemed = entity.isRedeemed,
            source = entity.source,
            redeemPoints = entity.redeemPoints,
            items = gson.fromJson(entity.items, itemsType),
            subTotal = entity.subTotal,
            total = entity.total,
            applyTax = entity.applyTax,
            taxValue = entity.taxValue,
            discountAmount = entity.discountAmount,
            cashDiscountAmount = entity.cashDiscountAmount,
            rewardDiscount = entity.rewardDiscount,
            discountIds = entity.discountIds?.let { gson.fromJson<List<Int>>(it, discountIdsType) },
            transactionId = entity.transactionId,
            userId = entity.userId,
            voidReason = entity.voidReason,
            isVoid = entity.isVoid,
            transactions = entity.transactions?.let { gson.fromJson<List<com.retail.dolphinpos.domain.model.home.create_order.CheckoutSplitPaymentTransactions>>(it, transactionsType) },
            cardDetails = entity.cardDetails?.let { gson.fromJson<com.retail.dolphinpos.domain.model.home.create_order.CardDetails>(it, cardDetailsType) },
            taxDetails = entity.taxDetails?.let { gson.fromJson<List<com.retail.dolphinpos.domain.model.TaxDetail>>(it, taxDetailsType) },
            taxExempt = entity.taxExempt
        )
    }

    // DEPRECATED: Order numbers should be generated in HomeViewModel.generateOrderNumber()
    // This function is kept for reference but should not be used
    @Deprecated("Use HomeViewModel.generateOrderNumber() instead")
    private fun generateOrderNo(storeId: Int, locationId:Int, registerId: Int, userId: Int): String {
        val epochMillis = System.currentTimeMillis()
        return "S${storeId}L${locationId}R${registerId}U${userId}-$epochMillis"
    }

    fun parseOrderItemsFromJson(json: String): List<com.retail.dolphinpos.domain.model.home.create_order.CheckOutOrderItem> {
        val type = object : TypeToken<List<com.retail.dolphinpos.domain.model.home.create_order.CheckOutOrderItem>>() {}.type
        return gson.fromJson(json, type)
    }

    fun convertOrderItemsToJson(items: List<com.retail.dolphinpos.domain.model.home.create_order.CheckOutOrderItem>): String {
        return gson.toJson(items)
    }

    fun convertToPendingOrder(entity: PendingOrderEntity): PendingOrder {
        val itemsType = object : TypeToken<List<com.retail.dolphinpos.domain.model.home.create_order.CheckOutOrderItem>>() {}.type
        val discountIdsType = object : TypeToken<List<Int>>() {}.type
        val transactionsType = object : TypeToken<List<com.retail.dolphinpos.domain.model.home.create_order.CheckoutSplitPaymentTransactions>>() {}.type
        val cardDetailsType = object : TypeToken<com.retail.dolphinpos.domain.model.home.create_order.CardDetails>() {}.type
        val taxDetailsType = object : TypeToken<List<com.retail.dolphinpos.domain.model.TaxDetail>>() {}.type

        val items: List<com.retail.dolphinpos.domain.model.home.create_order.CheckOutOrderItem> =
            gson.fromJson<List<com.retail.dolphinpos.domain.model.home.create_order.CheckOutOrderItem>>(entity.items, itemsType)
                ?: emptyList()
        val discountIds: List<Int>? = entity.discountIds?.let {
            gson.fromJson(it, discountIdsType)
        }
        val transactions: List<com.retail.dolphinpos.domain.model.home.create_order.CheckoutSplitPaymentTransactions>? = entity.transactions?.let {
            gson.fromJson(it, transactionsType)
        }
        val cardDetails: com.retail.dolphinpos.domain.model.home.create_order.CardDetails? = entity.cardDetails?.let {
            gson.fromJson<com.retail.dolphinpos.domain.model.home.create_order.CardDetails>(it, cardDetailsType)
        }
        val taxDetails: List<com.retail.dolphinpos.domain.model.TaxDetail>? = entity.taxDetails?.let {
            gson.fromJson(it, taxDetailsType)
        }

        return PendingOrder(
            id = entity.id,
            orderNumber = entity.orderNumber,
            invoiceNo = entity.invoiceNo,
            customerId = entity.customerId,
            storeId = entity.storeId,
            locationId = entity.locationId,
            storeRegisterId = entity.storeRegisterId,
            batchNo = entity.batchNo,
            paymentMethod = entity.paymentMethod,
            isRedeemed = entity.isRedeemed,
            source = entity.source,
            redeemPoints = entity.redeemPoints,
            items = items,
            subTotal = entity.subTotal,
            total = entity.total,
            applyTax = entity.applyTax,
            taxValue = entity.taxValue,
            discountAmount = entity.discountAmount,
            cashDiscountAmount = entity.cashDiscountAmount,
            rewardDiscount = entity.rewardDiscount,
            discountIds = discountIds,
            transactionId = entity.transactionId,
            userId = entity.userId,
            voidReason = entity.voidReason,
            isVoid = entity.isVoid,
            transactions = transactions,
            cardDetails = cardDetails,
            createdAt = entity.createdAt,
            isSynced = entity.isSynced,
            taxDetails = taxDetails,
            taxExempt = entity.taxExempt
        )
    }
}

