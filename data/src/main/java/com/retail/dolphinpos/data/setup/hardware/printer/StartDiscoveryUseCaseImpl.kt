package com.retail.dolphinpos.data.setup.hardware.printer

import android.content.Context
import android.util.Log
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

        Log.d(TAG, "Starting discovery with interfaces: $interfacesTypesList, excludeBluetooth: $excludeBluetooth")

        // Increase discovery time to 15 seconds for better Bluetooth discovery (especially for QL-800)
        val discoveryTime = if (!excludeBluetooth) 15000 else 10000

        DiscoveryManager.startDiscovery(
            context,
            interfacesTypesList,
            discoveryTime = discoveryTime,
            callback = object : DiscoveryManager.DiscoveryCallback {
                override fun onPrinterFound(printer: StarPrinter) {
                    Log.d(TAG, "Printer found: ${printer.connectionSettings.identifier}")
                    printer.information?.let { information ->
                        Log.d(TAG, "Printer information - Model: ${information.model.name}, Emulation: ${information.emulation.name}")
                        Log.d(TAG, "LAN IP: ${information.detail.lan.ipAddress}, MAC: ${information.detail.lan.macAddress}")
                        Log.d(TAG, "Bluetooth Name: ${information.detail.bluetooth.deviceName}, Address: ${information.detail.bluetooth.address}")
                        Log.d(TAG, "USB Port: ${information.detail.usb.portName}")

                        val printerDetails = parsePrinterDetails(information)
                        Log.d(TAG, "Parsed printer details - Name: ${printerDetails.name}, Address: ${printerDetails.address}, Type: ${printerDetails.connectionType}")

                        if (printerDetails.connectionType != PrinterConnectionType.UNKNOWN) {
                            onPrinterFound(printerDetails)
                        } else {
                            Log.w(TAG, "Printer connection type is UNKNOWN, skipping printer: ${printer.connectionSettings.identifier}")
                        }
                    } ?: run {
                        Log.w(TAG, "Printer information is null for printer: ${printer.connectionSettings.identifier}")
                    }
                }

                override fun onDiscoveryFinished() {
                    Log.d(TAG, "Discovery finished")
                    onDiscoveryFinished()
                }

                override fun onError(exception: Exception) {
                    Log.e(TAG, "Discovery error: ${exception.message}", exception)
                    onError(exception)
                }
            }
        )
    }

    override fun stopDiscovery() {
        DiscoveryManager.stopDiscovery()
    }

    private fun parsePrinterDetails(information: StarPrinterInformation): PrinterDetails {
        // Check connection type - prioritize by availability
        // For Bluetooth, check both deviceName and address as some printers might not have deviceName
        val connectionType = when {
            !information.detail.lan.ipAddress.isNullOrBlank() -> PrinterConnectionType.LAN
            !information.detail.bluetooth.deviceName.isNullOrBlank() ||
                    !information.detail.bluetooth.address.isNullOrBlank() -> PrinterConnectionType.BLUETOOTH
            !information.detail.usb.portName.isNullOrBlank() -> PrinterConnectionType.USB
            else -> PrinterConnectionType.UNKNOWN
        }

        val address = when (connectionType) {
            PrinterConnectionType.LAN -> {
                // Prefer MAC address, fallback to IP address
                information.detail.lan.macAddress ?: information.detail.lan.ipAddress
            }
            PrinterConnectionType.BLUETOOTH -> {
                // Prefer address, fallback to deviceName
                information.detail.bluetooth.address ?: information.detail.bluetooth.deviceName
            }
            PrinterConnectionType.USB -> information.detail.usb.portName
            else -> "N/A"
        }

        // Build printer name - use model name and emulation
        val modelName = information.model.name
        val emulationName = information.emulation.name
        val printerName = if (modelName.isNotBlank() && emulationName.isNotBlank()) {
            "$emulationName ($modelName)"
        } else if (modelName.isNotBlank()) {
            modelName
        } else if (emulationName.isNotBlank()) {
            emulationName
        } else {
            "Unknown Printer"
        }

        // Format MAC address with colons (for display)
        val formattedAddress = if (connectionType == PrinterConnectionType.LAN && address != null) {
            // Format MAC address: XX:XX:XX:XX:XX:XX
            address.replace("(..)(?!$)".toRegex(), "$1:")
        } else {
            address ?: "N/A"
        }

        return PrinterDetails(
            name = printerName,
            address = formattedAddress,
            connectionType = connectionType,
            isGraphic = printerName.contains("graphic", ignoreCase = true) ||
                    modelName.contains("graphic", ignoreCase = true)
        )
    }

    companion object {
        private const val TAG = "StartDiscoveryUseCase"
    }
}

