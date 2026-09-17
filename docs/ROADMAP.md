# AALyrics Core-First Roadmap

## Purpose

This roadmap prevents the project from drifting back into provider-driven architecture while also avoiding unnecessary reinvention of behavior that is already proven in the working Auto Lyrics fork.

AALyrics is built core-first, but the previous fork remains an active behavioral reference throughout development. Before implementing non-trivial behavior, check `docs/MIGRATION_INVENTORY.md` and the current fork to determine whether the behavior should be preserved, refactored, rewritten, or dropped.

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
- fake-selector contract tests

Merged in PR #8. This phase intentionally defined only the port; the mature resolver was reserved for post-gate adaptation.

### Phase 3.3 — Lyrics coordinator ✅

- fresh request identity for every lookup
- concurrent provider fan-out
- partial provider failure isolation
- cancellation propagation
- no-result and terminal-failure behavior
- cancellation/supersession on a newer lookup
- stale-result protection through lookup identity and atomic state reduction
- one handoff to `CandidateSelector` after candidates are collected

Merged in PR #9, with integration/readiness follow-up in PRs #10 and #11.

### Phase 3.4 — Core integration/readiness tests ✅

The pure provider-independent domain flow is covered end-to-end with fake providers and a fake selector. Validated behavior includes provider-order independence, failure isolation, no-result/degraded/failure states, fresh lookup identity, clear ownership, and stale-result rejection. PR #11 fixed and regression-tested a concurrent stale-publication race by making `LyricsState` transitions atomic through `MutableStateFlow.update`.

### Phase 4 — Playback boundary ✅

Merged in PR #12.

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

### Phase 5 — Core Readiness validation ✅

Merged in PR #14 after the explicit STOP GATE review.

Phase 5 validated, rather than expanded, the provider-independent foundation:

- executable CI architecture checks for pure Kotlin/JVM core modules,
- one shared `LyricsState` contract for phone and automotive features,
- feature ownership guards preventing provider fetching/ranking in UI modules,
- Android MediaSession / MediaController confinement to `:platform:media`,
- provider-specific behavior exclusion from pure core,
- regression evidence for provider order, stale-result rejection, failure isolation, and playback identity,
- migration-inventory re-review against `whoxamxl/auto-lyrics` `main` at `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`).

Detailed evidence is recorded in `docs/CORE_READINESS_GATE.md`.

### Phase 6 — Production candidate selection migration ✅

Merged in PR #17.

Phase 6 adapted the mature cross-provider resolver behind the existing `CandidateSelector` port without making `:core:lyrics` depend on the production implementation.

Completed work includes:

- pure Kotlin `:provider:selection` production selector,
- mature metadata, payload-quality, source-confidence, synchronized/plain fallback, cross-script, recording-version, and karaoke preference behavior,
- provider-neutral candidate evidence needed by the resolver,
- deterministic exact-score tie breaking so provider execution order is not winner policy,
- regression coverage adapted to AALyrics models,
- rejection of unusable lyric candidates before selection.

### Phase 7 — Concrete provider adapters ✅

Completed:

- LRCLIB — merged in PR #19.
- PetitLyrics — merged in PR #20.
- Musixmatch — merged in PR #21.
- SyncLRC — merged in PR #24.

### Phase 8 — Application composition ✅

Merged in PR #25.

The application now constructs one production object graph containing all four providers, `CrossProviderCandidateSelector`, `LyricsCoordinator`, and `PlaybackLyricsController`. The graph is process-owned by `AALyricsApplication`, exposes the shared `LyricsState`, uses existing BuildConfig values for PetitLyrics, and preserves the karaoke-enabled production default through `preferredSyncType = WORD`.

Playback lookup ownership now includes both track identity and candidate-selection preferences. Same-track preference changes trigger a fresh lookup while position/status/rate/duration churn does not.

## Current work

### Phase 9 — Live MediaSession runtime planning

The next slice is documented in `docs/MEDIA_SESSION_RUNTIME.md` and is currently planning-only on `feature/media-session-runtime`; production implementation is not yet authorized.

Its purpose is to connect Android's live active media sessions to the already-composed lyrics engine:

```text
NotificationListenerService
        ↓
MediaSessionManager
        ↓
selected MediaController
        ↓
MediaControllerSnapshotAdapter
        ↓
PlaybackSnapshot
        ↓
PlaybackLyricsController
        ↓
LyricsState
```

The runtime should preserve/refactor the mature working-fork session-selection behavior while keeping Android framework ownership in `:platform:media` and avoiding the old `MediaTracker` monolith.

The STOP gate for this phase is deliberately UI-free: with notification-listener access granted and a media app playing, real playback should be able to drive the production provider/selection pipeline and update `LyricsState`.

## Later phases

After live media-session runtime is stable, later work includes process-wide lyrics-demand gating, cache, translation, Android Auto/phone presentation, timing controls, karaoke rendering, persistence/settings, release/signing, and regression comparison against the previous fork.

These should remain separate responsibilities and topic branches. Do not use future feature needs as a reason to turn `LyricsCoordinator`, `PlaybackLyricsController`, media-session runtime, the production selector, or concrete providers into new god objects.

## Working method

Each implementation phase is split into small topic branches and PRs. `TASK.md` records the active branch-local plan/status when relevant, while this document remains the durable project roadmap.

Before starting any non-trivial implementation slice:

1. check `docs/MIGRATION_INVENTORY.md`,
2. inspect the current `whoxamxl/auto-lyrics` main branch for equivalent behavior,
3. read the relevant architecture/profile document,
4. classify the behavior as PRESERVE, REFACTOR, REWRITE, or DROP,
5. define the current PR acceptance criteria,
6. implement only after the user has explicitly authorized implementation for that slice,
7. run CI and the bounded review process in `AGENTS.md`,
8. stop before merge for explicit approval.
