package com.retail.dolphinpos.data.setup.hardware.barcode

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

open class TransparentOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class OverlayShape {
        RECTANGLE, SQUARE
    }

    private val overlayPaint = Paint().apply {
        color = ContextCompat.getColor(context, android.R.color.black)
        alpha = 150
    }

    private val transparentPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val rectBounds = RectF()
    private val cornerRadius = 30f

    var overlayShape: OverlayShape = OverlayShape.RECTANGLE
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        when (overlayShape) {
            OverlayShape.RECTANGLE -> drawRectangleOverlay(canvas)
            OverlayShape.SQUARE -> drawSquareOverlay(canvas)
        }
    }

    private fun drawRectangleOverlay(canvas: Canvas) {
        val rectWidth = width * 0.7f
        val rectHeight = height * 0.3f
        val left = (width - rectWidth) / 2
        val top = (height - rectHeight) / 2
        val right = left + rectWidth
        val bottom = top + rectHeight
        rectBounds.set(left, top, right, bottom)

        canvas.drawRoundRect(rectBounds, cornerRadius, cornerRadius, transparentPaint)
    }

    private fun drawSquareOverlay(canvas: Canvas) {
        val squareSize = minOf(width, height) * 0.5f
        val left = (width - squareSize) / 2
        val top = (height - squareSize) / 2
        val right = left + squareSize
        val bottom = top + squareSize
        rectBounds.set(left, top, right, bottom)

        canvas.drawRoundRect(rectBounds, cornerRadius, cornerRadius, transparentPaint)
    }

    fun getTransparentRect(): RectF {
        return rectBounds
    }
}

