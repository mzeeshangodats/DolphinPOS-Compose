package com.retail.dolphinpos.presentation.features.ui.orders

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.retail.dolphinpos.common.network.NetworkMonitor
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.presentation.R
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
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

    private val _startDate = MutableStateFlow<String?>(null)
    val startDate: StateFlow<String?> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow<String?>(null)
    val endDate: StateFlow<String?> = _endDate.asStateFlow()

    private val _orderDetail = MutableStateFlow<OrderDetailList?>(null)
    val orderDetail: StateFlow<OrderDetailList?> = _orderDetail.asStateFlow()

    fun setStartDate(date: String) {
        _startDate.value = date
    }

    fun setEndDate(date: String) {
        _endDate.value = date
    }

    fun loadOrders(keyword: String? = null, reset: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true
            _uiEvent.emit(OrdersUiEvent.ShowLoading)
            
            // Clear orders when resetting to ensure clean refresh
            if (reset) {
                _orders.value = emptyList()
            }
            
            // Check internet connection
            if (!networkMonitor.isNetworkAvailable()) {
                _isLoading.value = false
                _uiEvent.emit(OrdersUiEvent.HideLoading)
                _uiEvent.emit(
                    OrdersUiEvent.ShowNoInternetDialog(context.getString(R.string.no_internet_connection))
                )
                // Still load from local database for offline functionality
                loadOrdersFromLocalDatabase(keyword)
                return@launch
            }
            
            try {
                // If internet is available, fetch orders from API and save to unified table
                loadOrdersFromApi(keyword)
                
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
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val calendar = Calendar.getInstance()

            // Use selected dates, or default to last 1 day
            val endDateStr = _endDate.value ?: run {
                dateFormat.format(calendar.time)
            }

            val startDateStr = _startDate.value ?: run {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                dateFormat.format(calendar.time)
            }

            val storeId = preferenceManager.getStoreID()

            val result = ordersRepository.getOrdersDetails(
                orderBy = "createdAt",
                order = "desc",
                startDate = startDateStr,
                endDate = endDateStr,
                limit = 10,
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

            // Filter orders by date range
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val calendar = Calendar.getInstance()
            
            val startDateStr = _startDate.value
            val endDateStr = _endDate.value
            
            val filteredOrders = if (startDateStr != null && endDateStr != null) {
                try {
                    // Parse start date (beginning of day: 00:00:00.000)
                    val startDate = dateFormat.parse(startDateStr)
                    calendar.time = startDate
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startTimestamp = calendar.timeInMillis
                    
                    // Parse end date (end of day: 23:59:59.999)
                    val endDate = dateFormat.parse(endDateStr)
                    calendar.time = endDate
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.set(Calendar.MILLISECOND, 999)
                    val endTimestamp = calendar.timeInMillis
                    
                    // Filter orders within date range
                    allOrders.filter { order ->
                        order.createdAt >= startTimestamp && order.createdAt <= endTimestamp
                    }
                } catch (e: Exception) {
                    android.util.Log.e("OrdersViewModel", "Error parsing dates: ${e.message}")
                    // If date parsing fails, return all orders
                    allOrders
                }
            } else {
                // If dates not set, return all orders
                allOrders
            }

            // Convert OrderEntity to OrderDetailList and group by isSynced status
            val completedList = mutableListOf<OrderDetailList>()
            val pendingList = mutableListOf<OrderDetailList>()
            
            filteredOrders.forEach { entity ->
                val orderDetail = convertOrderEntityToOrderDetailList(entity)
                if (entity.isSynced) {
                    completedList.add(orderDetail)
                } else {
                    pendingList.add(orderDetail)
                }
            }

            _orders.value = filteredOrders.map { convertOrderEntityToOrderDetailList(it) }
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
        // Initialize default dates: end date = today, start date = 1 day ago (yesterday)
        initializeDefaultDates()
        loadOrders()
    }

    private fun initializeDefaultDates() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calendar = Calendar.getInstance()

        // End date = current date
        val endDateStr = dateFormat.format(calendar.time)
        _endDate.value = endDateStr

        // Start date = 1 day ago
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val startDateStr = dateFormat.format(calendar.time)
        _startDate.value = startDateStr
    }

    fun loadOrderByOrderNumber(orderNumber: String) {
        viewModelScope.launch {
            try {
                val orderEntity = orderRepository.getOrderByOrderNumber(orderNumber)
                if (orderEntity != null) {
                    _orderDetail.value = convertOrderEntityToOrderDetailList(orderEntity)
                } else {
                    _orderDetail.value = null
                }
            } catch (e: Exception) {
                android.util.Log.e("OrdersViewModel", "Error loading order: ${e.message}", e)
                _orderDetail.value = null
            }
        }
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

