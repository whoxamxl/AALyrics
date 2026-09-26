# Karaoke Architecture

## Status

Phone Karaoke presentation mapping and rendering are implemented. Android Auto Now Playing intentionally does not adopt Karaoke presentation: LINE_SYNC and WORD_SYNC are both line-oriented and consume only the shared active-line fact. Any future Android Auto Karaoke proposal requires a new explicit product decision rather than being implied by the Phone implementation.

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
- the working fork's final-group `650ms` fallback — **PRESERVE only as the no-evidence Phone presentation fallback**. When an otherwise open-ended final display group has useful local provider cadence, Phone may infer its visual end from that cadence first. Neither inferred nor fallback duration may enter `:core:timing`, canonical timestamps, or semantic `wordProgress`.

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
- text layout/display-token grouping;
- final open-ended word visual fallback.

### Phone boundary presentation contract

Phone Karaoke keeps boundary state separate from sweep animation state. A missing active sweep does **not** by itself mean that Karaoke presentation is unavailable.

For a credibly mappable WORD line:

- `BEFORE_FIRST` -> completed prefix length `0`; the whole current line remains pending/secondary and no sweep runs;
- `ACTIVE` -> completed prefix is primary, the active display group sweeps left-to-right, and the future suffix remains pending/secondary;
- `GAP` between different visible groups -> the completed visible prefix remains primary, the future suffix remains pending/secondary, and no sweep runs;
- `GAP` between timing fragments that map to the same visible group -> freeze that group's partial sweep at the last completed fragment boundary rather than marking the whole visible word complete;
- `AFTER_LAST` -> the entire current line is completed/primary and no sweep runs.

Only an unrenderable/low-credibility token-to-text mapping falls back to normal current-line styling. These rules are Phone presentation policy and do not change `LyricsTimingProjection`, provider timestamps, or `:core:timing`.

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
7. if the final visible display group has neither an explicit end nor a following token start, Phone first infers a presentation-only terminal interval from same-line provider cadence: fragmented groups prefer their own positive intra-group timestamp intervals, while a single-token final group may use the median of recent positive visible-group onset intervals when at least two samples exist;
8. if no such local cadence evidence exists, Phone uses the working fork's `650ms` visual fallback measured from that display group's start;
9. any inferred/fallback final-group end is capped at the next timed line start when one exists. These durations exist only for rendering and do not alter semantic timing.

This display-group interval exists only to render one readable lexical unit smoothly. It must not select a different active token, alter `wordBoundary`, rewrite source timestamps, or become a second Timing Semantic Engine.

For the common one-token/one-visible-word case, the display group is that token itself and the shared semantic `wordProgress` is the sweep progress directly.

## Rendering boundary

Phone owns the current production Karaoke renderer. Android Auto Now Playing is intentionally a non-Karaoke consumer of the same shared timing semantics.

```text
shared TimingProjection
        ├─ Phone Karaoke consumer
        │      ↓
        │   Compose continuous sweep
        │
        └─ Android Auto normal consumer
               ↓
           activeLineIndex only
```

Phone 11.4c uses continuous current-line sweep animation.

Android Auto Now Playing does not render active-word emphasis or progress. It still consumes shared timing facts so the current line is not recalculated independently.

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

The active Phone slice fixes feature gating, WORD-only presentation mapping, continuous sweep, working-fork display grouping, and the Phone-only adaptive final-group visual fallback with 650ms retained as the no-evidence default.

Still deferred:

- a dedicated Karaoke Gradle module unless implementation evidence actually justifies one;
- LINE-only synthetic intra-line progress;
- any future Android Auto Karaoke proposal, which requires a new explicit product decision;
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
- for the final open-ended display group only, infer a bounded Phone visual end from same-line provider cadence when defensible, otherwise retain the working fork's 650ms fallback; never convert either into shared semantic progress;
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

### 11.4d — Android Auto Karaoke — not adopted for Now Playing

The current Android Auto Now Playing product contract is deliberately line-oriented.

- LINE_SYNC uses shared `activeLineIndex`.
- WORD_SYNC also uses shared `activeLineIndex`.
- Android Auto ignores shared active-word/progress/boundary facts for visible presentation.
- No `▶` marker, word sweep, word pulse, or synthetic LINE progress is added.
- This is a product/presentation decision, not a limitation of the shared Timing Semantic Engine.

If Android Auto Karaoke is reconsidered later, it requires a new explicitly authorized product/implementation slice. The Phone Karaoke implementation does not create an automatic deferred requirement for Automotive.


## Phone clock invariants

Karaoke is not allowed to select a different clock.

For the same playback timing sample and monotonic instant:

- Karaoke OFF and Karaoke ON must produce the same projected playback position;
- they must produce the same `EffectiveLyricsPosition`;
- they must produce the same shared `LyricsTimingProjection`;
- enabling Karaoke only decides whether the already-computed WORD presentation facts are rendered.

Some Android MediaSession sources omit or publish internally contradictory `lastPositionUpdateTime` values. The platform runtime therefore emits a coherent playback sample with two possible monotonic anchors:

- a valid source `positionUpdatedAtMonotonicMs`, which remains authoritative;
- otherwise the AALyrics-side `positionSampledAtMonotonicMs` captured when `MediaControllerSnapshotAdapter` sampled the controller.

The platform-owned `PlaybackClockReconciler` decides whether the source timestamp is usable. Karaoke does not make that decision and does not create its own fallback anchor.

The shared Phone playback clock:

- applies to LINE_SYNC, WORD_SYNC, PLAIN playback progress, Details progress, and the Playback Surface independently of the Karaoke live toggle;
- survives Activity/Compose recreation because the fallback anchor is attached to the immutable playback snapshot;
- receives only identity/timeline-coherent snapshots from `:platform:media`; a pending different track's timeline is held during the 600 ms metadata-stabilization window rather than being rewritten onto the stable identity;
- may be observed at a higher presentation cadence while Karaoke is active, but cadence must never change clock semantics.

Karaoke OFF and ON therefore consume the same already-reconciled projected playback position. No Karaoke code may compensate for MediaSession drift, metadata stabilization, or cross-track identity/timeline errors.


## Line-wide pseudo-token policy

A provider may advertise WORD/RichSync while a particular line contains only one timing token spanning the entire canonical line. That is insufficient granularity for a word sweep when the line contains multiple readable lexical units.

Phone presentation must therefore reject a single line-wide pseudo-token before display-range grouping and before the 650ms final-group fallback.

The guard is language-independent:

- compare canonical token content with canonical line content;
- use the same lexical segmentation already used by `LyricWordLayout`;
- if one token covers the whole canonical line and the line has multiple readable lexical units, render normal current-line styling;
- do not create Japanese-specific or provider-specific timing exceptions;
- a genuine single lexical unit may still use the Phone-only 650ms final visual fallback.

This presentation guard does not mutate provider data, `LyricsDocument.syncType`, canonical timestamps, or `LyricsTimingProjection`.


### Provider token granularity

Provider timing-token boundaries and readable lexical-word boundaries are independent.

A single provider token may:

- be a fragment inside one visible word;
- match one visible word;
- span multiple visible words or a phrase.

`LyricWordLayout` must therefore map an aligned token spanning multiple lexical ranges to the complete canonical range from its first overlap through its last overlap. Mapping such a token only to the first lexical range causes the skipped remainder to become completed abruptly when the next token starts.

This rule is language-independent and preserves sequential source alignment for repeated text.


## Final open-ended group boundary

A final open-ended display group must not stretch across an unrelated inter-line pause merely because the next timed line is far away.

Phone presentation resolves the visual end in this order:

```text
explicit final word/group end
    -> following token start
    -> explicit line end
    -> same-line provider cadence inference
         fragmented final group:
             infer one terminal interval from its own positive token-start intervals
         single-token final group:
             median of up to four recent positive visible-group onset intervals
             (requires at least two intervals)
    -> 650ms Phone-only no-evidence fallback

then:
    cap inferred/fallback end at next timed line start, if earlier
```

This makes the next timed line an upper boundary rather than the default duration source. A timed `♪` line therefore still prevents a sweep from crossing into an interlude, but a distant interlude no longer stretches the preceding final word.

Provider normalization should preserve explicit source end timing before presentation inference is considered:

- Enhanced-LRC normalization preserves a trailing textless timestamp as the preceding visible word's `endMs`;
- Musixmatch RichSync preserves `te` as canonical `TimedLyricLine.endMs`, not as a synthetic final-word end.

When either canonical boundary exists, Phone presentation uses it before adaptive inference.

The shared timing engine remains unchanged. A genuinely open-ended provider word may still have semantic `wordProgress = null`; only the Phone mapper derives a bounded visual duration from same-line provider evidence, with 650ms retained when that evidence is insufficient.
