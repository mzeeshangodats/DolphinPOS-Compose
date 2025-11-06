package com.retail.dolphinpos.common.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color

fun createCenteredLogoBitmap(logo: Bitmap, receiptWidth: Int = 600, maxHeight: Int = 200): Bitmap {
    val scaledLogo = if (logo.width > receiptWidth / 2) {
        Bitmap.createScaledBitmap(logo, receiptWidth / 2, maxHeight, true)
    } else {
        logo
    }

    val centeredBitmap = Bitmap.createBitmap(receiptWidth, maxHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(centeredBitmap)
    canvas.drawColor(Color.WHITE)

    val left = (receiptWidth - scaledLogo.width) / 2f
    val top = (maxHeight - scaledLogo.height) / 2f

    canvas.drawBitmap(scaledLogo, left, top, null)

    return centeredBitmap
}

fun String.applyStrikethrough(): String {
    return this.map {
        if (it.isWhitespace()) it.toString() else "$it\u0336"
    }.joinToString("")
}