package com.retail.dolphinpos.presentation.features.ui.auth.login

sealed class LoginUiEvent {
    object ShowLoading : LoginUiEvent()
    object HideLoading : LoginUiEvent()
    data class ShowError(val message: String) : LoginUiEvent()
    object NavigateToRegister : LoginUiEvent()
}