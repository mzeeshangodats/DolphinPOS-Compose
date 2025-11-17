package com.retail.dolphinpos.data.customer_display

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.retail.dolphinpos.domain.model.home.cart.CartItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

class CustomerDisplayClient(
    private val gson: Gson,
    private val serverIp: String,
    private val serverPort: Int = 8080
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _cartData = MutableStateFlow<CartDisplayData?>(null)
    val cartData: StateFlow<CartDisplayData?> = _cartData.asStateFlow()

    fun connect() {
        val url = "ws://$serverIp:$serverPort/customer-display"
        Log.d(TAG, "Attempting to connect to WebSocket server: $url")
        
        if (_connectionState.value == ConnectionState.Connected) {
            Log.d(TAG, "Already connected to $url, skipping connection attempt")
            return
        }

        val request = Request.Builder()
            .url(url)
            .build()

        Log.d(TAG, "Creating WebSocket request: $url")
        _connectionState.value = ConnectionState.Connecting
        Log.d(TAG, "Connection state changed to: Connecting")

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "✓ WebSocket connected successfully to $serverIp:$serverPort")
                Log.d(TAG, "Response code: ${response.code}, message: ${response.message}")
                Log.d(TAG, "Response headers: ${response.headers}")
                _connectionState.value = ConnectionState.Connected
                Log.d(TAG, "Connection state changed to: Connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    Log.d(TAG, "Received message from server (length: ${text.length} chars)")
                    val data = gson.fromJson(text, CartDisplayData::class.java)
                    _cartData.value = data
                    Log.d(TAG, "✓ Parsed cart update: status=${data.status}, items=${data.cartItems.size}, total=${data.total}")
                } catch (e: JsonSyntaxException) {
                    Log.e(TAG, "✗ Error parsing cart data JSON", e)
                    Log.e(TAG, "JSON content: ${text.take(500)}")
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Unexpected error processing message", e)
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d(TAG, "Received binary message (${bytes.size} bytes), converting to text")
                onMessage(webSocket, bytes.utf8())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: code=$code, reason=$reason")
                _connectionState.value = ConnectionState.Disconnected
                Log.d(TAG, "Connection state changed to: Disconnected")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: code=$code, reason=$reason")
                _connectionState.value = ConnectionState.Disconnected
                Log.d(TAG, "Connection state changed to: Disconnected")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "✗ WebSocket connection failed to $serverIp:$serverPort", t)
                Log.e(TAG, "Failure details: ${t.javaClass.simpleName} - ${t.message}")
                
                // Provide detailed error information
                when {
                    t is java.net.ConnectException -> {
                        Log.e(TAG, "Connection refused - CFD device may not have WebSocket server running")
                        Log.e(TAG, "Make sure CFD device is running a WebSocket server on port $serverPort")
                    }
                    t is java.net.UnknownHostException -> {
                        Log.e(TAG, "Unknown host - Cannot resolve IP address $serverIp")
                        Log.e(TAG, "Check if both devices are on the same Wi-Fi network")
                    }
                    t is java.net.SocketTimeoutException -> {
                        Log.e(TAG, "Connection timeout - CFD device not responding")
                        Log.e(TAG, "Check if CFD device is powered on and connected to network")
                    }
                    else -> {
                        Log.e(TAG, "Network error: ${t.message}")
                    }
                }
                
                if (response != null) {
                    Log.e(TAG, "HTTP Response: code=${response.code}, message=${response.message}")
                    Log.e(TAG, "Response headers: ${response.headers}")
                } else {
                    Log.e(TAG, "No HTTP response available (likely network error or server not running)")
                }
                
                _connectionState.value = ConnectionState.Disconnected
                Log.d(TAG, "Connection state changed to: Disconnected")
                
                // Attempt to reconnect after 5 seconds (increased from 3)
                Log.d(TAG, "Scheduling reconnection attempt in 5 seconds...")
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (_connectionState.value == ConnectionState.Disconnected) {
                        Log.d(TAG, "Attempting to reconnect to $serverIp:$serverPort...")
                        connect()
                    } else {
                        Log.d(TAG, "Skipping reconnection - already connected or connecting")
                    }
                }, 5000)
            }
        })
        
        Log.d(TAG, "WebSocket connection request initiated")
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting from $serverIp:$serverPort")
        webSocket?.close(1000, "Client disconnecting")
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
        Log.d(TAG, "Disconnected from server")
    }

    fun isConnected(): Boolean {
        return _connectionState.value == ConnectionState.Connected
    }

    fun send(message: String) {
        if (_connectionState.value == ConnectionState.Connected) {
            webSocket?.send(message)
            Log.d(TAG, "Message sent to server (${message.length} chars)")
        } else {
            Log.w(TAG, "Cannot send message - not connected. State: ${_connectionState.value}")
        }
    }

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
    }

    companion object {
        private const val TAG = "CustomerDisplayClient"
    }
}

