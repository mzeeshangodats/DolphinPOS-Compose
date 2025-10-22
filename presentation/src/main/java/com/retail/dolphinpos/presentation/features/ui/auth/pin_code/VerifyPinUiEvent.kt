package com.retail.dolphinpos.presentation.features.ui.auth.pin_code

import com.retail.dolphinpos.domain.model.auth.active_user.ActiveUserDetails

sealed class VerifyPinUiEvent {
    object ShowLoading : VerifyPinUiEvent()
    object HideLoading : VerifyPinUiEvent()
    data class ShowError(val message: String) : VerifyPinUiEvent()
    data class GetActiveUserDetails(val activeUserDetails: ActiveUserDetails) : VerifyPinUiEvent()
    object NavigateToCashDenomination : VerifyPinUiEvent()
    object NavigateToCartScreen : VerifyPinUiEvent()
}