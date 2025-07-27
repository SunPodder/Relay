package com.sunpodder.relay

import android.app.Notification
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "RelayNotificationListener"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        
        sbn?.let { notification ->
            extractAndLogNotificationDetails(notification)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        
        sbn?.let { notification ->
            UILogger.d(TAG, "Notification removed from: ${notification.packageName}")
        }
    }

    private fun extractAndLogNotificationDetails(sbn: StatusBarNotification) {
        val notification = sbn.notification
        val packageName = sbn.packageName
        val postTime = sbn.postTime
        val id = sbn.id
        val tag = sbn.tag
        val key = sbn.key

        // Extract notification content
        val extras = notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val subText = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val bigText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val summaryText = extras?.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
        val infoText = extras?.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()
        
        // Extract additional details
        val category = notification.category
        val priority = notification.priority
        val flags = notification.flags
        val group = notification.group
        val sortKey = notification.sortKey

        // // Log all extracted details
        // UILogger.d(TAG, "=== New Notification ===")
        // UILogger.d(TAG, "Package: $packageName")
        // UILogger.d(TAG, "ID: $id")
        // UILogger.d(TAG, "Tag: $tag")
        // UILogger.d(TAG, "Key: $key")
        // UILogger.d(TAG, "Post Time: $postTime")
        // UILogger.d(TAG, "Title: $title")
        // UILogger.d(TAG, "Text: $text")
        // UILogger.d(TAG, "Sub Text: $subText")
        // UILogger.d(TAG, "Big Text: $bigText")
        // UILogger.d(TAG, "Summary Text: $summaryText")
        // UILogger.d(TAG, "Info Text: $infoText")
        // UILogger.d(TAG, "Category: $category")
        // UILogger.d(TAG, "Priority: $priority")
        // UILogger.d(TAG, "Flags: $flags")
        // UILogger.d(TAG, "Group: $group")
        // UILogger.d(TAG, "Sort Key: $sortKey")
        
        // Extract actions if any
        notification.actions?.let { actions ->
            // UILogger.d(TAG, "Actions count: ${actions.size}")
            actions.forEachIndexed { index, action ->
                // UILogger.d(TAG, "Action $index: ${action.title}")
            }
        }

        // Extract progress info if it's a progress notification
        if (extras?.containsKey(Notification.EXTRA_PROGRESS) == true) {
            val progress = extras.getInt(Notification.EXTRA_PROGRESS)
            val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX)
            val progressIndeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE)
            // UILogger.d(TAG, "Progress: $progress/$progressMax (indeterminate: $progressIndeterminate)")
        }

        // Extract people if it's a messaging notification
        val people = extras?.getStringArray(Notification.EXTRA_PEOPLE)
        people?.let {
            // UILogger.d(TAG, "People: ${it.joinToString(", ")}")
        }

        // UILogger.d(TAG, "========================")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        UILogger.d(TAG, "Notification Listener Service Connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        UILogger.d(TAG, "Notification Listener Service Disconnected")
    }
}
