package com.retail.dolphinpos.presentation.features.ui.auth.cash_denomination

import com.retail.dolphinpos.presentation.features.ui.auth.login.LoginUiEvent

sealed class CashDenominationUiEvent {
    object NavigateToHome : CashDenominationUiEvent()
    data class ShowError(val message: String) : CashDenominationUiEvent()
}

