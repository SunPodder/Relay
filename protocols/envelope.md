
# RFC: Protocol Envelope Format

## 1. Overview

All protocol messages are wrapped in a common envelope structure. This ensures consistency, extensibility, and easy parsing for all message types exchanged between Android and Desktop.

## 2. Envelope Structure

```jsonc
{
  "type": "<type>",         // Required. Protocol message type (e.g., conn, ack, ping, pong, sms, notification, etc.)
  "id": "uuid-123",         // Optional. Unique ID for tracking/correlation
  "timestamp": 1729577323,   // Optional. UNIX epoch seconds
  "payload": { ... }         // Required. Protocol-specific data
}
```

## 3. Field Descriptions

| Field      | Type     | Required | Description                                                      |
|------------|----------|----------|------------------------------------------------------------------|
| `type`     | string   | Yes      | Protocol message type (e.g., `conn`, `ack`, `ping`, `sms`, etc.) |
| `id`       | string   | No       | Unique identifier for tracking/correlation                       |
| `timestamp`| integer  | No       | UNIX epoch seconds (UTC)                                         |
| `payload`  | object   | Yes      | Protocol-specific message body                                   |

## 4. Example

```json
{
  "type": "ping",
  "id": "ping-001",
  "timestamp": 1729577323,
  "payload": {
    "device": "Sun-PC"
  }
}
```

## 5. Notes
- All protocol messages **must** use this envelope format.
- The `id` field is recommended for request/response correlation, especially for `ping`/`pong` and `conn`/`ack`.
- The `timestamp` field is optional but recommended for debugging and ordering.
