# Phone Shell Preview Catalog

## Branch and baseline

- Branch: `feature/phone-shell-previews`.
- Base: current `main` at `307a46bd563ec98460f72cfe92508d3d7a971d0a` after PR #33 merged.
- Classification: **PREVIEW-ONLY VISUAL VALIDATION**.
- Authoritative scope: this file plus `docs/UI_ARCHITECTURE.md` and `docs/PHONE_UI_SPEC.md`.
- The user explicitly authorized this debug-only Preview organization slice.

## Goal

Organize deterministic Android Studio Previews by the production Phone shell component they render. Each Preview must call the real production composable from `src/main`; fixtures and sample destination content remain debug-only.

## Acceptance criteria

- Add `PhoneTopBarPreviews.kt` with no-status, sync-status, loading, long-status, and narrow-width coverage.
- Add `PlaybackControlsBarPreviews.kt` with playing, paused, previous-disabled, next-disabled, all-disabled, and narrow-width coverage.
- Add `PhoneNavigationBarPreviews.kt` with each destination selected independently plus narrow-width coverage.
- Add `PhoneAppShellPreviews.kt` with a small integration set for typical/narrow Lyrics layout, hidden controls, and another selected destination.
- Keep the Track-Card-like block and sample lyric lines debug-only for shell-space inspection.
- Keep deterministic reusable fixtures under `ui/phone/src/debug`.
- Remove the combined `LyricsScreenPreviews.kt` ownership once its shell Preview responsibilities are split.
- Leave all production code under `ui/phone/src/main` unchanged.
- Do not add Compose UI tests or screenshot/golden tests; this slice is for human Android Studio Preview inspection.
- Run repository validation, review the complete diff, open a PR against `main`, and stop before merge for explicit approval.

## Explicit non-goals

Do **not** add or change:

- production state or component APIs for Preview convenience
- real destination behavior or state mapping
- ViewModels or Navigation Compose/runtime navigation
- media-session/controller integration or playback transport wiring
- provider/network logic
- cache, translation, timing, or karaoke implementation
- Android Auto code
- Compose UI tests or screenshot/golden infrastructure

## Plan/status

- [x] Create `feature/phone-shell-previews` from current `main`.
- [x] Read the repository instructions, UI architecture/specification, and production Phone shell.
- [x] Replace `TASK.md` with this Preview-only slice.
- [x] Consolidate deterministic reusable debug fixtures.
- [x] Add focused `PhoneTopBar` Previews.
- [x] Add focused `PlaybackControlsBar` Previews.
- [x] Add focused `PhoneNavigationBar` Previews.
- [x] Add focused `PhoneAppShell` integration Previews and debug-only sample Lyrics body.
- [x] Remove the combined `LyricsScreenPreviews.kt` file.
- [x] Verify no production files changed and no tests were added.
- [x] Run `./gradlew test check :app:assembleDebug`.
- [x] Run `bash scripts/verify-architecture.sh`.
- [x] Run `git diff --check`.
- [x] Review the complete branch diff.
- [ ] Open a PR against `main`, complete bounded review, and stop before merge.

## Scope guard

This branch only improves Preview ownership and visual-inspection coverage for already implemented Phone shell components. Any required production change is a scope boundary: stop and explain it instead of changing `src/main`.
