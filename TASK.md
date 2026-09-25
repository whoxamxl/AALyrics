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

- consume the already-computed `LyricsTimingProjection`;
- preserve canonical line text;
- map timed provider token text conservatively to character ranges in canonical text;
- do not recalculate active word/current line timing;
- do not mutate canonical timestamps;
- expose word range/index/progress only while effective Karaoke is active;
- if mapping fails, fall back to normal current-line presentation;
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

When there is no safely mappable active token or no defensible progress, render normal current-line styling rather than invent timing.

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

1. [ ] docs: align Phone Karaoke activation/mapping/rendering and defer Android Auto.
2. [ ] settings: persist Experimental feature gate + live Karaoke mode; defaults/reset OFF.
3. [ ] playback: expose Quick-controls Karaoke toggle only behind the feature gate.
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
- Canonical line text and Translation remain intact.
- Reset AALyrics restores both Karaoke settings to OFF.
- Android Auto production code is unchanged.
