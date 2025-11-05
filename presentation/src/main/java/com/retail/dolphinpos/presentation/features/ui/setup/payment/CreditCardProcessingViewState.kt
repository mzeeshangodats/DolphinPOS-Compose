package com.retail.dolphinpos.presentation.features.ui.setup.payment

data class CreditCardProcessingViewState(
    val ipAddress : String = "",
    val portNumber : String = "10009",
    val communicationType : String? = "tcp/ip",
    val timeout : Int = 30000,
    val isButtonEnabled : Boolean = false,
    val terminalResponse : String = "",
    val config: CreditCardConfigState = CreditCardConfigState(),
    val shouldNavigateBack: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

sealed class CreditCardProcessingViewEffect{
    data class ShowErrorSnackBar(val message : String) : CreditCardProcessingViewEffect()
    data class ShowSuccessSnackBar(val message : String) : CreditCardProcessingViewEffect()
    data class ShowInformationSnackBar(val message : String) : CreditCardProcessingViewEffect()
    data class ShowLoading(val message: String,val isLoading : Boolean) : CreditCardProcessingViewEffect()
    data object ShowCloseBatchConfirmationDialog : CreditCardProcessingViewEffect()
    data object OnCloseBatchDialogDismiss : CreditCardProcessingViewEffect()
}

sealed class CreditCardProcessingViewEvent {
    data object OnTestConnectionClicked : CreditCardProcessingViewEvent()
    data object OnUpdateIpAddressClicked : CreditCardProcessingViewEvent()
    data object OnCloseBatchManuallyClicked : CreditCardProcessingViewEvent()
    data object OnCloseBatchConfirmationClicked : CreditCardProcessingViewEvent()
    data object OnCloseBatchDialogDismiss : CreditCardProcessingViewEvent()
}

enum class TerminalType(val displayName: String) {
    EMV("EMV"),
    PAX_A35("PAX A35"),
    PAX_A920("PAX A920"),
    WIFI("WiFi"),
    D200("D200")
}

enum class CommunicationType(val displayName: String) {
    TCP_IP("TCP/IP"),
    HTTP_GET("HTTP/GET")
}

data class CreditCardConfigState(
    val isEnabled: Boolean = false,
    val selectedTerminalType: TerminalType = TerminalType.PAX_A35,
    val communicationType: CommunicationType = CommunicationType.TCP_IP,
    val bluetoothAddress: String = "",
    val ipAddress: String = "",
    val portNumber: String = "10009",
    val terminalId: String = "",
    val merchantId: String = "",
    val digitalSignatureEnabled: Boolean = true
)