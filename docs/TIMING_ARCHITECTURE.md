# Timing and Calibration Architecture

## Status

The timing/calibration architecture is now authorized for a first implementation foundation.

The first slice establishes a framework-independent effective-lyrics clock and its sign semantics only. It does **not** add Sync controls, persistence, per-track/provider/device calibration, drift correction, or Karaoke projection.

The working fork contains `lyrics/KaraokeTiming.kt` and `util/SyncCalibration.kt`. These remain behavioral references to re-check before implementation. Proven timing math may be **PRESERVE / REFACTOR**; Android/UI coupling must not be carried into the new boundary.

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

## What the foundation does not decide

The first foundation does **not** decide which line or word is active.

That is a downstream semantic projection:

```text
canonical LINE/WORD timestamps
            +
effective lyrics position
            ↓
current line / current word / progress
```

Existing LINE_SYNC lookup can later consume effective lyrics position instead of raw projected playback position. Future Karaoke may derive word/progress facts from the same effective position.

The timing foundation itself must not grow a second current-line/current-word implementation merely to prove the offset transform.

## Playback clock ownership

The existing playback projection remains upstream.

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

LINE_SYNC will eventually compare canonical line starts against effective lyrics position.

Example:

```text
line A start = 10,000 ms
line B start = 15,000 ms
line C start = 20,000 ms

effective lyrics position = 17,500 ms
=> line B is active under the existing line-boundary semantics
```

The first foundation does not change the existing line-boundary semantics. It only provides the position that later integration will feed into them.

## Relationship to WORD_SYNC / Karaoke

WORD timing follows the same clock rule.

Future Karaoke consumes:

```text
canonical word/segment timestamps
        +
effective lyrics position
        ↓
karaoke semantic projection
        ↓
active line / active word / progress
```

Karaoke must never add the offset again.

The foundation does not implement:

- active-word calculation;
- word-progress interpolation;
- sweep animation;
- malformed/overlapping-word policy;
- line-only synthetic progress.

Those remain Phase 11.4 concerns.

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

## Deterministic foundation tests

The first timing foundation must cover at least:

- zero offset preserves projected playback position exactly;
- positive offset advances effective lyrics position;
- negative offset delays effective lyrics position;
- positive and negative examples preserve the documented sign convention;
- negative effective position is representable without rewriting source timing;
- canonical source timestamps are not inputs mutated by the transform.

LINE/WORD boundary, seek, pause/resume, playback-rate, and track-change integration tests belong to the later integration slice unless the foundation implementation directly touches those call sites.

## Implementation sequence

The intended sequence is:

```text
Phase 11.3a — Effective Timing Foundation
    pure offset model + effective lyrics position engine
            ↓
Phase 11.3b — Existing timed-lyrics integration
    route Phone/current-line timing through effective lyrics position
            ↓
Phase 11.3c — Sync calibration UX
    user controls + scope/persistence only after explicit decisions
            ↓
Phase 11.4 — Karaoke / WORD projection
    consume the same effective lyrics position
```

Do not collapse these phases into one PR.

## Architecture guardrails

The implementation must preserve these invariants:

- source timestamps stay canonical and recoverable;
- positive means advance lyrics, negative means delay lyrics;
- the only foundation equation is `projectedPosition + offset`;
- calibration math is not duplicated in Phone or automotive UI;
- timing changes do not refetch providers or reselect lyrics;
- the foundation is framework-independent;
- Sync UI and persistence remain out of scope until separately authorized;
- Karaoke consumes effective lyrics position and never reapplies the offset.
