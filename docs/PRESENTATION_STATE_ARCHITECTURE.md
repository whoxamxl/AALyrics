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

## Shared facts versus surface state

Shared presentation-ready facts may eventually include semantically common information such as:

- current normalized track metadata;
- current canonical lyrics availability/state;
- optional translation availability/result;
- effective lyrics position / calibration facts;
- karaoke projection facts;
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

Phone process foreground and Android Auto projection connection remain application/runtime lifecycle inputs to `LyricsDemandGate`. Individual screens/composables/templates must not start or cancel provider lookup merely because they appear or disappear.

Playback controls, if/when implemented, should use an explicit transport/control boundary rather than importing `MediaController` into UI state.

## Capability composition

The presentation layer is where independently owned capabilities may be assembled for display without merging their ownership.

Conceptually:

```text
LyricsState
Translation state/result
Timing/calibration state / effective lyrics position
Karaoke projection
Track/playback facts
        ↓
surface presentation mapping
```

This composition must preserve each capability's failure and lifecycle semantics. For example, translation failure must not convert valid lyrics into a lyrics failure screen.

## Deferred decisions

Do not decide in this foundation slice:

- exact shared presentation-facts type;
- ViewModel ownership or DI framework;
- Flow/StateFlow combination implementation;
- Phone `LyricsUiState` final fields;
- Automotive screen-state final fields;
- navigation runtime;
- transport-control API;
- scroll/follow behavior;
- UI refresh/throttling cadence;
- settings ownership;
- error copy or visual states;
- exact capability flags.

## Future implementation gate

Before implementing presentation state:

1. inventory the actual data required by the first production Phone and Android Auto screens;
2. define the smallest shared semantic facts supported by both without surface leakage;
3. keep surface-local state local;
4. prove presentation maps from normalized/application state without provider/platform/storage dependencies;
5. add deterministic mapping tests for loading, ready, degraded, not-found, failure, translation, timing, and karaoke-capability combinations relevant to the implemented surface;
6. add shared abstractions only after demonstrated semantic reuse;
7. implement each surface on dedicated topic branches and stop before merge for approval.
