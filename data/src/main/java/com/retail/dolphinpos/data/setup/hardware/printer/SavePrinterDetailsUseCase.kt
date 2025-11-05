package com.retail.dolphinpos.data.setup.hardware.printer

import android.content.Context
import com.retail.dolphinpos.common.utils.Constants.PRINTER_DETAIL
import com.retail.dolphinpos.common.utils.preferences.saveObjectToSharedPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SavePrinterDetailsUseCase @Inject constructor(@param:ApplicationContext val context: Context) {

    operator fun invoke(printerDetails: PrinterDetails) {
        context.saveObjectToSharedPreference(
            PRINTER_DETAIL,
            printerDetails
        )
    }
}