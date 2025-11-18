package com.retail.dolphinpos.presentation.features.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.retail.dolphinpos.common.network.NetworkMonitor
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.data.entities.order.OrderEntity
import com.retail.dolphinpos.data.repositories.order.OrderRepositoryImpl
import com.retail.dolphinpos.domain.model.home.create_order.CheckOutOrderItem
import com.retail.dolphinpos.domain.model.home.create_order.CheckoutSplitPaymentTransactions
import com.retail.dolphinpos.domain.model.home.order_details.OrderDetailList
import com.retail.dolphinpos.domain.model.home.order_details.OrderDetailTransaction
import com.retail.dolphinpos.domain.model.home.order_details.OrderItem
import com.retail.dolphinpos.domain.model.home.order_details.Product
import com.retail.dolphinpos.domain.repositories.home.OrdersRepository
import com.retail.dolphinpos.domain.usecases.order.GetPrintableOrderFromOrderDetailUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.PrintOrderReceiptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val ordersRepository: OrdersRepository,
    private val orderRepository: OrderRepositoryImpl,
    private val preferenceManager: PreferenceManager,
    private val getPrintableOrderFromOrderDetailUseCase: GetPrintableOrderFromOrderDetailUseCase,
    private val printOrderReceiptUseCase: PrintOrderReceiptUseCase,
    private val networkMonitor: NetworkMonitor,
    private val gson: Gson
) : ViewModel() {

    private val _orders = MutableStateFlow<List<OrderDetailList>>(emptyList())
    val orders: StateFlow<List<OrderDetailList>> = _orders.asStateFlow()

    private val _completedOrders = MutableStateFlow<List<OrderDetailList>>(emptyList())
    val completedOrders: StateFlow<List<OrderDetailList>> = _completedOrders.asStateFlow()

    private val _pendingOrders = MutableStateFlow<List<OrderDetailList>>(emptyList())
    val pendingOrders: StateFlow<List<OrderDetailList>> = _pendingOrders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uiEvent = MutableSharedFlow<OrdersUiEvent>()
    val uiEvent: SharedFlow<OrdersUiEvent> = _uiEvent.asSharedFlow()

    fun loadOrders(keyword: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _uiEvent.emit(OrdersUiEvent.ShowLoading)
            try {
                // If internet is available, fetch orders from API and save to unified table
                if (networkMonitor.isNetworkAvailable()) {
                    loadOrdersFromApi(keyword)
                }
                
                // Always load orders from unified table (works for both online and offline)
                loadOrdersFromLocalDatabase(keyword)
            } catch (e: Exception) {
                _isLoading.value = false
                _uiEvent.emit(OrdersUiEvent.HideLoading)
                _uiEvent.emit(OrdersUiEvent.ShowError("Failed to load orders: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadOrdersFromApi(keyword: String?) {
        try {
            // Get date range (last 30 days by default)
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

            // End date is today
            val endDate = dateFormat.format(calendar.time)

            // Start date is 30 days ago
            calendar.add(Calendar.DAY_OF_YEAR, -30)
            val startDate = dateFormat.format(calendar.time)

            val storeId = preferenceManager.getStoreID()

            val result = ordersRepository.getOrdersDetails(
                orderBy = "createdAt",
                order = "desc",
                startDate = startDate,
                endDate = endDate,
                limit = 100, // Load 100 orders at a time
                page = 1,
                paginate = true,
                storeId = storeId,
                keyword = keyword
            )
            result.onSuccess { response ->
                // Save API orders to unified table
                orderRepository.saveApiOrders(response.data.list)
                // Clean up any duplicate orders
                orderRepository.removeDuplicateOrders()
                // Orders will be loaded from local database in loadOrdersFromLocalDatabase
            }.onFailure { e ->
                // Even if API fails, we can still load from local database
                android.util.Log.e("OrdersViewModel", "Failed to fetch orders from API: ${e.message}")
            }
        } catch (e: Exception) {
            android.util.Log.e("OrdersViewModel", "Error loading orders from API: ${e.message}")
        }
    }

    private suspend fun loadOrdersFromLocalDatabase(keyword: String?) {
        try {
            val storeId = preferenceManager.getStoreID()
            
            // Clean up any duplicate orders before loading
            orderRepository.removeDuplicateOrders()

            // Get all orders from unified table
            val allOrders = if (keyword.isNullOrBlank()) {
                orderRepository.getOrdersByStoreId(storeId)
            } else {
                orderRepository.searchOrdersByStoreId(storeId, keyword)
            }

            // Convert OrderEntity to OrderDetailList and group by isSynced status
            val completedList = mutableListOf<OrderDetailList>()
            val pendingList = mutableListOf<OrderDetailList>()
            
            allOrders.forEach { entity ->
                val orderDetail = convertOrderEntityToOrderDetailList(entity)
                if (entity.isSynced) {
                    completedList.add(orderDetail)
                } else {
                    pendingList.add(orderDetail)
                }
            }

            _orders.value = allOrders.map { convertOrderEntityToOrderDetailList(it) }
            _completedOrders.value = completedList
            _pendingOrders.value = pendingList
            _uiEvent.emit(OrdersUiEvent.HideLoading)
        } catch (e: Exception) {
            _uiEvent.emit(OrdersUiEvent.HideLoading)
            _uiEvent.emit(OrdersUiEvent.ShowError("Failed to load orders from local storage: ${e.message}"))
        }
    }

    private fun convertOrderEntityToOrderDetailList(entity: OrderEntity): OrderDetailList {
        // Parse items from JSON
        val itemsType = object : TypeToken<List<CheckOutOrderItem>>() {}.type
        val orderItems: List<CheckOutOrderItem> = try {
            gson.fromJson(entity.items, itemsType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        // Parse transactions from JSON
        val transactionsType = object : TypeToken<List<CheckoutSplitPaymentTransactions>>() {}.type
        val splitTransactions: List<CheckoutSplitPaymentTransactions> = try {
            if (!entity.transactions.isNullOrBlank()) {
                gson.fromJson(entity.transactions, transactionsType) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }

        // Convert CheckOutOrderItem to OrderItem
        val convertedOrderItems = orderItems.map { item ->
            OrderItem(
                discountedPrice = item.discountedPrice?.toString() ?: "0.0",
                isDiscounted = item.discountedPrice != null && item.discountedPrice!! > 0,
                price = item.price?.toString() ?: "0.0",
                product = Product(
                    id = item.productId ?: 0,
                    images = emptyList(), // Images not stored in OnlineOrderEntity
                    name = item.name ?: ""
                ),
                productVariant = item.productVariantId ?: Unit,
                quantity = item.quantity ?: 0,
                refundPrice = 0.0,
                refundQuantity = 0
            )
        }

        // Convert CheckoutSplitPaymentTransactions to OrderDetailTransaction
        val convertedTransactions = splitTransactions.mapIndexed { index, splitTransaction ->
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val nowString = dateFormat.format(Date())
            
            OrderDetailTransaction(
                amount = splitTransaction.amount.toString(),
                batch = Unit,
                batchId = entity.batchNo ?: Unit,
                cardDetails = splitTransaction.cardDetails ?: com.retail.dolphinpos.domain.model.home.create_order.CardDetails(),
                createdAt = nowString,
                id = index + 1,
                invoiceNo = splitTransaction.invoiceNo ?: entity.invoiceNo ?: "",
                locationId = entity.locationId,
                orderId = entity.serverId ?: entity.id.toInt(),
                orderSource = entity.orderSource,
                paymentMethod = splitTransaction.paymentMethod,
                status = if (entity.isSynced) "completed" else "pending",
                storeId = entity.storeId,
                tax = splitTransaction.taxAmount ?: 0.0, // Use taxAmount from split transaction
                tip = Unit,
                updatedAt = nowString,
                userId = entity.userId
            )
        }

        // Format createdAt date
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val createdAtString = dateFormat.format(Date(entity.createdAt))

        // Format updatedAt (use updatedAt if available, otherwise use createdAt)
        val updatedAtString = dateFormat.format(Date(entity.updatedAt))

        // Determine status based on isSynced: isSynced = 1 (true) -> "completed", isSynced = 0 (false) -> "pending"
        val orderStatus: String = when {
            entity.isVoid -> "void"
            entity.isSynced -> "completed"  // isSynced = 1 (true) -> completed
            else -> "pending"  // isSynced = 0 (false) -> pending
        }

        return OrderDetailList(
            applyTax = entity.applyTax,
            batchId = entity.batchNo ?: "",
            cashDiscountAmount = entity.cashDiscountAmount,
            cashier = Unit,
            cashierId = entity.userId,
            createdAt = createdAtString,
            customer = Unit,
            customerId = entity.customerId ?: Unit,
            customerPointsEarned = 0,
            deletedAt = Unit,
            discountAmount = entity.discountAmount.toString(),
            discountId = 0,
            giftyCardId = 0,
            id = entity.serverId?.toLong()?.toInt() ?: entity.id.toInt(),
            isRedeemed = entity.isRedeemed,
            locationId = entity.locationId,
            loyaltyPoints = 0,
            note = "",
            orderItems = convertedOrderItems,
            orderNumber = entity.orderNumber,
            paymentMethod = entity.paymentMethod,
            paymentStatus = if (entity.isSynced) "completed" else "pending",
            redeemPoints = entity.redeemPoints ?: 0,
            refundedDiscount = "0.0",
            refundedSubTotal = "0.0",
            refundedTax = 0.0,
            refundedTotal = "0.0",
            rewardDetails = Unit,
            rewardDiscount = entity.rewardDiscount,
            rewardRefundDiscount = 0.0,
            source = entity.source,
            status = orderStatus,
            storeId = entity.storeId,
            storeRegisterId = entity.storeRegisterId ?: 0,
            subTotal = entity.subTotal.toString(),
            taxValue = entity.taxValue,
            timeline = Unit,
            total = entity.total.toString(),
            transactions = convertedTransactions, // Parse transactions from entity
            updatedAt = updatedAtString,
            voidReason = entity.voidReason ?: "",
            wpId = 0
        )
    }

    init {
        loadOrders()
    }

    fun printOrder(order: OrderDetailList) {
        viewModelScope.launch {
            _uiEvent.emit(OrdersUiEvent.ShowLoading)
            try {
                // Try to get the order from database first to preserve tax details
                val orderEntity = orderRepository.getOrderByOrderNumber(order.orderNumber)
                val pendingOrder = if (orderEntity != null) {
                    // Use convertToPendingOrder which includes tax details
                    android.util.Log.d("OrdersViewModel", "Printing order from database: ${order.orderNumber}, Tax details: ${orderEntity.taxDetails}")
                    orderRepository.convertToPendingOrder(orderEntity)
                } else {
                    // Fallback to conversion from OrderDetailList if order not found in database
                    android.util.Log.d("OrdersViewModel", "Order not found in database, using OrderDetailList conversion")
                    getPrintableOrderFromOrderDetailUseCase(order)
                }
                
                android.util.Log.d("OrdersViewModel", "Printing order: ${pendingOrder.orderNumber}, Subtotal: ${pendingOrder.subTotal}, Tax: ${pendingOrder.taxValue}, Tax details count: ${pendingOrder.taxDetails?.size ?: 0}")
                val statusMessages = mutableListOf<String>()
                val result = printOrderReceiptUseCase(pendingOrder) { statusMessages.add(it) }
                if (result.isSuccess) {
                    val successMessage =
                        statusMessages.lastOrNull { it.contains("success", ignoreCase = true) }
                            ?: "Print command sent successfully."
                    _uiEvent.emit(OrdersUiEvent.ShowSuccess(successMessage))
                } else {
                    val errorMessage = result.exceptionOrNull()?.message
                        ?: statusMessages.lastOrNull { message ->
                            val normalized = message.lowercase(Locale.US)
                            normalized.contains("error") || normalized.contains("fail")
                        }
                        ?: "Failed to print receipt."
                    _uiEvent.emit(OrdersUiEvent.ShowError(errorMessage))
                }
            } catch (e: Exception) {
                android.util.Log.e("OrdersViewModel", "Error printing order: ${e.message}", e)
                _uiEvent.emit(OrdersUiEvent.ShowError(e.message ?: "Failed to print receipt."))
            } finally {
                _uiEvent.emit(OrdersUiEvent.HideLoading)
            }
        }
    }
}

