package com.retail.dolphinpos.data.setup.hardware.printer

import android.content.Context
import com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterConnectionType
import com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterDetails
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.StartDiscoveryUseCase
import com.starmicronics.stario10.InterfaceType
import com.starmicronics.stario10.StarPrinter
import com.starmicronics.stario10.StarPrinterInformation
import javax.inject.Inject

class StartDiscoveryUseCaseImpl @Inject constructor() : StartDiscoveryUseCase {

    override fun invoke(
        context: Context,
        excludeBluetooth: Boolean,
        onPrinterFound: (PrinterDetails) -> Unit,
        onDiscoveryFinished: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val interfacesTypesList = mutableListOf(InterfaceType.Lan, InterfaceType.Usb)
        if (!excludeBluetooth) {
            interfacesTypesList += InterfaceType.Bluetooth
        }

        DiscoveryManager.startDiscovery(
            context,
            interfacesTypesList,
            callback = object : DiscoveryManager.DiscoveryCallback {
                override fun onPrinterFound(printer: StarPrinter) {
                    printer.information?.let { information ->
                        val printerDetails = parsePrinterDetails(information)
                        if (printerDetails.connectionType != PrinterConnectionType.UNKNOWN) {
                            onPrinterFound(printerDetails)
                        }
                    }
                }

                override fun onDiscoveryFinished() {
                    onDiscoveryFinished()
                }

                override fun onError(exception: Exception) {
                    onError(exception)
                }
            }
        )
    }

    override fun stopDiscovery() {
        DiscoveryManager.stopDiscovery()
    }

    private fun parsePrinterDetails(information: StarPrinterInformation): PrinterDetails {
        val connectionType = when {
            !information.detail.lan.ipAddress.isNullOrBlank() -> PrinterConnectionType.LAN
            !information.detail.bluetooth.deviceName.isNullOrBlank() -> PrinterConnectionType.BLUETOOTH
            !information.detail.usb.portName.isNullOrBlank() -> PrinterConnectionType.USB
            else -> PrinterConnectionType.UNKNOWN
        }

        val address = when (connectionType) {
            PrinterConnectionType.LAN -> information.detail.lan.macAddress
            PrinterConnectionType.BLUETOOTH -> information.detail.bluetooth.address
            PrinterConnectionType.USB -> information.detail.usb.portName
            else -> "N/A"
        }

        val printerName = "${information.emulation.name} (${information.model.name})"

        return PrinterDetails(
            name = printerName,
            address = address?.replace("(..)(?!$)", "$1-") ?: "N/A",
            connectionType = connectionType,
            isGraphic = printerName.contains("graphic", ignoreCase = true)
        )
    }
}

