package com.retail.dolphinpos.presentation.features.ui.orders

import com.retail.dolphinpos.presentation.features.ui.auth.login.LoginUiEvent

sealed class OrdersUiEvent {
    object ShowLoading : OrdersUiEvent()
    object HideLoading : OrdersUiEvent()
    data class ShowNoInternetDialog(val message: String, val success: Boolean = false) : OrdersUiEvent()
    data class ShowError(val message: String) : OrdersUiEvent()
    data class ShowSuccess(val message: String) : OrdersUiEvent()
}

