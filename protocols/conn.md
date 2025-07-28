
# RFC: Connection Handshake (`conn`)

## 1. Overview
The `conn` message initiates a handshake between a client (Android or Desktop) and the server. It establishes identity, supported features, and optionally provides authentication.

## 2. Roles
- **Client → Server**: Initiates handshake
- **Server**: Validates and responds with `ack`

## 3. Message Format

```jsonc
{
  "type": "conn",
  "id": "conn-001",
  "payload": {
    "device_name": "Sun-PC",         // Human-readable device name
    "platform": "linux",             // "android" or "linux"
    "version": "0.1.0",              // Client version
    "supports": ["sms", "ping", "pong"], // List of supported protocols
    "auth_token": "optional-token"   // Optional authentication token
  }
}
```

## 4. Example

```json
{
  "type": "conn",
  "id": "conn-001",
  "payload": {
    "device_name": "Pixel 7",
    "platform": "android",
    "version": "1.2.3",
    "supports": ["sms", "ping", "pong", "notification"],
    "auth_token": "my-secret-token"
  }
}
```

## 5. Handling
- Server should validate the handshake and respond with an `ack` message (see `ack.md`).
- `auth_token` can be used for authentication (optional, but recommended for security).
- The `supports` array allows for feature negotiation.

## 6. Security Notes
- If `auth_token` is present, the server should verify it before accepting the connection.
- Consider using TLS or message authentication for sensitive data.
