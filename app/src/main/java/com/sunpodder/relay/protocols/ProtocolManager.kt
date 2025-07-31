package com.sunpodder.relay.protocols

import com.sunpodder.relay.ClientInfo
import com.sunpodder.relay.UILogger
import org.json.JSONObject
import java.io.*
import java.nio.charset.StandardCharsets

/**
 * Main protocol manager that coordinates all protocol handlers
 */
class ProtocolManager : BaseProtocolHandler {
    companion object {
        private const val TAG = "ProtocolManager"
    }

    // Protocol handlers
    val connection = ConnectionProtocolHandler()
    val heartbeat = HeartbeatProtocolHandler()
    val notification = NotificationProtocolHandler()
    val system = SystemProtocolHandler()
    val sms = SmsProtocolHandler()

    /**
     * Sends a message through an OutputStream
     */
    fun sendMessage(outputStream: OutputStream, message: ByteArray) {
        try {
            outputStream.write(message)
            outputStream.flush()
        } catch (e: IOException) {
            UILogger.e(TAG, "Failed to send message", e)
            throw e
        }
    }

    /**
     * Reads messages from an InputStream using length-prefixed protocol
     */
    fun readMessages(
        inputStream: InputStream,
        onMessageReceived: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            while (true) {
                // Read 4-byte length prefix (big-endian)
                val lengthBytes = ByteArray(4)
                var bytesRead = 0
                
                while (bytesRead < 4) {
                    val read = inputStream.read(lengthBytes, bytesRead, 4 - bytesRead)
                    if (read == -1) {
                        return // End of stream
                    }
                    bytesRead += read
                }
                
                // Convert to message length (big-endian)
                val messageLength = ((lengthBytes[0].toInt() and 0xFF) shl 24) or
                                  ((lengthBytes[1].toInt() and 0xFF) shl 16) or
                                  ((lengthBytes[2].toInt() and 0xFF) shl 8) or
                                  (lengthBytes[3].toInt() and 0xFF)
                
                // Validate message length
                if (messageLength <= 0 || messageLength > 1024 * 1024) { // Max 1MB
                    onError(IllegalArgumentException("Invalid message length: $messageLength"))
                    return
                }
                
                // Read the message of specified length
                val messageBytes = ByteArray(messageLength)
                bytesRead = 0
                
                while (bytesRead < messageLength) {
                    val read = inputStream.read(messageBytes, bytesRead, messageLength - bytesRead)
                    if (read == -1) {
                        onError(IllegalStateException("Unexpected end of stream while reading message"))
                        return
                    }
                    bytesRead += read
                }
                
                // Convert to string and validate JSON
                val messageString = String(messageBytes, StandardCharsets.UTF_8)
                
                try {
                    // Validate JSON
                    JSONObject(messageString)
                    onMessageReceived(messageString)
                } catch (e: Exception) {
                    UILogger.e(TAG, "Invalid JSON message: $messageString", e)
                    onError(IllegalArgumentException("Invalid JSON message", e))
                }
            }
        } catch (e: IOException) {
            UILogger.e(TAG, "Error reading messages", e)
            onError(e)
        }
    }

    /**
     * Parses a JSON message and returns the type
     */
    fun parseMessageType(jsonMessage: String): String? {
        return try {
            val json = JSONObject(jsonMessage)
            val type = json.optString("type")
            if (type.isNullOrEmpty()) null else type
        } catch (e: Exception) {
            UILogger.e(TAG, "Failed to parse message type", e)
            null
        }
    }

    /**
     * Parses a JSON message and returns it as JSONObject
     */
    fun parseMessage(jsonMessage: String): JSONObject? {
        return try {
            JSONObject(jsonMessage)
        } catch (e: Exception) {
            UILogger.e(TAG, "Failed to parse JSON message", e)
            null
        }
    }

    // Convenience methods that delegate to specific protocol handlers
    
    // Connection Protocol
    fun createAckMessage(refId: String?, status: String = "ok", reason: String? = null): ByteArray =
        connection.createAckMessage(refId, status, reason)
    
    fun parseConnMessage(jsonMessage: String, clientAddress: String): ClientInfo? =
        connection.parseConnMessage(jsonMessage, clientAddress)
    
    // Heartbeat Protocol
    fun createPingMessage(): ByteArray = heartbeat.createPingMessage()
    fun createPongMessage(pingId: String? = null): ByteArray = heartbeat.createPongMessage(pingId)
    fun parsePingMessage(jsonMessage: String): String? = heartbeat.parsePingMessage(jsonMessage)
    fun parsePongMessage(jsonMessage: String): String? = heartbeat.parsePongMessage(jsonMessage)
    
    // Notification Protocol
    fun createNotificationMessage(
        id: String,
        title: String?,
        body: String?,
        app: String?,
        packageName: String,
        timestamp: Long = System.currentTimeMillis() / 1000,
        canReply: Boolean = false,
        actions: List<NotificationAction> = emptyList()
    ): ByteArray = notification.createNotificationMessage(id, title, body, app, packageName, timestamp, canReply, actions)
    
    fun parseNotificationMessage(jsonMessage: String): NotificationData? =
        notification.parseNotificationMessage(jsonMessage)
    
    fun parseNotificationReplyMessage(jsonMessage: String): NotificationReply? =
        notification.parseNotificationReplyMessage(jsonMessage)
    
    fun parseNotificationActionMessage(jsonMessage: String): NotificationActionRequest? =
        notification.parseNotificationActionMessage(jsonMessage)
    
    fun parseNotificationDismissMessage(jsonMessage: String): NotificationDismiss? =
        notification.parseNotificationDismissMessage(jsonMessage)
    
    // System Protocol
    fun createLogMessage(message: String, level: String = "info"): ByteArray =
        system.createLogMessage(message, level)
    
    fun createErrorMessage(error: String, code: Int = 0): ByteArray =
        system.createErrorMessage(error, code)
    
    fun createStatusMessage(status: String, data: JSONObject? = null): ByteArray =
        system.createStatusMessage(status, data)
}
