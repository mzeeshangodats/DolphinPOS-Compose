package com.retail.dolphinpos.data.setup.hardware.printer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.starmicronics.stario10.InterfaceType
import com.starmicronics.stario10.StarDeviceDiscoveryManager
import com.starmicronics.stario10.StarDeviceDiscoveryManagerFactory
import com.starmicronics.stario10.StarPrinter

object DiscoveryManager {

    private var manager: StarDeviceDiscoveryManager? = null

    interface DiscoveryCallback {
        fun onPrinterFound(printer: StarPrinter)
        fun onDiscoveryFinished()
        fun onError(exception: Exception)
    }

    /**
     * Starts the discovery process.
     * @param context: Application or activity context.
     * @param interfaceTypes: List of interface types to discover (e.g., LAN, Bluetooth, USB).
     * @param discoveryTime: Time in milliseconds for discovery (default: 10 seconds).
     * @param callback: Callback to notify when printers are found or discovery completes.
     */
    fun startDiscovery(
        context: Context,
        interfaceTypes: List<InterfaceType>,
        discoveryTime: Int = 10000,
        callback: DiscoveryCallback
    ) {
        try {
            stopDiscovery()

            // Handle Bluetooth permission for Android 12+
            if (interfaceTypes.contains(InterfaceType.Bluetooth) && !hasBluetoothPermission(context)) {
                callback.onError(
                    Exception("Bluetooth permission is required for discovery on Android 12+.")
                )
                return
            }

            manager = StarDeviceDiscoveryManagerFactory.create(interfaceTypes, context).apply {
                this.discoveryTime = discoveryTime
                this.callback = object : StarDeviceDiscoveryManager.Callback {
                    override fun onPrinterFound(printer: StarPrinter) {
                        Log.d("DiscoveryManager", "Printer found: ${printer.connectionSettings.identifier}")
                        callback.onPrinterFound(printer)
                    }

                    override fun onDiscoveryFinished() {
                        Log.d("DiscoveryManager", "Discovery finished.")
                        callback.onDiscoveryFinished()
                    }
                }
            }

            manager?.startDiscovery()

        } catch (e: Exception) {
            Log.e("DiscoveryManager", "Error during discovery: ${e.message}")
            callback.onError(e)
        }
    }

    /**
     * Stops the discovery process.
     */
    fun stopDiscovery() {
        try {
            manager?.stopDiscovery()
            manager = null
        } catch (e: Exception) {
            Log.e("DiscoveryManager", "Error stopping discovery: ${e.message}")
        }
    }

    /**
     * Checks if Bluetooth permission is granted (Android 12+).
     */
    private fun hasBluetoothPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}