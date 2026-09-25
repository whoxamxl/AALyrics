# Karaoke Architecture

## Status

Karaoke rendering remains deferred.

The shared LINE/WORD timing semantics are no longer owned by a Karaoke-specific engine. They belong to the shared Timing Semantic Engine defined in `docs/TIMING_ARCHITECTURE.md`.

The current authorized work stops at:

```text
canonical timed lyrics
        +
EffectiveLyricsPosition
        ↓
Timing Semantic Engine
        ↓
LyricsTimingProjection
```

Current Phone presentation will continue consuming only `activeLineIndex`, so the semantic engine can run in production without changing current UI behaviour.

Future Karaoke work begins **after** that projection and consumes additional semantic facts. Karaoke enablement is not an input to the Timing Semantic Engine.

## Purpose

Define the downstream Karaoke consumer/rendering boundary without duplicating timing semantics or tying shared facts to Compose, Android Auto templates, Canvas, spans, text layout, or animation cadence.

The working fork contains:

- `lyrics/KaraokeTiming.kt`
- `util/LyricWordLayout.kt`
- `ui/KaraokeSweepSpan.kt`

Migration classification:

- active-word boundary semantics from `KaraokeTiming` — **PRESERVE / REFACTOR** into the shared Timing Semantic Engine;
- lexical/display-range logic from `LyricWordLayout` — **PRESERVE / REFACTOR later** only if required by a concrete Karaoke renderer;
- Android `KaraokeSweepSpan` rendering — **REWRITE / REFACTOR later** for the target surface;
- arbitrary visual fallback durations — rendering policy only; do not move them into shared timing semantics.

## Ownership model

There are three distinct responsibilities:

```text
1. Timing semantics
   canonical timed lyrics
          +
   EffectiveLyricsPosition
          ↓
   Timing Semantic Engine
          ↓
   LyricsTimingProjection
   ├─ activeLineIndex
   ├─ activeWordIndex
   ├─ wordProgress
   └─ wordBoundary

2. Consumer policy
                     ┌────────────── Normal consumer
   TimingProjection ─┤
                     └────────────── Karaoke consumer

3. Surface rendering
   Karaoke consumer facts
          ├─ Phone Compose renderer
          └─ Automotive renderer
```

The Timing Semantic Engine must not know whether Karaoke is enabled.

The Karaoke consumer must not recalculate:

- active line;
- active word;
- word progress;
- word boundary;
- lyrics offset/effective position.

Surface renderers must not recalculate them either.

## Normal consumer

Normal presentation consumes only the timing facts needed for the existing non-Karaoke UI.

Current Phone behaviour remains:

```text
LINE_SYNC
    TimingProjection.activeLineIndex
            ↓
    existing current-line presentation

WORD_SYNC
    TimingProjection.activeLineIndex
            ↓
    existing line-oriented presentation
    activeWord/progress/boundary ignored

PLAIN
    no timed active line
            ↓
    existing plain behaviour
```

The fact that WORD semantics are computed does not authorize showing word highlighting or sweep animation.

## Karaoke consumer

Future Karaoke mode is a **consumer-selection/presentation concern**, not a semantic-engine mode.

Conceptually:

```text
LyricsTimingProjection
        +
Karaoke enabled
        ↓
Karaoke consumer
        ↓
Karaoke presentation facts
        ↓
surface renderer
```

The consumer may choose which already-computed facts are relevant to presentation, but it does not reinterpret their timing.

For WORD_SYNC, a future Karaoke consumer may use:

- `activeLineIndex`;
- `activeWordIndex`;
- `wordProgress`;
- `wordBoundary`.

For LINE_SYNC, Karaoke mode does not reveal additional source timing. The shared semantic result is still line-level only. Any future synthetic intra-line animation would be a separately authorized presentation policy and must be explicitly distinguishable from genuine WORD timing.

For PLAIN lyrics, Karaoke timing is unavailable.

## LINE_SYNC and Karaoke

There is no separate LINE-level Karaoke timing algorithm.

For LINE_SYNC:

```text
canonical line timestamps
        +
EffectiveLyricsPosition
        ↓
Timing Semantic Engine
        ↓
activeLineIndex
```

Normal and Karaoke presentation receive the same semantic line fact.

If their visuals differ later, that difference belongs to presentation/rendering only.

## WORD_SYNC and Karaoke

For WORD_SYNC:

```text
canonical line + word timestamps
        +
EffectiveLyricsPosition
        ↓
Timing Semantic Engine
        ↓
activeLineIndex
activeWordIndex
wordProgress
wordBoundary
        │
        ├─ Normal consumer
        │      └─ activeLineIndex only
        │
        └─ Karaoke consumer
               └─ full WORD facts
```

This preserves one semantic answer for a given lyrics document and effective position regardless of presentation mode.

## Boundary and progress ownership

The shared Timing Semantic Engine owns semantic timing boundaries such as:

- before the first word;
- active word;
- explicit gap after a word end;
- after the final explicitly-ended word;
- open-ended latest word;
- deterministic backward seek.

It also owns progress only when timing provides a defensible duration.

Karaoke consumer/rendering may decide **how** those facts look, but not **what time state exists**.

Examples of rendering-only decisions:

- completed/pending colors;
- gradient or clipping direction;
- easing;
- alpha/scale;
- animation cadence;
- whether GAP visually freezes, clears, or transitions;
- text layout/display-token grouping;
- final open-ended word visual fallback.

## Text layout boundary

Timed provider tokens and visible lexical units are not necessarily identical.

The working fork's `LyricWordLayout` contains useful evidence for:

- preserving source text;
- aligning provider timing tokens to visible lexical ranges;
- grouping character/syllable timing into readable display units;
- conservative fallback when provider tokens do not align credibly.

Those are **display mapping** semantics, not playback timing semantics.

Therefore they must not enter `:core:timing` during the Timing Semantic Engine slice.

If/when Karaoke rendering is authorized, introduce the smallest framework-neutral display-mapping responsibility justified by the renderer. Do not mutate canonical `TimedWord` timestamps to make layout easier.

## Rendering boundary

Phone and Android Auto are not required to render Karaoke identically.

```text
shared TimingProjection
        ↓
Karaoke consumer/presenter
        │
        ├─ Phone-specific presentation
        │      ↓
        │   Compose rendering
        │
        └─ Automotive-specific presentation
               ↓
            host-supported rendering
```

Phone may eventually support continuous sweep animation.

Automotive surfaces may be constrained to coarser emphasis/update cadence by host APIs.

Both must consume shared timing facts and must not duplicate current-line/current-word algorithms.

A dedicated `:core:karaoke` module is **not pre-authorized** merely because Karaoke exists. If the eventual consumer needs enough reusable framework-neutral policy to justify a module, that decision belongs to the Karaoke implementation slice. If the consumer is only thin presentation mapping, a new core module may be unnecessary.

## Karaoke enablement ownership

Karaoke ON/OFF is presentation/application state.

It does not:

- alter canonical lyrics;
- alter `EffectiveLyricsPosition`;
- change Timing Semantic Engine output for the same input;
- trigger provider refetch solely to switch rendering;
- reapply timing offset.

If a later product decision changes Lyrics Provider preference when Karaoke is enabled, that is a separate lookup/preference policy and must not be confused with timing semantics.

## Translation relationship

Translation remains optional derived text attached to canonical lyric identity.

Karaoke timing:

- follows canonical timing;
- does not maintain a translated clock;
- does not derive word timing from translated text.

If translated text is displayed alongside Karaoke later, presentation must attach it to the canonical semantic line/segment identity without changing the shared timing result.

## Current behaviour preservation

The Timing Semantic Engine slice must not change current user-visible Karaoke behaviour because Karaoke remains disabled/unwired.

During that slice:

- current Phone LINE_SYNC presentation stays unchanged;
- current Phone WORD_SYNC remains line-oriented;
- no word sweep/highlight becomes visible;
- current Karaoke setting/affordance remains disabled/unwired;
- Android Auto Karaoke remains unchanged/unimplemented.

Computing semantic WORD facts internally is not itself a user-visible feature.

## Deferred Karaoke decisions

Until the Karaoke consumer/rendering slice is explicitly authorized, defer:

- Karaoke enablement persistence and settings behaviour;
- exact consumer/presenter type names;
- whether a dedicated Karaoke Gradle module is warranted;
- Phone visual styling;
- completed/pending color policy;
- sweep animation/easing;
- update cadence;
- lexical display-range implementation;
- final open-ended word visual fallback duration;
- LINE-only synthetic intra-line progress;
- Android Auto emphasis/update strategy;
- manual browse/follow interaction while Karaoke is active;
- translated Karaoke presentation.

## Future Karaoke implementation gate

Before implementing Karaoke consumer/rendering:

1. require the shared Timing Semantic Engine to be implemented and regression-validated;
2. consume `LyricsTimingProjection`; do not create a second timing algorithm;
3. re-check working-fork layout/rendering evidence at the then-current revision;
4. define the smallest consumer/presentation contract required by Phone and/or automotive;
5. keep Karaoke enablement outside the timing engine;
6. preserve genuine WORD timing separately from any synthesized visual policy;
7. test rendering/consumer behaviour separately from timing-engine tests;
8. keep Phone and automotive presentation independent;
9. stop before merge according to `AGENTS.md`.
