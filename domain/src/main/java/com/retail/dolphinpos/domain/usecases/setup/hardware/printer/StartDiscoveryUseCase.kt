package com.retail.dolphinpos.domain.usecases.setup.hardware.printer

import android.content.Context
import com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterDetails

interface StartDiscoveryUseCase {
    fun invoke(
        context: Context,
        excludeBluetooth: Boolean = false,
        onPrinterFound: (PrinterDetails) -> Unit,
        onDiscoveryFinished: () -> Unit,
        onError: (Exception) -> Unit
    )
    
    fun stopDiscovery()
}

