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

The validated **pre-#75 update-process baseline** remains:

```text
Check for updates
    -> Download
    -> Downloaded
    -> Install
    -> Android confirmation
```

#75 changes only discovery handoff: MANUAL and AUTOMATIC newer-release discovery now converge on the same shared `New release available` dialog. After the user presses `Update`, the existing Settings-owned download/install process presentation remains in place until #77. The split Download -> Downloaded -> Install route remains the validated process checkpoint underneath #75. PR #76 / `feature/one-step-update` is retained only as a legacy/reference prototype and is not the baseline for #77.

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

### #75 current permission-state ownership

Missing install-source trust is an application/runtime fact represented by `AppUpdateCheckState.InstallPermissionRequired`.

In #75, the validated #74 presentation is intentionally still in place:

- Settings may show the permission-required process state after the user has already entered the existing download/install route;
- `UpdateInstallPermissionPromptRuntime` separately owns transient visibility of the large permission explanation dialog;
- dismissing that transient dialog does not clear the authoritative permission-required runtime state or retained verified APK;
- invoking Install again while trust is still missing may request the explanation again;
- `Grant permission` opens Android's per-app source-trust Settings;
- returning from Android Settings re-checks platform trust rather than assuming permission was granted;
- granted trust resumes the same retained-artifact update flow;
- `Download from GitHub` remains an explicit external fallback and never bypasses Android confirmation;
- Reset clears app-owned update state/artifacts but does not revoke Android-owned install-source trust.

### #77 target permission presentation

#77 moves the existing permission explanation into the Unified Update Dialog and removes the parallel Settings process presentation. The runtime fact remains separate from transient presentation visibility.

Required #77 behavior:

- `AppUpdateCheckState.InstallPermissionRequired` remains authoritative;
- entering permission-required state must not discard or redownload a valid retained APK;
- the Unified Update Dialog presents the explanation and explicit `Grant permission` / GitHub fallback actions;
- returning from Android Settings re-checks `PackageManager.canRequestPackageInstalls()`;
- denied trust leaves the update recoverable;
- granted trust resumes install preparation from the retained artifact;
- #77 must not reintroduce a second Settings-owned Install/permission route.

Typical, narrow-width, and enlarged-font Previews should cover this permission-required dialog state as part of the #77 Unified Update Dialog suite. Runtime tests continue to cover denied and granted source-trust return paths.

The permission presentation remains content-height driven and scrollable when necessary. Its dismissible form uses the shared `PhoneDialogHeader` geometry: one vertically centered row, a 24dp close icon inside the standard 48dp touch target at the trailing edge, and no phase-specific X offset.

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
- if the user explicitly starts a manual check while an automatic release query is already in flight, that in-flight query is promoted to MANUAL presentation semantics rather than swallowing the tap or starting a duplicate network request; the user immediately sees Checking and receives the manual Up to date / Check failed / release-dialog result;
- if the setting was OFF at entry and is switched ON later, an automatic check is requested only if the durable 7-day cadence is due;
- a process-local attempt guard remains as secondary duplicate protection against recomposition, Activity recreation, navigation, or repeated READY rendering;
- automatic discovery never displaces a retained verified APK or active update/install operation;
- `Reset AALyrics` clears both the durable cadence timestamp and the process-local automatic-attempt guard.

### #75 discovery presentation and #77 Settings end state

For **discovery**, Settings owns only the user-initiated MANUAL states:

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

In #75, a MANUAL or AUTOMATIC discovery result that finds a newer eligible release does **not** render `Update available` or a Download action in Settings. Both user-visible discovery origins use the shared release-available dialog. Automatic Checking / Up to date / Failed remain silent.

#75 intentionally keeps the already-validated post-`Update` process presentation in Settings: Preparing download, Downloading/progress, Downloaded/Install, permission-required, installing, process failures/Retry, and the install-refresh retarget handoff remain there until #77. If `INSTALL_REFRESH` discovers a newer eligible release while preparing installation, Settings shows `Newer update available -> Download` for that replacement target. This is a process-recovery/retarget state, not a MANUAL/AUTOMATIC discovery presentation.

#77 is the migration that makes Settings a manual-discovery-only surface for the **entire** update lifecycle by moving those post-Update process states into the Unified Update Dialog.

The internal origin remains meaningful for cadence and notification-suppression policy, not for choosing a different release-available dialog.

### Shared release-available dialog handoff

Both user-visible discovery origins converge at the same presentation boundary:

```text
MANUAL UpdateAvailable ───────┐
                              ├─> Shared release-available dialog
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

## #77 target: Unified update-process presentation

In #75, pressing `Update` leaves the release-available dialog and enters the existing Settings-owned update-process presentation.

In #77, accepting `Update` promotes the shared release-available presentation into the Unified Update Dialog, which becomes the presentation owner for the app-owned update process. Settings stops mirroring those process states.

The target user-visible progression is:

```text
New release available
    -> Update
    -> Preparing download…
    -> Downloading…                    0–100%
    -> Verifying / preparing…
    -> DOWNLOADED / Ready to install
    -> Install
    -> Preparing installation…
    -> install-source permission explanation only if required
    -> PackageInstaller / Android confirmation
    -> Update successful
```

The first #77 implementation checkpoint defined this as a **pure presentation contract only**. The second checkpoint now moves the active download preparation/transfer presentation into the Unified Update Dialog while leaving download/install runtime semantics unchanged. Discovery availability and install-refresh retarget availability remain explicitly distinguished because `Not now` / ordinary pre-update dismissal semantics apply only to discovery availability; an install-refresh retarget occurs after app-owned update work has already started.

The existing progress semantics move from the Settings row into the dialog rather than being discarded:

- `PREPARING_DOWNLOAD` now renders as indeterminate progress in the Unified Update Dialog while assets/checksum/staging are prepared;
- `DOWNLOADING` now renders the existing determinate byte-based 0–100% progress in the Unified Update Dialog;
- Settings no longer renders either of those active download phases and its corresponding progress fixtures/copy have been removed;
- `VerifyingDownload(versionName)` is an explicit application-owned runtime state entered immediately before SHA-256 verification. Its presentation ownership is already on the Unified Update Dialog so the dialog does not disappear after transfer completion, but it temporarily reuses the preparing-style indeterminate presentation;
- the next #77 checkpoint gives `VerifyingDownload` dedicated verification copy/presentation so 100% transfer does not appear stalled or ambiguously return to preparation; checksum verification and verified-artifact promotion semantics remain unchanged;
- install preparation retains the latest-release refresh and APK package/version/signing preflight;
- missing Android install-source trust is explained in the update flow without bypassing Android Settings or final confirmation;
- recoverable download/install failures remain typed and expose Retry from the update flow;
- a valid retained verified APK remains reusable, so retry/permission return must not force a second download;
- Android's final installation confirmation remains system-owned.

`INSTALL_REFRESH` remains an internal update-process origin. In #75, a newer eligible release found during install refresh is handed back to the existing Settings process surface as `Newer update available -> Download`, and that state survives later Settings re-entry. In #77, after update-process ownership moves to the Unified Update Dialog, the active dialog instead returns to its release-available state for the newer release.

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

2. **#76 — legacy/reference one-step prototype**
   - retain PR #76 / `feature/one-step-update` as a historical implementation reference;
   - do not use it as the active base for #77 or #78;
   - any reused idea or code must be re-evaluated against the current post-#75 baseline;
   - #77 has intentionally adopted the explicit `VerifyingDownload` runtime boundary, but not #76's Settings-owned verification UI;
   - use #76's verification/permission/retry/retarget tests and Previews only as scenario references: recreate applicable cases against #77's two-stage Unified Update Dialog, while one-step assertions belong exclusively to #78.

3. **#77 — unify the complete update-process presentation while preserving the validated two-stage route**
   - move the existing preparing/downloading progress presentation into the Unified Update Dialog;
   - add explicit verification/preparation presentation around the existing SHA-256 boundary without changing verification semantics;
   - keep `DOWNLOADED` as the verified-artifact boundary and render a clear `Ready to install` state;
   - keep an explicit user-facing `Install` action after `DOWNLOADED`;
   - present install refresh, install preparation, permission-required, installing, typed failure, and Retry states through the dialog;
   - remove Download / Downloaded / Install / update-process progress and failure presentation from Settings;
   - preserve the existing independent `downloadUpdate()` and `installUpdate()` stages and all existing download, SHA-256, retained-artifact, install-refresh, preflight, source-trust, PackageInstaller, and recovery boundaries;
   - complete real-device E2E validation of the full two-stage route before any automatic Download -> Install continuation is introduced.

   #77's validated user-facing baseline is therefore:

   ```text
   Update
     -> Preparing / Downloading / Verify
     -> Downloaded / Ready to install
     -> Install
     -> Install refresh / Preflight / Permission if required
     -> Android PackageInstaller confirmation
     -> Update successful
   ```

4. **#78 — compose the validated two-stage route into one-step UX**
   - start from the validated #77 implementation;
   - treat old #76 as a scenario/design reference only: its continuation-intent idea, retry/permission/retarget scenarios, and single user-facing Update concept are useful, but its process-local boolean, Settings-owned presentation, stale prompt ownership, and outdated mapper/runtime details are not authoritative;
   - keep `DOWNLOADED` as a real internal/recovery state even if it is transient in normal production UX;
   - keep `downloadUpdate()` and `installUpdate()` independently testable and reusable;
   - introduce explicit application-owned one-step continuation intent/coordinator above those operations rather than copying old #76's raw `oneStepUpdateRequested` flag;
   - arm continuation only from explicit user Update/Retry intent;
   - continue from verified `DOWNLOADED` into the existing install stage only when continuation intent is valid and no install/preflight/session operation is already active;
   - use an idempotent continuation claim/guard so recomposition, Activity recreation, restored state, permission return, or duplicate state collection cannot start installation twice;
   - preserve source-trust pause/resume and retained-artifact reuse without redownload;
   - if install refresh finds a newer release, retarget the same one-step operation to the newer candidate rather than installing the stale retained APK;
   - stop automatic progression on recoverable failure and require explicit Retry to re-arm/resume the one-step operation;
   - define process-recovery semantics for continuation intent and clear it under Reset AALyrics;
   - adapt old #76 one-step tests only as scenario references against the current #77/#78 runtime contract;
   - remove the second user-facing Install action only after focused tests pass, then validate the one-step UX separately on-device.

Typical, narrow-phone, and enlarged-font Previews must cover the shared release-available dialog and representative #77 process states, including `Ready to install`. Settings Previews should cover only the manual discovery states plus automatic-check preference ON/OFF; they should not retain parallel update-process fixtures after #77 migration.

## Reset contract

Every durable state introduced by this UX follow-up must explicitly re-evaluate `Reset AALyrics`.

Both implemented recovery markers — `PendingUpdate` and `SuccessfulUpdate` — are app-owned and are deleted by `Reset AALyrics`, together with active app-owned update work and retained update artifacts. The durable automatic-update-check preference is app-owned and resets to its default value, ON. Its durable 7-day cadence timestamp and process-local attempt guard are also cleared so reset returns automatic discovery to a fresh state. Successful release-query cadence writes are serialized with the update runtime's generation invalidation: if Reset races an in-flight successful query, either the callback finishes first and Reset clears its timestamp afterward, or Reset invalidates the generation first and the stale callback is not allowed to write. A cleared cadence therefore cannot be resurrected by pre-Reset query work. Android's per-source install trust remains system-owned and must not be revoked by Reset.

## Implementation order

The recovery/install safety checkpoints are already established. Continue in bounded PR-scoped checkpoints:

### #75

1. freeze the discovery/presentation contract in docs;
2. generalize the release-prompt owner from automatic-only presentation to a unified release-dialog owner;
3. route MANUAL and AUTOMATIC newer-release results into the same available-state dialog while preserving origin-specific cadence/suppression semantics;
4. validate the final #75 discovery behavior without changing update-process ownership.

### #77

1. define the pure Unified Update Dialog presentation model and mapper without changing ownership;
2. move preparing/download progress from Settings into the Unified Update Dialog;
3. connect explicit `VerifyingDownload` to the dialog without changing the existing SHA-256 boundary;
4. render verified `DOWNLOADED` as `Ready to install` with an explicit `Install` action;
5. move install preparation, permission-required, installing, typed failure, and Retry presentation into the dialog;
6. move install-refresh retarget presentation into the dialog using its process-retarget availability context;
7. remove update-process presentation from Settings only after the corresponding dialog presentation is active;
8. align Previews, Reset behavior, docs, and focused tests;
9. complete full real-device E2E validation of the two-stage route from Update through Downloaded -> Install -> Android confirmation -> replacement/recovery.

### #78

1. freeze the validated #77 two-stage route as the immutable pre-#78 comparison baseline;
2. define application-owned continuation intent/coordinator semantics, using old #76's one-step intent only as a conceptual reference;
3. route explicit Update/Retry actions through the coordinator while preserving independently callable `downloadUpdate()` and `installUpdate()`;
4. auto-continue only after verified `DOWNLOADED`, guarded by an idempotent single-consumer/active-install check;
5. preserve source-trust pause/resume, retained-artifact recovery, and no-redownload behavior;
6. preserve install-refresh retargeting within the same one-step operation when a newer candidate appears;
7. stop on recoverable failure and require explicit Retry rather than automatically looping;
8. define restart/process-recovery behavior for continuation intent and clear it under Reset;
9. adapt old #76 tests as scenario references for current runtime behavior, not as code/presentation to copy;
10. remove the second production Install action only after focused tests prove orchestration correctness;
11. align Unified Update Dialog one-step Previews/docs while keeping the two-stage route covered internally;
12. validate the one-step production UX separately on-device.

Do not weaken, bypass, or collapse the validated internal update boundaries merely to simplify user-facing presentation.

