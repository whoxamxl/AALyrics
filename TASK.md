# Phone Lyrics Viewport

## Branch and baseline

- Branch: `feature/phone-lyrics-viewport`.
- Base: `main` at `88592e0d7846afbf1d9755e48915956ad7c13d6f` after PR #39 merged.
- Classification: **PHONE LYRICS VIEWPORT**.
- Authoritative references: `AGENTS.md`, `docs/UI_ARCHITECTURE.md`, `docs/PHONE_UI_SPEC.md`, and `docs/PHONE_LYRICS_VIEWPORT.md`.
- The user explicitly authorized LyricsViewport as the next Phone UI slice.

## Goal

Implement the production `LyricsViewport` as a responsive, scrollable lyrics-reading surface that works with WORD, LINE, and PLAIN lyrics while keeping runtime/media/provider wiring outside this slice.

The visual direction takes the legacy Auto-Lyrics fullscreen Performance mode as a reference for strong current-line hierarchy, but replaces its fixed three-line presentation with a responsive scrolling viewport.

## Accepted direction

- Use the full available viewport height instead of forcing a fixed six-line window.
- Render a continuous scrollable lyric list; visible line count is determined by device size, wrapping, font metrics, and current content.
- Keep the current playback region above visual center, approximately 40–45% from the top when follow mode owns the scroll.
- Apply top and bottom edge fading over roughly 15% of viewport height so rows fade smoothly in/out rather than clipping abruptly.
- Keep manual scrolling available for synchronized and plain lyrics.
- When manual scrolling moves away from the current playback region, suspend follow behavior and expose a transient action to return to the current line / playback region.
- Keep WORD, LINE, and PLAIN rendering behavior distinct while sharing one viewport geometry and scrolling model.
- Do not force app-brand imagery into lyrics content.

## Sync-mode behavior

### WORD

- Current timed line receives the strongest typography.
- Word-level timing should highlight karaoke progress without changing word geometry or causing layout reflow.
- Prefer color/progress emphasis over the legacy Performance mode's active-word size pop.
- Current-line anchoring follows playback while the user has not manually browsed away.

### LINE

- Current timed line receives strong emphasis.
- Previous/future rows remain readable with reduced emphasis.
- Follow scroll moves to the new timed line smoothly rather than snapping.

### PLAIN

- The lyrics remain fully scrollable manually.
- Optional smooth auto-follow estimates the current playback region from playback position and track duration.
- The estimate should be better than a naive discrete `position / duration * lineCount` jump: use continuous document progress and measured scroll range so movement is smooth across wrapped/variable-height rows.
- Plain auto-follow must later be user-configurable in Settings with an ON/OFF toggle. Keep the Settings row concise (for example, `Plain lyrics auto-scroll  ⓘ  [ON]`) and place the behavioral explanation in an on-demand info tooltip rather than persistent subtext. Settings UI/persistence is not implemented in this slice unless separately authorized.

## Interaction contract

- Follow mode: viewport tracks the playback region automatically.
- Browse mode: user drag/scroll temporarily owns the viewport and playback updates must not fight the gesture.
- Return action: while Browse mode is away from the playback region, show a minimal direction-only control over the viewport; tapping it smoothly restores Follow mode.
- Use Material `ExpandMore` when the playback region is below the visible viewport and `ExpandLess` when it is above.
- Do not show text such as "Current line", "Now", or "Follow playback" in the return control.
- Render the control as a barely visible circular silhouette rather than a prominent rounded pill: approximately 36dp visual circle inside a 48dp accessible touch target, with very low-opacity surface/border treatment and an AccentCyan chevron.
- On appearance, attract attention with only two subtle directional bounces (roughly 4–6dp travel, 350–450ms per cycle), then remain still.
- Do not auto-return merely because a timer expired while the user is reading elsewhere.

## Acceptance criteria

- Add focused immutable viewport presentation state for rows, sync type, current line/word, playback progress where needed, and follow/browse presentation.
- Implement the production `LyricsViewport` in `:ui:phone`.
- Use a responsive `LazyColumn`/scroll model rather than a fixed visible-line count.
- Keep current-line target position responsive to viewport height.
- Support long wrapped lyric rows without corrupting follow positioning.
- Add top/bottom gradient masking/fading over approximately 15% of the available viewport.
- Implement deterministic WORD, LINE, and PLAIN Previews.
- Add short-height, typical-height, tall-height, narrow-width, long-line, first-line, middle-line, last-line, and browsed-away Preview coverage where practical.
- Keep Preview fixtures under `src/debug`.
- Do not implement ViewModels, media-session ownership, provider/network behavior, real Settings persistence/UI, or Android Auto changes.
- Keep commits small and single-purpose.
- Run CI/repository validation, review the complete diff, open a PR, and stop before merge for explicit approval.

## Reference findings

Legacy Auto-Lyrics `PerformanceActivity` used previous/current/next rows with a 36sp bold current line and smaller dim side rows. For plain lyrics it estimated a current line from playback-position fraction. Legacy Phone UI also used a duration-long linear scroll animator for plain lyrics and exposed a jump-to-current action after manual scrolling.

AALyrics should preserve the useful hierarchy and manual-return concept while replacing fixed row count and discrete plain-line estimation with responsive measured scrolling.

## Planned commits

- [x] Prepare LyricsViewport branch and task.
- [x] Capture the approved LyricsViewport behavior contract in durable docs.
- [x] Add viewport presentation model.
- [x] Implement responsive continuous viewport shell and edge fading.
- [x] Add LINE follow behavior and Previews.
- [x] Add WORD visual progress behavior and Previews.
- [x] Add PLAIN estimated auto-follow behavior and Previews.
- [x] Add manual browse / return-to-playback interaction.
- [x] Align durable Phone UI docs with the implemented viewport contract.
- [x] Review the complete diff; require final-head CI before merge.
- [x] Open PR #40 and stop before merge.

## Scope guard

This branch establishes LyricsViewport presentation and interaction behavior only. `LyricsScreen` composition, runtime state mapping, real media-position wiring, Settings implementation/persistence, provider work, and Android Auto remain later slices.
