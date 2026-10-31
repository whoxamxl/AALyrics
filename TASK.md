# Manual global lyrics sync

## Scope

- Branch: `feature/lyrics-sync-offset`; implementation baseline: `e85c21c`.
- Implement Phone Sync with ±50/500 ms, exact signed millisecond entry, three-tap calibration, live preview, and one persisted global offset shared by Phone and Android Auto; align architecture and Phone UI documentation with that behavior.
- Record the user's physical-phone testing separately from automated and emulator evidence.
- Restore tracked `AGENTS.md` to the upstream version; retain workstation resource limits and previous personal handoff details in ignored local files.
- Preserve canonical timestamps, actual playback progress, provider/Translation ownership, user-owned Gradle/SDK files, and upstream contribution policy. The final preparation checkpoint changes documentation only.

## Checkpoints

- [x] Persist the global offset, connect existing timing consumers, and extend the explicit Reset path with focused regression coverage.
- [x] Implement fixed-step and exact entry, three-tap calibration, live preview, responsive controls, and Compose fixtures.
- [x] Preserve local workstation guidance and prior handoff details without adding them to the contribution diff.
- [x] Align Sync status, ownership, UI structure, and deferred scope across the remaining documents.
- [x] Validate the documentation diff, architecture guardrails, branch/commit naming, upstream baseline, and branch scope.

## Implemented behavior and acceptance

- One app-owned offset in −30,000…+30,000 ms survives navigation, track changes, and process restart. Positive advances lyrics; negative delays them.
- Phone lyrics/Karaoke, Sync preview, verbose Details line selection, and Android Auto use the same effective clock once. Actual playback progress and canonical timestamps remain unchanged.
- Manual adjustment and exact entry preserve the established availability and validation rules. Three-tap calibration saves only a complete valid sample set and cancels when its playback/lyrics context becomes invalid.
- Sync Reset and `Reset AALyrics` cancel unfinished calibration and restore zero through the explicit settings path.
- Per-track/provider offsets, separate Phone/Auto compensation, automatic audio calibration, and drift correction remain deferred.

## Validation evidence

- Earlier full-suite validation is recorded in `35775b9:TASK.md`: debug APK assembly and 771 tests across 94 suites passed with no failures, errors, or skips. The same record documents successful exact-entry emulator checks and the user's earlier Phone/Android Auto testing.
- The subsequent compact-panel checkpoint recorded a successful build and 31 focused regressions, with six normal/narrow/large-font/limit/zero/unavailable Compose fixtures. Its API 37 emulator failed before rendering; that attempt is not a passing visual check.
- On 2026-10-30, the user reported testing the current interface on a physical phone without noticing problems. This is user-reported phone validation, not a claim that every font size, orientation, or current Android Auto scenario was exercised. Device/version and individual scenarios were not supplied.
- No production source changed in this documentation follow-up. Successful build evidence was reused; no new Gradle or emulator run was needed for these prose changes.
- Final preparation checks passed: architecture guardrails, `git diff --check`, branch/commit naming, tracked `AGENTS.md` equality with upstream, and exclusion of local guidance/notes. Upstream `main` was rechecked at `e85c21c`; the branch retains that baseline and contains no unrelated production changes.

## Contribution context

- Upstream `CONTRIBUTING.md` currently says external code contributions are not yet accepted, while welcoming technical discussion. The Draft PR presents the completed implementation and its proposed calibration policy for maintainer discussion; it does not imply acceptance of external contributions.
- `.github/workflows/build.yml` listens to `pull_request` and manual dispatch; ordinary feature-branch pushes do not trigger Build. A fork PR can require maintainer approval before Actions runs.
- Delivery: push the completed branch, open a Draft PR for the whole Sync feature, then stop. Do not request automated review, mark ready, or merge without explicit authorization.
