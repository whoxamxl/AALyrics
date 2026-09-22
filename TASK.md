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

This slice enriches presentation/diagnostics and exposes MediaSession observation health without changing the existing session-selection policy, playback transport, lyrics lookup, provider behavior, or persistence.

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

### Top Bar

- Keep the current playback-source pill and human-readable label behavior.
- Show the selected playback application's icon in the pill when available.
- Retain the existing cyan-dot treatment as the visual fallback when no app icon is available.
- Present an explicit runtime status: `Connecting`, `Connected`, `Disconnected`, `Unavailable`, or `Error`.
- `Connected` requires the runtime-selected package, current playback package, and resolved app-info package to agree.
- `Disconnected` means session observation is healthy but no active media session is available.
- `Unavailable` means sessions exist but the current selection/support policy cannot use one; this state carries an explicit reason (`UNSUPPORTED_PLAYER` or `UNKNOWN`) and exposes it through a concise information tooltip. The final fallback UI must remain complete even when no app label/icon can be resolved.
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

Category and SDK display are diagnostic metadata only. They must not influence playback-source eligibility, media-session selection, provider selection, compatibility gating, or UI feature availability.

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
- [x] Add app category, min SDK, and target SDK to Verbose Details Developer / Diagnostics.
- [x] Keep Android package/application objects outside `:ui:phone`.
- [x] Keep normal Details user-facing playback-source labeling unchanged apart from sharing the new resolver.
- [x] Add focused tests for resolver/mapping fallback, category behavior, and SDK diagnostic mapping.
- [x] Align deterministic Top Bar / shell / Details Previews for icon-present, icon-unavailable, all five runtime states, Unavailable generic fallback, known-category, and undefined-category cases where practical.
- [x] Update relevant Phone/runtime/details documentation and keep implementation aligned with this task.
- [ ] Run architecture checks, unit tests, debug APK build, CI, and bounded review before merge.

## Reset contract

This slice adds no persisted setting, onboarding acknowledgement, downloaded asset, or durable user state. `Reset AALyrics` semantics are unchanged.

## Scope guard

Do not add playback-app allowlists, category-based filtering, media-session selection changes, provider behavior, package-version diagnostics, permission inspection, signature inspection, install-source inspection, compile-SDK diagnostics, or new persistence in this slice.

Do not expose raw `ApplicationInfo`, `PackageManager`, `Drawable`, `MediaController`, or other Android framework objects through the Phone presentation model.
