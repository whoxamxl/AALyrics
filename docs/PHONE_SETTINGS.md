# Phone Settings Specification

## Status

This document defines the production presentation contract for the Phone `Settings` destination.

The production `SettingsScreen` and its Phone-local row components are implemented as a presentation-only destination: `:ui:phone` receives immutable state and emits callbacks. Application/capability layers continue to own persistence and runtime policy.

The first Settings surface was integrated into `main` via PR #44 and polished in PR #45. PR #49 established the second-level `Advanced` surface with the functional `Verbose details` preference and disabled future `Karaoke mode` affordance. The current Advanced contract also includes explicit Translation model storage cleanup and AALyrics-owned reset actions. PR #50 hosts Settings in the production READY runtime and adds the in-app `License` second-level surface, the shared Phone Markdown renderer, and the adopted Phone popup/subscreen-header standards. PR #58 extends the bundled legal-document path so Settings > License presents the repository `NOTICE` together with the unchanged `LICENSE`. Changelog now follows the same application-owned bundled-document model.

## Product intent

Settings should expose stable user configuration without turning the Phone UI into an owner of application state.

The production Settings surface remains intentionally focused. Advanced contains one debug presentation preference, one explicitly unavailable experimental affordance, one Translation storage-management action, and one app-owned reset action. It does not become a general developer-settings surface. Changelog and License are read-only second-level document surfaces and do not create new runtime policy or networking ownership.

Second-level Settings surfaces use the shared `SettingsSubscreenHeader` rather than implementing their own header. The standard back affordance is the Material rounded chevron-left used by the current Advanced screen: 32dp icon inside a 48dp touch target, followed by the screen title. This intentionally mirrors the chevron-right affordance used to enter `Advanced`. Text-only `Back` actions and alternate arrow shapes are not used for normal second-level Settings navigation. System Back remains behaviorally equivalent.

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

Changelog
└─ repository CHANGELOG.md rendered as compact Markdown

License
└─ repository NOTICE + LICENSE rendered as compact Markdown

Advanced
├─ Debug
│  └─ Verbose details                  [switch]
├─ Experimental features
│  └─ Karaoke mode             [OFF, unavailable]
├─ Storage
│  └─ Clear translation models        ⓘ  Clear
└─ Reset
   └─ Reset AALyrics                  ⓘ  Reset

[branding footer]
AALyrics mark
AALyrics
Version: vX.X.X
© <current year> Yuta Miura (whoxamxl)
```

Provider preferences, appearance/theme selection, log export, and other future taxonomy remain out of scope. The approved Advanced surface remains narrow: Verbose Details controls read-only diagnostic presentation, Karaoke mode remains visible but unavailable and unwired, Storage owns explicit Translation-model cleanup, and Reset restores only AALyrics-owned state.

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
├─ verboseDetailsEnabled
└─ licenseText
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
Translation                        [OFF]
```

Translation is opt-in. When no persisted user choice exists, the application-owned setting defaults to disabled. The row emits the requested enabled state; only an explicit user enable starts Translation work. The UI does not start translation engines directly.

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

The persisted default target is English. English remains immediately selectable because ML Kit English support is treated as built in; keeping that model/capability ready does not imply that Translation itself is enabled.

The target-language row remains available while Translation is disabled. Changing the target while disabled is valid configuration and can be applied when Translation is later enabled.

Each language row also exposes Translation-model readiness through one stable trailing action slot:

- English is selectable from the start because AALyrics treats ML Kit English support as built in and requires no remote language-pack download;
- an unavailable remote model is not selectable and shows an explicit download action;
- an active download is not selectable and shows an indeterminate loading indicator;
- a ready remote model becomes selectable and shows no status icon unless it is the selected target;
- the selected ready/built-in target shows the check mark in the same trailing slot;
- a failed download is not selectable and shows a retry action in the trailing slot.

A successful manual download therefore transitions the row from disabled + download/loading UI to an ordinary selectable row. Downloading a model must not implicitly change the selected target.

Model readiness is reconciled from ML Kit on every application-process start. A debug/update install may preserve both SharedPreferences and ML Kit-downloaded language packs while recreating AALyrics' in-memory lifecycle state, so the picker must not assume that a missing in-memory entry means the model is absent. Non-English targets begin in a short CHECKING presentation state while the ML Kit downloaded-model inventory is restored; downloaded packs then become READY and genuinely absent packs become NOT_DOWNLOADED.

The primary trailing action uses one fixed token-sized slot on every row:

```text
language label (flex) | primary trailing action
```

The primary action is exactly one of download, loading, retry, selected check, or empty. This avoids duplicate downloaded + selected icons and keeps the right edge aligned regardless of language-name length or model state.

Only a failed row adds a failure-info icon immediately before the primary retry slot. Pressing that info icon opens a tooltip containing the presentation-ready failure reason. If no specific reason is available, the UI may show a generic download-failure explanation. Non-failed rows do not show or reserve a visible failure-info action.

The picker should clearly mark the draft selection and remain open when an available target is tapped. Tapping a language changes only the dialog-local draft selection; it does not immediately update the persisted Target language. The picker also remains open when a model download/retry action or failure tooltip is used, and remains usable at narrow widths and enlarged font scales.

The language list is a bounded internal viewport rather than an unbounded dialog body. Its maximum height is 312dp (six and a half 48dp language rows), intentionally revealing part of the next row when overflow exists. Additional languages scroll inside that viewport while the dialog title and `Cancel` / `Done` actions remain fixed and visible. If fewer rows exist, the viewport shrinks to content instead of reserving empty space.

Scrollable overflow is signaled with 24dp animated edge fades inside the language viewport. At the top only the lower fade is visible; during mid-list scrolling both fades may be visible; at the bottom the lower fade disappears. The fades follow `LazyListState.canScrollBackward` / `canScrollForward` and animate in/out over 180ms. They never cover the dialog title or action row.

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

### SettingsActionRow

Represents an operation performed from the current Settings surface rather than navigation to another surface.

Presentation:

```text
action title (flex) | optional info | text action
```

It must not show the navigation chevron. The trailing text action occupies the same 52dp control slot used by a Material 3 Switch so action labels and toggles share the same horizontal center axis. This is the default alignment rule for trailing Settings action buttons; exceptions require an explicit layout reason. The full row may open the same confirmation flow as the trailing action, while the info affordance remains independently actionable.

### SettingInfoTooltip

A reusable Phone-local on-demand explanatory surface.

The current `SettingInfoTooltip` + `PhonePopupMenu` implementation is the standard Phone tooltip treatment. New anchored informational tooltips should reuse it rather than styling a raw Material `DropdownMenu` independently.

It should:

- be anchored to an explicit info affordance;
- remain short;
- dismiss normally;
- avoid permanently consuming vertical space;
- use the same Phone popup surface as Playback Quick Controls: Radius16, `BackgroundSurfaceStrong`, `BorderSoft`, zero tonal elevation, and the same shadow treatment.

The first demonstrated use is Plain lyrics auto-scroll.

## Target-language picker

The first implementation uses a compact Material 3 modal picker because the supported target set is short and finite. This picker is not an anchored tooltip/popup and therefore is not required to use `PhonePopupMenu`.

Requirements:

- list only the presentation-provided options;
- initialize the dialog-local draft selection from the currently persisted Target language;
- visibly move the selected/check state immediately when another built-in/ready language is tapped;
- do not persist that draft selection until the user presses `Done`;
- show presentation-provided model readiness for every language;
- allow draft selection only for built-in/ready languages;
- expose a failure-reason tooltip only for failed models;
- emit a separate manual model-download/retry request without changing the draft selection;
- provide explicit `Cancel` and `Done` text actions;
- treat `Cancel`, system Back, and outside-dialog dismissal identically: discard the draft and keep the persisted Target language unchanged;
- `Done` commits the draft Target language and then closes the picker;
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

A `Changelog >` internal navigation row sits directly below the Version/update block.

Opening it presents an in-app second-level Settings surface using the standard `SettingsSubscreenHeader`. The changelog is vertically scrollable and selectable and is rendered through the shared `PhoneMarkdownText` wrapper.

The repository-root `CHANGELOG.md` file is the single source of truth for the user-facing release history. The app build automatically copies that file into generated app assets as `aalyrics_changelog.md`; `:app` reads the bundled asset and supplies its exact Markdown text as `SettingsScreenUiState.changelogText`.

Therefore:

- debug and release builds display the changelog checked into the exact source revision they were built from;
- Changelog works offline and does not fetch GitHub Releases at runtime;
- `:ui:phone` does not read Android assets directly;
- there is no Changelog loading/failure/retry lifecycle or network callback;
- changing `CHANGELOG.md` requires no Phone UI code update;
- the Settings Changelog row is normal internal navigation, matching License and Advanced.

Release entries are maintained newest-first. Before a release tag is created, the tagged version must be added as the newest version heading in `CHANGELOG.md`. The release workflow verifies that relationship before building/publishing the signed APK. See `docs/RELEASES.md`.

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

Opening it presents an in-app second-level Settings surface using the standard `SettingsSubscreenHeader`, matching the navigation model used by `Advanced` and Changelog. The legal text is vertically scrollable and selectable.

The repository-root `NOTICE` and `LICENSE` files remain the legal-content sources of truth. The app build copies them separately into generated assets as `aalyrics_notice.txt` and `aalyrics_license.txt`. `:app` reads and exposes them separately as presentation data; it does not concatenate or rewrite either source.

`LicenseSettingsScreen` presents the two sources deliberately:

- a compact `REQUIRED NOTICE` section shows `AALyrics` and a human-readable form of the required notice;
- the display removes only the mechanical `Required Notice:` prefix while preserving the notice content itself;
- the exact original `Required Notice:` line remains unchanged in the bundled `NOTICE` asset;
- a separate `LICENSE TERMS` section renders the untouched repository `LICENSE` through the shared Phone-local `PhoneMarkdownText` wrapper.

The current required notice is `Required Notice: © 2026 Yuta Miura`. The `©` symbol is part of the canonical repository notice rather than a UI-only substitution.

Markdown parsing/rendering for the license terms is delegated to `mikepenz/multiplatform-markdown-renderer` (Material 3 integration), currently pinned to `0.38.1` for compatibility with the app's Java 17 / compileSdk 36 baseline. AALyrics does not maintain its own Markdown grammar.

`PhoneMarkdownText` is shared by License terms and Changelog so bundled Markdown documents do not evolve separate Markdown implementations.

The wrapper applies a compact AALyrics Phone Markdown theme instead of the renderer's default Material display typography. Current baseline: H1 24sp/30sp, H2 20sp/26sp, body 14sp/20sp, inline/code text 13sp/18sp, compact block spacing, and AALyrics cyan underlined links. This keeps long technical documents readable on narrow phones without changing their Markdown sources.

Therefore:

- debug and release builds display the legal sources checked into the source revision they were built from;
- changing `NOTICE` or `LICENSE` requires no Phone UI code update;
- no network connection or GitHub fetch is required to read the legal text;
- the Settings License row is internal navigation, not an external browser link;
- `:ui:phone` does not read Android assets directly; asset ownership remains in `:app`.

The current repository license is **PolyForm Noncommercial License 1.0.0**, but the UI derives its displayed body from the bundled source files rather than assuming that text remains unchanged.

The notice copyright year is intentionally **source-controlled**, not calculated from the device clock. It records the notice authored for the software rather than acting as a current-year label. If the project later adopts a year range, update the repository `NOTICE` explicitly. This is separate from the Settings branding footer below, whose display year is runtime-derived.

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

### Storage — Clear translation models

`Clear translation models` is an explicit storage-management action. Its row remains compact and uses the shared info tooltip rather than permanent subtitle text.

The tooltip explains that the action:

- removes downloaded ML Kit translation models;
- keeps English available because English is the built-in/default capability and is not a downloadable model;
- turns Translation OFF;
- restores Target language to English.

The row is an action row, not navigation: it uses a trailing `Clear` text action instead of a chevron. The row title and trailing action use the normal Settings colors; only the confirmation dialog's `Clear` action is destructive. The full row opens the same confirmation surface, and the action always requires a confirmation dialog before execution.

The application boundary first restores Translation settings to their safe defaults, then asks the Translation model manager to delete engine-managed downloaded models. The Phone UI does not call ML Kit directly.

Cleanup must also account for model preparation already in flight when the action is confirmed, including the short pre-monitor window while ML Kit availability is still being checked. Preparation registration and cleanup start share one lifecycle barrier/generation so either the preparation is captured by cleanup or the stale preparation is suppressed before it can start a new download. A model that finishes downloading after cleanup starts must be deleted rather than silently surviving the storage action.

The confirmed cleanup operation is application-owned and runs in the process-level application scope, not a Composable-owned coroutine scope. Leaving Advanced, switching tabs, or recreating the Activity must not cancel a confirmed cleanup. The application exposes a small cleanup lifecycle to Phone presentation; immediate enumeration/deletion failures become a persistent FAILED presentation state and Advanced shows an explicit Retry/Close dialog when it is visible again.

### Reset — Reset AALyrics

`Reset AALyrics` is the final Advanced section and uses a trailing `Reset` text action rather than a navigation chevron. The row title stays in the normal primary text color; the trailing `Reset` action and the confirmation action use the destructive color while the normal section/card treatment remains consistent with the rest of Settings.

The info tooltip explains that reset restores AALyrics-owned settings and onboarding state while leaving external/system-owned state untouched.

After confirmation, reset restores:

- Translation -> OFF;
- Target language -> English;
- Verbose details -> OFF;
- Plain lyrics auto-scroll -> its Phone default;
- Android Auto compatibility acknowledgement -> Not reviewed.

Reset does **not**:

- delete downloaded translation models;
- revoke Notification Access or other Android permissions;
- change Android Auto Developer Mode / Unknown sources;
- modify any other application's state.

After the reset, the entry-state owner immediately re-evaluates onboarding. Existing Android permissions are respected, while the Android Auto compatibility acknowledgement is presented again because that AALyrics-owned acknowledgement has returned to `Not reviewed`.

#### Reset maintenance rule

`Reset AALyrics` is a maintained product contract, not a one-time list of keys. Any PR that introduces or changes app-owned persisted state, onboarding acknowledgement, durable preference, or long-lived Phone setting must explicitly re-evaluate the reset contract in the same change.

For every new state item, the implementing PR must do one of the following:

- add it to the explicit AALyrics reset path and cover the default/reset behavior with a test; or
- document why the state intentionally survives `Reset AALyrics`.

Do not use a blanket SharedPreferences/DataStore clear as a shortcut. External/system-owned state remains outside the reset boundary. Newly introduced caches, downloaded assets, or model files must also make an explicit keep/delete decision; the default is to preserve them unless a dedicated storage action or an explicit product decision says otherwise.

When the reset scope changes, update this document, the reset tooltip/dialog copy when user-visible semantics changed, relevant Preview fixtures, and reset tests in the same PR.

Both Storage and Reset explanations use the shared `SettingInfoTooltip`; explanatory subtitle text is not permanently rendered in the rows.

### Advanced navigation ownership

The Advanced surface remains Settings-owned UI. It does not become a fifth primary destination.

Reselecting the already-selected Settings bottom-navigation tab is a Settings-root reset. It dismisses any active Settings modal, discards uncommitted dialog-local draft state such as a Target-language selection, leaves Advanced, Changelog, or License, returns to the main Settings surface, and scrolls the Settings home content back to the top. This is the Settings implementation of the shared Phone primary-tab reselection contract; it is not a Settings-specific navigation exception.

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
- target-language picker open with bounded scroll viewport / overflow edge fade;
- Android Auto `Enabled`;
- Android Auto `Skipped`;
- Android Auto `Not reviewed`;
- narrow width;
- enlarged font;
- app update checking;
- app up-to-date state;
- app update available state;
- app update failure/retry state;
- Changelog screen at typical, narrow, and enlarged-font configurations;
- branding footer;
- Advanced navigation row;
- Advanced screen with Verbose details OFF and ON;
- disabled Karaoke mode row;
- Advanced Storage and Reset rows;
- Clear translation models confirmation;
- Clear translation models failure/retry;
- Reset AALyrics confirmation;
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

The host does not wire active no-op callbacks for unfinished Settings capabilities. Update remains an explicit `UNAVAILABLE` presentation state until its release-network runtime is implemented. Changelog is functional without release-network wiring: the application supplies the bundled repository `CHANGELOG.md` as presentation text.

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
- functional Karaoke mode or any Karaoke runtime wiring;
- additional developer/experimental controls beyond the approved Advanced contract.
