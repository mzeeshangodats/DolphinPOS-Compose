package com.retail.dolphinpos.presentation.features.ui.setup.label_printer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log

class UsbEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        intent?.let {
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED == it.action ||
                UsbManager.ACTION_USB_ACCESSORY_ATTACHED == it.action
            ) {

                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                Log.d("UsbEventReceiver", "USB device attached: " + (device?.deviceName ?: "unknown"))
            }
        }

    }
}