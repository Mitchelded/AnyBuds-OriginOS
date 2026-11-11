# Independent Enco acquisition — 1.5-dev

## Evidence and scope

Reconstructed from the user-supplied HeyMelody 117.6.6 APK identified in HEYMELODY_ANALYSIS.md. Source paths below refer to private JADX output, not vendored files. No HeyMelody classes or assets are packaged in AnyBuds.

| Evidence | Finding |
| --- | --- |
| A8/d.java, p363z8/b.java constructor | Default BR service UUID `00001107-D102-11E1-9B23-00025B00A5A5`; device configuration may override it |
| A8/c.java, socket creation | Secure `BluetoothDevice.createRfcommSocketToServiceRecord(UUID)` |
| F8/b.java sendWrapData and G8/b.java OPOv1Wrapper | `AA`, base-128 variable-length link size, two control bytes, optional fragment sequence, command packet; no checksum in this inspected wrapper |
| p224p8/a.java and F8/b.java | Command packet: uint16 LE command, uint8 sequence, uint16 LE payload size, payload |
| protocol/commands/j.java a() and case 33280 | Send command `0x0200` with empty payload; response `0x8200` has status, count, supported event IDs |
| Same class, capability response handler | Register advertised events with `0x0201` and one event-ID byte when using single registration; `0x8201` response has status and event ID |
| Same class, command 516 and event case 1 | `0x0204` carries event ID followed by event data; event 1 is battery |
| protocol/commands/g.java d(), BatteryInfo constructor | Battery data: count followed by `(part, packedLevel)` pairs; 1=left, 2=right, 3=case; low 7 bits percentage, high bit charging |
| BatteryInfo.fixBattery | HeyMelody normally rounds upward to a multiple of 10; its case value 1 is special. Our independent provider preserves raw valid percentages |
| Db/j.java, Aa/a.java w(), p265s8/b.java a() | Alternative system `+VDBTY` event: count followed by type/value pairs. Charging is carried forward from earlier vendor state, not present in that event |

Model-specific configuration and the actual Enco Air2i transport have **not** been observed on the phone. HeyMelody also contains a BLE transport. Its BR default is implemented as an opt-in experimental candidate, not declared universal Enco support. Initial notification on subscription, required initialization beyond capability negotiation, and coexistence with HeyMelody remain device validation items. The APK does not contain an obvious plaintext Enco Air2i configuration in the inspected assets.

## Implemented exchange

On an existing matching Bluetooth profile connection, source `enco` opens one RFCOMM session to an already bonded device. A new session cancels the previous one. It sends capability request sequence 0 and, only after a successful matching response advertises event 1, sends single-event registration sequence 1. No broad subscription or unknown command is sent. The following frames are derived from the inspected encoding, not captured phone traffic:

```text
Capabilities: AA 07 00 00 00 02 00 00 00
Register battery: AA 08 00 00 01 02 01 01 00 01
```

Valid battery notifications publish separate current report parts to the existing popup delivery path. Missing/zero/invalid/duplicate parts stay unknown. Reports do not reopen a dismissed popup. Cache reads are source-specific so switching from HeyMelody does not make its cached data appear to prove independent acquisition.

The session runs outside the Bluetooth main thread, closes after 20 seconds including connection time, and stops on ACL disconnect or replacement. There is no automatic retry loop, pairing, Bluetooth discovery/cancellation, audio profile change, firmware write, device setting command, persistent service, or HeyMelody startup. An inactive source/filter prevents further delivery. Settings changes with no incoming event may leave the socket open until its bounded deadline. Only the socket created by this client is closed.

The parser handles short stream reads, concatenated frames, two-byte lengths and ordered link fragmentation. Limits: 4096 bytes per assembled packet, 64 fragments, 128 packets per acquisition. Invalid framing ends the attempt. Unknown valid command packets are logged by ID/length, without recording device addresses or raw payloads.

This is a short popup acquisition window, not continuous battery monitoring. No polling-current-value command is implemented: the inspected sender path establishes subscription, while whether an initial report arrives requires the device test. `+VDBTY` listening and BLE acquisition are not implemented in this increment. Case presence and source measurement age remain unknown.

## Android API verification

Official [BluetoothSocket reference](https://developer.android.com/reference/android/bluetooth/BluetoothSocket) documents blocking connect/read and immediate abortion by close from another thread. This supports the worker/deadline design. Runtime permissions are the hooked Bluetooth host's; SecurityException is caught and logged. Module manifest permissions would not grant access to that host.

The official [BluetoothHeadset reference](https://developer.android.com/reference/android/bluetooth/BluetoothHeadset) documents vendor-specific headset events and company-ID categories. Receiving `+VDBTY` on this firmware remains unverified; it is not a claimed fallback.

## Verification boundary

JVM fixtures are specification-derived, not device captures. They validate codec behavior and cache source isolation. A successful build cannot establish RFCOMM service availability, initialization sufficiency, radio coexistence or fresh measurements on iQOO. Follow ENCO_TEST.md and retain the first failing diagnostic stage for the next increment.
