# AALyrics Release Policy

## Purpose

AALyrics is distributed outside Google Play. GitHub Releases are the durable distribution channel for signed Android APKs.

A release is a versioned, signed, installable distribution snapshot. It does **not** imply that the application is feature-complete. Early development builds may be published intentionally as GitHub Pre-releases so signing, installation, Android Auto discovery, update compatibility, and real-device behavior can be validated before the product is ready for a stable release.

## Distribution channels

### Development artifacts

Pull-request and `main` CI may upload debug APK workflow artifacts.

These artifacts are for development/testing only:

- they use the Android debug signing identity;
- they are not the durable user distribution package;
- they are not expected to update a release-signed installation.

When `AALYRICS_VERSION_NAME` is not injected, development builds derive Android
`versionName` from the latest reachable release tag and the current short Git commit.
For example, a clean development build after `v0.2.0-alpha.1` may report
`0.2.0-alpha.1-dev+89906f2`. A local build with tracked or untracked working-tree
changes appends `.dirty`. If Git metadata is unavailable, the build falls back to
`0.1.0-dev`.

Release builds are unaffected by this fallback: the Release workflow continues to
inject the tag-derived `AALYRICS_VERSION_NAME` explicitly.

### GitHub Pre-releases

Development milestones use semantic-version-style prerelease tags:

- `v0.1.0-alpha.1`
- `v0.1.0-beta.1`
- `v0.1.0-rc.1`

Any accepted release tag containing a prerelease suffix is published with GitHub's **Pre-release** flag.

The initial signed distribution, `v0.1.0-alpha.1`, has been published successfully as a GitHub Pre-release. It is intentionally functionally incomplete; its immediate purpose is to validate the durable signing/distribution path and provide an installable baseline.

### Stable releases

A tag with no prerelease suffix, such as `v0.1.0`, is published as a normal GitHub Release.

Stable tags are reserved for builds that are intentionally presented as generally usable AALyrics releases. Moving from an alpha/beta/RC build to stable is a product decision, not merely a CI milestone.

## Version tags

The Release workflow accepts only the stable tag form or one of the three defined prerelease channels:

```text
vMAJOR.MINOR.PATCH
vMAJOR.MINOR.PATCH-alpha.N
vMAJOR.MINOR.PATCH-beta.N
vMAJOR.MINOR.PATCH-rc.N
```

Examples:

```text
v0.1.0-alpha.1
v0.1.0-beta.1
v0.1.0-rc.1
v0.1.0
```

Release tags must point to commits contained in `main`.

Do not move or reuse a tag after a release has been published. If a published build is wrong, fix the problem and create a new version tag.

The tag without the leading `v` becomes Android `versionName`. The Release workflow run number is used as Android `versionCode`, providing increasing update ordering across this workflow's releases.

## Signing identity

All durable AALyrics release APKs must be signed by the same release key.

The release keystore must never be committed to Git. The repository ignores common keystore extensions, but that is only a guardrail; the canonical keystore should remain outside the repository and be backed up securely.

Required GitHub Actions repository secrets:

```text
AALYRICS_RELEASE_KEYSTORE_BASE64
AALYRICS_RELEASE_STORE_PASSWORD
AALYRICS_RELEASE_KEY_ALIAS
AALYRICS_RELEASE_KEY_PASSWORD
```

The existing PetitLyrics production credentials are also required by the Release workflow:

```text
PETITLYRICS_USER_ID
PETITLYRICS_APP_NAME
PETITLYRICS_PKG_NAME
PETITLYRICS_CLIENT_APP_ID
```

The workflow fails instead of publishing a release when required signing or provider credentials are missing.

### Keystore backup rule

Loss of the release private key or its passwords can prevent future APKs from updating existing release installations. Keep the original keystore and credentials backed up independently from GitHub Secrets.

Do not rotate the release key casually. A signing-key change requires an explicit migration decision and must not happen as an incidental CI/configuration change.

## Creating the keystore

Example with the JDK `keytool`:

```bash
keytool -genkeypair -v -keystore aalyrics-release.jks -storetype JKS -alias aalyrics -keyalg RSA -keysize 4096 -validity 10000
```

The X.509 subject does not need to contain a personal legal name. A project identity such as the following is sufficient:

```text
CN=AALyrics
OU=Development
O=AALyrics
L=Nagoya
ST=Aichi
C=JP
```

On Windows PowerShell, convert the keystore to the single-line Base64 value used by `AALYRICS_RELEASE_KEYSTORE_BASE64`:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("aalyrics-release.jks"))
```

## Publishing

After the intended commit is merged into `main`, ensure the release version is the newest entry in repository-root `CHANGELOG.md`, then create and push the version tag.

For the first signed development release:

```bash
git checkout main
git pull
git tag v0.1.0-alpha.1
git push origin v0.1.0-alpha.1
```

The Release workflow then:

1. verifies that the tagged commit belongs to `main`;
2. verifies required secrets and restores the release keystore in the ephemeral GitHub Actions runner;
3. validates the version tag and resolves `AALYRICS_VERSION_NAME`;
4. verifies that the newest version heading in `CHANGELOG.md` exactly matches the tag version without the leading `v`;
5. runs the architecture guard and unit tests;
6. builds the signed release APK, which bundles that same checked-in changelog;
7. publishes `AALyrics-vX.Y.Z[-suffix].apk`;
8. publishes a matching SHA-256 checksum;
9. marks prerelease-suffixed tags as GitHub Pre-releases.

A stable tag follows the same process but is published without the Pre-release flag. A tag/changelog mismatch is a release-blocking error: the workflow must fail before building or publishing the APK.

## Changelog policy

Repository-root `CHANGELOG.md` is the canonical **user-facing in-app release history**. It is intentionally distinct from the GitHub Release body:

- `CHANGELOG.md` is concise, curated, newest-first, and bundled into the APK for offline Settings display;
- the GitHub Release body is the distribution-page summary plus the durable auto-generated PR-level ledger;
- the two may summarize the same release at different levels of detail and are not expected to be byte-identical.

Every published version uses a heading in this form:

```md
## [0.2.0-alpha.1] - 2026-09-21
```

Before creating a release tag, add that release entry to `CHANGELOG.md` and merge it into `main`. The release tag must point to that revision (or a later `main` revision whose newest changelog version is still the tag being created).

The Release workflow removes the leading `v` from the tag and compares it with the first version heading in `CHANGELOG.md`. For example, `v0.2.0-alpha.1` requires the newest heading to identify `0.2.0-alpha.1`. This prevents publishing a signed APK whose bundled Changelog does not describe that release.

The workflow does **not** generate or write `CHANGELOG.md` after a tag is pushed. Doing so would be too late for the tagged APK: release-history content is source-controlled release input, while bundling and consistency validation are automatic.

At build time, the application Gradle configuration copies the checked-in file to generated assets as `aalyrics_changelog.md`. `:app` reads that bundled Markdown and passes it to the Phone Settings presentation layer. No GitHub API request is required to open Settings > Changelog.

## Release notes structure

GitHub's automatically generated release notes are useful as the durable change ledger, but they are not the primary product-facing summary for milestone releases.

The standard AALyrics Release body should therefore use this order:

1. **Curated milestone summary first** — explain what materially changed for users and testers, using product-level language rather than a commit-by-commit implementation log.
2. **Highlights / major areas** — group the important behavior into a small number of readable sections such as Phone UI, Lyrics, Playback, Android Auto, Translation, or other release-specific areas.
3. **Known limitations / still in development** — for alpha, beta, and RC releases, state intentionally incomplete or unavailable functionality clearly.
4. **Auto-generated GitHub notes last** — preserve the generated `## What's Changed` section and `Full Changelog` link at the bottom of the Release body.

The generated section should **not be deleted or replaced** when curating a Release. It remains the traceable PR-level record of what entered the version. Curated notes are added above it.

Recommended shape:

```md
## <Milestone summary>

<Short product-facing explanation of this release.>

### Highlights

- ...
- ...

### Still in development

- ...
- ...

---

## What's Changed
<GitHub auto-generated entries remain here>

**Full Changelog**: <GitHub-generated comparison link>
```

For milestone releases, the curated section should emphasize externally meaningful behavior and validation state. Avoid copying internal implementation details, temporary fix commits, or exhaustive PR history into the curated portion when the generated section already records them.

Pre-releases should identify themselves clearly as alpha/beta/RC and should not imply feature completeness. Stable releases may omit a limitations section when there are no material caveats, but the curated-summary-first / generated-notes-last structure still applies.

The current Release workflow intentionally continues to publish with `--generate-notes`. After the workflow has successfully created the signed GitHub Release, its body should be edited to add the curated summary **above** the generated notes while leaving the generated section at the bottom.

## Verifying a downloaded APK

Download both the APK and its `.sha256` file from the same GitHub Release. On a system with `sha256sum`:

```bash
sha256sum -c AALyrics-v0.1.0-alpha.1.apk.sha256
```

The checksum verifies file integrity. Android's package signature separately provides the signing identity used for update compatibility.

## In-app update entry

The Phone Settings surface exposes one combined version/update row. GitHub Releases is the authoritative source for update discovery, while the Phone Compose layer remains presentation-only.

### Manual and automatic update discovery

Manual discovery remains explicitly available through `Check for updates` and `Retry` regardless of the automatic-update preference or cadence.

Settings owns only the manual discovery presentation:

```text
Idle / Check for updates
Checking…
Up to date
Check failed / Retry
```

A manual query that finds a newer eligible release does not expose a Settings-row Download action. It immediately hands the result to the global Unified Update Dialog.

AALyrics also supports low-frequency automatic discovery. `Automatically check for updates` is an app-owned durable preference that defaults to ON. After normal Phone entry gates reach `READY`, the application may start an automatic release query only when the preference is enabled and the durable cadence is due. The cadence is **7 full days** from the last recorded check; no timestamp means the first automatic check is eligible immediately.

Starting an automatic check records the cadence timestamp before the network request so repeated app restarts do not hammer GitHub after a transient failure. A successful MANUAL query refreshes the same timestamp. AUTOMATIC completion does not shift its already-recorded start timestamp, and INSTALL_REFRESH does not alter discovery cadence.

Automatic non-update outcomes are intentionally silent in Phone UI. Automatic Checking, Up to date, and Failed do not appear in Settings.

Release-query results still carry an internal origin: `MANUAL`, `AUTOMATIC`, or `INSTALL_REFRESH`. Origin controls cadence and notification-suppression policy; it does not choose a second update flow.

Both user-visible newer-release results converge:

```text
MANUAL UpdateAvailable ───────┐
                              ├─> Unified Update Dialog
AUTOMATIC UpdateAvailable ────┘
```

The available-state dialog presents the discovered release with `Update`, `Not now`, close, and system-Back handling. Outside-tap dismissal is disabled.

Automatic same-session suppression remains scoped to automatic notification. Dismissing an automatically discovered release suppresses that exact automatic prompt for the current process session. An explicit later manual `Check for updates` may present that same still-current release because the user deliberately requested discovery.

Turning the automatic preference OFF suppresses automatic dialog presentation even when an already-started automatic query completes afterward. Manual discovery remains available.

Durable `AALyrics updated` feedback has modal priority, and an unconsumed success marker prevents automatic checking on that Phone entry.

### Verified in-app download

When a newer eligible release is available, Settings exposes an explicit `Download` action. AALyrics resolves exactly one matching APK and checksum asset:

```text
AALyrics-vX.Y.Z[-suffix].apk
AALyrics-vX.Y.Z[-suffix].apk.sha256
```

The download runtime is application-owned. It accepts only HTTPS asset URLs, follows only HTTPS redirects, bounds downloaded content, stages the APK under the app-private cache, parses the Release workflow's single-line `sha256sum` output for the exact APK filename, and calculates SHA-256 over the downloaded APK before accepting it.

The Unified Update Dialog distinguishes preparation from transfer. `PREPARING_DOWNLOAD` covers asset resolution, checksum retrieval/parsing, and staging-file preparation and uses indeterminate progress. The runtime enters `DOWNLOADING` immediately before the APK body transfer starts. GitHub Release asset `size` is the expected total byte count, while the download boundary reports bytes received; the dialog therefore renders determinate 0–100% progress. The completed transfer byte count must equal the Release-published asset size before checksum verification continues.

The APK is written first as an operation-owned partial artifact under app-private cache storage. Each reset generation uses a distinct staging filename, so a stale transport can only delete its own destination. Only after the published digest matches is it promoted into app-private no-backup persistent storage. Promotion uses an operation-owned temporary persistent `.promoting` file. Canonical verified-APK commit and Reset cleanup are serialized by the same file-store mutation lock, and commit revalidates operation ownership inside that critical section. Therefore Reset either runs after a completed commit and removes it, or invalidates ownership before commit so the canonical APK is never published. Missing/duplicate assets, malformed checksum content, transport/I/O failure, oversize content, or digest mismatch map to `DOWNLOAD_FAILED`; a failed or partial APK must not remain as a verified artifact.

Active downloads survive destination changes and Activity recreation because the runtime is application-owned. A verified `DOWNLOADED` artifact survives process restart: runtime initialization removes transient `.part` / `.promoting` artifacts, then restores `DOWNLOADED` when exactly one canonical verified APK exists, remains eligible for the installed update channel, and is still newer than the installed app. Stable installed builds never restore a retained prerelease APK. If the installed version has caught up, the retained release is no longer channel-eligible, or the APK is malformed/invalid for restoration, the persistent update artifact is removed and update state returns to `IDLE`.

`Reset AALyrics` cancels update work and clears both staging and verified update artifacts.

### In-app installation handoff

`DOWNLOADED` means that the retained signed-release APK bytes match the Release-published SHA-256. It remains an internal/recovery state, but the approved user-facing flow does not require a second Settings-row Install action; the Unified Update Dialog continues the same Update intent into install preparation.

Before installation continues from the verified artifact, AALyrics must refresh eligible GitHub Releases using the same release grammar and installed-channel rules used by ordinary update discovery. If the retained verified release is still the latest eligible release, installation preparation may continue. If a newer eligible release has appeared, AALyrics must not intentionally install the older retained APK first; the active Unified Update Dialog returns to the available state for the newer release. This refresh is an install-process boundary, not background discovery, and does not alter the automatic discovery cadence.

Before any PackageInstaller session is created, application-owned preflight validates the retained APK as an update of the installed AALyrics package. The retained file must still be the canonical verified artifact, archive metadata must be readable, the package name must match `io.github.whoxamxl.aalyrics`, the archive version must be newer than the installed Android package version, and its signing identity must be update-compatible with the installed AALyrics package. SHA-256 verification proves Release-asset integrity; package/version/signing validation separately proves that Android package handoff is appropriate.

AALyrics targets Android 8.0+ and declares `android.permission.REQUEST_INSTALL_PACKAGES` for this feature. Before installer handoff it checks `PackageManager.canRequestPackageInstalls()`. If Android does not currently trust AALyrics as an install source, the runtime enters permission-required state and Phone presents the #74 explanation modal. Dismissing that modal preserves the verified APK and the permission-required runtime state; pressing `Install` again reopens the explanation rather than jumping directly to Android Settings. Only the modal's explicit `Grant permission` action opens the platform's per-app unknown-source settings. On return, AALyrics re-checks platform state rather than assuming permission was granted; refusal leaves the prompt dismissed and the retained APK available, while granted trust resumes install preparation from that retained artifact. The secondary `Download from GitHub` action remains an external-release fallback and does not bypass Android confirmation. This platform-owned trust choice is not cleared by Reset AALyrics. Android's PackageManager exposes this trust check from API 26 onward, while the legacy `Intent.ACTION_INSTALL_PACKAGE` entry point is deprecated from API 29 in favor of `PackageInstaller`.

Installation uses `android.content.pm.PackageInstaller.Session`, not the deprecated ACTION_INSTALL_PACKAGE flow. The application-owned boundary creates a full-install session, streams the retained APK into the session, syncs/closes the write, and commits with an `IntentSender` status callback. A `STATUS_PENDING_USER_ACTION` result hands the system-provided confirmation intent to the user; AALyrics does not bypass or synthesize Android's final install confirmation.

PackageInstaller session recovery is process-safe. On application-process startup, AALyrics inspects the install sessions still owned by its installer package and best-effort abandons unsealed sessions left behind before `commit()`. Sealed sessions are preserved because sealing occurs when `commit()` is called. If `STATUS_PENDING_USER_ACTION` arrives after the original process died and the in-memory callback registry is therefore empty, the receiver revalidates the Android session itself and resumes the system confirmation only when that session is sealed, owned by AALyrics, and targets the AALyrics package.

Immediately before `PackageInstaller.Session.commit()`, after the APK has been written and fsynced into the session, AALyrics synchronously persists `PendingUpdate(targetVersion, targetVersionCode, installerSessionId, resumeAfterUpdate=true)`. The target versionCode comes from the APK metadata that already passed package/version/signing preflight. If this durable write fails, the installer commit is not performed.

Installer cancellation or terminal install failure preserves an otherwise-valid verified APK so the user can retry without downloading it again. The pending marker is bound to the exact PackageInstaller session: terminal failure clears it only when that session ID matches, including after process loss when the in-memory status sink no longer exists. Startup recovery also clears the matching marker after it successfully abandons an owned unsealed session left behind before commit. PackageInstaller success alone does not clear the marker because successful package replacement may terminate the old process before an in-process terminal state can become authoritative.

After Android replaces AALyrics, a non-exported `ACTION_MY_PACKAGE_REPLACED` receiver reconciles the pending target against the version running in the new binary. An exact target versionCode/versionName match succeeds, and any strictly newer installed versionCode also satisfies the pending target. Successful reconciliation atomically promotes the pending marker to durable `SuccessfulUpdate(installedVersion, installedVersionCode, resumeAfterUpdate)` state.

The durable success marker drives one-time Phone feedback on the next valid READY entry. Dismissing that feedback clears the marker first; if the clear fails, the feedback remains eligible. When `resumeAfterUpdate=true`, the replacement receiver also makes one separate best-effort request to open `MainActivity`. That request is not evidence of update success, may be suppressed by Android background-launch policy, does not alter the Phone destination model, and never clears the durable success marker.

Next-launch installed-version reconciliation remains authoritative for stale retained-APK cleanup: once the installed package version reaches or passes the retained release, the old verified APK is removed and update state returns to `IDLE`.

Reset AALyrics invalidates app-owned install preparation, abandons any installer session still under AALyrics control when practical, clears transient and verified update artifacts, and clears both pending and successful app-owned update recovery markers. The commit boundary is race-hardened: if Reset invalidates the operation while the synchronous pending-marker write is in flight, the installer path re-checks generation immediately after that write, clears only the matching session marker, and aborts before `Session.commit()`. Reset does not revoke Android's per-source install trust and does not undo an already installed package.

The Settings `Changelog` entry remains independent of the update-network path. It renders the repository `CHANGELOG.md` bundled into the installed APK; it does not fetch GitHub Release notes at runtime. GitHub Releases remain authoritative for signed update distribution, while `CHANGELOG.md` is authoritative for the in-app release history.

## Installation and updates

Debug and release APKs use different signing identities. If a debug AALyrics build is installed, uninstall it before installing the first release-signed APK.

After the first release-signed installation, later APKs signed with the same release key and a higher `versionCode` can update it normally.

### Update-install validation fixture

The manual `Build` workflow keeps the ordinary debug-signed `update_test_version` fixture for update-discovery/download testing. For a real self-update installation test, enable `update_test_release_signed` and provide both an older `update_test_version` and a positive `update_test_version_code` lower than the published target release's Android version code.

That opt-in path restores the durable release keystore only inside the Actions runner, builds an older-version release APK with the same signing identity as published AALyrics releases, and uploads it only as a short-retention Actions artifact. It is never published as a GitHub Release. The keystore is removed from the runner after the fixture path and is never included in an artifact or log.

The intended device validation is:

```text
install older same-release-signed fixture
    -> Check for updates
    -> Unified Update Dialog
    -> Update
    -> Download / verify current published release
    -> install preparation / Android confirmation
    -> current published AALyrics replaces the fixture
```

The debug-signed fixture remains unsuitable for proving successful replacement because Android update compatibility requires the same signing identity.

#### Validated Package Installer baseline

The same-release-signing fixture path has now been exercised on a real device. The validated baseline covers explicit update discovery/download, SHA-256 verification, Android per-source install trust, Android-owned confirmation, cancellation/retry without requiring a second download of an otherwise-valid retained APK, and successful replacement of the older release-signed fixture by the newer published release.

This validation establishes the current split `Download -> Install` flow as a functional checkpoint. A later Phone UX slice may compose those two user-visible actions into a single `Update` action, but the underlying download, verification, retained-artifact, install-time Release refresh, package/version/signing preflight, source-trust, and PackageInstaller boundaries remain required.

The published target used for this device pass predates the current update/installer runtime. It therefore proves Android's same-signing replacement path but cannot fully prove the new binary's next-launch retained-APK cleanup end to end. That cleanup remains part of the runtime/test contract until a release containing the current update runtime can be used as the update target.

The approved follow-up UX contract is documented in `docs/UPDATE_UX.md`.

AALyrics is intentionally distributed outside Google Play. For Android Auto, non-Play media apps still require Android Auto developer mode and **Developer settings → Unknown sources** on the test/user device. APK signing does not remove that Android Auto trust-source requirement.

## Release readiness

For an early Pre-release, feature completeness is not required. At minimum, the release pipeline should establish that:

- CI tests pass;
- the APK is release-signed;
- the GitHub Release contains the APK and checksum;
- the APK installs on a target Android device;
- a later release-signed APK can update the installed release using the same key.

Product-specific behavior, including Android Auto rendering and playback/lyrics behavior, should be documented in release notes with known limitations rather than hidden behind an implication that an alpha release is complete.

Stable releases should use a stricter product-readiness decision appropriate to the functionality present at that time.
