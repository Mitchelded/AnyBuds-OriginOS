# AnyBuds-OriginOS development guide

Read `PROGRESS.md` first to resume. User instructions and `C:/Users/trety/.codex/RTK.md` apply; prefix shell commands with `rtk` (`rtk proxy` works when its filtered subcommands cannot find Claude configuration).

- Android Java/Xposed project, module `app`, package `io.github.mitchelded.twspopup`.
- Preserve legacy vivo popup. Host package `com.android.vivo.tws.vivotws`; optional Bluetooth hook `com.android.bluetooth`.
- AGP 8.5.2, Gradle wrapper 8.7, Java 17, SDK/build tools 34. Min Android 10/API 29.
- Work on `feature/anybuds-development`, never directly on main. Review diffs, run appropriate checks, commit logical increments, push without force. No releases.
- Make a private backup with `scripts/backup.ps1` before changing existing code. Do not stage `.backups`, `.claude`, `.tools`, credentials, APKs, or new signing keys.
- Existing tracked debug keystore is legacy material. Preserve its identity for debug updates; never use it for releases or publish additional key material.
- Build: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build.ps1`.
- Check: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test.ps1`.
- Clean module outputs: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/clean.ps1`.
- No ADB, phone connection, installation, emulator, or automated device tests. User performs all device checks.
- Separate implemented / compiled / automated / user confirmed / untested in all reports. Reflection compiling does not verify the target vendor API.
- Maintain `DEVELOPMENT_PLAN.md`, `PROGRESS.md`, `TEST_REPORT.md`, `KNOWN_ISSUES.md`, and `docs/MANUAL_TESTS.md`.

Architecture and inherited assumptions: `docs/TECHNICAL_AUDIT.md`. Build setup and CI: `docs/BUILD.md`.
