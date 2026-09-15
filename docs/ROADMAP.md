# AALyrics Core-First Roadmap

## Purpose

This roadmap prevents the project from drifting back into provider-driven architecture while also avoiding unnecessary reinvention of behavior that is already proven in the working Auto Lyrics fork.

AALyrics is built core-first, but the previous fork is an active behavioral reference throughout development. Before implementing non-trivial behavior, check `docs/MIGRATION_INVENTORY.md` and the current fork to determine whether the behavior should be preserved, refactored, rewritten, or dropped.

Implementation code from the previous fork is still not imported before the final stop gate below. Until then, the fork is used to inform boundaries, tests, and migration decisions.

## Completed

### Phase 0 — Foundation ✅

- Greenfield repository and application identity
- Module boundaries
- CI and branch naming checks
- Source-available licensing and contribution policy

### Phase 1 — Domain model ✅

- Track and track references
- Playback snapshot/status/source
- Plain, line-timed, and word-timed lyrics
- Normalized synchronization type
- Timing invariants and unit tests

### Phase 2 — Provider contract ✅

- Provider identity and descriptor
- Provider-independent request
- Normalized lyrics candidate
- Suspending provider search contract
- Contract tests

This phase defines only the boundary. It does not include any concrete provider implementation.

### Phase 3.1 — Lyrics state ✅

- provider-independent lookup identity
- explicit idle/loading/ready/degraded/not-found/failure states
- pure lifecycle reduction
- stale-result rejection by lookup identity
- lifecycle invariants and unit tests

Merged in PR #6.

### Phase 3.2 — Candidate selection boundary ✅

- provider-independent `CandidateSelector`
- normalized `Track` + `LyricsCandidate` inputs
- explicit selection preferences
- nullable no-winner result
- no scoring/matching implementation
- fake-selector contract tests

Merged in PR #8. The mature `LyricsProviderResolver` remains reserved for post-gate adaptation behind this boundary.

### Phase 3.3 — Lyrics coordinator ✅

- fresh request identity for every lookup
- concurrent provider fan-out
- partial provider failure isolation
- cancellation propagation
- no-result and terminal-failure behavior
- cancellation/supersession on a newer lookup
- stale-result protection through lookup identity and the reducer
- one handoff to `CandidateSelector` after candidates are collected
- fake-provider/fake-selector tests only

Merged in PR #9. Before implementation, the working fork was re-checked at `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`) so mature behavior was not reimplemented unnecessarily.

### Phase 3.4 — Core integration/readiness tests ✅

The pure provider-independent domain flow is covered end-to-end with fake providers and a fake selector. Validated behavior includes provider-order independence, failure isolation, no-result/degraded/failure states, fresh lookup identity, clear ownership, and stale-result rejection. PR #10 added integration/readiness coverage. PR #11 then fixed and regression-tested a concurrent stale-publication race by making all `LyricsState` transitions atomic through `MutableStateFlow.update`.

No concrete provider or resolver implementation was introduced.

### Phase 4 — Playback boundary ✅

Merged in PR #12.

The Android playback edge is now separated from the pure lyrics core:

```text
Android MediaController
        ↓
MediaControllerSnapshotAdapter       (:platform:media)
        ↓
PlaybackSnapshot                     (:core:model)
        ↓
PlaybackTrackIdentity                (:core:model)
        ↓
PlaybackLyricsController             (:core:lyrics)
        ↓
LyricsLookupLifecycle
        ↓
LyricsCoordinator
```

Phase 4 established explicit track ownership, stable-reference/source-media/metadata identity fallback, Spotify playback-reference normalization at the platform edge, and JVM tests proving that position/status/duration-only churn does not restart lyrics lookup. Android media framework types remain inside `:platform:media`.

## Current work

### Phase 5 — Core Readiness validation

Phase 5 is validation-first. It adds no concrete provider or resolver implementation.

Current validation includes:

- executable CI architecture checks for pure Kotlin/JVM core modules,
- one shared `LyricsState` contract for phone and automotive features,
- feature ownership guards preventing provider fetching/ranking in UI modules,
- Android MediaSession / MediaController confinement to `:platform:media`,
- provider-specific behavior exclusion from pure core,
- existing regression evidence for provider order, stale-result rejection, failure isolation, and playback identity,
- migration-inventory re-review against `whoxamxl/auto-lyrics` `main` at `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`).

Detailed evidence is recorded in `docs/CORE_READINESS_GATE.md`.

If any validation fails, the project stays in core-first development.

## Next — STOP GATE

### Concrete provider / code adaptation

**Stop here for explicit review.**

Reaching this point means the AALyrics-specific foundation is ready to receive proven behavior from the working fork behind the new boundaries. Do not automatically continue into LRCLIB, Musixmatch, PetitLyrics, SyncLRC, or bulk code migration.

At this gate we will review:

1. whether the core architecture actually matches the intended design,
2. whether provider contracts or selector boundaries need adjustment,
3. the current `MIGRATION_INVENTORY.md` classifications,
4. which mature fork behaviors should be preserved with minimal semantic change,
5. which implementations need structural refactoring to remove Android/provider coupling,
6. which provider should be adapted first,
7. which code must instead be rewritten or dropped.

Only after that explicit review does implementation adaptation begin.

## Later phases — intentionally not started

After the stop gate, likely work includes adapting the mature resolver/matching logic, concrete provider adapters, cache, translation, Android Auto/phone presentation, timing controls, karaoke behavior, persistence, release/signing, and regression comparison against the previous fork.

These are intentionally not scheduled in detail yet. The migration inventory should be refined as the working fork evolves so AALyrics does not duplicate already-proven work.

## Working method

Each implementation phase should be split into small topic branches and PRs. When a phase is large, a branch-local `TASK.md` may be used as a short execution checklist, while this document remains the durable project roadmap.

Before starting any non-trivial implementation slice:

1. check `docs/MIGRATION_INVENTORY.md`,
2. inspect the current `whoxamxl/auto-lyrics` main branch for equivalent behavior,
3. classify the behavior as PRESERVE, REFACTOR, REWRITE, or DROP,
4. only then implement the AALyrics-specific work that is actually necessary.
