package com.retail.dolphinpos.domain.model.setup.hardware.printer

data class PrinterDetails(
    val name: String,
    val address: String,
    val connectionType: PrinterConnectionType,
    val isGraphic: Boolean = false,
    val isAutoPrintReceiptEnabled: Boolean = false,
    val isAutoOpenDrawerEnabled: Boolean = false
)

