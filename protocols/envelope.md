```md
# RFC: Length-Prefixed Protocol Envelope Format

## 1. Overview

This document defines the protocol envelope format and framing for communication between Android and Desktop systems. All messages use a length-prefixed JSON envelope structure to ensure consistent, efficient, and extensible communication.

## 2. Transport Framing

Each message transmitted over the socket must be **prefixed with a 4-byte unsigned integer (big-endian)** indicating the length (in bytes) of the JSON message that follows.

### Format

```

+--------------------+-----------------------------+
\| 4-byte length (u32)| JSON-encoded envelope bytes |
+--------------------+-----------------------------+

````

- The length is the number of bytes in the JSON payload (excluding the prefix).
- The JSON must be UTF-8 encoded.

## 3. Envelope Structure

```jsonc
{
  "type": "<type>",         // Required. Message type (e.g., ping, sms, notification)
  "id": "uuid-123",         // Optional. Unique identifier for tracking
  "timestamp": 1729577323,  // Optional. UNIX epoch seconds (UTC)
  "payload": { ... }        // Required. Protocol-specific data
}
````

## 4. Field Descriptions

| Field       | Type    | Required | Description                                                |
| ----------- | ------- | -------- | ---------------------------------------------------------- |
| `type`      | string  | Yes      | Protocol message type (e.g., `conn`, `ack`, `ping`, `sms`) |
| `id`        | string  | No       | Unique ID for request/response correlation                 |
| `timestamp` | integer | No       | UNIX timestamp in seconds (UTC)                            |
| `payload`   | object  | Yes      | Message-specific payload                                   |

## 5. Example Message

### Envelope

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

### Full Transmission (Wire Format)

```
[0x00 0x00 0x00 0x58] // 88-byte length prefix
{"type":"ping","id":"ping-001","timestamp":1729577323,"payload":{"device":"Sun-PC"}}
```

## 6. Requirements

* All messages **must** be framed using the 4-byte length prefix format.
* The envelope **must** be a UTF-8 encoded JSON object.
* All protocol message types **must** conform to this structure.
* Clients and servers **must** read the exact number of bytes specified by the length prefix before attempting to parse the JSON.
