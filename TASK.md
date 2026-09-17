# UI Foundation

## Branch and baseline

- Branch: `feat/ui-foundation`.
- Base: `main` at `3ea97ce` after application-composition PR #25 merged.
- Classification: **REFACTOR / NEW UI FOUNDATION**.
- User explicitly authorized the UI foundation and module cleanup.
- Read `AGENTS.md`, `docs/ARCHITECTURE.md`, and `docs/UI_ARCHITECTURE.md` before extending production UI.

## Acceptance criteria

- Replace the empty `:feature:phone` / `:feature:automotive` presentation modules with `:ui:phone` / `:ui:automotive`.
- Add a shared `:ui:designsystem` Compose module.
- Preserve the established AALyrics dark/cyan/blue visual direction as initial theme tokens.
- Establish typography, spacing, radius, stroke, theme, icon, and reusable-component ownership.
- Put production code under `src/main` and Preview / deterministic development fixtures under `src/debug`.
- Ensure Preview renders production composables rather than introducing a second UI implementation.
- Create enough placeholder source files to make intended ownership/file structure obvious without inventing unapproved screen behavior.
- Keep `:ui:designsystem` independent of core/provider/platform modules.
- Keep phone/automotive presentation on the shared `LyricsState` boundary; UI must not fetch/rank providers or depend directly on `:platform:media`.
- Update architecture guardrails and durable documentation for the new module names/boundaries.
- Do not implement the final phone/automotive screen design in this branch.
- Do not merge without explicit user approval.
- Run `./gradlew test check :app:assembleDebug`, `bash scripts/verify-architecture.sh`, and `git diff --check` through normal validation/CI.

## Plan/status

- [x] Create `feat/ui-foundation` from current `main`.
- [x] Confirm the old feature modules contain no production implementation requiring migration.
- [x] Replace `:feature:phone` / `:feature:automotive` with `:ui:phone` / `:ui:automotive`.
- [x] Add `:ui:designsystem` and Compose build configuration.
- [x] Add AALyrics color, typography, spacing, radius, stroke, and theme foundation.
- [x] Add focused Palette / Typography previews and a combined Design System preview.
- [x] Add phone/automotive production and debug-preview file skeletons.
- [x] Remove obsolete empty `feature/` module files.
- [x] Update architecture guardrails for `ui/*` ownership.
- [x] Add durable UI architecture/source-set/Preview documentation.
- [ ] Run CI/build validation.
- [ ] Review the complete branch diff.
- [ ] Open PR and stop before merge.

## Scope guard

This branch creates the presentation foundation only. It intentionally does not decide the final Lyrics screen layout, Settings UI, navigation model, live media-session wiring, or detailed component APIs beyond the shared foundation already needed for UI work.

Screen implementation should begin from a screen/state/interaction specification and then grow reusable components from demonstrated product needs.
