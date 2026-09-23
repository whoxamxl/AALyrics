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
- [x] Implement the large install-permission explanation dialog and lifecycle behavior.
- [ ] Add durable pending/success update reconciliation and one-time success feedback.
- [ ] Add best-effort resume-after-update behavior.
- [ ] Add automatic update checking preference and new-release dialog.
- [ ] Compose Download + Install into one user-facing Update action.
- [ ] Align Previews, Reset behavior, docs, tests, CI, and real-device regression validation.

## Current checkpoint

The install-permission explanation UX is implemented on top of the separated prompt state.

Completed in this checkpoint:

- large modal explanation surface with responsive scrollable content;
- explicit copy explaining sideload distribution, Android per-source permission, and the continued Android-owned final install confirmation;
- top-right close button;
- system Back dismissal with the same transient-only behavior;
- outside-tap dismissal intentionally disabled;
- primary `Grant permission` action that dismisses the prompt before opening Android's per-app source-trust Settings;
- secondary `Download from GitHub` action with external-link icon that dismisses the prompt and opens the matching GitHub Release page;
- permission-required Settings row now exposes `Install` as the explicit dialog re-entry action rather than jumping directly to system Settings;
- prompt dismissal still preserves `INSTALL_PERMISSION_REQUIRED` and the retained verified APK;
- leaving Settings, Settings root reset, Activity stop/background, Activity/composition disposal, Reset, and Android source-trust Settings return continue to clear only the transient prompt;
- foreground return does not automatically recreate a dismissed permission prompt;
- denied source-trust return keeps `INSTALL_PERMISSION_REQUIRED` with the prompt dismissed until the user explicitly presses `Install` again;
- granted source-trust return resumes installation from the retained verified APK without reopening the explanation;
- typical, narrow-phone, and enlarged-font dialog Previews plus explicit denied/granted permission-return Settings Previews;
- focused runtime tests cover denied/granted Android source-trust return behavior;
- Phone/Update UX docs aligned with the implemented behavior.

No durable update-success marker, post-update feedback, automatic update check, or one-step Download/Install composition has been started.

Next checkpoint: **add durable pending/success update reconciliation and one-time Update successful feedback**. Do not begin it until explicitly requested.
