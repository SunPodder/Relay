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
    fun sendMessage(outputStream: OutputStream, message: String) {
        try {
            val bytes = message.toByteArray(StandardCharsets.UTF_8)
            outputStream.write(bytes)
            outputStream.flush()
        } catch (e: IOException) {
            UILogger.e(TAG, "Failed to send message", e)
            throw e
        }
    }

    /**
     * Reads messages from an InputStream using the protocol
     */
    fun readMessages(
        inputStream: InputStream,
        onMessageReceived: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val messageBuffer = StringBuilder()
            
            while (true) {
                val byte = inputStream.read()
                if (byte == -1) {
                    break
                }
                
                val char = byte.toChar()
                messageBuffer.append(char)
                
                // Check if we have the terminator at the end
                if (messageBuffer.length >= BaseProtocolHandler.MESSAGE_TERMINATOR.length) {
                    val bufferEnd = messageBuffer.substring(
                        messageBuffer.length - BaseProtocolHandler.MESSAGE_TERMINATOR.length
                    )
                    
                    if (bufferEnd == BaseProtocolHandler.MESSAGE_TERMINATOR) {
                        // Found complete message, remove terminator
                        val completeMessage = messageBuffer.substring(
                            0, 
                            messageBuffer.length - BaseProtocolHandler.MESSAGE_TERMINATOR.length
                        )
                        
                        if (completeMessage.isNotEmpty()) {
                            try {
                                // Validate JSON
                                JSONObject(completeMessage)
                                onMessageReceived(completeMessage)
                            } catch (e: Exception) {
                                UILogger.e(TAG, "Invalid JSON message: $completeMessage", e)
                                onError(IllegalArgumentException("Invalid JSON message", e))
                            }
                        }
                        
                        // Clear buffer for next message
                        messageBuffer.clear()
                    }
                }
                
                // Safety check to prevent extremely large messages from consuming too much memory
                if (messageBuffer.length > 1024 * 1024) { // 1MB limit
                    UILogger.w(TAG, "Message too large, clearing buffer")
                    messageBuffer.clear()
                    onError(IllegalStateException("Message size limit exceeded"))
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

    /**
     * Validates if a string is a properly formatted protocol message
     */
    fun isValidMessage(message: String): Boolean {
        return try {
            val jsonPart = if (message.endsWith(BaseProtocolHandler.MESSAGE_TERMINATOR)) {
                message.substring(0, message.length - BaseProtocolHandler.MESSAGE_TERMINATOR.length)
            } else {
                message
            }
            
            val json = JSONObject(jsonPart)
            json.has("type") && json.has("timestamp")
        } catch (e: Exception) {
            false
        }
    }

    // Convenience methods that delegate to specific protocol handlers
    
    // Connection Protocol
    fun createAckMessage(refId: String?, status: String = "ok", reason: String? = null): String =
        connection.createAckMessage(refId, status, reason)
    
    fun parseConnMessage(jsonMessage: String, clientAddress: String): ClientInfo? =
        connection.parseConnMessage(jsonMessage, clientAddress)
    
    // Heartbeat Protocol
    fun createPingMessage(): String = heartbeat.createPingMessage()
    fun createPongMessage(pingId: String? = null): String = heartbeat.createPongMessage(pingId)
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
    ): String = notification.createNotificationMessage(id, title, body, app, packageName, timestamp, canReply, actions)
    
    fun parseNotificationMessage(jsonMessage: String): NotificationData? =
        notification.parseNotificationMessage(jsonMessage)
    
    fun parseNotificationReplyMessage(jsonMessage: String): NotificationReply? =
        notification.parseNotificationReplyMessage(jsonMessage)
    
    fun parseNotificationActionMessage(jsonMessage: String): NotificationActionRequest? =
        notification.parseNotificationActionMessage(jsonMessage)
    
    fun parseNotificationDismissMessage(jsonMessage: String): NotificationDismiss? =
        notification.parseNotificationDismissMessage(jsonMessage)
    
    // System Protocol
    fun createLogMessage(message: String, level: String = "info"): String =
        system.createLogMessage(message, level)
    
    fun createErrorMessage(error: String, code: Int = 0): String =
        system.createErrorMessage(error, code)
    
    fun createStatusMessage(status: String, data: JSONObject? = null): String =
        system.createStatusMessage(status, data)
}
