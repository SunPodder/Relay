
# RFC: Heartbeat Protocol (`ping`/`pong`)

## 1. Overview
The heartbeat protocol maintains connection health between Android and Desktop. Either side may initiate a `ping` to check if the other is alive; the recipient must reply with a `pong`.

## 2. Roles
- **Either → Other**: Send `ping` to check liveness
- **Recipient**: Must reply with `pong`

## 3. Message Formats

### `ping`
```jsonc
{
  "type": "ping",
  "id": "ping-001",
  "payload": {
    "device": "Sun-PC" // Optional device name for context
  }
}
```

### `pong`
```jsonc
{
  "type": "pong",
  "id": "ping-001", // Should match the ping ID
  "payload": {
    "device": "Sun-Phone"
  }
}
```

## 4. Example

**Ping from Desktop:**
```json
{
  "type": "ping",
  "id": "ping-001",
  "payload": { "device": "Desktop" }
}
```

**Pong from Android:**
```json
{
  "type": "pong",
  "id": "ping-001",
  "payload": { "device": "Pixel 7" }
}
```

## 5. Behavior Table

| Sender      | Type   | Expected Reply |
| ----------- | ------ | -------------- |
| Linux       | `ping` | `pong`         |
| Android     | `ping` | `pong`         |
| Either side | ✅      | ✅              |

## 6. Handling
- Use the same `id` for `ping` and `pong` to correlate messages.
- If no `pong` is received within a timeout, disconnect the client.

## 7. Notes
- Heartbeat is essential for detecting dropped or dead connections.
- Both sides should implement automatic pinging and pong response.
