package com.retail.dolphinpos.domain.usecases.setup.hardware.printer

import com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterDetails

interface GetPrinterDetailsUseCase {
    operator fun invoke(): PrinterDetails?
}

