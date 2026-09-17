# SyncLRC Provider Migration

## Branch and baseline

- Branch: `feature/synclrc-provider-migration`.
- Base: main `f5a7263` after Musixmatch PR #21 and launcher icon PR #23 merged.
- Working fork: `whoxamxl/auto-lyrics` main `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`), re-checked on 2026-09-16 before implementation.
- Classification: **PRESERVE / REFACTOR**.
- Read `AGENTS.md`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`, `docs/MIGRATION_INVENTORY.md`, `docs/PROVIDER_ARCHITECTURE.md`, and `docs/providers/SYNCLRC.md` before changing production code.
- User explicitly authorized SyncLRC implementation on this branch.

## Acceptance criteria

- Preserve SyncLRC's deliberately narrow karaoke-only role; do not broaden it into a generic plain/line provider.
- Implement SyncLRC behind `LyricsProvider` with descriptor capability `WORD` only.
- Preserve request gating from the working fork: only perform the network lookup when `LyricsRequest.preferredSyncType == WORD`; otherwise return no candidates without calling SyncLRC.
- Preserve the public `GET /lyrics` request shape: nonblank track + artist, `type=karaoke`, optional album, and duration rounded to seconds.
- Preserve compatibility with the current `karaoke` field and the legacy `lyrics` + `type=karaoke` response shape.
- Reject synced/plain-only responses from a karaoke request, instrumental responses, malformed payloads, empty/placeholder lyric content, and karaoke payloads without genuine timed word tokens.
- Reuse the shared `:provider:lrc` Enhanced-LRC parser (`LrcParser.parseKaraoke`) rather than creating provider-local timing syntax.
- Preserve provider metadata when supplied; fall back to requested title/artist/album where the API omits them. Normalize duration seconds to domain milliseconds and report artist-query corroboration when an artist constraint was sent.
- Keep global metadata scoring, source confidence, WORD-vs-LINE preference, and final winner policy in `:provider:selection`; SyncLRC must not introduce a second selector.
- Follow the provider contract: no acceptable karaoke result -> empty list; operational/network/service failure -> exception; coroutine cancellation propagates and cancels underlying HTTP work where practical.
- Port all working-fork SyncLRC regressions and add deterministic AALyrics coverage for request construction/preference gating, transport/service failure, normalization, and cancellation.
- Do not change LRCLIB, PetitLyrics, Musixmatch, global candidate-selection policy, application wiring, cache, translation, UI, or karaoke rendering except for a genuinely neutral shared helper if required.
- Run targeted tests plus `./gradlew test check :app:assembleDebug`, `bash scripts/verify-architecture.sh`, and `git diff --check`. Open a PR, complete bounded review, and stop before merge for explicit user approval.

## Plan/status

- [x] Rebase the existing branch onto current main (`f5a7263`).
- [x] Re-check working-fork main baseline and current public SyncLRC API contract.
- [x] Inspect `SyncLrcClient.kt`, all reference tests, the shared karaoke parser, and relevant `MediaTracker` call sites.
- [x] Align architecture, roadmap, migration inventory, provider profile, README, and branch task for SyncLRC.
- [x] Implement/refactor the provider behind AALyrics boundaries.
- [x] Port/add regression coverage.
- [x] Run final validation after the bounded-review 404 no-match fix.
- [x] Open PR #24 and complete both bounded review rounds.
- [x] Stop before merge after final CI and review-record checks.

## Scope guard

This branch is a SyncLRC karaoke-provider migration, not a general karaoke-rendering, application-wiring, or UI branch. Preserve mature provider behavior and move only the ownership required by AALyrics architecture.
