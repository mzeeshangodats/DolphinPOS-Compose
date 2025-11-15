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
        if (_connectionState.value == ConnectionState.Connected) {
            return
        }

        val request = Request.Builder()
            .url("ws://$serverIp:$serverPort/customer-display")
            .build()

        _connectionState.value = ConnectionState.Connecting

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected to $serverIp:$serverPort")
                _connectionState.value = ConnectionState.Connected
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val data = gson.fromJson(text, CartDisplayData::class.java)
                    _cartData.value = data
                    Log.d(TAG, "Received cart update: ${data.cartItems.size} items")
                } catch (e: JsonSyntaxException) {
                    Log.e(TAG, "Error parsing cart data", e)
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                onMessage(webSocket, bytes.utf8())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $reason")
                _connectionState.value = ConnectionState.Disconnected
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $reason")
                _connectionState.value = ConnectionState.Disconnected
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket connection failed", t)
                _connectionState.value = ConnectionState.Disconnected
                
                // Attempt to reconnect after 3 seconds
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (_connectionState.value == ConnectionState.Disconnected) {
                        connect()
                    }
                }, 3000)
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnecting")
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
    }

    fun isConnected(): Boolean {
        return _connectionState.value == ConnectionState.Connected
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

