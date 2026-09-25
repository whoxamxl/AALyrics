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
- expose presentation-ready range/progress only while effective Karaoke is active;
- if mapping is not credible, fall back to normal current-line presentation;
- if the final visible display group has no explicit end and no following token start, use the working fork's `650ms` duration as a Phone-only visual sweep fallback; do not expose that duration as semantic `wordProgress` or mutate source timestamps;
- keep Translation text outside word sweep.

## 11.4c — Phone rendering

Use the existing LyricsViewport.

For the current WORD_SYNC line:

- completed text before the active token is primary;
- active token uses left-to-right continuous progress;
- pending text after the active token is secondary;
- current line focus/scale remains the existing viewport behaviour;
- Translation row remains unchanged;
- no Performance-mode pulse/fullscreen renderer is introduced.

When there is no safely mappable active token, render normal current-line styling. For the final open-ended visible display group only, a missing semantic duration may use the documented 650ms Phone visual fallback.

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
4. [ ] mapping: expose WORD presentation facts and conservative token ranges only when effective Karaoke is active.
5. [ ] rendering: implement current-line continuous sweep with normal-style fallback.
6. [ ] tests/previews: cover gate/mode/WORD matrix and current behaviour when disabled.
7. [ ] final validation: architecture, unit tests, debug APK, regression/scope audit, docs alignment.
8. [ ] open a new Draft PR and stop.

## Acceptance criteria

- Experimental gate OFF hides/disables Karaoke everywhere and clears live mode.
- Experimental gate ON exposes Quick-controls Karaoke.
- Quick toggle defaults OFF.
- Karaoke renders only for WORD_SYNC when both toggles are ON.
- LINE_SYNC and PLAIN remain unchanged even when toggles are ON.
- Current line selection still comes only from `:core:timing`.
- Phone uses continuous sweep, not Performance-mode pulse.
- fragmented timing tokens mapping to one visible word produce one continuous visible-word sweep, not repeated resets;
- working-fork layout/grouping behavior is preserved/refactored where compatible rather than reimplemented without evidence;
- the working fork's 650ms final-group fallback is preserved only as Phone visual policy and never becomes shared timing truth;
- Canonical line text and Translation remain intact.
- Reset AALyrics restores both Karaoke settings to OFF.
- Android Auto production code is unchanged.
