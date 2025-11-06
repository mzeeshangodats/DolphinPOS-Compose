package com.retail.dolphinpos.data.usecases.order

import android.content.Context
import com.retail.dolphinpos.common.utils.preferences.PRINTER_DETAIL
import com.retail.dolphinpos.common.utils.preferences.getObjectFromSharedPreference
import com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterDetails
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class GetPrinterDetailsUseCase @Inject constructor(@ApplicationContext val context: Context) {

    operator fun invoke() = context.getObjectFromSharedPreference<PrinterDetails>(PRINTER_DETAIL)

}