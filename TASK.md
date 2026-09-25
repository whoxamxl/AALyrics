# Timing Semantic Engine + Behaviour-Preserving Wiring

## Branch and baseline

- Branch: `feature/effective-timing-foundation`.
- Base: `main` at `ae9ed3f27097388b32537ad4b40147679567efaf` (PR #79 merged).
- Phase 11.3a — Effective Timing Foundation: implemented and validated.
- Phase 11.3b — Existing timed-lyrics integration: implemented and validated.
- Draft PR #80 was intentionally closed before review so the same branch can continue into the shared timing-semantic engine.
- Active implementation is in progress on this branch.
- `docs/KARAOKE_ARCHITECTURE.md` is aligned with the shared-engine consumer model. The documentation gate before implementation is complete.

Authoritative references for the active engine slice are:

- `AGENTS.md`
- this `TASK.md`
- `docs/ARCHITECTURE.md`
- `docs/LYRICS_PIPELINE_ARCHITECTURE.md`
- `docs/TIMING_ARCHITECTURE.md`
- `docs/ROADMAP.md`
- `docs/KARAOKE_ARCHITECTURE.md` for downstream consumer/rendering boundaries
- current branch code/tests

## Goal

Implement one framework-neutral **Timing Semantic Engine** and wire it into the existing Phone production path without changing current UI behaviour.

Stable data flow:

```text
projected playback position
        +
lyrics timing offset
        ↓
EffectiveLyricsPosition
        +
canonical timed lyrics
        ↓
Timing Semantic Engine
        ↓
LyricsTimingProjection
├─ activeLineIndex
├─ activeWordIndex
├─ wordProgress
└─ wordBoundary
        ↓
current Phone consumer
        ↓
activeLineIndex only
        ↓
existing UI behaviour unchanged
```

The engine is shared timing semantics. It must **not** accept Karaoke mode as an input and must not branch on Karaoke ON/OFF.

Normal presentation and future Karaoke presentation consume the same semantic result. The current Phone consumer continues to use only `activeLineIndex`.

## Current behaviour contract

The active slice must preserve all current production behaviour:

- `projectedPlaybackPosition(...)` remains unchanged;
- production `LyricsTimingOffset` remains `ZERO`;
- playback progress continues to use the real projected playback position, not effective lyrics position;
- LINE_SYNC current-line selection remains exactly equivalent to the current `currentTimedLineIndex(...)` behaviour;
- WORD_SYNC remains line-oriented in the current Phone UI;
- current Phone WORD presentation still exposes no active word/sweep/progress;
- PLAIN lyrics behaviour is unchanged;
- Translation presentation and diagnostics are unchanged;
- Sync UI remains a non-functional placeholder;
- no timing persistence is added;
- Android Auto timing/presentation is unchanged;
- canonical provider timestamps are never rewritten.

A new engine may calculate additional WORD facts internally, but those facts must not become visible through the current UI in this slice.

## Engine ownership

Extend the existing pure Kotlin/JVM `:core:timing` capability rather than create a Karaoke-mode-specific engine.

After this slice, `:core:timing` owns:

1. signed `LyricsTimingOffset`;
2. `EffectiveLyricsPosition`;
3. the pure effective-position transform;
4. deterministic line/word timing semantics over canonical lyrics.

For semantic projection, `:core:timing` is authorized to depend on `:core:model`.

It must remain independent of:

- Android/framework types;
- `:app`;
- Phone/automotive UI;
- providers and provider selection;
- Translation;
- persistence;
- MediaSession/runtime ownership;
- coroutines/stateful ticking;
- Karaoke mode/enablement;
- rendering/layout primitives.

Update the architecture guard so `core/timing` permits only its newly justified production dependency on `core:model`.

## Projection contract

Use this concrete semantic shape unless implementation evidence requires a very small naming adjustment:

```kotlin
data class LyricsTimingProjection(
    val activeLineIndex: Int?,
    val activeWordIndex: Int?,
    val wordProgress: Float?,
    val wordBoundary: WordTimingBoundary,
)

enum class WordTimingBoundary {
    UNAVAILABLE,
    BEFORE_FIRST,
    ACTIVE,
    GAP,
    AFTER_LAST,
}

fun projectLyricsTiming(
    document: LyricsDocument,
    position: EffectiveLyricsPosition,
): LyricsTimingProjection
```

Semantics:

- `activeLineIndex` is an index into `LyricsDocument.lines`;
- `activeWordIndex` is an index into the active `TimedLyricLine.words`;
- `activeWordIndex == null` when no word is active;
- `wordProgress` is `0f..1f` only when progress can be derived for the active word;
- `wordProgress == null` when there is no active word or the active word has no defensible duration;
- `wordBoundary` describes word-level timing state for the active timed line;
- no field contains UI styling, animation cadence, text ranges, colors, alpha, scale, or Karaoke enablement.

## LINE semantics

LINE selection must preserve the existing production rule exactly:

```text
active line = latest TimedLyricLine whose startMs <= effectiveLyricsPosition
```

Rules:

- return the original document-line index, including mixed plain/timed documents;
- before the first timed line, return `null`;
- an exact line start activates that line;
- after the final line start, the final timed line remains active;
- line `endMs` does not terminate `activeLineIndex` in this slice because current production behaviour does not use it for current-line selection;
- negative effective position is valid and yields no active line;
- backward seek is naturally deterministic because projection is stateless.

Before production wiring replaces the app-local current-line calculation, tests must prove parity with the existing behaviour.

## WORD semantics

WORD semantics are derived only from the active timed line.

When the active line has no timed words:

```text
activeWordIndex = null
wordProgress    = null
wordBoundary    = UNAVAILABLE
```

For a line with words:

1. choose the latest word whose `startMs <= effectiveLyricsPosition`;
2. if none has started, use `BEFORE_FIRST`;
3. an explicit `endMs` is exclusive: `position >= endMs` means that word is no longer active;
4. if the latest started word has explicitly ended and a later word has not started, use `GAP`;
5. if the final explicitly-ended word has ended, use `AFTER_LAST`;
6. if a word has no explicit end, it remains the latest active word until a later word starts;
7. an ended newer word must not reactivate an older open-ended word;
8. backward seek must select the earlier semantic state directly; no retained state/history is allowed.

These preserve/refactor the mature working-fork `KaraokeTiming` behaviour rather than its Android rendering architecture.

## Word progress

For an active word:

- when `endMs > startMs`, derive progress from that explicit interval;
- when `endMs == null` and the next word starts later, the next word's start may be used as the inferred progress end;
- when no defensible end exists (for example the final open-ended word), keep the word active but return `wordProgress = null`;
- do not introduce an arbitrary fallback duration into the shared semantic engine;
- zero-duration explicit words must not produce division-by-zero or fabricated progress;
- clamp only the returned progress fraction to `0f..1f`; do not mutate timestamps or effective position.

Display-token grouping, lexical range mapping, sweep easing, and final-word visual fallback durations are rendering/consumer concerns and are outside this slice.

## Production wiring

After semantic tests are established:

```text
PhoneLyricsMapper
    projectedPlaybackPosition
            +
    LyricsTimingOffset.ZERO
            ↓
    EffectiveLyricsPosition
            +
    canonical LyricsDocument
            ↓
    projectLyricsTiming(...)
            ↓
    projection.activeLineIndex
            ↓
    existing LyricsViewport current line
```

Requirements:

- use `projection.activeLineIndex` for the existing current-line field;
- keep `playbackProgress` on `projectedPlaybackPositionMs`;
- do not expose `activeWordIndex`, `wordProgress`, or `wordBoundary` to the current Phone UI;
- keep WORD source downgraded to current line-oriented Phone presentation;
- remove the duplicate app-local production current-line algorithm once parity is proven, so timing semantics have one owner;
- do not change `PhoneRuntimeHost` to provide a non-zero offset.

## Working-fork evidence

Working fork: `whoxamxl/auto-lyrics` `v1.13.0`.

Preserve/refactor these semantic behaviours from `lyrics/KaraokeTiming.kt`:

- latest-started word selection;
- explicit end times leave gaps unhighlighted;
- missing end falls back to latest-started word;
- an ended newer word does not reactivate an older open-ended word;
- backward seek deterministically selects earlier words.

Do **not** migrate into this engine:

- `KaraokeSweepSpan`;
- display-range/layout mapping;
- arbitrary final-group visual fallback duration;
- Compose/Canvas/Span behaviour;
- Karaoke enablement state.

## Scope guardrails

Do not implement in this slice:

- Sync controls or SyncScreen behaviour;
- timing persistence or calibration scope;
- non-zero production timing offset;
- provider-specific timing correction;
- drift/rate correction;
- audio/waveform analysis;
- Karaoke mode toggle/enablement;
- Karaoke consumer/presenter;
- Karaoke sweep/rendering;
- word highlighting in Phone UI;
- Android Auto timing/Karaoke wiring;
- text/token layout mapping;
- Translation timing changes.

## Reset AALyrics contract

No persisted state is introduced.

Therefore:

- `Reset AALyrics` remains unchanged;
- reset copy remains unchanged;
- no timing preference is added.

## Implementation checkpoints

Implement in small coherent commits and push each completed checkpoint.

1. [x] **Pure semantic model + LINE projection**
   - add `LyricsTimingProjection` / `WordTimingBoundary`;
   - implement LINE selection in `:core:timing`;
   - allow only `:core:model` as the new production dependency;
   - add parity tests for existing current-line semantics.

2. [x] **WORD selection + boundary semantics**
   - add active-word selection;
   - preserve explicit-end gaps, open-ended fallback, no older-word reactivation, and backward-seek semantics;
   - cover exact start/end and negative/before cases.

3. [ ] **Word progress**
   - explicit-end progress;
   - inferred-next-start progress;
   - null progress when duration is not defensible;
   - zero-duration safety.

4. [ ] **Behaviour-preserving production wiring**
   - route Phone current-line selection through `LyricsTimingProjection.activeLineIndex`;
   - remove duplicate production current-line semantics;
   - keep current WORD/LINE/PLAIN UI behaviour unchanged;
   - keep playback progress unchanged.

5. [ ] **Final validation**
   - architecture guard;
   - `:core:timing` tests;
   - relevant app mapper tests;
   - repository unit tests;
   - debug APK build;
   - branch-wide regression/scope audit;
   - verify no user-visible timing/Karaoke/Sync change;
   - update docs only when implementation evidence requires correction.

Do not open a PR during checkpoints.

After all implementation and final validation are complete, open a new **Draft PR** and stop according to `AGENTS.md`.

## Acceptance criteria

The slice is complete when:

- one shared semantic engine owns current LINE and WORD timing facts;
- the engine consumes canonical lyrics + `EffectiveLyricsPosition`;
- Karaoke mode is not an engine input;
- LINE active-line output is regression-equivalent to current production behaviour;
- WORD active/boundary behaviour matches the documented semantic contract;
- word progress is deterministic and does not invent timing when duration is unknown;
- Phone production uses the engine but still consumes only `activeLineIndex`;
- current Phone LINE/WORD/PLAIN behaviour is unchanged;
- playback progress is unchanged;
- canonical timestamps remain immutable;
- no persistence/Reset/Sync/Karaoke UI scope is added;
- CI/build/tests are green.

## Documentation checkpoint before implementation

Documentation is aligned for implementation:

- [x] Timing Semantic Engine contract;
- [x] behaviour-preserving production wiring contract;
- [x] Karaoke consumer/rendering ownership contract;
- [x] Normal consumer uses line facts only;
- [x] future Karaoke consumer may use additional word/progress/boundary facts;
- [x] Karaoke enablement remains outside the semantic engine.

The branch is now ready to hand to Codex for implementation and validation using these documents as references.

Do not begin implementation in this documentation checkpoint.
