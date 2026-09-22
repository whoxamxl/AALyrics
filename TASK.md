# Playback Source App Info

## Branch and baseline

- Branch: `feature/playback-source-app-info`
- Base: `main` at `f03469227116e066ab9267a1445027a79bfd4e8d`.
- Classification: **PHONE UI / APPLICATION METADATA / DIAGNOSTICS**.
- Authoritative references: `AGENTS.md`, `docs/PHONE_UI_SPEC.md`, `docs/PHONE_RUNTIME_HOST.md`, `docs/PHONE_DETAILS.md`, and the existing selected-MediaSession/runtime boundaries.

## Goal

Replace the current label-only playback-source package resolution with one application-owned metadata resolver that derives the selected playback app's human-readable label, application icon, Android application category, minimum SDK level, and target SDK level from `ApplicationInfo`.

The selected MediaSession package remains the source identity:

```text
MediaSession / MediaController
        ↓
packageName
        ↓
PlaybackSourceAppInfoResolver
        ↓
PlaybackSourceAppInfo
├─ packageName
├─ label
├─ icon
├─ category
├─ minSdkVersion
└─ targetSdkVersion
        ↓
        ├─ persistent Phone Top Bar: icon + label
        └─ Verbose Details: package + category + SDK levels
```

This slice enriches presentation/diagnostics, exposes MediaSession observation health, and defines an app-owned playback-source eligibility policy that may suppress unnecessary lyrics lookup. It must not change MediaSession discovery/selection, playback transport, provider ordering/scoring, or Android framework ownership.

## Approved behavior

### Resolution

- Resolve normal selected-source metadata from the active playback package. For `Unavailable`, prefer the package carried by `PlaybackSourceRuntimeState.Unavailable` so a policy-rejected session can still resolve its real app identity; use the current playback package only as a transitional fallback.
- Replace `PlaybackSourceLabelResolver` with an application-owned `PlaybackSourceAppInfoResolver`.
- Perform one `ApplicationInfo` lookup per package and cache the resolved result rather than issuing separate label/icon/category/SDK lookups.
- Keep the package identifier as the human-readable label fallback when application lookup or label resolution fails.
- Treat the source icon as optional presentation data; failure to resolve an icon must not hide the playback source.
- Normalize known `ApplicationInfo.category` constants into stable presentation labels such as `Audio`, `Video`, and `Game`.
- Preserve an explicit `Undefined` category when Android reports `CATEGORY_UNDEFINED`; if application metadata itself cannot be resolved, the diagnostic category may be unavailable.
- Carry `minSdkVersion` and `targetSdkVersion` from the same resolved `ApplicationInfo` into presentation-ready diagnostic fields; they are informational only and must not drive compatibility or feature decisions.

### Lyrics source eligibility

Add two persisted application-owned settings:

- `Ignore non-audio apps` under `Settings > Lyrics`, default **ON**.
- `Allow unclassified apps` under `Settings > Advanced > Playback source`, default **OFF**.

The eligibility policy runs after a MediaSession source/package has been observed and app metadata has been resolved, but before any lyrics-provider lookup is started. It does not alter MediaSession discovery, selected-session ownership, transport, app launching, or provider ranking.

Policy:

```text
Ignore non-audio apps = OFF
        -> allow lyrics lookup regardless of app category

Ignore non-audio apps = ON
        -> CATEGORY_AUDIO
              -> allow lyrics lookup
        -> known non-audio category
              -> block lyrics lookup
              -> Unavailable(NON_AUDIO_APP)
        -> CATEGORY_UNDEFINED or ApplicationInfo unavailable
              -> Allow unclassified apps = ON
                    -> allow lyrics lookup
              -> Allow unclassified apps = OFF
                    -> block lyrics lookup
                    -> Unavailable(UNCLASSIFIED_APP)
```

Unknown/future Android category values normalized to `Undefined` follow the unclassified path. `Allow unclassified apps` has no behavioral effect while `Ignore non-audio apps` is OFF.

### Top Bar

- Keep the current playback-source pill and human-readable label behavior.
- Show the selected playback application's icon in the pill when available.
- Retain the existing cyan-dot treatment as the visual fallback when no app icon is available.
- Present an explicit runtime status: `Connecting`, `Connected`, `Disconnected`, `Unavailable`, or `Error`.
- `Connected` requires the runtime-selected package, current playback package, and resolved app-info package to agree.
- `Disconnected` means session observation is healthy but no active media session is available.
- `Unavailable` means a session exists but AALyrics cannot use that source for the current lyrics policy. Reasons are `NON_AUDIO_APP`, `UNCLASSIFIED_APP`, and `UNKNOWN`. The final fallback UI must remain complete even when no app label/icon can be resolved.
- `NON_AUDIO_APP` tooltip: "This app is not classified as an audio app. Lyrics lookup is disabled while Ignore non-audio apps is enabled."
- `UNCLASSIFIED_APP` tooltip: "AALyrics could not verify this app as an audio app. Enable Settings > Advanced > Allow unclassified apps to allow lyrics lookup."
- `UNKNOWN` uses a generic unavailable explanation and must not point users to an override that may not apply.
- `Error` carries one of `NOTIFICATION_ACCESS_LOST`, `SESSION_QUERY_FAILED`, `SESSION_ATTACH_FAILED`, or `UNKNOWN`; the Top Bar exposes the concise reason through an information tooltip.
- When `Connected` and the selected playback app has a real launch capability, the whole source pill opens that app and shows the same external-link affordance used by Settings.
- Apply semantic status color coding without changing the pill geometry: Connecting keeps the neutral treatment, Connected uses `Success`, Disconnected uses disabled/tertiary neutral, Unavailable uses `Warning`, and Error uses the shared `Error` token. Background and border receive only low-emphasis blends of the same semantic color.
- Do not pass `ApplicationInfo`, `PackageManager`, or Android `Drawable` objects into `:ui:phone`; platform icon ownership stays on the application side and the Phone UI receives renderable presentation content only.

### Developer / Diagnostics

When `Settings > Advanced > Verbose details` is enabled, keep the raw playback package and add the resolved Android app category plus SDK levels:

```text
DEVELOPER / DIAGNOSTICS

App package           com.spotify.music
App category          Audio
Min SDK               23
Target SDK            35
Provider ID           ...
Source ID             ...
Track references      ...
```

The Verbose Details rows are diagnostic presentation. The underlying application category metadata may also be consumed by the explicit playback-source lyrics eligibility policy above, independently of whether Verbose Details is enabled. Min/target SDK levels remain diagnostic-only and must not drive eligibility, MediaSession selection, provider selection, compatibility gating, or UI feature availability.

## Acceptance criteria

- [x] Replace the label-only resolver with `PlaybackSourceAppInfoResolver` and an immutable resolved app-info model.
- [x] Resolve and cache package name, label, icon, category, min SDK, and target SDK from the selected playback package.
- [x] Preserve package-name fallback semantics when application/label resolution fails.
- [x] Present the real playback app icon in the persistent Top Bar when available, with the current cyan dot as fallback.
- [x] Replace the connected Boolean with explicit Connecting / Connected / Disconnected / Unavailable / Error runtime states.
- [x] Preserve explicit playback-source error reasons and expose a concise Error tooltip in the Top Bar.
- [x] Make launch-capable Connected pills open the selected playback app with the shared external-link icon.
- [x] Apply consistent semantic state colors to Top Bar pill foreground, border, and background.
- [x] Keep Top Bar runtime-state pills content-sized with shared padding/max-width behavior, and resolve Unavailable app identity from its runtime package.
- [x] Give Unavailable an explicit reason, concise tooltip, and generic no-app-identity fallback presentation.
- [x] Replace the provisional Unavailable reasons with `NON_AUDIO_APP`, `UNCLASSIFIED_APP`, and `UNKNOWN`, including the approved tooltip copy.
- [x] Add application-owned persistence for `Ignore non-audio apps` (default ON) and `Allow unclassified apps` (default OFF), including Reset defaults and focused persistence tests.
- [x] Add Settings presentation/callback wiring for `Ignore non-audio apps` and `Allow unclassified apps`, including disabled Advanced override presentation and deterministic Previews.
- [x] Add an app-owned source-eligibility policy and atomically gate lyrics lookup before provider work without changing MediaSession selection/transport.
- [x] Map known non-audio sources to `Unavailable(NON_AUDIO_APP)` and undefined/unresolved sources to `Unavailable(UNCLASSIFIED_APP)` unless the Advanced override allows them.
- [x] Update Settings/Top Bar Previews and focused tests for policy defaults, overrides, both Unavailable reasons, and Reset behavior.
- [x] Add app category, min SDK, and target SDK to Verbose Details Developer / Diagnostics.
- [x] Keep Android package/application objects outside `:ui:phone`.
- [x] Keep normal Details user-facing playback-source labeling unchanged apart from sharing the new resolver.
- [x] Add focused tests for resolver/mapping fallback, category behavior, and SDK diagnostic mapping.
- [x] Align deterministic Top Bar / shell / Details Previews for icon-present, icon-unavailable, all five runtime states, Unavailable generic fallback, known-category, and undefined-category cases where practical.
- [x] Update relevant Phone/runtime/details documentation and keep implementation aligned with this task.
- [ ] Run architecture checks, unit tests, debug APK build, CI, and bounded review before merge.

## Reset contract

This policy adds two AALyrics-owned persisted settings. `Reset AALyrics` must restore:

- `Ignore non-audio apps` -> **ON**;
- `Allow unclassified apps` -> **OFF**.

No Android permission, MediaSession state, or other application's state is changed by reset.

## Scope guard

Do not add playback-app package allowlists/denylists, change MediaSession selection, change provider ordering/scoring, or add package-version, permission, signature, install-source, or compile-SDK diagnostics. Category-based eligibility is limited to the two approved persisted settings and may gate only whether lyrics lookup starts for the selected source.

Do not expose raw `ApplicationInfo`, `PackageManager`, `Drawable`, `MediaController`, or other Android framework objects through the Phone presentation model.
