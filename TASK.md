# Musixmatch Provider Migration

## Branch and baseline

- Branch: `feature/musixmatch-provider-migration`.
- Base: main `839d2bd` after PetitLyrics PR #20 merged.
- Working fork: `whoxamxl/auto-lyrics` main `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`), re-checked on 2026-09-16 before implementation.
- Classification: **PRESERVE / REFACTOR**.
- Read `AGENTS.md`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`, `docs/MIGRATION_INVENTORY.md`, `docs/PROVIDER_ARCHITECTURE.md`, and `docs/providers/MUSIXMATCH.md` before changing production code.
- User explicitly authorized Musixmatch implementation on this branch.

## Acceptance criteria

- Preserve the proven anonymous mobile API flow; do not switch endpoint strategy during migration.
- Implement Musixmatch behind `LyricsProvider` with `LINE` and `WORD` capabilities.
- Preserve token acquisition/refresh, `macro.subtitles.get`, embedded RichSync, dedicated `track.richsync.get` fallback, and subtitle fallback.
- Consume Spotify identity only through normalized `TrackReference`; Spotify is not a lyrics provider.
- Use Spotify ID as strong match evidence when present and reject explicit Spotify-ID conflicts.
- Preserve metadata validation, instrumental rejection, artist-query corroboration, malformed/missing-response handling, and cancellation/failure isolation.
- Normalize RichSync word timing and line subtitles into AALyrics domain models without moving provider logic into core or UI.
- Port working-fork Musixmatch regression coverage and add deterministic transport/cancellation/failure tests where required by AALyrics contracts.
- Do not change global candidate-selection policy, LRCLIB, PetitLyrics, SyncLRC, application wiring, cache, translation, or UI except where a neutral shared helper is genuinely required.
- Run targeted tests plus `./gradlew test check :app:assembleDebug`, architecture checks, and bounded review. Stop before merge for explicit user approval.

## Plan/status

- [x] Create branch from post-PR #20 main.
- [x] Re-check working-fork main baseline.
- [x] Align architecture, roadmap, migration inventory, provider profile, and branch task for Musixmatch.
- [x] Inspect `MusixmatchClient.kt`, all reference tests, and relevant call sites in the working fork.
- [x] Implement/refactor the provider behind AALyrics boundaries.
- [x] Port/add regression coverage.
- [x] Run full validation and bounded review.
- [x] Open PR #21 and stop before merge.

## Validation record

- Targeted Musixmatch, production-selector, and Spotify playback-reference tests passed.
- `./gradlew test check :app:assembleDebug` passed with 174 tests across 18 suites, Android checks, and debug APK assembly after the review fix.
- `bash scripts/verify-architecture.sh` and `git diff --check` passed.
- Normal review found one current-scope failure-contract defect: a failed dedicated RichSync lookup could be hidden by a timestamp-only subtitle. Commit `a60801b` requires usable subtitle text before treating that local fallback as successful when an operational failure is pending.
- Targeted re-review and the added regression passed. No unresolved P0/P1 or current-scope blocking P2 remains; final-head CI remains pending.

## Scope guard

This branch is a Musixmatch provider migration, not a general application-wiring or UI branch. Preserve mature behavior where it is already proven; change ownership only where required by AALyrics architecture.
