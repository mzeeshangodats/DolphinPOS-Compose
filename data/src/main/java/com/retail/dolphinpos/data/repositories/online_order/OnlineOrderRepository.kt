package com.retail.dolphinpos.data.repositories.online_order

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.retail.dolphinpos.data.dao.OnlineOrderDao
import com.retail.dolphinpos.data.entities.order.OnlineOrderEntity
import com.retail.dolphinpos.domain.model.home.create_order.CardDetails
import com.retail.dolphinpos.domain.model.home.create_order.CheckOutOrderItem
import com.retail.dolphinpos.domain.model.home.create_order.CheckoutSplitPaymentTransactions
import com.retail.dolphinpos.domain.model.home.create_order.CreateOrderRequest
import com.retail.dolphinpos.domain.model.order.PendingOrder
import java.lang.reflect.Type

class OnlineOrderRepository(
    private val onlineOrderDao: OnlineOrderDao,
    private val gson: Gson
) {

    suspend fun saveSuccessfulOrder(orderRequest: CreateOrderRequest): Long {
        val orderEntity = OnlineOrderEntity(
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
            isVoid = orderRequest.isVoid
        )
        return onlineOrderDao.insertOnlineOrder(orderEntity)
    }

    suspend fun saveSuccessfulOrderFromEntity(pendingOrder: com.retail.dolphinpos.data.entities.order.PendingOrderEntity): Long {
        val orderEntity = OnlineOrderEntity(
            orderNumber = pendingOrder.orderNumber,
            invoiceNo = pendingOrder.invoiceNo,
            customerId = pendingOrder.customerId,
            storeId = pendingOrder.storeId,
            locationId = pendingOrder.locationId,
            storeRegisterId = pendingOrder.storeRegisterId,
            batchNo = pendingOrder.batchNo,
            paymentMethod = pendingOrder.paymentMethod,
            isRedeemed = pendingOrder.isRedeemed,
            source = pendingOrder.source,
            redeemPoints = pendingOrder.redeemPoints,
            items = gson.toJson(pendingOrder.items),
            subTotal = pendingOrder.subTotal,
            total = pendingOrder.total,
            applyTax = pendingOrder.applyTax,
            taxValue = pendingOrder.taxValue,
            discountAmount = pendingOrder.discountAmount,
            cashDiscountAmount = pendingOrder.cashDiscountAmount,
            rewardDiscount = pendingOrder.rewardDiscount,
            discountIds = pendingOrder.discountIds?.let { gson.toJson(it) },
            transactionId = pendingOrder.transactionId,
            transactions = pendingOrder.transactions?.let { gson.toJson(it) },
            cardDetails = pendingOrder.cardDetails?.let { gson.toJson(it) },
            userId = pendingOrder.userId,
            voidReason = pendingOrder.voidReason,
            isVoid = pendingOrder.isVoid
        )
        return onlineOrderDao.insertOnlineOrder(orderEntity)
    }

    suspend fun getAllOnlineOrders(): List<OnlineOrderEntity> {
        return onlineOrderDao.getAllOnlineOrders()
    }

    suspend fun getLatestOnlineOrder(): OnlineOrderEntity? {
        return onlineOrderDao.getLatestOnlineOrder()
    }

    suspend fun getOrderById(orderId: Long): OnlineOrderEntity? {
        return onlineOrderDao.getOrderById(orderId)
    }

    suspend fun getOrderByOrderNumber(orderNumber: String): OnlineOrderEntity? {
        return onlineOrderDao.getOrderByOrderNumber(orderNumber)
    }

    suspend fun getOrdersByStoreId(storeId: Int): List<OnlineOrderEntity> {
        return onlineOrderDao.getOrdersByStoreId(storeId)
    }

    suspend fun getOrdersByLocationId(locationId: Int): List<OnlineOrderEntity> {
        return onlineOrderDao.getOrdersByLocationId(locationId)
    }

    suspend fun getOrdersByUserId(userId: Int): List<OnlineOrderEntity> {
        return onlineOrderDao.getOrdersByUserId(userId)
    }

    suspend fun deleteOrder(orderId: Long) {
        onlineOrderDao.deleteOrderById(orderId)
    }

    fun convertToPendingOrder(entity: OnlineOrderEntity): PendingOrder {
        val itemsType = object : TypeToken<List<CheckOutOrderItem>>() {}.type
        val discountIdsType = object : TypeToken<List<Int>>() {}.type
        val transactionsType = object : TypeToken<List<CheckoutSplitPaymentTransactions>>() {}.type
        val cardDetailsType = object : TypeToken<CardDetails>() {}.type

        val items: List<CheckOutOrderItem> = decodeJson(entity.items, itemsType) ?: emptyList()
        val discountIds: List<Int>? = decodeJson(entity.discountIds, discountIdsType)
        val transactions: List<CheckoutSplitPaymentTransactions>? = decodeJson(entity.transactions, transactionsType)
        val cardDetails: CardDetails? = decodeJson(entity.cardDetails, cardDetailsType)

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
            isSynced = true
        )
    }

    private fun <T> decodeJson(raw: String?, type: Type): T? {
        if (raw.isNullOrBlank()) return null
        return try {
            gson.fromJson<T>(raw, type)
        } catch (_: JsonSyntaxException) {
            try {
                val inner = gson.fromJson(raw, String::class.java)
                if (inner.isNullOrBlank()) null else gson.fromJson(inner, type)
            } catch (e: Exception) {
                null
            }
        }
    }
}

