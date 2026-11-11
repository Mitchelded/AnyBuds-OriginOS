# Known issues and limitations

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

Record user-reported problems with APK hash, exact OS/vivo/earbud versions, reproduction and expected/actual behavior. No user device results received yet.
