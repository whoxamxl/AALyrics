# Media Session Runtime

## Branch and baseline

- Branch: `feature/media-session-runtime`.
- Base: main `3ea97ce` after application-composition PR #25 merged.
- Working fork: `whoxamxl/auto-lyrics` main `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`), re-checked on 2026-09-17 and still current.
- Classification: **PRESERVE / REFACTOR** for mature session-selection behavior and **REWRITE** for integration ownership.
- Read `AGENTS.md`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`, `docs/MIGRATION_INVENTORY.md`, `docs/APPLICATION_COMPOSITION.md`, and `docs/MEDIA_SESSION_RUNTIME.md` before changing production code.
- The user explicitly authorized media-session runtime implementation on this branch.

## Acceptance criteria for the later implementation slice

- Keep Android `MediaSession`/`MediaController`/`NotificationListenerService` concerns inside `:platform:media`.
- Use notification-listener access rather than privileged `MEDIA_CONTENT_CONTROL` for other apps' active sessions.
- Register the notification-listener service correctly and wait for `onListenerConnected()` before using listener-backed active-session APIs.
- Preserve the working fork's mature session-selection semantics: ignore self, retain the current selected session while it is playing, otherwise prefer the first playing session, otherwise fall back to the first active session, and clear when no session remains.
- Track selection by `MediaSession.Token` so active-session list reorder does not become source-switch policy.
- Attach callbacks only to the selected controller; detach old callbacks on ownership changes/disconnect/destruction.
- Normalize selected-controller state exclusively through `MediaControllerSnapshotAdapter` and forward normalized `PlaybackSnapshot` values through a narrow application/platform boundary into the existing `PlaybackLyricsController`.
- Re-evaluate active sessions when the selected session is destroyed.
- Preserve or deliberately reject the working fork's 600 ms metadata stabilization only after regression-focused review; any retained debounce belongs in `:platform:media`, not lyrics core.
- Missing notification access and `SecurityException` paths must fail safely without crashing or leaving stale lookup ownership.
- Add deterministic regression coverage for session selection, ownership, callback attachment, clear behavior, and snapshot handoff.
- Do **not** implement phone UI, Android Auto UI, Compose, demand gating, cache, translation, artwork, transport controls, timing/calibration, karaoke rendering, provider changes, or candidate-selection changes in this branch.
- Run `./gradlew test check :app:assembleDebug`, `bash scripts/verify-architecture.sh`, and `git diff --check` before PR review.
- Open a PR, complete bounded review, and stop before merge for explicit user approval.

## Plan/status

- [x] Merge application composition in PR #25.
- [x] Create `feature/media-session-runtime` from post-PR #25 main (`3ea97ce`).
- [x] Re-check current working-fork `main` and the relevant `MediaListenerService`, `LyricsDemandController`, `MediaTracker`, and manifest behavior.
- [x] Re-check current Android notification-listener / active-media-session API requirements.
- [x] Define runtime ownership, session-selection semantics, callback lifecycle, permission boundary, STOP gate, and explicit no-UI scope in `docs/MEDIA_SESSION_RUNTIME.md`.
- [x] Obtain explicit authorization before production implementation.
- [x] Implement the live media-session runtime behind the documented platform/application boundary.
- [x] Add deterministic regressions and update durable docs with the implementation result.
- [x] Run final validation and bounded review.
- [x] Open PR #29 and stop before merge.

## Validation/review record

- Targeted `SelectedMediaSessionRuntimeTest` passed after the review fix.
- `./gradlew test check :app:assembleDebug` passed against current `main` after conflict reconciliation and again after the review fix.
- `bash scripts/verify-architecture.sh` and `git diff --check` passed.
- GitHub Actions branch-name and build checks passed on the reviewed implementation.
- Codex review round 1 found one current-scope P2: a selected session leaving `PLAYING` did not refresh selection for an already-playing replacement. Commit `7a5e82f` fixes it with a deterministic regression.
- Codex review round 2 targeted the original concern at `7a5e82f` and reported no major issues; the addressed thread is resolved.
- PR #29 is ready for the explicit pre-merge approval gate and has not been merged.

## Scope guard

This branch exists to connect real Android playback to the already-composed lyrics engine. It is **not** a phone UI branch, **not** an Android Auto presentation branch, and **not** the demand-gating/cache/translation/karaoke-rendering branch.
