# Karaoke Architecture

## Status

Phone Karaoke presentation mapping and rendering are authorized on `feature/phone-karaoke-rendering`. Android Auto Karaoke remains deferred to documentation-only Phase 11.4d.

The shared LINE/WORD timing semantics are no longer owned by a Karaoke-specific engine. They belong to the shared Timing Semantic Engine defined in `docs/TIMING_ARCHITECTURE.md`.

The merged shared timing foundation ends at `LyricsTimingProjection`. The active Phone slice begins **after** that projection:

```text
canonical timed lyrics
        +
EffectiveLyricsPosition
        ↓
Timing Semantic Engine
        ↓
LyricsTimingProjection
        ↓
Phone Karaoke presentation mapping
        ↓
current-line continuous sweep
```

Normal Phone presentation continues to consume only the facts it needs. Effective Karaoke may additionally consume the existing WORD facts, but Karaoke enablement is never an input to the Timing Semantic Engine.

## Purpose

Define the downstream Karaoke consumer/rendering boundary without duplicating timing semantics or tying shared facts to Compose, Android Auto templates, Canvas, spans, text layout, or animation cadence.

The working fork contains:

- `lyrics/KaraokeTiming.kt`
- `util/LyricWordLayout.kt`
- `ui/KaraokeSweepSpan.kt`

Migration classification for the active Phone slice:

- active-word boundary semantics from `KaraokeTiming` — already **PRESERVED / REFACTORED** into the shared Timing Semantic Engine; do not migrate this timing owner again;
- lexical/display-range logic from `LyricWordLayout` — **PRESERVE / REFACTOR now** into the smallest Phone presentation/display-mapping seam;
- same-visible-range grouping from `PhoneKaraokeSweep` in `ui/KaraokeSweepSpan.kt` — **PRESERVE / REFACTOR now** as display-group policy downstream of the shared active-word decision;
- `KaraokeSweepSpan` drawing behavior — preserve the useful continuous left-to-right sweep idea, but **REWRITE for Compose** rather than transplanting Android `ReplacementSpan`;
- working-fork layout/grouping tests — **PRESERVE / ADAPT** wherever their behavior is still part of the approved AALyrics contract;
- the working fork's final-group `650ms` fallback — **PRESERVE only as Phone presentation policy**. It is a visual duration for an otherwise open-ended final display group; it must never be inserted into `:core:timing`, canonical timestamps, or semantic `wordProgress`.

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

The fact that WORD semantics are computed does not by itself activate Karaoke; the documented Phone feature gate and live mode still control presentation.

## Karaoke consumer

Karaoke mode is a **consumer-selection/presentation concern**, not a semantic-engine mode.

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

For WORD_SYNC, the authorized Phone Karaoke consumer may use:

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

The working fork's `LyricWordLayout` is the mature implementation reference for:

- preserving canonical source text;
- reconstructing provider separators without inventing spaces;
- aligning provider timing tokens to visible lexical ranges;
- case-insensitive and Unicode-normalized sequential alignment;
- repeated-word/order-safe alignment;
- grouping character/syllable fragments into readable display units;
- Japanese lexical/display grouping;
- conservative credibility checks and fallback when provider tokens do not align sufficiently.

For the active Phone Karaoke slice, these behaviors should be **preserved/refactored rather than independently reinvented**. Relevant working-fork tests should be adapted before adding new behavior.

Those are **display mapping** semantics, not playback timing semantics. They must stay downstream of `:core:timing`, and canonical `TimedWord` timestamps must remain immutable.

### Same-visible-range token grouping

One visible word may correspond to multiple consecutive timing tokens.

Example:

```text
canonical text:  Provider timing works
timing tokens:   Pro | vi | der | timing | works
visible ranges:  └──── Provider ────┘
```

AALyrics must not restart a full visible-word sweep for `Pro`, then `vi`, then `der`.

Instead:

1. `LyricsTimingProjection.activeWordIndex` remains the authority for which timing token is semantically active;
2. presentation mapping resolves that token to its visible character range;
3. consecutive tokens that map to that same visible range form one **display group**;
4. that visible range sweeps once from the first grouped token's start to the group's defensible end;
5. the group end may use the final grouped token's explicit end, or the next token start when that provides the natural end;
6. if a non-final group has no defensible end, use normal current-line styling rather than invent timing;
7. if the final visible display group has neither an explicit end nor a following token start, Phone may use the working fork's `650ms` visual fallback measured from that display group's start. This fallback exists only for rendering and does not alter semantic timing.

This display-group interval exists only to render one readable lexical unit smoothly. It must not select a different active token, alter `wordBoundary`, rewrite source timestamps, or become a second Timing Semantic Engine.

For the common one-token/one-visible-word case, the display group is that token itself and the shared semantic `wordProgress` is the sweep progress directly.

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

Phone 11.4c uses continuous current-line sweep animation.

Automotive surfaces may be constrained to coarser emphasis/update cadence by host APIs.

Both must consume shared timing facts and must not duplicate current-line/current-word algorithms.

A dedicated `:core:karaoke` module is not justified by default. The active Phone slice should use the smallest presentation seam supported by implementation evidence; introduce a new shared module only if real reusable framework-neutral policy emerges.

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

## 11.4a baseline preserved by the Phone slice

The merged Timing Semantic Engine established a behavior-preserving baseline before Karaoke presentation was authorized.

The active Phone slice must preserve the same non-Karaoke behavior whenever its feature gate or live mode is OFF:

- Phone LINE_SYNC stays unchanged and never synthesizes Karaoke;
- Phone WORD_SYNC remains the existing line-oriented presentation unless effective Phone Karaoke is active;
- PLAIN stays unchanged;
- Android Auto remains unchanged/unimplemented for Karaoke;
- computing semantic WORD facts does not itself imply visible Karaoke.

## Remaining deferred decisions

The active Phone slice fixes feature gating, WORD-only presentation mapping, continuous sweep, working-fork display grouping, and the Phone-only 650ms final-group visual fallback.

Still deferred:

- a dedicated Karaoke Gradle module unless implementation evidence actually justifies one;
- LINE-only synthetic intra-line progress;
- Android Auto Karaoke rendering/update strategy;
- any translated-word Karaoke timing;
- broader fullscreen/Performance-mode Karaoke presentation.

The active implementation must continue to consume `LyricsTimingProjection`, keep enablement outside the timing engine, test presentation separately from timing semantics, keep Phone and automotive presentation independent, and stop before merge according to `AGENTS.md`.


## Approved Phone Karaoke policy

The first production Karaoke surface is intentionally narrow:

- Karaoke rendering is **WORD_SYNC only**.
- LINE_SYNC must not synthesize or imitate Karaoke progress.
- PLAIN lyrics never enter Karaoke presentation.
- Phone uses the **continuous current-word sweep** direction from the working fork's normal Phone renderer, not the Performance-mode whole-word pulse renderer.
- The existing LyricsViewport line focus, Follow/Browse ownership, Translation secondary text, and scroll geometry remain the outer presentation model.

The activation contract is:

```text
Advanced > Experimental features > Karaoke mode
        │
        └─ feature gate ON
                ↓
Expanded Player > Quick controls
        └─ Karaoke toggle visible
                │
                └─ runtime toggle ON
                        +
                   source == WORD_SYNC
                        ↓
              Phone Karaoke display active
```

Both persisted booleans default to **OFF**.

The Experimental toggle is a feature-availability gate, not the live Karaoke mode switch. Turning the Experimental gate OFF must also turn the Quick-controls Karaoke state OFF so re-enabling the experiment cannot silently reactivate Karaoke.

The Quick-controls Karaoke toggle is shown only while the Experimental gate is enabled.

Effective Phone Karaoke is therefore:

```text
karaokeFeatureEnabled &&
karaokeModeEnabled &&
sourceSyncType == WORD
```

The timing engine remains unaware of both toggles.

### 11.4b — Phone presentation mapping

The application mapper may consume `LyricsTimingProjection` and canonical `TimedWord` text to create presentation-ready display ranges.

Stable rules:

- preserve canonical line text;
- preserve/refactor the working fork's `LyricWordLayout` behavior instead of generating a new token-layout algorithm without evidence;
- align provider timing tokens conservatively to ranges in canonical text;
- group consecutive timing tokens that resolve to the same visible range so that readable words such as `Provider` sweep once rather than restarting for `Pro` / `vi` / `der`;
- keep `LyricsTimingProjection.activeWordIndex` as the semantic active-token authority;
- for a one-token display group, use shared `wordProgress` directly;
- for a multi-token display group, derive only presentation-ready group sweep progress from the active semantic token plus the canonical grouped timing interval; this must not change active-word/boundary semantics;
- do not rewrite `TimedWord` timestamps;
- if token-to-text alignment is not credible, render the normal current line rather than invent a Karaoke range;
- if an ordinary/non-final display group has no defensible end, fall back to normal styling;
- for the final open-ended display group only, preserve the working fork's 650ms Phone visual fallback without converting it into shared semantic progress;
- expose Karaoke presentation facts only while effective Phone Karaoke is active;
- when Karaoke is disabled, WORD source keeps the existing line-oriented Phone behaviour.

Display mapping and display-group progress are presentation policy, not a second timing engine.

### 11.4c — Phone continuous sweep

For the current line only:

- text before the active token range is completed/emphasized;
- the active token range uses continuous left-to-right progress;
- text after the active token range remains pending/dim;
- translated text is never word-swept;
- existing line scale/alpha/focus behaviour continues to wrap the whole canonical + translated row;
- semantic GAP / BEFORE_FIRST / unavailable-progress states may fall back to normal current-line styling rather than fabricate timing;
- no arbitrary synthetic LINE_SYNC progress is permitted.

A continuous sweep may use a presentation-only gradient/clip implementation. The renderer consumes presentation-ready range/progress and must never calculate the active word itself. For ordinary one-token groups that progress is the shared `wordProgress`; multi-token visible groups may receive mapper-derived display-group progress as defined above.

### 11.4d — Android Auto Karaoke — documented, deferred

No Android Auto Karaoke production code is authorized in this slice.

Future Android Auto work must:

- consume the same shared `LyricsTimingProjection`;
- remain WORD_SYNC only unless a later explicit product decision changes that rule;
- respect host update/throttling constraints;
- not copy Phone Compose rendering machinery;
- not duplicate line/word timing semantics;
- define its own host-appropriate emphasis strategy in a dedicated implementation slice.


## Phone clock invariants

Karaoke is not allowed to select a different clock.

For the same playback timing sample and monotonic instant:

- Karaoke OFF and Karaoke ON must produce the same projected playback position;
- they must produce the same `EffectiveLyricsPosition`;
- they must produce the same shared `LyricsTimingProjection`;
- enabling Karaoke only decides whether the already-computed WORD presentation facts are rendered.

Some Android MediaSession sources omit `lastPositionUpdateTime`. For Phone WORD_SYNC presentation, AALyrics may anchor the received `positionMs` to the monotonic time at which that timing sample was received so continuous progress can be projected between coarse callbacks.

That fallback anchor:

- applies to WORD_SYNC independently of the Karaoke live toggle;
- is ignored when the source supplies a valid monotonic position timestamp;
- resets for an actual timing-sample change such as position, playback rate, playback status, track identity, or source timestamp;
- must not reset merely because unrelated track metadata such as duration is updated;
- may change presentation update cadence when Karaoke is active, but cadence must never change clock semantics.

Verbose Details for WORD_SYNC must consume the same Phone fallback clock so diagnostics cannot disagree with the Lyrics surface.
