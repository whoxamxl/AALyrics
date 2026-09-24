# Phone Settings Specification

## Status

This document defines the production presentation contract for the Phone `Settings` destination.

The production `SettingsScreen` and its Phone-local row components are implemented as a presentation-only destination: `:ui:phone` receives immutable state and emits callbacks. Application/capability layers continue to own persistence and runtime policy.

The first Settings surface was integrated into `main` via PR #44 and polished in PR #45. PR #49 established the second-level `Advanced` surface with the functional `Verbose details` preference and disabled future `Karaoke mode` affordance. The current Advanced contract also includes explicit Translation model storage cleanup and AALyrics-owned reset actions. PR #50 hosts Settings in the production READY runtime and adds the in-app `License` second-level surface, the shared Phone Markdown renderer, and the adopted Phone popup/subscreen-header standards. PR #58 extends the bundled legal-document path so Settings > License presents the repository `NOTICE` together with the unchanged `LICENSE`. Changelog now follows the same application-owned bundled-document model.

PR #60 integrated the lower Settings information architecture into `main`: `APP`, `ABOUT & SUPPORT`, a standalone `Advanced` card, the in-app Privacy Policy, and the native `Support AALyrics` surface. The current `feature/settings-legal-help` slice extends that merged baseline with Terms of Use, inline third-party license notices within License, and a distinct Help & Feedback routing hub.

## Product intent

Settings should expose stable user configuration without turning the Phone UI into an owner of application state.

The production Settings surface remains intentionally focused. Lyrics owns the user-facing playback-source eligibility toggle alongside Plain auto-scroll. Advanced contains the narrow unclassified-source override, one debug presentation preference, one explicitly unavailable experimental affordance, one Translation storage-management action, and one app-owned reset action. It does not become a general developer-settings surface. Changelog, Privacy Policy, Terms of Use, and License are read-only bundled-document surfaces and do not create networking ownership; License includes the bundled third-party license notices inline as its third section. `Help & Feedback` is a native routing hub for end-user help and feedback destinations. `Support AALyrics` remains a separate voluntary project-support surface; payment interaction remains entirely outside AALyrics.

Settings subscreens use the shared `SettingsSubscreenHeader` rather than implementing their own header. The standard back affordance is the Material rounded chevron-left used by the current Advanced screen: 32dp icon inside a 48dp touch target, followed by the screen title. This intentionally mirrors the chevron-right affordance used to enter `Advanced`. Text-only `Back` actions and alternate arrow shapes are not used for normal Settings hierarchy navigation. Legal content remains at one Settings depth: License renders its required notice, AALyrics license terms, and third-party licenses inline on one scrollable screen.

Initial structure:

```text
Settings
├─ Lyrics
│  ├─ Plain lyrics auto-scroll       [switch]  ⓘ
│  └─ Ignore non-audio apps          [switch]  ⓘ
├─ Translation
│  ├─ Translation                    [switch]
│  └─ Target language                <value>  >
├─ Android Auto
│  └─ Compatibility setup            <status> >
├─ App
│  ├─ Automatically check for updates [switch]  ⓘ
│  ├─ Version / update
│  │  ├─ Version                    <version>
│  │  └─ <stateful update action>
│  ├─ Changelog                               >
│  └─ Source code                 GitHub       ↗
├─ About & Support
│  ├─ Privacy Policy                          >
│  ├─ Terms of Use                            >
│  ├─ License                                 >
│  ├─ Help & Feedback                         >
│  └─ Support AALyrics                        >
└─ Advanced                                  >

Changelog
└─ repository CHANGELOG.md rendered as compact Markdown

Privacy Policy
└─ repository PRIVACY.md rendered as compact Markdown

Terms of Use
└─ repository TERMS_OF_USE.md rendered as compact Markdown

License
├─ REQUIRED NOTICE
│  └─ repository NOTICE
├─ LICENSE TERMS
│  └─ repository LICENSE rendered as compact Markdown
└─ THIRD-PARTY LICENSES
   └─ repository THIRD_PARTY_LICENSES.md rendered as compact Markdown

Help & Feedback
├─ Report a bug                               ↗
├─ Ask a question                             ↗
├─ Suggest an idea                            ↗
├─ General discussion                         ↗
└─ Report a security issue                    ↗

Support AALyrics
├─ native AALyrics explanation
└─ Support on Buy Me a Coffee                 ↗
   └─ external Custom Tab / browser handoff

Advanced
├─ Playback source
│  └─ Allow unclassified apps           [switch]  ⓘ
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
Provider preferences, appearance/theme selection, log export, and other future taxonomy remain out of scope. The approved Advanced surface remains narrow: Playback source owns only the unclassified-app escape hatch, Verbose Details controls read-only diagnostic presentation, Karaoke mode remains visible but unavailable and unwired, Storage owns explicit Translation-model cleanup, and Reset restores only AALyrics-owned state.

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
├─ ignoreNonAudioApps
├─ allowUnclassifiedApps
├─ translationEnabled
├─ translationTarget
├─ translationTargetOptions
├─ androidAutoCompatibilityStatus
├─ verboseDetailsEnabled
├─ noticeText
├─ licenseText
├─ changelogText
├─ privacyPolicyText
├─ termsOfUseText
└─ thirdPartyLicensesText
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

### Ignore non-audio apps

Presentation:

```text
Ignore non-audio apps            ⓘ   [ON]
```

This is an application-owned persisted preference and defaults to **ON**. It controls whether a selected playback source must be verified as an Android audio application before AALyrics starts lyrics-provider lookup.

Approved tooltip copy:

> Only apps Android identifies as audio apps are used for lyrics lookup. This prevents unnecessary searches from games, browsers, social apps, and other media sources. Unclassified apps are blocked unless allowed in Advanced settings.

Behavior:

- OFF -> do not apply category-based lyrics eligibility filtering;
- ON + `CATEGORY_AUDIO` -> allow lyrics lookup;
- ON + known non-audio category -> block lookup with `Unavailable(NON_AUDIO_APP)`;
- ON + `CATEGORY_UNDEFINED`, unknown/future category normalized to Undefined, or unresolved `ApplicationInfo` -> defer to `Advanced > Playback source > Allow unclassified apps`.

This setting gates whether lyrics lookup begins. It does not change MediaSession discovery/selection, playback transport, source-app launching, or provider ranking.

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

The `APP` section exposes app/distribution information without moving release-network behavior into `:ui:phone`. It contains Version/update, Changelog, and Source code. Legal/support entries live in the separate `ABOUT & SUPPORT` section below.

### Version and update

The APP section keeps one Version/update row. In #75, **release discovery** is narrow: Settings owns MANUAL Checking / Up to date / Check failed, while MANUAL and AUTOMATIC newer-release results use the same global release dialog. The already-validated post-Update download/install process presentation remains in Settings until #76.

The installed version is supplied from `BuildConfig.VERSION_NAME` and rendered through the shared `VersionChip`.

Approved Settings presentation:

```text
IDLE
Version                     [ v0.2.0-alpha.1 ]
                                   Check for updates

CHECKING
Checking for updates…                              ◌

UP_TO_DATE
Up to date                                         ✓

CHECK_FAILED
Update check failed                        ⓘ   ↻ Retry
```

These are the only **discovery** states Settings should present. They become the only update-related Settings states overall after the #76 process-presentation migration.

Manual discovery behavior:

1. `Check for updates` or `Retry` starts a `MANUAL` release query.
2. While the query is active, Settings shows `Checking for updates…`.
3. If no newer eligible release exists, Settings shows `Up to date`.
4. If the query fails, Settings shows `Update check failed` with Retry.
5. If a newer eligible release exists, Settings does **not** show `Update available` or a Download action; it immediately hands the release to the global release-available dialog.
6. If a MANUAL check is requested while an AUTOMATIC query is already in flight, the existing query is promoted to MANUAL presentation semantics. Settings immediately shows `Checking for updates…`, no duplicate release request is started, and the eventual Up to date / Check failed / newer-release result is treated as MANUAL.

#75 still presents the existing post-Update process states in Settings. After the #76 migration, Settings must not present any of the following:

```text
Update available
Preparing download
Downloading / percent / progress bar
Downloaded / Ready to install
Install
Preparing installation
Installation permission required
Installing update
Download failed
Install failed
```

Those states remain application-owned runtime facts, but their user-facing presentation belongs to the Unified Update Dialog.

This means the existing determinate and indeterminate progress behavior is **moved, not removed**. The byte-based download percentage, preparation progress, retained verified APK, typed failures, Retry behavior, install-time latest-release refresh, package/version/signing preflight, source-trust handling, PackageInstaller handoff, and Android confirmation all remain part of the update runtime.

#### Manual and automatic discovery convergence

The durable `Automatically check for updates` switch remains above the Version/update row. It defaults to ON and uses the shared `SettingInfoTooltip`.

Manual and automatic discovery differ only before a newer release is found:

```text
MANUAL
  Checking       -> Settings
  Up to date     -> Settings
  Check failed   -> Settings / Retry
  Update found   -> Unified Update Dialog

AUTOMATIC
  Checking       -> silent
  Up to date     -> silent
  Failed         -> silent
  Update found   -> Unified Update Dialog
```

Both origins use the same release grammar, installed-channel eligibility rules, and comparison logic. The internal `UpdateCheckOrigin` remains necessary for cadence and notification-suppression policy, but it must not select a different update process.

A successful manual GitHub Releases query refreshes the same durable 7-day automatic cadence timestamp. Automatic completion does not shift the timestamp already recorded at automatic-check start, and install-time refresh does not alter discovery cadence.

Turning automatic checking OFF suppresses automatic release notification even if an already-started automatic query completes later. It never disables manual `Check for updates`.

#### Unified Update Dialog

A newer release found by either MANUAL or AUTOMATIC discovery opens the same global dialog:

```text
UPDATE AVAILABLE

New release available

AALyrics [ v0.3.0-alpha.1 ] is ready to download.

Update
Not now
```

The version is rendered with the shared `VersionChip` inline with the supporting copy. The dialog uses `PhoneDialogHeader` for the standard trailing close affordance. Before Update begins, `Not now`, close, and system Back dismiss the available-state dialog; outside-tap dismissal remains disabled.

Automatic dismissal suppression is notification-specific. Dismissing an automatically discovered version suppresses that exact automatic prompt for the current app-process session. An explicit manual `Check for updates` may still present that same current release because the user has deliberately requested discovery.

Durable `SuccessfulUpdate` feedback retains modal priority and is not merged into this pre-install dialog lifecycle.

#### Update-process presentation ownership

**#75 current:** after the user chooses `Update`, the release-available dialog closes and the existing Settings-owned download/install process presentation continues unchanged.

**#76 target:** the Unified Update Dialog becomes the presentation owner for that same update process, and Settings stops mirroring process states.

#76 deliberately preserves the validated **two-stage user action** while moving its presentation into the dialog:

```text
Update
  -> PREPARING_DOWNLOAD
  -> DOWNLOADING
  -> VERIFYING / PREPARING
  -> DOWNLOADED / Ready to install
  -> Install
  -> PREPARING_INSTALL
  -> INSTALL_PERMISSION_REQUIRED if needed
  -> INSTALLING / Android confirmation
```

Target #76 presentation states include:

```text
PREPARING_DOWNLOAD
[indeterminate progress]

DOWNLOADING
[determinate 0–100% progress + percentage]

VERIFYING / PREPARING
[indeterminate progress]

DOWNLOADED / READY_TO_INSTALL
[verified version + explicit Install action]

PREPARING_INSTALL
[indeterminate progress]

INSTALL_PERMISSION_REQUIRED
[permission explanation / Grant permission / GitHub fallback]

INSTALLING
[handoff / waiting presentation]

DOWNLOAD_FAILED / INSTALL_FAILED
[typed explanation + Retry]
```

The existing Settings-row progress behavior should be transplanted into this dialog. GitHub Release asset size remains the expected total, downloaded bytes remain the determinate-progress numerator, and size mismatch / SHA-256 mismatch continue to fail closed.

`DOWNLOADED` remains an authoritative application-owned safety and recovery boundary. In #76 it is also a visible `Ready to install` checkpoint with an explicit user-facing `Install` action. #76 must not automatically continue into installation merely because verification completed.

When Android install-source trust is missing, the update flow explains the requirement without bypassing Android Settings. Granting permission still hands off to Android's per-app source-trust Settings and re-checks `PackageManager.canRequestPackageInstalls()` on return. Android continues to own final package-install confirmation.

If install-time latest-release refresh discovers a newer eligible release, the active dialog returns to the release-available state for that newer version. It must not send the user back to a Settings `Update available` row.

A valid verified APK remains reusable across permission handling and recoverable retry. Presentation dismissal must not silently destroy the runtime's authoritative operation/artifact state.

#### #77 one-step orchestration

Only after the #76 two-stage route has passed focused tests and full real-device E2E validation may a later PR remove the second user-facing Install action.

#77 does not replace the validated core stages. It keeps:

- `downloadUpdate()` as the download/verify operation;
- `DOWNLOADED` as the verified-artifact and recovery boundary;
- `installUpdate()` as the existing install-refresh/preflight/permission/PackageInstaller operation.

The one-step UX adds orchestration above those stages: an Update operation carrying one-step continuation intent may automatically call the existing install stage after verified `DOWNLOADED`. The coordinator must prevent duplicate continuation across recomposition, Activity recreation, process recovery, and permission return.

Production UX may therefore become one-step in #77 while the independently validated two-stage route remains preserved in runtime boundaries, tests, and recovery behavior.

#### Version presentation

Semantic AALyrics application/release versions use the shared `VersionChip` presentation defined in `docs/PHONE_UI_SPEC.md`.

In Settings, the installed Version row renders the chip as standalone metadata. In update dialogs, when a version is grammatically part of supporting copy, the same chip participates in the sentence rather than creating an extra metadata row.

The dedicated VersionChip Preview matrix remains the visual baseline for DEV / ALPHA / BETA / RC / STABLE channel treatment.

#### Preview contract

After the migration, Settings Previews should cover:

- automatic-check preference ON and OFF;
- manual discovery Idle;
- manual Checking;
- manual Up to date;
- manual Check failed / Retry.

Update-process Previews belong to the Unified Update Dialog and should cover representative release-available, preparing, downloading/progress, verification, Ready to install / explicit Install, permission-required, failure/retry, narrow-phone, and enlarged-font configurations. #76 Preview coverage must demonstrate the two-stage route; #77 may later change only the normal user-facing continuation behavior.

The Phone UI remains presentation-only. It emits semantic discovery/update actions and renders application-owned state; it does not perform GitHub HTTP requests, file I/O, checksum verification, package inspection, signing checks, Android settings mutation, or PackageInstaller session work directly.

Do not use publication timestamp alone as version ordering. Stable installed builds consider stable releases only. Alpha/beta/RC builds consider prerelease and stable releases. Development builds inherit the channel and comparison base embedded in their generated version name.

A development version such as:

```text
0.2.0-alpha.1-dev+abcdef0
0.2.0-alpha.1-dev+abcdef0.dirty
```

compares as `0.2.0-alpha.1` for update discovery. Build identity metadata does not make the corresponding published `0.2.0-alpha.1` release an update.

Within the same numeric version, release precedence is:

```text
alpha.N < beta.N < rc.N < stable
```

The version parser/comparator is Android-independent and directly unit-testable. The GitHub client and check orchestration remain application-owned; `:ui:phone` remains presentation-only.

The check path is public and unauthenticated. Do not embed a GitHub token or repository secret in AALyrics.

### Update state lifetime

Manual discovery presentation is Settings-visit scoped, while update-process state is application/process owned.

On entering Settings, only the manual-discovery presentation is relevant:

```text
MANUAL CHECKING      -> keep if the user-started query is still active
MANUAL UP_TO_DATE    -> visit-local
MANUAL CHECK_FAILED  -> visit-local
IDLE                 -> ordinary Version / Check for updates presentation
```

A newer-release result leaves Settings presentation ownership immediately and is represented by the Unified Update Dialog instead. Active download/install work, retained verified artifacts, permission-required state, and install handoff are not reset merely because the user changes primary destination or Settings visit.

Automatic Checking / Up to date / Failed never become Settings presentation states.

Configuration changes, Activity recreation, recomposition, and destination changes must not duplicate or restart application-owned update work. Only one check, download, or install preparation/session handoff may be active at a time.

Partial APK bytes live only in app-private cache storage. A SHA-256-verified APK is promoted into app-private no-backup persistent storage and becomes the source of truth for `DOWNLOADED`. On process restart, the runtime removes transient staging/promotion files and restores `DOWNLOADED` only when the retained APK has a canonical AALyrics release filename, remains eligible for the installed update channel, and is still newer than the installed version. Stable installed builds therefore do not restore a retained prerelease APK. Once the installed app reaches or passes that retained release, or the retained release is no longer channel-eligible, the stale verified APK is deleted and update state returns to `IDLE`.

Immediately before a PackageInstaller session is committed, application-owned update recovery persistence records `PendingUpdate(targetVersion, targetVersionCode, installerSessionId, resumeAfterUpdate=true)`. The versionCode is the value already validated from APK package metadata during install preflight. Persistence is synchronous and happens only after the APK has been written/fsynced into the session. The marker stores the exact PackageInstaller session ID. If that durable write fails, the installer session is not committed. The old binary does not clear the marker on PackageInstaller success. A terminal installer failure clears only a pending marker whose session ID matches the failed session, even if the original process-local status sink is gone; startup cleanup does the same after successfully abandoning an owned unsealed session.

After Android replaces the package, a manifest-registered non-exported `ACTION_MY_PACKAGE_REPLACED` receiver compares the pending target with the version running in the new binary. Exact target versionCode/versionName matches reconcile as success, and any strictly newer installed versionCode also satisfies the pending target. Older or same-code/name-mismatched replacements do not consume the pending marker. Successful reconciliation synchronously promotes the pending marker to durable `SuccessfulUpdate(installedVersion, installedVersionCode, resumeAfterUpdate)` state. Promotion itself has no presentation side effect; only after successful reconciliation may the receiver make the separate best-effort `MainActivity` resume request described below.

The next valid Phone entry keeps the ordinary startup destination (`PhoneDestination.Home`, which aliases Lyrics) and overlays the update-success dialog. There is no update-specific destination override. The dialog shows the actual installed version from `SuccessfulUpdate`, not an inferred target label, and presents it inline with the support copy as `You're now running [version].` rather than as a separate row. Its `UPDATE COMPLETE` eyebrow/X header uses the same shared `PhoneDialogHeader` geometry as the install-permission dialog; dialog-specific code must not reposition or resize the close affordance. Done, the close button, and system Back share the same dismissal path. Outside-tap dismissal is disabled. The durable success marker is cleared before the dialog is removed from process state; if that clear fails, the dialog remains visible. Process death before dismissal therefore preserves the marker and causes the dialog to reappear at the next valid Phone entry.

After successful replacement reconciliation, `resumeAfterUpdate=true` causes one best-effort explicit request to open `MainActivity`. A false resume flag, missing pending state, or an unreconciled replacement causes no request. The launch uses new-task/clear-top/single-top semantics but does not alter the Phone destination model. Android may reject or suppress background Activity launch; the request result is never used as update-success evidence and never clears `SuccessfulUpdate`. If the request does not bring AALyrics forward, the next ordinary launch still presents the durable success dialog. Notification fallback is not part of this checkpoint.

`Reset AALyrics` cancels app-owned update/install preparation, abandons any PackageInstaller session still under AALyrics control when practical, deletes transient and verified update artifacts, clears the selected release and all app-owned update recovery markers, and restores presentation to `IDLE`. Reset does not revoke Android's per-source install trust and does not undo a package already installed by Android.

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
- the Settings Changelog row is normal internal navigation, matching Privacy Policy, License, Support AALyrics, and Advanced.

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

## About & Support section

`ABOUT & SUPPORT` groups policy/legal information, help routing, and the voluntary project-support entry. It is deliberately separate from `APP`: Version/update, Changelog, and Source code remain application/distribution information. The final row order is Privacy Policy, Terms of Use, License, Help & Feedback, then Support AALyrics. `Help & Feedback` and `Support AALyrics` remain distinct because one routes users seeking help or providing feedback while the other is voluntary funding for continued development.

### Privacy Policy

A `Privacy Policy >` internal navigation row is the first entry in `ABOUT & SUPPORT`.

Opening it presents an in-app second-level Settings surface using the standard `SettingsSubscreenHeader`. The policy is vertically scrollable and selectable and is rendered through the shared `PhoneMarkdownText` wrapper.

The repository-root `PRIVACY.md` file is the canonical privacy-policy source for AALyrics. The implementation follows the existing bundled-document ownership model:

```text
repository PRIVACY.md
    -> app build copies generated asset
    -> :app reads bundled text
    -> SettingsScreenUiState.privacyPolicyText
    -> PrivacyPolicySettingsScreen
    -> PhoneMarkdownText
```

The bundled policy must represent the exact source revision used to build the installed APK. Reading the policy must work offline and must not fetch GitHub or any remote policy page at runtime. `:ui:phone` receives presentation-ready Markdown text and does not read Android assets directly.

The policy itself must be written from the actual AALyrics data-flow/privacy behavior before implementation is declared complete; do not publish placeholder claims about collection, retention, providers, or external services.

### Terms of Use

A `Terms of Use >` internal navigation row sits directly below Privacy Policy.

Opening it presents an in-app second-level Settings surface using the standard `SettingsSubscreenHeader`. The document is vertically scrollable and selectable and is rendered through the shared `PhoneMarkdownText` wrapper.

The repository-root `TERMS_OF_USE.md` file is the canonical source. It follows the same application-owned bundled-document path as Privacy Policy and Changelog:

```text
repository TERMS_OF_USE.md
    -> app build copies generated asset
    -> :app reads bundled text
    -> SettingsScreenUiState.termsOfUseText
    -> TermsOfUseSettingsScreen
    -> PhoneMarkdownText
```

The installed app displays the Terms checked into the exact source revision used for that build. Reading the Terms must work offline and must not fetch GitHub at runtime.

### License

A `License >` internal navigation row sits directly below Terms of Use.

Opening it presents an in-app second-level Settings surface using the standard `SettingsSubscreenHeader`, matching the navigation model used by `Advanced` and Changelog. The legal text is vertically scrollable and selectable.

The repository-root `NOTICE` and `LICENSE` files remain the legal-content sources of truth. The app build copies them separately into generated assets as `aalyrics_notice.txt` and `aalyrics_license.txt`. `:app` reads and exposes them separately as presentation data; it does not concatenate or rewrite either source.

`LicenseSettingsScreen` presents the two sources deliberately:

- a compact `REQUIRED NOTICE` section shows `AALyrics` and a human-readable form of the required notice;
- the display removes only the mechanical `Required Notice:` prefix while preserving the notice content itself;
- the exact original `Required Notice:` line remains unchanged in the bundled `NOTICE` asset;
- a separate `LICENSE TERMS` section renders the untouched repository `LICENSE` through the shared Phone-local `PhoneMarkdownText` wrapper;
- a dedicated `THIRD-PARTY LICENSES` section renders the bundled third-party OSS notices inline below AALyrics' own license terms, preserving visual separation without adding another navigation level.

The current required notice is `Required Notice: © 2026 Yuta Miura`. The `©` symbol is part of the canonical repository notice rather than a UI-only substitution.

Markdown parsing/rendering for the license terms is delegated to `mikepenz/multiplatform-markdown-renderer` (Material 3 integration), currently pinned to `0.38.1` for compatibility with the app's Java 17 / compileSdk 36 baseline. AALyrics does not maintain its own Markdown grammar.

`PhoneMarkdownText` is shared by License terms, Changelog, Privacy Policy, Terms of Use, and Third-party licenses so bundled Markdown documents do not evolve separate Markdown implementations.

The wrapper applies a compact AALyrics Phone Markdown theme instead of the renderer's default Material display typography. Current baseline: H1 24sp/30sp, H2 20sp/26sp, body 14sp/20sp, inline/code text 13sp/18sp, compact block spacing, and AALyrics cyan underlined links. This keeps long technical documents readable on narrow phones without changing their Markdown sources.

The repository-root `THIRD_PARTY_LICENSES.md` file is likewise copied into the generated app assets and exposed as `SettingsScreenUiState.thirdPartyLicensesText`. `LicenseSettingsScreen` renders it directly in the `THIRD-PARTY LICENSES` section through the shared Markdown renderer.

There is no third-level legal navigation. Header Back and System Back from License return directly to Settings home, while Settings-tab reselection retains the same root-reset behavior.

Therefore:

- debug and release builds display the legal sources checked into the source revision they were built from;
- changing `NOTICE`, `LICENSE`, or `THIRD_PARTY_LICENSES.md` requires no Phone UI code update;
- no network connection or GitHub fetch is required to read the legal text;
- the Settings License row is internal navigation, not an external browser link;
- `:ui:phone` does not read Android assets directly; asset ownership remains in `:app`.

The current repository license is **PolyForm Noncommercial License 1.0.0**, but the UI derives its displayed body from the bundled source files rather than assuming that text remains unchanged.

The notice copyright year is intentionally **source-controlled**, not calculated from the device clock. It records the notice authored for the software rather than acting as a current-year label. If the project later adopts a year range, update the repository `NOTICE` explicitly. This is separate from the Settings branding footer below, whose display year is runtime-derived.

### Help & Feedback

A `Help & Feedback >` internal navigation row sits directly below License.

Opening it presents a native second-level routing surface using `SettingsSubscreenHeader`. It is intentionally not a Markdown rendering of repository `SUPPORT.md`; that file remains the canonical GitHub-facing support policy while the app exposes only end-user-relevant destinations.

Approved routes:

```text
Report a bug                 -> GitHub Issues
Ask a question               -> Discussions / Q&A
Suggest an idea              -> Discussions / Ideas
General discussion           -> Discussions / General
Report a security issue      -> GitHub private vulnerability reporting
```

Each route is presented as an external-link row. `:ui:phone` emits a semantic `HelpFeedbackDestination` callback only; `:app` maps that action to the repository URL and owns Custom Tabs / browser fallback.

Current destinations are:

- Report a bug -> `https://github.com/whoxamxl/AALyrics/issues/new`
- Ask a question -> `https://github.com/whoxamxl/AALyrics/discussions/categories/q-a`
- Suggest an idea -> `https://github.com/whoxamxl/AALyrics/discussions/categories/ideas`
- General discussion -> `https://github.com/whoxamxl/AALyrics/discussions/categories/general`
- Report a security issue -> `https://github.com/whoxamxl/AALyrics/security/advisories/new`

Security vulnerabilities are therefore routed to GitHub private vulnerability reporting, never a public Issue or Discussion.

### Support AALyrics

A `Support AALyrics >` internal navigation row sits directly below Help & Feedback.

Opening it presents a native second-level Settings surface using `SettingsSubscreenHeader`. The surface may explain that AALyrics is free to use and that voluntary support helps continued development, but it must remain concise and non-coercive.

The support surface is a compact landing card rather than a generic Settings row. It uses the user-approved Buy Me a Coffee SVG artwork as the single CTA, converted to an Android VectorDrawable so the artwork remains crisp across phone densities. The content order is: concise native AALyrics explanation, muted optional/external-payment note, 32dp breathing room, the centered animated Buy Me a Coffee CTA, then a small `Opens Buy Me a Coffee` affordance label. QR assets are intentionally not used on the in-app phone surface.

The CTA animation is native Compose presentation over the static vector artwork: a subtle pulse scales from 0.98 to 1.02 and back, while a diagonal shimmer sweeps across the clipped button surface. There is intentionally no tilt or positional float. This removes GIF decoding, raster scaling, frame/canvas cropping, and platform-specific animated-drawable fallback behavior from the Phone UI. Tapping the CTA hands off to the configured Buy Me a Coffee page in a secure browser surface, preferably Android Custom Tabs with ordinary external-browser fallback where necessary. The application/runtime boundary owns launching that external destination; `:ui:phone` emits a support-link callback and must not own Android intents, Custom Tabs, or browser APIs.

The canonical support account must stay aligned with repository `.github/FUNDING.yml` (currently Buy Me a Coffee account `whoxamxi`).

AALyrics must not:

- embed the Buy Me a Coffee checkout in a WebView;
- collect or proxy card/payment credentials;
- implement payment confirmation or transaction state;
- expose amount/message inputs that cannot be passed through a documented, supported prefill contract;
- unlock features, content, badges, entitlements, or runtime behavior because a user supports the project.

Payment amount, optional message, authentication, and payment completion remain owned by Buy Me a Coffee and its payment providers after the external handoff.

## Advanced

The main Settings list exposes one internal navigation row:

```text
Advanced                                      >
```

Opening it presents a second-level Settings surface rather than adding another primary bottom-navigation destination.

Initial structure:

```text
Advanced

Playback source
Allow unclassified apps                 [OFF]  ⓘ

Debug
Verbose details                         [OFF]

Experimental features
Karaoke mode                            [OFF]
                                        Not available yet
```

### Playback source — Allow unclassified apps

`Allow unclassified apps` is an application-owned persisted escape hatch for valid music players Android cannot classify as Audio. It defaults to **OFF**.

Approved tooltip copy:

> Allows lyrics lookup when Android cannot identify a playback app as an audio app. Enable this if a valid music player appears as Unavailable. Only applies while Ignore non-audio apps is enabled.

Behavior while `Ignore non-audio apps` is ON:

- ON -> allow `CATEGORY_UNDEFINED`, unknown/future categories normalized to Undefined, and sources whose `ApplicationInfo` cannot be resolved;
- OFF -> block those sources with `Unavailable(UNCLASSIFIED_APP)`.

Known non-audio categories remain blocked with `NON_AUDIO_APP`; this override does not turn them into allowed audio sources.

When `Ignore non-audio apps` is OFF, this setting has no behavioral effect. The Advanced switch may be rendered disabled while retaining its persisted value so re-enabling the primary filter restores the user's override.

For `UNCLASSIFIED_APP`, the Top Bar tooltip explicitly directs the user here:

> AALyrics could not verify this app as an audio app. Enable Settings > Advanced > Allow unclassified apps to allow lyrics lookup.

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
- Ignore non-audio apps -> ON;
- Allow unclassified apps -> OFF;
- Android Auto compatibility acknowledgement -> Not reviewed;
- Automatically check for updates -> ON;
- automatic update-check 7-day cadence -> cleared;
- pending/success update recovery state -> cleared.

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

Bundled legal-document text, Settings-local subscreen selection, and `HelpFeedbackDestination` routing are not durable application preferences and are not reset targets. Adding or changing those read-only/transient presentation surfaces alone does not expand the `Reset AALyrics` scope.

Both Storage and Reset explanations use the shared `SettingInfoTooltip`; explanatory subtitle text is not permanently rendered in the rows.

### Advanced navigation ownership

The Advanced surface remains Settings-owned UI. It does not become a fifth primary destination.

Ordinary child-screen navigation preserves the Settings home scroll position. Entering Advanced, Changelog, Privacy Policy, Terms of Use, License, Help & Feedback, or Support AALyrics and then using Header/System Back returns to the same Settings home viewport the user left.

Reselecting the already-selected Settings bottom-navigation tab is a stronger Settings-root reset. It dismisses any active Settings modal, discards uncommitted dialog-local draft state such as a Target-language selection, leaves any Settings subscreen, returns directly to the main Settings surface, and scrolls the Settings home content back to the top.

A suitable presentation interaction is conceptually:

```text
SettingsScreen
    -> onAdvancedRequested()
application/navigation owner
    -> AdvancedSettingsScreen
```

PR #49 keeps Advanced as local Settings-owned presentation state. Opening the row swaps the Settings body to `AdvancedSettingsScreen`; its Back affordance and system Back return to the main Settings body without introducing a fifth primary destination or an application navigation stack.


## Branding footer

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
- manual app update failure/retry state;
- shared new-release dialog for MANUAL/AUTOMATIC update availability;
- install failure with signing-identity mismatch plus the expanded reason tooltip at typical, narrow, and enlarged-font configurations;
- automatic update checking ON and OFF;
- new-release dialog at typical, narrow, and enlarged-font configurations;
- install-permission explanation dialog at typical, narrow, and enlarged-font configurations;
- install-permission Settings return with trust denied and trust granted;
- update-success dialog at typical, narrow, and enlarged-font configurations;
- Changelog screen at typical, narrow, and enlarged-font configurations;
- Privacy Policy screen at typical, narrow, and enlarged-font configurations;
- Terms of Use screen at typical, narrow, and enlarged-font configurations;
- License screen with inline Third-party licenses content at typical, narrow, and enlarged-font configurations;
- Help & Feedback screen at typical, narrow, and enlarged-font configurations;
- Support AALyrics screen at typical, narrow, and enlarged-font configurations;
- branding footer;
- Advanced navigation row;
- Lyrics source filtering with Ignore non-audio apps ON (default) and OFF;
- Advanced screen with Verbose details OFF and ON;
- Advanced Playback source with Allow unclassified apps OFF (default), ON, and primary-filter-disabled presentation;
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
- application-owned playback-source eligibility preferences -> Lyrics and Advanced rows plus the pre-provider lookup gate;
- build/version facts and other already-owned application presentation data.

A durable Plain auto-scroll preference remains a separate ownership decision unless the runtime-host implementation has an already-approved backing seam.

The update runtime is application-owned. In #75, Settings owns explicit MANUAL discovery states (Check/Retry, Checking, Up to date, Check failed) and continues to present the inherited post-Update process states after the shared release dialog hands off to the existing download/install route. MANUAL and AUTOMATIC newer-release discovery already converge on the same global release-available dialog, while automatic non-update results remain silent. #76 moves the post-Update process presentation—preparation/download progress, verification, retained-artifact continuation, permission-required handling, typed failures/Retry, install preparation, and PackageInstaller handoff—out of Settings and into the Unified Update Dialog. Active work remains application/process-owned across ordinary destination changes. Durable pending/success replacement markers, one-time success feedback, best-effort post-replacement resume, and automatic-check cadence/suppression remain application-owned. Changelog remains functional independently of release-network wiring: the application supplies the bundled repository `CHANGELOG.md` as presentation text. The About & Support implementation keeps bundled legal-document access application-owned, maps Help & Feedback semantic actions to GitHub destinations in `:app`, and keeps the existing Buy Me a Coffee handoff application-owned; none of these capabilities moves asset access or browser launching into `:ui:phone`.

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
- embedded donation/payment WebViews, in-app payment handling, or undocumented Buy Me a Coffee prefill behavior;
- additional developer/experimental controls beyond the approved Advanced contract, including playback-source overrides other than `Allow unclassified apps`.
