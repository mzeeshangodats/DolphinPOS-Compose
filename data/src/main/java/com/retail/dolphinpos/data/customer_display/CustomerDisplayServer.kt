package com.retail.dolphinpos.data.customer_display

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.google.gson.Gson
import com.retail.dolphinpos.domain.model.home.cart.CartItem
import java.net.Inet4Address
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class CustomerDisplayServer(
    private val context: Context,
    private val gson: Gson,
    private val port: Int = 8080
) {
    private val server: HttpServer = HttpServer(port)
    private val clients = ConcurrentHashMap<SimpleWebSocket, String>()
    private val clientIdCounter = AtomicInteger(0)
    private var isRunning = false
    private var currentCartData: CartDisplayData? = null

    fun start() {
        if (isRunning) {
            Log.d(TAG, "Server already running, skipping start")
            return
        }
        
        val localIp = getLocalIpAddress()
        Log.d(TAG, "Starting Customer Display Server on port $port")
        Log.d(TAG, "Server IP Address: $localIp")
        Log.d(TAG, "Server WebSocket URL: ws://$localIp:$port/customer-display")
        
        try {
            server.start()
            isRunning = true
            Log.d(TAG, "Customer Display Server started successfully on port $port")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start server on port $port", e)
            throw e
        }
    }

    fun stop() {
        if (!isRunning) {
            Log.d(TAG, "Server not running, skipping stop")
            return
        }
        
        Log.d(TAG, "Stopping Customer Display Server. Active clients: ${clients.size}")
        clients.keys.forEach { 
            try {
                it.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing client", e)
            }
        }
        clients.clear()
        server.stop()
        isRunning = false
        Log.d(TAG, "Customer Display Server stopped")
    }

    fun broadcastCartUpdate(status: String, cartItems: List<CartItem>, subtotal: Double, tax: Double, total: Double, cashDiscountTotal: Double, orderDiscountTotal: Double, isCashSelected: Boolean) {
        if (!isRunning) {
            Log.w(TAG, "Cannot broadcast: Server is not running")
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
        
        currentCartData = cartData
        val json = gson.toJson(cartData)
        Log.d(TAG, "Broadcasting cart update: status=$status, items=${cartItems.size}, total=$total, clients=${clients.size}")

        // Run on background thread to avoid NetworkOnMainThreadException
        Thread {
            val connectedClients = clients.keys.toList()
            Log.d(TAG, "Sending to ${connectedClients.size} client(s)")
            
            connectedClients.forEach { client ->
                try {
                    if (client.isConnected()) {
                        Log.d(TAG, "Sending cart data to client: ${clients[client]}")
                        client.send(json)
                        Log.d(TAG, "Successfully sent cart data to client: ${clients[client]}")
                    } else {
                        Log.w(TAG, "Client ${clients[client]} is not connected, removing")
                        clients.remove(client)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending message to client ${clients[client]}", e)
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
        Log.d(TAG, "Getting local IP address...")
        
        getWifiIpAddress()?.let { 
            Log.d(TAG, "Found Wi-Fi IP address: $it")
            return it 
        }

        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            var fallbackAddress: String? = null
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (!networkInterface.isUp || networkInterface.isLoopback) {
                    Log.d(TAG, "Skipping interface: ${networkInterface.displayName} (up=${networkInterface.isUp}, loopback=${networkInterface.isLoopback})")
                    continue
                }

                val isPreferredInterface = networkInterface.displayName?.let { name ->
                    val lower = name.lowercase()
                    lower.contains("wlan") || lower.contains("wifi") || lower.contains("eth")
                } ?: false

                Log.d(TAG, "Checking interface: ${networkInterface.displayName} (preferred=$isPreferredInterface)")

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        val hostAddress = address.hostAddress ?: continue
                        Log.d(TAG, "Found IP address: $hostAddress on ${networkInterface.displayName}")
                        if (isPreferredInterface) {
                            Log.d(TAG, "Using preferred interface IP: $hostAddress")
                            return hostAddress
                        } else if (fallbackAddress == null) {
                            fallbackAddress = hostAddress
                            Log.d(TAG, "Storing fallback IP: $hostAddress")
                        }
                    }
                }
            }
            fallbackAddress?.let { 
                Log.d(TAG, "Using fallback IP address: $it")
                return it 
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local IP address", e)
        }
        // Fallback to localhost for single device setup
        Log.w(TAG, "No IP address found, using localhost fallback: 127.0.0.1")
        return "127.0.0.1"
    }

    private fun getWifiIpAddress(): String? {
        return try {
            Log.d(TAG, "Attempting to get Wi-Fi IP address from WifiManager")
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager == null) {
                Log.w(TAG, "WifiManager is null")
                return null
            }
            
            val connectionInfo = wifiManager.connectionInfo
            val ipAddress = connectionInfo?.ipAddress ?: run {
                Log.w(TAG, "Connection info or IP address is null")
                return null
            }
            
            if (ipAddress == 0) {
                Log.w(TAG, "IP address is 0 (not connected)")
                return null
            }

            val byteArray = ByteBuffer.allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(ipAddress)
                .array()

            val hostAddress = InetAddress.getByAddress(byteArray)?.hostAddress
            if (hostAddress != null) {
                Log.d(TAG, "Successfully retrieved Wi-Fi IP address: $hostAddress")
            } else {
                Log.w(TAG, "Failed to convert IP address bytes to InetAddress")
            }
            hostAddress
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Wi-Fi IP address", e)
            null
        }
    }
    
    /**
     * Get localhost address for single device setup (both apps on same tablet)
     */
    fun getLocalhostAddress(): String {
        return "127.0.0.1"
    }

    private inner class HttpServer(private val port: Int) {
        private val serverSocket: java.net.ServerSocket
        private var isRunning = false
        
        init {
            try {
                // Bind to all interfaces (0.0.0.0) to accept connections from any IP
                serverSocket = java.net.ServerSocket(port, 50, java.net.InetAddress.getByName("0.0.0.0"))
                Log.d(TAG, "HttpServer: ServerSocket created and bound to 0.0.0.0:$port")
            } catch (e: Exception) {
                Log.e(TAG, "HttpServer: Failed to create ServerSocket on port $port", e)
                throw e
            }
        }
        private val serverThread = Thread {
            Log.d(TAG, "HttpServer: Server thread started, listening on port $port")
            while (isRunning) {
                try {
                    Log.d(TAG, "HttpServer: Waiting for client connection on port $port...")
                    val clientSocket = serverSocket.accept()
                    val clientAddress = "${clientSocket.inetAddress.hostAddress}:${clientSocket.port}"
                    Log.d(TAG, "HttpServer: Client connected from $clientAddress")
                    handleClient(clientSocket)
                } catch (e: Exception) {
                    if (isRunning) {
                        Log.e(TAG, "HttpServer: Error accepting client connection", e)
                    } else {
                        Log.d(TAG, "HttpServer: Server stopped, exiting accept loop")
                    }
                }
            }
            Log.d(TAG, "HttpServer: Server thread exiting")
        }

        fun start() {
            Log.d(TAG, "HttpServer: Starting server thread on port $port")
            try {
                // Verify server socket is bound and listening
                val isBound = !serverSocket.isClosed && serverSocket.isBound
                val localAddress = serverSocket.localSocketAddress
                Log.d(TAG, "HttpServer: ServerSocket bound: $isBound, local address: $localAddress")
                
                if (!isBound) {
                    Log.e(TAG, "HttpServer: ServerSocket is not bound!")
                }
                
                isRunning = true
                serverThread.start()
                Log.d(TAG, "HttpServer: Server thread started, waiting for connections...")
                Log.d(TAG, "HttpServer: Server is now listening on $localAddress")
            } catch (e: Exception) {
                Log.e(TAG, "HttpServer: Error starting server", e)
                throw e
            }
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
            val clientAddress = "${clientSocket.inetAddress.hostAddress}:${clientSocket.port}"
            try {
                Log.d(TAG, "HttpServer: Reading HTTP request from $clientAddress")
                val request = readHttpRequest(clientSocket)
                Log.d(TAG, "HttpServer: Request path: ${request.path}")
                Log.d(TAG, "HttpServer: Request headers: ${request.headers.keys}")
                
                if (request.path == "/customer-display" && request.headers.containsKey("Upgrade") && 
                    request.headers["Upgrade"]?.equals("websocket", ignoreCase = true) == true) {
                    Log.d(TAG, "HttpServer: WebSocket upgrade requested from $clientAddress")
                    performWebSocketHandshake(clientSocket, request)
                } else {
                    Log.w(TAG, "HttpServer: Invalid request from $clientAddress - path=${request.path}, upgrade=${request.headers["Upgrade"]}")
                    sendHttpResponse(clientSocket, "HTTP/1.1 404 Not Found\r\n\r\n")
                    clientSocket.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "HttpServer: Error handling client $clientAddress", e)
                try {
                    clientSocket.close()
                } catch (ex: Exception) {
                    Log.e(TAG, "HttpServer: Error closing client socket", ex)
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
            val clientAddress = "${socket.inetAddress.hostAddress}:${socket.port}"
            val webSocketKey = request.headers["Sec-WebSocket-Key"]
            
            if (webSocketKey == null) {
                Log.e(TAG, "HttpServer: Missing Sec-WebSocket-Key header from $clientAddress")
                socket.close()
                return
            }
            
            Log.d(TAG, "HttpServer: Performing WebSocket handshake with $clientAddress")
            Log.d(TAG, "HttpServer: WebSocket Key: $webSocketKey")
            
            val acceptKey = generateWebSocketAccept(webSocketKey)
            Log.d(TAG, "HttpServer: Generated accept key: $acceptKey")
            
            val response = "HTTP/1.1 101 Switching Protocols\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Accept: $acceptKey\r\n\r\n"
            
            try {
                socket.getOutputStream().write(response.toByteArray())
                socket.getOutputStream().flush()
                Log.d(TAG, "HttpServer: WebSocket handshake response sent to $clientAddress")
            } catch (e: Exception) {
                Log.e(TAG, "HttpServer: Error sending handshake response to $clientAddress", e)
                socket.close()
                return
            }

            // Create WebSocket connection
            val clientId = "client_${clientIdCounter.incrementAndGet()}"
            Log.d(TAG, "HttpServer: Creating WebSocket connection for $clientId from $clientAddress")
            
            val webSocket = SimpleWebSocket(socket, clientId) { ws ->
                clients.remove(ws)
                Log.d(TAG, "HttpServer: Client disconnected: $clientId")
            }
            
            clients[webSocket] = clientId
            Log.d(TAG, "HttpServer: Client connected: $clientId (Total clients: ${clients.size})")
            
            // Send current cart data immediately if available, otherwise send welcome screen
            if (currentCartData != null) {
                Log.d(TAG, "HttpServer: Sending current cart data to $clientId")
                val json = gson.toJson(currentCartData)
                webSocket.send(json)
            } else {
                Log.d(TAG, "HttpServer: Sending welcome screen to $clientId")
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
            Log.d(TAG, "HttpServer: WebSocket started for $clientId")
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


