package com.retail.dolphinpos.data.setup.hardware.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.retail.dolphinpos.data.R
import dagger.hilt.android.qualifiers.ApplicationContext

class GetAppLogoUseCase (@param:ApplicationContext val context: Context) {

    operator fun invoke (): Bitmap = BitmapFactory.decodeResource(context.resources, 0/*R.drawable.logo*/)

}