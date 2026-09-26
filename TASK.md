# Phone LyricsViewport Lazy Composition

## Branch and baseline

- Branch: `feature/phone-lyrics-lazy-viewport`.
- Base: `main` at `0a02474f26bee3cb462f227a4293b9e9d3c52e3d`.
- This slice is a Phone presentation/performance refactor only. It does not change Lyrics Provider lookup, provider selection, canonical lyrics, Translation execution, shared timing semantics, Android Auto presentation, or persistent state.

## Problem

Android Auto Now Playing can present a resolved lyric almost immediately because it projects only the current line into its subtitle surface. The Phone `LyricsViewport` currently keeps the complete lyrics document in a non-lazy `Column + verticalScroll` tree, so a newly resolved document composes and measures every lyric row before/while it becomes visible.

The Phone runtime also updates presentation timing every 250 ms during ordinary timed playback and every 33 ms while effective Karaoke is active. Immutable lyric-row content should not be forced through the same high-frequency presentation path as current-line/Karaoke progress.

The goal is to reduce Phone presentation work after `LyricsState.Ready/Degraded` without changing lyrics retrieval behavior or the established viewport UX.

## Approved design

### Full document data, lazy visual composition

- Keep the complete canonical lyrics document and any matching Translation artifact available in memory.
- Lazy loading here means lazy **Compose/layout of lyric rows**, not incremental network fetching and not partial lyrics data.
- Replace the eager scrolling row container with a lazy list model so only visible and nearby prefetched rows are composed/measured.
- Use stable per-document row identity; do not use mutable timing/focus state as item identity.
- Treat the timed opening `♪` as a stable virtual lazy item before lyric index 0.
- Do not require global upfront measurement of every lyric row in order to enter Follow mode.

### Follow / Browse ownership

Preserve the existing product contract:

- deliberate user scrolling enters Browse and playback must not fight the gesture;
- Browse has no timeout;
- manual scrolling can reach any row even when it was never composed before;
- the return indicator points toward the playback region;
- tapping it returns to playback and restores Follow;
- manually returning to the accepted playback region may re-arm Follow after scrolling settles.

The lazy implementation may use `LazyListState` and visible-item geometry instead of a global absolute pixel scroll model. The approximately 45% current-row target remains authoritative for LINE/WORD once scrolling is available.

### Variable row height and Translation

- Canonical text plus optional translated text remain one logical row and one lazy item.
- Translation may change an item's measured height when an exact-identity `TranslationState.Ready` artifact arrives.
- In Follow, remeasure/re-align the current row as needed to keep the approximately 45% target.
- In Browse, Translation updates must not forcibly restore Follow or deliberately jump the user back to playback; stable item identity should preserve the browsing anchor as far as Compose permits.
- Translation remains secondary text only and receives no independent scrolling/timing model.

### Karaoke and high-frequency state

- Karaoke remains current-line-only presentation.
- The 33 ms Karaoke cadence must not require every lyric row to recompose.
- Keep static row presentation (canonical text, translated text, stable typography/layout inputs) separable from dynamic facts (current line, focus, current Karaoke sweep/progress).
- Non-current rows must not consume current-line Karaoke state merely because the viewport state changed.
- If the playback/current row is off-screen during Browse, it does not need to remain composed solely to advance Karaoke visually; timing state remains authoritative and rendering catches up when the row becomes visible again.

### Existing visual geometry

Preserve the current Phone contract unless a device regression forces a separately documented adjustment:

- continuous responsive document rather than a fixed visible-line count;
- 15% top/bottom edge fades;
- timed current-row center near 45% when Follow scrolling is available;
- opening `♪` behavior before the first timed lyric;
- final lyric row top edge at approximately the 50% viewport boundary;
- timed 1.15x focus hierarchy and stable wrapping/overflow reservation;
- manual scrolling for WORD, LINE, and PLAIN;
- existing PLAIN auto-scroll semantics, adapted so they do not depend on eagerly measuring the full document.

## Non-goals

- no Lyrics Provider/network timeout or query changes;
- no lyrics cache or track cache;
- no Translation algorithm/model changes;
- no Karaoke timing-semantic changes;
- no Android Auto changes;
- no Sync calibration work;
- no new persisted settings;
- no visual redesign of lyric typography, focus colors, or return control.

## Implementation checkpoints

- [x] Align durable viewport/Translation/Karaoke/roadmap documentation and establish this task.
- [ ] Replace eager Phone lyric-row composition with a stable lazy list foundation while preserving static rendering and manual scroll.
- [ ] Port timed Follow/Browse, opening/final boundaries, return-to-playback direction/action, and PLAIN auto-scroll to lazy-list geometry.
- [ ] Isolate immutable row content from high-frequency current-line/Karaoke updates and verify Translation remeasurement behavior.
- [ ] Add/update focused unit/Compose/Preview coverage for lazy composition, variable-height rows, Translation, Karaoke, Follow/Browse, seeks, and document boundaries.
- [ ] Run final validation, inspect branch-wide regression/scope alignment, update this task with evidence, then open a Draft PR and STOP per `AGENTS.md`.

## Acceptance criteria

- A resolved long lyrics document is not eagerly composed/measured in full merely to show the Phone viewport.
- Scrolling to previously uncomposed lyrics works naturally in all sync modes.
- LINE/WORD Follow keeps the active row near the established 45% target after the opening region and across ordinary row-height variation.
- The opening `♪`, top fade, final-row boundary, focus scale/alpha hierarchy, and return control retain their current visible behavior.
- User scroll reliably enters Browse; playback does not pull the viewport back; return-to-playback works even when the current row is outside the composed window.
- Translation stays inside the same logical row, supports variable height, and does not create a second scroll/timing owner.
- Karaoke continues to render the current WORD_SYNC line correctly while high-frequency sweep updates are localized to the smallest necessary presentation scope.
- PLAIN manual scroll and optional auto-scroll remain usable without requiring full-document eager measurement.
- Canonical lyrics/timestamps, timing projection semantics, Translation execution, provider selection/retrieval, and Android Auto behavior are unchanged.
