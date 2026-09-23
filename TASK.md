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
- [x] Persist durable `PendingUpdate` immediately before PackageInstaller commit.
- [x] Reconcile `ACTION_MY_PACKAGE_REPLACED` into durable update-success state.
- [x] Show one-time Update successful feedback on the next valid app entry.
- [x] Add best-effort resume-after-update behavior.
- [x] Preserve typed install-failure reasons through Settings and explain them in the failure tooltip.
- [x] Standardize dismissible custom update-dialog X placement through shared `PhoneDialogHeader`.
- [x] Bind durable pending-update recovery to the exact PackageInstaller session and clear matching stale markers on abandoned/failed-session recovery.
- [ ] Complete same-release-signing device E2E from a recovery-capable OLD APK to a newer recovery-capable APK, confirming `ACTION_MY_PACKAGE_REPLACED` reconciliation and one-time Successful dialog. *(explicitly deferred device validation; best-effort automatic resume is not required to occur)*
- [x] Add durable automatic update checking preference and bounded automatic discovery.
- [x] Add automatic-discovery new-release dialog and session suppression.
- [ ] Compose Download + Install into one user-facing Update action.
- [ ] Align #75 Previews, Reset behavior, docs, tests, CI, and real-device regression validation.

## Current checkpoint

Automatic discovery now surfaces a process-local `New release available` dialog without changing manual update discovery or install-refresh behavior.

Completed in this checkpoint:

- added `AutomaticUpdateReleasePromptRuntime` as the application-owned transient prompt/suppression owner;
- only `AppUpdateCheckState.UpdateAvailable` with `origin=AUTOMATIC` can request the prompt;
- `MANUAL` and `INSTALL_REFRESH` update availability never create the automatic dialog;
- the dialog shows the discovered version with `Update`, `Not now`, a top-right close action, and system-Back dismissal;
- outside-tap dismissal is disabled;
- `Not now`, close, and Back share the same dismissal path and suppress that exact version for the rest of the current app-process session;
- a different automatically discovered version remains eligible for presentation;
- `Update` closes the prompt, suppresses transient re-presentation of the same version, and starts the existing download/verification pipeline;
- this checkpoint does not auto-chain a completed download into install; that remains the next one-step Update checkpoint;
- prompt and same-session suppression survive ordinary navigation/recomposition/Activity recreation because ownership is process-level;
- `Reset AALyrics` clears the transient prompt and process-local suppression;
- durable `SuccessfulUpdate` feedback has presentation priority, dialogs are not stacked, and an unconsumed success marker prevents automatic checking on that Phone entry;
- focused prompt-runtime tests cover automatic-only eligibility, manual/install-refresh exclusion, same-version suppression, different-version eligibility, Update consumption, and Reset;
- typical, narrow-phone, and enlarged-font new-release dialog Previews were added;
- Update UX and Phone Settings documentation are aligned.
- inherited #74 recovery hardening remains preserved: process-death cleanup is session-bound and Reset cannot race a stale pending marker past the PackageInstaller commit boundary.

No end-to-end one-step Download + Install composition has been implemented yet. The `Update` action currently enters the already-validated download/verification state machine and leaves installation as the existing explicit follow-up.

Next checkpoint: **compose Download + Install into one user-facing `Update` action while preserving download, checksum verification, retained-artifact ownership, install refresh, APK preflight, source trust, PackageInstaller, and Android confirmation as separate internal boundaries**. Do not begin it until explicitly requested.
