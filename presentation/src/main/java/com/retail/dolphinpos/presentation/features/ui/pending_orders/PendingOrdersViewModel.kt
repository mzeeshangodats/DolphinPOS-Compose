package com.retail.dolphinpos.presentation.features.ui.pending_orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.data.entities.order.PendingOrderEntity
import com.retail.dolphinpos.data.repositories.pending_order.PendingOrderRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PendingOrdersViewModel @Inject constructor(
    private val pendingOrderRepository: PendingOrderRepositoryImpl
) : ViewModel() {

    private val _pendingOrders = MutableStateFlow<List<PendingOrderEntity>>(emptyList())
    val pendingOrders: StateFlow<List<PendingOrderEntity>> = _pendingOrders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uiEvent = MutableSharedFlow<PendingOrdersUiEvent>()
    val uiEvent: SharedFlow<PendingOrdersUiEvent> = _uiEvent.asSharedFlow()

    fun loadPendingOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val orders = pendingOrderRepository.getUnsyncedOrders()
                _pendingOrders.value = orders
            } catch (e: Exception) {
                _uiEvent.emit(PendingOrdersUiEvent.ShowError("Failed to load pending orders: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun syncOrder(orderId: Long) {
        viewModelScope.launch {
            _uiEvent.emit(PendingOrdersUiEvent.ShowLoading)
            try {
                val order = pendingOrderRepository.getOrderById(orderId)
                if (order != null) {
                    pendingOrderRepository.syncOrderToServer(order).onSuccess {
                        _uiEvent.emit(PendingOrdersUiEvent.ShowSuccess("Order synced successfully"))
                        loadPendingOrders() // Reload the list
                    }.onFailure { e ->
                        _uiEvent.emit(PendingOrdersUiEvent.ShowError("Failed to sync order: ${e.message}"))
                    }
                } else {
                    _uiEvent.emit(PendingOrdersUiEvent.ShowError("Order not found"))
                }
            } catch (e: Exception) {
                _uiEvent.emit(PendingOrdersUiEvent.ShowError("Failed to sync order: ${e.message}"))
            } finally {
                _uiEvent.emit(PendingOrdersUiEvent.HideLoading)
            }
        }
    }
}

sealed class PendingOrdersUiEvent {
    object ShowLoading : PendingOrdersUiEvent()
    object HideLoading : PendingOrdersUiEvent()
    data class ShowError(val message: String) : PendingOrdersUiEvent()
    data class ShowSuccess(val message: String) : PendingOrdersUiEvent()
}

