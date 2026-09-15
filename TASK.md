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
7. update roadmap/migration docs,
8. open PR, run CI, request Codex review, and stop before merge.

## Phase 4 checklist

- [ ] Make track-change identity explicit and independent from playback position/status.
- [ ] Prefer stable playback references when available; otherwise use source media identity, then metadata fallback.
- [ ] Ensure duration/position/playback-state-only changes do not restart lyrics lookup.
- [ ] Clear lyrics ownership when playback no longer has a track.
- [ ] Preserve Spotify playback identity semantics without leaking Spotify logic into lyrics core.
- [ ] Map Android MediaController metadata/state into `PlaybackSnapshot` inside `:platform:media`.
- [ ] Keep Android framework types out of `:core:model`, `:provider:api`, and `:core:lyrics`.
- [ ] Test the pure normalization and lookup-driving logic without an emulator or network.
- [ ] Update durable architecture/roadmap notes.
- [ ] Run CI and Codex review.
- [ ] Stop before merge for explicit approval.

## Exit criteria

Phase 4 is complete when Android media data can be normalized into the existing provider-independent playback model, and the pure lyrics layer can decide whether a playback snapshot starts, preserves, or clears a lookup without depending on Android classes or provider implementations.

Passing Phase 4 does not authorize provider migration. After merge, proceed only to Phase 5 Core Readiness validation, then stop at the documented STOP GATE.
