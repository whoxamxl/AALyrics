# Android Auto Now Playing Completion

## Branch and baseline

- Branch: `feature/android-auto-now-playing`.
- Original base: `main` at `52e9249395206f8c3821cc5c8e5893a8ccdcd125` (PR #82 merged).
- Reconciled baseline: current `main` at `86f4cd300161eb2038c637094a8e75a0afa9bf80` (changelog-only PR #83).
- Current checkpoint: PR #84 is implementation-complete and ready for merge review. Physical host rendering and forced process-death recovery are validated, the bounded cold-start lookup retry is implemented with deterministic coverage, and CI is green. Fixed Android Auto latency compensation is intentionally absent. Re-running the original Spotify-already-playing mid-track cold-start scenario on-device is retained as a non-blocking regression follow-up rather than a merge gate.
- Authoritative slice contract: `docs/ANDROID_AUTO_NOW_PLAYING.md`.
- Broader Android Auto strategy: `docs/ANDROID_AUTO_MEDIA_STRATEGY.md`.
- Shared timing authority: `docs/TIMING_ARCHITECTURE.md`.
- Translation ownership: `docs/TRANSLATION_ARCHITECTURE.md`.
- Karaoke ownership/non-goals: `docs/KARAOKE_ARCHITECTURE.md`.
- Repository execution rules: `AGENTS.md`.

## Goal

Complete the existing legacy Android Auto Now Playing surface without expanding it into Browse, Queue, Karaoke, diagnostics, or Car App Library work.

The finished surface is:

```text
selected media session
        ↓
normalized playback / artwork / capabilities
        +
canonical LyricsState
        +
Translation settings/state
        ↓
shared playback/timing semantics
        ↓
Automotive Now Playing projection
        ↓
MediaSessionCompat metadata + PlaybackStateCompat
        ↓
Android Auto host
```

The product contract is intentionally narrow:

- track title / artist / album / duration;
- album artwork;
- real playback state/rate/position;
- only source-supported transport actions;
- exactly one synchronized source lyric line;
- optional translated second line;
- animated Lyrics/Translation loading heartbeat.

## Fixed product decisions

### Lyrics

- LINE_SYNC: show the canonical current line only.
- WORD_SYNC: show the canonical current line only.
- WORD_SYNC does **not** use Karaoke projection in Android Auto.
- PLAIN: show `Synced lyrics unavailable`; never synthesize playback alignment.
- No previous/next lyric rows.
- No `▶` current-line marker.
- Before a timed line/interlude: `♪`.
- Lyrics Loading heartbeat:
  - `Loading lyrics.`
  - `Loading lyrics..`
  - `Loading lyrics...`
- Lyrics not found: `No synced lyrics found`.
- Lyrics failed: `Unable to load lyrics`.

### Translation

- Canonical/source current line is always the first line.
- Translation OFF/Idle/NotRequired: source only.
- Translation Translating: source + second-line heartbeat:
  - `Translating.`
  - `Translating..`
  - `Translating...`
- Translation Ready: source + translated second line only for exact current canonical identity/current target and a genuinely translated nonblank artifact line.
- Translation Failed: source only.
- No persistent Translation failure text.
- No partial Translation Artifact publication.
- Two-line layout is fixed; long lines may be ellipsized by the Android Auto host.

### Timing

- Reuse the current normalized playback clock.
- Use `LyricsTimingOffset.ZERO` in production for this slice.
- Use `effectiveLyricsPosition(...)` + `projectLyricsTiming(...)`.
- Automotive consumes `LyricsTimingProjection.activeLineIndex` only.
- Remove/deprecate automotive-local current-line selection and avoid a second UI-owned playback clock.
- Respect `positionUpdatedAtMonotonicMs` when valid and the existing `positionSampledAtMonotonicMs` fallback when source time is unavailable.
- Do not add fixed Android Auto audio-latency compensation in this slice. Keep playback position, canonical timestamps, Phone timing, and Automotive timing on the shared zero-offset semantics; investigate Karaoke sweep timing separately.

### Loading heartbeat

- Cadence: 250 ms.
- Frames are derived presentation state, not domain states.
- Heartbeat must continue while playback is paused.
- Service rendering must therefore run while playback is advancing **or** an animated loading state is active.
- Track/Lyrics/Translation/artwork/capability events render immediately.
- Host coalescing is allowed; AALyrics guarantees publication cadence, not physical redraw cadence.

### Artwork

- Reuse the existing selected-session artwork state.
- Do not rediscover artwork from Automotive UI.
- Publish current artwork into AALyrics' MediaSession metadata.
- Artwork arrival/null/track changes must invalidate visible metadata correctly.
- Do not implement queue artwork.

### Controls

- Reuse `PlaybackControlState.capabilities`.
- Advertise play/pause/previous/next/seek only when supported.
- Do not advertise queue actions.
- Existing transport callbacks remain the command boundary.

## Explicitly out of scope

Do not implement:

- Android Auto Karaoke;
- current-word progress/emphasis;
- `▶` current-line marker;
- PLAIN pseudo-sync;
- Lyrics Provider information;
- playback source/app/package information;
- queue presentation;
- Browse/Expanded Lyrics;
- Sync/calibration controls;
- non-zero persisted or user-configurable timing offset;
- Car App Library / templated media;
- provider selection changes or unbounded/manual refetch behavior;
- Translation algorithms/providers/model lifecycle changes;
- Phone UI changes except unavoidable shared-contract compile fixes.

## Implementation checkpoints

1. [x] Documentation gate: create authoritative Android Auto Now Playing contract and align active task.
2. [x] Align durable architecture/roadmap docs with the authorized surface contract and explicit Karaoke exclusion.
3. [x] Automotive state/runtime contract: expose the smallest required artwork, playback-capability, Translation, and timing inputs.
4. [x] Shared timing integration: remove automotive-local current-line authority and consume shared `activeLineIndex`.
5. [x] Lyrics presentation: implement LINE/WORD line-only mapping, PLAIN fallback, lifecycle copy, and deterministic loading heartbeat.
6. [x] Artwork + controls: wire existing artwork state and real capability-derived PlaybackState actions.
7. [x] Translation: wire exact-identity Translation state, two-line Ready presentation, translating heartbeat, and Failed source-only fallback.
8. [x] MediaSession invalidation: ensure visible metadata changes update while position-only movement within one lyric line does not rebuild metadata.
9. [x] Focused tests: cover the matrix in `docs/ANDROID_AUTO_NOW_PLAYING.md`.
10. [x] Final validation: architecture checks, relevant unit tests, debug APK, regression/scope audit, docs alignment, and available DHU/physical-host validation. Physical Android Auto rendering is now observed; host-dependent behavior is no longer classified as wholly unverified.
11. [x] Open a Draft PR only after final validation and stop per `AGENTS.md`.
12. [x] Android Auto process-recovery hardening: treat the active Automotive browser service as an independent lyrics-demand source and re-request Notification Listener binding when the host service activates or the listener disconnects.
13. [x] Cold-start lookup recovery: retry a provider-failure terminal path once after a bounded 500 ms backoff, never retry clean NotFound, and expose current lookup attempt/provider failure types in Verbose Details diagnostics.

## Expected implementation surface

Primary production files are expected to include:

- `app/src/main/java/io/github/whoxamxl/aalyrics/AALyricsApplication.kt`
- `ui/automotive/src/main/java/io/github/whoxamxl/aalyrics/ui/automotive/AutomotiveRuntimeHost.kt`
- `ui/automotive/src/main/java/io/github/whoxamxl/aalyrics/ui/automotive/state/AutomotiveLyricsUiState.kt`
- `ui/automotive/src/main/java/io/github/whoxamxl/aalyrics/ui/automotive/screen/NowPlayingScreen.kt`
- `ui/automotive/src/main/java/io/github/whoxamxl/aalyrics/ui/automotive/service/LyricsBrowserService.kt`
- automotive focused tests.

Existing authorities that should normally be consumed rather than rewritten:

- `core/timing/.../EffectiveLyricsPosition.kt`
- `core/timing/.../LyricsTimingProjection.kt`
- `platform/media/.../PlaybackSnapshotSink.kt`
- `translation/core/.../TranslationModels.kt`.

Implementation evidence may justify a small new Automotive presentation type/file. Do not create speculative shared UI abstractions or a Phone/Automotive universal state model.

## Acceptance criteria

- Automotive display identity prefers album artist when available and falls back to the full track artist without rewriting upstream track-artist metadata.
- Jacket artwork appears when selected-session artwork is available.
- Clearing/changing artwork cannot intentionally leave a previous track's jacket attached to the new track.
- Android Auto action mask reflects real `PlaybackControlState.capabilities`.
- LINE_SYNC and WORD_SYNC show the same semantic current line authority as shared AALyrics timing.
- Android Auto no longer owns an independent current-line selector or UI-created fallback playback clock.
- PLAIN never receives pseudo-sync and presents `Synced lyrics unavailable`.
- Lyrics lifecycle states are distinguishable from a frozen surface.
- `Loading lyrics.` / `..` / `...` cycles at the 250 ms presentation cadence, including while paused.
- Translation loading displays the canonical current line plus `Translating.` / `..` / `...`.
- Translation Ready displays exactly two logical lines: canonical source first, translated line second.
- Long source/translation text is allowed to be host-ellipsized without switching layout.
- Translation Failed returns to source-only presentation.
- Stale Translation identity cannot appear on the current canonical lyrics.
- Lyrics loading/not-found/failed/plain states take precedence over Translation status.
- No Karaoke word sweep/progress/marker is introduced.
- Provider/source/queue facts remain absent from Now Playing.
- Position-only advancement inside the same lyric line does not require metadata reconstruction.
- Lyric line, loading frame, Translation state/result, artwork, and track metadata changes invalidate visible metadata.
- Existing Phone Lyrics/Translation/Karaoke timing remains unchanged by Android Auto connection state.
- Closing the Phone Activity must not suspend lyrics work while either Android Auto projection or the active Automotive browser service still represents presentation demand.
- If Android Auto recreates the process through `LyricsBrowserService`, the Automotive host demand becomes active immediately and AALyrics re-requests the system Notification Listener binding so current playback observation can recover without reopening the Phone Activity.
- Notification Listener disconnection requests a system rebind instead of leaving MediaSession observation permanently detached.
- A current-track lookup that produces no usable candidate because one or more provider calls failed receives exactly one automatic retry after a 500 ms backoff; a clean NotFound result is never retried.
- Provider failure diagnostics remain separate from canonical LyricsState and are shown only in Verbose Details for the matching current lookup.
- No foreground-service keepalive, unconditional started service, boot receiver, or unbounded provider retry loop is introduced for this recovery path.
- No provider, Translation-engine, timing-engine, or Car App Library ownership is moved into `:ui:automotive`.

## Validation matrix

Codex must implement deterministic tests for at least:

- no media;
- waiting;
- Lyrics loading frames;
- paused Lyrics loading heartbeat;
- Lyrics not found;
- Lyrics failed;
- PLAIN-only;
- LINE_SYNC before first line/current line;
- WORD_SYNC line-only;
- interlude `♪`;
- line boundary movement from shared timing;
- stale previous-track Lyrics rejection;
- Translation disabled/translating/ready/not-required/failed;
- stale Translation canonical identity;
- translated=false/blank translated line;
- Translation loading frames;
- Lyrics failure/loading precedence over Translation;
- artwork present/delayed/null/track transition;
- artwork-only metadata invalidation;
- each playback capability mapping;
- missing source timestamp fallback using the stable AALyrics sample anchor;
- paused position stability;
- metadata unchanged for position-only motion inside one lyric line;
- metadata changed for lyric/loading/Translation/artwork changes;
- Automotive host-service demand keeps the gate active independently of Phone foreground and CarConnection demand;
- removing one Automotive demand source does not suspend provider work while another remains active;
- transient provider failure retries exactly once and can resolve within the same playback identity;
- persistent failure performs exactly two attempts total and then becomes Failed;
- clean NotFound does not retry;
- stale lookup diagnostics are not shown for the current track.

## Implementation progress

- Automotive runtime now receives selected-session artwork tagged to the playback track identity, source-derived transport capability facts, Translation settings/state, and the application-owned canonical lyrics identity adapter.
- Automotive state has explicit lyric primary/secondary presentation slots. Existing visible output is retained at this checkpoint.
- `:ui:automotive:compileDebugKotlin` and `:app:compileDebugKotlin` passed after this checkpoint.
- Automotive now uses the shared projected playback clock, zero-offset effective lyrics position, and `LyricsTimingProjection.activeLineIndex`. The Automotive-local line selector and observation-time fallback clock were removed. Source timestamps and AALyrics sample-time fallback are covered by focused test sources.
- Checkpoint 4 test sources and app compiled; architecture checks passed. Branch Build workflow [run 36210171777](https://github.com/whoxamxl/AALyrics/actions/runs/36210171777) passed the debug APK build and unit tests. Local Gradle test workers cannot establish a loopback connection in this environment.
- Automotive Lyrics now distinguishes no media, waiting, loading, not found, failed, PLAIN, and timed states. LINE and WORD use one canonical line, blank/interlude moments show `♪`, and loading frames derive from monotonic time every 250 ms. The service continues ticking during paused loading. Focused Automotive test sources compiled.
- Selected-session artwork now enters MediaSession metadata only when its tagged track identity matches current playback; arrival and clear events render immediately. PlaybackState actions derive individually from existing selected-source capability facts and omit unsupported controls and queue actions. Focused artwork identity and action-mask test sources compiled with the app.
- Branch Build workflow [run 36210491667](https://github.com/whoxamxl/AALyrics/actions/runs/36210491667) passed through artwork/controls, including unit tests and debug APK build.
- Translation presentation now consumes application-owned settings/state and canonical identity. Current synchronized source stays first; exact matching Translating adds animated second-line status, matching Ready adds a genuinely translated nonblank second line, and Failed/NotRequired/stale states remain source-only. Focused test sources compiled with the app.
- MediaSession metadata publication now keys on all serialized track, lyric, Translation, and artwork facts. Playback position/rate are excluded from this key, so same-line position ticks update PlaybackState without reconstructing metadata. Focused invalidation tests compiled with the app.
- Focused tests cover Lyrics lifecycle and loading frames, LINE/WORD and PLAIN behavior, shared timing boundaries and clock anchors, stale lyrics/Translation identity, Translation state and artifact gating, selected artwork identity/clearing, action masks, playback status mapping, paused heartbeat, metadata invalidation, and independent Automotive host-service demand.
- Android Auto lifecycle recovery now has two independent Automotive demand signals in addition to Phone foreground: `CarConnection.CONNECTION_TYPE_PROJECTION` and the actual `LyricsBrowserService` lifecycle. Process recreation through the Automotive service therefore reactivates provider demand even before Phone UI foregrounding.
- Automotive host activation proactively requests Notification Listener rebind, and `MediaSessionListenerService.onListenerDisconnected()` requests rebind again. This is recovery-oriented; the branch intentionally does not add a foreground-service keepalive or unconditional `startService()`.

## Final validation evidence

- Latest branch Build workflow [run 36220867319](https://github.com/whoxamxl/AALyrics/actions/runs/36220867319) passed architecture checks, debug APK build, all unit tests, and sideloadable debug APK upload after the process-recovery, bounded lookup-retry, and Verbose Details diagnostics changes.
- Local Automotive/application production and test sources compiled. Local Gradle test and APK worker processes cannot establish a loopback connection in this environment; CI provided the complete test/APK result.
- The branch-wide diff contains the approved documentation, application boundary, shared clock extraction, Automotive runtime/state/metadata wiring, process-recovery hardening, bounded transient lookup retry, Verbose Details lookup diagnostics, and focused tests. No concrete provider adapter, candidate-selection policy, Translation engine, Karaoke, queue, Browse, Sync, Car App Library, or persisted-state change is present. Reset behavior needs no change.
- The branch was reconciled with current `main` after the scope audit. Its only intervening change was `CHANGELOG.md`, with no implementation conflict.
- The CI debug APK was installed over the existing debuggable phone build using the matching local debug certificate; app data was preserved. DHU connected over ADB but reported no video focus, so it could not provide rendered-frame evidence.
- Physical Android Auto host validation subsequently provided real host-rendering evidence. Title/artist/lyrics presentation was observed on-device; host-controlled wrapping/ellipsis remains outside AALyrics layout control. An experimental `-75 ms` lyrics-only offset was tested and then deliberately removed because the perceived lead was not defensible as a fixed production value and may have been influenced by Karaoke sweep presentation.
- Physical process-death recovery validation passed: while Android Auto and Spotify were active with the Phone UI closed, the AALyrics process was force-killed, Android Auto recreated the process, and current playback/lyrics operation recovered without reopening the Phone Activity.
- The bounded cold-start lookup retry is covered by deterministic unit tests and CI. Re-running the original real-device symptom (Spotify already playing, then cold-start AALyrics mid-track) is a useful non-blocking regression follow-up, but it is not a remaining merge gate because retry ownership, attempt bounds, clean NotFound behavior, cancellation/supersession, and diagnostics are covered deterministically and process recreation is independently validated on-device.
