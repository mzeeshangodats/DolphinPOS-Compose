package com.retail.dolphinpos.presentation.features.ui.setup.cfd

data class CustomerDisplaySetupViewState(
    val ipAddress: String = "",
    val isEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val shouldNavigateBack: Boolean = false,
    val isButtonEnabled: Boolean = false
)

