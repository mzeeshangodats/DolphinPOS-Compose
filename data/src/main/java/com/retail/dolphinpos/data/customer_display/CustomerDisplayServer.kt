package com.retail.dolphinpos.data.customer_display

import android.util.Log
import com.google.gson.Gson
import com.retail.dolphinpos.domain.model.home.cart.CartItem
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class CustomerDisplayServer(
    private val gson: Gson,
    private val port: Int = 8080
) {
    private val server: HttpServer = HttpServer(port)
    private val clients = ConcurrentHashMap<SimpleWebSocket, String>()
    private val clientIdCounter = AtomicInteger(0)
    private var isRunning = false
    private var currentCartData: CartDisplayData? = null

    fun start() {
        if (isRunning) return
        
        server.start()
        isRunning = true
        Log.d(TAG, "Customer Display Server started on port $port")
    }

    fun stop() {
        if (!isRunning) return
        
        clients.keys.forEach { it.close() }
        clients.clear()
        server.stop()
        isRunning = false
        Log.d(TAG, "Customer Display Server stopped")
    }

    fun broadcastCartUpdate(status: String, cartItems: List<CartItem>, subtotal: Double, tax: Double, total: Double, cashDiscountTotal: Double, orderDiscountTotal: Double, isCashSelected: Boolean) {
        if (!isRunning) return

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
        
        currentCartData = cartData
        val json = gson.toJson(cartData)

        // Run on background thread to avoid NetworkOnMainThreadException
        Thread {
            clients.keys.toList().forEach { client ->
                try {
                    if (client.isConnected()) {
                        client.send(json)
                    } else {
                        clients.remove(client)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending message to client", e)
                    clients.remove(client)
                }
            }
        }.start()
    }

    fun getCurrentCartData(): CartDisplayData? = currentCartData

    fun getServerAddress(): String {
        return "ws://${getLocalIpAddress()}:$port/customer-display"
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local IP address", e)
        }
        // Fallback to localhost for single device setup
        return "127.0.0.1"
    }
    
    /**
     * Get localhost address for single device setup (both apps on same tablet)
     */
    fun getLocalhostAddress(): String {
        return "127.0.0.1"
    }

    private inner class HttpServer(private val port: Int) {
        private val serverSocket = java.net.ServerSocket(port)
        private var isRunning = false
        private val serverThread = Thread {
            while (isRunning) {
                try {
                    val clientSocket = serverSocket.accept()
                    handleClient(clientSocket)
                } catch (e: Exception) {
                    if (isRunning) {
                        Log.e(TAG, "Error accepting client", e)
                    }
                }
            }
        }

        fun start() {
            isRunning = true
            serverThread.start()
        }

        fun stop() {
            isRunning = false
            try {
                serverSocket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing server socket", e)
            }
        }

        private fun handleClient(clientSocket: java.net.Socket) {
            try {
                val request = readHttpRequest(clientSocket)
                if (request.path == "/customer-display" && request.headers.containsKey("Upgrade") && 
                    request.headers["Upgrade"]?.equals("websocket", ignoreCase = true) == true) {
                    performWebSocketHandshake(clientSocket, request)
                } else {
                    sendHttpResponse(clientSocket, "HTTP/1.1 404 Not Found\r\n\r\n")
                    clientSocket.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling client", e)
                try {
                    clientSocket.close()
                } catch (ex: Exception) {
                    // Ignore
                }
            }
        }

        private fun readHttpRequest(socket: java.net.Socket): HttpRequest {
            val reader = socket.getInputStream().bufferedReader()
            val requestLine = reader.readLine() ?: ""
            val headers = mutableMapOf<String, String>()
            
            var line: String?
            while (reader.readLine().also { line = it } != null && line!!.isNotEmpty()) {
                val parts = line!!.split(":", limit = 2)
                if (parts.size == 2) {
                    headers[parts[0].trim()] = parts[1].trim()
                }
            }

            val path = requestLine.split(" ").getOrNull(1) ?: "/"
            return HttpRequest(path, headers)
        }

        private fun performWebSocketHandshake(socket: java.net.Socket, request: HttpRequest) {
            val webSocketKey = request.headers["Sec-WebSocket-Key"] ?: return
            val acceptKey = generateWebSocketAccept(webSocketKey)
            
            val response = "HTTP/1.1 101 Switching Protocols\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Accept: $acceptKey\r\n\r\n"
            
            socket.getOutputStream().write(response.toByteArray())
            socket.getOutputStream().flush()

            // Create WebSocket connection
            val clientId = "client_${clientIdCounter.incrementAndGet()}"
            val webSocket = SimpleWebSocket(socket, clientId) { ws ->
                clients.remove(ws)
                Log.d(TAG, "Client disconnected: $clientId")
            }
            
            clients[webSocket] = clientId
            Log.d(TAG, "Client connected: $clientId")
            
            // Send current cart data immediately if available, otherwise send welcome screen
            if (currentCartData != null) {
                val json = gson.toJson(currentCartData)
                webSocket.send(json)
            } else {
                // Send welcome screen for new connections
                val welcomeData = CartDisplayData(
                    status = "WELCOME",
                    cartItems = emptyList(),
                    subtotal = 0.0,
                    tax = 0.0,
                    total = 0.0,
                    cashDiscountTotal = 0.0,
                    orderDiscountTotal = 0.0,
                    isCashSelected = false,
                    timestamp = System.currentTimeMillis()
                )
                val json = gson.toJson(welcomeData)
                webSocket.send(json)
            }
            
            webSocket.start()
        }

        private fun generateWebSocketAccept(key: String): String {
            val magic = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
            val combined = key + magic
            val sha1 = java.security.MessageDigest.getInstance("SHA-1")
            val hash = sha1.digest(combined.toByteArray())
            return android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP)
        }

        private fun sendHttpResponse(socket: java.net.Socket, response: String) {
            socket.getOutputStream().write(response.toByteArray())
            socket.getOutputStream().flush()
        }
    }

    private data class HttpRequest(val path: String, val headers: Map<String, String>)

    companion object {
        private const val TAG = "CustomerDisplayServer"
    }
}

data class CartDisplayData(
    val status: String,  // "WELCOME", "CHECKOUT_SCREEN", etc.
    val cartItems: List<CartItem>,
    val subtotal: Double,
    val tax: Double,
    val total: Double,
    val cashDiscountTotal: Double = 0.0,
    val orderDiscountTotal: Double = 0.0,
    val isCashSelected: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

