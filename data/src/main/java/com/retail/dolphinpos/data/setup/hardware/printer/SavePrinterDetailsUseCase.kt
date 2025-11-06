package com.retail.dolphinpos.data.setup.hardware.printer

import android.content.Context
import com.retail.dolphinpos.common.utils.Constants.PRINTER_DETAIL
import com.retail.dolphinpos.common.utils.preferences.saveObjectToSharedPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Old data layer use case - kept for backward compatibility.
 * Use SavePrinterDetailsUseCaseImpl instead.
 * @deprecated Use domain interface SavePrinterDetailsUseCase
 */
class SavePrinterDetailsUseCaseData @Inject constructor(
    @ApplicationContext private val context: Context
) {

    operator fun invoke(printerDetails: PrinterDetailsData) {
        context.saveObjectToSharedPreference(
            PRINTER_DETAIL,
            printerDetails
        )
    }
}