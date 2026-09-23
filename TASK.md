# In-app Update Download and Integrity Verification

## Branch and baseline

- Branch: `feature/update-download`.
- Base: `main` at `20d6a58301a2cf1d2ba38d07f54135fde99b8e06`.
- Classification: **SETTINGS / RELEASE DOWNLOAD / INTEGRITY VERIFICATION**.
- Authoritative references: `AGENTS.md`, `docs/RELEASES.md`, `docs/PHONE_SETTINGS.md`, the current GitHub Release workflow, and the merged Check-for-updates runtime.

## Goal

Extend the merged Check-for-updates flow by downloading the selected signed APK and verifying its published SHA-256 checksum.

This slice owns exactly four new capabilities:

1. resolve the expected APK and checksum assets from the already-selected GitHub Release;
2. download those two public assets through an application-owned boundary;
3. verify the downloaded APK against the published SHA-256 before accepting it;
4. expose `PREPARING_DOWNLOAD`, `DOWNLOADING`, `DOWNLOADED`, and `DOWNLOAD_FAILED` Settings presentation states.

## Scope boundary

This PR does **not** launch Android Package Installer, request install permissions, verify the installed package/signing certificate, background-check for releases, background-download updates, persist an update channel, or automatically install anything.

Package Installer handoff and signing-identity validation are follow-up slices after download/integrity is proven stable.

## Release asset contract

For a selected release tag:

```text
vMAJOR.MINOR.PATCH[-alpha.N|-beta.N|-rc.N]
```

the release must contain exactly one asset with each expected name:

```text
AALyrics-vMAJOR.MINOR.PATCH[-suffix].apk
AALyrics-vMAJOR.MINOR.PATCH[-suffix].apk.sha256
```

Examples:

```text
AALyrics-v0.2.0-alpha.2.apk
AALyrics-v0.2.0-alpha.2.apk.sha256
```

Rules:

- derive asset names from the selected release tag; do not guess from publication time or arbitrary asset order;
- unrelated assets are ignored;
- a missing APK or checksum asset is a download failure;
- duplicate assets with an expected name are a download failure rather than selecting one arbitrarily;
- both resolved download URLs must be HTTPS;
- asset resolution must remain deterministic and directly unit-testable.

## Download and integrity contract

Download starts only from an explicit user `Download` / retry action after `UPDATE_AVAILABLE`.

The application-owned runtime must:

1. enter `PREPARING_DOWNLOAD` while resolving assets, checksum, and staging;
2. enter `DOWNLOADING` immediately before APK byte transfer;
3. download the checksum and APK from the selected release;
4. report received-byte progress against the GitHub Release asset size;
5. require the completed byte count to match the published asset size;
6. parse exactly one SHA-256 digest from the checksum payload;
7. calculate SHA-256 for the downloaded APK;
8. compare expected and actual digests case-insensitively;
9. promote the verified APK from app-private cache staging into app-private no-backup persistent storage only after the digest matches;
10. enter `DOWNLOADED` only after successful verification.

Transport/protocol failures, asset-contract failures, malformed checksum content, I/O failures, and digest mismatch map to `DOWNLOAD_FAILED`.

A partial or checksum-failed APK must not remain as an accepted final artifact.

## File ownership and lifecycle

Update artifacts are application-owned distribution files, not user documents.

- use app-private storage only; do not request shared-storage permission;
- partial `.part` files live under cache staging, use operation-owned filenames, and are cleaned on failure/cancellation/restart;
- verified APKs live under app-private no-backup persistent storage;
- promotion uses a transient persistent `.promoting` file so interrupted promotion cannot become a completed APK;
- retain at most one verified update APK;
- an active download is application-owned and continues if the user leaves Settings;
- configuration change / Activity recreation must not cancel or restart the download;
- `DOWNLOADING` and a completed `DOWNLOADED` result survive Settings navigation;
- process restart restores `DOWNLOADED` from the retained canonical verified APK when it is still newer than the installed version;
- once the installed version catches up, the retained verified APK is stale and must be deleted;
- no APK download starts automatically on launch, resume, Settings entry, or successful update check.

## Reset AALyrics

This slice introduces app-owned staging and verified update artifacts, so Reset semantics change:

- cancel any active update download;
- delete partial and verified update artifacts owned by the update runtime;
- normalize the update presentation back to its ordinary idle/check lifecycle;
- do not affect externally installed packages or GitHub Releases.

The reset operation must remain application-owned; `:ui:phone` only emits the existing Reset callback.

## Phone presentation contract

This slice activates the update download states:

```text
UPDATE_AVAILABLE
    ↓ Download
PREPARING_DOWNLOAD
    ↓ APK transfer begins
DOWNLOADING
    ↓ verified
DOWNLOADED

failure
    ↓
DOWNLOAD_FAILED
    ↓ Retry
PREPARING_DOWNLOAD
    ↓ APK transfer begins
DOWNLOADING
```

Presentation remains informational/action-oriented only. `DOWNLOADED` does not expose an Install action in this PR.

The existing Check lifecycle remains unchanged:

```text
IDLE
CHECKING
UP_TO_DATE
UPDATE_AVAILABLE
CHECK_FAILED
```

## Ownership

- `:ui:phone`: presentation and callbacks only.
- `:app`: release-asset resolution, download orchestration, file ownership, checksum verification, lifecycle, and reset cleanup.
- GitHub access remains public and unauthenticated; no token or repository secret may be embedded in the APK.
- No provider, playback, Translation, Android Auto, Changelog, legal/help, or release-publishing behavior changes in this slice.

## Acceptance criteria

### Planning / contract

- [x] Create `feature/update-download` from current `main`.
- [x] Replace stale `TASK.md` with the download/integrity slice.
- [x] Define exact release asset naming and deterministic failure rules.
- [x] Define temporary file ownership and download lifecycle.
- [x] Re-evaluate Reset AALyrics for cached update artifacts.
- [x] Keep Package Installer and signing identity out of scope.

### Implementation

- [x] Extend the GitHub Release model/client with public asset metadata.
- [x] Add deterministic APK/checksum asset resolution with focused tests.
- [x] Add checksum parsing and SHA-256 verification with focused tests.
- [x] Add app-private download/file boundary and cleanup rules.
- [x] Extend the application-owned update runtime through download states.
- [x] Wire Download/Retry callbacks and production presentation.
- [x] Integrate Reset AALyrics cleanup/cancellation.
- [x] Align Previews and `docs/RELEASES.md` / `docs/PHONE_SETTINGS.md`.
- [x] Complete real-device verified-download, determinate-progress, and process-restart validation.
- [x] Complete final architecture/build/unit/CI validation and bounded review before merge.

## Current checkpoint

Completed:

1. GitHub Release asset metadata;
2. exact APK/checksum asset resolution;
3. focused asset-contract tests;
4. strict `sha256sum` payload parsing for the exact APK filename;
5. streaming SHA-256 calculation and digest verification;
6. focused checksum parsing / match / mismatch tests;
7. bounded HTTPS update-asset download boundary with HTTPS-only redirect handling;
8. app-private update file ownership with `.part` staging, verified promotion, stale cleanup, and path-ownership guards;
9. focused file-store lifecycle / cleanup tests;
10. application-owned download orchestration from the selected release through checksum fetch, APK staging, digest verification, verified promotion, retry, and failure cleanup;
11. runtime states for `PREPARING_DOWNLOAD`, `DOWNLOADING`, `DOWNLOADED`, and `DOWNLOAD_FAILED` plus focused lifecycle/state-mapping tests;

The asset-resolution, checksum/file-boundary, and runtime-orchestration checkpoints passed build/test validation before final production wiring.

Production wiring is now complete: `AALyricsApplication` owns separate cache staging and no-backup persistent verified-update storage, Settings Download/Retry is connected, Reset cancels update work and clears both storage areas, and Preview/docs are aligned.

The download presentation now separates preparation from transfer: `PREPARING_DOWNLOAD` uses an indeterminate linear progress bar while assets/checksum/staging are prepared, then `DOWNLOADING` switches to determinate 0–100% progress driven by received bytes against the GitHub Release asset size.

12. verified APK retention survives process restart without persisting a separate state record: runtime startup derives `DOWNLOADED` from the retained canonical APK and removes it once the installed version catches up.

Final regression review is complete. The branch remains Draft pending the normal PR review/merge decision. Package Installer remains deferred. Its explicit Install action must re-check the latest eligible Release before handoff so a retained older verified APK is not installed first when a newer update has appeared.

Real-device update-download validation uses the reusable manual `Build` workflow input `update_test_version`. Run the workflow against the branch/ref under test and supply an older published version such as `0.1.0-alpha.1`; CI then emits both ZIP and direct-APK debug test artifacts with that VERSION_NAME override. The reusable version-override fixture is intentionally retained as a base for the follow-up Package Installer slice instead of being tied to PR #71. Because these Build-workflow artifacts use the debug signing identity, the Installer slice must extend the fixture with a controlled same-release-signing test path before treating it as proof of a successful self-update installation.

Regression review hardening completed before merge readiness:

- throttle byte-progress state updates to percentage changes instead of every 16 KiB read;
- suppress late check/download results after Reset cancellation;
- replace verified APKs without deleting the previous retained APK before replacement succeeds;
- enforce installed-channel eligibility when restoring a retained APK;
- store verified APKs under `noBackupFilesDir/updates` so update payloads are not included in Auto Backup;
- clean the earlier branch-test `filesDir/updates` location on startup/reset;
- retain a reusable manual version-override workflow fixture for later update testing.
