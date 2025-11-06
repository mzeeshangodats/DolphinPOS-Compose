package com.retail.dolphinpos.data.setup.hardware.printer

import android.content.Context
import com.retail.dolphinpos.common.utils.Constants.PRINTER_DETAIL
import com.retail.dolphinpos.common.utils.preferences.getObjectFromSharedPreference
import com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterDetails
import com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterConnectionType
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.GetPrinterDetailsUseCase
import com.starmicronics.stario10.InterfaceType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class GetPrinterDetailsUseCaseImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GetPrinterDetailsUseCase {

    override operator fun invoke(): PrinterDetails? {
        val dataPrinterDetails = context.getObjectFromSharedPreference<PrinterDetailsData>(PRINTER_DETAIL)
        return dataPrinterDetails?.toDomain()
    }

    private fun PrinterDetailsData.toDomain(): PrinterDetails {
        return PrinterDetails(
            name = this.name,
            address = this.address,
            connectionType = this.connectionType.toDomain(),
            isGraphic = this.isGraphic,
            isAutoPrintReceiptEnabled = this.isAutoPrintReceiptEnabled,
            isAutoOpenDrawerEnabled = this.isAutoOpenDrawerEnabled
        )
    }

    private fun InterfaceType.toDomain(): PrinterConnectionType {
        return when (this) {
            InterfaceType.Lan -> PrinterConnectionType.LAN
            InterfaceType.Bluetooth -> PrinterConnectionType.BLUETOOTH
            InterfaceType.Usb -> PrinterConnectionType.USB
            else -> PrinterConnectionType.UNKNOWN
        }
    }
}

