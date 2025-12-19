package com.retail.dolphinpos.common.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream

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

fun getFileName(context: Context, uri: Uri): String {
    var name = "image_${System.currentTimeMillis()}.jpg"
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index != -1) name = it.getString(index)
        }
    }
    return name
}
fun uriToFile(context: Context, uri: Uri): File {
    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
    val extension = MimeTypeMap.getSingleton()
        .getExtensionFromMimeType(mime) ?: "jpg"

    val file = File(
        context.cacheDir,
        "img_${System.currentTimeMillis()}.$extension"
    )

    context.contentResolver.openInputStream(uri)?.use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    } ?: throw IllegalStateException("Cannot open input stream")

    // 🔴 VERIFY FILE
    if (!file.exists() || file.length() == 0L) {
        throw IllegalStateException("File is empty")
    }

    return file
}
