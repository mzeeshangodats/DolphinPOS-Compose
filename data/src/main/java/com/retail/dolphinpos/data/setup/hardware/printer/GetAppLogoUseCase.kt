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
            // Try to load dolphin_logo first (from presentation module)
            val dolphinLogoResId = context.resources.getIdentifier(
                "dolphin_logo",
                "drawable",
                context.packageName
            )
            
            if (dolphinLogoResId != 0) {
                BitmapFactory.decodeResource(context.resources, dolphinLogoResId)
                    ?: loadFallbackLogo()
            } else {
                loadFallbackLogo()
            }
        } catch (e: Exception) {
            loadFallbackLogo()
        }
    }

    private fun loadFallbackLogo(): Bitmap {
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