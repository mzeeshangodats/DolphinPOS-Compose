package com.retail.dolphinpos.data.customer_display

import android.util.Log
import com.google.gson.Gson
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.domain.model.home.cart.CartItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerDisplayManager @Inject constructor(
    private val gson: Gson,
    private val preferenceManager: PreferenceManager
) {
    private var server: CustomerDisplayServer? = null
    private val port = 8080

    fun startServer(): Boolean {
        if (!preferenceManager.isCustomerDisplayEnabled()) {
            Log.d(TAG, "Customer Display is disabled")
            return false
        }

        if (server != null && server?.let { isServerRunning() } == true) {
            Log.d(TAG, "Server already running")
            return true
        }

        try {
            server = CustomerDisplayServer(gson, port)
            server?.start()
            Log.d(TAG, "Customer Display Server started")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting server", e)
            return false
        }
    }

    fun stopServer() {
        try {
            server?.stop()
            server = null
            Log.d(TAG, "Customer Display Server stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server", e)
        }
    }

    fun broadcastCartUpdate(status: String, cartItems: List<CartItem>, subtotal: Double, tax: Double, total: Double, cashDiscountTotal: Double, orderDiscountTotal: Double, isCashSelected: Boolean) {
        if (!preferenceManager.isCustomerDisplayEnabled()) {
            return
        }

        if (server == null || !isServerRunning()) {
            // Try to restart server
            startServer()
        }

        server?.broadcastCartUpdate(status, cartItems, subtotal, tax, total, cashDiscountTotal, orderDiscountTotal, isCashSelected)
    }

    fun isServerRunning(): Boolean {
        return server != null
    }

    fun getServerIpAddress(): String? {
        return server?.getLocalIpAddress()
    }

    fun restartServerIfNeeded() {
        val isEnabled = preferenceManager.isCustomerDisplayEnabled()
        if (isEnabled && !isServerRunning()) {
            startServer()
        } else if (!isEnabled && isServerRunning()) {
            stopServer()
        }
    }

    companion object {
        private const val TAG = "CustomerDisplayManager"
    }
}

