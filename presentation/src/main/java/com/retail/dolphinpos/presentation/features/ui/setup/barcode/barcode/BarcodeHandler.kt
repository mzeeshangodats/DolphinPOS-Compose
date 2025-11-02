package com.retail.dolphinpos.presentation.features.ui.setup.barcode.barcode

interface BarcodeHandler {
    fun onScannedCode(scannedValue: String)
}