# Playback Source App Info

## Branch and baseline

- Branch: `feature/playback-source-app-info`
- Base: `main` at `f03469227116e066ab9267a1445027a79bfd4e8d`.
- Classification: **PHONE UI / APPLICATION METADATA / DIAGNOSTICS**.
- Authoritative references: `AGENTS.md`, `docs/PHONE_UI_SPEC.md`, `docs/PHONE_RUNTIME_HOST.md`, `docs/PHONE_DETAILS.md`, and the existing selected-MediaSession/runtime boundaries.

## Goal

Replace the current label-only playback-source package resolution with one application-owned metadata resolver that derives the selected playback app's human-readable label, application icon, and Android application category from `ApplicationInfo`.

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
└─ category
        ↓
        ├─ persistent Phone Top Bar: icon + label
        └─ Verbose Details: package + category
```

This is a presentation/diagnostic enrichment only. It must not change media-session discovery, source selection, playback transport, lyrics lookup, provider behavior, or persistence.

## Approved behavior

### Resolution

- Resolve from the selected playback package already exposed by `PlaybackSnapshot.source.id`.
- Replace `PlaybackSourceLabelResolver` with an application-owned `PlaybackSourceAppInfoResolver`.
- Perform one `ApplicationInfo` lookup per package and cache the resolved result rather than issuing separate label/icon/category lookups.
- Keep the package identifier as the human-readable label fallback when application lookup or label resolution fails.
- Treat the source icon as optional presentation data; failure to resolve an icon must not hide the playback source.
- Normalize known `ApplicationInfo.category` constants into stable presentation labels such as `Audio`, `Video`, and `Game`.
- Preserve an explicit `Undefined` category when Android reports `CATEGORY_UNDEFINED`; if application metadata itself cannot be resolved, the diagnostic category may be unavailable.

### Top Bar

- Keep the current playback-source pill and human-readable label behavior.
- Show the selected playback application's icon in the pill when available.
- Retain the existing cyan-dot treatment as the visual fallback when no app icon is available.
- Do not pass `ApplicationInfo`, `PackageManager`, or Android `Drawable` objects into `:ui:phone`; platform icon ownership stays on the application side and the Phone UI receives renderable presentation content only.

### Developer / Diagnostics

When `Settings > Advanced > Verbose details` is enabled, keep the raw playback package and add the resolved Android app category:

```text
DEVELOPER / DIAGNOSTICS

App package           com.spotify.music
App category          Audio
Provider ID           ...
Source ID             ...
Track references      ...
```

Category display is diagnostic metadata only. It must not influence playback-source eligibility, media-session selection, provider selection, or UI feature availability.

## Acceptance criteria

- [ ] Replace the label-only resolver with `PlaybackSourceAppInfoResolver` and an immutable resolved app-info model.
- [ ] Resolve and cache package name, label, icon, and category from the selected playback package.
- [ ] Preserve package-name fallback semantics when application/label resolution fails.
- [ ] Present the real playback app icon in the persistent Top Bar when available, with the current cyan dot as fallback.
- [ ] Add app category to Verbose Details Developer / Diagnostics.
- [ ] Keep Android package/application objects outside `:ui:phone`.
- [ ] Keep normal Details user-facing playback-source labeling unchanged apart from sharing the new resolver.
- [ ] Add focused tests for resolver/mapping fallback and category behavior.
- [ ] Align deterministic Top Bar / shell / Details Previews for icon-present, icon-unavailable, known-category, and undefined-category cases where practical.
- [ ] Update relevant Phone/runtime/details documentation and keep implementation aligned with this task.
- [ ] Run architecture checks, unit tests, debug APK build, CI, and bounded review before merge.

## Reset contract

This slice adds no persisted setting, onboarding acknowledgement, downloaded asset, or durable user state. `Reset AALyrics` semantics are unchanged.

## Scope guard

Do not add playback-app allowlists, category-based filtering, media-session selection changes, provider behavior, package-version diagnostics, SDK-version diagnostics, permission inspection, signature inspection, install-source inspection, or new persistence in this slice.

Do not expose raw `ApplicationInfo`, `PackageManager`, `Drawable`, `MediaController`, or other Android framework objects through the Phone presentation model.
