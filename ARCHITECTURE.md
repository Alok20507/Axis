# RaceLink architecture

## Phase 1 boundary

This repository establishes the Android application shell, module graph, visual language, and the binary control-packet contract. It intentionally does not pretend to be a controller: no socket is opened, no sensor is sampled, and no connection state is fabricated.

```
Compose UI -> feature ViewModel -> domain use case -> domain port
                                              ^             |
                                              |             v
                                      data implementation <- core modules

core-sensors -> normalized ControllerFrame -> core-network -> Windows companion -> virtual gamepad
Windows telemetry -----------------------------------------------------------> dashboard state
```

## Dependency rules

- `app` only composes navigation and dependency injection.
- Feature modules depend on `domain` and `core-ui`; they never access Android transport or storage directly.
- `domain` contains Kotlin-only models and ports. It has no Android dependency.
- `data` implements domain ports and is the only layer permitted to join storage, networking, Bluetooth, or sensors.
- `core-*` modules expose small platform-specific capabilities and have no feature dependencies.

## Real-time control path

The control lane is separate from UI state. It uses a single sensor producer, a bounded latest-value handoff, and one UDP sender. A 120 Hz sender reads the most recent normalized frame; it never queues old steering values. Buffers are preallocated and binary packets have a fixed width. Dashboard telemetry is downsampled before reaching Compose.

`ControllerPacketCodec` defines the initial 36-byte big-endian control packet. It includes a protocol version, sequence, and monotonic timestamp so the peer can reject malformed or stale data and calculate loss and one-way timing once clocks are aligned.

## Decisions and risks

- Wi-Fi UDP is the first transport because it can meet racing-input latency requirements. BLE and USB are separate transports, not alternate code paths inside the input loop.
- Pairing must establish an authenticated session before accepting control frames. A raw LAN UDP port is not acceptable for a release build.
- Android sensor timestamps are monotonic but are not peer-clock timestamps. Latency estimation requires an explicit ping/echo clock-offset estimator.
- The virtual Xbox driver choice belongs to the Windows companion milestone; it must be verified for maintenance, signing, redistribution, and Windows 11 compatibility before app UI promises broad game support.
- Android cold start under one second must be measured on target hardware after dependency injection, database, and discovery are implemented. It cannot be claimed from this foundation.
