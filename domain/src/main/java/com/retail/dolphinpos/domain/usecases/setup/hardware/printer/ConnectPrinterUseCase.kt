package com.retail.dolphinpos.domain.usecases.setup.hardware.printer

import com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterDetails

interface ConnectPrinterUseCase {
    suspend operator fun invoke(
        printer: PrinterDetails,
        onStatusUpdate: (String) -> Unit
    ): Boolean
}

