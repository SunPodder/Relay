package com.sunpodder.relay.protocols

import org.json.JSONObject

/**
 * Handles SMS protocol messages (future implementation)
 * 
 * This will handle:
 * - Incoming SMS messages from Android
 * - Outgoing SMS requests from clients
 * - SMS status updates (sent, delivered, failed)
 */
class SmsProtocolHandler : BaseProtocolHandler {
    companion object {
        private const val TAG = "SmsProtocol"
    }

    /**
     * Creates an SMS message for incoming SMS (from Android to clients)
     */
    fun createIncomingSmsMessage(
        from: String,
        body: String,
        timestamp: Long = System.currentTimeMillis(),
        status: String = "received"
    ): String {
        val smsJson = createBaseMessage("sms").apply {
            put("payload", JSONObject().apply {
                put("direction", "incoming")
                put("from", from)
                put("body", body)
                put("timestamp", timestamp)
                put("status", status)
            })
        }
        return formatMessage(smsJson.toString())
    }

    /**
     * Creates an SMS message for outgoing SMS requests (from clients to Android)
     */
    fun createOutgoingSmsMessage(
        to: String,
        body: String,
        timestamp: Long = System.currentTimeMillis()
    ): String {
        val smsJson = createBaseMessage("sms").apply {
            put("payload", JSONObject().apply {
                put("direction", "outgoing")
                put("to", to)
                put("body", body)
                put("timestamp", timestamp)
                put("status", "pending")
            })
        }
        return formatMessage(smsJson.toString())
    }

    /**
     * Creates an SMS status update message
     */
    fun createSmsStatusMessage(
        messageId: String,
        status: String, // "sent", "delivered", "failed"
        timestamp: Long = System.currentTimeMillis()
    ): String {
        val statusJson = createBaseMessage("sms_status").apply {
            put("payload", JSONObject().apply {
                put("message_id", messageId)
                put("status", status)
                put("timestamp", timestamp)
            })
        }
        return formatMessage(statusJson.toString())
    }

    // TODO: Add parsing methods for SMS messages when implementing SMS functionality
    // - parseSmsMessage()
    // - parseSmsStatusMessage()
}
