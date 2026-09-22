# Phone Details Specification

## Status

This document defines the first approved presentation contract for the Phone `Details` destination.

PR #49 implements this contract with a production `DetailsScreen`, Phone-local presentation state, application-owned runtime mapping, deterministic Previews, and focused mapper tests. The Details destination remains read-only. Its purpose is to make useful current track and lyrics metadata visible without crowding the Lyrics destination, while optionally exposing deeper framework-neutral diagnostics when the user enables `Settings > Advanced > Verbose details`.

## Product intent

Details has two presentation levels:

```text
Normal Details
    +
Verbose details enabled?
    ├─ no  -> user-facing metadata only
    └─ yes -> add Developer / Diagnostics
```

Normal Details should remain understandable to an ordinary user. Machine-facing identifiers belong only in the optional diagnostic section.

When Verbose Details is enabled, the existing Duration and Lines rows stay in place but expose live progress context:

```text
Duration (verbose)      1:32 / 3:41
Lines (verbose)         28 / 64
```

Duration uses projected current playback position over total track duration. Lines uses a one-based current synchronized lyric line over total resolved line count. PLAIN lyrics have no authoritative current line, so the verbose current-line value is shown as unavailable (`— / <total>`) rather than inventing a timed line.

Enabling Verbose Details changes presentation only. It must not alter lyrics lookup, provider ordering, candidate selection, timing, Translation, playback control, or rendering behavior.

## Normal Details

Initial structure:

```text
Details

TRACK
Title                 <title>
Artist                <artist(s)>
Album                 <album>
Duration              <duration>
Playback source       <media app/source>

LYRICS
Provider              <display name>
Sync type             <Plain / Line synced / Word synced>
Language              <language when known>
Lines                 <count>
```

### Track section

Use presentation-ready values derived from the current track/playback state.

Candidate fields:

- title;
- artist or artist list;
- album when available;
- duration when available;
- human-readable playback source such as Spotify, YouTube Music, or Poweramp.

Unknown optional values should be omitted or represented consistently as unavailable; the UI must not invent metadata.

### Lyrics section

Use presentation-ready values derived from the resolved canonical lyrics document.

Initial fields:

- Provider — human-readable `LyricsAttribution.displayName`;
- Sync type — user-facing rendering of `PLAIN`, `LINE`, or `WORD`;
- Language — presentation-ready language name/tag when known;
- Lines — logical lyric-line count.

Normal Details must not expose `providerId` or `sourceId` merely because they exist in the domain model.

WORD is a valid source sync type even while the Phone experience remains line-oriented by default. Displaying `Word synced` in Details does not enable Karaoke mode or change rendering behavior.

## Verbose Details

`Settings > Advanced > Debug > Verbose details` enriches the existing Duration and Lines rows with the live progress values described above and adds a separate section below the normal content:

```text
DEVELOPER / DIAGNOSTICS

App package           com.spotify.music
App category          Audio
Provider ID           musixmatch
Source ID             <provider source id>
Track references      <namespace:value ...>
```

The initial diagnostic surface should prefer already-available framework-neutral facts.

Suitable first fields include:

- playback application package / `PlaybackSource.id`, such as `com.spotify.music`; Verbose Details always exposes this raw identifier, while normal `Playback source` prefers the human-readable app label and uses the package name as the final fallback when label resolution fails;
- Android application category from the same application-owned playback-source metadata resolution, rendered as a stable diagnostic label such as `Audio`, `Video`, or `Game`; Android `CATEGORY_UNDEFINED` is shown as `Undefined`, while category may be unavailable if application metadata lookup itself fails;
- provider key / `LyricsAttribution.providerId`;
- provider source identifier / `LyricsAttribution.sourceId`, when available;
- normalized track references such as `namespace:value`, when available.

These values are useful for reproducing provider and identity issues but are not primary user-facing metadata.

### Diagnostic boundary

Verbose Details must remain presentation-only.

Turning it on must not:

- trigger another provider lookup;
- request extra provider endpoints;
- change candidate scores or tie breaking;
- change preferred synchronization type;
- enable WORD-level highlighting;
- enable timing correction;
- change Translation behavior;
- change MediaSession selection;
- retain raw provider DTOs inside `:ui:phone`.

If a future diagnostic field requires new evidence from the selection/lookup pipeline, that evidence must first receive an explicit framework-neutral application/domain contract. The Details UI must not reach into provider implementations to obtain it.

## Advanced setting ownership

`Verbose details` is a user setting owned outside the composable layer.

Conceptually:

```text
application-owned setting
        ↓
Phone presentation mapping
        ↓
DetailsScreenUiState
        ↓
DetailsScreen
```

PR #49 persists the preference through an application-owned SharedPreferences store. The stable requirement remains that `:ui:phone` receives only the resolved boolean/presentation state and never reads SharedPreferences directly.

The setting may control whether diagnostic values and live progress presentation are mapped/presented, but it does not control data acquisition behavior. Live verbose playback/line progress reuses the already-available playback snapshot and resolved lyrics and does not trigger provider work.

## Destination composition

`DetailsScreen` is destination-owned content hosted inside `PhoneAppShell`.

It should:

- fill the available destination area;
- scroll vertically when content exceeds the viewport;
- use the same compact section/row language established by Phone Settings where appropriate;
- reserve the shell-provided Playback Surface overlay inset at the bottom;
- avoid duplicating persistent Top Bar, Playback Surface, or Bottom Navigation;
- remain fully read-only in the first implementation.

No large duplicate Track Card is required merely to repeat the Lyrics destination. Details should favor dense labeled metadata rows.

## Presentation state boundary

The production Phone-local state contains presentation facts equivalent to:

```text
DetailsScreenUiState
├─ track
│  ├─ title
│  ├─ artists
│  ├─ album
│  ├─ duration
│  └─ playbackSourceLabel
├─ lyrics
│  ├─ providerDisplayName
│  ├─ syncTypeLabel
│  ├─ languageLabel
│  └─ lineCount
├─ verboseDetailsEnabled
└─ diagnostics
   ├─ appPackageName
   ├─ appCategory
   ├─ providerId
   ├─ sourceId
   └─ trackReferences
```

PR #49 established the concrete `DetailsScreenUiState` shape and application-owned `phoneDetailsState` mapping. The Phone runtime host defined in `docs/PHONE_RUNTIME_HOST.md` should consume that existing state rather than remapping provider/media facts in the Activity. Do not pass provider DTOs, `MediaController`, `PlaybackState`, framework queue objects, or Android intents into the screen.

## Empty and partial state

Details must tolerate partial runtime information.

Examples:

- active track but lyrics still loading;
- track metadata without album;
- resolved lyrics without language tag;
- provider attribution without source ID;
- no stable external track references;
- no active media session;
- playback package available while app category metadata is undefined or unavailable.

The screen should show only facts that are authoritative for the current state and avoid stale values from the previous track.

Exact loading/empty visual treatment will be tuned during implementation.

## Accessibility and responsive behavior

Validate at least:

- typical phone width;
- 320dp narrow width;
- enlarged font scale;
- long title/artist/album/provider values;
- long diagnostic identifiers;
- Playback Surface visible and hidden.

Labels and values should remain distinguishable through semantics, not color alone. Long machine identifiers may truncate visually if necessary, but the first implementation should not add copy/share interactions unless separately approved.

## Preview matrix

PR #49 provides deterministic Previews covering:

- normal Details with complete metadata;
- normal Details with partial metadata;
- lyrics loading/unavailable;
- Verbose Details OFF;
- Verbose Details ON with playback package/category, provider/source IDs, and track references;
- Verbose Details ON with missing optional diagnostic values;
- narrow width;
- enlarged font;
- Details hosted inside `PhoneAppShell` with Playback Surface visible.

## Explicitly deferred

The first Details contract does not define or implement:

- candidate score display;
- candidate rank/order;
- per-provider search-result lists;
- match-confidence internals;
- raw provider request/response payloads;
- log viewer or log export;
- copy/share diagnostic actions;
- network diagnostics;
- cache diagnostics;
- timing/calibration controls;
- Sync controls;
- Karaoke controls;
- any mutation of provider, playback, lyrics, Translation, or timing state.
