package com.retail.dolphinpos.presentation.features.ui.setup.barcode

interface BarcodeHandler {
    fun onScannedCode(scannedValue: String)
}