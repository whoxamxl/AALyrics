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
- [ ] Show one-time Update successful feedback on the next valid app entry.
- [ ] Add best-effort resume-after-update behavior.
- [ ] Add automatic update checking preference and new-release dialog.
- [ ] Compose Download + Install into one user-facing Update action.
- [ ] Align Previews, Reset behavior, docs, tests, CI, and real-device regression validation.

## Current checkpoint

Package replacement now reconciles durable pending intent into durable success state without launching presentation.

Completed in this checkpoint:

- extended update recovery persistence with `SuccessfulUpdate(installedVersion, installedVersionCode, resumeAfterUpdate)`;
- split recovery cleanup scopes so installer failure clears pending state only, while `Reset AALyrics` clears pending and successful state;
- added pure package-replacement reconciliation policy;
- exact target `versionCode` requires matching `versionName`; a strictly newer installed `versionCode` also satisfies the pending target;
- older or same-code/name-mismatched replacements leave pending state untouched;
- successful reconciliation carries the pending `resumeAfterUpdate` flag forward and atomically replaces pending keys with successful keys in one synchronous SharedPreferences commit;
- registered a non-exported manifest receiver for `android.intent.action.MY_PACKAGE_REPLACED`;
- the receiver uses the new binary's `BuildConfig.VERSION_NAME` / `VERSION_CODE`, does not start an Activity, and performs no Phone presentation work;
- receiver/persistence failure is contained rather than forcing a UI launch;
- focused policy/handler/persistence tests cover exact match, newer installed binary, mismatches, no-pending behavior, promotion, and untouched pending state;
- Reset tests now cover unconsumed successful update state as well as pending state;
- Reset copy and Update/Phone Settings documentation are aligned.

No `Update successful` dialog, success-marker consumption, forced Main/Lyrics routing, notification fallback, or best-effort resume behavior has been implemented yet.

Next checkpoint: **surface durable `SuccessfulUpdate` on the next valid app entry as a one-time `Update successful` dialog, and clear it only when the user dismisses that feedback**. Do not begin it until explicitly requested.
