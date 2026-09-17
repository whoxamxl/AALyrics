# Phone UI Architecture

## Branch and baseline

- Branch: `feature/phone-ui-architecture`.
- Base: `main` at `c0bfb15` after media-session runtime PR #29 merged.
- Classification: **PRESENTATION ARCHITECTURE / DOCUMENTATION**.
- User explicitly authorized documentation, package/folder structure, and architecture work only.
- Read `AGENTS.md`, `docs/ARCHITECTURE.md`, and `docs/UI_ARCHITECTURE.md` before extending phone UI.

## Goal

Define the durable Phone UI information architecture and source ownership before any Compose screen implementation. The slice should make the intended shell, destinations, state boundaries, and package structure obvious so later UI implementation can proceed incrementally without moving responsibilities again.

## Acceptance criteria

- Add a dedicated `docs/PHONE_UI_SPEC.md` describing the approved Phone shell and information architecture.
- Define four primary destinations: `Lyrics`, `Sync`, `Details`, and `Settings`, with `Lyrics` as home.
- Define a persistent compact top status bar for app identity plus runtime status.
- Define a persistent compact playback-controls bar above bottom navigation, exposing Previous / Play-Pause / Next without duplicating track artwork/title metadata.
- Define a persistent four-destination bottom navigation bar.
- Keep the richer Track Card inside the Lyrics destination, with artwork, title, artist, and lyrics/provider/sync metadata.
- Treat the Lyrics viewport as the primary content area and preserve vertical space; exact dp values are intentionally deferred to Preview/implementation work.
- Reshape the `:ui:phone` source tree into clear `shell`, `navigation`, destination, and `state` packages using architecture-only placeholders.
- Keep UI callbacks/platform boundaries explicit: phone UI must not directly own `MediaController`, provider lookup, networking, or candidate selection.
- Keep phone-specific components local first; promote them to `:ui:designsystem` only after their APIs are demonstrated and stable.
- Update `docs/UI_ARCHITECTURE.md` so the shared architecture and Phone-specific spec agree.
- Do **not** implement Compose layouts, navigation runtime, playback commands, ViewModels, media wiring, screen behavior, or final visual dimensions in this slice.
- Run repository CI/build validation, review the complete diff, open a PR, and stop before merge for explicit approval.

## Plan/status

- [x] Create `feature/phone-ui-architecture` from current `main`.
- [x] Define scope and no-implementation guard in `TASK.md`.
- [ ] Add `docs/PHONE_UI_SPEC.md`.
- [ ] Reshape `ui/phone` package/folder ownership using placeholders only.
- [ ] Align `docs/UI_ARCHITECTURE.md` with the Phone package model and shell boundary.
- [ ] Validate CI/build and architecture checks.
- [ ] Review the complete branch diff.
- [ ] Open PR and stop before merge.

## Scope guard

This branch is architecture scaffolding only. Placeholder Kotlin files may document intended ownership, but they must not add production behavior. Actual Compose components, navigation, runtime state mapping, playback transport integration, and visual tuning belong to later implementation slices after this architecture is reviewed and merged.
