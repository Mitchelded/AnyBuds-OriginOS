# Test report — 2026-11-11

## Current 1.4-dev / code 11

| Check | Result |
| --- | --- |
| Origin comparison | Main 2cd100c and development c418bb1 at review start. 1.3-dev changes were local |
| Preservation | Backup .backups/20261111-182651 and baseline commit 34277e9 |
| Incoming baseline | 1.3-dev build and 19 existing tests passed |
| Vendor API evidence | Supplied APK SHA256 matched, four hooked/reflected declarations verified from DEX |
| Unit tests | 31 passed, 0 failures/errors/skips across metadata, vendor report/cache/event and popup lifecycle suites |
| Lint | Passed with 0 errors and 4 inherited warnings |
| Local debug build | Passed, artifacts/AnyBuds-OriginOS-1.4-dev-debug.apk |
| Local APK SHA256 | c874e0297dd7005eb83f95121b3a4386af670ab2aa436653bb17677755fdc0a1 |
| APK metadata/signature | aapt confirms 1.4-dev/code 11, min 29/target 34. Debug signing identity is retained |
| Hosted CI | Run 43051250437 passed for f6fe28f. Downloaded XML confirms all 31 tests with 0 failures/errors/skips |
| CI artifact | APK 13870295371 and reports 13869820485 downloaded and inspected |
| CI APK SHA256 | b4563e12dc6e5ea5ce29901807acaf69ae53e8825d80a9c0f62977d1beb896a6 |
| CI APK metadata/signature | Version 1.4-dev/code 11/min 29/target 34, v2 signature and existing certificate verified |
| Device acceptance | Pending H01–H06. Earlier user feedback is preserved in docs/TEST_REPORT.md |

The new regression cases cover exclusion of aggregate events, identity/age checks, reports queued before a disconnect, delayed disconnects after newer connections, invalid observations, all-unknown reports and cancellation while the vivo session is still pending.

Hosted evidence is https://github.com/Mitchelded/AnyBuds-OriginOS/actions/runs/43051250437. The CI APK is preserved locally as `artifacts/ci-43051250437-AnyBuds-OriginOS-1.4-dev-debug.apk`. The pre-review backup passed independent checksum and ZIP integrity verification.

## Earlier infrastructure and 1.1-dev evidence

| Check | Status / evidence |
| --- | --- |
| Local/remote main comparison | Passed: live remote head = local initial `2cd100c` |
| Backup | Created with SHA256 checksums in private `.backups/20261111-082241` |
| Wrapper integrity | JAR SHA256 `cb0da6751c2b753a16ac168bb354870ebb1e162e9083f116729cec9c781156b8`, checked against Gradle distribution service |
| Toolchain | `gradlew.bat --version`: Gradle 8.7, Java 17.0.18 |
| Actual Gradle tasks | `:app:tasks --all` passed; assembleDebug, assembleRelease, testDebugUnitTest, lintDebug, clean exist |
| Debug build | Passed, including `scripts/clean.ps1` then clean `scripts/build.ps1`, Gradle 8.7 / JDK 17 / SDK 34 revision 3 |
| Unit tests | Infrastructure milestone had NO-SOURCE. Metadata provider increment has 8 tests, 0 failures, 0 errors, 0 skipped |
| Android lint | Passed, 0 errors and 4 existing warnings. Bluetooth-host permission context documented on nameOf with method-level annotation |
| Script/workflow validation | All five PowerShell scripts parsed successfully. Workflow YAML parsed. Hosted infrastructure workflow passed |
| APK metadata/signature | Package io.github.mitchelded.twspopup, current version 1.1-dev/code 8, baseline 1.0/code 7, min SDK 29, target 34. Both apksigner checks verify v2, certificate SHA256 75e1b49872afa6020fc61e564645f4b1d567746bb13ea07551609ffed9383fe4 |
| GitHub Actions | Infrastructure run 42968355430 and provider run 42969045913 passed. Both APK artifact downloads passed checksum/signature checks. Provider reports XML confirms 8 tests, 0 failures/errors/skips |
| Signed release | Not built; external release credentials not configured |
| Missing signing configuration | Release script correctly failed with exit code 1 before invoking Gradle |
| iQOO / OriginOS | Not tested; exclusively performed by user |
| Vendor battery accuracy | Not tested; source APKs/protocols unavailable |
| Custom themes/media/animations | Not implemented, not tested |
| 1.1-dev provider APK | Compiled locally, version code 8, SHA256 2fea300791e587f312a8dee3b79e822210edcb21e3db137b12d9aa7657d02c75 |
| CI 1.1-dev APK | Downloaded artifact 13832282188, package/version/SDK and v2 signature verified. SHA256 b8b3e48fcf02cb2c3ed2980ec10899ea3715c62d595ab80424867e1ac502762e |

No APK has been installed and no phone or emulator has been accessed. Compilation and static analysis cannot verify Xposed hooks, vendor reflection, runtime permissions, battery freshness or UI behavior.

Baseline local APK is `artifacts/AnyBuds-OriginOS-1.0-debug.apk`, SHA256 `0cb457626fd06f65c23648c58c1849f71ccde4f44a9f3ba1cd5fd4cd30632acf`. Lint warnings are two PrivateApi notices in VivoApi, DataExtractionRules and MissingApplicationIcon. Local Gradle also warns that optional platform-tools are absent. These diagnostics remain visible.

Current provider APK is `artifacts/AnyBuds-OriginOS-1.1-dev-debug.apk`. Eight MetadataBatteryProviderTest cases verify separate values/charging, unknown preservation with aggregate available, partial failures, strict malformed/range validation, boundary values, nullable charging, unavailable API and isolation across devices/reads. These JVM checks exercise parsing/model behavior. Bluetooth reflection, metadata population, cache age and vendor UI comparison await user tests.

Hosted provider evidence is available at https://github.com/Mitchelded/AnyBuds-OriginOS/actions/runs/42969045913. Commit `f6d2a5d` produced APK artifact `13832282188` and reports artifact `13832745845`. The downloaded CI APK is preserved under `artifacts/ci-42969045913-AnyBuds-OriginOS-1.1-dev-debug.apk`.
