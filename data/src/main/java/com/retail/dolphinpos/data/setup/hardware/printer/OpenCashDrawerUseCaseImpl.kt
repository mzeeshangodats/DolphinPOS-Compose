package com.retail.dolphinpos.data.setup.hardware.printer

import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.GetPrinterDetailsUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.OpenCashDrawerUseCase
import javax.inject.Inject

class OpenCashDrawerUseCaseImpl @Inject constructor(
    private val printerManager: PrinterManager,
    private val getPrinterDetailsUseCase: GetPrinterDetailsUseCase
) : OpenCashDrawerUseCase {

    override suspend operator fun invoke(
        onStatusUpdate: (String) -> Unit
    ): Pair<Boolean, String> {
        return printerManager.openCashDrawer(getPrinterDetailsUseCase, onStatusUpdate)
    }
}

