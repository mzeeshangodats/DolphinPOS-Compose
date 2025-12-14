package com.retail.dolphinpos.presentation.features.ui.auth.login

import com.retail.dolphinpos.presentation.features.ui.auth.pin_code.VerifyPinUiEvent

sealed class LoginUiEvent {
    object ShowLoading : LoginUiEvent()
    object HideLoading : LoginUiEvent()
    data class ShowError(val message: String) : LoginUiEvent()
    data class ShowNoInternetDialog(val message: String, val success: Boolean = false) : LoginUiEvent()
    object NavigateToRegister : LoginUiEvent()
}