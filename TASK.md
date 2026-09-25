# Phone Karaoke Presentation + Continuous Sweep

## Branch and baseline

- Branch: `feature/phone-karaoke-rendering`.
- Base: `main` at `fa17dcbd364718aa1ab475b93b29c8d39581c331` (PR #81 merged).
- Phase 11.4a shared Timing Semantic Engine is merged and is the only owner of line/word/progress/boundary timing semantics.
- Phase 11.4d Android Auto Karaoke is documentation-only and deferred.

## Goal

Implement Phone Karaoke through Phase 11.4b and 11.4c without reopening timing semantics.

```text
LyricsTimingProjection
        +
canonical WORD_SYNC line/token text
        ↓
Phone Karaoke presentation mapping
        ↓
current-line continuous sweep
```

Karaoke activation:

```text
Experimental feature gate ON
        &&
Quick-controls Karaoke ON
        &&
source sync type == WORD
```

LINE_SYNC and PLAIN must never synthesize Karaoke display.

Karaoke enablement is presentation-only. For a given playback sample/time, toggling Karaoke ON/OFF must not change the Phone projected playback position, EffectiveLyricsPosition, active line, or underlying timing projection. A missing MediaSession source timestamp uses the AALyrics-side monotonic time captured when the platform snapshot is sampled. This fallback playback clock is shared by timed Phone presentation regardless of LINE/WORD sync type or Karaoke enablement, and Activity/Compose recreation must never create a new anchor for an old snapshot.

## Product state contract

Persist two application-owned booleans:

1. `karaokeFeatureEnabled`
   - Advanced > Experimental features > Karaoke mode
   - default OFF
   - controls availability of the feature and Quick-controls row

2. `karaokeModeEnabled`
   - Expanded Player > Quick controls > Karaoke
   - default OFF
   - controls live Phone Karaoke presentation

Turning `karaokeFeatureEnabled` OFF must also clear `karaokeModeEnabled`.

Reset AALyrics resets both to OFF.

## 11.4b — Presentation mapping

Use the mature working-fork implementation as migration evidence rather than generating a new layout algorithm without need.

Reference revision:

- repository: `whoxamxl/auto-lyrics`;
- commit: `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f`;
- preserve/refactor: `util/LyricWordLayout.kt`;
- preserve/refactor display grouping: `PhoneKaraokeSweep` in `ui/KaraokeSweepSpan.kt`;
- adapt relevant `LyricWordLayoutTest` and `PhoneKaraokeSweepTest` coverage;
- do **not** migrate `KaraokeTiming.kt` because `:core:timing` is already the timing authority;
- preserve the working fork's final-group `650ms` fallback **only as Phone presentation policy**; it must not enter `:core:timing` or canonical timing data.

Mapping requirements:

- consume the already-computed `LyricsTimingProjection`;
- preserve canonical line text;
- map timed provider token text conservatively to character ranges in canonical text;
- preserve the working fork's proven sequential/case-insensitive/Unicode-normalized alignment and credibility fallback where compatible;
- when consecutive timing tokens map to one visible lexical range, form one display group so the visible word sweeps once instead of restarting for every fragment;
- `activeWordIndex` remains authoritative from `:core:timing`;
- for one-token groups, use shared `wordProgress` directly;
- for multi-token groups, presentation mapping may derive only the visible-group sweep progress from the active semantic token plus the canonical group timing interval;
- do not recalculate current line, choose a different active word, or reinterpret `wordBoundary`;
- do not mutate canonical timestamps;
- expose a presentation-ready Karaoke line state while effective Karaoke is active; a nullable sweep is only the animation sub-state, not Karaoke eligibility itself;
- preserve semantic boundary presentation without fabricating timing: BEFORE_FIRST is fully pending, GAP keeps the completed prefix while the future suffix stays pending, and AFTER_LAST is fully completed;
- if mapping is not credible, fall back to normal current-line presentation;
- if the final visible display group has no explicit end and no following token start, use the working fork's `650ms` duration as a Phone-only visual sweep fallback; do not expose that duration as semantic `wordProgress` or mutate source timestamps;
- keep Translation text outside word sweep.

## 11.4c — Phone rendering

Use the existing LyricsViewport.

For the current WORD_SYNC line:

- completed text before the active token is primary;
- active token uses left-to-right continuous progress;
- pending text after the active token is secondary;
- BEFORE_FIRST renders the whole mappable line as pending/secondary with no sweep;
- an inter-word GAP freezes the completed/pending split instead of falling back to an all-primary normal row;
- a GAP inside one visible multi-token display group freezes that group's partial sweep at the last completed token boundary;
- AFTER_LAST renders the whole mappable line completed/primary with no sweep;
- current line focus/scale remains the existing viewport behaviour;
- Translation row remains unchanged;
- no Performance-mode pulse/fullscreen renderer is introduced.

When the line/token mapping itself is not credible, render normal current-line styling. A valid BEFORE_FIRST, GAP, or AFTER_LAST boundary is still a Karaoke presentation state even though no active sweep is running. For the final open-ended visible display group only, a missing semantic duration may use the documented 650ms Phone visual fallback.

The Compose renderer should preserve the useful continuous-sweep behavior from the working fork but must not transplant `ReplacementSpan` architecture. It receives presentation-ready range/progress and never selects the active word itself.

A smoother presentation cadence may be used while the live Karaoke toggle is enabled; this is presentation cadence, not timing semantics.

## 11.4d — Android Auto

Documentation only. Do not change Android Auto production code.

## Scope guardrails

Do not:

- change `:core:timing` semantic rules;
- add LINE_SYNC synthetic progress;
- add provider refetch behaviour;
- change provider selection;
- alter Translation timing;
- implement Android Auto Karaoke;
- create a speculative `:core:karaoke` module;
- replace LyricsViewport geometry or Follow/Browse ownership.

## Implementation checkpoints

1. [x] docs: align Phone Karaoke activation/mapping/rendering, working-fork reuse policy, and deferred Android Auto.
2. [x] settings: persist Experimental feature gate + live Karaoke mode; defaults/reset OFF.
3. [x] playback: expose Quick-controls Karaoke toggle only behind the feature gate.
4. [x] mapping: expose WORD presentation facts and conservative token ranges only when effective Karaoke is active.
5. [x] rendering: implement current-line continuous sweep with normal-style fallback.
6. [x] tests/previews: cover gate/mode/WORD matrix and current behaviour when disabled.
7. [x] final validation: architecture, unit tests, debug APK, regression/scope audit, docs alignment.
8. [x] open a new Draft PR and stop.

## Acceptance criteria

- Experimental gate OFF hides/disables Karaoke everywhere and clears live mode.
- Experimental gate ON exposes Quick-controls Karaoke.
- Quick toggle defaults OFF.
- Karaoke renders only for WORD_SYNC when both toggles are ON.
- LINE_SYNC and PLAIN remain unchanged even when toggles are ON.
- Current line selection still comes only from `:core:timing`.
- Karaoke ON/OFF does not change the lyrics clock or projected playback position.
- Missing source-timestamp fallback projection uses the stable AALyrics snapshot sample time, is shared by LINE/WORD Phone timing, is Karaoke-toggle-independent, and does not reset when the Phone UI is recreated.
- Phone uses continuous sweep, not Performance-mode pulse.
- Karaoke boundary states do not flash back to normal all-primary current-line styling: BEFORE_FIRST stays pending, GAP preserves only the completed prefix, and AFTER_LAST stays completed.
- fragmented timing tokens mapping to one visible word produce one continuous visible-word sweep, not repeated resets;
- working-fork layout/grouping behavior is preserved/refactored where compatible rather than reimplemented without evidence;
- the working fork's 650ms final-group fallback is preserved only as Phone visual policy and never becomes shared timing truth;
- Canonical line text and Translation remain intact.
- Reset AALyrics restores both Karaoke settings to OFF.
- Android Auto production code is unchanged.

## Playback clock correction

- `PlaybackSnapshot.positionSampledAtMonotonicMs` records the AALyrics-side monotonic time when `MediaControllerSnapshotAdapter` samples the platform snapshot.
- `positionUpdatedAtMonotonicMs` from the source remains authoritative whenever available.
- Phone playback projection falls back to `positionSampledAtMonotonicMs` only when the source timestamp is unavailable.
- The fallback is no longer created by Compose and is no longer WORD-only; LINE and WORD presentation share the same playback clock, while PLAIN playback progress also benefits from the same projection.
- This changes playback-clock anchoring only. `:core:timing` line/word/boundary semantics remain unchanged.
- MediaSession source timestamps are sanity-checked before reaching Phone projection. An old timestamp remains valid by itself; AALyrics rejects it only when the snapshot values are internally contradictory: the same timestamp accompanies a changed raw position, playback status, or playback rate; the timestamp moves backwards on the same track; or it is later than the local sample time.
- A newly selected playing session with both source and local sample timestamps is re-sampled once after 250ms. If the raw position moves while the source timestamp stays unchanged, that source timestamp is quarantined until the source publishes a new timestamp and Phone falls back to the stable local sample clock.
- A missing/null source timestamp does not clear an existing quarantine. Recovery requires a new valid non-null source timestamp or a track/session identity change.

## Validation record

- `scripts/verify-architecture.sh` passed locally and in [Build run 36115022837](https://github.com/whoxamxl/AALyrics/actions/runs/36115022837).
- Build run 36115022837 passed `:app:assembleDebug` and the repository `test` task, including the new Phone Karaoke tests.
- `:app:compileDebugKotlin` and `:ui:phone:compileDebugKotlin` passed locally. The local Windows Gradle test worker could not establish its loopback connection; the Linux CI run executed the tests successfully.
- Branch merge base with `origin/main` is the documented `fa17dcbd364718aa1ab475b93b29c8d39581c331`. The complete diff against that baseline contains Phone settings, Quick controls, mapping, viewport rendering, tests/Previews, and task/documentation alignment. Android Auto production, provider, Translation execution, `:core:timing`, Sync UX/persistence, and Performance Karaoke code are unchanged.
- Reset AALyrics explicitly restores both new persisted Karaoke switches to OFF. Phase 11.4d Android Auto Karaoke remains documentation-only and deferred.


### Line-wide pseudo-token guard

Provider WORD/RichSync capability does not guarantee useful word granularity on every line.

For Phone Karaoke rendering:

- a single timing token that canonically covers an entire lyric line with multiple readable lexical units is treated as line-level timing, not as a renderable Karaoke word;
- such a line falls back to normal current-line styling and must not receive the 650ms final-group visual fallback;
- the rule is language-independent and must not special-case Japanese text;
- a genuine single readable word remains eligible for the final 650ms visual fallback;
- multi-token syllable/fragment grouping such as `Pro / vi / der -> Provider` remains eligible;
- the canonical `LyricsDocument.syncType` is not rewritten by this Phone-only rendering guard.


### Multi-lexical timing tokens

Do not assume one provider timing token equals one lexical word.

If a timing token overlaps multiple readable lexical ranges in the canonical line, its Phone display range is the union from the first overlapping lexical range through the last overlapping lexical range. This preserves the complete visible chunk and prevents skipped text from becoming completed instantly when the next timing token starts.

Repeated chunks must remain aligned sequentially to canonical source order. Existing same-visible-range grouping for fragments such as `Pro / vi / der -> Provider` remains unchanged.


### Final open-ended visual group end resolution

For a final visible Karaoke group whose provider word has no explicit `endMs`, Phone presentation resolves the visual end in this order:

1. explicit final word `endMs`;
2. next word `startMs` when the display group is not the final token group;
3. current line `endMs`;
4. next timed lyric line `startMs`, including a timed music/interlude marker such as `♪`;
5. only when no later canonical timing exists, the Phone-only 650ms visual fallback.

This resolution is presentation-only. It must not mutate canonical `TimedWord.endMs`, line timing, or shared `LyricsTimingProjection`.
