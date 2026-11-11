# Development plan

## 1. Audit and preservation

- [x] Review complete tracked implementation, build configuration, Git history and remote heads.
- [x] Preserve history and local working files in a private checksummed backup.
- [x] Create feature branch and technical assessment.

## 2. Reproducible development baseline (current increment)

- [x] Add official Gradle 8.7 wrapper with verified JAR and distribution checksums.
- [x] Add PowerShell build/test/clean scripts, named APK export, checksums and logs.
- [x] Add CI for development pushes, PRs and manual runs; upload APKs, tests, lint and logs.
- [x] Gate optional signed release build on complete external signing configuration.
- [x] Complete clean local build, unit-test task and lint, inspect actual APK metadata/signature. Unit task has NO-SOURCE at this milestone.
- [x] Review and commit, push feature branch and verify GitHub Actions artifact. Successful run 42968355430, downloaded APK and verified SHA256/signature.
- [ ] User tests legacy baseline on iQOO; record actual device/OS/vivo version.

## 3. Battery acquisition

Current review preserves the local 1.3-dev HeyMelody observer and advances it to 1.4-dev. The Enco Air2i metadata comparison failed to supply per-part readings. The supplied HeyMelody APK was traced and its hook signatures independently checked in DEX. Version 1.4-dev adds event-provenance filtering, pending-open cancellation and disconnect cutoffs. All 31 JVM tests and lint/build passed locally and in hosted run 43051250437. APK/test-report artifacts were downloaded and verified. User acceptance of H01–H06 remains pending.

The first optional Android metadata provider is implemented in `1.1-dev`, commit `f6d2a5d`. Eight pure JVM tests, lint and builds passed locally and in hosted run 42969045913. The downloaded hosted APK and test reports were inspected. User acceptance, vendor APK tracing, freshness/listener lifecycle and case-presence acquisition remain open.

1. Obtain exact target earbud model plus existing HeyMelody/vivo TWS APK paths and hashes; inspect offline without phone access.
2. Trace vivo bean values back through BLE advertisements, Bluetooth metadata, Binder and native code. Trace HeyMelody protocol/transport separately. Record concrete class/method names, version and evidence.
3. Introduce immutable battery snapshots with L/R/case, aggregate fallback, source, observation time, unknown tri-state charging and in-case values. Never turn aggregate data into measured per-bud values.
4. Prefer verified Android metadata/API paths; capability-check hidden APIs and isolate errors. Add vendor providers only after tracing real protocols.
5. Test parsing, unknown/out-of-range values, incomplete one-bud reports, freshness, device isolation and disconnect lifecycle.
6. Build, check, commit, push; provide APK and wait for user comparison with the vendor app.

## 4. Custom popup and themes

1. Keep legacy default, make custom mode explicitly selectable. Decide module-app overlay/service architecture with Android background/permission constraints documented before implementation.
2. Versioned manifest with logical canvas, state selectors, separate background/buds/case/decor/battery/text layers; transforms, z-order, visibility, asset overrides.
3. Asset adapters: Android PNG/WebP and animated image decode first; video with explicit lifecycle and decoder fallback; investigate APNG and Lottie before adding dependencies. Unsupported formats must show actionable errors.
4. Event dispatcher for open/close/connect/disconnect, case/charging/low-battery/level transitions; cancellable fade/scale/rotation/translation/keyframes and bounded loops.
5. ZIP import in private staging with canonical-path checks, entry/size/dimension limits, duplicate checks, full manifest validation and atomic install. Export only validated theme files. Add security tests before enabling imports.
6. Practical selection, preview, asset replacement, layer/animation editing and ZIP import/export UI using Android document access. Ship an original example theme and format specification.
7. Build/check/commit/push each logical feature; user verifies OriginOS behavior and animation/media lifecycle on their device.

## 5. Acceptance

All manual scenarios in `docs/MANUAL_TESTS.md` must carry user results, version and APK hash. No compatibility or battery accuracy claims based only on compilation. Release publication remains out of scope without authorization.
