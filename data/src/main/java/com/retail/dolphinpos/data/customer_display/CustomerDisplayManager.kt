package com.retail.dolphinpos.data.customer_display

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.data.customer_display.CartDisplayData
import com.retail.dolphinpos.domain.model.home.cart.CartItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerDisplayManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    private val preferenceManager: PreferenceManager
) {
    private var client: CustomerDisplayClient? = null
    private val port = 8080

    fun connectToCfdDevice(): Boolean {
        Log.d(TAG, "connectToCfdDevice() called")
        
        if (!preferenceManager.isCustomerDisplayEnabled()) {
            Log.w(TAG, "Customer Display is disabled in preferences, cannot connect")
            return false
        }

        val cfdIpAddress = preferenceManager.getCustomerDisplayIpAddress()
        if (cfdIpAddress.isBlank()) {
            Log.w(TAG, "CFD IP address is not configured")
            Log.w(TAG, "Please enter the CFD device IP address in Customer Display Setup")
            return false
        }

        // If already connected to the same IP, don't reconnect
        if (client != null && client?.isConnected() == true) {
            Log.d(TAG, "Already connected to CFD device at $cfdIpAddress:$port")
            return true
        }

        try {
            // Disconnect existing client if any
            disconnectFromCfdDevice()
            
            Log.d(TAG, "Creating new CustomerDisplayClient to connect to $cfdIpAddress:$port")
            Log.d(TAG, "WebSocket URL: ws://$cfdIpAddress:$port/customer-display")
            Log.d(TAG, "Make sure CFD device is running a WebSocket server on port $port")
            
            client = CustomerDisplayClient(gson, cfdIpAddress, port)
            client?.connect()
            Log.d(TAG, "✓ Customer Display Client connection initiated to $cfdIpAddress:$port")
            Log.d(TAG, "Waiting for connection... (check logs for connection status)")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error connecting to CFD device", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}")
            Log.e(TAG, "Troubleshooting:")
            Log.e(TAG, "1. Verify CFD device IP address: $cfdIpAddress")
            Log.e(TAG, "2. Check if both devices are on the same Wi-Fi network")
            Log.e(TAG, "3. Ensure CFD device has WebSocket server running on port $port")
            Log.e(TAG, "4. Check firewall settings on both devices")
            return false
        }
    }

    fun disconnectFromCfdDevice() {
        try {
            client?.disconnect()
            client = null
            Log.d(TAG, "Customer Display Client disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting client", e)
        }
    }

    fun broadcastCartUpdate(status: String, cartItems: List<CartItem>, subtotal: Double, tax: Double, total: Double, cashDiscountTotal: Double, orderDiscountTotal: Double, isCashSelected: Boolean) {
        if (!preferenceManager.isCustomerDisplayEnabled()) {
            Log.d(TAG, "Skipping broadcast - Customer Display is disabled")
            return
        }

        val cfdIpAddress = preferenceManager.getCustomerDisplayIpAddress()
        if (cfdIpAddress.isBlank()) {
            Log.w(TAG, "CFD IP address not configured, cannot send cart update")
            return
        }

        // Ensure we're connected
        if (client == null || client?.isConnected() != true) {
            Log.w(TAG, "Not connected to CFD device, attempting to connect...")
            connectToCfdDevice()
            // Wait a bit for connection
            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                // Ignore
            }
        }

        if (client?.isConnected() != true) {
            Log.e(TAG, "Not connected to CFD device, cannot send cart update")
            return
        }

        val cartData = CartDisplayData(
            status = status,
            cartItems = cartItems,
            subtotal = subtotal,
            tax = tax,
            total = total,
            cashDiscountTotal = cashDiscountTotal,
            orderDiscountTotal = orderDiscountTotal,
            isCashSelected = isCashSelected,
            timestamp = System.currentTimeMillis()
        )
        
        val json = gson.toJson(cartData)
        Log.d(TAG, "Sending cart update to CFD device: status=$status, items=${cartItems.size}, total=$total")
        
        // Send via client
        client?.send(json)
    }

    fun isConnected(): Boolean {
        return client?.isConnected() == true
    }

    fun getCfdIpAddress(): String? {
        return preferenceManager.getCustomerDisplayIpAddress().takeIf { it.isNotBlank() }
    }

    fun restartConnectionIfNeeded() {
        val isEnabled = preferenceManager.isCustomerDisplayEnabled()
        val cfdIp = preferenceManager.getCustomerDisplayIpAddress()
        
        if (isEnabled && cfdIp.isNotBlank() && !isConnected()) {
            connectToCfdDevice()
        } else if (!isEnabled || cfdIp.isBlank()) {
            disconnectFromCfdDevice()
        }
    }
    
    // Legacy method names for compatibility
    fun isServerRunning(): Boolean = isConnected()
    fun getServerIpAddress(): String? = getCfdIpAddress()
    fun restartServerIfNeeded() = restartConnectionIfNeeded()

    companion object {
        private const val TAG = "CustomerDisplayManager"
    }
}

