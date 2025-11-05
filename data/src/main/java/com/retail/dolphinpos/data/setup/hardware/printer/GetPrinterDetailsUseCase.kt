package com.retail.dolphinpos.data.setup.hardware.printer

import android.content.Context
import com.retail.dolphinpos.common.utils.Constants.PRINTER_DETAIL
import com.retail.dolphinpos.common.utils.preferences.getObjectFromSharedPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Old data layer use case - kept for backward compatibility.
 * Use GetPrinterDetailsUseCaseImpl instead.
 * @deprecated Use domain interface GetPrinterDetailsUseCase
 */
class GetPrinterDetailsUseCaseData @Inject constructor(
    @ApplicationContext private val context: Context
) {

    operator fun invoke() = context.getObjectFromSharedPreference<PrinterDetailsData>(PRINTER_DETAIL)

}