package com.retail.dolphinpos.data.setup.hardware.printer

import android.content.Context
import com.retail.dolphinpos.common.utils.Constants.PRINTER_DETAIL
import com.retail.dolphinpos.common.utils.preferences.getObjectFromSharedPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class GetPrinterDetailsUseCase @Inject constructor(@param:ApplicationContext val context: Context) {

    operator fun invoke() = context.getObjectFromSharedPreference<PrinterDetails>(PRINTER_DETAIL)

}