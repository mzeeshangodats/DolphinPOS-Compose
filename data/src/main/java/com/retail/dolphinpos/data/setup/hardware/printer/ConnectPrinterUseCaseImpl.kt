package com.retail.dolphinpos.data.setup.hardware.printer

import com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterConnectionType
import com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterDetails
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.ConnectPrinterUseCase
import com.starmicronics.stario10.InterfaceType
import com.starmicronics.stario10.StarConnectionSettings
import javax.inject.Inject

class ConnectPrinterUseCaseImpl @Inject constructor(
    private val printerManager: PrinterManager,
    private val savePrinterDetailsUseCase: com.retail.dolphinpos.domain.usecases.setup.hardware.printer.SavePrinterDetailsUseCase
) : ConnectPrinterUseCase {

    override suspend operator fun invoke(
        printer: com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterDetails,
        onStatusUpdate: (String) -> Unit
    ): Boolean {
        val dataPrinterDetails = printer.toData()
        val success = printerManager.connectAndSavePrinterDetails(
            dataPrinterDetails,
            onStatusUpdate
        )
        
        if (success) {
            savePrinterDetailsUseCase(printer)
        }
        
        return success
    }

    private fun com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterDetails.toData(): PrinterDetailsData {
        val interfaceType = when (this.connectionType) {
            com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterConnectionType.LAN -> InterfaceType.Lan
            com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterConnectionType.BLUETOOTH -> InterfaceType.Bluetooth
            com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterConnectionType.USB -> InterfaceType.Usb
            com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterConnectionType.UNKNOWN -> InterfaceType.Unknown
        }

        return PrinterDetailsData(
            name = this.name,
            address = this.address,
            connectionType = interfaceType,
            isGraphic = this.isGraphic,
            isAutoPrintReceiptEnabled = this.isAutoPrintReceiptEnabled,
            isAutoOpenDrawerEnabled = this.isAutoOpenDrawerEnabled
        )
    }
}

