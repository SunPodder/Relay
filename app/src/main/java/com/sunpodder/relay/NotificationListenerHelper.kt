package com.sunpodder.relay

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils

object NotificationListenerHelper {

    /**
     * Check if the notification listener service is enabled for this app
     */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        
        if (!flat.isNullOrEmpty()) {
            val names = flat.split(":").toTypedArray()
            for (name in names) {
                val componentName = ComponentName.unflattenFromString(name)
                if (componentName != null) {
                    if (TextUtils.equals(packageName, componentName.packageName)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * Get the component name for the notification listener service
     */
    fun getNotificationListenerComponentName(context: Context): ComponentName {
        return ComponentName(context, NotificationListenerService::class.java)
    }

    /**
     * Check if the specific notification listener service is enabled
     */
    fun isNotificationListenerServiceEnabled(context: Context): Boolean {
        val componentName = getNotificationListenerComponentName(context)
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        
        if (!flat.isNullOrEmpty()) {
            val names = flat.split(":").toTypedArray()
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && cn == componentName) {
                    return true
                }
            }
        }
        return false
    }
}
