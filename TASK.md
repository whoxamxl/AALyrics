# In-app Update UX

## Branch and baseline

- Branch: `feature/unified-update-dialog` (PR #77).
- Base: `feature/package-installer` at `1e0b28cff9ea772db5d46765cd4fe72092e3ef11`.
- Validated pre-auto-install checkpoint: `d5de6a46a442eec0d1e293812475b51b57d572a0` (Build #1118 passed; latest Codex review found no major issue; corrected two-stage route validated on-device).
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
9. in #77, move existing preparing/download progress, verification, downloaded/install, permission, failure/Retry, install-refresh retargeting, and install presentation out of Settings and into the Unified Update Dialog while preserving the explicit two-stage `Update -> Download/Verify -> DOWNLOADED/Ready to install -> Install` interaction;
10. freeze the corrected two-stage route as the validated #77 checkpoint after Build #1118, latest Codex review, and real-device validation;
11. finish #77 with a narrowly scoped UX connection: when the existing download/verification path reaches authoritative `DOWNLOADED`, automatically dispatch the existing `installUpdate()` path once. Keep `downloadUpdate()`, `DOWNLOADED`, and `installUpdate()` as separate runtime boundaries; do not create a monolithic Download+Install operation or a separate #78 development phase.

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

- Do not use #76 as the active base for #77; it remains historical/reference material only.
- Do not rebase or continue #76 as the active implementation path.
- It may be consulted for prior UI/component ideas, tests, or implementation approaches.
- Any code reused from #76 must be re-evaluated against the current `feature/package-installer` baseline and the #77 two-stage contract.
- The explicit `VerifyingDownload` runtime boundary is the only #76 implementation concept intentionally adopted early into #77.
- #76's Settings-owned `VERIFYING` presentation and its Settings Preview fixtures must **not** be copied. Recreate equivalent verification coverage against the #77 Unified Update Dialog when that presentation layer exists.
- #76's one-step runtime tests remain scenario reference material only. Any useful continuation scenario must be re-evaluated against the current #77 runtime; do not revive the old #76 orchestration wholesale.
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
- [x] At the validated pre-auto-install checkpoint, keep an explicit user-facing `Install` action after `DOWNLOADED`; that checkpoint is preserved as the comparison/recovery baseline. The final #77 slice later adds automatic continuation for the normal accepted-update path.
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

- [x] Complete real-device validation of the corrected two-stage route:
  `Update -> permission gate if required -> Download/Verify -> DOWNLOADED/Ready to install -> Install -> install refresh/preflight -> PackageInstaller -> replacement/recovery -> Update successful`.
- [x] Record `d5de6a46a442eec0d1e293812475b51b57d572a0` as the validated pre-auto-install #77 checkpoint. Build #1118 passed and the latest Codex review reported no major issue.

### #77 final slice — automatic install continuation

This is intentionally a small continuation change on top of the validated checkpoint above, not a new update architecture and not a separate #78 development phase.

1. [x] Keep the existing independent `downloadUpdate()` and `installUpdate()` operations unchanged in responsibility.
2. [x] Keep `DOWNLOADED` as the authoritative verified-artifact/recovery checkpoint.
3. [x] Arm process-local automatic continuation from the user-facing `startUpdate()` path. After its existing download/verification job completes successfully in `DOWNLOADED`, dispatch the existing `installUpdate()` path.
4. [x] Keep low-level `downloadUpdate()` independently callable: when no continuation is armed, it still stops at `DOWNLOADED`. This preserves the two-stage runtime/test/recovery boundary.
5. [x] Preserve continuation across same-process Download Retry and install-refresh replacement download. Recoverable install failure stops automatic progression; explicit Install/Retry re-arms continuation through the existing `installUpdate()` entry point.
6. [x] Keep source-trust gating, install refresh, package/version/signing preflight, retained-APK handling, PackageInstaller handoff, and Android-owned final confirmation unchanged.
7. [x] Keep `Ready to install -> Install` as a recovery/fallback presentation rather than deleting it. Normal accepted-update flow auto-continues through `DOWNLOADED`, but a verified APK restored after process restart is not auto-installed without a fresh in-process user intent.
8. [x] Keep the GitHub manual-download fallback dismissal-only. Opening GitHub does not arm, trigger, or imply successful AALyrics-managed installation.
9. [x] Add focused coverage for normal auto-continuation, duplicate start/source-trust protection, Download Retry continuation, install-refresh replacement continuation, and the still-independent direct `downloadUpdate()` path.
10. [x] Align update docs and rename the `Ready to install` Preview as a recovery fallback. Full Build/CI remains the validation gate for the new HEAD.

## Current checkpoint

**#75 is merged into `feature/package-installer`. PR #76 remains legacy/reference only. PR #77 keeps the validated two-stage checkpoint at `d5de6a46a442eec0d1e293812475b51b57d572a0` and now implements the narrow automatic handoff on top: accepted Update flows auto-dispatch the existing `installUpdate()` after verified `DOWNLOADED`, while direct/recovered `DOWNLOADED` remains available as the explicit recovery fallback. Build/CI for the new auto-continuation HEAD is the remaining validation gate.**

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
- The explicit `DOWNLOADED / Ready to install -> Install` route is now the validated #77 checkpoint.
- The final #77 slice may remove only the ordinary second user action by automatically dispatching the existing install path after verified `DOWNLOADED`; the internal boundary remains.
- The existing download, SHA-256 verification, retained APK, install refresh, package/version/signing preflight, source trust, PackageInstaller, durable recovery, and Reset boundaries are unchanged by #75.

A temporary process-presentation implementation was intentionally reverted from the #75 branch before #75 was finalized. The older PR #76 separately retains a legacy one-step prototype for reference, but it is not part of the active stack.

The active #77 branch starts from the post-#75 `feature/package-installer` baseline. Settings owns only Idle / manual Checking / manual Up to date / manual Check failed. The Unified Update Dialog owns the post-Update process, including install-refresh `Newer update available -> Download`, while MANUAL/AUTOMATIC discovery still uses the separate dismissible release-available dialog. The corrected explicit two-stage route is now the validated checkpoint; the next #77 work is only the small automatic `DOWNLOADED -> installUpdate()` handoff.

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
- [x] Build / architecture checks / unit tests / CI passed on Build #1118 for checkpoint `d5de6a46a4`.
- [x] Latest Codex review on `d5de6a46a4` reported no major issue.
- [x] Corrected explicit two-stage route validated on-device and accepted as the pre-auto-install checkpoint.

