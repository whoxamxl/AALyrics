# Phone Shell Runtime Host

## Branch and baseline

- Branch: `feature/phone-shell-runtime-host`.
- Base: `main` at `105ef4a8511d2fc085cef215d5ecb1eb438a8d03` (PR #49 merged).
- Classification: **PHONE RUNTIME HOST / DEVICE-TEST ENABLEMENT**.
- Authoritative references: `AGENTS.md`, `docs/PHONE_RUNTIME_HOST.md`, `docs/PHONE_UI_SPEC.md`, `docs/UI_ARCHITECTURE.md`, plus the existing destination specifications.
- Documentation alignment was completed first on this branch. Production implementation was then explicitly authorized and is now in progress through PR #50.

## Goal

Replace the current READY-state foundation placeholder with the real production Phone Compose surface so a debug APK can be exercised on a physical device:

```text
MainActivity entry gate
├─ Notification Access required -> existing setup View
├─ Android Auto compatibility   -> existing setup View
└─ READY
    -> AALyricsTheme
       -> PhoneAppShell
          ├─ Lyrics
          ├─ Sync placeholder
          ├─ Details
          └─ Settings
```

This is an application-composition slice. It must connect already-approved Phone presentation contracts without moving Android framework, provider, persistence, or capability ownership into `:ui:phone`.

## Implementation acceptance criteria

### Entry / host ownership
- [x] Start from current `main` on a topic branch.
- [x] Align runtime-host documentation before production changes.
- [x] Preserve Notification Access and Android Auto compatibility entry gating.
- [x] Change only the READY path from the foundation `TextView` to the production Compose host.
- [x] Keep `Lyrics` as the default/home destination.
- [x] Keep primary destination selection host-owned and saveable across normal Activity recreation where practical.

### Phone shell
- [x] Host the production `PhoneAppShell` under `AALyricsTheme`.
- [x] Supply shell state from application-owned presentation/runtime state.
- [x] Keep Top Bar, Playback Surface, and Bottom Navigation shell-owned.
- [x] Wire playback actions through the application/platform boundary rather than importing media framework objects into `:ui:phone`.
- [x] Keep playback-app launching application-owned.
- [x] Keep Translation toggle ownership outside `:ui:phone`.

### Destinations
- [x] Render the production `LyricsScreen` from presentation-ready runtime state.
- [x] Preserve current Lyrics follow/browse ownership rules and do not redesign lyric timing.
- [x] Render a deliberate non-functional Sync placeholder without inventing calibration behavior.
- [x] Render the production `DetailsScreen` from `AALyricsApplication.phoneDetailsState`.
- [x] Render the production `SettingsScreen` from application-owned presentation state and callbacks.
- [x] Wire `Verbose details` to its existing application-owned persisted preference.
- [x] Keep Karaoke mode disabled/unwired.
- [x] Do not expose provider DTOs, `MediaController`, raw `PlaybackState`, Android intents, or persistence objects to Phone composables.

### Settings/runtime scope
- [x] Wire existing implemented capabilities where application/runtime support already exists.
- [x] Add only the minimal application-owned presentation mapping/state required to render the approved Settings contract on-device.
- [x] Do not fabricate update/changelog/model-management behavior merely to make controls appear active.
- [x] Controls whose backing runtime is not yet implemented must be presented honestly (disabled/unavailable or otherwise non-misleading) until separately implemented.
- [x] Re-entering Android Auto compatibility setup must reuse the existing entry/onboarding ownership rather than duplicate it inside `:ui:phone`.

### Device-test readiness
- [ ] Debug APK launches the real Phone shell after onboarding prerequisites are satisfied.
- [ ] Bottom navigation can exercise Lyrics / Sync / Details / Settings on a physical device.
- [ ] Playback Surface can be exercised against a selected live MediaSession when available.
- [ ] Details updates from current playback/lyrics state.
- [ ] Advanced > Verbose details can be toggled and reflected in Details.
- [ ] Existing onboarding remains reachable/functional after host migration.

### Validation
- [x] Add focused state/host tests where durable.
- [x] Run architecture checks, unit tests, and debug APK build.
- [ ] Perform bounded Codex review.
- [ ] Stop before merge until explicit user approval.

## Expected implementation shape

Keep commits small and single-purpose. Expected sequence after this documentation checkpoint:

1. host/application presentation-state contracts;
2. READY-path Compose host;
3. Lyrics runtime mapping/route wiring;
4. Settings runtime mapping/callback wiring;
5. Sync placeholder presentation;
6. focused host/runtime tests;
7. documentation/status cleanup.

The exact split may change when the existing runtime seams make a smaller coherent commit preferable.

## Scope guard

Do not implement in this slice:

- Sync timing/calibration algorithms or controls;
- functional Karaoke mode;
- WORD-level Karaoke highlighting activation;
- provider ordering/preferences or provider migration work;
- new lyrics matching/scoring behavior;
- new Translation algorithms;
- persistent Translation Cache;
- new GitHub update/download capability unless already present behind an application-owned runtime seam;
- log viewer/export;
- theme/appearance settings;
- MediaSession selection-policy changes;
- Android Auto browsing/template redesign;
- unrelated UI redesign.

## Current runtime result

PR #50 now hosts the production Phone shell from `MainActivity` READY and produces a green debug APK build.

Implemented runtime behavior:

- READY -> `AALyricsTheme` -> `PhoneAppShell`;
- lifecycle-aware collection of app-owned playback, lyrics, Details, Translation, model, and Verbose Details state;
- host-local saveable primary destination and Plain auto-scroll state;
- live Lyrics mapping with monotonic playback projection;
- WORD-capable source lyrics remain line-oriented while Karaoke is unavailable;
- Playback Surface commands route through the existing application/platform media boundary;
- Translation target/model actions use the existing Translation runtime;
- Update/Changelog remain explicitly unavailable rather than active no-ops;
- Settings can re-enter the existing Android Auto compatibility setup;
- Sync is an explicit non-functional placeholder.

The code/build portion of device-test enablement is complete. The unchecked device-test items below require an actual physical-device smoke test rather than CI inference.

## Remaining stop gate

Do not merge PR #50 until:

1. bounded Codex review is complete with no current-scope blocker;
2. the final CI head is green;
3. the user explicitly authorizes merge.

Physical-device smoke testing may be performed before merge using the generated debug APK and is the next practical validation step.
