package com.retail.dolphinpos.data.setup.hardware.receipt

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.starmicronics.stario10.starxpandcommand.printer.ImageParameter
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import javax.inject.Inject

class GenerateBarcodeImageUseCase @Inject constructor() {

    operator fun invoke(orderNumber: String): ImageParameter {
        val receiptWidth = 600  // 48mm receipt width in pixels
        val barcodeWidth = 300  // Barcode width (75% of receipt width)
        val barcodeHeight = 100 // Barcode height
        val padding = 20        // Padding between elements

        return try {
            val format = BarcodeFormat.CODE_128
            val hints = mapOf(EncodeHintType.MARGIN to 1)
            val bitMatrix = MultiFormatWriter().encode(orderNumber, format, barcodeWidth, barcodeHeight, hints)

            val barcodeBitmap = Bitmap.createBitmap(barcodeWidth, barcodeHeight, Bitmap.Config.RGB_565).apply {
                for (x in 0 until barcodeWidth) {
                    for (y in 0 until barcodeHeight) {
                        setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                    }
                }
            }

            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = 30f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            val thankYouPaint = Paint().apply {
                color = Color.BLACK
                textSize = 28f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            val orderNumTextBounds = Rect()
            textPaint.getTextBounds(orderNumber, 0, orderNumber.length, orderNumTextBounds)

            val thankYouText = "Thank you for your Business."
            val thankYouBounds = Rect()
            thankYouPaint.getTextBounds(thankYouText, 0, thankYouText.length, thankYouBounds)

            val totalHeight = barcodeHeight +
                    orderNumTextBounds.height() +
                    thankYouBounds.height() +
                    (padding * 3)

            val combinedBitmap = Bitmap.createBitmap(receiptWidth, totalHeight, Bitmap.Config.ARGB_8888)
            Canvas(combinedBitmap).apply {
                drawColor(Color.WHITE)

                drawBitmap(
                    barcodeBitmap,
                    (receiptWidth - barcodeWidth) / 2f,
                    padding.toFloat(),
                    null
                )

                var currentY = barcodeHeight + padding * 2

                currentY += orderNumTextBounds.height()
                drawText(
                    orderNumber,
                    width / 2f,
                    (currentY - orderNumTextBounds.bottom).toFloat(),
                    textPaint
                )

                currentY += padding + thankYouBounds.height()
                drawText(
                    thankYouText,
                    width / 2f,
                    (currentY - thankYouBounds.bottom).toFloat(),
                    thankYouPaint
                )
            }

            ImageParameter(combinedBitmap, receiptWidth)

        } catch (e: Exception) {
            throw IllegalArgumentException("Could not generate barcode: ${e.message}")
        }
    }
}