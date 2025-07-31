package com.sunpodder.relay.protocols

import org.json.JSONObject

/**
 * Handles SMS protocol messages according to protocols/sms.md specification
 * Uses a single "sms" type with direction field ("incoming" or "outgoing")
 */
class SmsProtocolHandler : BaseProtocolHandler {
    companion object {
        private const val TAG = "SmsProtocol"
    }

    /**
     * Creates an incoming SMS message (Android → Desktop)
     * Uses "direction": "incoming" and "from" field
     */
    fun createIncomingSms(
        from: String,
        body: String,
        timestamp: Long = System.currentTimeMillis() / 1000, // UNIX epoch seconds
        status: String? = null
    ): ByteArray {
        val smsJson = createBaseMessage("sms").apply {
            put("payload", JSONObject().apply {
                put("direction", "incoming")
                put("from", from)
                put("body", body)
                put("timestamp", timestamp)
                status?.let { put("status", it) }
            })
        }
        return formatMessage(smsJson.toString())
    }

    /**
     * Creates an outgoing SMS message (Desktop → Android)
     * Uses "direction": "outgoing" and "to" field
     */
    fun createOutgoingSms(
        to: String,
        body: String,
        timestamp: Long = System.currentTimeMillis() / 1000, // UNIX epoch seconds
        status: String? = null
    ): ByteArray {
        val smsJson = createBaseMessage("sms").apply {
            put("payload", JSONObject().apply {
                put("direction", "outgoing")
                put("to", to)
                put("body", body)
                put("timestamp", timestamp)
                status?.let { put("status", it) }
            })
        }
        return formatMessage(smsJson.toString())
    }

    /**
     * Parses an SMS message according to the protocol specification
     */
    fun parseSmsMessage(jsonMessage: String): SmsData? {
        return try {
            val json = JSONObject(jsonMessage)
            val payload = json.getJSONObject("payload")
            
            SmsData(
                id = json.optString("id"),
                direction = payload.getString("direction"),
                from = payload.optString("from").takeIf { it.isNotEmpty() },
                to = payload.optString("to").takeIf { it.isNotEmpty() },
                body = payload.getString("body"),
                timestamp = payload.getLong("timestamp"),
                status = payload.optString("status").takeIf { it.isNotEmpty() }
            )
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Data class for SMS messages according to the protocol specification
 */
data class SmsData(
    val id: String,
    val direction: String, // "incoming" or "outgoing"
    val from: String? = null, // Required for incoming
    val to: String? = null, // Required for outgoing
    val body: String,
    val timestamp: Long, // UNIX epoch seconds
    val status: String? = null // Optional: "sent", "delivered", "failed"
)
