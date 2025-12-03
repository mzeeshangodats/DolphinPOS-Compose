package com.retail.dolphinpos.presentation.features.ui.auth.pin_code

import com.retail.dolphinpos.domain.model.auth.active_user.ActiveUserDetails

sealed class VerifyPinUiEvent {
    object ShowLoading : VerifyPinUiEvent()
    object HideLoading : VerifyPinUiEvent()
    data class ShowDialog(val message: String, val success: Boolean = false) : VerifyPinUiEvent()
    data class ShowBatchClosedDialog(val message: String) : VerifyPinUiEvent()
    data class ShowRegisterReleasedDialog(val message: String) : VerifyPinUiEvent()
    data class GetActiveUserDetails(val activeUserDetails: ActiveUserDetails) : VerifyPinUiEvent()
    object NavigateToCashDenomination : VerifyPinUiEvent()
    object NavigateToCartScreen : VerifyPinUiEvent()
    object NavigateToSelectRegister : VerifyPinUiEvent()
}