package com.sunpodder.relay.protocols

import org.json.JSONObject
import java.util.*

/**
 * Base interface for all protocol handlers
 */
interface BaseProtocolHandler {
    companion object {
        const val MESSAGE_TERMINATOR = "\u0000\u0000" // \0\0
    }
    
    /**
     * Formats a JSON message with the protocol terminator
     */
    fun formatMessage(jsonString: String): String {
        return jsonString + MESSAGE_TERMINATOR
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
