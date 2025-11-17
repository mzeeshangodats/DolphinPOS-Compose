package com.retail.dolphinpos.data.customer_display

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class SimpleWebSocket(
    private val socket: Socket,
    private val clientId: String,
    private val onClose: (SimpleWebSocket) -> Unit
) {
    private val inputStream: InputStream = socket.getInputStream()
    private val outputStream: OutputStream = socket.getOutputStream()
    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.IO)
    private val sendLock = Any() // Synchronization lock for sending messages

    fun start() {
        if (isRunning) return
        isRunning = true
        
        scope.launch {
            try {
                while (isRunning && socket.isConnected && !socket.isClosed) {
                    val frame = readFrame()
                    if (frame == null) {
                        break
                    }
                    // Handle ping/pong if needed
                    if (frame.opcode == 0x9) { // Ping
                        sendPong()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading WebSocket frame", e)
            } finally {
                close()
            }
        }
    }

    fun send(message: String) {
        if (!isRunning || socket.isClosed || !socket.isConnected) return
        
        // Run network operations on background thread
        scope.launch {
            try {
                val bytes = message.toByteArray(Charsets.UTF_8)
                writeFrame(bytes, 0x1) // Text frame
            } catch (e: Exception) {
                Log.e(TAG, "Error sending WebSocket message", e)
                close()
            }
        }
    }

    fun close() {
        if (!isRunning) return
        isRunning = false
        
        try {
            socket.close()
        } catch (e: Exception) {
            // Ignore
        }
        
        onClose(this)
    }

    fun isConnected(): Boolean {
        return isRunning && socket.isConnected && !socket.isClosed
    }

    private fun readFrame(): WebSocketFrame? {
        try {
            val firstByte = inputStream.read()
            if (firstByte == -1) return null

            val fin = (firstByte and 0x80) != 0
            val opcode = firstByte and 0x0F

            val secondByte = inputStream.read()
            if (secondByte == -1) return null

            val masked = (secondByte and 0x80) != 0
            var payloadLength = (secondByte and 0x7F).toLong()

            if (payloadLength == 126L) {
                val lengthBytes = ByteArray(2)
                inputStream.read(lengthBytes)
                payloadLength = (((lengthBytes[0].toInt() and 0xFF) shl 8) or (lengthBytes[1].toInt() and 0xFF)).toLong()
            } else if (payloadLength == 127L) {
                val lengthBytes = ByteArray(8)
                inputStream.read(lengthBytes)
                payloadLength = 0L
                for (i in 0 until 8) {
                    payloadLength = (payloadLength shl 8) or (lengthBytes[i].toInt() and 0xFF).toLong()
                }
            }

            val maskingKey = if (masked) {
                val key = ByteArray(4)
                inputStream.read(key)
                key
            } else null

            val payload = ByteArray(payloadLength.toInt())
            inputStream.read(payload)

            if (masked && maskingKey != null) {
                for (i in payload.indices) {
                    payload[i] = (payload[i].toInt() xor maskingKey[i % 4].toInt()).toByte()
                }
            }

            return WebSocketFrame(fin, opcode, payload)
        } catch (e: Exception) {
            return null
        }
    }

    private fun writeFrame(data: ByteArray, opcode: Int) {
        synchronized(sendLock) {
            val frame = ByteArray(2 + data.size + (if (data.size > 125) 2 else 0))
            var offset = 0

            // First byte: FIN + opcode
            frame[offset++] = (0x80 or opcode).toByte()

            // Second byte: MASK + payload length
            if (data.size <= 125) {
                frame[offset++] = data.size.toByte()
            } else {
                frame[offset++] = 126
                frame[offset++] = ((data.size shr 8) and 0xFF).toByte()
                frame[offset++] = (data.size and 0xFF).toByte()
            }

            // Payload
            System.arraycopy(data, 0, frame, offset, data.size)

            outputStream.write(frame)
            outputStream.flush()
        }
    }

    private fun sendPong() {
        writeFrame(ByteArray(0), 0xA) // Pong frame
    }

    private data class WebSocketFrame(val fin: Boolean, val opcode: Int, val payload: ByteArray)

    companion object {
        private const val TAG = "SimpleWebSocket"
    }
}

