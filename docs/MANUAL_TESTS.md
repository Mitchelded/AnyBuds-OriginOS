# Manual device tests

All device tests are performed personally by the user. Developer does not connect to the phone, install APKs, run ADB or automate device tests. Every result below is currently **NOT TESTED**.

## Record before testing

APK filename/SHA256, commit, iQOO model, Android and OriginOS versions, vivo TWS version, Xposed/Vector/LSPosed version, earbud model/firmware, HeyMelody version, enabled module scope. Record expected/actual behavior, pass/fail and any logs you choose to share. Remove MAC addresses/personal identifiers before sharing.

## Installation and rollback

1. Download the debug APK and its checksum from the supplied local artifacts or GitHub Actions artifact. Check SHA256 on the PC.
2. Transfer and install manually using the phone's file manager. Preserve prior APK/settings for rollback. Stop if Android reports a certificate mismatch; do not uninstall automatically.
3. Enable the module in your Xposed manager. Required scope: `com.android.vivo.tws.vivotws`; optional auto popup: `com.android.bluetooth`.
4. Restart the relevant app/phone using normal user controls so hooks reload. Capture any errors. Enable Bluetooth normally.
5. If regression occurs, disable the module, restart normally and restore the previous user-selected APK if desired.

## Legacy baseline (current acceptance)

Use existing commands in README from the user's own on-device terminal if desired; no developer-operated device commands.

| ID | Scenario | Expected result | Result |
| --- | --- | --- | --- |
| L01 | SHOW with model 32, left=60/right=90/box=40 | Separate supplied demo values displayed; these are not measured battery data | Not tested |
| L02 | SHOW state=connect, tap Connect | Existing connecting animation then connected card; no promise of actual pairing | Not tested |
| L03 | Close button, outside tap and natural timeout | Existing close behavior; no stuck card | Not tested |
| L04 | SHOW, dismiss, SHOW again several times | Popup can reopen without stale manager session | Not tested |
| L05 | Update same open device; send late update after close | Open card refreshes; late update does not reopen | Not tested |
| L06 | LIST models; try model 68 and missing artwork | Existing valid model visuals or clear diagnostic; host does not crash | Not tested |
| L07 | Normal genuine vivo earbud popup with module enabled | Original controls/timeout/animation remain functional | Not tested |

## Bluetooth and battery

| ID | Scenario | Check | Result |
| --- | --- | --- | --- |
| B01 | Matching device connects, HFP and A2DP close together | One popup, normal audio/calls; record battery | Not tested |
| B02 | Nonmatching device and auto filter disabled | No module auto popup; normal Bluetooth | Not tested |
| B03 | Rapid connect/disconnect, reconnect, Bluetooth off/on | No stale delayed popup, duplicate windows or Bluetooth crash; record existing races | Not tested |
| B04 | Both buds, left only, right only, each removed/returned to case | Compare real vendor app L/R/case vs module; mark unknowns and synthetic/aggregate values | Not tested |
| B05 | Case open/closed, cable charging on/off, bud charging | Current module does not acquire these states; do not accept demo flags as detection | Not tested |
| B06 | Watch readings for 5 minutes and around 20-second boundary | Record update timestamps/frequency; current automatic follow-up window is 20 s | Not tested |
| B07 | One/two different paired headsets | Values/windows must not be attributed to the other device | Not tested |
| B08 | Low/empty battery, unavailable case, stale cached data | Unknown distinct from zero/100%; baseline aggregate duplication documented | Not tested |

## Custom mode acceptance (planned; do not report as available)

After implementation: switch legacy/custom modes, preview/select themes, replace each asset, adjust each transform/z-order/visibility, edit animations, import/export/reimport ZIP and compare. Test transparent PNG/WebP, GIF, supported APNG, MP4/WebM and any supported Lottie with graceful rejection of unavailable formats. Check open/close/connect/disconnect/case/charging/low-battery/level event transitions, keyframes, loop cancellation, orientation, dark mode, display size, lock screen, repeated popups, audio playback and media cleanup.

Reject malformed/truncated/oversized ZIPs, traversal/absolute paths, duplicate entries, unsupported schema and missing assets; previous selected theme must remain usable. Verify rollback to legacy if custom permission/decoder/rendering fails. No custom behavior has been implemented or verified yet.

## Regression and feedback

Check normal Bluetooth settings, pairing done by the system, music, calls, reconnect after reboot and genuine vivo popup. Confirm no new host crashes/battery drain. Report scenario IDs, expected/actual output, version data and APK SHA256. Developer updates TEST_REPORT/KNOWN_ISSUES only from actual user feedback, then builds the next increment.
