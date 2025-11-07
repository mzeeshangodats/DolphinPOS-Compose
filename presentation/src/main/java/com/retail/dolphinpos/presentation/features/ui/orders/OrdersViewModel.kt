package com.retail.dolphinpos.presentation.features.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.domain.model.home.order_details.OrderDetailList
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
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val ordersRepository: OrdersRepository,
    private val preferenceManager: PreferenceManager,
    private val getPrintableOrderFromOrderDetailUseCase: GetPrintableOrderFromOrderDetailUseCase,
    private val printOrderReceiptUseCase: PrintOrderReceiptUseCase
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
                _isLoading.value = false
                _uiEvent.emit(OrdersUiEvent.HideLoading)
                _uiEvent.emit(OrdersUiEvent.ShowError("Failed to load orders: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
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
                    val successMessage = statusMessages.lastOrNull { it.contains("success", ignoreCase = true) }
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
