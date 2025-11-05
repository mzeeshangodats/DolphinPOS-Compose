package com.retail.dolphinpos.data.setup.hardware.printer

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.starmicronics.stario10.InterfaceType
import com.starmicronics.stario10.StarPrinter
import com.starmicronics.stario10.StarPrinterInformation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrinterViewModel @Inject constructor(
    getPrinterDetailsUseCase: GetPrinterDetailsUseCase,
    private val testPrintUseCase: TestPrintUseCase,
    private val savePrinterDetailsUseCase: SavePrinterDetailsUseCase
) : ViewModel() {

    private val _viewState = MutableStateFlow(PrinterViewState())
    val viewState: StateFlow<PrinterViewState> get() = _viewState

    private val _viewEffect = MutableSharedFlow<PrinterViewEffect>()
    val viewEffect: SharedFlow<PrinterViewEffect> get() = _viewEffect.asSharedFlow()


    @Inject
    lateinit var printerManager: PrinterManager


    init {

        _viewState.value = _viewState.value.copy(savedPrinterDetails = getPrinterDetailsUseCase())
    }

    private fun setIsLoading(value: Boolean) {
        //emitViewEffect(ShowLoading(value))
    }

    private fun emitViewEffect(effect: PrinterViewEffect) {
        viewModelScope.launch {
            _viewEffect.emit(effect)
        }

    }


    fun onDeviceClicked(printer: PrinterDetails) {

        handlePrinterConnection(printer) { message ->
            //emitViewEffect(ShowSuccessSnackBar(message))
        }
    }

    private fun handlePrinterConnection(
        printer: PrinterDetails,
        updateMessage: (String) -> Unit
    ) {
        updateMessage("Trying to connect...")

        viewModelScope.launch {
            val success = printerManager.connectAndSavePrinterDetails(
                printer
            ) { connectionStatus ->
                updateMessage(connectionStatus)
            }

            if (!success) {
                //emitViewEffect(ShowErrorSnackBar("Failed to connect to the printer."))
            } else {
                val printerCopy = printer.copy(
                    isAutoOpenDrawerEnabled = _viewState.value.isAutoOpenDrawerEnabled,
                    isAutoPrintReceiptEnabled = _viewState.value.isAutoPrintEnabled
                )
                _viewState.value = _viewState.value.copy(savedPrinterDetails = printerCopy)
                //emitViewEffect(ShowSuccessSnackBar("Printer details saved."))

            }
        }
    }

    fun onTestPrintClicked() {
        handleTestPrint()
    }

    fun openDrawerClicked() {
        openCashDrawer()
    }

    private fun handleTestPrint() {
        //emitViewEffect(ShowInformationSnackBar("Sending test print command..."))

        viewModelScope.launch {
            printerManager.sendTestPrintCommand(
                testPrintUseCase(
                    isGraphicPrinter = viewState.value.savedPrinterDetails?.isGraphic ?: false
                )
            ) { message ->
                //emitViewEffect(ShowSuccessSnackBar(message))
            }
        }

    }


    fun startDiscovery(context: Context, excludeBluetooth: Boolean = false) {

        val interfacesTypesList = mutableListOf(InterfaceType.Lan, InterfaceType.Usb)

        if (!excludeBluetooth) {
            interfacesTypesList += InterfaceType.Bluetooth
        }

        Log.d(TAG, "startDiscovery: $interfacesTypesList")
        _viewState.value = _viewState.value.copy(discoveredPrinters = emptyList())
        DiscoveryManager.startDiscovery(
            context, interfacesTypesList,
            callback = object : DiscoveryManager.DiscoveryCallback {
                override fun onPrinterFound(printer: StarPrinter) {

                    printer.information?.let { information ->
                        val printerDetails = parsePrinterDetails(information)

                        Log.e("PrinterDetails", "Name: ${printerDetails.name}")
                        Log.e("PrinterDetails", "Address: ${printerDetails.address}")
                        Log.e("PrinterDetails", "Connection Type: ${printerDetails.connectionType}")

                        if (printerDetails.connectionType != InterfaceType.Unknown) {
                            addPrinter(printerDetails)
                        } else {
                            Log.e(TAG, "onPrinterFound: Unknown connection type")
                        }

                    }


                }

                override fun onDiscoveryFinished() {
                    //emitViewEffect(ShowInformationSnackBar("Discovery finished. Found ${_viewState.value.discoveredPrinters.size} printers."))
                }

                override fun onError(exception: Exception) {
                    Log.e(TAG, "Discovery error: ${exception.message}", exception)
                    //emitViewEffect(ShowErrorSnackBar("Discovery failed: ${exception.localizedMessage}"))
                }
            }
        )
    }

    fun updateDeviceDiscoveryMessage() {
        _viewState.value = _viewState.value.copy(
            discoveryStatus = null
        )
    }

    private fun stopDiscovery() {
        DiscoveryManager.stopDiscovery()
        _viewState.value = _viewState.value.copy(discoveryStatus = "Discovery stopped.")

    }

    fun updateBluetoothPermissionStatus(isGranted: Boolean) {
        _viewState.value = _viewState.value.copy(isBluetoothPermissionGranted = isGranted)
    }

    override fun onCleared() {
        super.onCleared()
        stopDiscovery()

    }

    private fun parsePrinterDetails(information: StarPrinterInformation): PrinterDetails {
        val connectionType = when {
            !information.detail.lan.ipAddress.isNullOrBlank() -> InterfaceType.Lan
            !information.detail.bluetooth.deviceName.isNullOrBlank() -> InterfaceType.Bluetooth
            !information.detail.usb.portName.isNullOrBlank() -> InterfaceType.Usb
            else -> InterfaceType.Unknown
        }

        val address = when (connectionType) {
            InterfaceType.Lan -> information.detail.lan.macAddress//?.formatAsMacAddress()
            InterfaceType.Bluetooth -> information.detail.bluetooth.address//?.formatAsMacAddress()
            InterfaceType.Usb -> information.detail.usb.portName
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

    fun addPrinter(printer: PrinterDetails) {
        val currentList = _viewState.value.discoveredPrinters.toMutableList()
        if (currentList.none { it.address == printer.address }) {
            currentList.add(printer)
            _viewState.value = _viewState.value.copy(
                discoveredPrinters = currentList
            )
        }
    }

    private fun openCashDrawer() {
        viewModelScope.launch {
            printerManager.openCashDrawer { message ->
                //emitViewEffect(ShowInformationSnackBar(message))
            }
        }
    }

    fun onSaveClicked(isAutoPrintEnabled: Boolean = false, isAutoOpenDrawerEnabled: Boolean = false) {

        _viewState.value.savedPrinterDetails?.let {
            val detailsCopy = it.copy(
                isAutoPrintReceiptEnabled = isAutoPrintEnabled,
                isAutoOpenDrawerEnabled = isAutoOpenDrawerEnabled
            )
            savePrinterDetailsUseCase(detailsCopy)
            //emitViewEffect(ShowSuccessSnackBar("Printer settings updated"))
        } ?: run {
            //emitViewEffect(ShowErrorSnackBar("Please connect device to proceed"))
        }


    }

    companion object {
        private const val TAG = "PrinterViewModel"
    }


}