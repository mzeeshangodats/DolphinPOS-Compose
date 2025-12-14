package com.retail.dolphinpos.presentation.features.ui.auth.select_register

import com.retail.dolphinpos.domain.model.auth.login.response.Locations
import com.retail.dolphinpos.domain.model.auth.login.response.Registers

sealed class SelectRegisterUiEvent {
    data class ShowLoading(val message: String = "Please Wait...") : SelectRegisterUiEvent()
    object HideLoading : SelectRegisterUiEvent()
    data class ShowError(val message: String) : SelectRegisterUiEvent()
    data class PopulateLocationsList(val locationsList: List<Locations>) : SelectRegisterUiEvent()
    data class PopulateRegistersList(val registersList: List<Registers>) : SelectRegisterUiEvent()
    object NavigateToPinScreen : SelectRegisterUiEvent()
    object NavigateToLoginScreen : SelectRegisterUiEvent()

}