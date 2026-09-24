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

Pre-redesign checkpoint frozen before the next #77 runtime-contract slice:

- checkpoint commit: `2f25d0b9ad3e72685c66192ac6b92e8c7cb36e99`;
- PR #77 remains Draft against `feature/package-installer`;
- Build #1091 passed the complete workflow on that checkpoint;
- this checkpoint still has the old sequencing where source-trust permission is first requested during install preparation, only paused/recoverable dialog states are dismissible, and a dismissed process does not yet have a reliable `Check for updates` re-entry path.

- [x] Reduce the Settings Version/update presentation to only Idle / Checking / Up to date / Check failed for the complete update lifecycle.
- [x] Introduce explicit application-owned `VerifyingDownload(versionName)` immediately before SHA-256 verification.
- [x] Define the pure Unified Update Dialog presentation contract and mapper without changing runtime ownership: `UpdateDialogUiState` / `UpdateDialogPhase`, discovery-vs-install-refresh availability context, bounded download progress, explicit `VERIFYING` / `READY_TO_INSTALL`, typed install-failure mapping, and focused mapper coverage.
- [x] Move `PREPARING_DOWNLOAD` and `DOWNLOADING` presentation ownership from Settings into the Unified Update Dialog, including determinate 0–100% transfer progress, dedicated typical/narrow/large-font Previews, and Settings mapper/Preview cleanup.
- [x] Keep `VerifyingDownload` on the Unified Update Dialog surface during the ownership transition so the dialog does not disappear between transfer completion and `DOWNLOADED`.
- [x] Give `VerifyingDownload` dedicated `Verifying update` / `Checking download integrity…` presentation with indeterminate progress and typical/narrow/large-font Previews, without changing SHA-256 semantics.
- [x] Move `DOWNLOAD_FAILED` presentation and explicit `Retry` from Settings into the Unified Update Dialog. Retry calls the existing `downloadUpdate()` operation; no automatic retry loop or new download-failure runtime type is introduced.
- [x] Move `PREPARING_INSTALL`, `PERMISSION_REQUIRED`, and `INSTALLING` presentation from Settings into the Unified Update Dialog without changing install-refresh, APK preflight, Android source-trust, PackageInstaller, or recovery semantics. Permission-required keeps `Grant permission` and GitHub fallback actions on the unified surface.
- [x] Connect typed install failure/Retry presentation to the Unified Update Dialog. Retry calls the existing independent `installUpdate()` operation and Settings no longer owns install-failure presentation.
- [x] Connect install-refresh retarget presentation to the Unified Update Dialog. A newer release found during install preparation remains process-owned, returns the Unified Update Dialog to `Newer update available`, and uses the existing `downloadUpdate()` operation. Under the corrected dismissal contract this process-owned retarget presentation is dismissible without discarding the replacement candidate.
- [x] Keep `DOWNLOADED` as the authoritative verified-artifact boundary and render it as `Ready to install` in the Unified Update Dialog.
- [x] Keep an explicit user-facing `Install` action after `DOWNLOADED`; the button calls the existing `installUpdate()` operation and #77 does not auto-continue into installation.
- [x] Preserve independent `downloadUpdate()` and `installUpdate()` operations and all existing SHA-256, retained-artifact, install-refresh, package/version/signing preflight, source-trust, PackageInstaller, and recovery behavior.
- [x] Remove process presentation from Settings only when the corresponding dialog presentation exists.
- [x] Align process-state Previews and focused tests with Unified Update Dialog ownership, including `Ready to install`, typed install failure, and install-refresh retarget.

#### #77 pre-E2E contract correction

Complete these items before the final two-stage real-device validation:

1. [x] Separate Unified Update Dialog visibility from authoritative update-process state for every app-owned process phase.
2. [x] Make every app-owned Unified Update Dialog phase dismissible through the shared close affordance and System Back, including active download/verification/install-preparation states and install-refresh retarget. Outside-tap dismissal remains disabled.
3. [x] Dismissing the dialog hides presentation only: it does not cancel an active check/download/install operation, clear candidate/target state, delete a retained verified APK, abandon a PackageInstaller handoff, or otherwise mutate the authoritative runtime operation.
   - Implementation checkpoint: dismissal is now process-presentation scoped rather than phase-key scoped, so a dismissed active operation stays hidden as it advances through later phases. Re-entry remains intentionally deferred to item 4.
4. [x] Make Settings `Check for updates` the single re-entry path after process-dialog dismissal. If an app-owned update process already exists, reopen its current authoritative dialog state without starting a duplicate GitHub query or operation; only perform ordinary MANUAL discovery when no resumable/active process state exists.
   - Implementation checkpoint: Phone re-entry now clears only the local process-dialog hidden flag. It does not invoke `checkForUpdates()` while an app-owned process presentation exists, and `PERMISSION_REQUIRED` reopens from authoritative runtime state rather than legacy transient prompt visibility.
5. [x] Separate release-prompt acceptance from dismissal suppression. Accepting `Update` is no longer recorded as a dismissed MANUAL prompt; `Not now` / close / Back remain actual discovery-prompt dismissal. AUTOMATIC acceptance keeps the existing same-session automatic-notification suppression policy.
6. [x] Move the Android install-source trust gate ahead of APK download. Accepted release prompts now enter a dedicated permission-first `startUpdate()` path: missing trust shows `PERMISSION_REQUIRED` before transfer, denied return stays blocked, and granted return resumes the existing selected candidate into `downloadUpdate()` semantics without a second release query.
7. [x] Keep a second source-trust check immediately before PackageInstaller handoff as a fail-safe in case permission was revoked or state was restored after the pre-download gate. The existing `beginInstall()` trust check remains authoritative for installer handoff.
8. [x] Treat `Download from GitHub` in the permission UI as an external manual-download fallback **and as presentation dismissal**. The Phone host now marks the Unified Update Dialog hidden before opening the external release page, dismisses only the transient permission prompt, and leaves the authoritative `InstallPermissionRequired` state/candidate untouched. Opening GitHub does not imply that the user downloaded anything, does not mark permission as granted, and does not advance or clear the update process; a later `Check for updates` re-enters it.
9. [x] Add focused coverage for pre-download permission gating, denied/granted return, all-phase dismissal without runtime cancellation, `Check for updates` re-entry, duplicate-operation prevention, and manual GitHub fallback dismissal semantics.
   - [x] Re-entry selection is covered for every app-owned process phase; ordinary discovery/null state remains on the normal MANUAL discovery path.
   - [x] MANUAL release-prompt acceptance is covered separately from dismissal suppression; AUTOMATIC acceptance retains same-session notification suppression.
   - [x] Permission-first Update is covered for missing trust, denied return, granted return, zero download before grant, and immediate download when trust already exists. Existing install permission tests continue to cover the second pre-PackageInstaller trust gate.
   - [x] GitHub fallback dismissal is covered as transient-prompt dismissal only: authoritative `InstallPermissionRequired` remains unchanged and no APK transfer starts.
   - [x] Duplicate user acceptance and duplicate source-trust return cannot start a second download.
   - [x] Active permission-first download survives Settings re-entry without restart/cancellation and completes once the existing transfer resumes.
10. [x] Align Unified Update Dialog Previews so all process phases visibly use the shared dismissible header while outside-tap dismissal stays disabled; the existing Preview matrix inherits the shared header directly from `UnifiedUpdateDialogContent`.

- [ ] Complete full real-device E2E of the corrected two-stage route:
  `Update -> permission gate if required -> Download/Verify -> DOWNLOADED/Ready to install -> Install -> install refresh/preflight -> PackageInstaller -> replacement/recovery -> Update successful`.
- [ ] Record that corrected route as the validated baseline before #77 is considered complete.

### #78 — one-step Update orchestration

Start only from the validated #77 baseline.

#### Legacy #76 reference policy for #78

Adopt the **concepts**, not the old implementation wholesale:

- Reuse the old #76 idea of a user-originated **one-step continuation intent** that means "continue from download into the existing install stage when the verified artifact boundary is reached".
- Reuse the old #76 idea of a thin **coordinator / single user-facing Update entry point** above the existing `downloadUpdate()` and `installUpdate()` operations.
- Reuse the old #76 scenario coverage for:
  - normal Update -> download/verify -> install continuation;
  - download failure -> explicit Retry -> continuation;
  - install-source trust required -> pause -> permission return -> resume without redownload;
  - install-refresh finds a newer release -> retarget the same user operation to the newer candidate instead of installing the stale retained APK;
  - retained verified APK reuse.
- Do **not** copy the old process-local `oneStepUpdateRequested: Boolean` as the final #78 design. #78 continuation state must be owned explicitly enough to survive the recovery/lifecycle cases required by the current runtime contract and must be cleared by Reset.
- Do **not** copy the old #76 Settings-owned one-action presentation, old automatic-only release prompt ownership, stale mapper states, or old one-step documentation.
- Do **not** replace `downloadUpdate()` + `DOWNLOADED` + `installUpdate()` with a monolithic update routine.

#### Ordered #78 implementation plan

1. [ ] Freeze the fully validated #77 two-stage route as the pre-#78 baseline. No #78 orchestration work starts before this checkpoint is recorded.
2. [ ] Introduce an explicit one-step continuation intent/coordinator above the existing operations. The intent is created only by an explicit user Update/Retry action and is application-owned rather than composable-owned.
3. [ ] Keep `downloadUpdate()` and `installUpdate()` independently callable/testable. The coordinator may invoke them but must not absorb their download, verification, preflight, permission, PackageInstaller, or recovery logic.
4. [ ] Route the normal user-facing Update action through the coordinator: arm continuation intent, then invoke the existing download stage.
5. [ ] At verified `DOWNLOADED`, automatically invoke the existing install stage **only** when valid continuation intent exists and no install/preflight/session handoff is already active.
6. [ ] Add an idempotent continuation claim/guard so recomposition, duplicate state collection, Activity recreation, permission return, process recovery, or restored `DOWNLOADED` cannot start installation twice.
7. [ ] Preserve continuation across recoverable source-trust handoff: permission-required pauses the operation, Grant permission / return re-checks Android trust, and a valid retained APK resumes the existing install stage without redownload.
8. [ ] Preserve install-refresh retargeting: if preflight discovers a newer eligible release, never install the stale retained APK. While one-step continuation remains valid, retarget the same operation to the newer candidate and reuse the existing download -> verify -> `DOWNLOADED` -> install boundaries.
9. [ ] Stop automatic progression on recoverable Download/Install failure. An explicit Retry is a new user action that re-arms or resumes continuation; never create an automatic failure loop.
10. [ ] Define process-recovery semantics for continuation intent. If a valid retained `DOWNLOADED` artifact and continuation intent are restored, resume at most once from the authoritative runtime state; never duplicate an active/pending PackageInstaller operation. Reset AALyrics clears the continuation intent.
11. [ ] Add focused tests adapted from old #76 scenario coverage, but assert the current #77/#78 runtime contract rather than old Settings presentation or stale failure models.
12. [ ] Only after those tests are stable, remove the second user-facing `Install` action from the normal production path. Keep `DOWNLOADED` and the independently callable two-stage route in runtime/tests/recovery.
13. [ ] Align Unified Update Dialog Previews/docs for the one-step production path; do not restore old #76 Settings process Previews.
14. [ ] Complete a separate real-device #78 E2E proving that the only intended UX change from validated #77 is automatic continuation across `DOWNLOADED`, while Android source trust and final PackageInstaller confirmation remain explicit/system-owned.

## Current checkpoint

**#75 is merged into `feature/package-installer`. PR #76 remains legacy/reference only. #77 process-presentation migration is now complete through install-refresh retarget; runtime/build/review and real-device validation remain before #77 can close. One-step orchestration stays deferred to #78.**

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

The active #77 branch starts from the post-#75 `feature/package-installer` baseline and preserves the explicit two-stage route. Settings now owns only Idle / manual Checking / manual Up to date / manual Check failed. The Unified Update Dialog owns the post-Update process, including install-refresh `Newer update available -> Download`, while MANUAL/AUTOMATIC discovery still uses the separate dismissible release-available dialog. The next #77 work is validation rather than additional process-presentation migration.

Pre-Draft regression audit:

- [x] Static ownership/reference audit: no stale Settings process-state presentation remains; Unified Update Dialog phase/resource/callback coverage is internally aligned.
- [x] Semantic state-machine audit: download retry, explicit Ready-to-install -> Install, install-refresh retarget, source-trust return, retained-artifact reuse, and install Retry continue through the existing runtime operations.
- [x] Fix regression found by the semantic audit: Settings entry now preserves `InstallFailed` instead of collapsing it back to restored `Downloaded`, so the global reason/Retry presentation survives destination changes.
- [x] Historical Codex dismissal feedback was first addressed by making paused/recoverable states dismissible; the later pre-E2E contract correction supersedes that checkpoint and now makes every app-owned process phase, including active work and install-refresh retarget, dismissible without clearing runtime/artifact state.
- [x] Build / architecture checks / unit tests / CI pass on Build #1089 after fixing the Codex-review dismissal work's focused-test compile mismatch.
- [x] Codex review completed; valid feedback was addressed, including paused/recoverable dialog dismissal and obsolete Preview references.

Post-contract regression audit before Build / CI:

- [x] Static ownership/reference audit rerun after permission-first/dismiss-reentry changes. Removed stale non-dismissible wording, stale permission-after-download route descriptions, and pre-download UI copy that incorrectly referred to an already-verified update.
- [x] Semantic state-machine audit rerun across normal Update, permission deny/grant, Download Retry, dismiss/re-entry, explicit Ready-to-install -> Install, install-refresh retarget, fail-safe source-trust re-check, retained-artifact retry, and PackageInstaller handoff.
- [x] Fix audit finding: all user-started APK download paths now share the source-trust gate. Install-refresh replacement Download and Download Retry can no longer bypass the pre-transfer permission gate.
- [x] Add focused install-refresh coverage proving replacement Download is blocked until source trust is granted and resumes the selected replacement candidate afterward.
- [ ] Build / CI intentionally remain unrun after this audit checkpoint.

