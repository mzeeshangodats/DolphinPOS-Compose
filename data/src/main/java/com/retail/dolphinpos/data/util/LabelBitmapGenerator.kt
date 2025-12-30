package com.retail.dolphinpos.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.retail.dolphinpos.domain.model.label.Label
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LabelBitmapGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun createLabelBitmapFromLayout(label: Label): Bitmap {
        val widthDp = 370f
        val heightDp = 260f
        val widthPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, widthDp, context.resources.displayMetrics
        ).toInt()
        val heightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, heightDp, context.resources.displayMetrics
        ).toInt()

        val bitmap = createBitmap(widthPx, heightPx)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val centerX = widthPx / 2f

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = Color.BLACK
            textSize = 22f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT
        }

        val boldPaint = Paint(textPaint).apply {
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isFakeBoldText = true
        }

        val strikeThroughPaint = Paint(textPaint).apply {
            flags = Paint.STRIKE_THRU_TEXT_FLAG
            typeface = Typeface.DEFAULT
        }

        // SALE Banner
        if (label.isDiscounted) {
            boldPaint.color = Color.RED
            canvas.drawText("SALE", centerX, 25f, boldPaint)
            boldPaint.color = Color.BLACK
        }

        val name = if (label.variantName.isEmpty()) {
            label.productName.take(40)
        } else {
            "${label.productName.take(20)} - ${label.variantName.take(20)}"
        }

        // Product Name
        canvas.drawText(name, centerX, 65f, textPaint)

        // Positioning for Cash & Card Prices
        val cashX = if (label.applyDualPrice) centerX - 100f else centerX
        val cardX = centerX + 100f
        val labelY = 95f
        val priceY = 125f

        // "Cash" and "Card" labels
        textPaint.textSize = 20f
        canvas.drawText("Cash", cashX, labelY, textPaint)
        if (label.applyDualPrice) {
            canvas.drawText("Card", cardX, labelY, textPaint)
        }

        // Prices
        if (label.isDiscounted) {
            canvas.drawText("$${String.format("%.2f", label.cashPrice)}", cashX, priceY, strikeThroughPaint)
            canvas.drawText("$${String.format("%.2f", label.cashDiscountedPrice)}", cashX, priceY + 30f, boldPaint)

            if (label.applyDualPrice) {
                canvas.drawText("$${String.format("%.2f", label.cardPrice)}", cardX, priceY, strikeThroughPaint)
                canvas.drawText("$${String.format("%.2f", label.cardDiscountedPrice)}", cardX, priceY + 30f, boldPaint)
            }
        } else {
            canvas.drawText("$${String.format("%.2f", label.cashPrice)}", cashX, priceY, boldPaint)
            if (label.applyDualPrice) {
                canvas.drawText("$${String.format("%.2f", label.cardPrice)}", cardX, priceY, boldPaint)
            }
        }

        // Barcode
        val barcodeBitmap = generateBarcodeBitmap(label.barcode, 400, 100)
        barcodeBitmap?.let {
            val barcodeY = 165f
            canvas.drawBitmap(it, (widthPx - it.width) / 2f, barcodeY, null)
            canvas.drawText(label.barcode, centerX, barcodeY + it.height + 25f, textPaint)
        }

        return bitmap
    }

    private fun generateBarcodeBitmap(barcode: String, width: Int, height: Int): Bitmap? {
        return try {
            val format = BarcodeFormat.CODE_128
            val hints = mapOf(EncodeHintType.MARGIN to 1)
            val bitMatrix = MultiFormatWriter().encode(barcode, format, width, height, hints)

            Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565).apply {
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}

