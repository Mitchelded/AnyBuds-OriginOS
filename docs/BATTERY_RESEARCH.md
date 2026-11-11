# Battery source investigation — 2026-11-11

## Evidence levels

Project behavior is confirmed by source inspection. Android APIs below are confirmed in AOSP source, not on the user's firmware. The original vivo and HeyMelody APKs are absent; no proprietary protocol has been reverse-engineered in this session. No phone was accessed.

## Legacy source

`BtWatcher.batteryOf` reflectively calls `BluetoothDevice.getBatteryLevel()`, then duplicates that single value into L/R and supplies case=-1. It listens to aggregate BATTERY_LEVEL_CHANGED only during a 20-second follow-up window after a matching connection. `DeviceBean` already accepts separate supplied values through `setLeftBattery`, `setRightBattery`, `setBaseBattery`, plus charging/in-case bit masks. These setters are rendering inputs, not a measurement source. `PopupRequest` defaults (80/80/100) are demonstration values.

VivoApi comments describe a BLE state machine that produces TwsFastPairDeviceBean and passes JSON to the UI service. This is inherited documentation; tracing the actual scanner/parser requires the exact APK, version and hash. Never infer packet layouts or Binder transaction codes from these names.

## Verified Android candidate: Bluetooth metadata

The AOSP [BluetoothDevice source](https://android.googlesource.com/platform/packages/modules/Bluetooth/+/a5a1f4f32268a9bdff15978dcf6126825a8e0c44/framework/java/android/bluetooth/BluetoothDevice.java) defines hidden/system API `getMetadata(int)` returning nullable byte arrays. Keys 10/11/12 are untethered left/right/case battery; 13/14/15 are corresponding charging states. Battery entries are encoded numeric strings, 0–100. Key 18 is main-device battery, not per-part battery. Reflection can resolve existing APIs, but does not grant permissions or make missing metadata appear.

The AOSP [AdapterService source](https://android.googlesource.com/platform/packages/modules/Bluetooth/+/0e6d59eab0ab8f1971eb5b2c671973602b529ea3/android/app/src/com/android/bluetooth/btservice/AdapterService.java) checks caller/connection permissions and privileged Bluetooth access and reads metadata from its database manager. Therefore these are cached records, not a direct earbud poll. Their presence does not prove current charging/connection state or fresh case telemetry. Runtime availability, host privileges, population source and update timestamps need verification on the exact OriginOS build.

Older Android framework definitions also contain separate keys ([source](https://android.googlesource.com/platform/frameworks/base/+/c9611acb621254df51c3771033fd706a58d7e282/core/java/android/bluetooth/BluetoothDevice.java)). Some early versions used misspelled `UNTHETHERED` field names; inspect the runtime fields/API rather than inventing vendor method names.

No in-case state is established by these battery/charging keys. Charging=true may be evidence of a state but is not a universal case-presence API; charging=false cannot establish that an earbud is outside the case. The source cache cannot establish a live update frequency by itself. One-bud connection may leave old metadata for the other bud; freshness must remain explicit.

## Next investigation, in priority order

1. Exact HeyMelody APK: record SHA256/package/version/permissions, inspect DEX/resources/native libraries; find L/R/case UI bindings and trace back to transport/parser. Identify GATT characteristics or vendor frames only from concrete code evidence. Do not download untrusted mirrors merely to obtain an arbitrary version.
2. Exact vivo TWS/service APKs: trace TwsFastPairDeviceBean setters back to BLE scan/advertisement parsing, metadata setters, services/Binder and native libraries. Preserve version-specific evidence.
3. Android APIs: capability-check existing aggregate battery and metadata APIs in the Bluetooth host. Confirm whether metadata changes are published and by whom. Keep unknowns when absent, denied, malformed or stale.
4. GATT: use only characteristics verified in the target protocol. A generic battery service does not by itself label left/right/case. No active discovery/connect/write implementation until its interaction with normal Bluetooth is understood and user-tested.
5. Vendor reflection/Binder/native integration only after confirming concrete interfaces/signatures/version constraints. Read-only providers must not alter pairing, routing or other Bluetooth state.

## Provider design and acceptance

Model aggregate independently from L/R/case. Each observation carries source, device identity and local observation time; source measurement time can remain unknown. Charging and in-case are tri-state, never default unknown to false. Merge per part, expire or clearly label cached observations, clear connection lifecycle state, and reject values outside 0–100. An aggregate fallback may be shown as aggregate but must never be called separate measured levels.

First implement parser/provider contracts with unit tests for missing/malformed/partial/out-of-range inputs and timestamps. Then integrate an optional metadata reader behind capability/error handling and compare actual output with vendor UI using MANUAL_TESTS B04–B08. No vendor accuracy claim before user confirmation.

## Implemented in 1.1-dev

`BatteryProvider` defines the read-only acquisition boundary. `AndroidMetadataBatteryProvider` resolves the existing hidden public `BluetoothDevice.getMetadata(int)` method at runtime. `MetadataBatteryProvider` reads keys 10–15 independently. An absent API, permission denial, reflection failure or malformed value leaves the affected fields unknown. One field failing preserves other results.

`BatterySnapshot` retains the device address, local monotonic read time, separate aggregate value, L/R/case parts, source per part and nullable charging values. Percentages outside 0–100 are rejected. Charging accepts textual true/false. Unknown charging stays nullable in the model and is tracked through `chargingKnownMask()` in logs. Source measurement time remains unavailable. Snapshots are created afresh for each read and device.

The existing `BtWatcher.request` entry point selects this provider only when global setting `twspopup_battery_source` equals `metadata`. Enable with `settings put global twspopup_battery_source metadata` in the user's own on-device terminal. Clear it with `settings delete global twspopup_battery_source` to restore the aggregate path. Existing `twspopup_auto` device filter and Bluetooth module scope are required for automatic requests. Manual SHOW commands continue to use their explicit/demo inputs.

In metadata mode the SHOW extras receive each part independently. Missing parts remain -1 even when aggregate battery is available. The aggregate value remains visible in diagnostics. Charging indicators are enabled only for explicitly true fields. The legacy renderer has a bitmask for charging and cannot visually distinguish unknown from an absent charging icon. Its inherited in-case visual default remains 3. No case-presence telemetry is acquired.

Reads happen after the existing 1.5-second connection delay and on existing aggregate battery broadcasts during the 20-second follow-up window. Metadata-only changes have no listener or periodic poll yet. Android may retain old values across connections. Local read time establishes when the cache was accessed. Cache measurement age and one-bud presence remain unresolved. This provider requires user comparison with HeyMelody/vendor UI before accuracy acceptance.

Eight host-side unit tests passed for independent parts, empty metadata, partial permission failures, malformed/range validation, zero/full values, unknown charging, unavailable API and device/read isolation. Reflection and OriginOS behavior require manual confirmation.
