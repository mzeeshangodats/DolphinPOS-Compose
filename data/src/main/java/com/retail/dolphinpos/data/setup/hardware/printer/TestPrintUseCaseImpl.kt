package com.retail.dolphinpos.data.setup.hardware.printer

import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.GetPrinterDetailsUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.TestPrintUseCase
import javax.inject.Inject

class TestPrintUseCaseImpl @Inject constructor(
    private val testPrintUseCaseData: TestPrintUseCaseData,
    private val printerManager: PrinterManager,
    private val getPrinterDetailsUseCase: GetPrinterDetailsUseCase
) : TestPrintUseCase {

    override suspend operator fun invoke(isGraphicPrinter: Boolean): String {
        val template = testPrintUseCaseData(isGraphicPrinter)
        printerManager.sendTestPrintCommand(
            template = template,
            getPrinterDetailsUseCase = getPrinterDetailsUseCase,
            statusCallback = { }
        )
        return template
    }
}

