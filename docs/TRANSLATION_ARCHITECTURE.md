# Translation Architecture

## Purpose

Define AALyrics Translation as an additive derived capability over canonical lyrics, with a stable boundary between lyrics retrieval, translation orchestration, translation engines/providers, background model infrastructure, and presentation.

The current working fork `whoxamxl/auto-lyrics` remains the behavioral reference for mature Translation settings and Google ML Kit model lifecycle. AALyrics should preserve or refactor that proven behavior rather than rewrite it without evidence.

## Stable ownership rules

Translation is not part of lyrics retrieval or cross-provider candidate ranking.

```text
Lyrics Providers
      ↓
CandidateSelector
      ↓
Canonical Lyrics
      ↓
Translation capability
      ↓
Translation Artifact / status
      ↓
presentation projection
```

Rules:

- concrete Lyrics Providers do not invoke translation engines;
- Lyrics Provider selection never depends on Translation availability;
- `LyricsCoordinator` remains responsible for lyrics lookup/provider orchestration, not Translation execution policy;
- Translation Providers/engines use their own capability boundary and are not added to `:provider:api`;
- Phone and Android Auto consume Translation results downstream; they do not invoke concrete translators directly;
- Translation failure never destroys or replaces valid original lyrics;
- obsolete Translation work cannot publish after the canonical lyrics, target language, or Translation request has been superseded.

A valid result may therefore use different sources:

```text
Lyrics Provider:      LRCLIB
Translation Provider: Musixmatch
```

or:

```text
Lyrics Provider:      Musixmatch
Translation Provider: ML Kit
```

Neither relationship gives the Lyrics Provider a ranking advantage.

## Original lyrics remain canonical

Translation is additive.

```text
Canonical Lyrics
├─ original text
├─ original timing
├─ provider attribution
└─ derived Translation Artifact
```

A translated artifact must never overwrite provider text or timing in place.

Every Translation Artifact must remain associated with the exact canonical lyrics and target configuration from which it was derived.

## Migration policy

The current Auto-Lyrics fork at `v1.13.0` contains mature behavior that should not be recreated from memory.

### Preserve

- the intentionally limited target set: English, Japanese, French, German, Spanish, Korean, Chinese, Italian, Portuguese;
- language-tag normalization semantics where still applicable;
- the default target behavior unless a later product decision changes it.

### Refactor

- Translation settings ownership out of legacy View/MediaTracker code;
- ML Kit model availability/download handling out of the legacy monolithic `LyricsTranslator`;
- active model-download reuse;
- cancellation behavior where a cancelled foreground waiter does not unnecessarily cancel useful process-level model preparation;
- explicit model availability checks;
- latched model failure/timeout state that suppresses automatic retries until an explicit retry;
- thermal-wait handling and active-time timeout semantics.

### Do not migrate

- foreground Translation Views from the legacy app;
- the legacy monolithic `MediaTracker` ownership model;
- the existing whole-song source-language decision based on the first five nonblank lyric lines;
- assumptions that Translation is always English-targeted.

New algorithms approved for AALyrics should not be disguised as preserved fork behavior.

## Source-language profiling

The legacy pattern:

```kotlin
lines
    .map { it.text }
    .filter { it.isNotBlank() && it != "♪" }
    .take(5)
    .joinToString("\n")
```

must not determine the song's source language in AALyrics.

A later `LanguageProfiler` will inspect the complete canonical lyric content and produce a profile conceptually containing:

```text
LanguageProfile
├─ primary
├─ secondaryCandidate
└─ secondaryActivation
      ├─ NONE
      ├─ INCIDENTAL
      └─ ACTIVE
```

Provider language/region metadata is not authoritative runtime input for this profile. Some Lyrics Providers expose useful language metadata while others do not; using it to control Translation would make behavior depend on which Lyrics Provider happened to win.

Provider language metadata may be retained later for diagnostics or profiler validation, but it must not silently alter v1 Translation routing.

### Primary and Secondary intent

AALyrics should recognize a meaningful Primary and at most one Secondary candidate for Translation routing.

A Secondary candidate being detected does **not** mean it must be translated.

Secondary activation should eventually consider evidence such as:

- line coverage;
- substantive text/token coverage;
- contiguous runs;
- distribution across the song;
- whether the evidence is mostly short borrowed phrases such as `Oh`, `Yeah`, or `Baby`.

The exact thresholds are deliberately deferred to the implementation slice and must be tuned against real songs/tests.

Routing intent:

```text
PRIMARY              -> translation eligible
SECONDARY / ACTIVE   -> translation eligible
SECONDARY / INCIDENTAL -> preserve original
UNCERTAIN            -> preserve original
TARGET LANGUAGE      -> preserve original
```

A third language is not promoted into another routing lane in the initial design. Small or uncertain third-language passages remain original unless a later explicit decision expands the model.

The governing principle is:

> Translate meaningful foreign-language passages, not every foreign-language token.

## Context-aware block translation

AALyrics will move away from completely context-free line-by-line translation where doing so can preserve deterministic line ownership.

Future block planning may use:

### Hard boundaries

- language change;
- verse/chorus or equivalent structural boundary where available;
- large timestamp gap;
- explicit blank separation.

### Soft boundaries

- preferred block size;
- character/token limit.

Hard boundaries are normally not crossed for context. Soft boundaries may use adjacent context.

The exact boundary thresholds and detection algorithm are deferred.

## Core + Context Halo

Overlapping context must never create multiple authoritative translations for the same lyric line.

Conceptually:

```text
Block A
Context: L09
CORE:    L10
CORE:    L11
CORE:    L12
Context: L13

Block B
Context: L12
CORE:    L13
CORE:    L14
CORE:    L15
Context: L16
```

Here:

```text
L12 owner = Block A
L13 owner = Block B
```

Context/Halo text may overlap freely, but each translatable lyric line has exactly one Core owner.

This eliminates winner conflicts between overlapping blocks before they can reach presentation.

## Structural alignment and fallback

Context-aware Translation must preserve canonical line identity.

```text
Canonical line N
      ↕
Translated line N
```

If a block engine cannot return a structurally valid mapping, the invalid block must not be published.

A later implementation may degrade through smaller blocks and ultimately per-line translation:

```text
block
  ↓ validation failed
smaller block
  ↓ validation failed
per-line fallback
```

Delimiter/marker strategy and validation details are implementation decisions and require regression tests against ML Kit behavior rather than assumption.

## Atomic publication

Presentation must not receive partially assembled Translation blocks.

```text
TranslationPlan
    ↓
all required blocks/providers complete
    ↓
validation / fallback complete
    ↓
complete Translation Artifact
    ↓
atomic publication
```

While Translation is pending, valid original lyrics remain visible.

A user's displayed Translation must not repeatedly change as overlapping blocks finish or as late stale work arrives.

## Translation Providers

Translation Provider architecture is independent from Lyrics Provider architecture.

Conceptually:

```text
Canonical Lyrics
      ↓
TranslationCoordinator
      ↓
Translation Provider selection
      ├─ ML Kit local Translation
      ├─ Musixmatch native Translation
      └─ future providers
      ↓
Translation Artifact
```

The final concrete provider request/candidate signatures are intentionally deferred until the large Translation implementation slice proves the required alignment and lifecycle fields.

Do not put Translation Providers in `:provider:api`.

### Musixmatch

Musixmatch native Translation remains an approved research/implementation direction, but it must not influence Lyrics Provider ranking.

When canonical lyrics come from another Lyrics Provider, Musixmatch source lyrics must be aligned against the canonical source before its Translation can be accepted.

A mere line-index match is insufficient.

If coverage/alignment is not strong enough, reject that Translation candidate and fall back to another Translation Provider such as ML Kit.

The concrete Musixmatch Translation endpoint/entitlement must be re-verified before implementation.

## Translation Provider mixing

A completed Translation Artifact uses one Translation Provider unless a later explicit decision authorizes mixing.

Do not silently produce artifacts such as:

```text
85% Musixmatch
15% ML Kit
```

If one Translation Provider cannot produce an acceptable whole artifact, reject it and try the next provider.

This keeps provenance and regression diagnosis clear.

## Google ML Kit role

Google ML Kit remains the standard local Translation engine unless a later explicit decision changes it.

The current scaffold may implement background-only ML Kit model lifecycle:

- target-language model planning;
- model availability checks;
- model download;
- active-download reuse;
- failure/timeout state that remains latched until an explicit retry;
- thermal waiting;
- timeout based on active rather than thermally blocked download time.

The large Translation algorithm slice will later add actual text/block translation and LanguageProfiler integration.

ML Kit infrastructure belongs outside pure Translation contracts because it depends on Android/Google SDK APIs.

## Settings ownership

Translation preferences are application/capability state, not a foreground View responsibility.

The background scaffold may persist:

- Translation enabled/disabled;
- selected target language.

The unfinished Phone Settings UI is not part of the scaffold.

Future foreground Settings should consume/update the same state boundary rather than owning SharedPreferences keys directly.

Changing Translation settings must not refetch lyrics providers merely because Translation configuration changed.

## Background target-model preparation

When Translation is enabled, the application may prepare the selected target model independently of foreground UI.

English requires no downloaded ML Kit language pack.

Preparing a target model does not imply that source-language models are known yet. Once a later LanguageProfiler resolves the actual route, the Translation execution layer may ensure the additional route-specific model(s).

Background model preparation must not publish lyrics, mutate canonical lyrics, or create UI state.

## Persistent Translation Cache policy

Persistent Translation Cache is intentionally **not** part of the current implementation path.

Policy:

- through stable `v1.0.0`, persistent Translation Cache must not be introduced;
- after `v1.0.0`, it remains disabled unless explicitly authorized.

This prevents stale cached output from obscuring whether a Translation algorithm change actually fixed or regressed behavior during development.

If persistent Translation Cache is authorized later, it remains logically separate from canonical lyrics caching and must include canonical lyrics identity plus Translation configuration/engine/version factors required for correctness.

## Diagnostic anomaly policy

A previously observed Korean -> Japanese Translation produced an unexpected English line even though the source line appeared Korean.

The cause is not yet reproduced or established.

Do not introduce speculative mixed-language rerouting, automatic correction, or special-case retry logic solely for this observation.

A later implementation may add non-invasive debug diagnostics for suspicious source/target/output combinations. Diagnostics must not silently alter runtime output until evidence justifies a correction policy.

## Status and failure semantics

Translation lifecycle must remain separate from lyrics lookup lifecycle.

Background model infrastructure may expose model states such as:

```text
CHECKING
DOWNLOADING
WAITING_FOR_SYSTEM
READY
FAILED
TIMED_OUT
```

The later Translation execution lifecycle may additionally distinguish disabled, pending, translating, ready, and failed.

Stable rules:

- original lyrics remain usable during all Translation work;
- Translation or model failure is not lyrics lookup failure;
- target-language changes may restart Translation without refetching Lyrics Providers;
- stale work cannot publish after lyrics/target/request supersession;
- foreground presentation does not own the model download lifecycle.

## Timing and karaoke relationship

Translation does not own provider timestamps, calibration, playback position, or karaoke progression.

Translated text references canonical lyric line identity; timing truth remains in the lyrics/timing domains.

WORD timing does not imply word-by-word Translation.

## Scaffold boundary

The Translation background scaffold is allowed to implement only contracts demonstrated by immediate background responsibilities:

- supported target languages and normalization;
- Translation settings state/persistence;
- a replaceable model-lifecycle boundary;
- the ML Kit model-lifecycle adapter;
- application-owned background target-model preparation;
- tests and architecture guardrails.

It must not prematurely implement:

- LanguageProfiler heuristics;
- Secondary activation thresholds;
- contextual block planning;
- Core + Context Halo execution;
- block alignment parser/fallback;
- TranslationCoordinator;
- Translation Provider resolver/selector;
- Musixmatch native Translation;
- actual ML Kit lyric translation execution;
- Phone/Android Auto Translation presentation;
- persistent Translation Cache.

Those belong to the subsequent large implementation slice.

## Invariants

Future Translation implementation must preserve all of the following:

1. Original canonical lyrics are never mutated.
2. Lyrics Provider selection never depends on Translation availability.
3. Translation Provider architecture remains distinct from Lyrics Provider architecture.
4. Every translated lyric line has exactly one authoritative Core owner.
5. Partial Translation Artifacts are never exposed to presentation.
6. INCIDENTAL and UNCERTAIN foreign-language text remains original by default.
7. Lyrics Provider language metadata does not control LanguageProfiler routing.
8. One completed Translation Artifact uses one Translation Provider unless explicitly changed later.
9. Persistent Translation Cache is prohibited through stable `v1.0.0` and remains disabled afterward unless explicitly authorized.
10. Optional Translation failure never invalidates usable canonical lyrics.
