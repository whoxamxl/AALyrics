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

The current stacked follow-up starts from `feature/package-installer` after the #74 recovery UX was squash-merged at `e97894c0920d40cd3f01f3dd719dd0b52d3b6ad7`. The original Package Installer checkpoint remains the underlying safety baseline; #74's durable recovery, permission-dialog, typed-failure, and successful-update behavior are now inherited requirements for later update UX work.

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

The permission dialog is content-height driven rather than reserving a fixed percentage of the Phone viewport. Normal content therefore ends shortly after the secondary GitHub action instead of leaving unused lower-panel space. The surrounding dialog viewport still constrains oversized content, and the content remains vertically scrollable for narrow or enlarged-font configurations. Its `INSTALL UPDATES` eyebrow/X header uses the shared `PhoneDialogHeader`: one vertically centered row, a 24dp close icon inside the standard 48dp touch target at the trailing edge, and no dialog-specific X offset. The target version is presented immediately below with normal secondary-text contrast.

Install preparation failures remain distinct from missing source trust. The runtime carries a typed install-failure reason through Phone mapping, and the `Installation failed` info tooltip presents a reason-specific explanation for release refresh, retained APK/preflight, package/version/signing, durable recovery persistence, PackageInstaller handoff, and installer rejection/cancellation failures. A failure that occurs before source-trust evaluation must not show the permission dialog. For example, a downloaded APK whose signing identity differs from the installed AALyrics app fails at preflight and reports that signing mismatch explicitly.

## Successful-update feedback

Before PackageInstaller commit, AALyrics persists an app-owned pending-update marker containing enough information to reconcile the requested target after package replacement.

The implemented pending marker is:

```text
PendingUpdate
  targetVersion
  targetVersionCode
  installerSessionId
  resumeAfterUpdate = true
```

`targetVersionCode` comes from the APK package metadata that already passed package/version/signing preflight; it is not inferred from the release tag or filename.

The marker is stored in dedicated app-owned SharedPreferences using synchronous persistence. The write happens after the PackageInstaller session has received and fsynced the APK and immediately before `PackageInstaller.Session.commit()`. It also stores the exact PackageInstaller session ID so recovery cleanup can act only on the marker that belongs to the abandoned or failed session. If the durable write fails, AALyrics fails closed and does not commit the installer session.

The old binary must not clear this marker merely because PackageInstaller reports success. Successful package replacement may terminate that process, so the new binary owns final reconciliation. Terminal installer failure clears the pending marker only when its persisted `installerSessionId` matches the failed session, including when the original process and in-memory status sink are gone. Startup recovery likewise clears the matching pending marker only after it successfully abandons an owned unsealed session left behind before commit. A marker for another session is never cleared by either path. Reset is also protected against the commit-boundary race: if Reset invalidates the operation while the synchronous pending-marker write is in flight, the installer path re-checks the operation generation immediately after the write, clears that session's marker, and aborts before commit. `Reset AALyrics` clears all app-owned update recovery state. Reset does not change Android-owned install-source trust.

The new binary now registers a non-exported `ACTION_MY_PACKAGE_REPLACED` receiver. Its reconciliation step compares the durable pending target with the version actually running after replacement:

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

The success marker stores the new binary's actual version and carries forward the pending resume intent. Promotion removes the pending keys in the same synchronous SharedPreferences commit. If reconciliation or durable promotion fails, no resume request is attempted and recovery remains available for a later valid path.

A successful package replacement may terminate the old process. The new binary therefore owns durable success reconciliation.

On the next valid Phone entry after the installed package is confirmed to have reached the requested target, AALyrics keeps the normal Phone startup destination (`PhoneDestination.Home`, which is Lyrics) and overlays a one-time success dialog:

```text
AALyrics updated

You're now running [ v0.x.x ].

Done
```

No update-specific navigation override is applied. The dialog appears only once the normal entry gates have reached `READY`, so Notification Access and Android Auto compatibility onboarding remain authoritative.

The installed version is presented inline with the supporting copy (`You're now running [version]`) rather than as a separate metadata row; the shared `VersionChip` remains the version visual. The durable `SuccessfulUpdate` marker is loaded into a process-level feedback runtime. It is not cleared when the dialog is first shown. The success dialog uses the same shared `PhoneDialogHeader` X geometry as the permission dialog, so the close affordance remains fixed at the same trailing header position across dismissible custom update dialogs. Done, the top-right close button, and system Back all request the same dismissal. Dismissal clears the durable success marker first and removes the dialog from process state only when that clear succeeds. Outside-tap dismissal is disabled. If the process dies before dismissal, the marker remains and the dialog is shown again on the next valid Phone entry.

Typical, narrow-phone, and enlarged-font Previews cover the success dialog.

After successful reconciliation, the receiver now evaluates the carried `resumeAfterUpdate` intent. When it is true, AALyrics makes one best-effort request to open `MainActivity` using an explicit new-task/clear-top/single-top intent. When it is false, or replacement did not reconcile successfully, no launch request is made.

This is deliberately a launch **request**, not a correctness signal. Android may reject or suppress background Activity launch depending on platform state. A thrown launch failure is contained, and even a request accepted by `startActivity()` is not treated as proof that UI became visible. The durable `SuccessfulUpdate` marker is never cleared or modified by the resume attempt.

If automatic return succeeds, normal entry handling still decides what is displayed; no update-specific destination routing is added. If it does not succeed, the next ordinary user launch still guarantees the one-time success feedback. No notification fallback is implemented in this checkpoint.

## Manual and automatic update discovery

Settings exposes a durable automatic-discovery preference while retaining an explicit manual discovery action:

```text
Automatically check for updates   [i]   [ON/OFF]

Version                     [ v0.x.x ]
                                   Check for updates
```

The preference is app-owned and durable. Its default is **ON**, including users whose existing preferences do not yet contain the key. `Reset AALyrics` restores it to ON.

Automatic checking remains deliberately low-frequency and bounded:

- no automatic network check occurs before the normal Phone entry gates reach `READY`;
- automatic discovery is eligible only when at least **7 full days** have elapsed since the durable cadence timestamp, or when no cadence timestamp exists yet;
- starting an automatic check records the cadence timestamp immediately, so a failed network request does not cause repeated automatic retries after process restarts during the same 7-day window;
- a successful manual GitHub Releases query also refreshes the same cadence timestamp, so manually checking today suppresses redundant automatic discovery for the next 7 days;
- automatic-query completion and install-time refresh do not move that timestamp because the automatic start was already recorded and install refresh is not a discovery cadence event;
- manual `Check for updates` is never blocked by the automatic cadence;
- if the setting was OFF at entry and is switched ON later, an automatic check is requested only if the durable 7-day cadence is due;
- a process-local attempt guard remains as secondary duplicate protection against recomposition, Activity recreation, navigation, or repeated READY rendering;
- automatic discovery never displaces a retained verified APK or active update/install operation;
- `Reset AALyrics` clears both the durable cadence timestamp and the process-local automatic-attempt guard.

### Settings owns manual discovery only

The Settings Version/update row is not an update-process surface. It owns only the user-initiated discovery interaction and its direct result when no newer release is found.

Approved Settings presentation states are:

```text
IDLE
Version                     [ v0.x.x ]
                                   Check for updates

CHECKING
Checking for updates…                              ◌

UP_TO_DATE
Up to date                                         ✓

CHECK_FAILED
Update check failed                        ⓘ   ↻ Retry
```

A manual check that finds a newer eligible release does **not** render `Update available`, Download, progress, Downloaded, Install, permission-required, installing, or install-failure UI in the Settings row. Discovery hands the result to the unified Update Dialog immediately.

Automatic discovery is background-only unless it finds a newer eligible release:

- `AUTOMATIC + Checking` is silent;
- `AUTOMATIC + UpToDate` is silent;
- `AUTOMATIC + Failed` is silent;
- `AUTOMATIC + UpdateAvailable` opens the same unified Update Dialog used by manual discovery.

The internal origin remains meaningful for cadence and notification-suppression policy, not for choosing a different update flow.

### Unified Update Dialog handoff

Both user-visible discovery origins converge at the same presentation boundary:

```text
MANUAL UpdateAvailable ───────┐
                              ├─> Unified Update Dialog
AUTOMATIC UpdateAvailable ────┘
```

The available-state dialog is:

```text
New release available

AALyrics [ v0.x.x ] is ready to download.

Update
Not now
```

The release version remains part of the supporting sentence and uses the shared `VersionChip` in a wrapping layout. The available state keeps the shared `PhoneDialogHeader` close affordance. `Not now`, the close action, and system Back share one dismissal path; outside-tap dismissal is disabled.

Automatic dismissal suppression remains notification-specific: dismissing an automatically discovered version suppresses that exact automatic prompt for the remainder of the current app-process session. A different version remains eligible. An explicit manual `Check for updates` is user intent and may present the same still-current release again even when its automatic prompt was previously suppressed.

Turning `Automatically check for updates` OFF prevents an automatic result from opening or retaining the dialog, including when an already-started automatic query completes later. The toggle does not disable manual discovery.

Durable `SuccessfulUpdate` feedback has modal priority over release-available presentation, and an unconsumed success marker prevents starting an automatic check on that Phone entry.

## Unified update-process presentation

After the user chooses `Update`, the same Update Dialog becomes the presentation owner for the app-owned update process. Settings no longer mirrors those process states.

The target user-visible progression is:

```text
New release available
    -> Update
    -> Preparing download…
    -> Downloading…                    0–100%
    -> Verifying / preparing…
    -> Preparing installation…
    -> install-source permission explanation only if required
    -> PackageInstaller / Android confirmation
    -> Update successful
```

The existing progress semantics move from the Settings row into the dialog rather than being discarded:

- `PREPARING_DOWNLOAD` uses indeterminate progress while assets/checksum/staging are prepared;
- `DOWNLOADING` uses the existing determinate byte-based 0–100% progress;
- checksum verification and promotion remain explicit internal boundaries and receive a verifying/preparing presentation rather than appearing to stall;
- install preparation retains the latest-release refresh and APK package/version/signing preflight;
- missing Android install-source trust is explained in the update flow without bypassing Android Settings or final confirmation;
- recoverable download/install failures remain typed and expose Retry from the update flow;
- a valid retained verified APK remains reusable, so retry/permission return must not force a second download;
- Android's final installation confirmation remains system-owned.

`INSTALL_REFRESH` remains an internal update-process origin. If install-time refresh discovers a newer eligible release, the active dialog returns to the release-available state for that newer release rather than sending the user back to a Settings update row.

The Update Dialog is therefore the single app-owned presentation surface from `UpdateAvailable` through PackageInstaller handoff. `UpdateSuccessfulDialog` remains a separate post-replacement acknowledgement because successful package replacement may terminate the old process and the new binary reconstructs that feedback from durable `SuccessfulUpdate` state.

### Dismissal boundary

Availability and active update work have different dismissal semantics.

Before `Update` starts, `Not now`, close, and Back are ordinary dismissal actions. Once app-owned update work starts, `Not now` is no longer part of the process UI. Active-work, permission-required, and recoverable-failure dismissal/re-entry behavior must preserve the application-owned operation/artifact state and must never require Settings to become a second progress/install surface.

The implementation may evolve the exact close/Back affordance per process phase, but it must satisfy these invariants:

- dismissing presentation never corrupts or silently discards an active operation;
- dismissing permission explanation preserves a valid verified APK;
- recoverable failures preserve reusable artifacts where the existing runtime already does so;
- reopening or retrying resumes from the authoritative application-owned runtime state;
- no process phase reintroduces Download/Install/progress actions into the Settings row.

## Implementation split

The approved stacked implementation is:

1. **#75 — unify discovery handoff**
   - keep Settings manual discovery states: Idle / Checking / Up to date / Check failed;
   - route both MANUAL and AUTOMATIC newer-release results to one Update Dialog;
   - keep automatic-only cadence and same-session notification suppression;
   - allow explicit manual discovery to present a release even when its prior automatic prompt was suppressed;
   - remove Settings as the presentation owner of `UpdateAvailable`.

2. **#76 — unify the complete update process**
   - move the existing preparing/downloading progress presentation into the Update Dialog;
   - chain verified download into install preparation behind the same user-facing `Update` intent;
   - present verification, install preparation, permission-required, installing, typed failure, and Retry states through the dialog;
   - remove Download / Downloaded / Install / update-process progress and failure presentation from Settings;
   - preserve all existing download, SHA-256, retained-artifact, install-refresh, preflight, source-trust, PackageInstaller, and recovery boundaries.

Typical, narrow-phone, and enlarged-font Previews must cover the release-available dialog and representative update-process states. Settings Previews should cover only the manual discovery states plus automatic-check preference ON/OFF; they should not retain parallel update-process fixtures after the migration.

## Reset contract

Every durable state introduced by this UX follow-up must explicitly re-evaluate `Reset AALyrics`.

Both implemented recovery markers — `PendingUpdate` and `SuccessfulUpdate` — are app-owned and are deleted by `Reset AALyrics`, together with active app-owned update work and retained update artifacts. The durable automatic-update-check preference is app-owned and resets to its default value, ON. Its durable 7-day cadence timestamp and process-local attempt guard are also cleared so reset returns automatic discovery to a fresh state. Successful release-query cadence writes are serialized with the update runtime's generation invalidation: if Reset races an in-flight successful query, either the callback finishes first and Reset clears its timestamp afterward, or Reset invalidates the generation first and the stale callback is not allowed to write. A cleared cadence therefore cannot be resurrected by pre-Reset query work. Android's per-source install trust remains system-owned and must not be revoked by Reset.

## Implementation order

The recovery/install safety checkpoints are already established. For the remaining presentation unification work, implement in bounded checkpoints:

1. freeze this unified discovery/update presentation contract in docs;
2. generalize the release-prompt owner from automatic-only presentation to a unified update-dialog presentation owner;
3. route MANUAL and AUTOMATIC newer-release results into the same available-state dialog while preserving origin-specific cadence/suppression semantics;
4. reduce Settings Version/update presentation to Idle / Checking / Up to date / Check failed;
5. move preparing/download progress, verification, retained-artifact, install-preparation, permission-required, installing, typed failure, and Retry presentation into the Update Dialog;
6. compose the existing internal Download + Install stages behind the dialog's one user-facing `Update` action;
7. align Previews, Reset behavior, docs, tests, CI, and focused real-device regression.

Do not weaken or bypass the validated internal update boundaries while changing presentation ownership.
