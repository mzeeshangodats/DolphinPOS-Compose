package com.retail.dolphinpos.data.setup.hardware.printer

import android.annotation.SuppressLint
import com.retail.dolphinpos.common.utils.createCenteredLogoBitmap
import com.retail.dolphinpos.data.setup.hardware.receipt.GenerateBarcodeImageUseCase
import com.retail.dolphinpos.data.setup.hardware.receipt.GenerateReceiptTextUseCase
import com.retail.dolphinpos.data.setup.hardware.receipt.GetStoreDetailsFromLocalUseCase
import com.retail.dolphinpos.domain.model.order.PendingOrder
import com.starmicronics.stario10.starxpandcommand.DocumentBuilder
import com.starmicronics.stario10.starxpandcommand.PrinterBuilder
import com.starmicronics.stario10.starxpandcommand.StarXpandCommandBuilder
import com.starmicronics.stario10.starxpandcommand.printer.Alignment
import com.starmicronics.stario10.starxpandcommand.printer.CutType
import com.starmicronics.stario10.starxpandcommand.printer.ImageParameter

class GetPrinterReceiptTemplateUseCase(
    val createImageParameterFromTextUseCase: CreateImageParameterFromTextUseCase,
    val generateReceiptTextUseCase: GenerateReceiptTextUseCase,
    val generateBarcodeImageUseCase: GenerateBarcodeImageUseCase,
    val getStoreDetailsFromLocalUseCase: GetStoreDetailsFromLocalUseCase,
    val downloadAndUpdateCachedImageUseCase: DownloadAndUpdateCachedImageUseCase,
) {

    @SuppressLint("DefaultLocale")
    suspend operator fun invoke(
        order: PendingOrder?,
        isReceiptForRefund: Boolean = false
    ): String {
        val builder = StarXpandCommandBuilder()

        order?.let {

            val logo =
                downloadAndUpdateCachedImageUseCase(getStoreDetailsFromLocalUseCase()?.logoUrl?.original)

            val centeredLogo = logo?.let { createCenteredLogoBitmap(it) }

            val printerBuilder = PrinterBuilder().styleAlignment(Alignment.Center)

            centeredLogo?.let {
                printerBuilder.actionPrintImage(ImageParameter(it, 600))
            }

            // Generate receipt text (suspend function)
            val receiptText = generateReceiptTextUseCase(
                order,
                isReceiptForRefund = isReceiptForRefund
            )

            printerBuilder
                .actionPrintText("\n\n\n")
                .actionPrintImage(
                    createImageParameterFromTextUseCase(receiptText)
                )
                .actionPrintText("\n\n")
                .styleAlignment(Alignment.Center)
                .actionPrintImage(generateBarcodeImageUseCase(order.orderNumber))
                .actionCut(CutType.Partial)


            builder.addDocument(DocumentBuilder().addPrinter(printerBuilder))

        } ?: run {
            builder.addDocument(
                DocumentBuilder()
                    .addPrinter(
                        PrinterBuilder()
                            .styleAlignment(Alignment.Left)
                            .actionPrintText("Order was empty")
                            .actionCut(CutType.Partial)
                    )
            )
        }
        return builder.getCommands()
    }

}