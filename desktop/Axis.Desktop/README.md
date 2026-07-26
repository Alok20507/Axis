# Axis Desktop controller boundary

`IVirtualGamepadBackend` is the sole hardware-emulation boundary. UDP decoding produces
`NormalizedControllerState`; `XInputMapper` deterministically converts it to Xbox 360 ranges:
steering maps to signed 16-bit left-stick X and brake/throttle map to the two unsigned triggers.

The included backend deliberately fails closed. ViGEmBus, the historic XInput solution, is retired
and archived, so it is not a production-acceptable default. A selected replacement must implement
this interface, create a signed Windows-recognised Xbox 360 device, and only report connected after
that device is actually registered. `ControllerRuntime` immediately sends neutral/disconnect state
when the network session ends; diagnostics can consume `LastReport` without touching the hot path.
