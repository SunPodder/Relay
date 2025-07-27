package com.sunpodder.relay.protocols

import org.json.JSONObject

/**
 * Handles system protocol messages (log, error, status)
 */
class SystemProtocolHandler : BaseProtocolHandler {

    /**
     * Creates a log message
     */
    fun createLogMessage(message: String, level: String = "info"): String {
        val logJson = createBaseMessage("log").apply {
            put("payload", JSONObject().apply {
                put("message", message)
                put("level", level)
            })
        }
        return formatMessage(logJson.toString())
    }

    /**
     * Creates an error message
     */
    fun createErrorMessage(error: String, code: Int = 0): String {
        val errorJson = createBaseMessage("error").apply {
            put("payload", JSONObject().apply {
                put("message", error)
                put("code", code)
            })
        }
        return formatMessage(errorJson.toString())
    }

    /**
     * Creates a status message
     */
    fun createStatusMessage(status: String, data: JSONObject? = null): String {
        val statusJson = createBaseMessage("status").apply {
            put("payload", JSONObject().apply {
                put("status", status)
                data?.let { put("data", it) }
            })
        }
        return formatMessage(statusJson.toString())
    }
}
