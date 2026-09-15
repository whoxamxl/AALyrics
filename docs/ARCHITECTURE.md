# AALyrics Architecture

## Status

This document defines the architectural boundaries for AALyrics. The project is intentionally being built core-first before any concrete lyrics provider implementation is introduced.

AALyrics is a greenfield codebase, but not a greenfield behavior specification. The working `whoxamxl/auto-lyrics` fork is treated as a behavioral reference and regression oracle. Proven behavior should not be re-invented merely because the new module structure is different.

Implementation code from the previous fork is not imported into the new codebase before the Core Readiness Gate described in `ROADMAP.md`. Before that gate, the fork is inspected to inform boundaries, tests, migration classifications, and compatibility requirements.

## Design goals

1. Keep Android media integration out of the lyrics domain.
2. Keep provider-specific behavior out of application state management.
3. Centralize provider selection instead of allowing providers to compete through call order.
4. Expose one stable application/domain state to both phone and automotive presentation layers.
5. Make timing, translation, caching, and provider implementations independently replaceable.
6. Keep pure domain modules free of Android framework dependencies.
7. Make track changes, cancellation, provider failures, and stale results explicit domain concerns rather than incidental UI behavior.
8. Ensure AALyrics core behavior can be tested entirely with fake providers before any real provider is connected.
9. Preserve proven fork behavior unless there is a concrete architectural, correctness, or maintainability reason to change it.
10. Separate semantic migration from structural refactoring: behavior may stay the same even when ownership and module boundaries change.

## Core-first rule

Concrete provider implementations are adapters. They must conform to AALyrics; AALyrics must not grow around the quirks of a provider.

Before the Core Readiness Gate, development is limited to provider-independent models, contracts, orchestration, selection boundaries, playback abstractions, state transitions, and tests using fake providers.

No LRCLIB, Musixmatch, PetitLyrics, SyncLRC, or previous-fork implementation code is ported before that gate is reached and explicitly reviewed.

This does **not** mean existing behavior is ignored. Before implementing any substantial behavior, consult `docs/MIGRATION_INVENTORY.md` and the current fork. If equivalent mature behavior already exists, define only the AALyrics boundary needed to receive it later instead of creating a competing implementation.

## Migration principle

Every significant behavior from the working fork should be classified before implementation work begins:

- **PRESERVE** — behavior is already correct and should migrate with minimal semantic change.
- **REFACTOR** — behavior should stay, but ownership/dependencies should change to fit AALyrics.
- **REWRITE** — existing implementation is too coupled, obsolete, or unsuitable; reimplement the behavior against the new contracts.
- **DROP** — behavior is unused, superseded, or intentionally excluded.

The initial classification is tracked in `docs/MIGRATION_INVENTORY.md`. Classification can change when evidence changes, but silent reinvention is not allowed.

## Modules

### `:app`

Android application and composition root. It owns process-level wiring and application identity, but should contain very little feature logic.

### `:core:model`

Pure Kotlin domain types shared across the project. It owns normalized track, playback, playback identity, and lyrics representations. Provider-specific response types and Android media types do not belong here.

### `:provider:api`

Pure Kotlin contracts implemented by lyrics providers. It defines what a provider may return to AALyrics, not how provider selection works. Concrete provider modules live outside this module.

### `:core:lyrics`

Pure Kotlin AALyrics orchestration. It owns application lyrics state, provider orchestration policy, request lifecycle, stale-result rejection, playback-to-lookup ownership, the candidate-selection port, and domain-level state transitions. It depends on provider contracts, never concrete providers.

The mature scoring/matching policy from the working fork is **not** to be independently redesigned during core-first work. AALyrics defines a stable selection boundary first; the proven resolver behavior is adapted behind that boundary after the Core Readiness Gate.

### `:platform:media`

Android-specific media-session adaptation. Its job is to translate Android `MediaController` / `MediaMetadata` / `PlaybackState` values into provider-independent playback/domain models. It may contain source-specific playback identity extraction such as Spotify resource parsing, but it does not fetch or rank lyrics.

### `:feature:phone`

Phone presentation only. It consumes application/domain state and must not talk directly to provider implementations.

### `:feature:automotive`

Android Auto presentation only. It consumes the same application/domain state as the phone UI and must not own lyrics fetching or provider selection.

## Intended dependency direction

```text
                         +-------------------+
                         |       :app        |
                         +---------+---------+
                                   |
                 +-----------------+-----------------+
                 |                 |                 |
                 v                 v                 v
        :feature:phone   :feature:automotive   :platform:media
                 |                 |                 |
                 +--------+--------+                 |
                          v                          v
                    :core:lyrics                :core:model
                          |
                          v
                    :provider:api
                          |
                          v
                    :core:model
```

Concrete provider modules will depend on `:provider:api` and `:core:model`. The domain must never depend on a concrete provider.

## Runtime state flow

```text
Android MediaController
        |
        v
MediaControllerSnapshotAdapter       (:platform:media)
        |
        v
PlaybackSnapshot                     (:core:model)
        |
        v
PlaybackTrackIdentity                (:core:model)
        |
        v
PlaybackLyricsController             (:core:lyrics)
        |
        v
LyricsLookupLifecycle
        |
        v
LyricsCoordinator
        |
        +----> LyricsProvider contracts ----> provider adapters
        |                  |
        |                  v
        |          List<LyricsCandidate>
        |                  |
        +<----- CandidateSelector port
        |
        v
LyricsState
   |          |
   v          v
Phone UI   Android Auto UI
```

The central direction is deliberate: platform and provider adapters feed normalized inputs into AALyrics core; they do not own application behavior.

## Playback boundary

`PlaybackSnapshot` represents current playback facts. It is intentionally broader than track-change identity: position, playback status, rate, and late duration updates may change continuously without implying a new lyrics request.

`PlaybackTrackIdentity` is the pure decision key used to determine whether lyrics ownership should change. Identity is selected in this order:

1. explicit stable `TrackReference` values,
2. playback-source media id/URI,
3. identifying metadata fallback (source, title, artists, album).

Duration, position, playback status, and playback rate are excluded from identity. This prevents normal timeline/state updates from restarting provider work.

Source-specific extraction remains in `:platform:media`. For example, Spotify playback may add a `TrackReference(namespace = "spotify", ...)` only when the source is the Spotify Android package and metadata explicitly identifies a track resource. A bare 22-character media id is not assumed to be a Spotify track id because the resource type is ambiguous.

`PlaybackLyricsController` consumes only normalized `PlaybackSnapshot` values. It starts a new `LyricsLookupLifecycle` when identity changes, preserves the current lookup for non-identity updates, and clears lookup ownership when no track remains. It has no Android or provider-specific dependency.

Session selection, process-wide lyrics demand gating, metadata debounce, artwork, transport controls, and presentation behavior remain outside this boundary and are not responsibilities of `PlaybackLyricsController`.

## Core responsibilities

### `LyricsCoordinator`

Owns the lifecycle of a lyrics request for the current track. It coordinates providers through contracts, handles cancellation, ignores stale results, and publishes domain state. State transitions are applied atomically so a concurrent stale completion cannot overwrite a newer lookup.

### `LyricsLookupLifecycle`

Narrow provider-independent lifecycle used by playback-driven code. It exposes only start/clear operations, so playback ownership does not depend on provider fan-out, ranking, or observable-state implementation details.

### `PlaybackLyricsController`

Owns the pure transition from playback identity to lyrics-request ownership. It does not normalize Android metadata and does not decide provider behavior.

### Candidate-selection port

Owns the dependency boundary between orchestration and winner selection. The coordinator supplies a track, normalized candidates, and explicit selection preferences; a selector returns the selected candidate/result.

Before the Core Readiness Gate, tests use a fake selector. The production selector policy is expected to preserve/refactor the mature `LyricsProviderResolver` behavior from the working fork rather than invent a second scoring system.

### `LyricsState`

Represents the observable domain state consumed by presentation layers. Loading, resolved, unavailable, degraded, and failure states are explicit and are not inferred from UI widgets or nullable Android-specific fields.

## Failure and lifecycle rules

- One provider failure must not automatically fail the whole request when other providers can still produce candidates.
- Results belonging to an obsolete track/request must never replace state for the current track.
- Provider execution order must not implicitly determine the winning candidate.
- Position, duration, playback-status, and playback-rate updates must not restart lyrics lookup by themselves.
- Source-specific playback identity parsing must remain outside lyrics core.
- UI layers must not retry, rank, merge, or fetch provider results directly.
- Android framework types must not cross into `:core:model`, `:core:lyrics`, or `:provider:api`.
- Existing proven matching/scoring behavior must not be replaced without explicit regression evidence and a documented reason.

## Future extension points

Translation, caching, timing adjustment, demand gating, session selection, karaoke rendering, and other features may be introduced later behind explicit contracts. Their future existence must not be used as a reason to mix those responsibilities into `LyricsCoordinator` or `PlaybackLyricsController` now.

The roadmap and the Core Readiness Gate are defined in `docs/ROADMAP.md`. The migration classifications are defined in `docs/MIGRATION_INVENTORY.md`.
