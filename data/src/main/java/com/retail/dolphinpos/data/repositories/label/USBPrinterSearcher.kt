package com.retail.dolphinpos.data.repositories.label

import android.content.Context
import android.util.Log
import com.brother.sdk.lmprinter.Channel
import com.brother.sdk.lmprinter.PrinterSearcher
import com.retail.dolphinpos.domain.model.label.DiscoveredPrinterInfo
import com.retail.dolphinpos.domain.usecases.label.PrinterSearchError
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class USBPrinterSearcher : IPrinterSearcher, CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Default + Job()

    override fun start(
        context: Context,
        targetModels: Array<String>,
        callback: (PrinterSearchError?, com.brother.sdk.lmprinter.PrinterSearchError.ErrorCode?, ArrayList<DiscoveredPrinterInfo>) -> Unit
    ) {
        launch {
            try {
                val result = PrinterSearcher.startUSBSearch(context)

                when (result.error.code) {
                    com.brother.sdk.lmprinter.PrinterSearchError.ErrorCode.NoError -> {
                        if (result.channels.isEmpty()) {
                            withContext(Dispatchers.Main) {
                                callback(PrinterSearchError.None, result.error.code, arrayListOf())
                            }
                            return@launch
                        }
                    }

                    com.brother.sdk.lmprinter.PrinterSearchError.ErrorCode.NotPermitted -> {
                        withContext(Dispatchers.Main) {
                            callback(
                                PrinterSearchError.USBPermissionNotGrant,
                                result.error.code,
                                arrayListOf()
                            )
                        }
                        return@launch
                    }

                    com.brother.sdk.lmprinter.PrinterSearchError.ErrorCode.Canceled,
                    com.brother.sdk.lmprinter.PrinterSearchError.ErrorCode.InterfaceInactive,
                    com.brother.sdk.lmprinter.PrinterSearchError.ErrorCode.InterfaceUnsupported,
                    com.brother.sdk.lmprinter.PrinterSearchError.ErrorCode.AlreadySearching,
                    com.brother.sdk.lmprinter.PrinterSearchError.ErrorCode.UnknownError -> {
                        withContext(Dispatchers.Main) {
                            callback(
                                PrinterSearchError.USBPermissionNotGrant,
                                result.error.code,
                                arrayListOf()
                            )
                        }
                        return@launch
                    }

                    null -> {
                        withContext(Dispatchers.Main) {
                            callback(
                                PrinterSearchError.USBPermissionNotGrant,
                                null,
                                arrayListOf()
                            )
                        }
                        return@launch
                    }
                }

                // require permission: com.android.example.USB_PERMISSION
                val usbDeviceList = result.channels.map {
                    DiscoveredPrinterInfo(
                        modelName = it.extraInfo[Channel.ExtraInfoKey.ModelName] ?: "",
                        address = it.channelInfo,
                        connectionType = "USB",
                        extraInfo = it.extraInfo as Map<String, String>?
                    )
                }
                withContext(Dispatchers.Main) {
                    callback(PrinterSearchError.None, result.error.code, ArrayList(usbDeviceList))
                }
            } catch (e: Exception) {
                Log.e("USBPrinterSearcher", "Error during USB printer search: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(
                        PrinterSearchError.USBPermissionNotGrant,
                        null,
                        arrayListOf()
                    )
                }
            }
        }
    }

    override fun cancel() {
        // ignore
    }
}

