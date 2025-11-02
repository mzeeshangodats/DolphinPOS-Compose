package com.retail.dolphinpos.data.setup.hardware.barcode

interface BarcodeHandler {
    fun onScannedCode(scannedValue: String)
}