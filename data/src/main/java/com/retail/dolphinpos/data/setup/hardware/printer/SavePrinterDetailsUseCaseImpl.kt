package com.retail.dolphinpos.data.setup.hardware.printer

import android.content.Context
import com.retail.dolphinpos.common.utils.Constants.PRINTER_DETAIL
import com.retail.dolphinpos.common.utils.preferences.saveObjectToSharedPreference
import com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterConnectionType
import com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterDetails
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.SavePrinterDetailsUseCase
import com.starmicronics.stario10.InterfaceType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SavePrinterDetailsUseCaseImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SavePrinterDetailsUseCase {

    override operator fun invoke(printerDetails: PrinterDetails) {
        val dataPrinterDetails = printerDetails.toData()
        context.saveObjectToSharedPreference(
            PRINTER_DETAIL,
            dataPrinterDetails
        )
    }

    private fun PrinterDetails.toData(): PrinterDetailsData {
        return PrinterDetailsData(
            name = this.name,
            address = this.address,
            connectionType = this.connectionType.toData(),
            isGraphic = this.isGraphic,
            isAutoPrintReceiptEnabled = this.isAutoPrintReceiptEnabled,
            isAutoOpenDrawerEnabled = this.isAutoOpenDrawerEnabled
        )
    }

    private fun PrinterConnectionType.toData(): InterfaceType {
        return when (this) {
            PrinterConnectionType.LAN -> InterfaceType.Lan
            PrinterConnectionType.BLUETOOTH -> InterfaceType.Bluetooth
            PrinterConnectionType.USB -> InterfaceType.Usb
            PrinterConnectionType.UNKNOWN -> InterfaceType.Unknown
        }
    }
}

