package com.retail.dolphinpos.data.repositories.online_order

import com.google.gson.Gson
import com.retail.dolphinpos.data.dao.OnlineOrderDao
import com.retail.dolphinpos.data.entities.order.OnlineOrderEntity
import com.retail.dolphinpos.domain.model.home.create_order.CreateOrderRequest

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
            items = pendingOrder.items,
            subTotal = pendingOrder.subTotal,
            total = pendingOrder.total,
            applyTax = pendingOrder.applyTax,
            taxValue = pendingOrder.taxValue,
            discountAmount = pendingOrder.discountAmount,
            cashDiscountAmount = pendingOrder.cashDiscountAmount,
            rewardDiscount = pendingOrder.rewardDiscount,
            discountIds = pendingOrder.discountIds,
            userId = pendingOrder.userId,
            voidReason = pendingOrder.voidReason,
            isVoid = pendingOrder.isVoid
        )
        return onlineOrderDao.insertOnlineOrder(orderEntity)
    }

    suspend fun getAllOnlineOrders(): List<OnlineOrderEntity> {
        return onlineOrderDao.getAllOnlineOrders()
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
}

