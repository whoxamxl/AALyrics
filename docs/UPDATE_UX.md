# In-app Update UX

## Purpose

This document defines the Phone-facing update UX that follows the validated in-app Package Installer handoff.

The underlying update pipeline remains application-owned and keeps its existing safety boundaries:

```text
release discovery
    -> APK download
    -> SHA-256 verification
    -> retained canonical APK
    -> install-time latest-release refresh
    -> package/version/signing preflight
    -> Android source-trust check
    -> PackageInstaller.Session
    -> Android-owned confirmation
```

The UX follow-up may compose these stages into fewer user-visible actions, but it must not remove, bypass, or weaken any stage.

## Validated baseline

The follow-up branch starts from `feature/package-installer` at `df1e394f8dce87b87cc25e6c57cbaa071e40e29d`.

The baseline has been exercised with an older APK built through the manual Actions fixture using the same durable release-signing identity as published AALyrics releases. Real-device validation covered the explicit update path through update discovery, verified download, Android per-source install trust, Android-owned install confirmation, cancellation/retry, and successful same-signing self-update.

The current user-visible baseline intentionally remains:

```text
Check for updates
    -> Download
    -> Downloaded
    -> Install
    -> Android confirmation
```

That split flow is a validated implementation checkpoint, not the intended final UX.

The currently published update target used by this validation predates the new installer/update runtime. It can prove the same-signing Android replacement path, but it cannot fully prove the new binary's post-update retained-APK cleanup behavior end to end. That cleanup remains covered by the current runtime contract/tests until a release containing the new runtime is available as the update target.

## Install-permission explanation

When an update reaches the point where Android does not yet trust AALyrics as an install source, Phone UI should present a large modal dialog rather than only a compact Settings-row status.

The dialog should explain:

- AALyrics is distributed outside Google Play;
- Android requires per-source permission before AALyrics may hand an APK to the system installer;
- granting this permission does not allow AALyrics to install an update silently;
- Android still owns and displays the final installation confirmation.

Actions:

```text
Grant permission
Download from GitHub  ↗
```

The dialog has an explicit close affordance. System Back has the same dismissal semantics. Leaving the current primary tab dismisses it. Moving AALyrics to the background or stopping/replacing the Activity also dismisses the transient prompt. Returning to the foreground, recreating the Activity, or surviving process loss must not make a dismissed dialog automatically reappear.

The durable/runtime fact that install-source trust is missing must remain separate from transient dialog visibility. If the user dismisses the explanation, it stays dismissed until the user explicitly invokes Update/Install again while permission is still missing.

### State ownership checkpoint

The separation layer is implemented before the dialog itself:

- `AppUpdateCheckState.InstallPermissionRequired` remains the durable process-runtime fact that Android source trust is missing for the retained install target;
- `UpdateInstallPermissionPromptRuntime` owns a separate process-local, non-persisted prompt request;
- entering the permission-required state requests the prompt once;
- explicitly invoking Install/Update again while permission is still required re-requests the prompt without repeating download or install preparation;
- dismissing the prompt does not clear `InstallPermissionRequired`;
- Settings root reset, leaving Settings for another primary destination, Activity stop/background, Activity/composition disposal, Reset AALyrics, and returning from Android source-trust Settings clear only the transient prompt.

The Phone presentation model carries the prompt separately from `AppUpdateUiPhase.INSTALL_PERMISSION_REQUIRED`.

### Permission dialog checkpoint

The large explanation dialog is now implemented as a modal Phone surface rather than a full-screen setup destination.

Required behavior:

- right-side close button dismisses only the transient prompt;
- system Back has the same dismissal behavior;
- tapping outside the dialog does not dismiss it;
- leaving Settings for another primary destination, Settings root reset, Activity stop/background, Activity/composition disposal, Reset, or returning from Android source-trust Settings clears the transient prompt;
- foreground return does not recreate the prompt merely because `INSTALL_PERMISSION_REQUIRED` is still true;
- returning from Android source-trust Settings re-checks platform trust only: refusal keeps `INSTALL_PERMISSION_REQUIRED` with the prompt dismissed, while granted trust resumes the retained-APK install without reopening the explanation;
- dismissing the dialog does not clear `INSTALL_PERMISSION_REQUIRED` or the retained verified APK;
- the compact Settings update row remains in the permission-required phase and exposes `Install` as the explicit way to reopen the explanation;
- `Grant permission` dismisses the explanation and opens Android's per-app source-trust Settings;
- `Download from GitHub` dismisses the explanation and opens the matching GitHub Release page externally;
- neither action bypasses Android's final installation confirmation.

The dialog explains why sideload-distributed AALyrics needs the per-source permission and explicitly states that the permission does not grant silent-install capability. Typical, narrow-width, and enlarged-font Previews cover the shared dialog content. Settings Previews also pin the permission-return outcomes: denied returns to the permission-required row with no modal, while granted proceeds to the installing presentation. Runtime tests independently cover the denied and granted source-trust return paths.

## Successful-update feedback

Before PackageInstaller commit, AALyrics persists an app-owned pending-update marker containing enough information to reconcile the requested target after package replacement.

The implemented pending marker is:

```text
PendingUpdate
  targetVersion
  targetVersionCode
  resumeAfterUpdate = true
```

`targetVersionCode` comes from the APK package metadata that already passed package/version/signing preflight; it is not inferred from the release tag or filename.

The marker is stored in dedicated app-owned SharedPreferences using synchronous persistence. The write happens after the PackageInstaller session has received and fsynced the APK and immediately before `PackageInstaller.Session.commit()`. If the durable write fails, AALyrics fails closed and does not commit the installer session.

The old binary must not clear this marker merely because PackageInstaller reports success. Successful package replacement may terminate that process, so the new binary owns final reconciliation. Terminal installer failure clears only the pending marker; `Reset AALyrics` clears all app-owned update recovery state. Reset does not change Android-owned install-source trust.

The new binary now registers a non-exported `ACTION_MY_PACKAGE_REPLACED` receiver. The receiver does not start an Activity. It compares the durable pending target with the version actually running after replacement:

- installed `versionCode` greater than the target is treated as target reached;
- equal `versionCode` requires an exact `versionName` match;
- an older code or same-code/name mismatch leaves the pending marker untouched;
- no pending marker is a no-op.

When the target is reached, the store atomically promotes pending state into:

```text
SuccessfulUpdate
  installedVersion
  installedVersionCode
  resumeAfterUpdate
```

The success marker stores the new binary's actual version and carries forward the pending resume intent. Promotion removes the pending keys in the same synchronous SharedPreferences commit. A receiver/persistence failure does not attempt to launch presentation and leaves recovery for a later valid path.

A successful package replacement may terminate the old process. The new binary therefore owns durable success reconciliation.

On the next valid Phone entry after the installed package is confirmed to have reached the requested target, AALyrics keeps the normal Phone startup destination (`PhoneDestination.Home`, which is Lyrics) and overlays a one-time success dialog:

```text
AALyrics updated

You're now running v0.x.x.

Done
```

No update-specific navigation override is applied. The dialog appears only once the normal entry gates have reached `READY`, so Notification Access and Android Auto compatibility onboarding remain authoritative.

The durable `SuccessfulUpdate` marker is loaded into a process-level feedback runtime. It is not cleared when the dialog is first shown. Done, the top-right close button, and system Back all request the same dismissal. Dismissal clears the durable success marker first and removes the dialog from process state only when that clear succeeds. Outside-tap dismissal is disabled. If the process dies before dismissal, the marker remains and the dialog is shown again on the next valid Phone entry.

Typical, narrow-phone, and enlarged-font Previews cover the success dialog.

Automatic return to AALyrics after replacement remains best-effort only. The UX does not depend on background Activity launch succeeding. If Android does not permit automatic return, the next ordinary user launch still guarantees the one-time success feedback.

## Automatic update checks

Settings should expose an opt-in product control named for what it actually does, for example:

```text
Automatically check for updates        [toggle]
```

It checks for a newer eligible release and may notify/prompt the user. It does not silently install an update.

The existing `Check for updates` action remains available as the explicit manual path regardless of the automatic-check setting.

When an automatic check discovers a newer eligible release, Phone UI may show a `New release available` dialog with an Update action and a dismiss/not-now action. Dismissing a release prompt must not cause the same prompt to reappear repeatedly during ordinary navigation in the same app session.

## One-step user update action

The final user-facing flow should not require users to understand the distinction between downloading and installing an APK.

The preferred UX is:

```text
New release available / manual check result
    -> Update
    -> Downloading update...
    -> Verifying / preparing...
    -> permission explanation only if required
    -> Android confirmation
    -> Update successful
```

Internally, download, verification, retained-artifact ownership, install refresh, APK preflight, source trust, and PackageInstaller remain separate states and boundaries.

If permission is missing after the APK has already been verified, dismissing the permission explanation must preserve the verified APK. Invoking Update again should continue from the retained artifact rather than force a second download while that artifact remains valid.

## Reset contract

Every durable state introduced by this UX follow-up must explicitly re-evaluate `Reset AALyrics`.

Both implemented recovery markers — `PendingUpdate` and `SuccessfulUpdate` — are app-owned and are deleted by `Reset AALyrics`, together with active app-owned update work and retained update artifacts. Future automatic-check preferences must make the same explicit keep/delete/default decision when introduced. Android's per-source install trust remains system-owned and must not be revoked by Reset.

## Implementation order

Implement in bounded checkpoints:

1. separate install-permission runtime state from transient dialog visibility;
2. add the large install-permission explanation dialog and lifecycle behavior;
3. persist durable `PendingUpdate` immediately before PackageInstaller commit;
4. reconcile `ACTION_MY_PACKAGE_REPLACED` into durable update-success state;
5. present one-time `Update successful` feedback on the next valid app entry;
6. add best-effort resume-after-update behavior without relying on it for correctness;
7. add the automatic update-check preference and release-available dialog;
8. compose Download + Install into one user-facing Update action while preserving the existing internal state machine;
9. align Previews, Reset behavior, docs, tests, CI, and real-device regression validation.

Do not combine these checkpoints into one large implementation change.
