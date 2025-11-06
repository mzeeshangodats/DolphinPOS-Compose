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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrinterSetupViewModel @Inject constructor(
    getPrinterDetailsUseCase: GetPrinterDetailsUseCase,
    private val testPrintUseCase: TestPrintUseCase,
    private val savePrinterDetailsUseCase: SavePrinterDetailsUseCase,
    private val startDiscoveryUseCase: StartDiscoveryUseCase,
    private val connectPrinterUseCase: ConnectPrinterUseCase,
    private val openCashDrawerUseCase: OpenCashDrawerUseCase
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
        
        startDiscoveryUseCase.invoke(
            context = context,
            excludeBluetooth = excludeBluetooth,
            onPrinterFound = { printer ->
                addPrinter(printer)
            },
            onDiscoveryFinished = {
                emitViewEffect(
                    PrinterViewEffect.ShowInformationSnackBar(
                        "Discovery finished. Found ${_viewState.value.discoveredPrinters.size} printers."
                    )
                )
            },
            onError = { exception ->
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
                emitViewEffect(PrinterViewEffect.ShowErrorSnackBar("Please connect a printer first before testing print."))
                return@launch
            }
            
            emitViewEffect(PrinterViewEffect.ShowInformationSnackBar("Sending test print command..."))
            
            try {
                val isGraphicPrinter = savedPrinter.isGraphic
                testPrintUseCase(isGraphicPrinter)
                emitViewEffect(PrinterViewEffect.ShowSuccessSnackBar("Test print sent successfully."))
            } catch (e: Exception) {
                emitViewEffect(PrinterViewEffect.ShowErrorSnackBar("Error during test print: ${e.localizedMessage ?: e.message ?: "Unknown error"}"))
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
}

