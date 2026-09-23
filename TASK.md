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
4. expose the existing `DOWNLOADING`, `DOWNLOADED`, and `DOWNLOAD_FAILED` Settings presentation states.

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

1. enter `DOWNLOADING`;
2. download the checksum and APK from the selected release;
3. parse exactly one SHA-256 digest from the checksum payload;
4. calculate SHA-256 for the downloaded APK;
5. compare expected and actual digests case-insensitively;
6. move the verified APK into its final app-private cache location only after the digest matches;
7. enter `DOWNLOADED` only after successful verification.

Transport/protocol failures, asset-contract failures, malformed checksum content, I/O failures, and digest mismatch map to `DOWNLOAD_FAILED`.

A partial or checksum-failed APK must not remain as an accepted final artifact.

## File ownership and lifecycle

Update artifacts are application-owned temporary distribution files, not user documents.

- use an app-private cache subdirectory; do not request shared-storage permission;
- partial files use temporary names and are cleaned on failure/cancellation;
- retain at most the currently verified update APK for the active application process/slice;
- an active download is application-owned and continues if the user leaves Settings;
- configuration change / Activity recreation must not cancel or restart the download;
- `DOWNLOADING` and a completed `DOWNLOADED` result survive Settings navigation within the same application process;
- process death does not require restoring download state in this slice; stale update-cache artifacts may be cleaned when the update runtime initializes;
- no APK download starts automatically on launch, resume, Settings entry, or successful update check.

## Reset AALyrics

This slice introduces an app-owned cached artifact, so Reset semantics change:

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
- [ ] Run architecture checks, unit tests, debug APK build, CI, real-device verification, and bounded review before merge.

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
11. runtime states for `DOWNLOADING`, `DOWNLOADED`, and `DOWNLOAD_FAILED` plus focused lifecycle/state-mapping tests.

The asset-resolution, checksum/file-boundary, and runtime-orchestration checkpoints passed build/test validation before final production wiring.

Production wiring is now complete: `AALyricsApplication` owns the download client/file store, Settings Download/Retry is connected, Reset cancels update work and clears update cache, and Preview/docs are aligned.

The download presentation now separates preparation from transfer: `PREPARING_DOWNLOAD` retains the compact circular activity indicator while assets/checksum/staging are prepared, then `DOWNLOADING` switches to an indeterminate linear progress bar when APK transfer begins.

Remaining work is final architecture/build/unit/CI validation, real-device verified-download testing, bounded review, and merge readiness. Package Installer remains deferred.

For real-device validation only, PR #71 temporarily builds an additional debug APK with `AALYRICS_VERSION_NAME=0.1.0-alpha.1` so the public `v0.2.0-alpha.1` Release is discoverable as a newer update. This CI-only scaffolding must be removed after device validation and before merge.
