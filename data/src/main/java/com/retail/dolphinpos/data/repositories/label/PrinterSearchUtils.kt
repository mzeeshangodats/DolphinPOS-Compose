package com.retail.dolphinpos.data.repositories.label

import android.content.Context
import android.hardware.usb.UsbManager
import android.util.Log
import com.retail.dolphinpos.domain.model.label.DiscoveredPrinterInfo
import com.retail.dolphinpos.domain.usecases.label.PrinterSearchError
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun searchUsbPrinters(context: Context, targetModels: Array<String>): List<DiscoveredPrinterInfo> =
    suspendCancellableCoroutine { cont ->
        try {
            // Check for USB permission before starting search
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            // (Here, you may filter for Brother devices by vendor ID; for example 0x04F9)
            val brotherDevices = usbManager.deviceList.values.filter { it.vendorId == 0x04F9 }
            if (brotherDevices.isEmpty()) {
                Log.d("PrinterSearchUtils", "No Brother devices found")
                cont.resume(emptyList())
                return@suspendCancellableCoroutine
            }
            
            // If any of the discovered devices do NOT have permission, we treat it as not permitted.
            if (brotherDevices.any { !usbManager.hasPermission(it) }) {
                Log.e("PrinterSearchUtils", "USB permission not granted for Brother devices")
                cont.resumeWithException(Exception("USB permission not granted. Please grant USB permission for the printer."))
                return@suspendCancellableCoroutine
            }

            // Proceed with the search using the provided USBPrinterSearcher
            val searcher = USBPrinterSearcher()
            searcher.start(context, targetModels) { error, sdkError, data ->
                when (error) {
                    PrinterSearchError.None -> {
                        if (data.isEmpty()) {
                            Log.d("PrinterSearchUtils", "No printers found")
                            cont.resume(emptyList())
                        } else {
                            Log.d("PrinterSearchUtils", "Found ${data.size} printers")
                            cont.resume(data)
                        }
                    }
                    PrinterSearchError.USBPermissionNotGrant -> {
                        val errorMsg = "USB permission not granted. Error code: $sdkError"
                        Log.e("PrinterSearchUtils", errorMsg)
                        cont.resumeWithException(Exception(errorMsg))
                    }
                    null -> {
                        val errorMsg = "Unknown printer search error. Error code: $sdkError"
                        Log.e("PrinterSearchUtils", errorMsg)
                        cont.resumeWithException(Exception(errorMsg))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PrinterSearchUtils", "Exception during printer search: ${e.message}", e)
            cont.resumeWithException(Exception("Failed to search for printers: ${e.message}"))
        }
    }

