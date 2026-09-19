# Phone Track Card

## Branch and baseline

- Branch: `feature/phone-track-card`.
- Base: `main` at `a6f6270b00c3340d671a8d896b596d74466e0180` after PR #38 merged.
- Classification: **PHONE LYRICS TRACK CARD**.
- Authoritative references: `AGENTS.md`, `docs/UI_ARCHITECTURE.md`, and `docs/PHONE_UI_SPEC.md`.
- The user explicitly authorized continuing from the refined Phone shell into the Track Card as the next small slice.

## Goal

Implement the production `TrackCard` composable and its presentation state, then validate it through focused deterministic Previews.

This slice should establish the visual/API contract for current-track identity inside the Lyrics destination without implementing the Lyrics viewport, route/ViewModel wiring, media-session discovery, artwork loading, provider behavior, or navigation runtime.

## Acceptance criteria

- Add a focused immutable `TrackCardUiState` for title, artist, and compact provider/sync metadata.
- Implement the real production `TrackCard` in `:ui:phone`.
- Keep the card compact enough to preserve Lyrics viewport vertical priority.
- Keep artwork rendering caller-owned so the component does not gain networking/image-loading policy.
- Keep the 64dp artwork slot caller-owned and use a neutral placeholder when no artwork content is supplied; do not substitute the AALyrics brand mark for album artwork.
- Support synchronized marquee behavior for overflowing title/artist text: 4s pause at the start, constant-speed scroll, 32dp repeat spacing, then repeat from the start; keep short text static and handle absent optional metadata cleanly.
- Add focused Previews for normal artwork, long title, long artist, long title + artist, no metadata, no artist, and artwork-placeholder states.
- Keep narrow-width checks isolated in the responsive Preview file rather than mixing them into the primary Track Card Preview set.
- Do not implement `LyricsViewport`, `LyricsScreen`, ViewModels, media/runtime wiring, provider/network work, or Android Auto changes.
- Keep commits small and single-purpose.
- Run CI/repository validation, review the complete diff, open a PR, and stop before merge for explicit approval.

## Planned commits

- [x] Prepare Track Card branch and task.
- [x] Add `TrackCardUiState`.
- [x] Implement production `TrackCard`.
- [x] Add focused Track Card Previews.
- [x] Add responsive-only Track Card Preview.
- [x] Align durable Phone UI docs with the stable Track Card/branding contract.
- [x] Review the complete diff and require green final-head CI before merge.
- [x] Open PR and stop before merge.

## Scope guard

This branch implements only the Lyrics destination Track Card contract and visual component. The surrounding Lyrics screen and viewport remain the next slices.
