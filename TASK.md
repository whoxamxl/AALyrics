# Phone Lyrics Screen

## Branch and baseline

- Branch: `feature/phone-lyrics-screen`.
- Base: `main` at `f49189c823e5f0a68f4c9f2b5bc82f40f163f7c1` after PR #43 merged.
- Classification: **PHONE LYRICS SCREEN COMPOSITION**.
- Authoritative references: `AGENTS.md`, `docs/PHONE_UI_SPEC.md`, and `docs/PHONE_LYRICS_VIEWPORT.md`.
- The user explicitly authorized implementation.

## Goal

Compose the production Phone Lyrics destination from the already implemented `TrackCard` and `LyricsViewport`, then use that same production screen inside debug Previews and the existing `PhoneAppShellPreviews`.

This slice establishes the final presentation boundary before runtime/media/provider wiring.

## Accepted direction

- `PhoneAppShell` continues to own persistent TopBar, floating Playback Controls, Bottom Navigation, and the bottom overlay inset exposed to destination content when playback controls are present.
- `LyricsScreen` owns only Lyrics-destination content:
  - Track Card
  - Lyrics viewport
- Do not duplicate persistent playback controls inside the Lyrics screen.
- Keep Track Card and LyricsViewport as the production components already approved.
- Preserve the LyricsViewport internal 20dp horizontal lyric inset; do not wrap the whole viewport in another horizontal screen padding.
- Align the Track Card visually with the viewport using compact destination padding.
- Keep the viewport flexible with `weight(1f)` so actual remaining height determines lyric visibility/focus behavior.
- Artwork remains caller-owned and optional.
- Runtime/media/provider lookup and state mapping remain outside this slice.

## Acceptance criteria

- [x] Add immutable `LyricsScreenUiState` composed of Track Card and LyricsViewport presentation state.
- [x] Implement production `LyricsScreen`.
- [x] Forward LyricsViewport browse/follow interaction changes through the screen API.
- [x] Add deterministic LyricsScreen Previews for LINE, WORD, PLAIN, long track metadata, and constrained height.
- [x] Replace the fake Lyrics body in `PhoneAppShellPreviews` with the production `LyricsScreen`.
- [x] Update the narrow shell Preview to render the production Lyrics destination.
- [x] Validate composition ownership and reserve the shell-provided playback overlay inset so LyricsViewport interactions remain clear of the floating controls.
- [x] Update durable Phone UI docs with the production LyricsScreen composition.
- [ ] Run final-head CI/review on PR #42 and stop before merge for explicit approval.

## Scope guard

This branch does not add ViewModels, navigation runtime, media-session wiring, provider/network behavior, artwork loading, translation presentation, Settings UI, Sync/Details implementation, or Android Auto changes.
