package com.retail.dolphinpos.data.setup.hardware.printer

data class PrinterViewState(
    val discoveryStatus: String? = null,
    val discoveredPrinters: List<PrinterDetails> = emptyList(),
    val isBluetoothPermissionGranted: Boolean = false,
    val savedPrinterDetails : PrinterDetails? = null,
    val isAutoPrintEnabled : Boolean = false,
    val isAutoOpenDrawerEnabled : Boolean = false
)

sealed class PrinterViewEffect {
    data class ShowErrorSnackBar(val message : String) : PrinterViewEffect()
    data class ShowSuccessSnackBar(val message : String) : PrinterViewEffect()
    data class ShowInformationSnackBar(val message : String) : PrinterViewEffect()
    data class ShowLoading(val isLoading : Boolean) : PrinterViewEffect()
}

sealed class PrinterViewEvent {
    data object OnNextClicked : PrinterViewEvent()
    data object OnSaveClicked : PrinterViewEvent()
}