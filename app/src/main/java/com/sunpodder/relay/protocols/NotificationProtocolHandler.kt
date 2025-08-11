package com.sunpodder.relay.protocols

import org.json.JSONArray
import org.json.JSONObject

/**
 * Handles notification protocol messages according to RFC specification
 */
class NotificationProtocolHandler : BaseProtocolHandler {

    /**
     * Creates a notification message
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
                        put("type", action.type.value)
                        put("key", action.key)
                    })
                }
                put("actions", actionsArray)
            })
        }
        return formatMessage(notificationJson.toString())
    }

    /**
     * Creates a notification action message for dismiss (Android → Desktop or Desktop → Android)
     */
    fun createNotificationActionMessage(
        notificationId: String,
        type: NotificationActionType,
        key: String? = null,
        body: String? = null
    ): ByteArray {
        val actionJson = createBaseMessage("notification_action").apply {
            put("payload", JSONObject().apply {
                put("id", notificationId)
                put("type", type.value)
                if (key != null) {
                    put("key", key)
                }
                if (body != null) {
                    put("body", body)
                }
            })
        }
        return formatMessage(actionJson.toString())
    }

    /**
     * Parses notification action message
     */
    fun parseNotificationActionMessage(jsonMessage: String): NotificationActionRequest? {
        return try {
            val json = JSONObject(jsonMessage)
            val payload = json.getJSONObject("payload")
            
            val typeString = payload.getString("type")
            val actionType = NotificationActionType.fromString(typeString)
                ?: return null // Invalid action type
            
            NotificationActionRequest(
                id = payload.getString("id"),
                key = payload.optString("key").takeIf { it.isNotEmpty() },
                type = actionType,
                body = payload.optString("body").takeIf { it.isNotEmpty() }
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
            val typeString = actionObj.getString("type")
            val actionType = NotificationActionType.fromString(typeString)
            
            if (actionType != null) {
                actions.add(
                    NotificationAction(
                        title = actionObj.getString("title"),
                        type = actionType,
                        key = actionObj.getString("key")
                    )
                )
            }
        }
        return actions
    }
}

/**
 * Enums for notification protocol
 */
enum class NotificationActionType(val value: String) {
    REMOTE_INPUT("remote_input"),
    ACTION("action"),
    DISMISS("notification_dismiss");
    
    companion object {
        fun fromString(value: String): NotificationActionType? {
            return values().find { it.value == value }
        }
    }
}

/**
 * Data classes for notification protocol
 */
data class NotificationAction(
    val title: String,
    val type: NotificationActionType,
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

data class NotificationActionRequest(
    val id: String,
    val key: String? = null, // Not needed for dismiss
    val type: NotificationActionType,
    val body: String? = null
)

data class NotificationDismiss(
    val id: String
)
