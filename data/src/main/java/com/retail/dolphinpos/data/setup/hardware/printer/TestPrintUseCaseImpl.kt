package com.retail.dolphinpos.data.setup.hardware.printer

import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.GetPrinterDetailsUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.TestPrintUseCase
import javax.inject.Inject

class TestPrintUseCaseImpl @Inject constructor(
    private val testPrintUseCaseData: TestPrintUseCaseData,
    private val printerManager: PrinterManager,
    private val getPrinterDetailsUseCase: GetPrinterDetailsUseCase
) : TestPrintUseCase {

    override suspend operator fun invoke(
        isGraphicPrinter: Boolean,
        statusCallback: (String) -> Unit
    ): String {
        return try {
            val template = testPrintUseCaseData(isGraphicPrinter)
            printerManager.sendTestPrintCommand(
                template = template,
                getPrinterDetailsUseCase = getPrinterDetailsUseCase,
                statusCallback = { message ->
                    // Pass status messages to the callback, similar to PrintOrderReceiptUseCaseImpl
                    statusCallback(message)
                }
            )
            template
        } catch (e: Exception) {
            // If an error occurs, notify via status callback
            statusCallback("Error during test print: ${e.localizedMessage ?: e.message ?: "Unknown error"}")
            throw e
        }
    }
}

