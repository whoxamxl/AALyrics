# Phone Shell Runtime Host

## Branch and baseline

- Branch: `feature/phone-shell-runtime-host`.
- Base: `main` at `105ef4a8511d2fc085cef215d5ecb1eb438a8d03` (PR #49 merged).
- Classification: **PHONE RUNTIME HOST / DEVICE-TEST ENABLEMENT**.
- Authoritative references: `AGENTS.md`, `docs/PHONE_RUNTIME_HOST.md`, `docs/PHONE_UI_SPEC.md`, `docs/UI_ARCHITECTURE.md`, plus the existing destination specifications.
- This first checkpoint is **documentation-only**. Do not change production code until the user continues implementation work on this same branch.

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
- [ ] Preserve Notification Access and Android Auto compatibility entry gating.
- [ ] Change only the READY path from the foundation `TextView` to the production Compose host.
- [ ] Keep `Lyrics` as the default/home destination.
- [ ] Keep primary destination selection host-owned and saveable across normal Activity recreation where practical.

### Phone shell
- [ ] Host the production `PhoneAppShell` under `AALyricsTheme`.
- [ ] Supply shell state from application-owned presentation/runtime state.
- [ ] Keep Top Bar, Playback Surface, and Bottom Navigation shell-owned.
- [ ] Wire playback actions through the application/platform boundary rather than importing media framework objects into `:ui:phone`.
- [ ] Keep playback-app launching application-owned.
- [ ] Keep Translation toggle ownership outside `:ui:phone`.

### Destinations
- [ ] Render the production `LyricsScreen` from presentation-ready runtime state.
- [ ] Preserve current Lyrics follow/browse ownership rules and do not redesign lyric timing.
- [ ] Render a deliberate non-functional Sync placeholder without inventing calibration behavior.
- [ ] Render the production `DetailsScreen` from `AALyricsApplication.phoneDetailsState`.
- [ ] Render the production `SettingsScreen` from application-owned presentation state and callbacks.
- [ ] Wire `Verbose details` to its existing application-owned persisted preference.
- [ ] Keep Karaoke mode disabled/unwired.
- [ ] Do not expose provider DTOs, `MediaController`, raw `PlaybackState`, Android intents, or persistence objects to Phone composables.

### Settings/runtime scope
- [ ] Wire existing implemented capabilities where application/runtime support already exists.
- [ ] Add only the minimal application-owned presentation mapping/state required to render the approved Settings contract on-device.
- [ ] Do not fabricate update/changelog/model-management behavior merely to make controls appear active.
- [ ] Controls whose backing runtime is not yet implemented must be presented honestly (disabled/unavailable or otherwise non-misleading) until separately implemented.
- [ ] Re-entering Android Auto compatibility setup must reuse the existing entry/onboarding ownership rather than duplicate it inside `:ui:phone`.

### Device-test readiness
- [ ] Debug APK launches the real Phone shell after onboarding prerequisites are satisfied.
- [ ] Bottom navigation can exercise Lyrics / Sync / Details / Settings on a physical device.
- [ ] Playback Surface can be exercised against a selected live MediaSession when available.
- [ ] Details updates from current playback/lyrics state.
- [ ] Advanced > Verbose details can be toggled and reflected in Details.
- [ ] Existing onboarding remains reachable/functional after host migration.

### Validation
- [ ] Add focused state/host tests where durable.
- [ ] Run architecture checks, unit tests, and debug APK build.
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

## Stop point for this checkpoint

The branch and documentation are aligned for the upcoming runtime-host implementation. **Stop after docs.** Continue production work on this same branch only after the user explicitly resumes implementation.
