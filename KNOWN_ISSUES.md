# Known issues and limitations

## Current HeyMelody increment

- Version 1.4-dev supports the supplied HeyMelody 117.6.6/code 117006006. User reports correct percentage display while HeyMelody runs, and all unknowns when closed/unloaded. The observer depends on that process executing vendor callbacks. Independent acquisition is not implemented. Exact process state, measurement freshness and remaining manual scenarios are unverified.
- Enco Air2i metadata mode was exercised by the user and produced unknown L/R/case despite aggregate 100. Device feedback is in `docs/TEST_REPORT.md`.
- HeyMelody's setter receives both aggregate and per-part data. The observer now requires its matching per-part event 1048588/1048589. Runtime pairing across the hooked calls remains a manual acceptance item.
- Event 1048589 can use vendor cached preferences. Local observation age is bounded to 10 seconds, while source measurement age remains unknown. Case presence remains unavailable.
- Four inherited lint warnings remain. The complete custom theme/editor system remains planned in DEVELOPMENT_PLAN.md.

## Existing application

- Vivo internal API assumptions are inherited from comments naming vivo TWS 9.2.41.2; no target APK or user runtime confirmation in this session. Other versions may fail reflection.
- Default aggregate battery mode retains its duplicated L/R presentation and unknown case. The optional metadata mode in 1.1-dev reads independent cached L/R/case and nullable charging. Availability and accuracy require user confirmation. Broadcast demo defaults remain synthetic (80/80/100).
- Auto updates only within 20 seconds after connection; no measurement cadence/freshness guarantee. Pending connection popup can race a disconnect. These need lifecycle work in the battery increment.
- Metadata-only changes do not trigger reads yet. Cached values may survive disconnects, and their source measurement time is unavailable. One connected bud does not establish freshness or presence of the other part.
- Case-presence acquisition remains unimplemented. The legacy renderer retains its inherited in-case visual default of 3. Unknown charging has no distinct visual state in that renderer, though the snapshot and diagnostic known-mask preserve it.
- Connect button animates a visual connection; it does not pair Bluetooth devices.
- Module commands use an exported host receiver; do not treat broadcast extras as trusted measured telemetry.
- Popup commands have no timeout upper limit; theme/media/import paths are not yet implemented.
- Custom mode, theme editing/import/export, independent layers and event animation configuration remain planned work.

## Development

- A public debug signing key already exists in repository history. Preserve debug compatibility and use a separate private identity for releases. No history rewriting performed.
- No unit test sources initially. A successful `testDebugUnitTest` with NO-SOURCE is not an automated behavior test.
- Lint passes with four inherited warnings covering vendor reflection, backup rules and missing application icon. Optional local platform-tools are absent and produce a Gradle warning.
- No signed release credentials are configured. Release builds are opt-in and must use external secrets; no release publication is configured.
- `gh` absent locally. Git and GitHub API are the synchronization/CI inspection fallback.
- Earlier restricted runs required elevated Gradle cache/network access. The current session has full access. SDK/cache files stay ignored.

Record user-reported problems with APK hash, exact OS/vivo/earbud versions, reproduction and expected/actual behavior. Preserve the device feedback in `docs/TEST_REPORT.md` separately from build evidence in the root TEST_REPORT.md.
