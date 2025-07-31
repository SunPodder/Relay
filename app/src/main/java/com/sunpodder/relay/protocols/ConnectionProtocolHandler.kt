package com.sunpodder.relay.protocols

import com.sunpodder.relay.ClientInfo
import com.sunpodder.relay.UILogger
import org.json.JSONObject
import java.util.*

/**
 * Handles connection protocol (conn/ack messages)
 */
class ConnectionProtocolHandler : BaseProtocolHandler {
    companion object {
        private const val TAG = "ConnectionProtocol"
    }

    /**
     * Creates an acknowledgment message in response to a conn message
     */
    fun createAckMessage(refId: String?, status: String = "ok", reason: String? = null): ByteArray {
        val ackJson = createBaseMessage("ack").apply {
            put("payload", JSONObject().apply {
                refId?.let { put("ref_id", it) }
                put("status", status)
                reason?.let { put("reason", it) }
            })
        }
        return formatMessage(ackJson.toString())
    }

    /**
     * Parses a conn message and extracts client information
     */
    fun parseConnMessage(jsonMessage: String, clientAddress: String): ClientInfo? {
        return try {
            val json = JSONObject(jsonMessage)
            if (json.getString("type") != "conn") {
                UILogger.w(TAG, "Expected 'conn' message type, got: ${json.optString("type")}")
                return null
            }

            val payload = json.getJSONObject("payload")
            val deviceName = payload.getString("device_name")
            val platform = payload.getString("platform")
            val version = payload.getString("version")
            
            val supportsArray = payload.optJSONArray("supports")
            val supports = mutableListOf<String>()
            if (supportsArray != null) {
                for (i in 0 until supportsArray.length()) {
                    supports.add(supportsArray.getString(i))
                }
            }
            
            val authToken = payload.optString("auth_token").takeIf { it.isNotEmpty() }

            ClientInfo(
                deviceName = deviceName,
                platform = platform,
                version = version,
                supports = supports,
                authToken = authToken,
                address = clientAddress
            )
        } catch (e: Exception) {
            UILogger.e(TAG, "Failed to parse conn message", e)
            null
        }
    }
}
