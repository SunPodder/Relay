
# RFC: Acknowledgment (`ack`)

## 1. Overview
The `ack` message is used to acknowledge receipt or result of a previous message, such as a handshake, command, or error.

## 2. Roles
- **Server → Client**: Acknowledge connection or command
- **Client → Server**: (Optional) Acknowledge server commands

## 3. Message Format

```jsonc
{
  "type": "ack",
  "payload": {
    "ref_id": "conn-001",         // ID of the message being acknowledged
    "status": "ok",               // "ok" or "error"
    "reason": "Invalid token"     // Optional, present if status is "error"
  }
}
```

## 4. Example

```json
{
  "type": "ack",
  "payload": {
    "ref_id": "conn-001",
    "status": "ok"
  }
}
```

## 5. Handling
- `ref_id` must match the `id` of the message being acknowledged.
- `status` is `ok` for success, `error` for failure.
- If `status` is `error`, `reason` should be provided for debugging.

## 6. Notes
- All critical protocol messages should be acknowledged for reliability.
