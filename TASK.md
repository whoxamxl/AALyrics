# Lyrics Demand Gating

## Branch and baseline

- Branch: `feature/lyrics-demand-gating`.
- Base: main `c0bfb15` after PR #29 merged.
- Working-fork reference: `whoxamxl/auto-lyrics` main `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`).
- Classification: **PRESERVE / REFACTOR** for demand semantics; **REWRITE** for AALyrics ownership/integration.
- Authoritative scope: `docs/LYRICS_DEMAND_GATING.md`.
- The user explicitly authorized production implementation of this slice; implementation is active on this branch.

## Acceptance criteria

- Preserve `phone process foreground OR Android Auto projection connected` as the demand rule.
- Keep MediaSession discovery/selection running independently of lyrics demand.
- Gate only the handoff from normalized playback into lyrics lookup/provider work.
- Retain the latest `PlaybackSnapshot` while demand is off.
- `OFF -> ON` immediately replays the latest retained snapshot once.
- `ON -> OFF` clears current lookup ownership once and stops/cancels provider work.
- Phone and automotive demand sources remain independently composable; removing one source must not disable demand while the other remains active.
- Preserve process-level phone lifecycle semantics so configuration changes do not cause provider-work flapping.
- Preserve Android Auto projection-level demand for the whole projection session, not only while AALyrics is the foreground automotive surface.
- Do not make `:platform:media`, providers, `LyricsCoordinator`, or UI composables own demand policy.
- Do not change MediaSession selection, provider behavior, candidate scoring, cache, translation, timing, karaoke rendering, or finished phone/Android Auto presentation.
- Add deterministic regressions defined in `docs/LYRICS_DEMAND_GATING.md`.
- Run `./gradlew test check :app:assembleDebug`, `bash scripts/verify-architecture.sh`, and `git diff --check` before PR review.
- Open a PR, complete bounded review, and stop before merge for explicit approval.

## Plan/status

- [x] Merge live MediaSession runtime in PR #29.
- [x] Create `feature/lyrics-demand-gating` from main `c0bfb15`.
- [x] Re-check working-fork `LyricsDemandController` and `AutoLyricsApp` lifecycle wiring.
- [x] Define demand semantics, ownership, snapshot replay/clear behavior, scope, regressions, and STOP gate in `docs/LYRICS_DEMAND_GATING.md`.
- [x] Implement demand aggregation and gating behind the documented application/runtime boundary.
- [x] Wire process-level phone demand without adding finished phone UI.
- [x] Wire Android Auto projection demand without adding finished automotive UI.
- [x] Add deterministic regressions.
- [x] Update durable docs with the implementation result.
- [ ] Run validation and bounded review.
- [ ] Open PR and stop before merge.

## Scope guard

This is a background/runtime lifecycle slice. It is not a presentation, cache, translation, timing, karaoke-rendering, provider, candidate-selection, or MediaSession-selection slice.
