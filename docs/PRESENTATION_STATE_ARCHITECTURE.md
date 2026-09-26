# Presentation State Architecture Foundation

## Purpose

Define how shared lyrics/application facts reach Phone and Android Auto presentation without creating one oversized UI state model or allowing either surface to own provider, media-session, cache, translation, timing, or karaoke policy.

The working fork's legacy `LyricsState` mixed Android objects, playback, lyrics, translation, timing offset, UI indices, and visual data. Migration intent remains **REWRITE**: preserve useful user-facing behavior while keeping state ownership separated in AALyrics.

## Stable ownership rule

AALyrics should share semantic facts, not force Phone and Android Auto into one surface model.

```text
application/domain state
        ↓
shared presentation-ready facts where semantics are truly common
        ↓
   ┌───────────────┴───────────────┐
   ↓                               ↓
Phone presentation state      Automotive presentation state
   ↓                               ↓
Compose UI                     host-rendered templates
```

Phone and automotive state may differ in shape, granularity, update cadence, navigation, interaction, and rendering capability.

## Timing projection consumption

Shared timing semantics are produced before presentation mode is chosen.

```text
canonical timed lyrics + EffectiveLyricsPosition
                ↓
        Timing Semantic Engine
                ↓
        LyricsTimingProjection
                │
        ┌───────┴────────┐
        ↓                ↓
Normal presentation   Karaoke presentation
```

Karaoke ON/OFF is application/presentation state, not an input to the Timing Semantic Engine.

For current Phone presentation, normal line-oriented rendering consumes `activeLineIndex`. When effective Phone Karaoke is enabled for WORD_SYNC, the Phone mapper also consumes the already-computed word-level timing facts and maps them into Phone-local sweep presentation. Android Auto Now Playing remains line-oriented and consumes `activeLineIndex` only.

Neither Phone nor automotive renderers may recalculate shared timing semantics. Karaoke enablement changes which already-computed facts the Phone presentation consumes; it does not select a different timing engine or playback clock.

## Shared facts versus surface state

Shared presentation-ready facts may eventually include semantically common information such as:

- current normalized track metadata;
- current canonical lyrics availability/state;
- optional translation availability/result;
- effective lyrics position / calibration facts;
- shared timing projection facts and implemented Phone Karaoke consumer inputs;
- provider/source attribution needed for display;
- capability flags derived from domain state.

They must remain UI-framework-neutral.

Surface-specific state may include:

- selected destination/screen;
- expanded/collapsed presentation;
- scroll/follow mode;
- Phone-only layout state;
- Android Auto template sections/actions;
- host update throttling/invalidation state;
- transient interaction state that has no application meaning.

## Static versus high-frequency Phone presentation

PR #86 demonstrates a second surface-specific state rule: data with different change cadences should not be forced through one high-frequency allocation path merely because it appears in one screen.

For Phone Lyrics:

```text
static / infrequent
- canonical lyric row text
- optional translated row text
- stable row identity

dynamic
- current line
- playback progress
- animated focus position
- current Karaoke line/sweep
```

The app-owned host memoizes static row projection by playback/lyrics/Translation identity. The normal 250 ms clock tick and effective-Karaoke 33 ms tick update dynamic presentation facts without rebuilding the complete row list. In `:ui:phone`, lazy rows receive the smallest dynamic input required; non-current rows do not consume current Karaoke sweep state.

This optimization does not justify moving timing, Translation, or provider policy into the UI. It is a presentation-shape/cadence decision at the existing app-to-Phone boundary.

## No universal `UiState`

This foundation explicitly rejects a requirement for one giant `PresentationState` or `UiState` shared by both surfaces.

A shared type is justified only when the semantics are truly common and stable. Surface-specific needs should remain local.

This prevents Android Auto host constraints from distorting Phone Compose state and prevents Phone visual richness from leaking into automotive models.

## Dependency direction

Allowed direction:

```text
core/application capability state
        ↓
presentation mapping
        ↓
:ui:phone or :ui:automotive
```

Forbidden ownership:

- UI must not call concrete providers;
- UI must not read cache storage directly;
- UI must not own translation engine execution;
- UI must not normalize MediaSession data;
- UI must not repair or synthesize cross-track playback snapshots; track identity and timeline coherence is a `:platform:media` invariant;
- UI must not implement calibration formulas or compute `projectedPlaybackPosition + lyricsOffset` itself;
- UI must not duplicate karaoke current-line/current-word algorithms;
- `:ui:phone` and `:ui:automotive` must not depend on each other.

## State mapping

Presentation mapping may live in the surface module when it is surface-specific and depends only on stable domain/application facts.

If repeated mapping semantics become genuinely shared, they may later be extracted into a framework-neutral presentation/projection boundary. Extraction should follow demonstrated reuse, not anticipation alone.

The architecture should prefer:

```text
shared semantic projection
       ↓             ↓
Phone mapper    Automotive mapper
```

over:

```text
one giant common UI state
       ↓
conditional rendering everywhere
```

## Playback and lifecycle

Presentation observes state; it does not own playback discovery or lyrics demand policy.

Phone process foreground, Android Auto projection connection, and active legacy `LyricsBrowserService` lifetime are application/runtime lifecycle inputs to `LyricsDemandGate`. The browser service reports host demand through a narrow application boundary; individual screens/composables/templates still must not start or cancel provider lookup merely because they appear or disappear.

Playback controls, if/when implemented, should use an explicit transport/control boundary rather than importing `MediaController` into UI state.

## Capability composition

The presentation layer is where independently owned capabilities may be assembled for display without merging their ownership.

Conceptually:

```text
LyricsState
Translation state/result
Timing/calibration state / effective lyrics position
Shared timing projection
Optional Karaoke consumer facts
Track/playback facts
        ↓
surface presentation mapping
```

This composition must preserve each capability's failure and lifecycle semantics. For example, translation failure must not convert valid lyrics into a lyrics failure screen.

## Current implemented Automotive slice

The Android Auto Now Playing completion in PR #84 provides concrete evidence for one surface-specific state contract without changing the general rule against a universal UI state.

For this slice, Automotive presentation may compose:

- normalized track/playback facts;
- selected-session artwork;
- `PlaybackControlState.capabilities`;
- canonical `LyricsState`;
- existing Translation settings/state;
- shared timing projection, consuming `activeLineIndex` only.

The resulting Automotive-local state should distinguish primary lyric text from optional secondary Translation/status text instead of continuing to hide the entire presentation in one ambiguous subtitle string.

Surface-local presentation policy includes the 250 ms loading heartbeat and MediaSession metadata invalidation. Those facts must not be promoted into Lyrics, Translation, or timing domain state.

This authorization does not justify a shared Phone/Automotive `UiState`, Automotive provider access, Android framework objects in core state, or Karaoke word presentation.

See `docs/ANDROID_AUTO_NOW_PLAYING.md` for the exact surface contract.

## Deferred decisions

Still deliberately deferred at this architecture level:

- one exact shared presentation-facts type;
- ViewModel ownership or DI framework beyond demonstrated need;
- a universal Flow/StateFlow combination layer;
- transport-control API changes not already required by implemented surfaces;
- cross-surface navigation abstractions;
- promotion of Phone-local scroll/follow or refresh-cadence policy into shared state;
- speculative shared capability flags or error models.

Implemented Phone and Automotive state shapes remain surface-local evidence, not a mandate for a universal model.

## Ongoing implementation gate

Before adding or broadening presentation state:

1. inventory the actual data required by the first production Phone and Android Auto screens;
2. define the smallest shared semantic facts supported by both without surface leakage;
3. keep surface-local state local;
4. prove presentation maps from normalized/application state without provider/platform/storage dependencies;
5. add deterministic mapping tests for loading, ready, degraded, not-found, failure, translation, timing, and karaoke-capability combinations relevant to the implemented surface;
6. add shared abstractions only after demonstrated semantic reuse;
7. implement each surface on dedicated topic branches and stop before merge for approval.
