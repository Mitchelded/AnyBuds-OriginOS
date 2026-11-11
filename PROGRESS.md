# Progress — 2026-11-11

## Latest correction: 1.3-dev (code 10)

User reports 1.2-dev produces no popup. Logs requested; missing vendor-report cause remains unconfirmed. Code inspection found that 1.2-dev disabled Bluetooth-owned popup opening in heymelody mode. 1.3-dev restores connection-owned opening with unknown fields, while HeyMelody reports only update open matching vendor popups. Added a bounded 10-second delivery cache for reports arriving before opening, expiry/order checks, ACL-disconnect cache clearing and pending-opening cancellation. Added hook/callback/filter/receipt diagnostics. All three scopes are now required for this mode. See `docs/HEYMELODY_TEST.md`.

Local validation: 19 tests passed, zero failures/errors; lint passed with four existing warnings; debug build passed. APK `artifacts/AnyBuds-OriginOS-1.3-dev-debug.apk`, SHA256 `7b3811209a13813077983830b1476ce7425930349d24891f44045604dd1c0715`. Device popup and separate battery delivery remain pending validation. No claim that the missing HeyMelody delivery has been diagnosed. Changes remain local.

## Latest increment: 1.2-dev (code 9)

Implemented optional `twspopup_battery_source=heymelody` observer for the user-supplied HeyMelody 117.6.6/code 117006006. Requires additional scope `com.heytap.headset`. Reads separate values and charging from current vendor reports without opening Bluetooth sockets; version-checks, validates parts, filters the selected device, and prevents subsequent reports from reopening closed popups. Full vendor disconnect resets bookkeeping. Aggregate/metadata automatic requests are suppressed in this mode. No measurement freshness or case-presence claim. See `docs/HEYMELODY_TEST.md`.

Final local checks: 16 JVM tests passed (0 failures/errors/skips); lint passed with the same four existing warnings. Debug build succeeded. APK `artifacts/AnyBuds-OriginOS-1.2-dev-debug.apk`, SHA256 `35dee9d764bab760bd55c0b7d765f632cab91416ba03118bff5daf26faa75698`. aapt confirms code 9/version 1.2-dev/min 29/target 34; apksigner verifies v2 signature. Device validation is still pending. Changes are local; this increment has not been pushed or verified in hosted CI.

Latest user feedback after reboot confirms metadata provider execution: L/R/case all unknown, aggregate 100, failedReads=0. Popup correctly renders the supplied unknowns as `--`; metadata does not supply usable separate battery data for this tested Enco Air2i setup. Earlier aggregate popup showed 90/90/case unknown against vendor 90/100/case 100. Battery accuracy acceptance failed. See `docs/TEST_REPORT.md`. Other manual scenarios remain unconfirmed.

Source setting `metadata` and installed version 1.1-dev/code 8 are confirmed. Before reboot logs lacked metadata snapshots; after reboot snapshots appeared, consistent with previously stale loaded code. User-supplied HeyMelody 117.6.6 was inspected offline; per-part vendor battery parsing and multiple transport paths were identified in `docs/HEYMELODY_ANALYSIS.md`. Vendor integration remains unimplemented and is needed for further progress on separate battery readings.

## Current status

Audit and build infrastructure are verified locally and in GitHub Actions. Optional Android metadata battery provider is implemented in version `1.1-dev` / code 8 and pushed as commit `f6d2a5d`. Local and hosted builds, lint and all 8 tests passed. CI APK checksum, package/version/SDK and signature were inspected. Current increment awaits the user's manual device feedback.

Documentation commit `9c6a233`, infrastructure commit `67a7891` and CI correction `1f01e90` are pushed. Hosted run `42968355430` succeeded. Its APK and reports artifacts exist. Downloaded baseline APK SHA256 is `f098a36768e97c546a83a2c1b7c2556665e8bb336f5ad2d83ec0d35016af54d3`, signature verified with the existing certificate. First failed run `42968041422` requested removed SDK package `tools`, corrected through explicit packages and current Actions on Ubuntu 24.04.

## Completed

- Live remote main matches initial local `2cd100c`; no tracked user edits. Preserved untracked `.claude` settings.
- Private backup `.backups/20261111-082241/` (Git bundle, working ZIP, status and checksums).
- Branch `feature/anybuds-development` created.
- Read complete implementation and documented architecture/gaps in `docs/TECHNICAL_AUDIT.md`.
- Installed official checksum-verified Gradle wrapper 8.7. Verified Java 17.0.18 and actual Gradle tasks.
- Added scripts, CI, signing configuration, development and testing documentation.
- Clean, rebuild and lint passed. Unit task returned NO-SOURCE. APK package/version/SDK and v2 signature inspected.
- Local baseline APK SHA256 is `0cb457626fd06f65c23648c58c1849f71ccde4f44a9f3ba1cd5fd4cd30632acf`.
- Two MissingPermission lint errors addressed only on Bluetooth-host name lookup using a documented method annotation. Four existing lint warnings remain visible.
- Added immutable snapshots, provider interface, independent AOSP metadata decoding, runtime getter resolution and optional BtWatcher integration through `twspopup_battery_source=metadata`.
- Provider tests cover partial/empty/invalid data, unknown charging, API errors and device isolation. All 8 passed. Local 1.1-dev APK SHA256 is `2fea300791e587f312a8dee3b79e822210edcb21e3db137b12d9aa7657d02c75`.
- Provider CI run https://github.com/Mitchelded/AnyBuds-OriginOS/actions/runs/42969045913 succeeded. Downloaded XML confirms 8 tests with 0 failures/errors/skips. APK artifact ID is `13832282188`, reports ID is `13832745845`.
- Downloaded CI 1.1-dev APK is `artifacts/ci-42969045913-AnyBuds-OriginOS-1.1-dev-debug.apk`, SHA256 `b8b3e48fcf02cb2c3ed2980ec10899ea3715c62d595ab80424867e1ac502762e`. Certificate matches baseline.
- Latest pre-provider backup `.backups/20261111-084831/` passed bundle verification, checksums and ZIP integrity. Local settings and original debug keystore entries are present.

## Important findings

- Existing popup is entirely vivo-rendered. Existing L/R/case display setters do not acquire measurements.
- Default auto source duplicates aggregate Bluetooth battery into L/R. Optional metadata source preserves unknown parts and reads separate cached values. Updates remain limited to existing events within 20 seconds after connection.
- Vivo compatibility comments name 9.2.41.2 but source APK is absent. No HeyMelody APK is present in the project.
- No custom renderer, editor, ZIP theme system or general event animation engine exists.
- Existing `app/debug.keystore` is tracked upstream; preserved for debug update compatibility. No new key material staged.
- `gh` unavailable; use Git for synchronization and GitHub REST for CI inspection.
- Earlier sandbox runs redirected Java home and required elevated cache access. Current session has full filesystem/network access. No machine-wide environment changes were made.
- Java is `C:/Users/trety/miniconda3/Library/lib/jvm`. SDK 34 revision 3 and build tools 34.0.0 are verified in ignored `.tools/android-sdk`, selected through ignored `local.properties`. Optional platform-tools are absent locally and Gradle logs a warning. APK/lint tasks pass.
- GitHub Schannel TLS failed after the environment update. Per-command `git -c http.sslBackend=openssl` works, with certificate verification enabled. GitHub API uses existing Git Credential Manager authentication in memory after an anonymous rate limit.
- claude-mem observation storage is unavailable due to provider refusal. Repository documents carry the resumable state.
- Git author configuration was absent. Commits use the author identity from the existing repository history through per-command configuration. Global Git settings remain untouched.

## Remaining and exact next steps

1. Await user testing of 1.1-dev using `docs/MANUAL_TESTS.md`. Use the baseline/default source first, then metadata mode and each single bud. Enable with `settings put global twspopup_battery_source metadata` in the user's terminal, reconnect, compare with the vendor app and record diagnostics. Clear with `settings delete global twspopup_battery_source`.
2. Record the exact earbud model, OriginOS/vivo versions, APK hash and actual pass/fail results. Vendor APK paths remain requested from the user. No device acceptance has been recorded.
3. Trace HeyMelody/vivo protocols from actual supplied APKs, improve disconnect lifecycle and metadata updates/freshness using confirmed findings.
4. Implement isolated custom theme system in `DEVELOPMENT_PLAN.md` order, carrying forward actual device findings.
