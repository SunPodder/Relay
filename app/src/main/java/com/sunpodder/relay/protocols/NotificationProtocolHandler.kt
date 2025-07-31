package com.sunpodder.relay.protocols

import org.json.JSONArray
import org.json.JSONObject

/**
 * Handles notification protocol messages according to RFC specification
 */
class NotificationProtocolHandler : BaseProtocolHandler {

    /**
     * Creates a notification message (Android → Desktop)
     */
    fun createNotificationMessage(
        id: String,
        title: String?,
        body: String?,
        app: String?,
        packageName: String,
        timestamp: Long = System.currentTimeMillis() / 1000, // Convert to seconds
        canReply: Boolean = false,
        actions: List<NotificationAction> = emptyList()
    ): ByteArray {
        val notificationJson = createBaseMessage("notification").apply {
            put("payload", JSONObject().apply {
                put("id", id)
                put("title", title ?: "")
                put("body", body ?: "")
                put("app", app ?: "")
                put("package", packageName)
                put("timestamp", timestamp)
                put("can_reply", canReply)
                
                // Add actions array
                val actionsArray = JSONArray()
                actions.forEach { action ->
                    actionsArray.put(JSONObject().apply {
                        put("title", action.title)
                        put("type", action.type)
                        put("key", action.key)
                    })
                }
                put("actions", actionsArray)
            })
        }
        return formatMessage(notificationJson.toString())
    }

    /**
     * Creates a notification reply message (Desktop → Android)
     */
    fun createNotificationReplyMessage(
        notificationId: String,
        key: String,
        text: String
    ): ByteArray {
        val replyJson = createBaseMessage("notification_reply").apply {
            put("payload", JSONObject().apply {
                put("id", notificationId)
                put("key", key)
                put("text", text)
            })
        }
        return formatMessage(replyJson.toString())
    }

    /**
     * Creates a notification action message (Desktop → Android)
     */
    fun createNotificationActionMessage(
        notificationId: String,
        key: String
    ): ByteArray {
        val actionJson = createBaseMessage("notification_action").apply {
            put("payload", JSONObject().apply {
                put("id", notificationId)
                put("key", key)
            })
        }
        return formatMessage(actionJson.toString())
    }

    /**
     * Creates a notification dismiss message (Desktop → Android)
     */
    fun createNotificationDismissMessage(
        notificationId: String
    ): ByteArray {
        val dismissJson = createBaseMessage("notification_dismiss").apply {
            put("payload", JSONObject().apply {
                put("id", notificationId)
            })
        }
        return formatMessage(dismissJson.toString())
    }

    /**
     * Parses a notification message and extracts the payload
     */
    fun parseNotificationMessage(jsonMessage: String): NotificationData? {
        return try {
            val json = JSONObject(jsonMessage)
            val payload = json.getJSONObject("payload")
            
            NotificationData(
                id = payload.getString("id"),
                title = payload.optString("title", ""),
                body = payload.optString("body", ""),
                app = payload.optString("app", ""),
                packageName = payload.getString("package"),
                timestamp = payload.getLong("timestamp"),
                canReply = payload.optBoolean("can_reply", false),
                actions = parseActions(payload.optJSONArray("actions"))
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parses notification reply message
     */
    fun parseNotificationReplyMessage(jsonMessage: String): NotificationReply? {
        return try {
            val json = JSONObject(jsonMessage)
            val payload = json.getJSONObject("payload")
            
            NotificationReply(
                id = payload.getString("id"),
                key = payload.getString("key"),
                text = payload.getString("text")
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parses notification action message
     */
    fun parseNotificationActionMessage(jsonMessage: String): NotificationActionRequest? {
        return try {
            val json = JSONObject(jsonMessage)
            val payload = json.getJSONObject("payload")
            
            NotificationActionRequest(
                id = payload.getString("id"),
                key = payload.getString("key")
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parses notification dismiss message
     */
    fun parseNotificationDismissMessage(jsonMessage: String): NotificationDismiss? {
        return try {
            val json = JSONObject(jsonMessage)
            val payload = json.getJSONObject("payload")
            
            NotificationDismiss(
                id = payload.getString("id")
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseActions(actionsArray: JSONArray?): List<NotificationAction> {
        if (actionsArray == null) return emptyList()
        
        val actions = mutableListOf<NotificationAction>()
        for (i in 0 until actionsArray.length()) {
            val actionObj = actionsArray.getJSONObject(i)
            actions.add(
                NotificationAction(
                    title = actionObj.getString("title"),
                    type = actionObj.getString("type"),
                    key = actionObj.getString("key")
                )
            )
        }
        return actions
    }
}

/**
 * Data classes for notification protocol
 */
data class NotificationAction(
    val title: String,
    val type: String, // "remote_input" or "action"
    val key: String
)

data class NotificationData(
    val id: String,
    val title: String,
    val body: String,
    val app: String,
    val packageName: String,
    val timestamp: Long,
    val canReply: Boolean,
    val actions: List<NotificationAction>
)

data class NotificationReply(
    val id: String,
    val key: String,
    val text: String
)

data class NotificationActionRequest(
    val id: String,
    val key: String
)

data class NotificationDismiss(
    val id: String
)
