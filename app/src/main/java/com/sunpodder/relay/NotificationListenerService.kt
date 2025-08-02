package com.sunpodder.relay

import android.app.Notification
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService as AndroidNotificationListenerService
import android.service.notification.StatusBarNotification
import com.sunpodder.relay.protocols.NotificationAction
import kotlinx.coroutines.*

class NotificationListenerService : AndroidNotificationListenerService() {

    companion object {
        private const val TAG = "RelayNotificationListener"
        // Singleton instance for accessing the service from other components
        var instance: NotificationListenerService? = null
    }

    // Reference to the TCP server helper for broadcasting notifications
    private var tcpServerHelper: TcpServerHelper? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Try to connect to TCP server if it's already running
        connectToTcpServerIfAvailable()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    /**
     * Try to connect to the TCP server if it's available
     */
    private fun connectToTcpServerIfAvailable() {
        // This method will be called when the NotificationListenerService starts
        // The RelaySocketService should also try to connect when it starts
        // Removed verbose logging
    }

    /**
     * Set the TCP server helper for broadcasting notifications
     */
    fun setTcpServerHelper(helper: TcpServerHelper?) {
        tcpServerHelper = helper
        if (helper != null) {
            UILogger.i(TAG, "TCP server connected for notification broadcasting")
        } else {
            UILogger.w(TAG, "TCP server disconnected")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        
        sbn?.let { notification ->
            extractAndBroadcastNotification(notification)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        
        sbn?.let { _ ->
            // TODO: Send notification dismiss message to clients
        }
    }

    private fun extractAndBroadcastNotification(sbn: StatusBarNotification) {
        val notification = sbn.notification
        val packageName = sbn.packageName
        val postTime = sbn.postTime
        val key = sbn.key

        // Skip system notifications and our own app notifications
        if (shouldSkipNotification(packageName, notification)) {
            return
        }

        // Extract notification content
        val extras = notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        
        // Use bigText if available, otherwise use text
        val finalText = bigText?.takeIf { it.isNotEmpty() } ?: text
        
        // Extract app name
        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (e: Exception) {
            packageName // Fallback to package name
        }

        // Extract actions
        val actions = extractNotificationActions(notification)
        val canReply = actions.any { it.type == "remote_input" }

        // Use StatusBarNotification key as unique ID, fallback to package + timestamp
        val notificationId = key ?: "${packageName}_${postTime}"
        // Broadcast to connected clients
        tcpServerHelper?.let { server ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    server.sendNotificationToAllClients(
                        id = notificationId,
                        title = title,
                        body = finalText,
                        app = appName,
                        packageName = packageName,
                        canReply = canReply,
                        actions = actions
                    )
                } catch (e: Exception) {
                    UILogger.e(TAG, "Failed to broadcast notification", e)
                }
            }
        } ?: run {
            UILogger.w(TAG, "TCP server not available")
        }
    }

    private fun shouldSkipNotification(packageName: String, notification: Notification): Boolean {
        // Skip our own notifications
        if (packageName == this.packageName) {
            return true
        }
        
        // Skip system UI notifications (like charging, low battery, etc.)
        if (packageName == "com.android.systemui") {
            return true
        }
        
        // Skip other system packages
        if (packageName.startsWith("android") || 
            packageName.startsWith("com.android") ||
            packageName == "com.google.android.gms") {
            return true
        }
        
        // Check notification content for system-like notifications
        val extras = notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.lowercase()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.lowercase()
        
        // Skip charging/battery notifications
        if (title?.contains("charging") == true || 
            title?.contains("battery") == true ||
            text?.contains("charging") == true ||
            text?.contains("battery") == true ||
            text?.contains("until full") == true) {
            return true
        }
        
        // Skip network/connection notifications
        if (title?.contains("connected to") == true ||
            title?.contains("wifi") == true ||
            title?.contains("network") == true) {
            return true
        }
        
        // Skip screenshot notifications
        if (title?.contains("screenshot") == true) {
            return true
        }
        
        // Skip ongoing system notifications (flags check)
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) {
            // Allow music/media notifications even if ongoing
            if (notification.category != Notification.CATEGORY_TRANSPORT) {
                return true
            }
        }
        
        return false
    }

    private fun extractNotificationActions(notification: Notification): List<NotificationAction> {
        val actions = mutableListOf<NotificationAction>()
        
        notification.actions?.forEach { action ->
            val actionType = if (action.remoteInputs?.isNotEmpty() == true) {
                "remote_input"
            } else {
                "action"
            }
            
            // Use action title as key, or generate one
            val actionKey = action.title?.toString()?.lowercase()?.replace(" ", "_") ?: "action_${actions.size}"
            
            actions.add(
                NotificationAction(
                    title = action.title?.toString() ?: "Action",
                    type = actionType,
                    key = actionKey
                )
            )
        }
        
        return actions
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        UILogger.i(TAG, "Notification Listener Connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        UILogger.w(TAG, "Notification Listener Disconnected")
    }
}
