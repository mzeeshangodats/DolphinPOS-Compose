//package com.retail.dolphinpos.data.setup.hardware.printer
//
//import android.annotation.SuppressLint
//import android.graphics.Bitmap
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.Paint
//import android.graphics.Typeface
//import android.text.Layout
//import android.text.StaticLayout
//import android.text.TextPaint
//import com.google.zxing.BarcodeFormat
//import com.google.zxing.EncodeHintType
//import com.google.zxing.MultiFormatWriter
//import com.lingeriepos.common.usecases.printer.receipt.GenerateReceiptTextUseCase
//import com.lingeriepos.common.usecases.store.GetStoreDetailsFromLocalUseCase
//import androidx.core.graphics.createBitmap
//
//const val PAPER_WIDTH = 576
//
//class GetPreviewReceiptImageUseCase(
//    val generateReceiptTextUseCase: GenerateReceiptTextUseCase,
//    val getStoreDetailsFromLocalUseCase: GetStoreDetailsFromLocalUseCase,
//    val downloadAndUpdateCachedImageUseCase: DownloadAndUpdateCachedImageUseCase,
//    val getAppLogo: GetAppLogoUseCase,
//) {
//
//    @SuppressLint("DefaultLocale")
//    suspend operator fun invoke(
//        singleOrder: SingleOrder?,
//        isReceiptForRefund: Boolean = false
//    ): Bitmap {
//
//        var bitmap: Bitmap = createEmptyBitmapWithStoreName()
//
//        singleOrder?.let { order ->
//
//            val logo =
//                downloadAndUpdateCachedImageUseCase(getStoreDetailsFromLocalUseCase()?.logo?.fileURL)
//            val centeredLogo = createCenteredLogoBitmap(logo ?: getAppLogo())
//
//            bitmap = if (logo != null) {
//                createReceiptBitmap(
//                    generateReceiptTextUseCase(order, isReceiptForRefund),
//                    order,
//                    centeredLogo
//                )
//            } else{
//                createReceiptBitmap(
//                    generateReceiptTextUseCase(order, isReceiptForRefund),
//                    order,
//                    logo = null
//                )
//            }
//
//
//        } ?: run {
//            return createEmptyBitmapWithStoreName()
//        }
//
//        return bitmap
//    }
//
//    fun createBoxedTextBitmap(labelText: String = "TOTAL", valueText: String): Bitmap {
//        val bitmapWidth = 500
//        val bitmapHeight = 150
//        val negativeOffset = 20f
//
//        val bitmap = createBitmap(bitmapWidth, bitmapHeight)
//        val canvas = Canvas(bitmap)
//
//        canvas.drawColor(Color.WHITE)
//
//        val boxPaint = Paint().apply {
//            color = Color.BLACK
//            style = Paint.Style.STROKE
//            strokeWidth = 3f
//        }
//
//        val labelTextPaint = Paint().apply {
//            color = Color.BLACK
//            textSize = 20f
//            textAlign = Paint.Align.CENTER
//        }
//
//        val valueTextPaint = Paint().apply {
//            color = Color.BLACK
//            textSize = 40f
//            textAlign = Paint.Align.CENTER
//        }
//
//        val boxLeft = 10f
//        val boxTop = 10f
//        val boxRight = bitmapWidth.toFloat() - 10f
//        val boxBottom = bitmapHeight.toFloat() - 10f
//        val cornerRadius = 10f
//        canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, cornerRadius, cornerRadius, boxPaint)
//
//        val xPos = (boxLeft + boxRight) / 2
//        val yCenter = (boxTop + boxBottom) / 2
//
//        val labelHeight = labelTextPaint.descent() - labelTextPaint.ascent()
//        val valueHeight = valueTextPaint.descent() - valueTextPaint.ascent()
//
//        val totalHeightOfText = labelHeight + valueHeight - negativeOffset
//        val topOfTextBlock = yCenter - totalHeightOfText / 2
//
//        val labelYPos = topOfTextBlock - labelTextPaint.ascent()
//        val valueYPos = labelYPos + labelHeight - negativeOffset - valueTextPaint.ascent()
//
//        canvas.drawText(labelText, xPos, labelYPos, labelTextPaint)
//        canvas.drawText(valueText, xPos, valueYPos, valueTextPaint)
//
//        return bitmap
//    }
//
//    private fun createReceiptBitmap(
//        receiptContent: String,
//        order: SingleOrder,
//        logo: Bitmap?
//    ): Bitmap {
//        val width = PAPER_WIDTH
//        val padding = 22
//
//        val barcodeBitmap = generateBarcode(order.orderNumber!!)
//
//        val typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
//        val paint = Paint().apply {
//            textSize = 22f
//            this.typeface = typeface
//            color = Color.BLACK
//        }
//        val textPaint = TextPaint(paint)
//
//        val staticLayout = StaticLayout.Builder.obtain(
//            receiptContent, 0, receiptContent.length, textPaint, width - 2 * padding
//        )
//            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
//            .setLineSpacing(0f, 1f)
//            .setIncludePad(false)
//            .build()
//
//        val totalHeight = staticLayout.height + barcodeBitmap.height + 3 * padding
//
//        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
//        val canvas = Canvas(bitmap)
//        canvas.drawColor(Color.WHITE)
//
//        var currentY = padding.toFloat()
//
//
//        logo?.let {
//
//            canvas.drawBitmap(it, (width - logo.width) / 2f, currentY, null)
//            currentY += it.height + padding
//        }
//
//        canvas.translate(padding.toFloat(), currentY)
//        staticLayout.draw(canvas)
//        canvas.translate(-padding.toFloat(), 0f)
//
//        currentY += staticLayout.height + padding
//
//        canvas.drawBitmap(barcodeBitmap, (width - barcodeBitmap.width) / 2f, currentY, null)
//
//        return bitmap
//    }
//
//
//    private fun createEmptyBitmapWithStoreName(): Bitmap {
//        val width = PAPER_WIDTH
//        val height = 100
//        val typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
//        val paint = Paint().apply {
//            textSize = 30f
//            this.typeface = typeface
//            color = Color.BLACK
//            textAlign = Paint.Align.CENTER
//        }
//
//        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//        val canvas = Canvas(bitmap)
//        canvas.drawColor(Color.WHITE)
//
//        val xPos = width / 2f
//        val yPos = height / 2f - (paint.descent() + paint.ascent()) / 2
//        canvas.drawText("POS", xPos, yPos, paint)
//
//        return bitmap
//    }
//
//    private fun generateBarcode(orderNumber: String): Bitmap {
//        val width = 400
//        val height = 100
//        val format = BarcodeFormat.CODE_128
//        val hints = mapOf(EncodeHintType.MARGIN to 1)
//
//        return try {
//            val bitMatrix = MultiFormatWriter().encode(orderNumber, format, width, height, hints)
//            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
//            for (x in 0 until width) {
//                for (y in 0 until height) {
//                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
//                }
//            }
//            bitmap
//        } catch (e: Exception) {
//            throw IllegalArgumentException("Could not generate barcode: ${e.message}")
//        }
//    }
//
//
//}