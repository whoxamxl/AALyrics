# Android Auto Now Playing Contract

## Status

- Active topic branch: `feature/android-auto-now-playing`.
- Original branch baseline: `main` at `52e9249395206f8c3821cc5c8e5893a8ccdcd125` (PR #82 merged); the branch was later reconciled with `main` at `86f4cd300161eb2038c637094a8e75a0afa9bf80`.
- This slice completes the existing legacy Android Auto Now Playing path built on `MediaBrowserServiceCompat` + `MediaSessionCompat`.
- Car App Library templated media remains a separate future slice under `docs/ANDROID_AUTO_MEDIA_STRATEGY.md`.
- Android Auto Karaoke is intentionally out of scope for this product surface. WORD_SYNC is presented line-by-line.
- Production integration and focused tests are implemented on this branch. Physical Android Auto rendering and forced process-death recovery have been validated; the bounded cold-start retry is CI-covered. Re-running the original mid-track cold-start symptom on-device is a non-blocking regression follow-up, not a merge gate. See `TASK.md` for the exact evidence.

This document is the authoritative product/implementation contract for the Android Auto Now Playing completion slice. `TASK.md` records execution checkpoints; capability documents remain authoritative for their underlying ownership rules.

## Product intent

Android Auto Now Playing is a driving-safe synchronized-lyrics surface, not a reduced copy of the Phone player.

The surface should expose:

- the selected track's normal media metadata;
- album artwork;
- accurate playback state and only the transport capabilities actually advertised by the selected source session;
- exactly one current synchronized source lyric line;
- an optional translated second line;
- visible live loading feedback when lyrics or Translation work is actively progressing.

It deliberately does not expose unrelated diagnostics or browse/detail information.

### Playback-source compatibility

Android Auto presentation is fed from the same selected Android MediaSession observed by the phone-side runtime. The automotive surface does not integrate directly with Spotify, YouTube Music, Apple Music, or another music service.

Public player examples therefore inherit the MediaSession compatibility contract in `docs/MEDIA_SESSION_RUNTIME.md`: when a supported media app exposes an eligible active session, AALyrics can follow its normalized track/playback state and present the resulting lyrics on Android Auto.

The automotive host must continue to respect source capabilities. Artwork, metadata, seek state, queue availability, and transport actions are published only when the selected media session/application boundary provides them; the Android Auto layer must not invent controls or metadata to make one player look like another.

## Baseline context

At the original branch baseline:

- `LyricsBrowserService` publishes a `MediaSessionCompat` and transport callback.
- `NowPlayingScreen` publishes title, artist, album, duration, display title, one subtitle, playback position/state/rate, and a fixed action mask.
- `AutomotiveLyricsUiStateMapper` calculates its own projected position and its own current timed line.
- `AutomotiveRuntimeBinding` receives only playback, lyrics, transport, and browser trust.
- application state already owns:
  - `playbackArtworkState`;
  - `playbackControlState`;
  - `translationSettings`;
  - `translationState`;
  - normalized `PlaybackSnapshot`;
  - canonical `LyricsState`.
- `:core:timing` already owns `effectiveLyricsPosition(...)` and `projectLyricsTiming(...)`.

Therefore the missing work is presentation integration, not new provider, Translation-engine, media-discovery, or timing algorithms.

Primary files expected to participate:

- `app/src/main/java/io/github/whoxamxl/aalyrics/AALyricsApplication.kt`
- `ui/automotive/src/main/java/io/github/whoxamxl/aalyrics/ui/automotive/AutomotiveRuntimeHost.kt`
- `ui/automotive/src/main/java/io/github/whoxamxl/aalyrics/ui/automotive/state/AutomotiveLyricsUiState.kt`
- `ui/automotive/src/main/java/io/github/whoxamxl/aalyrics/ui/automotive/screen/NowPlayingScreen.kt`
- `ui/automotive/src/main/java/io/github/whoxamxl/aalyrics/ui/automotive/service/LyricsBrowserService.kt`
- automotive mapping/metadata tests, including the existing `AutomotiveLyricsUiStateMapperTest`.

Codex may choose a smaller internal type split when implementation evidence supports it, but it must preserve the ownership and behavior contracts below.

## Surface scope

### Included

Track/media:

- title;
- compact Automotive artist identity: prefer album artist when available, otherwise track artist;
- preserve the upstream full track artist metadata for non-Automotive consumers and lyrics lookup;
- album;
- duration;
- album artwork.

Playback:

- playing / paused / buffering / stopped / idle state;
- playback rate;
- live position using the same normalized clock authority as current AALyrics timed presentation;
- play;
- pause;
- previous;
- next;
- seek;
- source-advertised capability mapping.

Lyrics:

- LINE_SYNC current line;
- WORD_SYNC current line using only the shared active-line semantic fact;
- lyrics loading/ready/not-found/failed presentation;
- interlude/before-line presentation;
- explicit PLAIN/unsynchronized fallback.

Translation:

- Translation OFF/original-only;
- Translation loading feedback;
- Translation ready as source + translated two-line presentation;
- Translation failed fallback to source-only.

### Explicitly excluded

Do not add these to Now Playing:

- Karaoke sweep, active-word emphasis, word progress, or other Karaoke presentation;
- current-line marker such as `▶`;
- previous or next lyric rows;
- PLAIN lyric pseudo-synchronization;
- Lyrics Provider attribution;
- playback source/app/package information;
- queue presentation;
- Translation Provider attribution;
- Sync/calibration controls or non-zero calibration persistence;
- Browse/Expanded Lyrics screens;
- Car App Library / `CarAppService` / `MediaPlaybackTemplate` / `SectionedItemTemplate`;
- provider selection changes or general/manual refetch behavior changes; the one bounded transient-failure retry defined by the process-recovery contract is the only retry exception in this slice.

Provider/source/queue/detail facts may be useful on later Browse/Details surfaces, but they do not belong in the constrained Now Playing lyric area.

## Timing contract

Android Auto must stop owning a second current-line algorithm.

The target flow is:

```text
normalized PlaybackSnapshot
        ↓
shared projected playback clock
        ↓
effectiveLyricsPosition(projectedPlaybackPosition, LyricsTimingOffset.ZERO)
        ↓
projectLyricsTiming(canonicalLyrics, effectivePosition)
        ↓
LyricsTimingProjection.activeLineIndex
        ↓
Automotive one-line presentation
```

Rules:

- canonical provider timestamps remain unchanged;
- the production lyrics offset remains zero in this slice;
- Android Auto does not implement Sync UX or calibration persistence;
- LINE_SYNC and WORD_SYNC both use `activeLineIndex`;
- WORD_SYNC ignores `activeWordIndex`, `wordProgress`, and `wordBoundary` for automotive presentation;
- PLAIN lyrics do not enter timing projection for visible current-line text;
- Android Auto must not retain an independent `currentTimedLine(positionMs)` selector;
- Android Auto must not invent a separate fallback playback clock;
- source `positionUpdatedAtMonotonicMs` remains authoritative when valid;
- when it is unavailable, the existing AALyrics-side `positionSampledAtMonotonicMs` fallback must be honored rather than anchoring to the time the Automotive UI happens to observe the snapshot;
- track identity and timeline coherence remain a `:platform:media` invariant.

For the same normalized playback sample and monotonic instant, Phone normal timed presentation and Android Auto agree on the active line.

## Lyrics presentation contract

The lyrics area is owned first by Lyrics state. Translation may add only a secondary line after usable synchronized lyrics exist.

### No media

```text
Play a song to see lyrics
```

### Lyrics waiting

Use the existing non-animated waiting state where appropriate:

```text
Waiting for lyrics…
```

### Lyrics loading

Cycle at the Automotive presentation cadence:

```text
Loading lyrics.
Loading lyrics..
Loading lyrics...
Loading lyrics.
```

This is a live-state heartbeat so the user can distinguish active loading from a frozen surface.

### Lyrics not found

```text
No synced lyrics found
```

### Lyrics failed

```text
Unable to load lyrics
```

### PLAIN-only lyrics

PLAIN lyrics are not synchronized and must not be proportionally or heuristically mapped to playback position.

```text
Synced lyrics unavailable
```

### Timed lyrics before a current vocal line / interlude

```text
♪
```

### LINE_SYNC ready

Show only the canonical current line.

```text
忘れないで
```

### WORD_SYNC ready

Show only the canonical current line using the same shared `activeLineIndex` semantics.

Do not show word progress or Karaoke styling.

```text
忘れないで
```

No `▶` marker is used. The displayed lyric is already the current line, and the marker would consume scarce horizontal space.

## Translation presentation contract

Translation remains an optional derived capability. Translation failure must never turn valid source lyrics into a Lyrics failure.

Translation state is relevant only after the Lyrics state has yielded usable synchronized canonical lyrics and an active canonical line. If Lyrics itself is loading/not-found/failed/plain-only, the Lyrics presentation above remains authoritative.

The automotive two-line contract is fixed:

### Translation disabled / idle / not required

Source only:

```text
忘れないで
```

### Translation translating

Keep the usable source line and use the second line as a live heartbeat:

```text
忘れないで
Translating.
```

then:

```text
忘れないで
Translating..
```

then:

```text
忘れないで
Translating...
```

No partial Translation Artifact is exposed. `Translating...` is status text only.

### Translation ready

Only an enabled/current-target `TranslationState.Ready` artifact whose canonical identity matches the currently displayed canonical lyrics may contribute translated text.

When the current artifact line has `translated == true` and nonblank translated text:

```text
忘れないで
Don't forget me
```

The canonical/source line remains first. The translated line remains second.

When the current artifact line is preserved/not translated or its translated text is blank, show source only.

### Translation failed

Fall back to source only:

```text
忘れないで
```

Do not leave a persistent `Translation unavailable` or failure message in the second line.

### Host layout policy

AALyrics does not dynamically switch layout based on string length.

The automotive presentation remains source line + optional second line. Real-device behavior already shows that overlong text is ellipsized by the host without changing the surrounding layout. That truncation is an accepted tradeoff.

Do not add application-side manual truncation unless device evidence later requires it.

## Loading heartbeat

The existing Automotive projection cadence is 250 ms. Keep that cadence for this slice.

Animated frames are presentation-only:

```text
frame = floor(monotonicTimeMs / 250) % 3

0 -> "."
1 -> ".."
2 -> "..."
```

Exact implementation shape may differ, but the behavior must remain deterministic and testable from an injected/current monotonic time rather than mutable domain state.

Rules:

- do not create domain states such as `LOADING_1/2/3`;
- do not persist the frame;
- do not restart Lyrics/Translation work to animate it;
- Lyrics loading and Translation loading use the same three-frame cadence;
- the heartbeat must continue when playback is paused;
- therefore the service tick condition must not be only `latestPlayback.isPlaying`;
- render on a 250 ms tick while playback position needs projection **or** an animated loading presentation is active;
- event-driven changes such as track, Lyrics state, Translation state, artwork, or capability updates should render immediately rather than waiting for the next heartbeat;
- the host may coalesce/throttle metadata updates; AALyrics guarantees publication cadence, not a guaranteed physical redraw every 250 ms.

## Artwork contract

Reuse the existing selected-session artwork boundary.

Current platform priority is already:

1. `METADATA_KEY_ALBUM_ART`;
2. `METADATA_KEY_ART`;
3. media description `iconBitmap`.

Do not re-query Android `MediaController` from `:ui:automotive` and do not create a second artwork discovery pipeline.

Wire the existing application-owned artwork state into the automotive runtime and publish it through the AALyrics `MediaSessionCompat` metadata using the appropriate artwork bitmap metadata key(s).

Requirements:

- current artwork arrival updates metadata immediately;
- null/no artwork must not leave a previous track's jacket indefinitely visible;
- track transitions must not intentionally bind known stale artwork to the new track;
- artwork updates must participate in metadata invalidation even when title/subtitle are unchanged;
- no queue artwork work is required in this slice.

If current artwork state lacks enough identity information to prove cross-track safety, prefer the smallest upstream-compatible fix or conservative clearing behavior over an automotive-only stale-art cache.

## Playback capability contract

Reuse `PlaybackControlState.capabilities`; do not rediscover source actions in `:ui:automotive`.

Map the source-advertised facts into `PlaybackStateCompat.actions` conservatively:

- `canPlay` -> `ACTION_PLAY`;
- `canPause` -> `ACTION_PAUSE`;
- `canSkipPrevious` -> `ACTION_SKIP_TO_PREVIOUS`;
- `canSkipNext` -> `ACTION_SKIP_TO_NEXT`;
- `canSeek` -> `ACTION_SEEK_TO`;
- advertise `ACTION_PLAY_PAUSE` only when the available capability facts support both sides of the toggle.

Do not advertise queue actions in this slice.

Existing transport callbacks remain the command boundary. Unsupported capabilities should not be advertised merely because AALyrics has a callback method.

## Process and service lifecycle contract

Android Auto lyrics demand must not depend on the Phone Activity remaining foreground.

The production demand contract is:

```text
Phone process foreground
        OR
CarConnection projection connected
        OR
LyricsBrowserService active for the Android Auto host
        ↓
lyrics presentation demand
```

Requirements:

- `LyricsBrowserService.onCreate()` marks Automotive host demand active through the application-owned demand gate;
- `LyricsBrowserService.onDestroy()` removes only that host-service demand source;
- losing Phone foreground or one Automotive signal must not suspend provider work while another demand source remains active;
- Automotive host activation re-requests the system Notification Listener binding so process recreation can recover active MediaSession observation without requiring the Phone Activity to reopen;
- `MediaSessionListenerService.onListenerDisconnected()` requests a system rebind after publishing the disconnected/error transition;
- recovery must rebuild state from current MediaSession observation and canonical provider work rather than persisting transient playback/lyrics state;
- do not use an unconditional started service, foreground-service keepalive, or boot receiver merely to prevent process death in this slice.

The recovery goal is **correct reconstruction after process death**, not making the application process immortal.

### Cold-start lyrics lookup recovery

Process reconstruction can recover the current MediaSession while the first provider fan-out is still subject to transient network/provider initialization failure. That failure must not pin the current playback identity in `LyricsState.Failed` until the user changes tracks.

The bounded recovery rule is:

```text
attempt 1
    ├─ usable candidate -> complete normally
    ├─ all providers complete successfully with no usable candidate -> NotFound
    └─ no usable candidate + one or more provider failures
            ↓ 500 ms
         attempt 2
            ↓
         complete normally or terminal Failed
```

Rules:

- retry only when no usable candidate exists and at least one provider attempt failed;
- retry exactly once;
- do not retry a clean `NotFound`;
- do not use MediaSession position/metadata churn as the retry trigger;
- a new playback identity still supersedes/cancels the old lookup normally;
- provider-specific failure identifiers and exception types stay out of canonical `LyricsState`;
- current lookup attempt count and provider failure types may be exposed through Verbose Details diagnostics for validation;
- no infinite retry, periodic retry, or persistent background polling is introduced.

## Automotive state boundary

Do not continue growing one ambiguous `subtitle: String` as the entire presentation model.

The implementation should expose the smallest surface-local state that makes the contract explicit, conceptually:

```text
Automotive Now Playing state
├─ track metadata
├─ artwork
├─ playback facts
├─ playback capabilities
└─ lyric presentation
   ├─ primaryText
   ├─ secondaryText?
   └─ presentation/lifecycle facts needed for deterministic mapping
```

This is a conceptual requirement, not a mandated class name.

Do not create one universal Phone/Automotive UI model. Do not put Android framework media types into framework-neutral shared capability state.

## Metadata/update policy

Preserve the useful split between playback-state updates and metadata updates:

- playback position/state may be republished at the 250 ms projection cadence;
- metadata should be republished when visible metadata changes;
- loading heartbeat frames intentionally count as visible metadata changes;
- artwork changes intentionally count as metadata changes;
- Translation ready/loading transitions intentionally count as metadata changes;
- position-only movement inside the same lyric line must not force metadata rebuilds.

The implementation may replace the current `MetadataSignature` with a more suitable invalidation key, but it must include every visible fact that can change independently.

## Identity and stale-result rules

Automotive presentation must not weaken existing ownership guarantees.

- Lyrics must belong to the current track before they are shown.
- Translation must belong to the exact current canonical lyrics identity and selected target before it is shown.
- Translation failure/loading must not erase usable canonical source lyrics.
- Artwork must not deliberately survive a known track-identity change as though it belonged to the new track.
- The automotive layer must not repair cross-track PlaybackSnapshots; `:platform:media` owns snapshot atomicity.
- Provider lookup, Translation execution, and model download lifecycle remain upstream.

## Implementation checkpoints

Implement in this order unless concrete compile-time evidence requires a very small adjustment:

1. Define/extend Automotive Now Playing presentation state and runtime inputs without changing visible behavior unnecessarily.
2. Replace automotive-local current-line/clock ownership with the existing shared playback/timing authority; use only `activeLineIndex`.
3. Implement Lyrics state mapping, PLAIN rejection, and deterministic 250 ms loading heartbeat.
4. Wire selected-session artwork and real playback capabilities.
5. Wire Translation settings/state with exact identity gating, two-line ready presentation, translating heartbeat, and failed source-only fallback.
6. Align MediaSession metadata/update invalidation with artwork, lyrics, Translation, and heartbeat changes.
7. Add focused mapping/metadata/tick tests and regression coverage.
8. Run final validation, align docs/TASK to actual behavior, inspect branch-wide scope, open a Draft PR, and stop per `AGENTS.md`.

Keep commits small and checkpoint-oriented.

## Required test matrix

At minimum cover:

### Lyrics

- no media;
- waiting/idle;
- loading frames `.` / `..` / `...`;
- loading heartbeat while paused;
- not found;
- failed;
- PLAIN-only;
- LINE_SYNC before first line;
- LINE_SYNC current line;
- WORD_SYNC current line is line-only;
- interlude/current line fallback;
- backward/forward line boundary movement using shared timing;
- track change cannot display stale previous-track lyrics.

### Translation

- disabled -> source only;
- translating -> source + animated `Translating.`/ `..`/ `...`;
- ready exact identity -> source + translated text;
- ready preserved/nontranslated line -> source only;
- ready stale canonical identity -> source only;
- failed -> source only;
- not required -> source only;
- Lyrics failure/loading takes precedence over Translation status.

### Artwork

- artwork present;
- delayed artwork arrival;
- artwork becomes null;
- track change without immediate new artwork does not intentionally preserve stale jacket;
- artwork-only change causes metadata publication.

### Controls/playback

- each capability bit is advertised only when supported;
- unsupported previous/next/seek is omitted;
- playback status mapping remains correct;
- playing position advances from the normalized shared clock;
- paused position remains stable;
- missing source timestamp uses the AALyrics sample-time fallback rather than a new UI observation anchor.

### Update behavior

- position-only movement inside one line does not rebuild metadata;
- line change rebuilds metadata;
- loading frame change rebuilds metadata;
- Translation state/result change rebuilds metadata;
- artwork change rebuilds metadata.

### Lifecycle/recovery

- Phone foreground demand can disappear while Automotive host demand keeps lyrics work active;
- CarConnection demand can disappear temporarily while an active `LyricsBrowserService` still keeps lyrics work active;
- final removal of all Phone/Automotive demand suspends provider-owning work;
- Notification Listener disconnection must request rebind;
- process recreation via the Automotive browser service must not require reopening the Phone Activity before MediaSession observation can recover;
- a first-attempt provider failure can recover on one bounded retry without requiring a track change;
- persistent provider failure stops after the second attempt;
- clean NotFound remains single-attempt behavior.

## Validation

Required validation for this slice is:

- relevant `:core:timing`, Translation, platform/media, application, and `:ui:automotive` unit tests;
- repository architecture checks;
- debug APK build;
- branch-wide scope-drift inspection;
- DHU where useful/available;
- physical Android Auto validation for host-controlled rendering behavior;
- confirmation that Phone Lyrics/Translation/Karaoke behavior is unchanged;
- documentation/TASK alignment.

Current result:

- latest CI run `36220867319` passes architecture checks, debug APK build, all unit tests, and sideloadable APK upload;
- physical Android Auto rendering has been observed on-device;
- forced process-death recovery has been validated end-to-end without reopening the Phone Activity;
- fixed Android Auto lyrics latency compensation is intentionally absent from production after physical comparison;
- bounded cold-start provider-failure recovery is deterministic-test covered; the original mid-track cold-start scenario is retained as a non-blocking regression follow-up;
- PR #84 is implementation-complete and may leave Draft state once final repository checks are green.

## Explicit non-goals after completion

Completing this document does not imply completion of:

- full lyrics Browse/Expanded UI;
- Lyrics Provider/diagnostic Browse UI;
- queue UI;
- source-app diagnostics;
- Sync/calibration controls;
- Android Auto Karaoke;
- Car App Library templated media migration.

Those require separate product/implementation authorization.
