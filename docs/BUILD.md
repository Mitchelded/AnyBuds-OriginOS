# Build and CI

## Requirements

JDK 17, Android SDK platform 34 + build-tools 34.0.0, internet for the first dependency download. Gradle 8.7 is pinned by the official wrapper, including distribution SHA256 verification. AGP remains 8.5.2 ([compatibility requirements](https://developer.android.com/build/releases/agp-8-5-0-release-notes)).

Set `JAVA_HOME` and `ANDROID_HOME` in your own environment, or point `sdk.dir` in untracked `local.properties` at the SDK (forward slashes work on Windows). Scripts do not modify machine/user environment variables. Android Studio's SDK manager can prepare these packages. No device connection is required.

## PowerShell (Windows PowerShell 5.1 or PowerShell 7)

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/clean.ps1
```

Use `pwsh -File` on Linux/macOS. These invoke only `:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:lintDebug`, and `:app:clean`. Never install or run device tests. Exit 0 means success, 1 means failure. Gradle stdout/stderr is retained in `artifacts/*.log`. Clean removes Gradle module outputs and preserves exported APKs/logs.

Build reads actual `app/build/outputs/apk/debug/output-metadata.json` and copies its single APK to `artifacts/AnyBuds-OriginOS-<versionName>-debug.apk`, with adjacent `.sha256`. Raw output is `app/build/outputs/apk/debug/app-debug.apk`. No APK is committed. Unit/lint HTML/XML reports are under `app/build/reports/` and `app/build/test-results/`. Unit task NO-SOURCE means there were no tests.

## Release signing (optional)

Use your own private release keystore outside the repository. Set all four environment variables: `ANYBUDS_SIGNING_STORE_FILE` (absolute path), `ANYBUDS_SIGNING_STORE_PASSWORD`, `ANYBUDS_SIGNING_KEY_ALIAS`, `ANYBUDS_SIGNING_KEY_PASSWORD`. Run `scripts/build.ps1 -Configuration Release`. A partial configuration fails immediately. The script refuses release builds with no signing configuration; direct Gradle `assembleRelease` without it may produce an unsigned APK, which must not be distributed as signed.

The pre-existing tracked `app/debug.keystore` continues to sign debug builds. It is not a release identity. If a prior installed APK has a different certificate, Android refuses an upgrade; the user decides whether to uninstall after preserving needed settings.

## GitHub Actions

`.github/workflows/android-build.yml` runs on feature/fix/chore/develop/development/main pushes, PRs and manual dispatch. Java 17, SDK 34, wrapper build, Gradle caching, debug APK, unit tests and lint. PR cache writes are disabled. Reports/logs are uploaded even on failure; APKs and SHA256 files are uploaded only after successful checks. There is no install/deploy/release step.

Download `AnyBuds-OriginOS-<commit SHA>` from the workflow run's Artifacts section. Reports are in `android-reports-<commit SHA>`.

Manual `build_release=true` additionally requires GitHub repository secrets `ANYBUDS_KEYSTORE_BASE64`, `ANYBUDS_SIGNING_STORE_PASSWORD`, `ANYBUDS_SIGNING_KEY_ALIAS`, `ANYBUDS_SIGNING_KEY_PASSWORD`. Missing secrets fail explicitly. The keystore is decoded into runner temporary storage and removed afterward. Do not print secret values or commit keystores.

## Backup and Git

`scripts/backup.ps1` saves all Git refs plus working source/local settings in ignored `.backups/<timestamp>/`; includes private files and must not be published. Generated outputs/tool caches are excluded. Before code edits: backup, feature branch, inspect status/diffs. Before commit: checks, review exact staged list/diff, commit logical changes, push without force. Do not publish GitHub releases.
