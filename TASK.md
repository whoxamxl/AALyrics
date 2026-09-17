# Automotive Design System Boundary

## Branch and baseline

- Branch: `feature/automotive-design-system`.
- Base: `main` at `772519a` after UI foundation PR #27 merged.
- Classification: **REFACTOR / PRESENTATION ARCHITECTURE**.
- User explicitly authorized this automotive design-system boundary slice.
- Read `AGENTS.md`, `docs/ARCHITECTURE.md`, and `docs/UI_ARCHITECTURE.md` before extending automotive UI.

## Goal

Establish a clear boundary between the shared AALyrics Compose design system, Android Auto host-rendered design adapters/components, and concrete automotive screens before implementing the `SectionedItemTemplate` lyrics prototype.

## Acceptance criteria

- Keep `:ui:designsystem` as the shared semantic/Compose visual foundation.
- Add an automotive-local `designsystem/` package for reusable Android Auto presentation adapters and component builders.
- Keep Car App Library screen/template composition under `ui/automotive/screen/`, not inside the design system.
- Move automotive presentation state under `ui/automotive/state/`.
- Represent the intended main surfaces as `NowPlayingScreen` and `ExpandedLyricsScreen` skeletons without inventing final interaction behavior.
- Document that `SectionedItemTemplate`, `Screen.onGetTemplate()`, navigation, invalidation, and scroll/follow orchestration are screen/host-contract concerns rather than design-system primitives.
- Document that reusable automotive representations such as lyrics rows, current-line sections, headers, and semantic color adaptation belong to the automotive-local design-system layer.
- Keep provider access, networking, media-session adaptation, and candidate selection outside `:ui:automotive`.
- Do not implement the final `SectionedItemTemplate` prototype in this slice; prepare the ownership model for that next experiment.
- Run CI/build validation and stop before merge for explicit approval.

## Plan/status

- [x] Create `feature/automotive-design-system` from current `main`.
- [x] Define scope and ownership rules in `TASK.md`.
- [x] Reshape the automotive source tree into `designsystem/`, `screen/`, and `state/`.
- [x] Add placeholder files that make the intended ownership visible without inventing behavior.
- [x] Update `docs/UI_ARCHITECTURE.md` with shared-vs-automotive design-system rules.
- [x] Re-check `docs/ARCHITECTURE.md`; its module-level boundary remains accurate, while the package-level ownership detail belongs in `docs/UI_ARCHITECTURE.md`.
- [ ] Run CI/build validation.
- [ ] Review the complete branch diff.
- [ ] Open PR and stop before merge.

## Scope guard

This branch defines automotive presentation ownership. It does not yet add Car App Library dependencies, `SectionedItemTemplate`, media-template registration, Android Auto service wiring, follow/manual-scroll behavior, or final visual styling.

The next automotive prototype should be able to add those pieces without moving responsibilities again.
