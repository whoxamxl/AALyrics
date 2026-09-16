# AALyrics Architecture

## Status

This document defines the architectural boundaries for AALyrics. The project was built core-first through the Core Readiness Gate before implementation code from the working fork was adapted.

AALyrics is a greenfield codebase, but not a greenfield behavior specification. The working `whoxamxl/auto-lyrics` fork is treated as a behavioral reference and regression oracle. Proven behavior should not be re-invented merely because the new module structure is different.

The Core Readiness Gate completed in PR #14. Post-gate migration now preserves mature behavior behind the boundaries established before adaptation began. Production candidate selection was migrated in PR #17 and LRCLIB in PR #19; the PetitLyrics adapter is the current migration slice.

## Design goals

1. Keep Android media integration out of the lyrics domain.
2. Keep provider-specific behavior out of application state management.
3. Centralize provider selection instead of allowing providers to compete through call order.
4. Expose one stable application/domain state to both phone and automotive presentation layers.
5. Make timing, translation, caching, and provider implementations independently replaceable.
6. Keep pure domain modules free of Android framework dependencies.
7. Make track changes, cancellation, provider failures, and stale results explicit domain concerns rather than incidental UI behavior.
8. Ensure AALyrics core behavior can be tested entirely with fake providers.
9. Preserve proven fork behavior unless there is a concrete architectural, correctness, or maintainability reason to change it.
10. Separate semantic migration from structural refactoring: behavior may stay the same even when ownership and module boundaries change.

## Migration principle

Every significant behavior from the working fork is classified before implementation work begins:

- **PRESERVE** — behavior is already correct and should migrate with minimal semantic change.
- **REFACTOR** — behavior should stay, but ownership/dependencies should change to fit AALyrics. Mature implementation code may be reused/refactored where appropriate; REFACTOR does not imply a gratuitous rewrite.
- **REWRITE** — existing implementation is too coupled, obsolete, or unsuitable; reimplement the behavior against the new contracts.
- **DROP** — behavior is unused, superseded, or intentionally excluded.

The classification is tracked in `docs/MIGRATION_INVENTORY.md`. Classification can change when evidence changes, but silent reinvention is not allowed.

Before every non-trivial migration slice, re-check the current working-fork `main` rather than relying only on an older snapshot.

Concrete-provider ownership and migration rules are defined in `docs/PROVIDER_ARCHITECTURE.md`; provider-specific capabilities and quirks are recorded under `docs/providers/`. Provider-profile or migration-policy approval is not by itself authorization to start provider implementation.

## Modules

### `:app`

Android application and composition root. It owns process-level wiring and application identity, but should contain very little feature logic.

### `:core:model`

Pure Kotlin domain types shared across the project. It owns normalized track, playback, playback identity, and lyrics representations. Provider-specific response types and Android media types do not belong here.

### `:provider:api`

Pure Kotlin contracts implemented by lyrics providers. It defines what a provider may return to AALyrics, not how a concrete provider performs HTTP/search/parsing.

A normalized `LyricsCandidate` may include provider-neutral search evidence such as whether an artist-constrained query corroborated the candidate. Providers report facts; they do not convert those facts into the final cross-provider winner score.

### `:core:lyrics`

Pure Kotlin AALyrics orchestration. It owns application lyrics state, provider orchestration policy, request lifecycle, stale-result rejection, playback-to-lookup ownership, the `CandidateSelector` port, and domain-level state transitions. It depends on provider contracts, never concrete providers or the production selector implementation.

### `:provider:selection`

Pure Kotlin production implementation of the `CandidateSelector` port.

This module owns cross-provider matching/ranking policy, including the mature metadata, payload-quality, source-confidence, synchronized/plain fallback, cross-script, recording-version, and karaoke-preference behavior adapted from the working fork. Generic matching/version helpers historically embedded in `LrcLibClient` live in `:provider:matching`, shared with LRCLIB without depending on selector implementation.

Provider-specific source-confidence policy is intentionally outside `:core:lyrics`. This preserves dependency inversion: core knows the selector interface, while the composition root may inject `CrossProviderCandidateSelector` without making core depend on its implementation.

Concrete provider networking, parsing, authentication, and provider-local search strategy do not belong in this module.

The neutral `:provider:matching` module owns the preserved generic title, artist, duration, recording-version, and metadata-plausibility semantics. It has no production dependencies. Selection, LRCLIB, and PetitLyrics reuse the relevant functions; payload quality, source confidence, cross-provider weights, and winner policy remain in selection.

### `:provider:matching` and `:provider:lrc`

Pure Kotlin outer utilities. Matching owns shared metadata/version semantics; LRC owns the preserved ordinary/enhanced parser normalized to core model timing types. Neither utility owns provider networking or application state. The LRC parser retains enhanced word-timing regression coverage, while LRCLIB uses ordinary line parsing and advertises only PLAIN/LINE.

### Concrete provider modules

Concrete providers are outer adapters implementing `LyricsProvider`.

Each provider owns only its provider-local transport/authentication, query/fallback strategy, DTOs, parsing, provider-local validation, and normalization into `LyricsCandidate`. A provider may report provider-neutral evidence discovered during search, but it must not decide the final winner across providers.

Shared policy is defined in `docs/PROVIDER_ARCHITECTURE.md`; concise provider profiles live in `docs/providers/`.

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
        +----------------+------------------+------------------+----------------+
        |                |                  |                  |                |
        v                v                  v                  v                v
:feature:phone  :feature:automotive  :platform:media  :provider:selection  provider adapters
        |                |                  |                  |                |
        +--------+-------+                  v                  v                v
                 v                    :core:model        :core:lyrics      :provider:api
           :core:lyrics                                      |                |
                 |                                           v                v
                 +------------------------------------> :provider:api ----> :core:model
```

The diagram is a compile-time dependency sketch, not a runtime call-order diagram. `:provider:selection` depends on the selector port in `:core:lyrics`; `:core:lyrics` never depends on `:provider:selection`.

Concrete provider modules depend on `:provider:api` and normalized model types required by that contract. The domain must never depend on a concrete provider. LRCLIB, PetitLyrics, and selection share `:provider:matching`; LRCLIB also uses `:provider:lrc`. These utilities remain outside core orchestration.

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
        +----> CandidateSelector port
                    ^
                    |
        CrossProviderCandidateSelector       (:provider:selection)
        |
        v
LyricsState
   |          |
   v          v
Phone UI   Android Auto UI
```

The central direction is deliberate: platform and provider adapters feed normalized inputs into AALyrics core; they do not own application state. Selection is injected behind a core port rather than being embedded in provider execution order.

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

`CandidateSelector` owns the dependency boundary between orchestration and winner selection. The coordinator supplies a track, normalized candidates, and explicit selection preferences; a selector returns the selected candidate/result.

The production implementation is `CrossProviderCandidateSelector` in `:provider:selection`. It is adapted from the mature working-fork resolver rather than being a second independently invented scoring system.

The current production policy preserves these important semantics:

- metadata identity dominates the final score,
- incompatible recording versions are rejected,
- title/artist/duration/album evidence is weighted consistently,
- cross-script artist mismatches require independent corroboration,
- payload quality can penalize likely Japanese/romanization interleaving,
- source confidence may differ by provider and track/script context,
- synchronized candidates beat plain fallback candidates,
- a WORD preference may choose a real word-timed candidate only when metadata and payload quality remain near-equivalent,
- exact score ties use a stable candidate key so provider execution order does not become winner policy.

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
- Concrete providers surface operational failures according to the provider contract instead of silently converting every failure into a no-result outcome.

## Architecture guardrails

`scripts/verify-architecture.sh` is a best-effort regression guardrail for common accidental boundary violations. It is not a formal Kotlin/Gradle static-analysis proof and should not be expanded indefinitely to enumerate every theoretical bypass. Stronger structural enforcement, if later needed, should use appropriate Gradle/static-analysis tooling as separate work.

## Future extension points

Translation, caching, timing adjustment, demand gating, session selection, karaoke rendering, and other features may be introduced later behind explicit contracts. Their future existence must not be used as a reason to mix those responsibilities into `LyricsCoordinator`, `PlaybackLyricsController`, provider selection, or concrete provider adapters.

The roadmap and the completed Core Readiness Gate are defined in `docs/ROADMAP.md` and `docs/CORE_READINESS_GATE.md`. Migration classifications are defined in `docs/MIGRATION_INVENTORY.md`; concrete-provider migration policy is defined in `docs/PROVIDER_ARCHITECTURE.md`.
