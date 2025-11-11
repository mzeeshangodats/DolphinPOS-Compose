package com.retail.dolphinpos.presentation.features.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.retail.dolphinpos.common.network.NetworkMonitor
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.data.repositories.online_order.OnlineOrderRepository
import com.retail.dolphinpos.domain.model.home.create_order.CheckOutOrderItem
import com.retail.dolphinpos.domain.model.home.order_details.OrderDetailList
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
    private val preferenceManager: PreferenceManager,
    private val getPrintableOrderFromOrderDetailUseCase: GetPrintableOrderFromOrderDetailUseCase,
    private val printOrderReceiptUseCase: PrintOrderReceiptUseCase,
    private val networkMonitor: NetworkMonitor,
    private val onlineOrderRepository: OnlineOrderRepository,
    private val gson: Gson
) : ViewModel() {

    private val _orders = MutableStateFlow<List<OrderDetailList>>(emptyList())
    val orders: StateFlow<List<OrderDetailList>> = _orders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uiEvent = MutableSharedFlow<OrdersUiEvent>()
    val uiEvent: SharedFlow<OrdersUiEvent> = _uiEvent.asSharedFlow()

    fun loadOrders(keyword: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _uiEvent.emit(OrdersUiEvent.ShowLoading)
            try {
                // Check network availability
                if (!networkMonitor.isNetworkAvailable()) {
                    // Load orders from Room database when offline
                    loadOrdersFromRoom(keyword)
                } else {
                    // Load orders from API when online
                    loadOrdersFromApi(keyword)
                }
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
                _orders.value = response.data.list
                _uiEvent.emit(OrdersUiEvent.HideLoading)
            }.onFailure { e ->
                _uiEvent.emit(OrdersUiEvent.HideLoading)
                _uiEvent.emit(OrdersUiEvent.ShowError("Failed to load orders: ${e.message}"))
            }
        } catch (e: Exception) {
            _uiEvent.emit(OrdersUiEvent.HideLoading)
            _uiEvent.emit(OrdersUiEvent.ShowError("Failed to load orders: ${e.message}"))
        }
    }

    private suspend fun loadOrdersFromRoom(keyword: String?) {
        try {
            val storeId = preferenceManager.getStoreID()

            // Get orders from Room database
            val onlineOrders = onlineOrderRepository.getOrdersByStoreId(storeId)

            // Filter by keyword if provided
            val filteredOrders = if (keyword.isNullOrBlank()) {
                onlineOrders
            } else {
                onlineOrders.filter {
                    it.orderNumber.contains(keyword, ignoreCase = true) ||
                            it.paymentMethod.contains(keyword, ignoreCase = true)
                }
            }

            // Convert OnlineOrderEntity to OrderDetailList
            val orderDetailList = filteredOrders.map { entity ->
                convertToOrderDetailList(entity)
            }

            _orders.value = orderDetailList
            _uiEvent.emit(OrdersUiEvent.HideLoading)
        } catch (e: Exception) {
            _uiEvent.emit(OrdersUiEvent.HideLoading)
            _uiEvent.emit(OrdersUiEvent.ShowError("Failed to load orders from local storage: ${e.message}"))
        }
    }

    private fun convertToOrderDetailList(entity: com.retail.dolphinpos.data.entities.order.OnlineOrderEntity): OrderDetailList {
        // Parse items from JSON
        val itemsType = object : TypeToken<List<CheckOutOrderItem>>() {}.type
        val orderItems: List<CheckOutOrderItem> = try {
            gson.fromJson(entity.items, itemsType) ?: emptyList()
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

        // Format createdAt date
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val createdAtString = dateFormat.format(Date(entity.createdAt))

        // Format updatedAt (use createdAt if not available)
        val updatedAtString = createdAtString

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
            id = entity.id.toInt(),
            isRedeemed = entity.isRedeemed,
            locationId = entity.locationId,
            loyaltyPoints = 0,
            note = "",
            orderItems = convertedOrderItems,
            orderNumber = entity.orderNumber,
            paymentMethod = entity.paymentMethod,
            paymentStatus = "completed",
            redeemPoints = entity.redeemPoints ?: 0,
            refundedDiscount = "0.0",
            refundedSubTotal = "0.0",
            refundedTax = 0.0,
            refundedTotal = "0.0",
            rewardDetails = Unit,
            rewardDiscount = entity.rewardDiscount,
            rewardRefundDiscount = 0.0,
            source = entity.source,
            status = if (entity.isVoid) "void" else "completed",
            storeId = entity.storeId,
            storeRegisterId = entity.storeRegisterId ?: 0,
            subTotal = entity.subTotal.toString(),
            taxValue = entity.taxValue,
            timeline = Unit,
            total = entity.total.toString(),
            transactions = emptyList(), // Transactions not stored in OnlineOrderEntity
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
                val printableOrder = getPrintableOrderFromOrderDetailUseCase(order)
                val statusMessages = mutableListOf<String>()
                val result = printOrderReceiptUseCase(printableOrder) { statusMessages.add(it) }
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
                _uiEvent.emit(OrdersUiEvent.ShowError(e.message ?: "Failed to print receipt."))
            } finally {
                _uiEvent.emit(OrdersUiEvent.HideLoading)
            }
        }
    }
}
