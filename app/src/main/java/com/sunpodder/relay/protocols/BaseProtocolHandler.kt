package com.sunpodder.relay.protocols

import org.json.JSONObject
import java.nio.ByteBuffer
import java.util.*

/**
 * Base interface for all protocol handlers
 */
interface BaseProtocolHandler {
    
    /**
     * Formats a JSON message with length prefix (4-byte big-endian)
     */
    fun formatMessage(jsonString: String): ByteArray {
        val messageBytes = jsonString.toByteArray(Charsets.UTF_8)
        val lengthPrefix = ByteBuffer.allocate(4).putInt(messageBytes.size).array()
        return lengthPrefix + messageBytes
    }
    
    /**
     * Creates a base message structure with common fields
     */
    fun createBaseMessage(type: String, id: String? = null): JSONObject {
        return JSONObject().apply {
            put("type", type)
            put("id", id ?: UUID.randomUUID().toString())
            put("timestamp", System.currentTimeMillis() / 1000) // Unix timestamp
        }
    }
}
