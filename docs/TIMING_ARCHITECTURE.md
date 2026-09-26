# Timing and Calibration Architecture

## Status

Phase 11.3a (effective-position foundation), Phase 11.3b (existing current-line integration), and Phase 11.4a (shared Timing Semantic Engine) are implemented and validated. Phase 11.4b/11.4c Phone Karaoke presentation is implemented on top of that shared engine. Android Auto Now Playing intentionally remains line-oriented and does not adopt Karaoke presentation.

The shared **Timing Semantic Engine** consumes canonical timed lyrics + `EffectiveLyricsPosition` and produces deterministic line/word/progress/boundary facts. The engine is mode-agnostic; Karaoke ON/OFF is not an input.

Normal Phone timed presentation consumes the active-line fact. The authorized Phone WORD_SYNC Karaoke consumer may additionally consume the shared word/progress/boundary facts downstream without changing timing semantics. The authorized Android Auto Now Playing completion consumes the same shared active-line fact for both LINE_SYNC and WORD_SYNC while intentionally ignoring word/progress/boundary facts. Sync controls, persistence, user calibration, and Android Auto Karaoke remain outside this scope. Android Auto host validation currently uses a fixed presentation-only `-75 ms` audio-path compensation; this does not alter the normalized playback clock, canonical timestamps, or calibration ownership.

The working fork contains `lyrics/KaraokeTiming.kt` and `util/SyncCalibration.kt`. They were re-checked at `v1.13.0` on 2026-09-25. `SyncCalibration.offsetForTap(targetTimeMs, rawPositionMs) = targetTimeMs - rawPositionMs` preserves the approved sign convention. `KaraokeTiming` provides mature active-word boundary evidence that is **PRESERVE / REFACTOR**; Android-specific sweep/layout behaviour remains outside the shared engine.

## Purpose

Create one timing boundary that every later synchronized presentation can trust.

AALyrics must keep three ideas separate:

1. **canonical source timing** — provider/parser timestamps stored in normalized lyrics;
2. **projected playback position** — AALyrics' best current estimate of the media position;
3. **effective lyrics position** — the virtual position used only when comparing playback against lyric timestamps.

The first implementation does not rewrite lyric timestamps. It adjusts the position used to read those timestamps.

## Stable terminology

### Canonical source timing

LINE/WORD timestamps obtained from Lyrics Providers and normalized into AALyrics models.

Canonical timing is provider truth for the resolved lyric document. Calibration must never overwrite it in place.

### Projected playback position

The current framework-neutral playback position after the existing MediaSession snapshot/monotonic projection logic.

This remains the real playback clock used for ordinary playback facts. Timing calibration does not mutate the MediaSession, seek the player, or pretend that the actual media position changed.

### Lyrics timing offset

A signed duration used only to shift the lyric-reading clock.

The sign convention is a product contract:

```text
positive offset  -> advance lyrics
negative offset  -> delay lyrics
zero offset      -> preserve current behavior
```

Human interpretation:

```text
lyrics are behind the music -> press/use +
lyrics are ahead of the music -> press/use -
```

Example at the same real media position:

```text
before:
She'd take the world off | my shoulders if it was ever hard to move

positive offset:
She'd take the world off my shoulders | if it was ever hard to move

negative offset:
She'd take the | world off my shoulders if it was ever hard to move
```

This convention must stay identical across future Phone Sync controls, diagnostics, Karaoke, and Android Auto.

### Effective lyrics position

The derived, lyrics-only virtual clock:

```text
effectiveLyricsPositionMs
    = projectedPlaybackPositionMs + lyricsTimingOffsetMs
```

Example:

```text
projected playback position = 31,200 ms
lyrics timing offset        =   +800 ms
effective lyrics position   = 32,000 ms
```

The media is still at 31,200 ms. Only lyric timing comparisons behave as though they are at 32,000 ms.

## Why position is shifted instead of source timestamps

A constant offset could be expressed mathematically by rewriting every source timestamp with the opposite sign. AALyrics deliberately does not use that representation.

The canonical representation is:

```text
canonical lyric timestamps remain unchanged
                    +
projected playback position + lyrics offset
                    ↓
effective lyrics position
                    ↓
compare against canonical timestamps
```

Benefits:

- provider/source timestamps remain inspectable and comparable;
- reset-to-source means offset = 0 rather than reconstructing timestamps;
- Translation stays attached to canonical line identity without inheriting another clock;
- later Karaoke consumes one effective position rather than reapplying calibration;
- Phone and Android Auto cannot accidentally implement opposite sign conventions;
- future persistence can store calibration parameters rather than modified lyrics.

## First foundation boundary

The first implementation should introduce a small pure Kotlin/JVM timing capability. The intended home is a dedicated `:core:timing` module unless concrete implementation evidence shows a smaller existing pure-core placement is materially cleaner.

The foundation needs only:

- a signed lyrics-offset value/semantic contract;
- a pure transform from projected playback position + offset to effective lyrics position;
- zero offset as the neutral/default behavior;
- framework-independent tests for the transform and sign convention.

The foundation engine is intentionally stateless. It does not own SharedPreferences/DataStore, track lifecycle, UI state, or MediaSession objects.

Conceptually:

```text
projectedPlaybackPositionMs
            +
lyricsTimingOffsetMs
            ↓
pure timing transform
            ↓
effectiveLyricsPositionMs
```

The exact Kotlin type names may follow repository conventions, but the semantics above are fixed.

## Timing semantic projection

The effective-position transform and timing semantics are separate layers inside the same timing capability:

```text
canonical LINE/WORD timestamps
            +
EffectiveLyricsPosition
            ↓
Timing Semantic Engine
            ↓
active line / active word / word progress / word boundary
```

The semantic engine is not a Karaoke-mode engine. It returns the same facts regardless of how presentation later chooses to consume them.

Phase 11.3b introduced effective position into the app-local current-line selector. Phase 11.4a replaces that duplicate selector with the shared projection after parity tests established identical line behaviour.

Current Phone presentation continues to consume only the active-line result, so computing additional WORD facts must not by itself change UI behaviour.

## Android Auto Now Playing consumer

The authorized legacy Android Auto Now Playing completion is a normal, line-oriented consumer of the shared timing semantics.

```text
normalized PlaybackSnapshot
        ↓
shared projected playback clock
        ↓
effectiveLyricsPosition(..., LyricsTimingOffset.ZERO)
        ↓
projectLyricsTiming(...)
        ↓
activeLineIndex
        ↓
Automotive current-line presentation
```

Stable rules for this surface:

- LINE_SYNC and WORD_SYNC both use `LyricsTimingProjection.activeLineIndex`;
- Automotive does not consume `activeWordIndex`, `wordProgress`, or `wordBoundary` for visible Now Playing behavior;
- PLAIN lyrics are not pseudo-synchronized;
- Automotive must retire its independent current-line selector rather than keep a parallel `startMs <= position` implementation;
- Automotive must not create a UI-observation-time fallback clock;
- a valid source `positionUpdatedAtMonotonicMs` remains authoritative and the existing `positionSampledAtMonotonicMs` is the fallback when the source timestamp is unavailable;
- production offset remains `LyricsTimingOffset.ZERO` in this slice;
- this authorization does not add Sync controls, calibration persistence, or Android Auto Karaoke.

The detailed presentation contract is `docs/ANDROID_AUTO_NOW_PLAYING.md`.

## Playback clock ownership

Playback projection remains upstream. Before timing sees a playback sample, `:platform:media` is responsible for making the snapshot internally coherent:

- source timestamp contradictions are reconciled at the MediaSession boundary;
- a fallback sample timestamp is captured when the platform snapshot is sampled, not when UI later observes it;
- during track-metadata stabilization, track/source identity and position/status/rate/timestamps are committed as one logical sample rather than cross-spliced between tracks.

The timing layer therefore accepts projected playback position as an already-normalized fact. It does not carry MediaSession drift state, metadata-stabilization state, or a second identity/timeline repair mechanism.

Timing calibration must not:

- own MediaSession discovery;
- alter or seek actual playback;
- duplicate monotonic playback projection;
- infer a new clock from UI animation frames;
- change playback rate;
- start provider work.

Pause, resume, seek, playback-rate changes, and track changes first affect the existing projected playback position. Timing then applies only the current lyrics offset to that projected value.

## Source timing immutability

Provider adapters may normalize provider-native timing units into AALyrics domain models, but they must not apply user/device calibration.

Rules:

- canonical LINE and WORD timestamps stay unchanged;
- calibration changes do not refetch Lyrics Providers;
- calibration changes do not rerun cross-provider candidate selection;
- cacheable canonical lyrics must not silently contain calibrated timestamps;
- Translation never owns or mutates timing.

## Relationship to LINE_SYNC

LINE active-line semantics are fixed by current production behaviour:

```text
active line = latest TimedLyricLine whose startMs <= effective lyrics position
```

Example:

```text
line A start = 10,000 ms
line B start = 15,000 ms
line C start = 20,000 ms

effective lyrics position = 17,500 ms
=> line B is active
```

The projection returns the original `LyricsDocument.lines` index. Before the first timed line (including a negative effective position), no line is active. Exact start timestamps activate the new line. Line `endMs` does not terminate the active-line fact in this slice because current production current-line semantics do not use it.

The new engine must prove parity before replacing the existing app-local selector. Playback progress remains tied to the real projected playback position.

## Relationship to WORD_SYNC

WORD timing follows the same effective clock and is part of the shared timing semantic projection:

```text
canonical word timestamps
        +
EffectiveLyricsPosition
        ↓
shared Timing Semantic Engine
        ↓
active word / word progress / word boundary
```

Stable word-selection rules:

- choose the latest word whose start has occurred;
- explicit `endMs` is exclusive and may create an unhighlighted gap;
- a newer explicitly-ended word does not reactivate an older open-ended word;
- an open-ended word remains active until a later word starts;
- backward seek is stateless and deterministically recomputes earlier facts.

Word progress uses an explicit end when available. When an open-ended word has a later word with a later start, that next start may bound progress. If no defensible end exists, the word may remain active while progress is unavailable.

The shared engine must not invent visual fallback durations, lexical display ranges, sweep easing, or Karaoke enablement. Those are downstream consumer/rendering concerns.

Karaoke must consume this semantic output and must never add the timing offset again.

## Relationship to Translation

Translation remains additive text attached to canonical line identity.

Translation:

- does not change source timestamps;
- does not calculate the lyrics offset;
- does not maintain a separate translated playback clock;
- follows the canonical row's timing when rendered.

## Calibration scope and persistence

The sign convention and effective-position equation are now fixed. The **scope** of a non-zero offset is deliberately not fixed by the foundation.

Possible later scopes include:

- transient/session offset;
- global user offset;
- Phone-specific presentation compensation;
- Android Auto-specific presentation compensation;
- track/provider-specific correction only if later evidence justifies it.

Likewise, persistence is deferred. The foundation must not add a durable preference merely because a value type exists.

When persistence is eventually authorized, the implementing PR must re-evaluate the `Reset AALyrics` contract.

## Offset versus drift

Constant offset and playback drift are different problems.

Phase 11.3a implements only a constant offset transform.

Future drift/rate correction, if evidence requires it, may enrich the timing policy behind the same effective-position boundary. It must not reinterpret the sign convention or require callers to duplicate timing formulas.

## Negative and out-of-range effective positions

The timing transform should remain a transparent signed calculation rather than silently mutating canonical timestamps or clamping to a lyric/track boundary.

A negative effective lyrics position simply represents a point before the beginning of timed lyrics. Downstream line/word lookup decides that no timed unit is active yet.

Track-duration clamping remains playback/projection policy, not calibration policy.

Extreme arithmetic-overflow hardening may follow normal repository conventions; it must not introduce user-visible semantics that contradict the equation above.

## Presentation and Sync UI boundary

The current Phone `SyncScreen` remains a deliberate placeholder.

The timing foundation does not authorize:

- +/- buttons;
- sliders;
- offset text fields;
- tap-to-sync;
- auto-calibration;
- persistence;
- provider-specific correction;
- Phone/Android Auto-specific offset settings.

When Sync UI is later authorized, it must emit semantic offset changes to application/capability ownership. The UI must not implement the timing equation itself.

## Diagnostics

A later integration may expose presentation-ready timing diagnostics such as:

```text
Projected playback position
Lyrics timing offset
Effective lyrics position
```

Verbose Details must consume already-owned values and must not become the timing owner.

## Deterministic tests

The effective-position foundation already covers zero/positive/negative offset semantics.

The shared semantic engine must additionally cover:

- LINE parity with current production selection;
- mixed plain/timed document indices;
- before-first, exact-start, between-line, after-last, negative-position, and backward-seek LINE cases;
- WORD before-first, exact-start, explicit-end, gap, after-last, open-ended, newer-word-ended, and backward-seek cases;
- explicit-end word progress;
- inferred-next-start word progress;
- unavailable progress for an open-ended final word;
- zero-duration safety;
- Phone mapper regression proving current WORD/LINE/PLAIN presentation remains unchanged when the engine is wired.

## Implementation sequence

Current execution order is intentionally engine-first and UX-later:

```text
Phase 11.3a — Effective Timing Foundation                 ✅
    pure offset model + effective lyrics position
            ↓
Phase 11.3b — Existing timed-lyrics integration          ✅
    current Phone line path consumes effective position
            ↓
Phase 11.4a — Shared Timing Semantic Engine              implemented
    LINE + WORD + progress + boundary semantics
    + behaviour-preserving Phone wiring
            ↓
Phase 11.3c — Sync calibration UX                        deferred
    controls + scope + persistence
            ↓
Phase 11.4b/11.4c — Phone Karaoke consumer/rendering     ✅
    consumes shared WORD semantic facts; does not recompute them
```

Phase numbering groups capabilities; implementation order follows dependency and regression safety.

The active semantic-engine work continues on the existing timing topic branch. Its previous Draft PR was intentionally closed before review so implementation can continue without treating a partial checkpoint as final review scope.

## Architecture guardrails

The implementation must preserve these invariants:

- source timestamps stay canonical and recoverable;
- positive means advance lyrics, negative means delay lyrics;
- the only foundation equation is `projectedPosition + offset`;
- calibration math is not duplicated in Phone or automotive UI;
- timing changes do not refetch providers or reselect lyrics;
- the timing capability remains framework-independent;
- `:core:timing` may depend on `:core:model` for semantic projection, but not Android/UI/provider/Translation/runtime infrastructure;
- the shared semantic engine does not accept Karaoke enablement/mode as input;
- normal Phone timed presentation consumes the active-line fact, while the authorized Phone WORD_SYNC Karaoke path may consume the shared word/progress/boundary facts downstream;
- Sync UI and persistence remain out of scope until separately authorized;
- Karaoke consumers use shared semantic output and never reapply the offset or duplicate line/word timing semantics.
