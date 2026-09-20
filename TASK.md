# Phone Settings Foundation

## Branch and baseline

- Branch: `feature/phone-settings-foundation`.
- Base: `main` at `e5443e52673cd130a437f2b5185d4d1978ee45e9` after PR #42 merged.
- Classification: **PHONE SETTINGS PRESENTATION FOUNDATION**.
- Authoritative references: `AGENTS.md`, `docs/PHONE_UI_SPEC.md`, `docs/PHONE_SETTINGS.md`, `docs/PHONE_LYRICS_VIEWPORT.md`, `docs/TRANSLATION_ARCHITECTURE.md`, and `docs/ANDROID_AUTO_COMPATIBILITY.md`.
- The user explicitly authorized this UI implementation slice.

## Goal

Define and implement the first production Phone Settings destination using presentation-ready state and callbacks only.

This slice should establish a durable Settings composition and reusable Phone-local setting-row primitives without moving persistence, Translation policy, Android Auto acknowledgement ownership, or other application state into `:ui:phone`.

## Accepted first-slice content

### Lyrics

- `Plain lyrics auto-scroll` switch.
- Compact on-demand info affordance using the approved explanatory copy from the LyricsViewport specification.
- The UI exposes state and a callback only; durable preference ownership/wiring is not introduced by this slice.

### Translation

- `Translation` enabled/disabled switch.
- `Target language` navigation row showing the selected display name.
- A compact target-language picker driven entirely by presentation-provided options.
- The target-language row remains available while Translation is disabled so the user can preconfigure the target.
- `:ui:phone` must not depend directly on `:translation:api` or concrete SharedPreferences.

### Android Auto

- `Compatibility setup` navigation row.
- Show presentation status as `Enabled`, `Skipped`, or `Not reviewed`.
- Tapping the row emits a callback to reopen the already implemented compatibility setup flow.
- Do not claim automatic verification of Android Auto `Unknown sources`.

## Presentation boundary

The Settings screen consumes a Phone-local immutable presentation model and emits callbacks.

Conceptually:

```text
application / capability state
        ↓
Phone Settings presentation mapping
        ↓
SettingsScreenUiState
        ↓
SettingsScreen
        ↓
callbacks
```

The UI must not own:

- SharedPreferences keys;
- `TranslationSettingsStore`;
- `AndroidAutoCompatibilityOnboarding`;
- ML Kit model lifecycle;
- Android Auto setting verification;
- Lyrics provider preferences or provider execution.

The screen should accept the shell-provided playback overlay inset and reserve it in scroll content so the final settings rows remain reachable above the floating Playback Controls Bar.

## Local component direction

Prove these components inside `:ui:phone` first:

- `SettingsSection`;
- `SettingsSwitchRow`;
- `SettingsNavigationRow`;
- `SettingInfoTooltip`;
- target-language picker presentation.

Do not promote them to `:ui:designsystem` until another screen demonstrates genuine reuse.

## Visual direction

- Vertically scrollable destination.
- Compact section headers and grouped setting rows.
- Avoid a separate oversized card for every row; related rows should read as one section.
- Preserve accessible touch targets.
- Keep explanatory copy on demand rather than permanently expanding the page.
- Use existing AALyrics colors, typography, radius, spacing, and stroke tokens.
- Validate normal, narrow, and enlarged-font layouts.

## Acceptance criteria

- [x] Add `docs/PHONE_SETTINGS.md` and align the related durable docs.
- [ ] Replace the Settings placeholder with an immutable Phone-local presentation contract.
- [ ] Implement the production `SettingsScreen`.
- [ ] Implement the first-slice Lyrics, Translation, and Android Auto sections described above.
- [ ] Keep `:ui:phone` free of concrete persistence/application/Translation-engine dependencies.
- [ ] Reserve the shell playback-overlay inset in Settings scroll content.
- [ ] Add deterministic Previews for typical, narrow, enlarged-font, Translation-off, and Android Auto status variants.
- [ ] Render the production Settings destination in the shell Preview.
- [ ] Run CI and review the final diff before integration.
- [ ] Stop before merge until explicit user approval.

## Scope guard

This slice does not implement:

- application/runtime wiring for the setting callbacks;
- durable Plain lyrics auto-scroll persistence;
- new Translation algorithms, providers, or model-lifecycle behavior;
- Android Auto Developer Mode or `Unknown sources` verification;
- provider preference UI;
- appearance/theme settings;
- About/diagnostics;
- Sync or Details destination behavior;
- navigation-framework/ViewModel architecture.
