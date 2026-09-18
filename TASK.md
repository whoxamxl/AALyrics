# Android Auto Now Playing and Sideload

## Branch and baseline

- Branch: `feature/android-auto-now-playing`.
- Base: `main` at `342c84a6023785c4b8682cfa6a7bd5999a3148ed` after PR #35 merged.
- Working-fork behavioral reference: `whoxamxl/auto-lyrics` main `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (v1.13.0).
- Classification: **PRESERVE / REFACTOR** for proven Android Auto media-host behavior, **REWRITE** for AALyrics ownership/integration.
- Authoritative references: `AGENTS.md`, `docs/MIGRATION_INVENTORY.md`, `docs/UI_ARCHITECTURE.md`, `docs/PRESENTATION_STATE_ARCHITECTURE.md`, and `docs/KARAOKE_ARCHITECTURE.md`.
- The user explicitly authorized implementation of Android Auto split/full Now Playing behavior and a sideloadable Android Auto build path.
- Lyrics browse-window/browse-tree presentation is explicitly deferred from this slice.

## Goal

Expose AALyrics as a sideloadable Android Auto media app and project the existing normalized playback + lyrics state into the host-managed Now Playing surfaces without reintroducing the working fork's `MediaTracker` monolith.

```text
existing selected MediaSession runtime
        ↓
PlaybackSnapshot + LyricsState
        ↓
application composition
        ↓
automotive presentation binding
        ↓
MediaBrowserServiceCompat + MediaSessionCompat
        ↓
Android Auto host
   ├─ split / compact Now Playing
   └─ full Now Playing
```

## Acceptance criteria

- Add Android Auto media-app discovery metadata and an `automotive_app_desc.xml` declaring `media`.
- Add a thin `MediaBrowserServiceCompat` host service that publishes a `MediaSessionCompat`.
- Populate `DISPLAY_TITLE` with current track/artist and `DISPLAY_SUBTITLE` with the active line-timed lyric so Android Auto can render useful split/compact and full Now Playing metadata.
- Mirror normalized playback state and projected playback position into the automotive MediaSession.
- Forward host Play/Pause/Previous/Next/Seek controls through the explicit transport boundary owned by `:platform:media`; do not rediscover active sessions from `:ui:automotive`.
- Keep current-line selection line-level only in this slice. Do not implement word-level karaoke, translation, timing calibration/offset, cache, sync editing, or lyrics browse windows.
- Preserve `:ui:automotive` independence from concrete providers and `:platform:media`.
- Make debug APKs available as CI artifacts suitable for sideloading.
- Document Android Auto developer-mode / Unknown sources setup and local `installDebug` workflow.
- Add deterministic tests for automotive state projection and transport routing.
- Run repository validation, review the complete diff, open a PR, and stop before merge for explicit approval.

## Planned commits

- [x] Prepare Android Auto Now Playing task and branch.
- [x] Add platform transport-control boundary.
- [x] Add automotive runtime binding and line-level Now Playing projection.
- [x] Add Android Auto MediaBrowser/MediaSession host service and media discovery descriptor.
- [x] Wire application playback/lyrics state into the automotive host.
- [x] Add sideloadable CI artifact and setup documentation.
- [x] Add deterministic regression tests.
- [x] Run validation and architecture checks.
- [x] Review complete diff and open PR #38.
- [x] Address first bounded Codex review findings.
- [ ] Complete second bounded Codex review.
- [ ] Stop before merge for explicit approval.

## Scope guard

This slice migrates the proven Android Auto Now Playing media-host path only. It must not copy legacy provider, translation, calibration, cache, artwork/color, word-karaoke, or browse-window logic into AALyrics. Richer automotive browse presentation remains a later slice.


## Validation record

Initial PR validation passed in CI runs #172 and #173. The first bounded Codex review then identified two current-scope P1 findings and one current-scope P2 finding:

- reject untrusted MediaBrowser clients before exposing the MediaSession token;
- retain the source playback-position sample time instead of anchoring projection only at AALyrics receipt time;
- require notification-listener access in the fresh-sideload setup.

All three findings were fixed. CI run #182 subsequently passed the architecture guard, debug APK build, complete unit-test suite, and `aalyrics-debug-apk` artifact upload. Browse-window UI remains explicitly deferred. A second bounded review is pending before the merge approval gate.
