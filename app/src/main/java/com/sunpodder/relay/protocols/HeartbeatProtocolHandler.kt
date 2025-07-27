package com.sunpodder.relay.protocols

import android.os.Build
import com.sunpodder.relay.UILogger
import org.json.JSONObject

/**
 * Handles heartbeat protocol (ping/pong messages)
 */
class HeartbeatProtocolHandler : BaseProtocolHandler {
    companion object {
        private const val TAG = "HeartbeatProtocol"
    }

    /**
     * Creates a ping message for keep-alive
     */
    fun createPingMessage(): String {
        val pingJson = createBaseMessage("ping").apply {
            put("payload", JSONObject().apply {
                put("device", Build.MODEL)
            })
        }
        return formatMessage(pingJson.toString())
    }

    /**
     * Creates a pong message in response to a ping
     */
    fun createPongMessage(pingId: String? = null): String {
        val pongJson = createBaseMessage("pong", pingId).apply {
            put("payload", JSONObject().apply {
                put("device", Build.MODEL)
            })
        }
        return formatMessage(pongJson.toString())
    }

    /**
     * Parses a ping message and extracts the ping ID
     */
    fun parsePingMessage(jsonMessage: String): String? {
        return try {
            val json = JSONObject(jsonMessage)
            if (json.getString("type") != "ping") {
                UILogger.w(TAG, "Expected 'ping' message type, got: ${json.optString("type")}")
                return null
            }
            json.optString("id").takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            UILogger.e(TAG, "Failed to parse ping message", e)
            null
        }
    }

    /**
     * Parses a pong message and extracts the original ping ID
     */
    fun parsePongMessage(jsonMessage: String): String? {
        return try {
            val json = JSONObject(jsonMessage)
            if (json.getString("type") != "pong") {
                UILogger.w(TAG, "Expected 'pong' message type, got: ${json.optString("type")}")
                return null
            }
            json.optString("id").takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            UILogger.e(TAG, "Failed to parse pong message", e)
            null
        }
    }
}
