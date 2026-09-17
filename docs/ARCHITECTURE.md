# AALyrics Architecture

## Status

This document defines the architectural boundaries for AALyrics. The project was built core-first through the Core Readiness Gate before mature behavior from the working `whoxamxl/auto-lyrics` fork was adapted behind the new boundaries.

Completed milestones:

- Core Readiness Gate — PR #14
- production candidate selection — PR #17
- all four concrete providers through SyncLRC — PR #24
- first production application composition — PR #25
- Compose UI foundation — PR #27
- automotive design-system boundary — PR #28

The live Android media-session runtime is implemented and has completed validation/bounded review in PR #29; explicit merge approval is pending. It is documented in `docs/MEDIA_SESSION_RUNTIME.md`. Presentation remains in the dedicated `:ui` boundary: a shared Compose design system plus separate phone and automotive screen-composition modules.

## Design goals

1. Keep Android media integration out of the lyrics domain.
2. Keep provider-specific behavior out of application state management.
3. Centralize provider selection instead of allowing providers to compete through call order.
4. Expose one stable application/domain state to both phone and automotive presentation layers.
5. Make timing, translation, caching, provider implementations, and media runtime independently replaceable.
6. Keep pure domain modules free of Android framework dependencies.
7. Make track changes, cancellation, provider failures, stale results, and playback ownership explicit concerns rather than incidental UI behavior.
8. Ensure AALyrics core behavior can be tested entirely with fakes.
9. Preserve proven fork behavior unless there is a concrete architectural, correctness, or maintainability reason to change it.
10. Separate semantic migration from structural refactoring: behavior may stay the same even when ownership and module boundaries change.
11. Keep reusable visual tokens/components independent from phone- or automotive-specific screen composition.

## Migration principle

Every significant working-fork behavior is classified before implementation:

- **PRESERVE** — keep behavior with minimal semantic change.
- **REFACTOR** — preserve behavior but move ownership/dependencies to fit AALyrics.
- **REWRITE** — preserve the requirement where appropriate but replace unsuitable coupling/implementation.
- **DROP** — do not migrate unless later evidence shows it is required.

The classification is tracked in `docs/MIGRATION_INVENTORY.md`. Re-check working-fork `main` before each non-trivial migration slice rather than relying on an old snapshot.

## Modules and ownership

### `:app`

Android application and composition root. It owns process-level object wiring, application identity, and the process coroutine scope. It may depend on concrete outer adapters, but should contain very little feature logic.

`AALyricsApplication` currently composes:

```text
LRCLIB
PetitLyrics
Musixmatch
SyncLRC
        ↓
CrossProviderCandidateSelector
        ↓
LyricsCoordinator
        ↓
PlaybackLyricsController
        ↓
LyricsState
```

### `:core:model`

Pure Kotlin normalized track, playback, playback identity, and lyrics representations. Provider DTOs and Android framework types do not belong here.

### `:provider:api`

Pure Kotlin provider contracts. Concrete providers normalize results into `LyricsCandidate` and may report provider-neutral search evidence, but do not decide the final cross-provider winner.

### `:core:lyrics`

Pure Kotlin application lyrics orchestration. It owns `LyricsState`, lookup identity/lifecycle, provider fan-out, stale-result protection, playback-to-lookup ownership, and the `CandidateSelector` port. It depends on provider contracts, never concrete providers or the production selector implementation.

### `:provider:selection`

Production implementation of the `CandidateSelector` port. It owns cross-provider ranking policy including metadata plausibility, payload quality, source confidence, synchronized/plain fallback, recording-version handling, and WORD/karaoke preference behavior.

`:core:lyrics` never depends on this implementation; `:app` injects it through the core port.

### `:provider:matching` and `:provider:lrc`

Pure shared utilities. Matching owns genuinely provider-neutral metadata/version semantics. LRC owns ordinary/enhanced LRC parsing normalized to core timing types. Neither owns networking or application state.

### Concrete provider modules

`:provider:lrclib`, `:provider:petitlyrics`, `:provider:musixmatch`, and `:provider:synclrc` are outer adapters implementing `LyricsProvider`.

Each provider owns only its provider-local transport/authentication, query/fallback strategy, DTOs, parsing, provider-local validation, and normalization into `LyricsCandidate`. A provider may report provider-neutral evidence discovered during search, but it must not decide the final winner across providers.

LRCLIB, PetitLyrics, Musixmatch, and SyncLRC are migrated behind the provider contract. Shared policy is defined in `docs/PROVIDER_ARCHITECTURE.md`; concise provider profiles live in `docs/providers/`.

### `:platform:media`

Android media-session and playback adaptation. It owns Android framework interaction required to turn live playback into normalized `PlaybackSnapshot` values.

Current implemented responsibilities:

- `MediaControllerSnapshotAdapter`
- `MediaSessionSnapshotNormalizer`
- Spotify-specific playback reference extraction
- `NotificationListenerService` access boundary
- `MediaSessionManager` active-session discovery
- selected-session ownership and token-based retention
- selected `MediaController.Callback` lifecycle
- safe normalization/forwarding of live controller state
- platform-owned 600 ms track-metadata stabilization

It must not fetch/rank lyrics, depend on concrete providers, own `LyricsState`, or implement presentation.

Because Android constructs `NotificationListenerService`, constructor injection from `:app` is not available. The live runtime uses the narrow platform-defined `MediaSessionRuntimeHost`/`PlaybackSnapshotSink` boundary, which the application composition root attaches to the existing `PlaybackLyricsController`. `:platform:media` delivers only normalized `PlaybackSnapshot` values through that boundary and does not know `LyricsCoordinator` or provider implementations.

### `:ui:designsystem`

Shared Compose visual foundation for all AALyrics surfaces. It owns theme tokens, typography, dimensions, shapes, icons, and reusable presentation components. It must not depend on `:core`, provider modules, `:platform:media`, `:ui:phone`, or `:ui:automotive`.

Production design-system code lives under `src/main`; debug-only catalogs and previews live under `src/debug`. Preview code renders production composables rather than maintaining a second UI implementation.

### `:ui:phone`

Phone-specific presentation and screen composition. It consumes the shared lyrics-core contract and `:ui:designsystem`, maps domain state into phone UI state, and must not talk directly to provider implementations or the media platform adapter.

### `:ui:automotive`

Automotive-specific presentation and screen composition. It consumes the same application/domain state as the phone UI plus `:ui:designsystem`. It must not own lyrics fetching, provider selection, or Android media-session adaptation.

Detailed presentation/source-set rules are defined in `docs/UI_ARCHITECTURE.md`.

## Compile-time dependency direction

```text
                                  +-------------------+
                                  |       :app        |
                                  +---------+---------+
                                            |
       +-----------------+------------------+------------------+----------------+
       |                 |                  |                  |                |
       v                 v                  v                  v                v
  :ui:phone       :ui:automotive     :platform:media   :provider:selection  provider adapters
       |                 |                  |                  |                |
       +--------+--------+                  v                  v                v
                |                      :core:model        :core:lyrics      :provider:api
                v                                              |                |
       :ui:designsystem                                        v                v
                |                                       :provider:api ----> :core:model
                +---- no dependency on core/provider/platform

  :ui:phone -------> :core:lyrics
  :ui:automotive --> :core:lyrics
```

The diagram is a compile-time dependency sketch, not a runtime call-order diagram. `:provider:selection` depends on the selector port in `:core:lyrics`; `:core:lyrics` never depends on `:provider:selection`. `:ui:designsystem` is intentionally lower-level than both screen-composition modules and is isolated from application/domain ownership.

## Runtime flow

```text
NotificationListenerService          (:platform:media)
        |
        v
MediaSessionManager
        |
        v
selected MediaController
        |
        v
MediaControllerSnapshotAdapter       (:platform:media)
        |
        v
PlaybackSnapshot                     (:core:model)
        |
        v
application/platform host boundary
        |
        v
PlaybackLyricsController             (:core:lyrics)
        |
        v
LyricsCoordinator
        |
        +----> LyricsProvider contracts ----> provider adapters
        |
        +----> CandidateSelector port
                    ^
                    |
        CrossProviderCandidateSelector       (:provider:selection)
        |
        v
LyricsState
   |                         |
   v                         v
Phone UI (:ui:phone)   Automotive UI (:ui:automotive)
        \                 /
         v               v
           :ui:designsystem
```

The central direction is deliberate: platform and provider adapters feed normalized inputs into AALyrics core; they do not own application state. Selection is injected behind a core port rather than being embedded in provider execution order. Presentation converts shared domain state into surface-specific UI state and reusable visual components.

## Playback identity and lookup ownership

`PlaybackSnapshot` represents current playback facts and is broader than track-change identity. Position, playback status, rate, and late duration updates may change continuously without implying a new lyrics request.

`PlaybackTrackIdentity` selects ownership in this order:

1. explicit stable `TrackReference` values,
2. playback-source media id/URI,
3. identifying metadata fallback (source, title, artists, album).

Duration, position, status, and playback rate are excluded from track identity.

`PlaybackLyricsController` now keys lookup ownership by both:

```text
PlaybackTrackIdentity
+
CandidateSelectionPreferences
```

Therefore:

- same track + same preferences -> preserve current lookup;
- same track + changed preferences -> fresh lookup;
- track identity change -> fresh lookup;
- position/status/rate/duration churn -> no refetch;
- no active track -> clear lookup ownership.

Source-specific identity extraction stays in `:platform:media`. Spotify may add `TrackReference(namespace = "spotify", ...)` only when the source/package and resource metadata actually identify a Spotify track.

## Live media-session selection boundary

The implemented runtime preserves/refactors the mature session-selection semantics from the working fork while removing its monolithic ownership:

1. ignore AALyrics' own session;
2. retain the currently selected session while it remains `PLAYING`;
3. otherwise choose the first playing active session;
4. otherwise use the first active session;
5. when no eligible session remains, clear playback ownership;
6. retain by `MediaSession.Token` so harmless active-session reorder does not switch sources.

The selected controller alone owns a runtime callback. Switching selection detaches the old callback and attaches the new one. Session destruction re-evaluates active sessions.

Android notification-listener access is a platform concern. The service must be declared with `BIND_NOTIFICATION_LISTENER_SERVICE`, must wait for `onListenerConnected()`, and should pass its component to active-session APIs rather than depending on privileged `MEDIA_CONTENT_CONTROL`.

Detailed acceptance criteria are in `docs/MEDIA_SESSION_RUNTIME.md`.

## Demand gating boundary

The working fork gates provider work based on phone foreground or Android Auto projection demand. That policy remains valuable but is intentionally a later lifecycle slice.

The media-session runtime may establish the UI-free end-to-end STOP gate first. Demand gating must be introduced before release/presentation work so provider lookup is not permanently active in the background. Do not mix phone/automotive lifecycle ownership into the initial live-session adapter merely to reproduce the old `LyricsDemandController` object shape.

## Core responsibilities

### `LyricsCoordinator`

Owns one active lyrics-request lifecycle: provider fan-out, cancellation/supersession, failure isolation, candidate handoff, stale-result rejection, and publication of `LyricsState`.

### `LyricsLookupLifecycle`

Narrow provider-independent start/clear boundary used by playback ownership.

### `PlaybackLyricsController`

Owns the pure transition from normalized playback identity/preferences to lyrics-request ownership. It does not normalize Android metadata, discover media sessions, fetch providers, or render UI.

### `CandidateSelector`

Port between core orchestration and winner selection. The production implementation is `CrossProviderCandidateSelector` in `:provider:selection`.

### `LyricsState`

Provider-independent observable domain state for future phone and automotive presentation. Loading, ready/degraded, not-found, and failure states are explicit domain outcomes.

## Failure and lifecycle rules

- One provider failure must not automatically fail the whole request when other providers can still produce candidates.
- Results belonging to an obsolete track/request must never replace state for the current track.
- Provider execution order must not implicitly determine the winning candidate.
- Position, duration, playback-status, and playback-rate updates must not restart lyrics lookup by themselves.
- Source-specific playback identity parsing and Android session ownership must remain outside lyrics core.
- Missing notification-listener access or `SecurityException` from active-session APIs must fail safely.
- No eligible live session must clear playback ownership instead of leaving stale lyrics active.
- Selected-controller callbacks/listeners must be detached when ownership ends.
- UI layers must not retry, rank, merge, fetch, or select media sessions directly.
- Reusable design-system code must not import app/domain/provider/platform ownership merely for convenience.
- Android framework media types must not cross into `:core:model`, `:core:lyrics`, or `:provider:api`.
- Existing proven matching/scoring behavior must not be replaced without explicit regression evidence and a documented reason.
- Concrete providers surface operational failures according to the provider contract instead of silently converting every failure into a no-result outcome.
- Provider-local cancellation should cancel underlying HTTP work where practical.

## Architecture guardrails

`scripts/verify-architecture.sh` is a best-effort regression guardrail for common accidental boundary violations. It checks the `ui/*` presentation ownership along with the established pure-core/provider/media boundaries. It is not a formal Kotlin/Gradle static-analysis proof and should not be expanded indefinitely to enumerate every theoretical bypass. Stronger structural enforcement, if later needed, should use appropriate Gradle/static-analysis tooling as separate work.

## Future extension points

Translation, caching, timing adjustment, demand gating, settings/persistence, karaoke rendering, and other features may be introduced later behind explicit contracts. Their future existence must not be used as a reason to mix those responsibilities into `LyricsCoordinator`, `PlaybackLyricsController`, the media-session runtime, provider selection, concrete provider adapters, or shared design-system components.

See `docs/ROADMAP.md`, `docs/CORE_READINESS_GATE.md`, `docs/MIGRATION_INVENTORY.md`, `docs/APPLICATION_COMPOSITION.md`, `docs/MEDIA_SESSION_RUNTIME.md`, and `docs/PROVIDER_ARCHITECTURE.md`.
