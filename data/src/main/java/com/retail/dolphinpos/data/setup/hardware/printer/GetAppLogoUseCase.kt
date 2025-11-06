package com.retail.dolphinpos.data.setup.hardware.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.retail.dolphinpos.common.R
import dagger.hilt.android.qualifiers.ApplicationContext

class GetAppLogoUseCase (
    @ApplicationContext private val context: Context
) {

    operator fun invoke(): Bitmap {
        return try {
            BitmapFactory.decodeResource(context.resources, R.drawable.logo_with_bg)
                ?: createDefaultLogo()
        } catch (e: Exception) {
            createDefaultLogo()
        }
    }

    private fun createDefaultLogo(): Bitmap {
        // Create a simple default bitmap if logo is not available
        val width = 200
        val height = 100
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.BLACK)
        return bitmap
    }
}