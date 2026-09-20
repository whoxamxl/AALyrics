# Phone Settings Specification

## Status

This document defines the first production presentation contract for the Phone `Settings` destination.

The production `SettingsScreen` and its Phone-local row components are implemented as a presentation-only destination: `:ui:phone` receives immutable state and emits callbacks. Application/capability layers continue to own persistence and runtime policy.

Implementation branch: `feature/phone-settings-foundation`.

## Product intent

Settings should expose stable user configuration without turning the Phone UI into an owner of application state.

The first surface is intentionally small and based only on settings whose product meaning is already established elsewhere in the repository.

Initial structure:

```text
Settings
├─ Lyrics
│  └─ Plain lyrics auto-scroll       [switch]  ⓘ
├─ Translation
│  ├─ Translation                    [switch]
│  └─ Target language                <value>  >
├─ Android Auto
│  └─ Compatibility setup            <status> >
└─ App
   ├─ Version / update
   │  ├─ Version                    <version>
   │  └─ <stateful update action>
   └─ About                                   >
```

Provider preferences, appearance/theme selection, diagnostics, and other future taxonomy are not part of this first slice.

## Destination composition

`SettingsScreen` is destination-owned content inside the existing `PhoneAppShell`.

It should:

- fill the available destination area;
- scroll vertically;
- use compact destination-side padding;
- reserve the shell-provided playback-controls overlay inset at the bottom of the scroll content;
- not duplicate the persistent Top Bar, Playback Controls Bar, or Bottom Navigation.

The bottom inset is required because the playback controls float over destination content when a controllable media session is present.

## Presentation state boundary

The Settings surface should use Phone-local presentation models rather than importing application persistence types.

Conceptually:

```text
TranslationSettingsStore ----------------┐
Plain auto-scroll application setting ---┼─> Phone settings mapper
Android Auto acknowledgement ------------┘           ↓
                                              SettingsScreenUiState
                                                       ↓
                                                 SettingsScreen
                                                       ↓
                                                  callbacks
```

A suitable presentation model may contain facts equivalent to:

```text
SettingsScreenUiState
├─ plainLyricsAutoScrollEnabled
├─ translationEnabled
├─ translationTarget
├─ translationTargetOptions
└─ androidAutoCompatibilityStatus
```

The exact Kotlin names may follow implementation needs, but the ownership rule is stable.

`:ui:phone` must not:

- read or write SharedPreferences;
- depend on the concrete application `AndroidAutoCompatibilityOnboarding` type;
- invoke `TranslationSettingsStore` directly;
- depend on ML Kit;
- download language models;
- inspect Android Auto settings;
- trigger lyrics provider lookup.

## Lyrics section

### Plain lyrics auto-scroll

The row controls whether PLAIN lyrics may estimate playback position and continuously follow the measured document extent.

Presentation:

```text
Plain lyrics auto-scroll       ⓘ   [ON]
```

The switch emits a callback. The first Settings UI slice does not establish the durable backing store for this preference.

The info affordance opens an on-demand explanation rather than showing a permanent subtitle.

Approved copy:

> Estimates where playback is in untimed lyrics and scrolls smoothly to match. Requires track duration.

The LyricsViewport continues to receive only the resolved boolean presentation value. Settings persistence must not move into the viewport.

## Translation section

Translation configuration already belongs to the Translation capability/application boundary.

### Translation toggle

Presentation:

```text
Translation                         [ON]
```

The row emits the requested enabled state. It does not start translation engines directly.

### Target language

Presentation:

```text
Target language              Japanese  >
```

The row shows the selected display name and opens a compact picker.

The picker receives presentation-ready options rather than reading `TranslationLanguages` directly from `:translation:api`. This preserves the UI dependency boundary.

The current product target set is:

- English;
- Japanese;
- French;
- German;
- Spanish;
- Korean;
- Chinese;
- Italian;
- Portuguese.

The persisted default target is English.

The target-language row remains available while Translation is disabled. Changing the target while disabled is valid configuration and can be applied when Translation is later enabled.

Each language row also exposes Translation-model readiness through one stable trailing action slot:

- English is selectable from the start because AALyrics treats ML Kit English support as built in and requires no remote language-pack download;
- an unavailable remote model is not selectable and shows an explicit download action;
- an active download is not selectable and shows an indeterminate loading indicator;
- a ready remote model becomes selectable and shows no status icon unless it is the selected target;
- the selected ready/built-in target shows the check mark in the same trailing slot;
- a failed download is not selectable and shows a retry action in the trailing slot.

A successful manual download therefore transitions the row from disabled + download/loading UI to an ordinary selectable row. Downloading a model must not implicitly change the selected target.

The primary trailing action uses one fixed token-sized slot on every row:

```text
language label (flex) | primary trailing action
```

The primary action is exactly one of download, loading, retry, selected check, or empty. This avoids duplicate downloaded + selected icons and keeps the right edge aligned regardless of language-name length or model state.

Only a failed row adds a failure-info icon immediately before the primary retry slot. Pressing that info icon opens a tooltip containing the presentation-ready failure reason. If no specific reason is available, the UI may show a generic download-failure explanation. Non-failed rows do not show or reserve a visible failure-info action.

The picker should clearly mark the selected language, remain open when a model download/retry action or failure tooltip is used, dismiss after selecting an available target, and remain usable at narrow widths and enlarged font scales.

## Android Auto section

Presentation:

```text
Compatibility setup          Enabled  >
```

Supported presentation statuses:

- `Enabled`;
- `Skipped`;
- `Not reviewed`.

These values represent acknowledgement state only. They are not a verified copy of Android Auto's `Unknown sources` setting.

Tapping the row emits a callback that the application can use to reopen the existing `AndroidAutoCompatibilitySetupScreen`.

The Settings destination must not attempt to read, infer, toggle, or spoof Android Auto Developer Mode or `Unknown sources`.

## Settings row components

Settings-specific components begin in `:ui:phone`.

### SettingsSection

Groups related rows under one compact section title.

Related rows should visually read as one section rather than as many disconnected large cards.

### SettingsSwitchRow

Supports:

- title;
- checked state;
- enabled state when needed;
- optional compact info affordance;
- toggle callback.

The full row should provide a comfortable touch target; the info affordance remains separately actionable when present.

### SettingsNavigationRow

Supports:

- title;
- optional current value/status;
- trailing navigation affordance;
- click callback.

Long values should truncate gracefully rather than forcing the row to uncontrolled height.

### SettingInfoTooltip

A reusable Phone-local on-demand explanatory surface.

It should:

- be anchored to an explicit info affordance;
- remain short;
- dismiss normally;
- avoid permanently consuming vertical space.

The first demonstrated use is Plain lyrics auto-scroll.

## Target-language picker

The first implementation uses a compact Material 3 modal picker because the supported target set is short and finite.

Requirements:

- list only the presentation-provided options;
- identify the current selection;
- show presentation-provided model readiness for every language;
- allow selection only for built-in/ready languages;
- expose a failure-reason tooltip only for failed models;
- emit one selected language identifier;
- emit a separate manual model-download/retry request;
- dismiss after a valid target selection;
- remain open for download actions;
- remain presentation-only.

Do not expose model-download internals or Translation Provider details in this picker.

## App and About section

The final Settings section exposes app/distribution information without moving release-network behavior into `:ui:phone`.

### Version and update

Current version and update actions share one grouped Settings row.

Initial presentation:

```text
Version                         v0.1.0-dev
                         [Check for updates]
```

The Settings state carries the installed version plus an update lifecycle:

```text
IDLE
CHECKING
UP_TO_DATE
UPDATE_AVAILABLE
CHECK_FAILED
DOWNLOADING
DOWNLOADED
DOWNLOAD_FAILED
```

Expected presentation:

```text
Version                         v0.1.0-dev
                         [Check for updates]

Checking for updates…                    ◌

Up to date                               ✓

Update available: v0.1.2       [Download]

Downloading v0.1.2                       ◌

Downloaded v0.1.2                        ✓

Update check failed            ⓘ [Retry]
```

The UI emits separate callbacks for checking and downloading. It does not perform GitHub HTTP requests or filesystem/download-manager work directly.

Application/runtime wiring should:

1. inspect the GitHub Releases distribution channel defined in `docs/RELEASES.md`;
2. select the newest release eligible for the app's release channel;
3. compare it against the installed `BuildConfig.VERSION_NAME` / `versionCode`;
4. map the result into the update presentation lifecycle;
5. when Download is pressed, download the release APK asset to the device;
6. map download completion/failure back into presentation state.

Release APK assets follow the existing workflow naming contract:

```text
AALyrics-vX.Y.Z[-suffix].apk
```

The matching `.sha256` asset should be used by the runtime implementation to verify file integrity before a downloaded APK is treated as complete.

### Update state lifetime

Update results are intentionally short-lived so Settings does not keep presenting a stale GitHub Release result.

When the Settings destination is entered, the UI emits `onSettingsEntered`. Application/presentation wiring normalizes the update state with one rule:

```text
CHECKING     -> keep
DOWNLOADING  -> keep
everything else -> IDLE
```

Therefore `UP_TO_DATE`, `UPDATE_AVAILABLE`, `CHECK_FAILED`, `DOWNLOADED`, and `DOWNLOAD_FAILED` are results for the current Settings visit only. Leaving Settings and returning presents `Check for updates` again, forcing the next explicit check to query the current GitHub Releases state instead of reusing an old available-version result.

Active checking/downloading work remains application-owned and continues across destination changes. If that work completes while Settings is away, its completed result is normalized back to `IDLE` on the next Settings entry.

The installed version shown in Settings should come from `BuildConfig.VERSION_NAME`; debug builds currently default to `0.1.0-dev` unless the build environment overrides it.

### About

The `About` row opens a compact AALyrics dialog containing:

- a short AALyrics description;
- the current version;
- a GitHub action;
- a close action.

The GitHub action emits a callback. Application/runtime wiring should open the canonical AALyrics repository:

```text
https://github.com/whoxamxl/AALyrics
```

The Phone UI must not own Android intent/browser launching.

## Accessibility and responsive behavior

Validate at least:

- typical phone width;
- 320dp narrow width;
- enlarged font scale;
- long localized language/status values;
- playback controls visible and hidden.

All interactive controls retain accessible touch targets and meaningful semantics.

A switch row must expose the setting meaning rather than only the word `On` or `Off`. Navigation rows should expose their current value/status to accessibility services where practical.

## Preview matrix

Deterministic debug Previews should cover at least:

- typical Settings screen;
- Translation enabled;
- Translation disabled;
- target-language picker open;
- Android Auto `Enabled`;
- Android Auto `Skipped`;
- Android Auto `Not reviewed`;
- narrow width;
- enlarged font;
- app update checking;
- app up-to-date state;
- app update available state;
- app update failure/retry state;
- full Settings destination hosted inside `PhoneAppShell` with Playback Controls visible.

## Runtime wiring boundary

This first production Settings slice proves the presentation contract and interactions only.

A later application-composition slice may map:

- `TranslationSettingsStore.settings` -> Translation rows;
- Translation callbacks -> `setEnabled` / `setTargetLanguage`;
- Android Auto acknowledgement -> presentation status;
- compatibility-row callback -> existing compatibility setup flow;
- a durable Plain auto-scroll preference -> LyricsViewport presentation state.

That wiring must preserve the existing capability ownership documented in the relevant architecture files.

## Explicitly deferred

The first Settings slice does not define or implement:

- provider ordering/preferences;
- theme/appearance selection;
- diagnostics/log export;
- notification-access management;
- Translation Provider selection UI;
- Android Auto runtime/projection settings;
- Sync/calibration settings;
- cache controls;
- experimental/developer settings.
