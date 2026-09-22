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

This is a presentation/diagnostic enrichment only. It must not change media-session discovery, source selection, playback transport, lyrics lookup, provider behavior, or persistence.

## Approved behavior

### Resolution

- Resolve from the selected playback package already exposed by `PlaybackSnapshot.source.id`.
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
- [ ] Add app category, min SDK, and target SDK to Verbose Details Developer / Diagnostics.
- [x] Keep Android package/application objects outside `:ui:phone`.
- [x] Keep normal Details user-facing playback-source labeling unchanged apart from sharing the new resolver.
- [ ] Add focused tests for resolver/mapping fallback, category behavior, and SDK diagnostic mapping.
- [ ] Align deterministic Top Bar / shell / Details Previews for icon-present, icon-unavailable, known-category, and undefined-category cases where practical.
- [x] Update relevant Phone/runtime/details documentation and keep implementation aligned with this task.
- [ ] Run architecture checks, unit tests, debug APK build, CI, and bounded review before merge.

## Reset contract

This slice adds no persisted setting, onboarding acknowledgement, downloaded asset, or durable user state. `Reset AALyrics` semantics are unchanged.

## Scope guard

Do not add playback-app allowlists, category-based filtering, media-session selection changes, provider behavior, package-version diagnostics, permission inspection, signature inspection, install-source inspection, compile-SDK diagnostics, or new persistence in this slice.

Do not expose raw `ApplicationInfo`, `PackageManager`, `Drawable`, `MediaController`, or other Android framework objects through the Phone presentation model.
