# Settings update visit lifecycle fix

## Branch and baseline

- Branch: `fix/settings-update-visit-lifecycle`.
- Base: `main` at `005e71b9abe46241d951c6c5c2cdb75a9f3f3a6d`.
- Classification: **SETTINGS / UPDATE LIFECYCLE / REGRESSION FIX**.
- Trigger: Codex P2 review follow-up after PR #69.

## Problem

PR #69 made completed update-check results visit-local, but `SettingsScreen` invoked `onSettingsEntered()` from `LaunchedEffect(Unit)`. A configuration change can recreate the Activity and composition while the user is still in the same Settings visit, which incorrectly normalizes `UP_TO_DATE`, `UPDATE_AVAILABLE`, or `CHECK_FAILED` back to `IDLE`.

## Contract

- Only an actual navigation transition from a non-Settings destination into Settings starts a new Settings visit.
- Configuration changes, Activity recreation, recomposition, Settings subscreen navigation, and Settings-tab reselection while already in Settings remain the same visit.
- A new Settings visit keeps an active `CHECKING` state and normalizes completed results to `IDLE`.
- No update-network, version-selection, persistence, download, or Reset AALyrics semantics change.

## Acceptance criteria

- [x] Move Settings-entry normalization ownership out of `:ui:phone` composition and into the Phone navigation host.
- [x] Preserve completed update-check results when Settings is already the active destination.
- [x] Add focused unit coverage for Settings navigation-entry detection.
- [x] Align deterministic Previews and `docs/PHONE_SETTINGS.md`.
- [x] Run architecture checks, unit tests, debug APK build, CI, and bounded review before merge.

## Current stop point

The lifecycle trigger is corrected and validated. Architecture checks, debug APK build, unit tests, CI, and bounded regression review are complete.
