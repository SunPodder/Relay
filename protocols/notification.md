

# RFC: Notification Protocol

## 1. Overview
The notification protocol allows Android to send notification events to Desktop, and Desktop to send interaction commands (reply, dismiss, action) back to Android.

## 2. Roles
- **Android → Desktop**: Notifies about notifications
- **Desktop → Android**: Sends interaction commands (reply, dismiss, action)

## 3. Message Formats

### 3.1 Notification Event (`notification`)
Sent from Android to Desktop to inform about a new or updated notification.

```jsonc
{
  "type": "notification",
  "payload": {
    "id": "abc123",                // Unique notification key (see Notes)
    "title": "Messenger",
    "body": "Hey, what's up?",
    "app": "Messenger",
    "package": "com.facebook.orca",
    "timestamp": 1729612345,
    "can_reply": true,
    "actions": [
      {
        "title": "Reply",
        "type": "remote_input",
        "key": "quick_reply"
      },
      {
        "title": "Mark as Read",
        "type": "action",
        "key": "mark_read"
      }
    ]
  }
}
```

### 3.2 Notification Reply (`notification_reply`)
Sent from Desktop to Android to reply to a notification.

```jsonc
{
  "type": "notification_reply",
  "payload": {
    "id": "abc123",             // Notification ID
    "key": "quick_reply",       // RemoteInput key
    "text": "Sure, talk later"  // The reply message
  }
}
```

### 3.3 Notification Action (`notification_action`)
Sent from Desktop to Android to trigger a button/action in the notification.

```jsonc
{
  "type": "notification_action",
  "payload": {
    "id": "abc123",
    "key": "mark_read"        // Action key from the notification
  }
}
```

### 3.4 Notification Dismiss (`notification_dismiss`)
Sent from Desktop to Android to dismiss the notification.

```jsonc
{
  "type": "notification_dismiss",
  "payload": {
    "id": "abc123"
  }
}
```

## 4. Examples

**Notification from Android:**
```json
{
  "type": "notification",
  "payload": {
    "id": "abc123",
    "title": "Messenger",
    "body": "Hey, what's up?",
    "app": "Messenger",
    "package": "com.facebook.orca",
    "timestamp": 1729612345,
    "can_reply": true,
    "actions": [
      { "title": "Reply", "type": "remote_input", "key": "quick_reply" },
      { "title": "Mark as Read", "type": "action", "key": "mark_read" }
    ]
  }
}
```

**Reply from Desktop:**
```json
{
  "type": "notification_reply",
  "payload": {
    "id": "abc123",
    "key": "quick_reply",
    "text": "Sure, talk later"
  }
}
```

**Action from Desktop:**
```json
{
  "type": "notification_action",
  "payload": {
    "id": "abc123",
    "key": "mark_read"
  }
}
```

**Dismiss from Desktop:**
```json
{
  "type": "notification_dismiss",
  "payload": {
    "id": "abc123"
  }
}
```

## 5. Handling

- On Android, store a map of active notifications with their `id` and action metadata.
- On `notification_reply`, use `RemoteInput` and `PendingIntent` to send the reply.
- On `notification_action`, call `PendingIntent.send()` for the specified action.
- On `notification_dismiss`, use `NotificationManager.cancel()` with the tag/id.

## 6. Notes on `id`
- Use Android’s `StatusBarNotification.getKey()` if available — it's a stable unique ID across updates/dismissals.
- Fallback can be a custom hash of `package + timestamp`.

## 7. Security Notes
- Anyone on the LAN could send a fake JSON to act like a dismiss or reply.
- Recommended: Pairing with a key/fingerprint, auth tokens, MACs, or optional encryption.

