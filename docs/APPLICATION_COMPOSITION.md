# Application Composition

## Status

- Merged in PR #25.
- Main baseline after merge: `3ea97ce`.
- Classification: **REWRITE / REFACTOR** at the application boundary. Reuse the completed core/provider contracts; do not recreate the old `MediaTracker` monolith.
- The production graph is now established and is the input boundary for later live-media/runtime and presentation work.

## Purpose

The provider and core layers are composed into the first real application object graph. This slice connected those existing parts without starting presentation work or live Android media-session tracking.

The production object graph is:

```text
:app composition root
        |
        +--> LrcLibProvider
        +--> PetitLyricsProvider
        +--> MusixmatchProvider
        +--> SyncLrcProvider
        |
        +--> CrossProviderCandidateSelector
        |
        v
LyricsCoordinator
        |
        v
PlaybackLyricsController
        |
        v
LyricsState
```

No UI currently consumes that state.

## Composition behavior

- `:app` is the composition root and depends on all four concrete provider modules plus `:provider:selection`.
- Composition is explicit/manual; no DI framework was added.
- One process-lifetime application graph and application-owned coroutine scope are used for the coordinator.
- LRCLIB, PetitLyrics, Musixmatch, and SyncLRC are instantiated behind the existing `LyricsProvider` contract.
- `CrossProviderCandidateSelector` is injected behind the existing `CandidateSelector` port.
- `LyricsCoordinator` is constructed from providers, selector, and application scope.
- `PlaybackLyricsController` depends on `LyricsLookupLifecycle`, not concrete providers.
- The shared coordinator `LyricsState` / `StateFlow` is exposed for later runtime and presentation work.

## PetitLyrics configuration

The existing `BuildConfig` injection remains authoritative:

- `PETITLYRICS_USER_ID`
- `PETITLYRICS_APP_NAME`
- `PETITLYRICS_PKG_NAME`
- `PETITLYRICS_CLIENT_APP_ID`

`PetitLyricsConfig` is built from those values. Secrets remain outside source/logs/tests, and an unconfigured PetitLyrics provider remains a safe no-result source rather than preventing graph construction.

## Playback preference wiring

SyncLRC intentionally performs no request unless `LyricsRequest.preferredSyncType == WORD`, so application composition carries the selection preference through playback-to-lookup ownership.

Current behavior:

- same track identity + same preferences -> do not restart lookup;
- same track identity + changed preferences -> start a fresh lookup;
- track identity change -> start a fresh lookup;
- position/status/rate/duration-only churn -> do not restart lookup;
- no active track -> clear lookup ownership.

The working fork defaults karaoke preference to enabled. Until a later settings/persistence slice exists, the application boundary initializes production selection with `preferredSyncType = WORD`. The default remains outside provider and selector implementations.

## Explicitly not owned by composition

Application composition does **not** own:

- phone UI or `MainActivity` presentation;
- Android Auto presentation;
- live `NotificationListenerService` / `MediaSessionManager` runtime;
- process-wide lyrics-demand gating;
- cache;
- translation;
- artwork/colors;
- timing offset/calibration or karaoke rendering;
- provider scoring/selection policy;
- provider transport/search behavior.

The next planned independent slice is documented in `docs/MEDIA_SESSION_RUNTIME.md`.

## Validation record

PR #25 passed:

```text
./gradlew test check :app:assembleDebug
bash scripts/verify-architecture.sh
git diff --check
```

The first bounded review found the missing `android.permission.INTERNET` declaration for the newly composed network providers; that was fixed before merge. The second bounded review found no major issue.
