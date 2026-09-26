# Phone LyricsViewport Lazy Composition

## Branch and baseline

- Branch: `feature/phone-lyrics-lazy-viewport`.
- Base: `main` at `0a02474f26bee3cb462f227a4293b9e9d3c52e3d`.
- This slice is a Phone presentation/performance refactor only. It does not change Lyrics Provider lookup, provider selection, canonical lyrics, Translation execution, shared timing semantics, Android Auto presentation, or persistent state.

## Problem

Android Auto Now Playing can present a resolved lyric almost immediately because it projects only the current line into its subtitle surface. Before this branch, the Phone `LyricsViewport` kept the complete lyrics document in a non-lazy `Column + verticalScroll` tree, so a newly resolved document composed and measured every lyric row before/while it became visible.

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
- [x] Replace eager Phone lyric-row composition with a stable lazy list foundation while preserving static rendering and manual scroll.
- [x] Port timed Follow/Browse, opening/final boundaries, return-to-playback direction/action, and PLAIN auto-scroll to lazy-list geometry.
- [x] Isolate immutable row content from high-frequency current-line/Karaoke updates and verify Translation remeasurement behavior.
- [x] Add/update focused unit/Compose/Preview coverage for lazy composition, variable-height rows, Translation, Karaoke, Follow/Browse, seeks, and document boundaries.
- [x] Run final validation, inspect branch-wide regression/scope alignment, update this task with evidence, then open a Draft PR and STOP per `AGENTS.md`.

## Checkpoint 1 record

- Replaced the eager `Column + verticalScroll` row container with `LazyColumn + LazyListState`.
- Kept the complete lyrics data in presentation state; only row composition/measurement is lazy.
- Preserved the timed opening `♪` as a dedicated lazy item and canonical/Translation content as one lyric item.
- Added stable canonical row keys that intentionally ignore Translation-only changes.
- Preserved user-scroll detection so direct scrolling continues to hand ownership to Browse.
- Removed the obsolete absolute-`ScrollState` Follow/PLAIN geometry instead of pretending it remains valid against a lazy list.
- Timed Follow positioning, return-to-playback, PLAIN auto-scroll, and final-boundary settlement are intentionally pending Checkpoint 2.
- No provider, Translation execution, shared timing, Android Auto, persistence, or cache behavior changed.

## Checkpoint 2 record

- Timed LINE/WORD Follow now derives scroll correction from visible lazy-item centers and the shared animated focus coordinate; it no longer needs a complete row-height map.
- The timed virtual index maps directly to lazy item indices (`♪ = 0`, canonical lyric `n = n + 1`), preserving the opening focus model.
- When a timed playback item is not materialized, Follow first brings that item into the lazy window, then refines its measured center toward the established 45% viewport target.
- Start-of-document clamping preserves the opening region; end-of-document clamping plus the measured last-row bottom padding preserves the final-row boundary behavior.
- Browse return direction uses visible item indices for off-screen targets and measured geometry for visible targets.
- Return-to-playback supports both timed and PLAIN targets and restores Follow only after the return action.
- Manual scrolling that settles back inside the accepted playback region re-arms Follow.
- PLAIN auto-scroll now maps continuous playback progress (with the existing lead-in/lead-out policy) into a lazy item + local estimated stride rather than requiring the full document pixel extent.
- No current-row/Karaoke recomposition isolation was attempted here; that remains Checkpoint 3.

## Checkpoint 3 record

- PhoneRuntimeHost now memoizes canonical + optional Translation row presentation independently of the 250 ms / 33 ms playback clock tick.
- `mapPhoneLyricsState` can consume that precomputed row list, so timing/Karaoke projection updates no longer allocate the complete lyric-row presentation on every tick.
- `LyricsViewportRow` no longer receives the complete `LyricsViewportUiState`; each lazy item receives only its immutable row content, sync mode, shared focus object, and an optional Karaoke line for the current row.
- Karaoke sweep/progress changes therefore alter the current row's dynamic parameter while non-current row parameters remain unchanged and eligible for Compose skipping.
- Canonical row identity and cached lazy measurements intentionally ignore Translation-only changes.
- When Translation adds/removes secondary text on a materialized row, `onSizeChanged` updates that row's measurement. Follow re-aligns from the new measured geometry; Browse does not run the Follow effect and remains user-owned.
- Stable lazy item keys remain unchanged across Translation-only updates, allowing LazyColumn to preserve its keyed browse anchor instead of treating translated rows as new items.
- Off-screen Karaoke rows are still not retained solely for animation; when composed again they receive the latest current timing state.
- No Translation execution, Karaoke timing semantics, provider behavior, Android Auto behavior, or persistence changed.

## Checkpoint 4 record

- Added a deterministic 160-row Phone preview fixture to exercise lazy composition with mixed Translation rows.
- Added a long-document Follow preview centered deep in the document, a long-document Browse preview with playback far below the visible window, and an interactive large-seek preview that jumps across distant lazy regions.
- Existing Previews continue to cover Karaoke + Translation, narrow/large-font Translation reflow, opening `♪`, final row, Browse above/below, PLAIN follow, PLAIN no-duration, PLAIN auto-scroll off, and long wrapped canonical rows.
- Added direct geometry tests for opening padding, final-row boundary padding, measured variable-height interpolation, fractional focus with an unmaterialized neighbor, visible playback focus tolerance, and invalid PLAIN lazy targets.
- Existing tests continue to cover stable lazy keys across Translation-only changes, off-screen timed direction, PLAIN target progression/return direction, Karaoke timing semantics, Translation matching, stale identity rejection, and reuse of precomputed row lists across timing-only updates.
- This checkpoint adds validation surfaces only; it does not change provider, Translation, timing, Android Auto, persistence, or caching behavior.

## Final validation record

Branch validation is current for the feature branch based on `main @ 0a02474f26bee3cb462f227a4293b9e9d3c52e3d`.

- Baseline alignment: current `main` remains `0a02474f26bee3cb462f227a4293b9e9d3c52e3d`; the feature branch is behind by zero.
- Branch policy and commit-message validation pass in CI.
- Architecture-boundary validation passes in CI.
- The first PR Build exposed an implementation mistake from Checkpoint 1: existing non-eager rendering helpers (`timedFocusIndex`, stable lazy key helpers, return control, focus/scale helpers) had been removed together with obsolete eager-scroll helpers. Commit `239fb7b` restores only the required rendering helpers; obsolete `ScrollState`/absolute eager-scroll geometry remains removed.
- Build workflow #1296 on `239fb7b` passes `assembleDebug`, repository-wide `./gradlew test --stacktrace`, `:ui:phone:testDebugUnitTest`, `:app:testDebugUnitTest`, APK artifact generation, branch naming, commit-message checks, and architecture checks.
- Scope review remains limited to Phone lyrics presentation/runtime mapping, focused tests/Previews, `TASK.md`, and related viewport/Karaoke/Translation/presentation architecture documentation. No provider, Android Auto production, persistence, cache, Gradle dependency, or build-workflow behavior changed.
- Regression audit A (semantic/main comparison): the eager `main` and lazy implementation were compared by behavior for 15% fades, approximately 45% timed focus, opening `♪`, final-row boundary, Follow/Browse ownership, Return-to-playback, Translation reflow, Karaoke current-row rendering, track identity, and seek behavior. The normal timed-focus geometry is mathematically equivalent before normal scroll-boundary clamping.
- Regression audit B (execution/coverage): green debug build + repository unit tests exercise focused lazy geometry, stable Translation-insensitive keys, variable-height rows, opening/final boundaries, off-screen return direction, PLAIN target mapping, stale playback identity, Karaoke timing, Translation matching, and precomputed-row reuse. Debug Previews include a 160-row document, distant Browse, and large seeks.
- PLAIN auto-scroll is the one intentional behavioral approximation: it now estimates item + local stride from playback progress rather than measuring the complete document pixel extent. It remains non-authoritative by product contract and requires device judgment rather than exact old-pixel equivalence.
- Preliminary physical-device observation indicates a noticeable improvement in user-visible lyrics loading speed after the lazy migration. This is recorded as qualitative Phone presentation evidence only; this branch does not demonstrate faster provider/network lookup.
- No Codex review was requested in this slice.

## Result

The branch now implements and validates the intended Phone presentation optimization:

- full lyrics/Translation data remains available while only visible/near-visible rows are composed;
- static canonical + Translation row projection is reused across playback timing ticks;
- current-row Karaoke remains high-frequency without intentionally rebuilding static rows;
- established Follow/Browse and timed visual geometry are preserved;
- PR #86 Build workflow #1296 is green after the rendering-helper restoration fix;
- preliminary device use suggests the user-visible loading delay is substantially reduced.

The last point is intentionally not generalized into a provider optimization claim. Quantifying the remaining latency requires separate instrumentation of provider lookup (`LOOKUP_START -> LYRICS_READY`) and Phone presentation (`LYRICS_READY -> first presented`).

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
