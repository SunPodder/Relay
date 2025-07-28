
# RFC: SMS Protocol (`sms`)

## 1. Overview
The `sms` protocol is used for sending and receiving SMS messages between Android and Desktop. It supports both directions and includes delivery status.

## 2. Roles
- **Android → Desktop**: Notify about incoming SMS
- **Desktop → Android**: Send outgoing SMS

## 3. Message Format

```jsonc
{
  "type": "sms",
  "id": "sms-uuid-001",
  "payload": {
    "direction": "incoming",        // "incoming" or "outgoing"
    "from": "+880123456789",        // Incoming only
    "to": "+880123456789",          // Outgoing only
    "body": "Hey, how are you?",
    "timestamp": 1729578000,         // UNIX epoch seconds
    "status": "sent"                // Optional: sent, delivered, failed
  }
}
```

## 4. Example

**Incoming SMS (Android → Desktop):**
```json
{
  "type": "sms",
  "id": "sms-uuid-002",
  "payload": {
    "direction": "incoming",
    "from": "+880123456789",
    "body": "Hello!",
    "timestamp": 1729579000
  }
}
```

**Outgoing SMS (Desktop → Android):**
```json
{
  "type": "sms",
  "id": "sms-uuid-003",
  "payload": {
    "direction": "outgoing",
    "to": "+880123456789",
    "body": "Hi, this is a test.",
    "timestamp": 1729579050
  }
}
```

## 5. Handling
- `direction` indicates if the message is incoming or outgoing.
- `status` can be used to track delivery state (`sent`, `delivered`, `failed`).
- Timestamps should be UNIX epoch seconds (UTC).

## 6. Notes
- For incoming messages, `from` is required; for outgoing, `to` is required.
- Delivery status updates may be sent as additional `sms` messages with the same `id`.
