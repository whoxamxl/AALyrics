# In-app Update UX

## Branch and baseline

- Branch: `feature/automatic-update-check`.
- Base: `feature/package-installer` at `e97894c0920d40cd3f01f3dd719dd0b52d3b6ad7` (`#74` squash-merged recovery UX baseline).
- Parent installer PR: #72.
- Classification: SETTINGS / UPDATE UX / ANDROID PACKAGE INSTALLER.
- Authoritative references: `AGENTS.md`, `docs/UPDATE_UX.md`, `docs/RELEASES.md`, `docs/PHONE_SETTINGS.md`, and the validated package-installer runtime/tests inherited from the parent branch.

## Validated Package Installer checkpoint

The follow-up branch starts from the completed Package Installer implementation rather than reopening the installer architecture.

Validated baseline:

- [x] Package/version/signing preflight fails closed before installer handoff.
- [x] Explicit Install refreshes the latest eligible GitHub Release before installing a retained APK.
- [x] Android per-source install trust is checked through `PackageManager.canRequestPackageInstalls()`.
- [x] Unknown-source Settings return re-checks platform state instead of assuming permission was granted.
- [x] PackageInstaller.Session write/fsync/commit and status callback handling are implemented.
- [x] Installer cancellation/failure preserves the verified APK for retry.
- [x] Reset invalidates app-owned install work/artifacts without revoking Android-owned source trust.
- [x] Process-death recovery abandons interrupted unsealed sessions and can recover a valid sealed pending-user-action handoff.
- [x] Phone presentation and Preview coverage exist for the current split Download / Install flow.
- [x] Same-release-signing manual Actions fixture is implemented.

Validation evidence:

- Pull-request CI Build #900 passed branch/commit validation, architecture boundaries, debug APK build, unit tests, and debug artifact upload on parent head `df1e394`.
- Manual Build #901 passed the release-signing fixture path and produced `aalyrics-update-test-release-signed-0.1.0-alpha.1.apk` with Android `versionCode=1`.
- Real-device validation exercised the older same-release-signed fixture against published `v0.2.0-alpha.1` / Android `versionCode=2`.
- The real-device pass covered update discovery/download, install-source permission handling, Android-owned confirmation, cancellation/retry, and successful same-signing package replacement.
- No functional blocker was found in that pass; the remaining findings are UX refinements captured by this follow-up slice.

Known validation limit:

The published `v0.2.0-alpha.1` target predates the current download/installer runtime. It can prove Android's same-signing self-update path, but cannot fully exercise the new binary's post-update retained-APK cleanup on next launch. That cleanup remains covered by the current runtime contract/tests until a release containing this runtime is available as the update target.

## Current user-visible baseline

The validated baseline remains intentionally explicit:

```text
Check for updates
    -> Download
    -> Downloaded
    -> Install
    -> Android confirmation
```

This is the safe checkpoint for the UX follow-up. Do not remove or weaken the internal stages while simplifying presentation.

## Goal

Polish the user-facing update experience while preserving the validated update pipeline and Android-owned security/confirmation boundaries.

Approved follow-up direction:

1. replace the compact install-permission-required presentation with a large explanatory modal dialog;
2. separate durable permission-required state from transient dialog visibility and dismissal;
3. persist pending update intent so successful replacement can produce one-time `Update successful` feedback on the next valid app entry;
4. attempt post-update return to AALyrics only on a best-effort basis and never depend on background Activity launch for correctness;
5. keep manual `Check for updates` and add durable bounded automatic discovery;
6. make Settings own manual discovery only: Idle / Checking / Up to date / Check failed;
7. route MANUAL and AUTOMATIC newer-release results into the same shared release-available dialog;
8. keep PR #76 / `feature/one-step-update` as a legacy/reference prototype only; it is not the baseline for new implementation work;
9. in #77, move existing preparing/download progress, verification, downloaded/install, permission, failure/Retry, install-refresh retargeting, and install presentation out of Settings and into the Unified Update Dialog while preserving the validated two-stage `Update -> Download/Verify -> DOWNLOADED/Ready to install -> Install` interaction;
10. in #77, complete full real-device E2E validation of that two-stage route before changing its transition contract;
11. in #78, compose the validated Download + Install stages behind one user-facing `Update` intent by adding automatic continuation above the existing internal download/verify/`DOWNLOADED`/preflight/install state machine.

## Scope guardrails

- No silent/unattended package installation.
- No bypass of Android per-source install trust.
- No bypass or replacement of Android's final installation confirmation.
- No requirement that Android always relaunch AALyrics after package replacement.
- No removal of manual `Check for updates`.
- No regression to APK SHA-256 verification, package/version/signing preflight, retained-artifact retry, or install-time latest-release refresh.
- Every new durable preference/marker must explicitly re-evaluate `Reset AALyrics`.

## Implementation checkpoints

### #75 — automatic discovery and unified release handoff

- [x] Freeze the validated Package Installer baseline in TASK/update documentation.
- [x] Separate install-permission runtime state from transient dialog visibility.
- [x] Implement the large install-permission explanation dialog and lifecycle behavior.
- [x] Persist durable `PendingUpdate` immediately before PackageInstaller commit.
- [x] Reconcile `ACTION_MY_PACKAGE_REPLACED` into durable update-success state.
- [x] Show one-time Update successful feedback on the next valid app entry.
- [x] Add best-effort resume-after-update behavior.
- [x] Preserve typed install-failure reasons and retained-artifact retry semantics.
- [x] Standardize dismissible custom update-dialog X placement through shared `PhoneDialogHeader`.
- [x] Bind durable pending-update recovery to the exact PackageInstaller session and harden Reset races.
- [x] Add durable automatic update checking preference and bounded automatic discovery.
- [x] Add release-available prompting and same-session automatic suppression.
- [x] Standardize semantic application/release version presentation through `VersionChip`.
- [x] Freeze the original #75 discovery contract in `docs/UPDATE_UX.md`, `docs/PHONE_SETTINGS.md`, `docs/RELEASES.md`, and `docs/PHONE_UI_SPEC.md`.
- [x] Generalize the automatic-only release prompt owner so MANUAL and AUTOMATIC `UpdateAvailable` use the same release dialog.
- [x] Keep automatic same-session suppression notification-specific while allowing explicit manual discovery to re-present the same current release.
- [x] Preserve check origin through Checking / Up to date / Failed so AUTOMATIC non-update outcomes remain silent in Settings while MANUAL outcomes stay visible.
- [x] Keep `UpdateAvailable` itself out of the Settings row and hand both MANUAL and AUTOMATIC discoveries to the same release dialog.
- [x] Add focused runtime/mapper coverage for the MANUAL/AUTOMATIC discovery split and prompt behavior.
- [x] Confirm the shared release-available dialog on-device for both MANUAL and AUTOMATIC discovery.
- [x] Run final #75 CI/review validation after checkpoint documentation is aligned.
- [x] Merge #75 into `feature/package-installer`.

### #76 — legacy/reference one-step prototype

PR #76 / `feature/one-step-update` is intentionally retained as a **legacy/reference branch and Draft PR**.

- Do not use #76 as the base for #77 or #78.
- Do not rebase or continue #76 as the active implementation path.
- It may be consulted for prior UI/component ideas, tests, or implementation approaches.
- Any code reused from #76 must be re-evaluated against the current `feature/package-installer` baseline and the #77 two-stage contract.
- The explicit `VerifyingDownload` runtime boundary is the only #76 implementation concept intentionally adopted early into #77.
- #76's Settings-owned `VERIFYING` presentation and its Settings Preview fixtures must **not** be copied. Recreate equivalent verification coverage against the #77 Unified Update Dialog when that presentation layer exists.
- #76's one-step runtime tests remain reference material for #78. For #77, reuse only the scenario coverage ideas that also apply to the explicit two-stage route: verification, retained-artifact reuse, permission stop/resume, failure/Retry, and install-refresh retargeting.
- Keeping #76 open also preserves its branch as a stable historical reference.

### #77 — Unified Update Dialog and validated two-stage E2E

Active branch: `feature/unified-update-dialog`, created fresh from the current `feature/package-installer` baseline after #75 was merged.

- [ ] Reduce the Settings Version/update presentation to only Idle / Checking / Up to date / Check failed for the complete update lifecycle.
- [x] Introduce explicit application-owned `VerifyingDownload(versionName)` immediately before SHA-256 verification. Until the Unified Update Dialog mapper lands, keep this runtime state temporarily folded into the existing Settings preparing presentation; do not add a new Settings verification UI.
- [ ] Move preparing/download progress and the explicit `VerifyingDownload` presentation, permission-required, typed failure/Retry, installing, and install-refresh retarget presentation into the Unified Update Dialog.
- [ ] Keep `DOWNLOADED` as the authoritative verified-artifact boundary and render it as `Ready to install`.
- [ ] Keep an explicit user-facing `Install` action after `DOWNLOADED`; do not auto-continue into installation in #77.
- [ ] Preserve independent `downloadUpdate()` and `installUpdate()` operations and all existing SHA-256, retained-artifact, install-refresh, package/version/signing preflight, source-trust, PackageInstaller, and recovery behavior.
- [ ] Remove process presentation from Settings only when the corresponding dialog presentation exists.
- [ ] Align process-state Previews and focused tests with Unified Update Dialog ownership, including `Ready to install`.
- [ ] Complete full real-device E2E of the two-stage route:
  `Update -> Download/Verify -> DOWNLOADED/Ready to install -> Install -> Android confirmation -> replacement/recovery -> Update successful`.
- [ ] Record that route as the validated baseline before #77 is considered complete.

### #78 — one-step Update orchestration

Start only from the validated #77 baseline:

- [ ] Keep `DOWNLOADED` as a real internal/recovery state even when normal production UX auto-continues through it.
- [ ] Keep `downloadUpdate()` and `installUpdate()` independently testable and reusable.
- [ ] Add one-step continuation intent/coordinator above the existing operations rather than replacing them with a monolithic update routine.
- [ ] Automatically continue from verified `DOWNLOADED` into the existing install stage only when one-step continuation intent is present.
- [ ] Prevent duplicate install continuation across recomposition, Activity recreation, process recovery, and permission return.
- [ ] Preserve retained verified APK recovery without requiring a second download.
- [ ] Remove the second user-facing Install action only after focused tests and separate real-device E2E prove the one-step orchestration.

## Current checkpoint

**#75 is merged into `feature/package-installer`. PR #76 remains legacy/reference only. Active implementation now starts fresh in #77; one-step orchestration is deferred to #78.**

The #75 production behavior is:

```text
MANUAL
  Check for updates
  -> Checking        -> Settings
  -> Up to date      -> Settings
  -> Check failed    -> Settings / Retry
  -> UpdateAvailable -> shared release dialog

AUTOMATIC
  -> Checking        -> silent in Settings
  -> Up to date      -> silent in Settings
  -> Failed          -> silent in Settings
  -> UpdateAvailable -> same shared release dialog
```

Additional #75 invariants:

- MANUAL and AUTOMATIC `UpdateAvailable` use the same `New release available` dialog and the same existing verified-download entry point.
- Turning automatic checks OFF suppresses only automatic presentation; manual discovery remains available.
- Dismissing an automatic prompt suppresses that exact automatic version for the current process session.
- An explicit later manual check may present the same still-current version even when its automatic prompt was suppressed.
- If MANUAL discovery is requested while an AUTOMATIC query is already in flight, the existing query is promoted to MANUAL presentation semantics: Settings immediately shows Checking, no duplicate release request is started, and the eventual result is surfaced as MANUAL.
- `INSTALL_REFRESH` remains internal and does not create a MANUAL/AUTOMATIC discovery prompt in this #75 scope. If install preparation finds a newer eligible release, the existing Settings process surface shows `Newer update available -> Download` and preserves that retarget state across Settings re-entry.
- Settings does not present `UpdateAvailable` for MANUAL/AUTOMATIC discovery; after the user presses `Update`, the existing download/install presentation remains unchanged from the validated pre-unification baseline until #77 moves that same two-stage process, including install-refresh retargeting, into the Unified Update Dialog.
- #77 must preserve the explicit `DOWNLOADED / Ready to install -> Install` user checkpoint and prove it end-to-end on-device.
- #78 alone may remove that second user action by automatically continuing across the already-validated `DOWNLOADED` boundary.
- The existing download, SHA-256 verification, retained APK, install refresh, package/version/signing preflight, source trust, PackageInstaller, durable recovery, and Reset boundaries are unchanged by #75.

A temporary process-presentation implementation was intentionally reverted from the #75 branch before #75 was finalized. The older PR #76 separately retains a legacy one-step prototype for reference, but it is not part of the active stack.

The active #77 branch starts from the post-#75 `feature/package-installer` baseline and must implement only the Unified Update Dialog plus the explicit two-stage route.
