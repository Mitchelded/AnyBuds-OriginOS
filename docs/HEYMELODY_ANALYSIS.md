# HeyMelody 117.6.6 — offline battery investigation

## Input and reproducibility

User-supplied `HeyMelody_v117.6.6.apk`, received 2026-11-11. Package `com.heytap.headset`, versionName `117.6.6`, versionCode `117006006` (decoded AndroidManifest.xml).

- Size: 16,482,304 bytes.
- SHA256: `d0e5a2b2a51c24a17c9f7fcf6485c68a3f3d33c16de60a5351733a9c34b0ada3`.
- Three DEX files. Native libraries: glide-webp and sqlcipher.
- JADX 1.5.6 official archive verified against release asset digest `545ea2be9c242511bc145755cf4bda2485ade42966e096f8b4d3da2a230e8974`.
- Local output: ignored `.tools/heymelody/`. JADX completed with 66 errors; decompiled Java is analysis evidence, not a guarantee that every method was reconstructed correctly. No vendor code/assets are added to the module.

## Concrete findings

Paths and line numbers below refer to this local JADX output, with its default renamed package directories.

1. `com/oplus/melody/btsdk/api/data/BatteryInfo.java`: public fields `mDeviceType`, `mLevel`, `mCharging`. Constructor `BatteryInfo(int offset, byte[] data)` reads two bytes: device type, then level in low 7 bits and charging in bit 7. This is a vendor message representation, not the Android aggregate battery API.
2. `com/oplus/melody/btsdk/protocol/commands/g.java`, `d(int offset, byte[] data)`: reads an unsigned count followed by that many two-byte BatteryInfo records. Rejects zero count and truncated data. Does not establish packet framing/checksum or target-model support by itself.
3. `com/oplus/melody/btsdk/protocol/commands/j.java` (NotificationCommandManager), `b(String address, int offset, byte[] data, p224p8.a packet)`, lines 86–108 and 364–374: notification type 1 invokes that parser and emits handler message 10 with `Pair(address, batteryList)`. This preserves device identity alongside per-part data.
4. `com/oplus/melody/model/repository/earphone/N.java`, `g(EarphoneDTO)`, lines 280–294: type 1 = left, 2 = right, 3 = case. Selects vendor/Spp fields or separate headset fields based on `isSupportSpp()`.
5. `BatteryInfo.fixBatteries` / `fixBattery`: UI values can be rounded using configured battery radix (default 10), with special handling for case value 1. Vendor display percentages therefore need not be raw packet values. Do not copy its zero/unknown convention blindly into our snapshot model.
6. `A8/c.java`, lines 164–174: transport abstraction uses `BluetoothDevice.createRfcommSocketToServiceRecord(UUID)`. `p349y8/f.java` also implements GATT characteristic lookup, notifications and writes. The APK supports multiple transports; these findings alone do not identify the active Enco Air2i transport.
7. `com/oplus/melody/btsdk/manager/service/BluetoothService.java`, lines 505–538: another path reads per-part cached preferences, constructs BatteryInfo records and emits internal BluetoothReceiveData. This is not evidence that an external app can bind or read those values.

## Implication and next integration boundary

Separate data exists inside HeyMelody's own pipeline. This does not prove that HeyMelody populates Android metadata keys 10–15. An Android metadata reader alone cannot promise equivalent results.

The implemented optional, version-checked Xposed observer runs inside `com.heytap.headset`, receiving already-parsed per-device battery events and passing validated snapshots to the popup. It avoids opening a competing Bluetooth socket. The user now reports successful percentage display while HeyMelody runs, but all unknowns when it is closed/unloaded. This process dependency is inherent in the current observer. Independent acquisition additionally requires transport selection, framing, request/response and lifecycle tracing; a longer cache cannot provide fresh readings without a source. Exact process suspension state and measurement freshness remain unverified.

Initial investigation produced no APK. The preserved 1.3-dev implementation observes `DeviceInfo.setBatteryInfo(List)`. Its code does not hook `updateConnected(String)`. That method exists in the supplied source, but the earlier documentation overstated the implemented disconnect observer. Disconnect handling uses Android Bluetooth ACL events. The main-process BluetoothService is confirmed in the supplied manifest. See `HEYMELODY_TEST.md` for opt-in setup and limitations.

## Follow-up audit and 1.4-dev

The live origin heads matched `main=2cd100c` and development `c418bb1` at the start of this review. Versions 1.2/1.3 and their feedback were present as local uncommitted files. Backup `.backups/20261111-182651` preserves that state and attachments. Commit `34277e9` preserves the reviewed 1.3-dev source separately from subsequent fixes.

The setter alone is insufficient to identify per-part provenance. In `Db/j.java` the `BATTERY_LEVEL_CHANGED` path constructs a single `BatteryInfo(1, aggregate)` and calls the same setter. Its following BluetoothReceiveData has event ID 1048590. The `+VDBTY` vendor event path in that file builds separate type 1/2/3 records and emits 1048589. `BluetoothService.java` also emits 1048589 from per-part cached preferences. `M9/k.java`, case 15, handles the parsed protocol list and emits 1048588 after the setter. Every inspected path creates the event synchronously on the setter's thread with the same DeviceInfo object.

Version 1.4-dev copies current input values after the setter, correlates them with that object's following BluetoothReceiveData constructor, and accepts only 1048588/1048589. Captures expire after 10 seconds, keep a weak reference to the device and are consumed by the battery event. The aggregate event is explicitly excluded. This also covers protocol reports arriving before the vendor's connection flag becomes true. Bluetooth events retain responsibility for window lifecycle.

APK SHA256 was verified again. An independent DEX table read confirmed declared signatures for DeviceInfo.setBatteryInfo(List), getDeviceAddress(), getDeviceName(), and BluetoothReceiveData(int, android.os.Parcelable). Constructor/setter correlation and Xposed runtime delivery on the phone still require manual verification. Event 1048589 may represent cached per-part data, so the integration does not establish source measurement freshness.
