# Device feedback — 2026-11-11

## OPPO Enco Air2i: separate battery readings unresolved

User reports different earbud levels but identical levels in the module popup, and no case battery level. Supplied screenshots show:

- Module popup: left 90%, right 90%, case `--`.
- Vendor app: left 90%, right 100%, case 100% (captured about one minute later).
- Terminal: `settings put global twspopup_battery_source metadata`, manual SHOW with explicit 60/90/40 values, and manual SHOW with `state connect`.

This is a failed battery accuracy comparison for B04 and a missing case reading relevant to B05. It does not establish whether the displayed popup was produced by the metadata provider. Installed APK/version, active Bluetooth hook, auto filter, reconnection after setting the source and TwsPopup logs are unconfirmed. Screenshots alone cannot establish measurement freshness or the active source.

Source inspection: metadata selection happens only in `BtWatcher.request` for automatic matching Bluetooth connection/battery events. Manual SHOW uses its supplied/default values and does not query metadata. The legacy automatic path duplicates aggregate battery into left/right and sets case to unknown. Thus the observed display is consistent with the legacy path, but logs are needed to distinguish that from cached equal metadata or rendering behavior.

Next diagnostic: verify source/filter settings, reconnect physically without manual SHOW, then collect `logcat -d -s TwsPopup`. Look for `source=android_metadata_cache`, independent L/R/case fields and the final `shown`/`updated` request. If the metadata provider supplies unknown or inaccurate values, investigate the exact vendor APK rather than infer a proprietary protocol from the screenshots.

## Follow-up supplied by user

Settings query returns `metadata`. Package query confirms versionCode 8 / versionName 1.1-dev. Automatic logs show `battery update`, `asked for a popup` and final `shown ... L=90 R=90 case=-1`; no metadata snapshot line appears. This rules out a simple missing setting or older installed package version. It does not establish which module code is already loaded in the Bluetooth process. A stale hook after APK replacement is a hypothesis; a normal phone reboot followed by reconnection and logs is the next check. Device addresses from supplied logs are intentionally omitted here.

User supplied HeyMelody 117.6.6 for offline analysis. See the accompanying HeyMelody analysis document for its hash and concrete parser findings.

## After phone reboot: metadata path confirmed, per-part data unavailable

User rebooted and reconnected. Logs at 09:45:17 and 09:45:18 now explicitly show `source=android_metadata_cache L=-1 R=-1 case=-1 aggregate=100 charging=0 chargingKnown=0 failedReads=0`. The final popup request also carries L/R/case=-1; the screenshot shows all three as `--`.

The updated provider is now executing. All six reads completed without caught exceptions, but yielded no usable per-part percentages or charging states. Logs do not distinguish null/empty/malformed metadata. This is not a failure to display supplied per-part values: the renderer received unknowns. The aggregate 100 remains available and is deliberately not substituted into both individual buds in metadata mode. No evidence of actual charging/power failure is present.

Conclusion: the metadata-only approach does not supply the required Enco Air2i data on this tested setup. Repeating the same metadata test is unnecessary. Use the legacy aggregate source temporarily if desired, with its documented duplicate-L/R limitation. A separate vendor-data integration is required for the requested accuracy; the supplied HeyMelody APK provides a concrete investigation basis. Integration and device validation remain open.

## 1.2-dev: no automatic window

User reports no popup after the HeyMelody integration. No log supplied with that report; requested TwsPopup logs after opening HeyMelody and reconnecting. Code inspection confirms 1.2-dev suppresses every Bluetooth request in heymelody mode, making all opening depend on a usable vendor callback. Actual cause of missing vendor delivery remains unconfirmed (hook loading, event timing/connection guard, filter or transport).

1.3-dev restores Bluetooth-owned opening with unknown fields while vendor reports update only matching open popups. A 10-second delivery cache handles reports arriving before opening. Added callback/filter/transport diagnostics and cancellation/cache clearing on ACL disconnect. Battery accuracy and on-device behavior remain pending user validation.

## Review continuation with 1.4-dev

Remote heads were unchanged at review time. The local 1.3-dev work was backed up and preserved in commit `34277e9`. Its build and existing 19 tests passed.

Source inspection found that HeyMelody's aggregate Android callback also reaches setBatteryInfo as type 1. Version 1.4-dev gates forwarding on the paired per-part BluetoothReceiveData event. It also cancels pending vivo opens on disconnect and keeps disconnect cutoffs to reject queued earlier observations. These findings explain concrete code paths. The user's actual missing-delivery cause remains unconfirmed until new logs arrive.

Local 1.4-dev validation passed with 31 JVM tests, 0 failures/errors/skips, lint with 0 errors and 4 existing warnings, and a debug build. User tests H01–H06 are pending. Final APK checksum is recorded in the root TEST_REPORT.md.

Hosted run 43051250437 also passed with all 31 tests confirmed in downloaded XML. APK checksum and v2 signature were checked. This supplies build evidence while user device acceptance remains pending.
