package com.retail.dolphinpos.data.setup.hardware.printer

import com.retail.dolphinpos.domain.model.order.PendingOrder
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.GetPrinterDetailsUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.PrintOrderReceiptUseCase
import java.util.Locale
import javax.inject.Inject

class PrintOrderReceiptUseCaseImpl @Inject constructor(
    private val getPrinterDetailsUseCase: GetPrinterDetailsUseCase,
    private val getPrinterReceiptTemplateUseCase: GetPrinterReceiptTemplateUseCase,
    private val printerManager: PrinterManager
) : PrintOrderReceiptUseCase {

    override suspend operator fun invoke(
        order: PendingOrder,
        statusCallback: (String) -> Unit
    ): Result<Unit> {
        return try {
            if (getPrinterDetailsUseCase() == null) {
                return Result.failure(IllegalStateException("No printer connected. Please set up a printer first."))
            }

            val receiptTemplate = getPrinterReceiptTemplateUseCase(
                order = order,
                isReceiptForRefund = false
            )

            var failureMessage: String? = null

            val success = printerManager.sendPrintCommand(
                data = receiptTemplate,
                getPrinterDetailsUseCase = getPrinterDetailsUseCase,
                statusCallback = { message ->
                    statusCallback(message)
                    val normalized = message.lowercase(Locale.US)
                    if (normalized.contains("error") || normalized.contains("fail")) {
                        failureMessage = message
                    }
                }
            )

            if (success && failureMessage == null) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException(failureMessage ?: "Failed to send print command."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


