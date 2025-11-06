package com.retail.dolphinpos.data.setup.hardware.printer

import android.content.Context
import android.util.Log
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.GetPrinterDetailsUseCase
import com.starmicronics.stario10.PrinterDelegate
import com.starmicronics.stario10.StarConnectionSettings
import com.starmicronics.stario10.StarIO10Exception
import com.starmicronics.stario10.StarPrinter
import com.starmicronics.stario10.starxpandcommand.DocumentBuilder
import com.starmicronics.stario10.starxpandcommand.DrawerBuilder
import com.starmicronics.stario10.starxpandcommand.StarXpandCommandBuilder
import com.starmicronics.stario10.starxpandcommand.drawer.Channel
import com.starmicronics.stario10.starxpandcommand.drawer.OpenParameter
import com.starmicronics.stario10.InterfaceType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrinterManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var printer: StarPrinter? = null
    private var isMonitoring: Boolean = false

    suspend fun connectAndSavePrinterDetails(
        printerDetails: PrinterDetailsData,
        statusCallback: (String) -> Unit
    ): Boolean {
        val settings = StarConnectionSettings(printerDetails.connectionType, printerDetails.address)

        try {
            printer = StarPrinter(settings, context).apply {
                printerDelegate = createPrinterDelegate(statusCallback)
            }

            printer?.openAsync()?.await()

            isMonitoring = true
            statusCallback("Printer connected and details saved successfully.")
            return true
        } catch (e: Exception) {
            statusCallback("Error connecting to printer: ${e.localizedMessage}")
            return false
        }
    }

    suspend fun reconnectToPrinter(
        getPrinterDetailsUseCase: GetPrinterDetailsUseCase,
        statusCallback: (String) -> Unit
    ): Boolean {
        if (printer != null) {
            statusCallback("Reusing existing printer connection.")
            return true
        }

        val printerDetails = getPrinterDetailsUseCase()?.let { domainPrinter ->
            // Convert domain to data
            PrinterDetailsData(
                name = domainPrinter.name,
                address = domainPrinter.address,
                connectionType = domainPrinter.connectionType.toInterfaceType(),
                isGraphic = domainPrinter.isGraphic,
                isAutoPrintReceiptEnabled = domainPrinter.isAutoPrintReceiptEnabled,
                isAutoOpenDrawerEnabled = domainPrinter.isAutoOpenDrawerEnabled
            )
        } ?: return false.also { statusCallback("No printer details saved. Please set up the printer again.") }

        return connectAndSavePrinterDetails(printerDetails, statusCallback)
    }

    suspend fun sendPrintCommand(
        data: String,
        getPrinterDetailsUseCase: GetPrinterDetailsUseCase,
        statusCallback: (String) -> Unit
    ) {
        if (reconnectToPrinter(getPrinterDetailsUseCase, statusCallback)) {
            try {
                printer?.printAsync(data)?.await()
                statusCallback("Print command sent successfully.")
            } catch (e: Exception) {
                statusCallback("Error printing: ${e.localizedMessage}")
            }
        } else {
            statusCallback("Printer connection failed. Cannot send print command.")
        }
    }

    suspend fun sendTestPrintCommand(
        template: String,
        getPrinterDetailsUseCase: GetPrinterDetailsUseCase,
        statusCallback: (String) -> Unit
    ) {
        try {
            reconnectToPrinter(getPrinterDetailsUseCase, statusCallback)
            printer?.printAsync(template)?.await()
            statusCallback("Test print command sent successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "sendTestPrintCommand: ${e.message}", e)
            statusCallback("Error during test print: ${e.localizedMessage}")
        }
    }

    suspend fun openCashDrawer(
        getPrinterDetailsUseCase: GetPrinterDetailsUseCase,
        statusCallback: (String) -> Unit
    ): Pair<Boolean, String> {
        statusCallback("Attempting to reconnect to printer...")
        return if (reconnectToPrinter(getPrinterDetailsUseCase, statusCallback)) {
            try {
                withContext(Dispatchers.IO) {
                    printer?.printAsync(getCommandsForOpenCashDrawer())?.await()
                }
                val successMessage = "Cash drawer opened successfully."
                statusCallback(successMessage)
                true to successMessage
            } catch (e: Exception) {
                val errorMessage = "Error opening cash drawer: ${e.localizedMessage}"
                statusCallback(errorMessage)
                false to errorMessage
            }
        } else {
            val failureMessage = "Printer connection failed. Cannot open cash drawer."
            statusCallback(failureMessage)
            false to failureMessage
        }
    }

    private fun getCommandsForOpenCashDrawer(): String {
        val builder = StarXpandCommandBuilder()
        builder.addDocument(
            DocumentBuilder()
                .addDrawer(
                    DrawerBuilder()
                        .actionOpen(
                            OpenParameter()
                                .setChannel(Channel.No1) // Set Channel 1
                        )
                )
        )
        return builder.getCommands()
    }

    suspend fun sendPrintCommands(
        dataList: List<String>,
        getPrinterDetailsUseCase: GetPrinterDetailsUseCase,
        statusCallback: (String) -> Unit
    ): Boolean {
        if (!reconnectToPrinter(getPrinterDetailsUseCase, statusCallback)) {
            statusCallback("Printer connection failed. Cannot send print commands.")
            return false
        }

        try {
            for (data in dataList) {
                var attempt = 0
                var success = false

                while (attempt < 2 && !success) { // Retry once if needed
                    try {
                        printer?.printAsync(data)?.await()
                        statusCallback("Printed: $data")
                        success = true
                    } catch (e: Exception) {
                        if (e.localizedMessage?.contains("paper", ignoreCase = true) == true) {
                            if (attempt == 0) {
                                statusCallback("Paper ran out, please reload. Retrying...")
                            } else {
                                statusCallback("Printing failed after retry. Skipping: $data")
                            }
                        } else {
                            statusCallback("Error printing: ${e.localizedMessage}")
                            return false // Stop function if a critical error occurs
                        }
                    }
                    attempt++
                }
            }
            statusCallback("All print commands processed successfully.")
            return true // Success
        } catch (e: Exception) {
            statusCallback("Unexpected error: ${e.localizedMessage}")
            return false
        }
    }

    private fun createPrinterDelegate(statusCallback: (String) -> Unit) = object : PrinterDelegate() {
        override fun onReady() = statusCallback("Printer is Ready")
        override fun onError() = statusCallback("Printer encountered an Error")
        override fun onCommunicationError(e: StarIO10Exception) = statusCallback("Communication Error: ${e.message}")
        override fun onPaperEmpty() = statusCallback("Printer is out of Paper")
    }

    private fun com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterConnectionType.toInterfaceType(): InterfaceType {
        return when (this) {
            com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterConnectionType.LAN -> InterfaceType.Lan
            com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterConnectionType.BLUETOOTH -> InterfaceType.Bluetooth
            com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterConnectionType.USB -> InterfaceType.Usb
            com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterConnectionType.UNKNOWN -> InterfaceType.Unknown
        }
    }

    companion object {
        private const val TAG = "PrinterManager"
    }
}