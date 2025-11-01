package com.retail.dolphinpos.presentation.features.ui.orders

sealed class OrdersUiEvent {
    object ShowLoading : OrdersUiEvent()
    object HideLoading : OrdersUiEvent()
    data class ShowError(val message: String) : OrdersUiEvent()
    data class ShowSuccess(val message: String) : OrdersUiEvent()
}

