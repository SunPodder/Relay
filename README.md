# Relay Android App

Relay is an Android app for cross-device notification and message relay, supporting a custom protocol for real-time communication with desktop clients.

## Features

- Notification mirroring from Android to desktop
- Actionable notifications (reply, action buttons, dismiss)
- Bidirectional protocol with length-prefixed JSON messages

## Getting Started

**Install the APK**: Download the latest APK from the [Releases page](https://github.com/SunPodder/Relay/releases) and install it on your Android device.

Or **Build from source**: Clone this repository and build the app using Android Studio or Gradle:
```bash
./gradlew assembleDebugOptimized
```
Then install the generated APK on your device.

**First-time setup:**

- Open the Relay app after installation.
- Grant the required notification permissions when prompted.
- Go to your device's App Settings for Relay → Battery optimizations → Allow unrestricted usage (to ensure background operation).
- Make sure the app server is running.
- You can now close the app; the Relay service will keep running in the background and forward notifications to connected clients as long as permissions are granted.

## Cross-Platform Desktop Client

A cross-platform desktop client implementation is available at:
- https://github.com/SunPodder/relay-pc

This client supports Windows, Linux, and macOS, and is designed to work seamlessly with this Android app.

## Protocol

- All communication uses a 4-byte big-endian length-prefixed JSON envelope.
- See `protocols/` for protocol details.

## Contributing

Pull requests and issues are welcome!

## License

MIT License
