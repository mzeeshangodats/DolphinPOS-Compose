package com.retail.dolphinpos.presentation.features.ui.setup.printer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterDetails
import com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterViewEffect
import com.retail.dolphinpos.domain.model.setup.hardware.printer.PrinterViewState
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.ConnectPrinterUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.GetPrinterDetailsUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.OpenCashDrawerUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.SavePrinterDetailsUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.StartDiscoveryUseCase
import com.retail.dolphinpos.domain.usecases.setup.hardware.printer.TestPrintUseCase
import com.retail.dolphinpos.domain.usecases.order.GetLastPendingOrderUseCase
import com.retail.dolphinpos.domain.model.order.PendingOrder
import com.retail.dolphinpos.data.setup.hardware.printer.GetPrinterReceiptTemplateUseCase
import com.retail.dolphinpos.data.setup.hardware.printer.PrinterManager
import dagger.hilt.android.lifecycle.HiltViewModel
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrinterSetupViewModel @Inject constructor(
    private val getPrinterDetailsUseCase: GetPrinterDetailsUseCase,
    private val testPrintUseCase: TestPrintUseCase,
    private val savePrinterDetailsUseCase: SavePrinterDetailsUseCase,
    private val startDiscoveryUseCase: StartDiscoveryUseCase,
    private val connectPrinterUseCase: ConnectPrinterUseCase,
    private val openCashDrawerUseCase: OpenCashDrawerUseCase,
    private val getLastPendingOrderUseCase: GetLastPendingOrderUseCase,
    private val getReceiptTemplateUseCase: GetPrinterReceiptTemplateUseCase,
    private val printerManager: PrinterManager
) : ViewModel() {

    private val _viewState = MutableStateFlow(PrinterViewState())
    val viewState: StateFlow<PrinterViewState> = _viewState

    private val _viewEffect = MutableSharedFlow<PrinterViewEffect>()
    val viewEffect: SharedFlow<PrinterViewEffect> = _viewEffect.asSharedFlow()

    init {
        val savedPrinter = getPrinterDetailsUseCase()
        _viewState.value = _viewState.value.copy(
            savedPrinterDetails = savedPrinter,
            isAutoPrintEnabled = savedPrinter?.isAutoPrintReceiptEnabled ?: false,
            isAutoOpenDrawerEnabled = savedPrinter?.isAutoOpenDrawerEnabled ?: false
        )
    }

    fun startDiscovery(context: Context, excludeBluetooth: Boolean = false) {
        _viewState.value = _viewState.value.copy(discoveredPrinters = emptyList())
        emitViewEffect(PrinterViewEffect.ShowLoading(true))

        startDiscoveryUseCase.invoke(
            context = context,
            excludeBluetooth = excludeBluetooth,
            onPrinterFound = { printer ->
                addPrinter(printer)
            },
            onDiscoveryFinished = {
                emitViewEffect(PrinterViewEffect.ShowLoading(false))
                emitViewEffect(
                    PrinterViewEffect.ShowInformationSnackBar(
                        "Discovery finished. Found ${_viewState.value.discoveredPrinters.size} printers."
                    )
                )
            },
            onError = { exception ->
                emitViewEffect(PrinterViewEffect.ShowLoading(false))
                emitViewEffect(
                    PrinterViewEffect.ShowErrorSnackBar(
                        "Discovery failed: ${exception.localizedMessage}"
                    )
                )
            }
        )
    }

    fun stopDiscovery() {
        startDiscoveryUseCase.stopDiscovery()
    }

    fun onDeviceClicked(printer: PrinterDetails) {
        viewModelScope.launch {
            emitViewEffect(PrinterViewEffect.ShowLoading(true))

            val success = connectPrinterUseCase(
                printer = printer,
                onStatusUpdate = { message ->
                    emitViewEffect(PrinterViewEffect.ShowInformationSnackBar(message))
                }
            )

            if (success) {
                val printerCopy = printer.copy(
                    isAutoOpenDrawerEnabled = _viewState.value.isAutoOpenDrawerEnabled,
                    isAutoPrintReceiptEnabled = _viewState.value.isAutoPrintEnabled
                )
                _viewState.value = _viewState.value.copy(savedPrinterDetails = printerCopy)
                emitViewEffect(PrinterViewEffect.ShowSuccessSnackBar("Printer connected successfully."))
            } else {
                emitViewEffect(PrinterViewEffect.ShowErrorSnackBar("Failed to connect to the printer."))
            }

            emitViewEffect(PrinterViewEffect.ShowLoading(false))
        }
    }

    fun onTestPrintClicked() {
        viewModelScope.launch {
            val savedPrinter = _viewState.value.savedPrinterDetails
            if (savedPrinter == null) {
                emitViewEffect(PrinterViewEffect.ShowErrorDialog("Failed to send print command"))
                return@launch
            }

            emitViewEffect(PrinterViewEffect.ShowLoading(true))
            emitViewEffect(PrinterViewEffect.ShowInformationSnackBar("Sending test print command..."))

            try {
                val isGraphicPrinter = savedPrinter.isGraphic
                val statusMessages = mutableListOf<String>()
                
                // Check if printer is connected before proceeding
                if (getPrinterDetailsUseCase() == null) {
                    emitViewEffect(PrinterViewEffect.ShowLoading(false))
                    emitViewEffect(PrinterViewEffect.ShowErrorDialog("Failed to send print command"))
                    return@launch
                }
                
                // Call test print - printer online check is done inside sendTestPrintCommand
                // The testPrintUseCase internally uses sendTestPrintCommand which now checks printer online status
                // Status messages are now properly passed through the callback
                try {
                    testPrintUseCase(
                        isGraphicPrinter = isGraphicPrinter,
                        statusCallback = { message ->
                            statusMessages.add(message)
                            emitViewEffect(PrinterViewEffect.ShowInformationSnackBar(message))
                        }
                    )
                    // Check if any error messages indicate printer is offline
                    val hasOfflineError = statusMessages.any { 
                        it.contains("offline", ignoreCase = true) || 
                        it.contains("not connected", ignoreCase = true) ||
                        it.contains("connection failed", ignoreCase = true) ||
                        it.contains("Failed to connect", ignoreCase = true)
                    }
                    
                    if (hasOfflineError) {
                        val errorMessage = statusMessages.lastOrNull { 
                            it.contains("offline", ignoreCase = true) || 
                            it.contains("not connected", ignoreCase = true) ||
                            it.contains("Failed to connect", ignoreCase = true)
                        } ?: "Printer is offline. Please check printer connection and try again."
                        emitViewEffect(PrinterViewEffect.ShowErrorSnackBar(errorMessage))
                    } else {
                        val successMessage = statusMessages.lastOrNull { 
                            it.contains("success", ignoreCase = true) 
                        } ?: "Test print command sent successfully."
                        emitViewEffect(PrinterViewEffect.ShowSuccessSnackBar(successMessage))
                    }
                } catch (e: Exception) {
                    // Check if error indicates printer is offline
                    val errorMessage = e.message ?: e.localizedMessage ?: "Unknown error"
                    if (errorMessage.contains("offline", ignoreCase = true) ||
                        errorMessage.contains("not connected", ignoreCase = true) ||
                        errorMessage.contains("connection failed", ignoreCase = true) ||
                        errorMessage.contains("connection", ignoreCase = true)) {
                        emitViewEffect(PrinterViewEffect.ShowErrorSnackBar("Printer is offline. Please check printer connection and try again."))
                    } else {
                        emitViewEffect(
                            PrinterViewEffect.ShowErrorSnackBar(
                                "Error during test print: $errorMessage"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                emitViewEffect(
                    PrinterViewEffect.ShowErrorSnackBar(
                        "Error during test print: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
                    )
                )
            } finally {
                emitViewEffect(PrinterViewEffect.ShowLoading(false))
            }
        }
    }

    fun openDrawerClicked() {
        viewModelScope.launch {
            val (success, message) = openCashDrawerUseCase { statusMessage ->
                emitViewEffect(PrinterViewEffect.ShowInformationSnackBar(statusMessage))
            }

            if (success) {
                emitViewEffect(PrinterViewEffect.ShowSuccessSnackBar(message))
            } else {
                emitViewEffect(PrinterViewEffect.ShowErrorSnackBar(message))
            }
        }
    }

    fun onSaveClicked(isAutoPrintEnabled: Boolean, isAutoOpenDrawerEnabled: Boolean) {
        _viewState.value.savedPrinterDetails?.let { savedPrinter ->
            val detailsCopy = savedPrinter.copy(
                isAutoPrintReceiptEnabled = isAutoPrintEnabled,
                isAutoOpenDrawerEnabled = isAutoOpenDrawerEnabled
            )
            savePrinterDetailsUseCase(detailsCopy)
            _viewState.value = _viewState.value.copy(
                savedPrinterDetails = detailsCopy,
                isAutoPrintEnabled = isAutoPrintEnabled,
                isAutoOpenDrawerEnabled = isAutoOpenDrawerEnabled
            )
            emitViewEffect(PrinterViewEffect.ShowSuccessSnackBar("Printer settings updated"))
        } ?: run {
            emitViewEffect(PrinterViewEffect.ShowErrorSnackBar("Please connect device to proceed"))
        }
    }

    fun updateBluetoothPermissionStatus(isGranted: Boolean) {
        _viewState.value = _viewState.value.copy(isBluetoothPermissionGranted = isGranted)
    }

    fun updateAutoPrintEnabled(enabled: Boolean) {
        _viewState.value = _viewState.value.copy(isAutoPrintEnabled = enabled)
    }

    fun updateAutoOpenDrawerEnabled(enabled: Boolean) {
        _viewState.value = _viewState.value.copy(isAutoOpenDrawerEnabled = enabled)
    }

    fun onPrintLastPendingOrderClicked() {
        Log.d(TAG, "onPrintLastPendingOrderClicked: Button clicked, fetching last pending order...")
        viewModelScope.launch {
            emitViewEffect(PrinterViewEffect.ShowLoading(true))
            try {
                Log.d(TAG, "onPrintLastPendingOrderClicked: Calling getLastPendingOrderUseCase...")
                val lastPendingOrder = getLastPendingOrderUseCase()

                if (lastPendingOrder != null) {
                    Log.d(
                        TAG,
                        "onPrintLastPendingOrderClicked: Last pending order found - OrderNumber: ${lastPendingOrder.orderNumber}, OrderId: ${lastPendingOrder.id}, Total: ${lastPendingOrder.total}"
                    )
                    Log.d(
                        TAG,
                        "onPrintLastPendingOrderClicked: Order details - InvoiceNo: ${lastPendingOrder.invoiceNo}, PaymentMethod: ${lastPendingOrder.paymentMethod}, ItemsCount: ${lastPendingOrder.items}"
                    )

                    // Store the order for now - printing logic will be added in next command
                    emitViewEffect(
                        PrinterViewEffect.ShowInformationSnackBar(
                            "Last pending order found: ${lastPendingOrder.orderNumber}. Ready to print."
                        )
                    )
                    printReceiptClicked(lastPendingOrder)
                    Log.d(TAG, "onPrintLastPendingOrderClicked: Success message displayed to user")
                } else {
                    Log.w(
                        TAG,
                        "onPrintLastPendingOrderClicked: No pending orders found in database"
                    )
                    emitViewEffect(
                        PrinterViewEffect.ShowErrorSnackBar("No pending orders found.")
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "onPrintLastPendingOrderClicked: Error fetching last pending order", e)
                Log.e(
                    TAG,
                    "onPrintLastPendingOrderClicked: Error message: ${e.message}, StackTrace: ${e.stackTraceToString()}"
                )
                emitViewEffect(
                    PrinterViewEffect.ShowErrorSnackBar(
                        "Error fetching last pending order: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
                    )
                )
            } finally {
                Log.d(TAG, "onPrintLastPendingOrderClicked: Operation completed, hiding loading")
                emitViewEffect(PrinterViewEffect.ShowLoading(false))
            }
        }
    }

    private fun printReceiptClicked(lastPendingOrder: PendingOrder) {
        Log.d(TAG, "printReceiptClicked: Starting print receipt for order: ${lastPendingOrder.orderNumber}")
        viewModelScope.launch {
            emitViewEffect(PrinterViewEffect.ShowLoading(true))
            
            try {
                val printerDetails = getPrinterDetailsUseCase()
                if (printerDetails == null) {
                    Log.w(TAG, "printReceiptClicked: No printer connected")
                    emitViewEffect(
                        PrinterViewEffect.ShowErrorSnackBar("No printer connected. Please set up printer first.")
                    )
                    return@launch
                }
                
                Log.d(TAG, "printReceiptClicked: Printer found - Name: ${printerDetails.name}, IsGraphic: ${printerDetails.isGraphic}")
                
                // Generate receipt template
                Log.d(TAG, "printReceiptClicked: Generating receipt template")
                emitViewEffect(PrinterViewEffect.ShowInformationSnackBar("Generating receipt template..."))
                val receiptTemplate = getReceiptTemplateUseCase(
                    order = lastPendingOrder,
                    isReceiptForRefund = false
                )
                Log.d(TAG, "printReceiptClicked: Receipt template generated, length: ${receiptTemplate.length}")
                
                // Send print command
                Log.d(TAG, "printReceiptClicked: Sending print command to printer")
                emitViewEffect(PrinterViewEffect.ShowInformationSnackBar("Sending print command..."))
                val success = printerManager.sendPrintCommand(
                    data = receiptTemplate,
                    getPrinterDetailsUseCase = getPrinterDetailsUseCase,
                    statusCallback = { message ->
                        Log.d(TAG, "printReceiptClicked: Printer status: $message")
                        emitViewEffect(PrinterViewEffect.ShowInformationSnackBar(message))
                    }
                )
                
                if (success) {
                    Log.d(TAG, "printReceiptClicked: Receipt printed successfully")
                    // Success popup removed - no need to show "Print command sent successfully" message
                    // emitViewEffect(
                    //     PrinterViewEffect.ShowSuccessSnackBar(
                    //         "Receipt printed successfully for order: ${lastPendingOrder.orderNumber}"
                    //     )
                    // )
                } else {
                    Log.w(TAG, "printReceiptClicked: Printer reported failure")
                }
            } catch (e: Exception) {
                Log.e(TAG, "printReceiptClicked: Error printing receipt", e)
                emitViewEffect(
                    PrinterViewEffect.ShowErrorSnackBar(
                        "Error printing receipt: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
                    )
                )
            } finally {
                Log.d(TAG, "printReceiptClicked: Operation completed")
                emitViewEffect(PrinterViewEffect.ShowLoading(false))
            }
        }
    }

    private fun addPrinter(printer: PrinterDetails) {
        val currentList = _viewState.value.discoveredPrinters.toMutableList()
        if (currentList.none { it.address == printer.address }) {
            currentList.add(printer)
            _viewState.value = _viewState.value.copy(discoveredPrinters = currentList)
        }
    }

    private fun emitViewEffect(effect: PrinterViewEffect) {
        viewModelScope.launch {
            _viewEffect.emit(effect)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopDiscovery()
    }

    companion object {
        private const val TAG = "PrinterSetupViewModel"
    }
}

