# Technical audit — 2026-11-11

## Repository and preservation

- Initial branch: `main`, commit `2cd100c606f5bdd254755c84a886450b77f169a8`.
- Live `git ls-remote --heads origin` returned the same main SHA; no other remote branches at audit time.
- No tracked local edits. `.claude/settings.local.json` was untracked and is preserved without publication.
- Private backup: `.backups/20261111-082241/` contains all Git history in `repository.bundle`, the working files including local settings and existing signing key in `working-tree.zip`, status, and SHA256 checksums. Never upload this directory.
- Development branch: `feature/anybuds-development`. No history rewrite or release authorized.

## Actual architecture

One Android application module, Java 17, AGP 8.5.2, compile/target SDK 34, min SDK 29, Xposed API 82 (`compileOnly`). No AndroidX, launcher activity, service, editor, asset pack, or tests initially.

| Component | Existing responsibility |
| --- | --- |
| Hook | Install hooks in vivo main process and optional Bluetooth process |
| VivoApi | Reflectively resolve vivo manager, bean, receiver, click callbacks |
| PopupController | Intercept module broadcasts; own popup session, rebind, timers, simulated connect animation |
| DeviceBean | Set independent display fields supplied by broadcasts; these are inputs, not battery acquisition |
| ModelCatalog | Read model and picture metadata from the host vivo APK |
| PopupRequest | Parse command extras; demo defaults 80/80/100, charging=0, in-case=3 |
| BtWatcher | On matching A2DP/HFP connection, delay 1.5 s, query hidden aggregate getBatteryLevel; duplicate into L/R, case unknown; follow-up only within 20 s; deduplicate 15 s |
| Props/AppLogMirror/L | Optional logging and diagnostics |

Reviewed every Java source, manifest/resource/asset, Gradle file, workflow, README, tracked file list, recent history, remote heads, and untracked file inventory. Local settings and signing-key contents are private and were not printed.

## Compatibility and gaps

The code says its reflective names came from vivo TWS 9.2.41.2. That APK is absent in this checkout, so this is an inherited compatibility claim, not independently verified evidence. `ld.r` is explicitly obfuscated. Manager fields and bean setters can break across firmware versions. Xposed hooks run with host-process privileges; module manifest permissions do not grant permissions to those hosts.

Legacy popup is the only renderer. The module does not control real pairing; its Connect button simulates visual state. No vendor battery protocol, charging/case detector, GATT client, custom mode, theme format, editor, import/export, or general animation engine exists. A single Android battery value cannot establish which bud it describes. Bluetooth broadcasts currently have no per-part provenance/freshness tracking, no disconnect cancellation for delayed requests, and no source authentication beyond the Android broadcast system.

## Initial build findings

- Existing CI uses Java 17 + globally provisioned Gradle 8.7 and only `assembleDebug`.
- No wrapper, ignore rules, tests, lint CI, PR trigger, PowerShell scripts, report uploads, or release-signing gate initially.
- AGP 8.5 requires Gradle 8.7 and JDK 17; preserve the plugin/SDK combination. Source: https://developer.android.com/build/releases/agp-8-5-0-release-notes
- Java 17.0.18 exists at the configured JAVA_HOME but is not on PATH. Cached Gradle 8.8 exists. Android SDK availability still being established.
- `gh` is absent from PATH and a user-directory search. Git transport is available after network escalation.
- `app/debug.keystore` is already tracked with a public debug password in Gradle. Preserve its signing identity for existing debug upgrades; do not add new keys or use it as release identity. Removing historical material requires a separate decision, not history rewriting here.

## Incremental design

First make the legacy build reproducible and produce a baseline APK. Next introduce source-tagged, timestamped battery snapshots with unknown values distinct from false/zero, backed by verified Android/vendor evidence and tests. Keep legacy rendering as the default; isolate custom rendering in the module app with explicit configuration/permission handling rather than adding media/editor dependencies to the Bluetooth process. Define and validate the bounded ZIP/manifest format before connecting imports to a renderer. Device confirmation is a separate acceptance gate for every behavioral change.
