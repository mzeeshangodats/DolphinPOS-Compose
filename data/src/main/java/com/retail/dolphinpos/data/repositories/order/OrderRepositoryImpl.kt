package com.retail.dolphinpos.data.repositories.order

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.retail.dolphinpos.data.dao.OrderDao
import com.retail.dolphinpos.data.entities.order.OrderEntity
import com.retail.dolphinpos.data.entities.order.OrderSyncStatus
import com.retail.dolphinpos.data.service.ApiService
import com.retail.dolphinpos.data.util.safeApiCallResult
import com.retail.dolphinpos.domain.model.home.create_order.CreateOrderRequest
import com.retail.dolphinpos.domain.model.home.create_order.CreateOrderResponse
import com.retail.dolphinpos.domain.model.home.create_order.CardDetails
import com.retail.dolphinpos.domain.model.home.create_order.CheckOutOrderItem
import com.retail.dolphinpos.domain.model.home.create_order.CheckoutSplitPaymentTransactions
import com.retail.dolphinpos.domain.model.home.order_details.OrderDetailList
import com.retail.dolphinpos.domain.model.order.PendingOrder
import com.retail.dolphinpos.domain.model.home.create_order.TaxDetail
import java.lang.reflect.Type

class OrderRepositoryImpl(
    private val orderDao: OrderDao,
    private val apiService: ApiService,
    private val gson: Gson
) {

    /**
     * Save order to local database (offline)
     * Sets isSynced = false, status = "pending", orderSource = "local"
     */
    suspend fun saveOrderToLocal(orderRequest: CreateOrderRequest): Long {
        val orderEntity = OrderEntity(
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
            taxDetails = orderRequest.taxDetails?.let { gson.toJson(it) },
            userId = orderRequest.userId,
            voidReason = orderRequest.voidReason,
            isVoid = orderRequest.isVoid,
            orderSource = "local",
            syncStatus = OrderSyncStatus.LOCAL_ONLY,  // ✅ Set explicit sync status for offline orders
            isSynced = false,  // ✅ isSynced = 0 for offline orders (legacy field)
            status = "pending"
        )
        return orderDao.insertOrder(orderEntity)
    }

    /**
     * Sync order to server
     * On success, updates isSynced = true, status = "completed"
     */
    suspend fun syncOrderToServer(order: OrderEntity): Result<CreateOrderResponse> {
        return try {
            val orderRequest = convertToCreateOrderRequest(order)
            val result = safeApiCallResult(
                apiCall = { apiService.createOrder(orderRequest) },
                defaultMessage = "Order creation failed"
            )

            // Mark as synced only if successful
            // Note: CreateOrderResponse only contains a message, not the order ID
            // The serverId will be updated when orders are fetched from the API
            result.onSuccess { response ->
                val updatedOrder = order.copy(
                    isSynced = true,  // ✅ Update isSynced = 1
                    status = "completed",  // ✅ Update status to completed
                    // serverId remains null for now, will be updated when fetching orders from API
                    updatedAt = System.currentTimeMillis()
                )
                orderDao.updateOrder(updatedOrder)
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all unsynced local orders (for WorkManager sync)
     */
    suspend fun getUnsyncedLocalOrders(): List<OrderEntity> {
        return orderDao.getUnsyncedLocalOrders()
    }

    /**
     * Save API orders to local database
     * Sets orderSource = "api", isSynced from API response (1 = completed, 0 = pending)
     */
    suspend fun saveApiOrders(orders: List<OrderDetailList>) {
        orders.forEach { apiOrder ->
            val orderEntity = convertApiOrderToEntity(apiOrder)
            // Check if order with same serverId already exists
            val existingOrder = if (orderEntity.serverId != null) {
                orderDao.getOrderByServerId(orderEntity.serverId)
            } else {
                null
            }
            
            if (existingOrder != null) {
                // Update existing order with new data
                val updatedOrder = existingOrder.copy(
                    orderNumber = orderEntity.orderNumber,
                    invoiceNo = orderEntity.invoiceNo ?: existingOrder.invoiceNo,
                    customerId = orderEntity.customerId ?: existingOrder.customerId,
                    storeId = orderEntity.storeId,
                    locationId = orderEntity.locationId,
                    storeRegisterId = orderEntity.storeRegisterId ?: existingOrder.storeRegisterId,
                    batchNo = orderEntity.batchNo ?: existingOrder.batchNo,
                    paymentMethod = orderEntity.paymentMethod,
                    transactionId = orderEntity.transactionId ?: existingOrder.transactionId,
                    items = orderEntity.items,
                    subTotal = orderEntity.subTotal,
                    total = orderEntity.total,
                    applyTax = orderEntity.applyTax,
                    taxValue = orderEntity.taxValue,
                    discountAmount = orderEntity.discountAmount,
                    cashDiscountAmount = orderEntity.cashDiscountAmount,
                    rewardDiscount = orderEntity.rewardDiscount,
                    discountIds = orderEntity.discountIds ?: existingOrder.discountIds,
                    transactions = orderEntity.transactions ?: existingOrder.transactions,
                    cardDetails = orderEntity.cardDetails ?: existingOrder.cardDetails,
                    userId = orderEntity.userId,
                    voidReason = orderEntity.voidReason ?: existingOrder.voidReason,
                    isVoid = orderEntity.isVoid,
                    orderSource = "api",
                    isSynced = orderEntity.isSynced,
                    status = orderEntity.status,
                    isRedeemed = orderEntity.isRedeemed,
                    source = orderEntity.source,
                    redeemPoints = orderEntity.redeemPoints ?: existingOrder.redeemPoints,
                    updatedAt = System.currentTimeMillis()
                )
                orderDao.updateOrder(updatedOrder)
            } else {
                // Check if order with same orderNumber exists (for local orders that were synced)
                val existingByOrderNumber = orderDao.getOrderByOrderNumber(orderEntity.orderNumber)
                if (existingByOrderNumber != null && existingByOrderNumber.serverId == null) {
                    // Update local order with serverId and mark as synced
                    val updatedOrder = existingByOrderNumber.copy(
                        serverId = orderEntity.serverId,
                        orderSource = "api",
                        isSynced = true,
                        status = "completed",
                        updatedAt = System.currentTimeMillis()
                    )
                    orderDao.updateOrder(updatedOrder)
                } else if (existingByOrderNumber == null) {
                    // New order, insert it
                    orderDao.insertOrder(orderEntity)
                }
                // If existingByOrderNumber exists with serverId, skip (already exists)
            }
        }
    }

    /**
     * Convert API OrderDetailList to OrderEntity
     */
    private fun convertApiOrderToEntity(apiOrder: OrderDetailList): OrderEntity {
        // Convert OrderItem list to JSON
        val itemsJson = gson.toJson(apiOrder.orderItems.map { orderItem ->
            CheckOutOrderItem(
                productId = orderItem.product.id,
                quantity = orderItem.quantity,
                productVariantId = if (orderItem.productVariant is Int) orderItem.productVariant as Int else null,
                name = orderItem.product.name,
                isCustom = false,
                price = orderItem.price.toDoubleOrNull() ?: 0.0,
                barCode = null,
                reason = null,
                discountId = null,
                discountedPrice = orderItem.discountedPrice.toDoubleOrNull(),
                discountedAmount = if (orderItem.isDiscounted) {
                    (orderItem.price.toDoubleOrNull() ?: 0.0) - (orderItem.discountedPrice.toDoubleOrNull() ?: 0.0)
                } else null,
                fixedDiscount = 0.0,
                discountReason = null,
                fixedPercentageDiscount = 0.0,
                discountType = "",
                cardPrice = orderItem.price.toDoubleOrNull() ?: 0.0
            )
        })

        // Determine isSynced from status
        // If status is "completed", isSynced = true (1), else false (0)
        val isSynced = apiOrder.status.lowercase() == "completed"
        val orderStatus = if (isSynced) "completed" else "pending"

        return OrderEntity(
            serverId = apiOrder.id,
            orderNumber = apiOrder.orderNumber,
            invoiceNo = null,
            customerId = if (apiOrder.customerId is Int) apiOrder.customerId as Int else null,
            storeId = apiOrder.storeId,
            locationId = apiOrder.locationId,
            storeRegisterId = apiOrder.storeRegisterId,
            batchNo = apiOrder.batchId?.toString()?.takeIf { it != "null" && it.isNotEmpty() },
            paymentMethod = apiOrder.paymentMethod,
            isRedeemed = apiOrder.isRedeemed,
            source = apiOrder.source,
            redeemPoints = if (apiOrder.redeemPoints is Int) apiOrder.redeemPoints as Int else null,
            items = itemsJson,
            subTotal = apiOrder.subTotal.toDoubleOrNull() ?: 0.0,
            total = apiOrder.total.toDoubleOrNull() ?: 0.0,
            applyTax = apiOrder.applyTax,
            taxValue = apiOrder.taxValue,
            discountAmount = apiOrder.discountAmount.toDoubleOrNull() ?: 0.0,
            cashDiscountAmount = if (apiOrder.cashDiscountAmount is Double) apiOrder.cashDiscountAmount as Double else 0.0,
            rewardDiscount = if (apiOrder.rewardDiscount is Double) apiOrder.rewardDiscount as Double else 0.0,
            discountIds = null,
            transactionId = null,
            transactions = null,
            cardDetails = null,
            userId = if (apiOrder.cashierId is Int) apiOrder.cashierId as Int else 0,
            voidReason = if (apiOrder.voidReason is String) apiOrder.voidReason as String else null,
            isVoid = apiOrder.status.lowercase() == "void",
            orderSource = "api",
            isSynced = isSynced,  // ✅ From API: 1 = completed, 0 = pending
            status = orderStatus,
            createdAt = parseDateToTimestamp(apiOrder.createdAt),
            updatedAt = parseDateToTimestamp(apiOrder.updatedAt)
        )
    }

    /**
     * Get all orders from local database
     */
    suspend fun getAllOrders(): List<OrderEntity> {
        return orderDao.getAllOrders()
    }

    /**
     * Get orders by store ID
     */
    suspend fun getOrdersByStoreId(storeId: Int): List<OrderEntity> {
        return orderDao.getOrdersByStoreId(storeId)
    }

    /**
     * Get orders by sync status
     */
    suspend fun getOrdersBySyncStatus(isSynced: Boolean): List<OrderEntity> {
        return orderDao.getOrdersBySyncStatus(isSynced)
    }

    /**
     * Get orders by source (api or local)
     */
    suspend fun getOrdersBySource(source: String): List<OrderEntity> {
        return orderDao.getOrdersBySource(source)
    }

    /**
     * Search orders by keyword
     */
    suspend fun searchOrdersByStoreId(storeId: Int, keyword: String): List<OrderEntity> {
        return orderDao.searchOrdersByStoreId(storeId, keyword)
    }

    /**
     * Get order by ID
     */
    suspend fun getOrderById(orderId: Long): OrderEntity? {
        return orderDao.getOrderById(orderId)
    }

    /**
     * Get order by server ID
     */
    suspend fun getOrderByServerId(serverId: Int): OrderEntity? {
        return orderDao.getOrderByServerId(serverId)
    }

    /**
     * Get order by order number
     */
    suspend fun getOrderByOrderNumber(orderNumber: String): OrderEntity? {
        return orderDao.getOrderByOrderNumber(orderNumber)
    }

    /**
     * Get latest order
     */
    suspend fun getLatestOrder(): OrderEntity? {
        return orderDao.getLatestOrder()
    }

    /**
     * Update order
     */
    suspend fun updateOrder(order: OrderEntity) {
        orderDao.updateOrder(order)
    }

    /**
     * Delete order
     */
    suspend fun deleteOrder(orderId: Long) {
        orderDao.deleteOrderById(orderId)
    }

    /**
     * Clean up duplicate orders
     * Removes duplicate orders based on order_no, keeping the best record
     */
    suspend fun removeDuplicateOrders() {
        orderDao.removeDuplicateOrders()
    }

    /**
     * Convert OrderEntity to CreateOrderRequest
     */
    private fun convertToCreateOrderRequest(entity: OrderEntity): CreateOrderRequest {
        val itemsType = object : TypeToken<List<CheckOutOrderItem>>() {}.type
        val discountIdsType = object : TypeToken<List<Int>>() {}.type
        val transactionsType = object : TypeToken<List<CheckoutSplitPaymentTransactions>>() {}.type
        val cardDetailsType = object : TypeToken<CardDetails>() {}.type
        val taxDetailsType = object : TypeToken<List<TaxDetail>>() {}.type

        val items: List<CheckOutOrderItem> = decodeJson(entity.items, itemsType) ?: emptyList()
        val discountIds: List<Int>? = entity.discountIds?.let { decodeJson(it, discountIdsType) }
        val transactions: List<CheckoutSplitPaymentTransactions>? = entity.transactions?.let { decodeJson(it, transactionsType) }
        val cardDetails: CardDetails? = entity.cardDetails?.let { decodeJson(it, cardDetailsType) }
        val taxDetails: List<TaxDetail>? = entity.taxDetails?.let { decodeJson(it, taxDetailsType) }

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
            taxDetails = taxDetails,
            taxExempt = !entity.applyTax || entity.taxValue == 0.0
        )
    }

    /**
     * Convert OrderEntity to PendingOrder (for backward compatibility)
     */
    fun convertToPendingOrder(entity: OrderEntity): PendingOrder {
        val itemsType = object : TypeToken<List<CheckOutOrderItem>>() {}.type
        val discountIdsType = object : TypeToken<List<Int>>() {}.type
        val transactionsType = object : TypeToken<List<CheckoutSplitPaymentTransactions>>() {}.type
        val cardDetailsType = object : TypeToken<CardDetails>() {}.type
        val taxDetailsType = object : TypeToken<List<TaxDetail>>() {}.type

        val items: List<CheckOutOrderItem> = decodeJson(entity.items, itemsType) ?: emptyList()
        val discountIds: List<Int>? = entity.discountIds?.let { decodeJson(it, discountIdsType) }
        val transactions: List<CheckoutSplitPaymentTransactions>? = entity.transactions?.let { decodeJson(it, transactionsType) }
        val cardDetails: CardDetails? = entity.cardDetails?.let { decodeJson(it, cardDetailsType) }
        val taxDetails: List<TaxDetail>? = entity.taxDetails?.let { decodeJson(it, taxDetailsType) }

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
            taxDetails = taxDetails ?: emptyList(),
            taxExempt = !entity.applyTax || entity.taxValue == 0.0
        )
    }

    /**
     * Parse date string to timestamp
     */
    private fun parseDateToTimestamp(dateString: String): Long {
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
            format.parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    /**
     * Decode JSON string to object
     */
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

