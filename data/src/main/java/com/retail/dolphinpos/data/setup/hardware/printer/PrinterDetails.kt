package com.retail.dolphinpos.data.setup.hardware.printer

import com.starmicronics.stario10.InterfaceType

/**
 * Data layer model for printer details.
 * Uses Star Micronics SDK types.
 */
data class PrinterDetailsData(
    val name: String,
    val address: String,
    val connectionType: InterfaceType,
    val isGraphic: Boolean = false,
    val isAutoPrintReceiptEnabled: Boolean = false,
    val isAutoOpenDrawerEnabled: Boolean = false
)