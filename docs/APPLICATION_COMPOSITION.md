# Application Composition

## Status

- Branch: `feature/application-composition`
- Base: main `56eb43c` after SyncLRC PR #24 merged.
- Classification: **REWRITE / REFACTOR** at the application boundary. Reuse the completed core/provider contracts; do not recreate the old `MediaTracker` monolith.
- This slice is explicitly authorized for implementation.

## Purpose

The provider and core layers are complete enough to build the first real application object graph. This slice connects those existing parts without starting presentation work or live Android media-session tracking.

The target object graph is:

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

No UI consumes that state in this slice.

## Required composition behavior

- `:app` is the composition root and may depend on all four concrete provider modules plus `:provider:selection`.
- Prefer explicit/manual construction over adding a DI framework. Hilt/Koin is not required for this object graph.
- Create one process-lifetime application graph and one application-owned coroutine scope for the coordinator.
- Instantiate LRCLIB, PetitLyrics, Musixmatch, and SyncLRC behind the existing `LyricsProvider` contract.
- Instantiate `CrossProviderCandidateSelector` behind the existing `CandidateSelector` port.
- Construct `LyricsCoordinator` from the concrete providers, selector, and application scope.
- Construct `PlaybackLyricsController` from the `LyricsLookupLifecycle` boundary rather than teaching it about concrete providers.
- Expose the shared coordinator `LyricsState`/`StateFlow` to later presentation work without adding phone or automotive rendering now.

## PetitLyrics configuration

The existing `BuildConfig` injection remains authoritative:

- `PETITLYRICS_USER_ID`
- `PETITLYRICS_APP_NAME`
- `PETITLYRICS_PKG_NAME`
- `PETITLYRICS_CLIENT_APP_ID`

Build the existing `PetitLyricsConfig` from those values. Do not move secrets into source, logs, tests, or committed configuration. An unconfigured PetitLyrics provider must remain a safe no-result source rather than preventing graph construction.

## Playback preference wiring

SyncLRC intentionally performs no request unless `LyricsRequest.preferredSyncType == WORD`, so application composition must not discard the selection preference.

Update the pure playback-to-lookup boundary so `CandidateSelectionPreferences` is part of lookup ownership:

- same track identity + same preferences -> do not restart lookup;
- same track identity + changed preferences -> start a fresh lookup;
- track identity change -> start a fresh lookup;
- position/status/rate/duration-only churn -> do not restart lookup;
- no active track -> clear lookup ownership.

Do not put preference persistence, Android settings, SharedPreferences, or UI controls into `PlaybackLyricsController`.

The working fork defaults karaoke preference to enabled. Until a later settings/persistence slice exists, application composition should define the initial production preference as `preferredSyncType = WORD` so real word-timed providers can participate by default. Keep that default at the application boundary rather than hard-coding it into provider or selector behavior.

## Explicitly out of scope

Do **not** implement any of the following in this branch:

- phone UI or changes to `MainActivity` presentation;
- Android Auto UI/service presentation;
- Compose or another UI toolkit migration;
- `NotificationListenerService`;
- `MediaSessionManager` active-session discovery or session selection;
- live `MediaController.Callback` plumbing;
- process-wide lyrics-demand gating;
- cache;
- translation;
- artwork/colors;
- timing offset/calibration or karaoke rendering;
- provider scoring/selection-policy changes;
- provider transport/search behavior changes.

Live Android media-session runtime is the next independent slice after composition is stable.

## Validation

Add deterministic tests for the new composition/preference responsibilities, especially preference changes on the same playback identity. Existing playback invariants must remain covered.

Run:

```text
./gradlew test check :app:assembleDebug
bash scripts/verify-architecture.sh
git diff --check
```

Complete the bounded review in `AGENTS.md`, fix material in-scope findings, and stop before merge for explicit approval.
