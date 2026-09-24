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
7. route MANUAL and AUTOMATIC newer-release results into the same global Unified Update Dialog;
8. move existing preparing/download progress, verification, permission, failure/Retry, and install presentation out of Settings and into that dialog;
9. compose Download + Install behind one user-facing `Update` intent while retaining the existing internal download/verify/preflight/install state machine.

## Scope guardrails

- No silent/unattended package installation.
- No bypass of Android per-source install trust.
- No bypass or replacement of Android's final installation confirmation.
- No requirement that Android always relaunch AALyrics after package replacement.
- No removal of manual `Check for updates`.
- No regression to APK SHA-256 verification, package/version/signing preflight, retained-artifact retry, or install-time latest-release refresh.
- Every new durable preference/marker must explicitly re-evaluate `Reset AALyrics`.

## Implementation checkpoints

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
- [x] Add the initial automatic-discovery release dialog and session suppression.
- [x] Standardize semantic application/release version presentation through `VersionChip`.
- [x] Freeze the approved unified discovery/update presentation contract in `docs/UPDATE_UX.md`, `docs/PHONE_SETTINGS.md`, `docs/RELEASES.md`, and `docs/PHONE_UI_SPEC.md`.
- [ ] Generalize the automatic-only prompt owner into the Unified Update Dialog presentation owner.
- [ ] Route both MANUAL and AUTOMATIC `UpdateAvailable` results into the same dialog.
- [ ] Keep automatic same-session suppression notification-specific while allowing explicit manual discovery to re-present the same current release.
- [ ] Reduce Settings Version/update presentation to Idle / Checking / Up to date / Check failed.
- [ ] Move preparing/download progress, verification/preparation, permission-required, typed failure/Retry, and install presentation into the Unified Update Dialog.
- [ ] Compose Download + Install behind the dialog's one user-facing `Update` action.
- [ ] Align production Previews and focused tests with the new presentation ownership.
- [ ] Complete same-release-signing device E2E from a recovery-capable OLD APK to a newer recovery-capable APK, confirming durable replacement reconciliation and one-time Successful dialog.
- [ ] Complete focused real-device regression for manual/automatic discovery convergence and unified update-dialog behavior.

## Current checkpoint

**Documentation-first checkpoint complete. No unified-flow production code has been implemented yet.**

The approved target is now explicit:

```text
Settings
  Manual discovery only
  -> Check for updates
  -> Checking
  -> Up to date
  -> Check failed / Retry

MANUAL UpdateAvailable ───────┐
                              ├─> Unified Update Dialog
AUTOMATIC UpdateAvailable ────┘
                              -> Update
                              -> Preparing / Download progress
                              -> Verify / Prepare
                              -> Install preparation
                              -> Permission if required
                              -> Android confirmation
                              -> Update successful
```

Automatic Checking / Up to date / Failed remain silent. `INSTALL_REFRESH` remains an internal update-process origin and never creates a Settings update row.

The existing byte-based download progress, SHA-256 verification, retained verified APK, latest-release refresh, package/version/signing preflight, source-trust handling, typed failure reasons, PackageInstaller handoff, durable pending/success recovery, and Reset race hardening are preserved as implementation requirements. The refactor changes presentation ownership, not safety boundaries.

Current production code still reflects the previous checkpoint in two places and is therefore intentionally **not yet aligned with the new docs**:

- `AutomaticUpdateReleasePromptRuntime` only opens the release dialog for `AUTOMATIC` results;
- Settings still owns `UpdateAvailable`, Download/progress, Downloaded/Install, permission-required, and failure presentation.

Those are the next implementation targets. Do not start #76-style one-step composition until the #75 discovery handoff is unified and validated.

