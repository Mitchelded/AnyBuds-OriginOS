# HeyMelody source — 1.3-dev (code 10)

Experimental integration for the exact supplied HeyMelody 117.6.6 (com.heytap.headset, code 117006006). Local tests/build do not establish device compatibility.

## Enable manually on the phone

1. Install `artifacts/AnyBuds-OriginOS-1.3-dev-debug.apk` as an update.
2. Enable all three module scopes: `com.android.vivo.tws.vivotws`, **`com.android.bluetooth`** and **`com.heytap.headset`**. Bluetooth owns automatic opening; HeyMelody provides separate battery reports.
3. In the user's root terminal:

```sh
settings put global twspopup_auto Enco
settings put global twspopup_battery_source heymelody
```

4. Reboot once to load the new hooks. Open HeyMelody and keep it running. Reconnect the earbuds. Do not use manual SHOW: it does not acquire battery data.
5. A matching Bluetooth connection should open a popup even without vendor reports, initially showing `--` for unavailable parts. Compare with HeyMelody: both buds, left alone, right alone, case available/unavailable. Capture `logcat -d -s TwsPopup`. Expected chain: `module=1.3-dev HeyMelody process=...`, `HeyMelody observer installed`, `HeyMelody battery callback received`, `source=heymelody_observed`, `HeyMelody report received`, then `shown`/`updated`. The Bluetooth path logs `source=heymelody_waiting`.
6. Close the popup and wait for more reports: it must stay closed. Disconnect fully and reconnect: a Bluetooth connection event opens the next popup. Repeat with another headset to check isolation, and rapid disconnect/reconnect to check delayed opening cancellation.

## Behavior and limits

This observes `DeviceInfo.setBatteryInfo(List)` after HeyMelody processes the report. It reads **the current input list**, rather than a merged cache, and forwards type 1/2/3 (L/R/case) and charging state. HeyMelody rounds some values; output may therefore match its display rather than raw packets. Missing, invalid or ambiguous duplicate parts are `--`. Vendor zero is treated as unknown because this pipeline also uses zero for unavailable parts. There is no proven live source measurement timestamp and no in-case detection.

No extra Bluetooth connection, pairing, query or command is initiated. HeyMelody must itself receive data. A stopped/frozen HeyMelody or absent reports leaves battery fields unknown, but no longer suppresses the Bluetooth popup. Other HeyMelody versions are refused, with a diagnostic. Opening HeyMelody mid-connection can update an open popup but cannot open a new one.

Vendor reports only update a matching open vendor-mode popup, never open one. A bounded per-device delivery cache in the vivo process holds reports for at most 10 seconds since local observation so reports arriving just before the Bluetooth request can be used. This is not a measurement-age guarantee. Out-of-order/expired/future reports are rejected. ACL disconnect clears that device's cache and closes its vendor popup; pending Bluetooth requests are cancelled. Aggregate battery broadcasts do not erase vendor data. Existing popup timeout remains unchanged. All-unknown reports are ignored and an open popup retains its last values until normal timeout or a usable report.

## Return to default

```sh
settings delete global twspopup_battery_source
```

Reconnect to restore the legacy aggregate source (duplicates one percentage into L/R; case unknown). In heymelody mode without the HeyMelody hook, Bluetooth can still open the popup with unknown fields.
