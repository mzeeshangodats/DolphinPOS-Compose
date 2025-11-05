package com.retail.dolphinpos.data.setup.hardware.printer

import android.graphics.Typeface
import com.starmicronics.stario10.starxpandcommand.printer.ImageParameter

class CreateImageParameterFromTextUseCase(val createBitmapFromTextUseCase: CreateBitmapFromTextUseCase) {

    operator fun invoke(text: String): ImageParameter {
        val width = 576
        val typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        val bitmap = createBitmapFromTextUseCase(
            text = text,
            textSize = 22,
            width = width,
            typeface = typeface
        )
        return ImageParameter(bitmap, width)
    }
}