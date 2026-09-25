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
- live Android media-session runtime — PR #29
- process-wide lyrics-demand gating — PR #30

The live Android media-session runtime is implemented and merged in PR #29. Process-wide lyrics-demand gating is implemented and merged in PR #30; its boundary is specified in `docs/LYRICS_DEMAND_GATING.md`. Presentation remains in the dedicated `:ui` boundary: a shared Compose design system plus separate phone and automotive screen-composition modules.

The lyrics-capability architecture is defined by `docs/LYRICS_PIPELINE_ARCHITECTURE.md` and its five focused capability documents for cache, translation, timing/calibration, karaoke projection, and presentation state. That foundation fixes ownership, dependency direction, lifecycle constraints, and canonical-versus-derived data rules. Translation has justified its concrete modules through implementation, and Phase 11.3a now explicitly authorizes one additional pure-core module, `:core:timing`, for the effective-lyrics-position foundation. Other future capability modules remain evidence-driven rather than pre-created.

## Design goals

1. Keep Android media integration out of the lyrics domain.
2. Keep provider-specific behavior out of application state management.
3. Centralize provider selection instead of allowing providers to compete through call order.
4. Expose stable application/domain facts to both phone and automotive presentation layers without forcing both surfaces into one universal UI state.
5. Make timing, translation, caching, karaoke projection, provider implementations, media runtime, and demand lifecycle independently replaceable.
6. Keep pure domain modules free of Android framework dependencies.
7. Make track changes, cancellation, provider failures, stale results, playback ownership, lyrics demand, and derived-capability ownership explicit concerns rather than incidental UI behavior.
8. Ensure AALyrics core behavior can be tested entirely with fakes.
9. Preserve proven fork behavior unless there is a concrete architectural, correctness, or maintainability reason to change it.
10. Separate semantic migration from structural refactoring: behavior may stay the same even when ownership and module boundaries change.
11. Keep reusable visual tokens/components independent from phone- or automotive-specific screen composition.
12. Stabilize future capability seams before signatures: define ownership and dependency direction first, then introduce only the smallest implementation-specific contracts justified by each later slice.

## Migration principle

Every significant working-fork behavior is classified before implementation:

- **PRESERVE** — keep behavior with minimal semantic change.
- **REFACTOR** — preserve behavior but move ownership/dependencies to fit AALyrics.
- **REWRITE** — preserve the requirement where appropriate but replace unsuitable coupling/implementation.
- **DROP** — do not migrate unless later evidence shows it is required.

The classification is tracked in `docs/MIGRATION_INVENTORY.md`. Re-check working-fork `main` before each non-trivial migration slice rather than relying on an old snapshot.

## Modules and ownership

### `:app`

Android application and composition root. It owns process-level object wiring, application identity, the process coroutine scope, and application/runtime lifecycle composition. It may depend on concrete outer adapters, but should contain very little feature logic.

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

The demand-gating slice adds an application-lifecycle boundary in front of `PlaybackLyricsController`; it does not move provider or media-session ownership into `:app`.

The Translation background scaffold also lets `:app` compose persisted Translation settings with a process-level ML Kit target-model preparation runtime. That runtime does not observe canonical lyrics, execute lyric Translation, or publish foreground state.

Translation execution and Phone presentation are now composed through their dedicated capability/application boundaries. Future cache, timing integration, karaoke, Android Auto Translation, and broader presentation-capability composition may be wired from `:app` or other appropriate composition roots, but the composition root must not become the owner of their feature logic.

### `:core:model`

Pure Kotlin normalized track, playback, playback identity, and lyrics representations. Provider DTOs and Android framework types do not belong here.

### `:provider:api`

Pure Kotlin provider contracts. Concrete providers normalize results into `LyricsCandidate` and may report provider-neutral search evidence, but do not decide the final cross-provider winner.

### `:core:lyrics`

Pure Kotlin application lyrics orchestration. It owns `LyricsState`, lookup identity/lifecycle, provider fan-out, stale-result protection, playback-to-lookup ownership, and the `CandidateSelector` port. It depends on provider contracts, never concrete providers or the production selector implementation.

It must not become a general-purpose cache, translation, timing, karaoke, persistence, or presentation orchestrator merely because those capabilities consume lyrics.

### `:core:timing`

A dedicated pure Kotlin/JVM timing capability.

Implemented foundation responsibilities:

- represent signed `LyricsTimingOffset` semantics;
- derive `EffectiveLyricsPosition` from projected playback position plus offset;
- define zero offset as neutral behaviour.

The implemented shared timing semantic engine also has these responsibilities:

- consume canonical `:core:model` timed lyrics + `EffectiveLyricsPosition`;
- project active line;
- project active word for WORD timing;
- derive word progress only when duration is defensible;
- expose word boundary facts deterministically.

The sign convention remains fixed: positive advances lyrics, negative delays lyrics.

`:core:timing` is now authorized to depend on `:core:model` for this semantic projection and on no other production module. It remains framework-independent and stateless.

It must not own MediaSession projection, canonical lyrics mutation, providers, Translation, persistence, Sync UI, Karaoke enablement, Phone/automotive presentation, rendering, layout, or animation cadence.

### `:provider:selection`

Production implementation of the `CandidateSelector` port. It owns cross-provider ranking policy including metadata plausibility, payload quality, source confidence, synchronized/plain fallback, recording-version handling, and WORD/karaoke preference behavior.

`:core:lyrics` never depends on this implementation; `:app` injects it through the core port.

### `:provider:matching` and `:provider:lrc`

Pure shared utilities. Matching owns genuinely provider-neutral metadata/version semantics. LRC owns ordinary/enhanced LRC parsing normalized to core timing types. Neither owns networking or application state.

### Concrete provider modules

`:provider:lrclib`, `:provider:petitlyrics`, `:provider:musixmatch`, and `:provider:synclrc` are outer adapters implementing `LyricsProvider`.

Each provider owns only its provider-local transport/authentication, query/fallback strategy, DTOs, parsing, provider-local validation, and normalization into `LyricsCandidate`. A provider may report provider-neutral evidence discovered during search, but it must not decide the final winner across providers.

LRCLIB, PetitLyrics, Musixmatch, and SyncLRC are migrated behind the provider contract. Shared policy is defined in `docs/PROVIDER_ARCHITECTURE.md`; concise provider profiles live in `docs/providers/`.

### `:translation:api`

Pure Kotlin Translation capability contracts demonstrated by the background scaffold.

Current responsibilities:

- the approved product-level target-language set and normalization;
- immutable Translation settings state and a persistence-agnostic settings-store boundary;
- engine-independent model lifecycle state and the `TranslationModelManager` boundary.
- engine-independent language-identification evidence;
- Translation Provider identity, route, and prepared-session contracts.

It does not depend on Android, Google ML Kit, Lyrics Providers, `:core:lyrics`, or presentation.

### `:translation:core`

Pure Kotlin Translation execution and orchestration over canonical `LyricsDocument` values.

It owns:

- exact in-process canonical lyrics/request identity;
- complete-document Primary/Secondary profiling and conservative line routing;
- contextual block planning with unique Core ownership and overlapping Context Halos;
- structural marker validation plus smaller-block/per-line fallback;
- one-provider-per-artifact assembly;
- cancellation, stale-result rejection, and atomic `TranslationState` publication.

It depends on `:core:model` and pure `:translation:api`. It does not depend on Lyrics Provider APIs, `:core:lyrics`, Android, networking, ML Kit, or presentation.

### `:translation:mlkit`

Android/Google ML Kit adapter for Translation model lifecycle.

Current responsibilities include model planning, availability checks, download-task/monitor reuse, retryable failure state, thermal waiting, active-download-time timeout semantics, ML Kit language identification, and prepared source/target Translation sessions refactored from the mature Auto-Lyrics implementation.

It depends on `:translation:api` and ML Kit. It executes text only behind Translation Provider sessions; it does not own profiling policy, canonical lyrics, artifact assembly, stale-result policy, or Phone/Android Auto state.

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

It must not fetch/rank lyrics, depend on concrete providers, own `LyricsState`, implement presentation, or decide whether phone/automotive lifecycle currently demands lyrics.

Because Android constructs `NotificationListenerService`, constructor injection from `:app` is not available. The live runtime uses the narrow platform-defined `MediaSessionRuntimeHost`/`PlaybackSnapshotSink` boundary. `:platform:media` delivers only normalized `PlaybackSnapshot` values through that boundary and does not know `LyricsCoordinator`, provider implementations, or demand policy.

### `:ui:designsystem`

Shared Compose visual foundation for all AALyrics surfaces. It owns theme tokens, typography, dimensions, shapes, icons, and reusable presentation components. It must not depend on `:core`, provider modules, `:platform:media`, `:ui:phone`, or `:ui:automotive`.

Production design-system code lives under `src/main`; debug-only catalogs and previews live under `src/debug`. Preview code renders production composables rather than maintaining a second UI implementation.

### `:ui:phone`

Phone-specific presentation and screen composition. It consumes the shared lyrics-core contract and `:ui:designsystem`, maps domain/application capability facts into phone UI state, and must not talk directly to provider implementations, cache storage, translation infrastructure, or the media platform adapter.

Phone lyrics demand is process-lifecycle state owned/wired above the presentation module; individual composables must not start or cancel provider work directly.

### `:ui:automotive`

Automotive-specific presentation and screen composition. It consumes the same application/domain capability facts as the phone UI plus `:ui:designsystem`. It must not own lyrics fetching, provider selection, cache storage, translation execution, timing math, karaoke semantics, or Android media-session adaptation.

Android Auto lyrics demand is projection-connection lifecycle state owned/wired above the presentation module; the automotive composables do not own that policy.

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

Translation now has one justified concrete dependency path:

```text
:app
  ├─> :translation:core ─> :core:model
  │          ↓
  │    :translation:api
  └─> :translation:mlkit ─> :translation:api
```

`:translation:api` and `:translation:core` remain pure Kotlin. UI modules must not depend on the concrete `:translation:mlkit` adapter. Translation remains downstream of canonical lyrics without moving execution into Lyrics Providers or `:core:lyrics`.

Cache, Karaoke, and any additional Translation modules remain absent until implementation evidence justifies their placement and contracts. Phase 11.3a is the explicit evidence/authorization for introducing `:core:timing`; the module must remain pure and does not gain an application/UI consumer until the separately authorized Phase 11.3b integration.

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
MediaSessionRuntimeHost
        |
        v
LyricsDemandGate                     (:app / lifecycle boundary)
        |
        | demand active
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

The central direction is deliberate: platform and provider adapters feed normalized inputs into AALyrics core; they do not own application state. Demand gating controls whether observed playback owns lyrics work, but it does not change playback normalization, track identity, provider execution, or candidate selection. Presentation converts shared domain state into surface-specific UI state and reusable visual components.

Future capability integration extends the post-lookup system through explicit seams rather than by changing the ownership above:

```text
canonical normalized lyrics
        ├─ cache boundary
        ├─ translation boundary
        └─ timing/calibration boundary
                    ↓
          effective lyrics position
                    ↓
             karaoke projection
                    ↓
       presentation-ready semantic facts
             /                 \
        Phone state       Automotive state
```

This is an ownership sketch, not a requirement that all capabilities execute linearly or synchronously.

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

Detailed runtime behavior is in `docs/MEDIA_SESSION_RUNTIME.md`.

## Demand gating boundary

Demand gating is implemented at the application lifecycle boundary and is specified in `docs/LYRICS_DEMAND_GATING.md`.

Preserve the mature working-fork rule:

```text
phone process foreground
        OR
Android Auto projection connected
        =
lyrics demand active
```

Demand policy is application/runtime lifecycle state, not presentation state and not MediaSession selection state.

The MediaSession runtime continues observing and normalizing playback regardless of demand. The gate retains the newest normalized `PlaybackSnapshot` while inactive but does not forward provider-owning playback to `PlaybackLyricsController`.

Required transitions:

- `OFF -> ON`: replay the latest retained snapshot exactly once so lyrics start immediately without waiting for another track event;
- `ON -> OFF`: suspend lookup ownership once, cancelling in-flight/background provider work while retaining an already resolved usable result in process memory;
- repeated same-value demand updates: no-op;
- one source turning off while the other remains active: demand stays on.

Phone demand uses process-level lifecycle semantics so ordinary Activity recreation does not flap demand. Automotive demand represents the whole Android Auto projection connection, even while another AA app is foreground.

The gate must not reach into provider jobs, change `PlaybackTrackIdentity`, or teach `:platform:media` about UI/application lifecycle policy.

## Lyrics capability foundation

Future cache, translation, timing/calibration, karaoke projection, and presentation-state work share one architectural foundation defined in `docs/LYRICS_PIPELINE_ARCHITECTURE.md`.

Capability-specific rules live in:

- `docs/CACHE_ARCHITECTURE.md`
- `docs/TRANSLATION_ARCHITECTURE.md`
- `docs/TIMING_ARCHITECTURE.md`
- `docs/KARAOKE_ARCHITECTURE.md`
- `docs/PRESENTATION_STATE_ARCHITECTURE.md`

The central invariant is that canonical normalized lyrics and source timing remain distinguishable from derived artifacts and projections:

```text
canonical lyrics
├─ source text/timing
├─ cacheable canonical facts
├─ derived translation
├─ derived effective lyrics position / calibration
└─ derived karaoke projection
```

Stable rules:

- cache/storage infrastructure stays behind a replaceable data-access boundary;
- translation is additive and must not overwrite valid original lyrics;
- calibration keeps source timing immutable and derives `effectiveLyricsPosition = projectedPlaybackPosition + lyricsOffset`; positive advances lyrics and negative delays lyrics;
- line/word timing semantics are framework-neutral and shared in the timing capability before Normal or Karaoke presentation consumes them;
- Phone and automotive consume common semantic facts but retain independent surface state;
- asynchronous/persisted derived work must respect canonical lyrics identity and stale-result ownership;
- optional capability failure must not erase valid lower-level lyrics state;
- providers, MediaSession runtime, `LyricsCoordinator`, and UI must not absorb unrelated capability ownership.

This foundation generally avoids speculative concrete modules and APIs. Translation introduced concrete modules only when implementation justified them; timing now has enough evidence to extend the existing `:core:timing` module with shared semantic projection over `:core:model`. Cache storage, timing persistence/scope, Sync UX, drift algorithms, Karaoke consumer/rendering contracts, ViewModels, and final UI-state shapes remain deferred until their own evidence-bearing slices.

## Core responsibilities

### `LyricsCoordinator`

Owns one active lyrics-request lifecycle: provider fan-out, cancellation/supersession, failure isolation, candidate handoff, stale-result rejection, and publication of `LyricsState`.

### `LyricsLookupLifecycle`

Narrow provider-independent start/clear boundary used by playback ownership.

### `PlaybackLyricsController`

Owns the pure transition from normalized playback identity/preferences to lyrics-request ownership. It does not normalize Android metadata, discover media sessions, decide application demand, fetch providers directly, or render UI.

### `CandidateSelector`

Port between core orchestration and winner selection. The production implementation is `CrossProviderCandidateSelector` in `:provider:selection`.

### `LyricsState`

Provider-independent observable domain state for phone and automotive presentation. Loading, ready/degraded, not-found, and failure states are explicit domain outcomes.

`LyricsState` is not intended to become a replacement monolith containing cache storage state, translation engine internals, calibration persistence, karaoke renderer state, or both surfaces' complete UI state.

## Failure and lifecycle rules

- One provider failure must not automatically fail the whole request when other providers can still produce candidates.
- Results belonging to an obsolete track/request must never replace state for the current track.
- Provider execution order must not implicitly determine the winning candidate.
- Position, duration, playback-status, and playback-rate updates must not restart lyrics lookup by themselves.
- Source-specific playback identity parsing and Android session ownership must remain outside lyrics core.
- Missing notification-listener access or `SecurityException` from active-session APIs must fail safely.
- No eligible live session must clear playback ownership instead of leaving stale lyrics active.
- Selected-controller callbacks/listeners must be detached when ownership ends.
- With no lyrics demand, provider-owning playback must not be forwarded even though media-session observation continues.
- Losing the final demand source must suspend current lookup work; a resolved usable result may remain owned for same-identity resume, while losing only one of multiple active demand sources must not suspend it.
- UI layers must not retry, rank, merge, fetch, select media sessions, or directly start/cancel provider jobs.
- Reusable design-system code must not import app/domain/provider/platform ownership merely for convenience.
- Android framework media types must not cross into `:core:model`, `:core:lyrics`, or `:provider:api`.
- Existing proven matching/scoring behavior must not be replaced without explicit regression evidence and a documented reason.
- Concrete providers surface operational failures according to the provider contract instead of silently converting every failure into a no-result outcome.
- Provider-local cancellation should cancel underlying HTTP work where practical.
- Translation failure must leave valid original lyrics usable.
- Calibration changes must not silently rewrite canonical provider timestamps or refetch lyrics unless a later explicit policy requires it.
- Karaoke semantic calculation must not be duplicated independently by Phone and automotive renderers.
- Cache or optional derived-capability failure must not corrupt canonical lyrics lifecycle state.

## Architecture guardrails

`scripts/verify-architecture.sh` is a best-effort regression guardrail for common accidental boundary violations. It checks the `ui/*` presentation ownership along with the established pure-core/provider/media boundaries. It is not a formal Kotlin/Gradle static-analysis proof and should not be expanded indefinitely to enumerate every theoretical bypass. Stronger structural enforcement, if later needed, should use appropriate Gradle/static-analysis tooling as separate work.

As future capability modules become concrete, their implementation slices should add executable dependency guardrails only for boundaries that now exist in code. The project should not invent empty modules solely so CI can enforce speculative dependency rules.

## Future extension points

The architecture seam for cache, translation, timing/calibration, karaoke projection, and presentation state is defined. Translation is implemented through Phone presentation, and Phase 11.3a now authorizes the minimal `:core:timing` foundation. Remaining concrete work stays in independent slices and must follow `docs/LYRICS_PIPELINE_ARCHITECTURE.md` plus the relevant capability-specific document.

Settings/persistence, release/signing, and other later features remain separate responsibilities. Their future existence must not be used as a reason to mix those concerns into `LyricsCoordinator`, `PlaybackLyricsController`, the media-session runtime, demand gate, provider selection, concrete provider adapters, capability services, or shared design-system components.

See `docs/ROADMAP.md`, `docs/CORE_READINESS_GATE.md`, `docs/MIGRATION_INVENTORY.md`, `docs/APPLICATION_COMPOSITION.md`, `docs/MEDIA_SESSION_RUNTIME.md`, `docs/LYRICS_DEMAND_GATING.md`, `docs/LYRICS_PIPELINE_ARCHITECTURE.md`, `docs/CACHE_ARCHITECTURE.md`, `docs/TRANSLATION_ARCHITECTURE.md`, `docs/TIMING_ARCHITECTURE.md`, `docs/KARAOKE_ARCHITECTURE.md`, `docs/PRESENTATION_STATE_ARCHITECTURE.md`, and `docs/PROVIDER_ARCHITECTURE.md`.
