# In-app Package Installer Handoff

## Branch and baseline

- Branch: feature/package-installer.
- Base: main at 0408033accadad8f52667e0e5e22152dcba6661f.
- Classification: SETTINGS / UPDATE INSTALL / ANDROID PACKAGE INSTALLER.
- Authoritative references: AGENTS.md, docs/RELEASES.md, docs/PHONE_SETTINGS.md, the merged update-download runtime, and Android PackageInstaller contracts.

## Goal

Extend the merged verified-update flow from DOWNLOADED through an explicit, user-confirmed Android package update handoff.

This slice owns:

1. validating the retained APK as an AALyrics update before installer handoff;
2. refreshing eligible GitHub Releases when the user explicitly presses Install so a stale retained update is not intentionally installed first;
3. handling Android's per-source package-install trust requirement;
4. streaming the verified APK into PackageInstaller.Session;
5. exposing install preparation / permission / failure states in Phone Settings while retaining the verified APK for retry;
6. extending the existing update-test fixture so a same-release-signing self-update can be validated on a real device.

## Scope boundary

This PR does not add background update checks, background downloads, unattended/silent installation, automatic installation after download, a persistent update-channel preference, or Play-distribution behavior.

Installation remains explicitly user-triggered. AALyrics must not bypass Android package-install confirmation or source-trust controls.

The existing download and SHA-256 verification contract remains unchanged and is an input to this slice.

## Installer entry contract

Install is available only while a canonical verified APK is retained and update state is DOWNLOADED.

~~~text
DOWNLOADED
    ↓ Install
PREPARING_INSTALL
    ↓ retained APK still latest eligible + preflight valid
INSTALL_PERMISSION_REQUIRED   (only when source trust is not granted)
    ↓ user grants permission and returns
PREPARING_INSTALL
    ↓ PackageInstaller session committed
INSTALLING / system confirmation
    ↓ cancel/failure
INSTALL_FAILED
    ↓ Retry
PREPARING_INSTALL
~~~

A successful self-update may replace/stop the current process before an in-process terminal state is durable. On next launch, existing installed-version reconciliation removes the stale retained APK once the installed version has caught up.

## Install-time Release refresh

Pressing Install must explicitly refresh the public GitHub Release collection before package-manager handoff.

Rules:

- reuse the existing release grammar and installed-channel eligibility rules;
- compare the retained verified APK version with the latest currently eligible release;
- if the retained version is still latest eligible, continue install preparation;
- if a newer eligible release exists, do not intentionally install the older retained APK first;
- return to the ordinary update/download path for the newer release;
- preserve the existing verified artifact until a newer verified download successfully replaces it;
- if refresh fails, do not hand the APK to Package Installer; expose a retryable install-preparation failure;
- do not perform this refresh in the background, on Settings entry, or merely because DOWNLOADED was restored.

## APK preflight contract

Before creating an install session, inspect the retained APK through an application-owned Android package boundary.

Required checks:

- retained file still exists and is the canonical verified artifact;
- archive metadata can be parsed;
- package name is io.github.whoxamxl.aalyrics;
- archive Android version is newer than the currently installed package;
- archive signing identity is update-compatible with the installed AALyrics package;
- archive target/version agrees with the update runtime's retained target.

Package, version, archive, or signing validation failure must fail closed and must not create or commit an installer session.

SHA-256 proves the retained bytes match the GitHub Release asset. Package/signing preflight separately proves that the APK is intended and compatible as an Android update of AALyrics.

## Package-install source trust

AALyrics targets Android 8.0+ and must declare android.permission.REQUEST_INSTALL_PACKAGES before requesting package installation.

Before session handoff, application wiring checks PackageManager.canRequestPackageInstalls().

If source trust is not granted:

- expose INSTALL_PERMISSION_REQUIRED;
- only an explicit user action opens Android's per-app unknown-source settings for AALyrics;
- on return, re-check platform state rather than assuming permission was granted;
- declining/leaving without granting permission preserves the verified APK for later retry;
- never toggle or spoof the platform setting.

This system-owned trust state is outside Reset AALyrics.

## PackageInstaller contract

Use android.content.pm.PackageInstaller.Session. Do not build this feature around deprecated Intent.ACTION_INSTALL_PACKAGE.

The application-owned installer boundary must:

1. create a full-install session for the retained verified APK;
2. stream APK bytes into the session;
3. finish/sync the session write before commit;
4. commit through an IntentSender status callback;
5. handle STATUS_PENDING_USER_ACTION through the system-provided confirmation intent;
6. map terminal failure/cancellation to a retryable app state while preserving the verified APK;
7. treat next-launch installed-version reconciliation as the durable cleanup proof after a successful self-update.

The user remains in control of Android's final confirmation.

## Installer state and artifact lifecycle

The verified APK remains the retry source until:

- a newer verified APK successfully replaces it;
- Reset AALyrics clears app-owned update artifacts;
- the installed version catches up/passes the retained release;
- preflight proves the retained artifact is no longer a valid AALyrics update and the implementation explicitly invalidates it.

Installer cancellation or ordinary installer failure must not delete an otherwise-valid verified APK.

Any app-owned temporary PackageInstaller session/staging resource requires explicit cleanup/abandon behavior.

## Reset AALyrics

This slice re-evaluates Reset because PackageInstaller introduces additional app-owned transient state.

Reset must:

- invalidate/cancel active update check/download/install preparation owned by AALyrics;
- abandon any install session still under AALyrics control when practical;
- delete app-owned partial, promotion, and retained verified update artifacts under the existing update reset contract;
- return update/install presentation to IDLE;
- not revoke Android's per-source install trust;
- not undo/downgrade a package already installed by Android;
- not attempt to override system-owned confirmation behavior beyond platform-supported session cancellation/abandon.

ui:phone remains presentation/callback-only; Reset execution remains application-owned.

## Phone presentation contract

The Version/update block extends DOWNLOADED with an explicit Install action.

Target states:

~~~text
IDLE
CHECKING
UP_TO_DATE
UPDATE_AVAILABLE
CHECK_FAILED
PREPARING_DOWNLOAD
DOWNLOADING
DOWNLOADED
DOWNLOAD_FAILED
PREPARING_INSTALL
INSTALL_PERMISSION_REQUIRED
INSTALLING
INSTALL_FAILED
~~~

Initial presentation intent:

~~~text
Update downloaded v0.2.0-alpha.2          Install

Preparing installation…
[indeterminate linear progress]

Installation permission required           Open settings

Installing update…
[indeterminate linear progress / system confirmation handoff]

Installation failed                   ⓘ   ↻ Retry
~~~

Exact copy/layout can be refined during the Phone presentation checkpoint, but these state/action semantics are the contract.

## Ownership

- ui:phone: installer state presentation and semantic callbacks only.
- app: release refresh, APK/package/signing preflight, platform install-trust state, PackageInstaller session lifecycle, result mapping, retained-artifact lifecycle, and Reset integration.
- Android PackageInstaller and unknown-source settings remain platform-owned.
- GitHub access remains public/unauthenticated; no GitHub token or repository secret may be embedded in the APK.
- No provider, playback, Lyrics, Translation, Android Auto rendering, Changelog, legal/help, or release-publishing behavior changes in this slice except the CI-only validation fixture below.

## Real-device self-update validation fixture

The existing manual Build workflow input update_test_version remains useful for update discovery/download testing, but its debug-signed APK cannot prove replacement by a release-signed GitHub Release.

This slice must extend it with a controlled test path that:

- builds an older-version AALyrics APK using the same release signing identity as durable Release APKs;
- does not publish that old APK as a GitHub Release;
- emits it only as a short-retention Actions artifact for explicit real-device validation;
- uses a sufficiently low Android versionCode so the published release is a valid upgrade target;
- never exposes keystore material or signing secrets in artifacts/logs.

~~~text
install older same-release-signed test APK
    ↓
Check for updates
    ↓
Download + SHA-256 verify current published release
    ↓
Install
    ↓
Android confirmation
    ↓
new release-signed AALyrics replaces old build
    ↓
next launch removes stale retained APK
~~~

## Implementation checkpoints

1. Contract/docs.
2. APK preflight boundary and focused tests.
3. Install-time latest-release refresh.
4. Package-install source trust and settings return/recheck.
5. PackageInstaller session boundary and status callback.
6. Runtime + Phone presentation + Reset + Previews.
7. Same-release-signing real-device test fixture.
8. Final architecture/unit/build/CI/device validation and bounded review.

## Acceptance criteria

### Planning / contract

- [x] Create feature/package-installer from current main.
- [x] Replace stale download-slice TASK.md with the installer slice.
- [x] Define install-time Release refresh behavior.
- [x] Define APK package/version/signing preflight.
- [x] Define platform source-trust handling.
- [x] Define PackageInstaller session ownership and failure behavior.
- [x] Re-evaluate Reset AALyrics for installer state.
- [x] Define same-release-signing real-device validation requirements.
- [ ] Align docs/RELEASES.md and docs/PHONE_SETTINGS.md.

### Implementation

- [ ] Add APK preflight boundary and focused tests.
- [ ] Add install-time Release refresh and stale-retained-release handling.
- [ ] Add source-trust permission/settings handoff.
- [ ] Add PackageInstaller session boundary and status handling.
- [ ] Add runtime states and Phone Install/Retry actions.
- [ ] Integrate Reset with installer/session lifecycle.
- [ ] Add/update Previews and presentation coverage.
- [ ] Extend CI with a same-release-signing update-install test artifact.
- [ ] Complete real-device install validation.
- [ ] Complete final architecture/build/unit/CI validation and bounded review before merge.

## Current checkpoint

Planning checkpoint only. No Package Installer implementation has started yet.

Next checkpoint: align docs/RELEASES.md, commit, then align docs/PHONE_SETTINGS.md separately.
