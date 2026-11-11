# Test report — 2026-11-11

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
| GitHub Actions | Infrastructure run 42968355430 passed after correction of removed SDK tools package. APK and reports artifacts confirmed, downloaded APK checksum/signature verified. Provider increment hosted checks pending |
| Signed release | Not built; external release credentials not configured |
| Missing signing configuration | Release script correctly failed with exit code 1 before invoking Gradle |
| iQOO / OriginOS | Not tested; exclusively performed by user |
| Vendor battery accuracy | Not tested; source APKs/protocols unavailable |
| Custom themes/media/animations | Not implemented, not tested |
| 1.1-dev provider APK | Compiled locally, version code 8, SHA256 2fea300791e587f312a8dee3b79e822210edcb21e3db137b12d9aa7657d02c75 |

No APK has been installed and no phone or emulator has been accessed. Compilation and static analysis cannot verify Xposed hooks, vendor reflection, runtime permissions, battery freshness or UI behavior.

Baseline local APK is `artifacts/AnyBuds-OriginOS-1.0-debug.apk`, SHA256 `0cb457626fd06f65c23648c58c1849f71ccde4f44a9f3ba1cd5fd4cd30632acf`. Lint warnings are two PrivateApi notices in VivoApi, DataExtractionRules and MissingApplicationIcon. Local Gradle also warns that optional platform-tools are absent. These diagnostics remain visible.

Current provider APK is `artifacts/AnyBuds-OriginOS-1.1-dev-debug.apk`. Eight MetadataBatteryProviderTest cases verify separate values/charging, unknown preservation with aggregate available, partial failures, strict malformed/range validation, boundary values, nullable charging, unavailable API and isolation across devices/reads. These JVM checks exercise parsing/model behavior. Bluetooth reflection, metadata population, cache age and vendor UI comparison await user tests.
