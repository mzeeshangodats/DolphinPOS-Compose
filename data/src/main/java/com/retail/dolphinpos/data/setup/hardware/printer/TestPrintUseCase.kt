package com.retail.dolphinpos.data.setup.hardware.printer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.lingeriepos.common.usecases.printer.DownloadAndUpdateCachedImageUseCase
import com.starmicronics.stario10.starxpandcommand.DocumentBuilder
import com.starmicronics.stario10.starxpandcommand.MagnificationParameter
import com.starmicronics.stario10.starxpandcommand.PrinterBuilder
import com.starmicronics.stario10.starxpandcommand.StarXpandCommandBuilder
import com.starmicronics.stario10.starxpandcommand.printer.Alignment
import com.starmicronics.stario10.starxpandcommand.printer.BarcodeParameter
import com.starmicronics.stario10.starxpandcommand.printer.BarcodeSymbology
import com.starmicronics.stario10.starxpandcommand.printer.CutType
import com.starmicronics.stario10.starxpandcommand.printer.ImageParameter
import com.starmicronics.stario10.starxpandcommand.printer.InternationalCharacterType
import com.starmicronics.stario10.starxpandcommand.printer.RuledLineParameter
import javax.inject.Inject

class TestPrintUseCase @Inject constructor(
    val getAppLogo: GetAppLogoUseCase,
    val createImageParameterFromText: CreateImageParameterFromTextUseCase,
    val downloadAndUpdateCachedImageUseCase: DownloadAndUpdateCachedImageUseCase
) {

    suspend operator fun invoke(isGraphicPrinter: Boolean = false): String {


        val builder = StarXpandCommandBuilder()

        val logo = downloadAndUpdateCachedImageUseCase("https://placehold.co/600x400?text=Malik%27s")

        if (isGraphicPrinter) {
            builder.addDocument(
                DocumentBuilder()
                    .addPrinter(
                        PrinterBuilder()
                            .styleAlignment(Alignment.Center)
                            .actionPrintImage(ImageParameter(logo ?: getAppLogo(), 406))
                            .actionFeed(1.0)
                            .actionPrintImage(
                                createImageParameterFromText(
                                    "POS\n" +
                                            "123 Star Road\n" +
                                            "City, State 12345\n" +
                                            "\n" +
                                            "Date:22/01/2000 Time:10:11 PM\n" +
                                            "-----------------------------\n" +
                                            "\n" +
                                            "Id   Description   Total\n" +
                                            "1    PLAIN T-SHIRT 10.99\n" +
                                            "2    BLACK DENIM   29.99\n" +
                                            "3    BLUE DENIM    29.99\n" +
                                            "4    STRIPED DRESS 49.99\n" +
                                            "5    BLACK BOOTS   35.99\n" +
                                            "\n" +
                                            "Subtotal               156.95\n" +
                                            "Tax                      0.00\n" +
                                            "-----------------------------\n" +
                                            "\n" +
                                            "Total                 $156.95\n" +
                                            "-----------------------------\n" +
                                            "\n\n"
                                )
                            )
                            .actionPrintImage(generateBarcode("TEST123"))
                            .actionPrintText("\n\n")
                            .actionCut(CutType.Partial)

                    )
            )

        } else {

            builder.addDocument(
                DocumentBuilder()
                    .settingPrintableArea(48.0)
                    .addPrinter(
                        PrinterBuilder()
                            .actionPrintImage(ImageParameter(getAppLogo(), 406))
                            .styleInternationalCharacter(InternationalCharacterType.Usa)
                            .styleCharacterSpace(0.0)
                            .add(
                                PrinterBuilder()
                                    .styleAlignment(Alignment.Center)
                                    .styleBold(true)
                                    .styleInvert(true)
                                    .styleMagnification(MagnificationParameter(2, 2))
                                    .actionPrintText("\${store_name}\n")
                            )
                            .actionFeed(1.0)
                            .actionPrintText("Order \${order_number}")
                            .actionPrintText(" \${time}\n")
                            .actionPrintText("Sale for \${sales_type}")
                            .actionPrintText("Served by \${server}\n")
                            .actionPrintText("Transaction #\${transaction_id}\n")
                            .actionPrintRuledLine(RuledLineParameter(48.0))
                            .add(
                                PrinterBuilder()
                                    .actionPrintText("\${item_list.quantity}")
                                    .actionPrintText("\${item_list.name}")
                                    .actionPrintText("\${item_list.unit_price%6.2lf}\n")
                            )
                            .actionPrintRuledLine(RuledLineParameter(48.0))
                            .actionPrintText("Subtotal \${subtotal%6.2lf}\n")
                            .actionPrintText("Tax \${tax%6.2lf}\n")
                            .actionPrintText("Total \${total%6.2lf}\n")
                            .actionPrintRuledLine(RuledLineParameter(48.0))
                            .actionPrintRuledLine(RuledLineParameter(32.0).setX(8.0))
                            .actionPrintRuledLine(RuledLineParameter(48.0))
                            .actionPrintText("\${address}\n")
                            .actionPrintText("\${tel}\n")
                            .actionPrintText("\${mail}\n")
                            .actionPrintText("\n\n")
                            .actionPrintBarcode(
                                BarcodeParameter(
                                    "TEST1234",
                                    symbology = BarcodeSymbology.Code128
                                )
                            ).actionPrintText("\n\n")
                            .actionFeed(1.0)
                            .actionCut(CutType.Partial)
                    )
            )

        }

        return builder.getCommands()
    }

    private fun generateBarcode(orderNumber: String): ImageParameter {
        val barcodeWidth = 400
        val barcodeHeight = 100
        val textHeight = 50
        val totalHeight = barcodeHeight + textHeight // Total height for the combined image
        val format = BarcodeFormat.CODE_128
        val hints = mapOf(EncodeHintType.MARGIN to 1)

        return try {
            val bitMatrix = MultiFormatWriter().encode(orderNumber, format, barcodeWidth, barcodeHeight, hints)

            val barcodeBitmap = Bitmap.createBitmap(barcodeWidth, barcodeHeight, Bitmap.Config.RGB_565)
            for (x in 0 until barcodeWidth) {
                for (y in 0 until barcodeHeight) {
                    barcodeBitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            val combinedBitmap = Bitmap.createBitmap(barcodeWidth, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(combinedBitmap)

            canvas.drawColor(Color.WHITE)

            canvas.drawBitmap(barcodeBitmap, 0f, 0f, null)

            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 30f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true // Ensure smooth text rendering
            }

            val textX = barcodeWidth / 2f
            val textY = barcodeHeight + textHeight / 2f - ((paint.descent() + paint.ascent()) / 2)
            canvas.drawText(orderNumber, textX, textY, paint)

            ImageParameter(combinedBitmap, barcodeWidth)
        } catch (e: Exception) {
            throw IllegalArgumentException("Could not generate barcode: ${e.message}")
        }
    }


}
