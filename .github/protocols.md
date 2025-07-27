### Shared Envelope Format:

```json
{
  "type": "<type>",         // conn, ack, ping, pong, sms
  "id": "uuid-123",         // Optional unique ID for tracking
  "timestamp": 1729577323,  // Optional UNIX epoch
  "payload": { ... }        // Payload depends on message type
}
```

---

## 1️⃣ `conn` – Connection Handshake

```json
{
  "type": "conn",
  "id": "conn-001",
  "payload": {
    "device_name": "Sun-PC",
    "platform": "linux",               // "android" or "linux"
    "version": "0.1.0",
    "supports": ["sms", "ping", "pong"],
    "auth_token": "optional-token"
  }
}
```

---

## 2️⃣ `ack` – Acknowledge a Previous Message

```json
{
  "type": "ack",
  "payload": {
    "ref_id": "conn-001",
    "status": "ok",               // or "error"
    "reason": "Invalid token"     // Optional if error
  }
}
```

---

## 3️⃣ `ping` – Heartbeat Request

```json
{
  "type": "ping",
  "id": "ping-001",
  "payload": {
    "device": "Sun-PC"           // Optional device name for context
  }
}
```

---

## 4️⃣ `pong` – Heartbeat Reply

```json
{
  "type": "pong",
  "id": "ping-001",              // Should match the ping ID (optional, but clean)
  "payload": {
    "device": "Sun-Phone"
  }
}
```

> 🔄 You can use the same `id` as the `ping` message to link it, or generate a new one.

---

## 5️⃣ `sms` – Send or Receive SMS

```json
{
  "type": "sms",
  "id": "sms-uuid-001",
  "payload": {
    "direction": "incoming",        // or "outgoing"
    "from": "+880123456789",        // Incoming only
    "to": "+880123456789",          // Outgoing only
    "body": "Hey, how are you?",
    "timestamp": 1729578000,
    "status": "sent"                // Optional: sent, delivered, failed
  }
}
```

---

## 🔁 Ping-Pong Behavior Summary

| Sender      | Type   | Expected Reply |
| ----------- | ------ | -------------- |
| Linux       | `ping` | `pong`         |
| Android     | `ping` | `pong`         |
| Either side | ✅      | ✅              |

---
