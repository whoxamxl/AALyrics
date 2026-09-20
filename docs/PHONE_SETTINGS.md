# Phone Settings Specification

## Status

This document defines the production presentation contract for the Phone `Settings` destination.

The production `SettingsScreen` and its Phone-local row components are implemented as a presentation-only destination: `:ui:phone` receives immutable state and emits callbacks. Application/capability layers continue to own persistence and runtime policy.

The first Settings surface was integrated into `main` via PR #44 and polished in PR #45. PR #49 implements the approved second-level `Advanced` surface containing one functional Debug preference (`Verbose details`) and one disabled future Experimental affordance (`Karaoke mode`).

## Product intent

Settings should expose stable user configuration without turning the Phone UI into an owner of application state.

The production Settings surface remains intentionally focused. The Advanced extension adds only one functional presentation preference and one explicitly unavailable future affordance; it does not open a general developer-settings surface.

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
├─ App
│  ├─ Version / update
│  │  ├─ Version                    <version>
│  │  └─ <stateful update action>
│  ├─ Changelog                               >
│  ├─ Source code                 GitHub       ↗
│  └─ License                                 >
└─ Advanced                                  >

Advanced
├─ Debug
│  └─ Verbose details                  [switch]
└─ Experimental features
   └─ Karaoke mode             [OFF, unavailable]

[branding footer]
AALyrics mark
AALyrics
Version: vX.X.X
© <current year> Yuta Miura (whoxamxl)
```

Provider preferences, appearance/theme selection, log export, and other future taxonomy remain out of scope. The approved Advanced extension is intentionally narrow: Verbose Details controls read-only diagnostic presentation, while Karaoke mode remains visible but unavailable and unwired.

## Destination composition

`SettingsScreen` is destination-owned content inside the existing `PhoneAppShell`.

It should:

- fill the available destination area;
- scroll vertically;
- use compact destination-side padding;
- reserve the shell-provided Playback Surface overlay inset at the bottom of the scroll content;
- not duplicate the persistent Top Bar, Playback Surface, or Bottom Navigation.

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
├─ androidAutoCompatibilityStatus
└─ verboseDetailsEnabled
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

## App section

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
                         Check for updates

Checking for updates…                    ◌

Up to date                               ✓

Update available: v0.1.2        ↓ Download

Downloading v0.1.2                       ◌

Downloaded v0.1.2                        ✓

Update check failed            ⓘ   ↻ Retry
```

Every update-state row uses the same trailing-edge alignment as the installed version value. Download and Retry are compact inline actions rather than filled buttons; Download uses the same leading-action-icon pattern as Retry. Spinner/check/action content therefore terminates on the same right-edge guide across all phases.

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

### Changelog

A `Changelog >` navigation row sits directly below the Version/update block.

Pressing it opens an on-demand changelog dialog. The Phone UI renders presentation state only:

```text
IDLE
LOADING
READY
FAILED
```

Application/runtime wiring fetches release notes from the canonical GitHub Releases source and maps the result into:

- release version;
- presentation-ready release-note body;
- optional failure reason.

The UI does not call GitHub directly. Retry emits the same changelog-load callback again.

### Source code

A `Source code` row sits below Changelog and opens the canonical repository through the existing `onOpenGitHub` callback.

Presentation:

```text
Source code                  GitHub   [external-link icon]
```

The trailing glyph is a proper external-link icon rather than a text arrow. The row is visually distinct from internal navigation rows, which continue to use the standard chevron.

Canonical repository:

```text
https://github.com/whoxamxl/AALyrics
```

### License

A `License >` internal navigation row sits directly below Source code.

The repository license is **PolyForm Noncommercial License 1.0.0**. The Phone Settings surface emits `onLicenseRequested`; the detailed license presentation may be supplied by the application/navigation layer without duplicating license ownership in the Settings row itself.

### Branding footer

The Settings destination ends with a centered, always-visible AALyrics branding footer modeled after a compact About surface:

```text
[AALyrics foreground mark]

AALyrics

Version: vX.X.X
© <current year> Yuta Miura (whoxamxl)
```

The mark is derived from `branding/android/AALyrics_foreground_android.svg` and rendered from a Phone-local VectorDrawable so `:ui:phone` does not depend on `:app` resources.

The installed version comes from presentation state. The current year is also supplied as presentation state so it is not hard-coded into the Composable.

The copyright/username line acts as the GitHub affordance and emits `onOpenGitHub`. Application/runtime wiring should open:

```text
https://github.com/whoxamxl/AALyrics
```

The Phone UI must not own Android intent/browser launching.

## Advanced

The main Settings list exposes one internal navigation row:

```text
Advanced                                      >
```

Opening it presents a second-level Settings surface rather than adding another primary bottom-navigation destination.

Initial structure:

```text
Advanced

Debug
Verbose details                         [OFF]

Experimental features
Karaoke mode                            [OFF]
                                        Not available yet
```

### Debug — Verbose details

`Verbose details` is a functional user preference.

Its only approved effect is presentation density in the Phone Details destination:

```text
OFF -> normal user-facing Details only
ON  -> normal Details + Developer / Diagnostics section
```

The detailed field contract is defined in `docs/PHONE_DETAILS.md`.

The setting must not:

- trigger provider lookup;
- change provider ordering, scoring, or selected candidate;
- change synchronization preference;
- alter timing/calibration;
- enable WORD-level rendering;
- change Translation execution;
- change MediaSession selection or playback behavior.

The Composable must not own persistence. A future implementation should receive the resolved setting value and emit a setting-change callback through the application/presentation boundary.

### Experimental features — Karaoke mode

`Karaoke mode` is intentionally present only as a disabled future affordance in this stage.

Required initial presentation:

- label: `Karaoke mode`;
- value: OFF;
- control: disabled / non-interactive;
- concise unavailable/experimental explanation where needed.

This row has **no runtime wiring** in the current stage:

- no persisted Karaoke setting;
- no callback that changes application state;
- no change to `preferredSyncType`;
- no provider-selection effect;
- no change to existing LINE-oriented Phone rendering;
- no WORD-level highlighting activation;
- no timing or Karaoke projection activation.

The current runtime may already acquire WORD-capable lyrics through existing provider/selection behavior. The disabled row must not reinterpret or modify that behavior.

Karaoke becomes functional only through a separately authorized implementation slice following `docs/KARAOKE_ARCHITECTURE.md`.

### Advanced navigation ownership

The Advanced surface remains Settings-owned UI. It does not become a fifth primary destination.

A suitable presentation interaction is conceptually:

```text
SettingsScreen
    -> onAdvancedRequested()
application/navigation owner
    -> AdvancedSettingsScreen
```

PR #49 keeps Advanced as local Settings-owned presentation state. Opening the row swaps the Settings body to `AdvancedSettingsScreen`; its Back affordance and system Back return to the main Settings body without introducing a fifth primary destination or an application navigation stack.

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
- changelog ready/failure states;
- branding footer;
- Advanced navigation row;
- Advanced screen with Verbose details OFF and ON;
- disabled Karaoke mode row;
- full Settings destination hosted inside `PhoneAppShell` with Playback Surface visible.

## Runtime wiring boundary

The production Settings slice now includes the Advanced presentation contract. PR #49 keeps Verbose Details persistence application-owned and leaves Karaoke mode intentionally disabled and unwired.

PR #50 implements the Phone runtime-host application-composition boundary from `docs/PHONE_RUNTIME_HOST.md`, making Settings reachable on-device. It maps existing application/capability state into `SettingsScreenUiState` and exposes existing application-owned actions through callbacks, including:

- `TranslationSettingsStore.settings` -> Translation rows;
- existing Translation callbacks/capability seams where already implemented;
- Android Auto acknowledgement -> presentation status;
- compatibility-row callback -> existing compatibility setup flow;
- application-owned Verbose Details preference -> Settings and Details presentation state;
- build/version facts and other already-owned application presentation data.

A durable Plain auto-scroll preference remains a separate ownership decision unless the runtime-host implementation has an already-approved backing seam.

The host does not wire active no-op callbacks for unfinished Settings capabilities. Update and Changelog currently map to explicit `UNAVAILABLE` presentation states; their controls remain visibly unavailable rather than expanding PR #50 into release-network implementation.

That wiring must preserve the existing capability ownership documented in the relevant architecture files.

## Explicitly deferred

The first Settings slice does not define or implement:

- provider ordering/preferences;
- theme/appearance selection;
- diagnostics log viewer/export beyond the approved Verbose Details fields;
- notification-access management;
- Translation Provider selection UI;
- Android Auto runtime/projection settings;
- Sync/calibration settings;
- cache controls;
- functional Karaoke mode or any Karaoke runtime wiring;
- additional developer/experimental controls beyond the two approved Advanced rows.
