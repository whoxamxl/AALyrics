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
- For LINE and WORD lyrics, use the measured center of the current lyric block as the normal Follow reference and target that center at approximately 45% of viewport height.
- Treat the instrumental lead-in before the first timed lyric as a virtual opening row rendered as `♪`. The virtual row participates in document geometry exactly like a row before lyric index 0.
- Keep the first real lyric row anchored at the 15% top-fade boundary. Fit the virtual `♪` row plus normal row spacing into the fade region above it, so adding the intro row does not push the lyric document downward. While no timed line is current, `♪` is the focused row.
- When the first timed line becomes current, keep the `♪` glyph in place and move one shared animated focus position from virtual row 0 (`♪`) to virtual row 1 (the first lyric). The note therefore demotes to supporting emphasis while the first lyric gains focus without changing either row's measured geometry.
- Keep scroll at the document origin while the focused row remains above the 45% center target. As successive current rows progress downward and their measured center would pass 45%, begin Follow scrolling so later current rows remain centered near 45%.
- At the document end, place the final lyric row so its **top edge** begins at the 50% viewport boundary.
- All focus/boundary positions are responsive viewport fractions, not fixed dp offsets.

## Edge fading

Lyrics should not clip abruptly at the top and bottom edges.

Use an alpha mask over the rendered lyrics content:

- top approximately 15%: transparent -> fully visible,
- middle approximately 70%: fully visible,
- bottom approximately 15%: fully visible -> transparent.

The fade applies to the lyric content itself rather than painting an opaque surface over it. This allows the effect to remain correct over the AALyrics background and future visual treatments.

Keep the edge mask active at the document boundaries as well. For timed lyrics, the virtual `♪` opening row may sit inside the top fade while the first real lyric begins where the fade reaches full opacity. For PLAIN lyrics, the first lyric row itself begins at that boundary. At the closing boundary, the existing 50% placement keeps the final row clear of the bottom fade.

The top and bottom fades are currently balanced at 15% each. These values may still be tuned slightly in Preview if either edge proves visually too strong or too weak.

## Timed focus motion

LINE and WORD presentation use one continuous animated focus position rather than separate text-emphasis and scroll animations.

- Treat the virtual `♪` row as focus index `0f`; lyric row `n` uses virtual focus index `n + 1f`.
- When playback advances from one timed row to the next, animate this focus index with a damped spring. The current implementation uses stiffness `120`, damping ratio `0.82`, and a small settle threshold.
- For seeks or discontinuities larger than approximately six rows, snap the focus index to the new playback region rather than animating visibly through unrelated lyrics.
- Derive both viewport scroll and row visual emphasis from the same animated focus index. Do not run an independent current-row tween beside an independent scroll tween.
- Interpolate the document focus position between the measured centers of adjacent rows. This keeps motion continuous even when rows have different wrapped heights.
- Keep timed-row text layout stable at 20sp / 30sp / Bold for all timed rows. Express hierarchy as a visual transform around the start-edge center: approximately `0.90x` scale with `0.48` alpha for completed rows and `0.70` alpha for upcoming rows, rising continuously to approximately `1.15x` scale and full opacity at focus.
- Reserve timed-row layout width for the maximum `1.15x` transform (approximately `1 / 1.15` of the available lyric width). This keeps the focused layer inside the viewport's horizontal bounds while preserving the existing start edge and stable wrapping across focus handoff.
- Reserve vertical room for maximum focus scaling without inflating ordinary rows unnecessarily. The normal 16dp inter-row spacing absorbs scale overflow first; only wrapped/tall timed rows whose `1.15x` growth exceeds that spacing receive additional stable measured height. Center the unscaled row inside that reservation so the focused transform cannot overlap adjacent rows.
- The scale/alpha transform itself must not participate in measurement, so a focus handoff does not change wrapping, row height reservation, or surrounding document geometry.
- PLAIN lyrics do not use the timed focus spring or timed focus transforms.

This motion model is adapted from the proven idea in the legacy Auto-Lyrics Performance view—one continuous focus coordinate drives both movement and emphasis—without adopting its fullscreen layout, centered text, focal position, or more aggressive visual scaling.

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
- Timed rows use stable 20sp / 30sp / Bold layout geometry and reserve horizontal plus overflow-safe vertical room for the maximum 1.15x focus transform. Do not change measured font size, line-height, weight, width/height reservation, or neighbor spacing when focus changes.
- The shared animated focus position drives visual scale and opacity continuously between supporting and focused states.
- A completed supporting timed row is rendered at approximately 0.90x scale and 0.48 alpha; an upcoming supporting row uses the same scale with approximately 0.70 alpha; the focused row reaches approximately 1.15x scale and full opacity.
- The current timed lyric row receives the strongest line hierarchy without reflowing the document.
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
- Timed rows use the same stable 20sp / 30sp / Bold measured geometry and maximum-scale width/vertical-overflow reservation as WORD mode.
- The shared animated focus position drives both visual emphasis and viewport movement; there is no separate emphasis tween and scroll tween.
- A completed supporting row is approximately 0.90x / 0.48 alpha, an upcoming supporting row approximately 0.90x / 0.70 alpha, and the focused row approximately 1.15x / full opacity.
- Previous and upcoming rows remain readable while the focused row has clearly stronger contrast.
- Interpolate between measured row centers and keep the animated focus center near 45% once scrolling is available.
- Snap only for large discontinuities such as seeks beyond approximately six rows; ordinary row changes use the spring transition.

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
- instrumental lead-in with the focused virtual `♪` opening row,
- handoff from focused `♪` to the first timed lyric while the note remains as a supporting row,
- first current lyric while the document is still at origin,
- middle current row centered near 45%,
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

- exact supporting scale/opacity values,
- exact spring stiffness/damping tuning,
- exact center-focus tuning for unusually tall wrapped current blocks,
- final low-opacity circle/border values for the return control,
- exact top/bottom fade percentages if the current 15% / 15% targets need slight visual adjustment,
- whether long instrumental gaps should dim the previous timed current row after its explicit `endMs`,
- exact PLAIN lead-in/lead-out weighting.

These are visual/behavioral tuning parameters, not reasons to change the ownership model above.
