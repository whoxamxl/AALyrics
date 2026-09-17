# Application Composition

## Branch and baseline

- Branch: `feature/application-composition`.
- Base: main `56eb43c` after SyncLRC PR #24 merged.
- Working fork behavioral reference: `whoxamxl/auto-lyrics` main `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`).
- Classification: **REWRITE / REFACTOR** at the application boundary.
- Read `AGENTS.md`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`, `docs/MIGRATION_INVENTORY.md`, `docs/PROVIDER_ARCHITECTURE.md`, and `docs/APPLICATION_COMPOSITION.md` before changing production code.
- User explicitly authorized this composition slice.

## Acceptance criteria

- Keep `:app` as the manual composition root; do not add a DI framework for this slice.
- Add `:app` dependencies on `:provider:selection`, `:provider:lrclib`, `:provider:petitlyrics`, `:provider:musixmatch`, and `:provider:synclrc`.
- Create one process-lifetime application graph/scope and construct the four providers, `CrossProviderCandidateSelector`, `LyricsCoordinator`, and `PlaybackLyricsController` through their existing boundaries.
- Build `PetitLyricsConfig` only from the existing BuildConfig values. Missing configuration must remain safe and must not expose secrets.
- Preserve each provider's own default transport/session behavior; do not replace provider-local clients with a new global networking policy.
- Carry `CandidateSelectionPreferences` through playback-to-lookup ownership so a preference change on the same track causes a fresh lookup while timeline/status churn does not.
- Keep the initial production preference at the application boundary and preserve the working fork's karaoke-enabled default as `preferredSyncType = WORD` until settings/persistence are implemented later.
- Expose the shared `LyricsCoordinator.state` for later consumers without building any phone or Android Auto presentation.
- Add deterministic tests for graph/preference wiring and preserve existing playback lifecycle regressions.
- Do not implement live MediaSession discovery/listeners, NotificationListenerService, demand gating, cache, translation, timing/calibration, karaoke rendering, or any UI in this branch.
- Do not change provider search/transport behavior or the production candidate-selection policy.
- Run `./gradlew test check :app:assembleDebug`, `bash scripts/verify-architecture.sh`, and `git diff --check`.
- Open a PR, complete bounded review, and stop before merge for explicit user approval.

## Plan/status

- [x] Merge all four concrete provider migrations through SyncLRC PR #24.
- [x] Create `feature/application-composition` from post-PR #24 main.
- [x] Define application-composition ownership and explicit no-UI scope.
- [x] Inspect current core playback/lookup APIs and the working-fork preference/fan-out call sites.
- [x] Implement the manual application object graph.
- [x] Wire playback selection preferences without introducing settings persistence or UI.
- [x] Add/adjust deterministic tests.
- [x] Update durable architecture/roadmap/migration docs to record Phase 7 completion and composition status.
- [ ] Run full validation and bounded review.
- [ ] Open PR and stop before merge.

## Scope guard

This branch proves that the completed core, selector, and provider adapters can be composed into one production object graph. It is deliberately **not** the MediaSession runtime branch and **not** a presentation/UI branch.
