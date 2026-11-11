# HeyMelody source — 1.4-dev (code 11)

Experimental integration for the exact supplied HeyMelody 117.6.6 (com.heytap.headset, code 117006006). Local tests/build do not establish device compatibility.

## Enable manually on the phone

1. Install `artifacts/AnyBuds-OriginOS-1.4-dev-debug.apk` as an update.
2. Enable all three module scopes: `com.android.vivo.tws.vivotws`, **`com.android.bluetooth`** and **`com.heytap.headset`**. Bluetooth owns automatic opening; HeyMelody provides separate battery reports.
3. In the user's root terminal:

```sh
settings put global twspopup_auto Enco
settings put global twspopup_battery_source heymelody
```

4. Reboot once to load the new hooks. Open HeyMelody and keep it running. Reconnect the earbuds. Do not use manual SHOW: it does not acquire battery data.
5. A matching Bluetooth connection should open a popup, initially showing `--` for unavailable parts. Compare with HeyMelody using both buds, left alone, right alone and the case when available. Capture `logcat -d -s TwsPopup`. The hook should log `module=1.4-dev HeyMelody process=...`, `HeyMelody observer installed` and `HeyMelody battery callback received`. Accepted part events then log `source=heymelody_observed` with `eventId=1048588` or `1048589`, followed by `HeyMelody report received` and `shown`/`updated`. The Bluetooth path logs `source=heymelody_waiting`. Generic aggregate callbacks log `eventId=1048590` as ignored.
6. Close the popup and wait for more reports: it must stay closed. Disconnect fully and reconnect: a Bluetooth connection event opens the next popup. Repeat with another headset to check isolation, and rapid disconnect/reconnect to check delayed opening cancellation.

## Behavior and limits

The observer copies the input to `DeviceInfo.setBatteryInfo(List)` after vendor rounding. It forwards that copy only when the subsequent `BluetoothReceiveData(int, Parcelable)` constructor has the same DeviceInfo object and event ID 1048588 or 1048589 on the same thread. Event 1048590 carries aggregate data and is excluded. The supplied APK declares all hooked signatures, verified directly from DEX. Missing, invalid or duplicate parts display `--`. Vendor zero represents unknown in this integration. Source measurement time and in-case state remain unavailable.

No extra Bluetooth connection, pairing, query or command is initiated. HeyMelody must itself receive data. A stopped/frozen HeyMelody or absent reports leaves battery fields unknown, but no longer suppresses the Bluetooth popup. Other HeyMelody versions are refused, with a diagnostic. Opening HeyMelody mid-connection can update an open popup but cannot open a new one.

Vendor reports update a matching open vendor popup. The vivo cache holds observations for up to 10 seconds, with at most 16 device records. These observations may originate from vendor cached preferences. Their source measurement age remains unknown. A report can arrive before HeyMelody marks its device connected and still enter this delivery flow. Bluetooth events control whether a popup opens.

ACL disconnect records a monotonic cutoff, clears older cache values, closes its open vendor popup and cancels its pending open in both the Bluetooth delay and vivo rebind delay. Queued reports observed at or before that cutoff are rejected. A late disconnect preserves a newer connection's pending/open popup and newer cached observation. The cache is read again immediately before rendering, so reports received during rebinding can be used and expired entries are discarded. All-unknown reports leave existing usable data available until expiry or popup closure.

## Regression scenarios for 1.4-dev

| ID | User action | Expected observation |
| --- | --- | --- |
| H01 | Allow a normal aggregate battery event | Event 1048590 is ignored by the vendor observer, without assigning its value to left |
| H02 | Open HeyMelody and reconnect, with both buds then each alone | Event 1048588/1048589 can update independent parts, missing parts show `--` |
| H03 | Disconnect promptly during automatic opening, then reconnect | The disconnected attempt stays cancelled, and the next connection can open normally |
| H04 | Close popup and wait for further vendor reports | The popup stays closed |
| H05 | Keep a second headset nearby and alternate connections | Only the selected device's matching popup receives its report |
| H06 | Trigger a genuine vivo popup after a module popup, then disconnect the module device | Automatic close checks popup ownership and leaves the superseding genuine popup visible |

All H01–H06 device results remain pending. Host-side regression tests cover event identity/type filtering, disconnect ordering, cache expiry and pending-open generation tokens. Automatic close also checks the manager's current data identity before closing a module session.

## Return to default

```sh
settings delete global twspopup_battery_source
```

Reconnect to restore the legacy aggregate source (duplicates one percentage into L/R; case unknown). In heymelody mode without the HeyMelody hook, Bluetooth can still open the popup with unknown fields.
