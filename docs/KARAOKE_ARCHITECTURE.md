# Karaoke Architecture Foundation

## Purpose

Define the seam for future karaoke behavior without tying timing semantics to Compose, Android Auto host templates, drawing primitives, or a specific animation implementation.

The working fork contains `lyrics/KaraokeTiming.kt`, `util/LyricWordLayout.kt`, and Android-specific rendering such as `ui/KaraokeSweepSpan.kt`. Migration intent is **PRESERVE / REFACTOR** for proven timing/layout semantics and **REWRITE / REFACTOR** for Android-specific rendering.

## Stable ownership rules

Karaoke has two distinct layers:

1. a pure semantic projection engine;
2. surface-specific rendering.

```text
timed lyrics
+ effective playback position
+ effective timing/calibration
        ↓
karaoke projection
        ↓
semantic playback facts
        ↓
Phone renderer / Android Auto renderer
```

The projection layer must not know about Compose, Canvas, `Span`, `CarText`, Android Auto templates, or view lifecycle.

The rendering layer must not independently decide current line/word semantics.

## Projection responsibility

The future karaoke projection boundary may expose facts such as:

- active line identity/index;
- active word/segment identity;
- elapsed/progress fraction within the active unit;
- previous/current/next relation;
- line or word transition boundaries;
- whether the current lyrics payload supports word-level progression.

The exact output type is deferred. The stable rule is that these are semantic facts derived from normalized lyrics and effective timing, not UI objects.

## Timing source

Karaoke consumes effective timing from the timing/calibration capability.

It must not:

- mutate source timestamps;
- reapply calibration independently;
- infer provider-specific timing units;
- talk to providers or parsers;
- own playback-source normalization.

This keeps one source of truth for timestamp transformation.

## Playback clock

Playback position is an input to projection. The engine should be deterministic for a given lyrics payload, effective timing policy, and playback position.

Animation frame scheduling, recomposition cadence, template invalidation cadence, and interpolation frequency are presentation concerns. They must not become part of karaoke domain semantics.

## LINE versus WORD lyrics

The architecture must support graceful capability differences.

```text
Plain lyrics       -> no timed karaoke projection
Line-timed lyrics  -> line-level projection
Word-timed lyrics  -> line + word/segment projection
```

A future implementation may provide synthesized/interpolated progress for line-timed lyrics if explicitly justified, but it must distinguish synthesized behavior from genuine word timing and must not falsify the normalized source model.

## Rendering boundary

Phone and Android Auto are not required to render karaoke identically.

Phone may use rich continuous visual progress. Android Auto host-rendered surfaces may expose only host-supported emphasis and update cadence.

Both should consume the same semantic projection where practical:

```text
KaraokeProjection
      ├─> Phone Compose rendering
      └─> Automotive host-model rendering
```

Surface constraints may require additional local mapping, but not duplicate current-line/current-word algorithms.

## Translation relationship

Translation is optional derived content and does not determine karaoke timing.

If translated text is shown with karaoke later, it should attach to semantic line/segment identity from the canonical lyrics projection rather than running a separate playback clock.

## Deferred decisions

Do not decide in this foundation slice:

- exact projection DTO/interface names;
- frame/update frequency;
- easing or sweep animation;
- interpolation between sparse timestamps;
- behavior for malformed overlapping word timings;
- host update throttling for Android Auto;
- whether line-only lyrics receive synthetic intra-line progress;
- text layout measurement algorithms;
- Phone visual styling;
- Android Auto emphasis styling;
- manual browse/follow interaction.

## Future implementation gate

Before implementing karaoke:

1. re-check working-fork `KaraokeTiming`, `LyricWordLayout`, rendering call sites, and regressions;
2. separate semantics worth preserving from Android-specific rendering machinery;
3. define deterministic boundary behavior at exact word/line start/end timestamps;
4. consume effective timing rather than raw/calibrated timestamps independently;
5. test plain, line-timed, word-timed, malformed, boundary, seek, and track-change cases;
6. prove Phone and automotive adapters can consume the projection without importing each other's UI technology;
7. implement projection and surface rendering in separate responsibilities and stop before merge for approval.
