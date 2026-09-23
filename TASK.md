# In-app Update UX

## Branch and baseline

- Branch: `feature/update-ux`.
- Base: `feature/package-installer` at `df1e394f8dce87b87cc25e6c57cbaa071e40e29d`.
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
5. add `Automatically check for updates` as a release-notification/check preference, while keeping manual `Check for updates`;
6. show a `New release available` dialog for automatic discovery;
7. eventually compose Download + Install into one user-facing `Update` action while retaining the existing internal download/verify/preflight/install state machine.

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
- [ ] Implement the large install-permission explanation dialog and lifecycle behavior.
- [ ] Add durable pending/success update reconciliation and one-time success feedback.
- [ ] Add best-effort resume-after-update behavior.
- [ ] Add automatic update checking preference and new-release dialog.
- [ ] Compose Download + Install into one user-facing Update action.
- [ ] Align Previews, Reset behavior, docs, tests, CI, and real-device regression validation.

## Current checkpoint

Install-permission state ownership is now separated from transient presentation visibility.

Completed in this checkpoint:

- `InstallPermissionRequired` remains the update-runtime fact and is not cleared when the explanation is dismissed;
- a process-local `UpdateInstallPermissionPromptRuntime` owns prompt visibility/request state separately;
- entering permission-required requests the prompt once;
- explicitly invoking Install/Update again while still permission-required re-requests the prompt without repeating download/preflight;
- Phone presentation maps the prompt separately from `AppUpdateUiPhase.INSTALL_PERMISSION_REQUIRED`;
- leaving Settings, Settings root reset, Activity/composition disposal, Reset, and Android source-trust Settings return dismiss only the transient prompt;
- focused runtime/controller/mapper tests cover the separation and explicit re-request contract.

The existing compact `Installation permission required / Open settings` row is intentionally still rendered. The large explanatory dialog has **not** been implemented yet.

Next checkpoint: **implement the large install-permission explanation dialog and its UI dismissal/actions**. Do not begin it until explicitly requested.
