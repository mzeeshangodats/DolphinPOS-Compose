package com.retail.dolphinpos.data.setup.hardware.printer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

class CreateBitmapFromTextUseCase {

    operator fun invoke(text: String, textSize: Int, width: Int, typeface: Typeface?) : Bitmap{

        val paint = Paint()
        val bitmap: Bitmap
        paint.textSize = textSize.toFloat()
        paint.typeface = typeface
        paint.getTextBounds(text, 0, text.length, Rect())
        val textPaint = TextPaint(paint)
        val builder = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)

        val staticLayout = builder.build()

        bitmap = Bitmap.createBitmap(
            staticLayout.width,
            staticLayout.height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.translate(0f, 0f)
        staticLayout.draw(canvas)
        return bitmap
    }
}