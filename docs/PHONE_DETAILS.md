# Phone Details Specification

## Status

This document defines the approved presentation contract for the Phone `Details` destination.

PR #49 implemented the original production `DetailsScreen`, Phone-local presentation state, application-owned runtime mapping, deterministic Previews, and focused mapper tests. On `feature/translation-runtime`, the application-owned Translation Details state/mapping is now implemented; Compose rendering and deterministic Translation Details Previews remain the next checkpoint. Details remains read-only: it presents already-owned runtime facts and must not start Translation, download models, retry work, or trigger provider/network activity.

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

### Translation section

When a current canonical lyric document exists, Normal Details may add a dedicated `TRANSLATION` section below `LYRICS` rather than mixing profiler/runtime facts into canonical Lyrics metadata. The current target language is always available from Translation settings; source-language presentation appears only when an authoritative current-profile result exists for the same canonical lyrics identity. Do not synthesize Source language from `LyricsDocument.languageTag` merely to fill the row.

Conceptually:

```text
TRANSLATION
Source language          English (Spanish)
Target language          Japanese
```

Rules:

- `Source language` comes from the Translation language profile, not from `LyricsDocument.languageTag`;
- show the Primary language as the main value;
- append the Secondary language in parentheses only when `secondaryActivation == ACTIVE`, for example `English (Spanish)`; incidental Secondary evidence remains an internal profiler fact and is not promoted into Normal Details;
- omit the Secondary suffix when there is no ACTIVE Secondary language;
- a Source language row is shown only for a profile whose canonical identity matches the current canonical lyrics; a stale profile from the previous track/request must never be reused;
- `Target language` is the current normalized Translation target rendered as a user-facing language name and may remain visible even when Translation is disabled;
- do not expose request IDs, provider IDs, model IDs, or other machine-facing Translation internals in Normal Details.

WORD is a valid source sync type even while the Phone experience remains line-oriented by default. Displaying `Word synced` in Details does not enable Karaoke mode or change rendering behavior.

## Verbose Details

`Settings > Advanced > Debug > Verbose details` enriches the existing Duration and Lines rows with the live progress values described above and adds a separate section below the normal content:

```text
DEVELOPER / DIAGNOSTICS

App package           com.spotify.music
App category          Audio
Min SDK               23
Target SDK            35
Provider ID           musixmatch
Source ID             <provider source id>
Track references      <namespace:value ...>
```

The initial diagnostic surface should prefer already-available framework-neutral facts.

Suitable first fields include:

- playback application package / `PlaybackSource.id`, such as `com.spotify.music`; Verbose Details always exposes this raw identifier, while normal `Playback source` prefers the human-readable app label and uses the package name as the final fallback when label resolution fails;
- Android application category from the same application-owned playback-source metadata resolution, rendered as a stable diagnostic label such as `Audio`, `Video`, or `Game`; Android `CATEGORY_UNDEFINED` is shown as `Undefined`, while category may be unavailable if application metadata lookup itself fails. The same underlying category fact may independently participate in the explicit playback-source lyrics eligibility policy; Verbose Details only controls whether that already-resolved fact is shown;
- minimum SDK level from `ApplicationInfo.minSdkVersion` and target SDK level from `ApplicationInfo.targetSdkVersion`; these values are diagnostic facts only and do not define AALyrics compatibility policy;
- provider key / `LyricsAttribution.providerId`;
- provider source identifier / `LyricsAttribution.sourceId`, when available;
- normalized track references such as `namespace:value`, when available.

These values are useful for reproducing provider and identity issues but are not primary user-facing metadata.

When Verbose Details is enabled, the existing `TRANSLATION` section gains only the compact runtime diagnostics needed to understand the current Translation state:

```text
TRANSLATION
Source language          English (Spanish)
Target language          Japanese
Runtime state            Ready
Source model (EN, ES)    Ready
Target model (JA)        Ready
```

The Translation runtime row uses exactly these stable presentation states:

- `Disabled`;
- `Idle`;
- `Translating`;
- `Not required`;
- `Ready`;
- `Failed`.

The source/target model rows use:

- `Not required`;
- `Checking`;
- `Downloading`;
- `Waiting for system`;
- `Ready`;
- `Failed`;
- `Timed out`.

Model-state semantics are availability-oriented, not "currently needed" semantics:

- a built-in model is `Ready`;
- an already-present downloaded model is `Ready`, including while Translation is OFF;
- `Not required` is reserved for the specific case where Translation is OFF, the relevant remote model is not present, and AALyrics therefore has no current reason to prepare it;
- when Translation is ON, a missing required model must not be presented as `Not required`; it should resolve through the active preparation lifecycle (`Checking`, `Downloading`, or `Waiting for system`) or a terminal failure state;
- a process-local missing model-state entry is not proof that a model is absent until startup inventory reconciliation has established that fact.

The source-model row is one compact aggregate row rather than one row per source language. The label lists the Translation-relevant source ISO tags: Primary plus ACTIVE Secondary when present, for example `Source model (EN, ES)`. Its value summarizes the source-side model requirements:

- `Ready` only when every required source-side model is available; built-in source languages count as Ready;
- an active preparation phase is shown when any required source-side model is still preparing;
- `Failed` or `Timed out` is shown when any required source-side model reaches that terminal state;
- when multiple source models have different nonterminal states, present the most actionable state in this order: `Waiting for system` -> `Downloading` -> `Checking` -> `Ready`;
- terminal failure takes precedence over nonterminal/Ready states, and the failure tooltip identifies the affected ISO language(s).

If no authoritative current source profile exists yet, omit the Source language and Source model rows instead of guessing from provider metadata. The Target language/model rows remain independently representable because the target setting is known before profiling.

Do not expand Verbose Details into a dump of Translation request IDs, provider IDs, profiler internals, or per-model implementation data. The goal is to answer "what is Translation doing, and which model is blocking it?" in a few rows.

### Failure reason tooltip standard

Details uses one consistent rule for failure states: **a displayed failure state must expose its authoritative reason through the standard info-tooltip affordance**.

This applies to Translation runtime/model rows and to future Details status/diagnostic rows added elsewhere.

Examples:

```text
Runtime state            Failed ⓘ
Source model (JA)        Failed ⓘ
Target model (EN)        Ready
```

Rules:

- `Failed` and `Timed out` values must show the shared semantic info icon and expose an authoritative reason; an unexplained visible failure state is not considered complete Details implementation;
- the tooltip contains the concrete framework-neutral reason/detail supplied by the owning runtime; for an aggregated Source model row it also identifies the affected source ISO language(s);
- do not inline long exception/error text into the Details row;
- do not invent a reason in `:ui:phone`;
- if a Details feature wants to expose a failure state but the owning runtime does not provide a reason, treat that as a missing diagnostic contract to resolve during implementation rather than silently adding an unexplained failure row;
- future additions to Details must re-check this rule whenever they introduce a new failure-capable status.

### Diagnostic boundary

Verbose Details must remain presentation-only. Playback-source category eligibility is application-owned and evaluated independently of this toggle; enabling Verbose Details neither enables nor disables filtering and must not initiate metadata/provider work that would not otherwise occur.

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
├─ translation
│  ├─ sourceLanguageLabel
│  ├─ targetLanguageLabel
│  ├─ runtimeState (verbose only)
│  ├─ runtimeFailureReason (verbose failure only)
│  ├─ sourceModelLabels / aggregateState (verbose only)
│  ├─ sourceModelFailureReason (verbose failure only)
│  ├─ targetModelLabel / state (verbose only)
│  └─ targetModelFailureReason (verbose failure only)
├─ verboseDetailsEnabled
└─ diagnostics
   ├─ appPackageName
   ├─ appCategory
   ├─ appMinSdkVersion
   ├─ appTargetSdkVersion
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
- playback package available while app category metadata is undefined or unavailable;
- playback package available while SDK metadata is unavailable because application metadata lookup failed.

The screen should show only facts that are authoritative for the current state and avoid stale values from the previous track. Translation profile/runtime facts must be canonical-identity gated just like translated lyric presentation. Target settings/model inventory are process/application facts and must not be confused with a stale per-track profile.

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
- Normal Details with Translation Primary only and Primary + ACTIVE Secondary (`English (Spanish)`);
- Translation target visible with no authoritative source profile yet;
- Verbose Details with every runtime state: Disabled / Idle / Translating / Not required / Ready / Failed;
- source/target model states covering Ready, Not required, Checking, Downloading, Waiting for system, Failed, and Timed out;
- built-in English and already-downloaded remote models shown as Ready while Translation is OFF;
- Failed and Timed out rows with the standard reason tooltip;
- aggregated multi-source model presentation such as `Source model (EN, ES)`;
- Verbose Details ON with playback package/category/SDK levels, provider/source IDs, and track references;
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
