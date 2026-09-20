# Phone LyricsViewport Specification

## Status

This document defines the implemented interaction and visual contract for the Phone `LyricsViewport`.

The viewport is the primary reading surface of the Lyrics destination. It must remain responsive to available height, width, text wrapping, and system font scale rather than targeting a fixed visible-line count.

Implementation branch: `feature/phone-lyrics-viewport`.

## Product intent

The viewport should feel like one continuous lyrics document that follows playback when appropriate, but immediately yields control when the user browses manually.

The design takes the legacy Auto-Lyrics fullscreen Performance mode as a reference for strong current-line hierarchy. AALyrics does not copy its fixed previous/current/next three-row layout or its active-word size pop. The Phone UI instead uses a responsive scrolling document.

## Responsive layout

The viewport consumes the space made available by the Lyrics destination after surrounding UI such as the Track Card and persistent Phone shell are composed.

Rules:

- Do not force six visible lines, or any other fixed line count.
- Render a continuous vertically scrollable lyrics document.
- Let the actual number of visible rows be determined by viewport height, width, line wrapping, font metrics, and user font scale.
- Long lyrics wrap naturally and remain one logical lyric row even when they occupy multiple visual lines.
- Do not shrink typography merely to fit more lyrics on screen.
- Do not use one fixed center point for every current lyric row. The rendered current block may occupy one, two, three, or more visual lines after wrapping.
- For ordinary timed rows, use the measured bottom edge of the current block as the primary focus reference. Target that bottom edge near 52% of viewport height.
- Keep the current block inside an approximate 28%–60% focus band when its measured height permits. This naturally places a one-line block lower, a two-line block near the previous ~42% visual center, and a three-line block slightly higher without special-casing visual-line counts.
- If a current block is taller than the focus band, center the block around the focus band's center rather than forcing either edge outside the viewport unnecessarily.
- At the document start, place the first lyric row so its **bottom edge** reaches the 50% viewport boundary.
- At the document end, place the final lyric row so its **top edge** begins at the 50% viewport boundary.
- These boundary rules create deliberate opening/closing breathing space while preserving the adaptive focus behavior through the middle of the document.
- All focus/boundary positions are responsive viewport fractions, not fixed dp offsets.

## Edge fading

Lyrics should not clip abruptly at the top and bottom edges.

Use an alpha mask over the rendered lyrics content:

- top approximately 20%: transparent -> fully visible,
- middle approximately 60%: fully visible,
- bottom approximately 20%: fully visible -> transparent.

The fade applies to the lyric content itself rather than painting an opaque surface over it. This allows the effect to remain correct over the AALyrics background and future visual treatments.

Keep the edge mask active at the document boundaries as well. Because the first and final lyric rows are centered vertically, the boundary spacing itself keeps those rows clear of the fade region.

The 20% value is the current approved target and may still be tuned slightly in Preview if it proves visually too strong or too weak.

## Scroll ownership

The viewport has two interaction modes.

### Follow mode

Playback owns the viewport position.

- LINE and WORD lyrics follow the timed current row.
- PLAIN lyrics may follow an estimated playback region when Plain lyrics auto-scroll is enabled and track duration is known.
- Follow movements should be smooth rather than abrupt jumps.

### Browse mode

The user owns the viewport position.

- Any deliberate user drag/scroll away from the playback region enters Browse mode.
- Playback updates must not fight the user's gesture or immediately pull the viewport back.
- Browse mode does not expire on a timer.
- Manual scrolling remains available for WORD, LINE, and PLAIN lyrics.

If the user manually returns to the playback focus region, the return indicator disappears. Follow mode may re-arm after the manual scroll gesture settles and the playback region is again inside the accepted focus zone.

The browse-return threshold is expressed as viewport displacement rather than a lyric-row count. The current implementation shows the return control when the playback target is displaced by roughly 15% of viewport height from the current scroll position.

## Return-to-playback control

When Browse mode moves the viewport away from the playback region, expose a minimal direction-only control.

Do not show persistent text such as `Current line`, `Now`, or `Follow playback`.

### Direction

Use Material icons:

- `ExpandMore` when the current playback region is below the visible viewport,
- `ExpandLess` when the current playback region is above the visible viewport.

The same rule applies to the estimated playback region for PLAIN lyrics.

### Visual treatment

The control should read as a subtle navigation marker, not a prominent button.

Initial geometry:

- accessible hit target: 48dp,
- visible circular silhouette: approximately 36dp,
- chevron icon: approximately 28dp,
- shape: circle,
- no visible rounded-rectangle/pill silhouette.
- Position the control low in the viewport: approximately one third of the previous edge-fade-to-bottom distance, rather than directly above the fade boundary.

Initial semantic treatment:

- circle fill: `BackgroundSurfaceStrong` at very low opacity,
- circle border: 1dp `BorderSoft` at very low opacity,
- chevron: `AccentCyan` with strong but not fully dominant opacity,
- no shadow, or only the minimum necessary if contrast testing proves it is required.

The circle should be more transparent than the earlier prototype and remain close to the threshold of visibility; the slightly larger chevron carries the interaction meaning.

### Attention animation

Only the chevron moves.

On appearance:

1. fade the control in subtly,
2. begin the directional chevron motion almost immediately as the fade becomes visible so the transition feels continuous rather than staged,
3. use approximately 3dp travel,
4. perform one approximately 400ms bounce cycle,
5. then remain completely still.

Do not bounce the circular silhouette itself and do not loop the animation continuously.

The implementation should respect the platform's effective animation/reduced-motion behavior.

### Action

Tapping the control:

1. smoothly scrolls to the current/estimated playback region,
2. restores Follow mode,
3. removes the return indicator once the playback region is back in the focus zone.

Accessibility semantics should describe the action as returning to the current playback position even though no text label is visible.

## Sync-mode rendering

All sync modes share the same responsive viewport, edge fading, manual scrolling, Follow/Browse ownership, and return-control behavior. Their lyric emphasis differs.

### WORD

WORD timing provides karaoke-level progress.

- Align lyrics to the start edge using the existing 20dp horizontal viewport inset.
- The current timed lyric row uses 22sp Bold typography.
- Supporting lyric rows use 18sp Medium typography.
- Transition between supporting and current emphasis over roughly 320ms: interpolate 18sp -> 22sp, Medium -> Bold, supporting -> primary color, and 0dp -> 16dp current-neighbor separation with one shared easing curve.
- Add an extra 16dp of vertical separation between the current row and its immediate supporting neighbors at full current emphasis.
- The current timed lyric row receives the strongest line hierarchy.
- Word progress must not change glyph/word geometry or trigger line reflow.
- Do not reproduce the legacy Performance mode's active-word size pop.
- Prefer color/progress emphasis:
  - completed words: strong/primary treatment,
  - active word: AccentCyan/progress treatment,
  - upcoming words: secondary/dim treatment.
- If timing data supports a stable continuous in-word sweep, it may be used without changing layout geometry.
- The viewport follows the timed line while Follow mode is active.

### LINE

LINE timing follows row boundaries.

- Align lyrics to the start edge using the existing 20dp horizontal viewport inset.
- Current row: 22sp Bold, strongest color treatment.
- Supporting rows: 18sp Medium.
- Add an extra 16dp of vertical separation between the current row and its immediate supporting neighbors at full current emphasis.
- Previous and upcoming rows remain readable with one consistent supporting color; do not add extra distance-based dimming because the viewport edge alpha mask already provides spatial falloff.
- When the timed current row changes, move the viewport smoothly toward the adaptive measured-height focus target.
- Do not snap merely because the current index changed.

### PLAIN

PLAIN lyrics have no authoritative current row.

- Align lyrics to the start edge using the existing 20dp horizontal viewport inset.
- Render all rows at 18sp Medium with the same supporting color; spatial fading comes from the viewport edge mask rather than per-row distance dimming.

The UI must not pretend that an estimated row is exact timing.

Manual scrolling always works. Optional auto-scroll estimates a playback region from playback progress.

The estimate should improve on the legacy discrete `position / duration * lineCount` approach:

- use continuous playback progress,
- map it to the measured scrollable document extent,
- account naturally for wrapped/variable-height rows,
- move continuously rather than jumping row-by-row,
- allow small lead-in/lead-out behavior so scrolling does not begin or finish unnaturally at the exact first/last playback millisecond.

If track duration is unknown or invalid, PLAIN auto-scroll does not run.

## Plain lyrics auto-scroll setting

PLAIN auto-scroll is intended to be user-configurable and defaults to ON.

The Settings row should remain compact:

```text
Plain lyrics auto-scroll    ⓘ    [ON]
```

Do not place a long explanatory subtitle permanently under the row.

Tapping the info control opens an on-demand tooltip/bubble. This follows the interaction pattern already proven by `SettingInfoView` in the user's Auto-Lyrics fork, adapted to Compose and the AALyrics design system.

Draft tooltip copy:

> Estimates where playback is in untimed lyrics and scrolls smoothly to match. Requires track duration.

The future reusable Compose component should support Settings-wide use rather than being specific to this one preference. A working name is `SettingInfoTooltip`.

The foreground row contract is now defined in `docs/PHONE_SETTINGS.md`. Durable preference ownership and runtime wiring remain outside `LyricsViewport`; the viewport continues to consume only the resolved `plainAutoScrollEnabled` presentation value.

## Preview matrix

The production composable should be exercised with deterministic Preview state.

At minimum cover:

- WORD lyrics,
- LINE lyrics,
- PLAIN lyrics,
- typical phone viewport,
- short-height viewport,
- tall viewport,
- narrow width,
- long wrapped lyric row,
- first current row,
- middle current row,
- last current row,
- Browse mode with playback above,
- Browse mode with playback below,
- return control visible in both directions,
- PLAIN estimated playback region,
- PLAIN with auto-scroll unavailable because duration is unknown.

Responsive Preview coverage should validate behavior rather than target a fixed visible-line count.

## Presentation/runtime boundary

The viewport accepts presentation-ready state and emits UI actions. It does not own:

- provider lookup/ranking,
- networking,
- Android media-session objects,
- track/session discovery,
- Settings persistence,
- Android Auto presentation.

Runtime state mapping will later supply playback position, duration, lyrics timing, and settings into the Phone presentation model.

## Deferred tuning

The following details are intentionally not frozen until the first interactive Preview/device pass:

- exact supporting-line spacing,
- exact adaptive focus-band bounds around the measured current block,
- final low-opacity circle/border values for the return control,
- exact fade percentage if the current 20% target needs slight visual adjustment,
- whether long instrumental gaps should dim the previous timed current row after its explicit `endMs`,
- exact PLAIN lead-in/lead-out weighting.

These are visual/behavioral tuning parameters, not reasons to change the ownership model above.
