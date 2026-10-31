# Persistent offline lyrics cache

## Baseline and scope

- Branch `feature/persistent-lyrics-cache`, based on freshly fetched `upstream/main` at `e85c21c`.
- Restore previously resolved lyrics after A → B → A and process recreation without provider requests.
- Reference inspected: `GitUpGitUp/auto-lyrics` main `854c5fa`, `lyrics/LyricsCache.kt` and `media/MediaTracker.kt` cache/read/refresh/prefetch call sites.
- Keep the change suitable for a later upstream contribution; no unrelated personal Sync or connection-diagnostics changes.

## Decisions

- Cache selected canonical lyrics behind a core port and a separate JVM file-storage adapter. Preserve PLAIN/LINE/WORD data, optional end times, language and attribution.
- Cache-first: a valid hit completes a fresh lookup without networking. Save only positive usable results, including results obtained despite another provider failing. Never persist failures or NotFound.
- Identity uses sorted stable catalog references when present; otherwise normalized metadata (title, ordered artists, album, album artist, exact optional duration). Selection preferences and the active provider set also participate. Preserve opaque reference values. Stable IDs tolerate corrected metadata/late duration, as playback ownership already does; metadata-only changes conservatively miss. Version the format/selection policy.
- App-private no-backup storage, atomic replacement, off-main-thread I/O, 256 entries / 32 MiB with least-recently-used eviction and a 1 MiB entry limit. No age expiry, automatic refresh or prefetch, so age alone cannot remove offline availability.
- Reset AALyrics preserves the cache. Eviction, clearing app storage or uninstall removes it. Translation output and calibrated/UI state are never persisted here.

## Checkpoints

- [x] Core cache contract and lookup integration, with lifecycle/failure/preference tests.
- [x] Versioned bounded file adapter and application wiring, with restart/offline/corruption/eviction tests.
- [x] Documentation/privacy/reset alignment, architecture checks, full tests and Debug build; branch-wide regression review.
- [x] Commit and push completed checkpoints; prepare upstream-ready handoff.
- [x] Record successful user phone testing and authorization to open the upstream PR ready for review.

## Validation constraints

- Run Gradle sequentially in a verified 3 GiB systemd scope, zero swap, one worker, no parallel tasks/daemon reuse, 1 GiB Gradle heap and in-process Kotlin.
- No emulator is needed for this storage/lifecycle change. Preserve the user's generated Gradle JVM settings.
- Required regressions: cache hit avoids every provider; selection/provider/recording identity changes miss; fresh lookup IDs; stale/cancelled reads cannot publish or start providers; failed cache I/O does not break lookup; persisted canonical round-trip and graph recreation; demand gating and partial-provider failures remain correct.

## Deferred

- Translation caching, prefetch, network refresh/TTL policy, cache-management UI, provider ranking changes and upstream merge.

## Validation record

- Checkpoint 1: `:core:lyrics:test` passed in 24 seconds in verified 3 GiB / zero-swap scope; architecture and whitespace checks passed.
- Checkpoint 2: file-cache and app tests passed; after stable-reference identity alignment and cancelled-write coverage, `:core:lyrics:test :cache:files:test :app:testDebugUnitTest` passed in 43 seconds (50 core, 13 file-cache, 345 app tests). All runs used the verified memory budget.
- Final `test :app:assembleDebug` passed in 62 seconds at `12b9d23`; 762 tests across 93 suites, zero failures/errors/skips. Existing successful tasks were reused.
- Architecture guardrails and branch-wide whitespace/scope review passed. A final fetch confirmed `upstream/main` remains `e85c21c`; the branch contains only the cache feature and supporting tests/docs. Personal main and the existing Sync/diagnostics branches are unchanged.
- Final build startup verified `MemoryMax=3221225472`, `MemorySwapMax=0`; the scope is now inactive/dead. No Gradle/Kotlin daemon or emulator remains. The generated Gradle JVM file checksum is unchanged (`6c13aa5ba36690ccfbbb25a8807d01d98c15833e446d7ae82ddc726472cb8ef7`).
- Existing Debug Compose previews compiled; no layout changes or new UI controls were introduced. The reset tooltip/dialog now explicitly mentions retained cached lyrics. Agent verification used no emulator or physical device.
- On 2026-10-31, the user reported successful phone testing with no issues and authorized opening the upstream PR. Android Auto physical-device testing was not separately reported.
- APK package/version/signature checked using aapt2/apksigner: `io.github.whoxamxl.aalyrics.debug`, `1.0.0-beta.1-dev+12b9d23`, local Android Debug signer SHA-256 `bbcffafa7296076c0a8c9565798a83c49f4860856e6743d758fa4d5aad1f5703`.
- APK: `app/build/outputs/apk/debug/aalyrics-offline-cache-12b9d23.apk`, 91,948,775 bytes. SHA-256: `e3bcc3035fd7f9116968512121598b885efa4521e163f23e3bf15fd513b997c0`.
- Only documentation status/alignment changes follow the validated build; reuse that APK without another build.

## Phone handoff and upstream preparation

1. Install the Debug APK and fetch song A online once using this version; the pre-cache version did not persist its earlier results.
2. Disconnect the network, switch to B, then return to A. A should restore the original lyrics and timing without a lyrics-provider request.
3. Stop/reopen AALyrics while offline and return to A again. Previously saved, valid entries should still be available.
4. Check the shared behavior on Phone and Android Auto. Translation still depends on separately available local models; Translation output is not cached here.

All implementation checkpoints and final validation are complete on the fork's `feature/persistent-lyrics-cache` branch. A fresh upstream fetch on 2026-10-31 confirmed that `upstream/main` remains `e85c21c`, with no baseline reconciliation needed. After successful phone testing, the user explicitly requested a regular upstream PR ready for review, overriding the default Draft PR workflow for this submission. The PR target is explicitly `whoxamxl/AALyrics:main`, with head `WanHanxi:feature/persistent-lyrics-cache`. Stop after creating the PR; automated review requests and merging require separate authorization.
