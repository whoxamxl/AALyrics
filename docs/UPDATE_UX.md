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

The dialog has an explicit close affordance. System Back has the same dismissal semantics. Leaving the current primary tab dismisses it. Activity/process loss does not make a dismissed dialog automatically reappear.

The durable/runtime fact that install-source trust is missing must remain separate from transient dialog visibility. If the user dismisses the explanation, it stays dismissed until the user explicitly invokes Update/Install again while permission is still missing.

### State ownership checkpoint

The separation layer is implemented before the dialog itself:

- `AppUpdateCheckState.InstallPermissionRequired` remains the durable process-runtime fact that Android source trust is missing for the retained install target;
- `UpdateInstallPermissionPromptRuntime` owns a separate process-local, non-persisted prompt request;
- entering the permission-required state requests the prompt once;
- explicitly invoking Install/Update again while permission is still required re-requests the prompt without repeating download or install preparation;
- dismissing the prompt does not clear `InstallPermissionRequired`;
- Settings root reset, leaving Settings for another primary destination, Activity/composition disposal, Reset AALyrics, and returning from Android source-trust Settings clear only the transient prompt.

The Phone presentation model carries the prompt separately from `AppUpdateUiPhase.INSTALL_PERMISSION_REQUIRED`. The large explanatory dialog is intentionally the next implementation checkpoint; this state-ownership checkpoint does not yet change the rendered Settings UI.

## Successful-update feedback

Before PackageInstaller handoff, AALyrics may persist an app-owned pending-update marker containing enough information to reconcile the requested target after package replacement.

The marker should include the target version/versionCode and whether AALyrics should attempt to resume the user-facing update flow after replacement.

A successful package replacement may terminate the old process. The new binary therefore owns durable success reconciliation.

On the next valid app entry after the installed package is confirmed to have reached the requested target:

1. route presentation to the main/Lyrics destination;
2. show an `Update successful` dialog once;
3. clear the success marker only after the success feedback has been consumed.

Automatic return to AALyrics after replacement is best-effort only. The UX must not depend on background Activity launch succeeding. If Android does not permit automatic return, the next ordinary user launch still guarantees the one-time success feedback.

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

At minimum, pending-update/success markers and automatic-check preferences require an explicit reset keep/delete/default decision and focused test coverage. Android's per-source install trust remains system-owned and must not be revoked by Reset.

## Implementation order

Implement in bounded checkpoints:

1. separate install-permission runtime state from transient dialog visibility;
2. add the large install-permission explanation dialog and lifecycle behavior;
3. add durable pending/success update reconciliation and one-time success feedback;
4. add best-effort resume-after-update behavior without relying on it for correctness;
5. add the automatic update-check preference and release-available dialog;
6. compose Download + Install into one user-facing Update action while preserving the existing internal state machine;
7. align Previews, Reset behavior, docs, tests, CI, and real-device regression validation.

Do not combine these checkpoints into one large implementation change.
