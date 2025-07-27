package com.sunpodder.relay.protocols

import org.json.JSONObject

/**
 * Handles notification protocol messages
 */
class NotificationProtocolHandler : BaseProtocolHandler {

    /**
     * Creates a notification message
     */
    fun createNotificationMessage(
        packageName: String,
        title: String?,
        text: String?,
        timestamp: Long = System.currentTimeMillis()
    ): String {
        val notificationJson = createBaseMessage("notification").apply {
            put("payload", JSONObject().apply {
                put("package_name", packageName)
                put("title", title ?: "")
                put("text", text ?: "")
                put("timestamp", timestamp)
            })
        }
        return formatMessage(notificationJson.toString())
    }
}
