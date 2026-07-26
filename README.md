# RaceLink

RaceLink turns an Android phone into a low-latency steering controller and dashboard for a paired Windows companion.

## Current milestone

The repository now contains a production-oriented Android foundation: Gradle modules, Compose design system, navigation shell, immutable home state, and a fixed-width UDP control packet contract. Connection, calibration, controller input, telemetry, haptics, and the desktop companion remain deliberately unimplemented.

## Next implementation slice

Build the Windows companion first: authenticated LAN discovery, pairing, a UDP receive loop, and a verified maintained virtual gamepad implementation. Then add Android pairing and a real connection state machine. This produces an end-to-end controller path before introducing sensor fusion.

See [ARCHITECTURE.md](ARCHITECTURE.md) for the dependency rules and real-time design.
