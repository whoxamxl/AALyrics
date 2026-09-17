# Phone App Shell

## Branch and baseline

- Branch: `feature/phone-app-shell`.
- Base: current `main` at `e009d0b75506ac1632a42c66263f0bf1a6c299b3` after PR #32 merged.
- Classification: **PHONE UI IMPLEMENTATION / SHELL ONLY**.
- Read `AGENTS.md`, `docs/UI_ARCHITECTURE.md`, and `docs/PHONE_UI_SPEC.md` before changing Phone UI.
- The user explicitly authorized implementation of the Phone app shell on this branch.

## Goal

Implement the persistent Phone Compose shell defined by the approved Phone UI architecture, together with deterministic debug-only previews that render the real production composables. This slice is for validating shell composition and vertical space before implementing the real destination screens or runtime wiring.

## Acceptance criteria

- Implement production `PhoneAppShell` under `:ui:phone`.
- Implement production `PhoneTopBar`.
- Implement production `PlaybackControlsBar` with Previous / Play-Pause / Next only.
- Implement production `PhoneNavigationBar` for `Lyrics`, `Sync`, `Details`, and `Settings`.
- Define the minimal `PhoneDestination` and shell presentation state/contracts needed to render the shell.
- Keep `Lyrics` as the home/default destination.
- Keep shell components local to `:ui:phone`; do not prematurely promote them to `:ui:designsystem`.
- Reuse existing `:ui:designsystem` theme/tokens where appropriate.
- Keep production composables in `src/main` and deterministic Preview fixtures in `src/debug`.
- Preview must render the same production shell/components that runtime will later use.
- Add preview scenarios sufficient to judge normal and narrow phone layouts, selected destinations, playing/paused state, and disabled/unavailable playback controls.
- Include a debug-only sample Lyrics body with a Track-Card-like block plus roughly six lyric lines only to evaluate available vertical space.
- Preserve the approved package ownership under `shell/`, `navigation/`, destination packages, and `state/`.
- UI APIs must consume presentation values/callbacks only; no Android media framework objects may leak into `:ui:phone`.
- Run repository validation, review the complete diff, open a PR against `main`, and stop before merge for explicit approval.

## Explicit non-goals

Do **not** implement in this slice:

- real `LyricsScreen` behavior or lyrics state mapping
- real Sync / Details / Settings screen behavior
- ViewModels
- Navigation Compose runtime or back-stack behavior
- `MediaSession` / `MediaController` wiring
- playback transport integration with platform code
- provider/network access
- artwork loading
- follow/manual-browse logic
- final animations or final visual tuning
- Android Auto changes
- cache/translation/timing/karaoke feature implementation

## Plan/status

- [x] Create `feature/phone-app-shell` from current `main`.
- [x] Define this implementation slice in `TASK.md`.
- [x] Implement minimal shell presentation contracts.
- [x] Implement `PhoneTopBar`.
- [x] Implement `PlaybackControlsBar`.
- [x] Implement `PhoneNavigationBar`.
- [x] Compose `PhoneAppShell`.
- [x] Add deterministic debug-only shell previews.
- [x] Validate vertical space with a sample Lyrics preview body.
- [x] Run `./gradlew test check :app:assembleDebug`.
- [x] Run `bash scripts/verify-architecture.sh`.
- [x] Run `git diff --check`.
- [x] Review the complete branch diff.
- [x] Open PR #33 against `main`; fix the status-bar inset P2; complete targeted re-review; stop before merge.

## Scope guard

This branch proves only the persistent Phone shell and its Compose/Preview contracts. It must not grow into destination implementation or runtime/media integration. If a shell API choice requires real destination behavior to justify it, defer that choice to the later destination slice instead of inventing speculative behavior here.
