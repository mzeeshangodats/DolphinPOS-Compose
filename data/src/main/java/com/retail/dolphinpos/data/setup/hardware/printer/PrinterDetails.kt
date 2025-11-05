package com.retail.dolphinpos.data.setup.hardware.printer

import com.starmicronics.stario10.InterfaceType

data class PrinterDetails(
    val name : String,
    val address: String,
    val connectionType : InterfaceType,
    val isGraphic : Boolean = false,
    val isAutoPrintReceiptEnabled : Boolean = false,
    val isAutoOpenDrawerEnabled : Boolean = false
)