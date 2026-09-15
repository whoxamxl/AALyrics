# Playback Boundary Task

This branch implements only Phase 4 of `docs/ROADMAP.md`.

## Reference check

Before implementation, the working fork was re-checked at `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`). Relevant behavior reviewed:

- `SpotifyTrackIdentity` — PRESERVE / REFACTOR semantics: only Spotify playback may yield a Spotify track reference; accept explicit `spotify:track:` URIs and `open.spotify.com/track/...` URLs; do not infer a Spotify track reference from a bare 22-character media id.
- `MediaTracker` — REWRITE boundary only. It currently mixes Android metadata, playback state, debounce, lyrics fetching, cache, translation, artwork, timing, and presentation state.
- `MediaListenerService` — REWRITE / REFACTOR later as a thin Android session-selection adapter.
- `LyricsDemandController` — PRESERVE / REFACTOR later. Demand gating is intentionally not folded into metadata normalization or lyrics lookup identity in this phase.

No previous-fork implementation code is copied into AALyrics before the STOP GATE.

## Scope guard

Phase 4 defines the Android playback boundary and the pure lookup-driving boundary only.

Do not add:

- concrete lyrics providers,
- resolver/scoring logic,
- cache or translation,
- phone or Android Auto presentation,
- process-wide demand gating,
- metadata debounce,
- artwork/color extraction,
- provider-specific timeout/networking policy.

## Commit strategy

Keep development commits small and single-purpose. PR-level squash remains separate from branch history.

1. document Phase 4 scope,
2. define explicit playback-track identity in `:core:model`,
3. add a pure playback-to-lyrics lookup controller in `:core:lyrics`,
4. add Spotify playback-reference normalization in `:platform:media`,
5. add Android MediaController → `PlaybackSnapshot` mapping,
6. add platform/core boundary tests,
7. update durable architecture/roadmap notes,
8. open PR, run CI, request Codex review, and stop before merge.

## Phase 4 checklist

- [x] Make track-change identity explicit and independent from playback position/status.
- [x] Prefer stable playback references when available; otherwise use source media identity, then metadata fallback.
- [x] Ensure duration/position/playback-state-only changes do not restart lyrics lookup.
- [x] Clear lyrics ownership when playback no longer has a track.
- [x] Preserve Spotify playback identity semantics without leaking Spotify logic into lyrics core.
- [x] Map Android MediaController metadata/state into `PlaybackSnapshot` inside `:platform:media`.
- [x] Keep Android framework types out of `:core:model`, `:provider:api`, and `:core:lyrics`.
- [x] Test the pure normalization and lookup-driving logic without an emulator or network.
- [x] Update durable architecture/roadmap notes.
- [x] Run a green CI build/test cycle after implementation.
- [ ] Complete Codex review on the final PR head.
- [x] Stop before merge for explicit approval.

## Readiness evidence

- `PlaybackTrackIdentity` separates lookup ownership from timeline/status/rate/duration updates.
- `PlaybackLyricsControllerTest` verifies start/preserve/supersede/clear behavior using a fake `LyricsLookupLifecycle`.
- `SpotifyPlaybackReference` preserves the old fork's explicit-resource rules without moving Spotify logic into `:core:lyrics`.
- `MediaSessionSnapshotNormalizerTest` exercises structural media normalization entirely as a local JVM test.
- `MediaControllerSnapshotAdapter` is the only new Phase 4 source that imports Android media framework classes.
- Initial PR CI exposed that Android-library unit tests need the Kotlin JUnit binding; commit `b4b04ca` fixed that build configuration without changing production behavior.
- CI run `34947737041` passed branch-name validation, debug APK assembly, and all unit tests on commit `0d248fd`.

## Exit criteria

Phase 4 is complete when Android media data can be normalized into the existing provider-independent playback model, and the pure lyrics layer can decide whether a playback snapshot starts, preserves, or clears a lookup without depending on Android classes or provider implementations.

Passing Phase 4 does not authorize provider migration. After merge, proceed only to Phase 5 Core Readiness validation, then stop at the documented STOP GATE.

## Next action

Run CI and Codex review on the final PR head. Address substantive review findings in small commits. When the PR is green and reviewed, stop before merge and wait for explicit approval.
