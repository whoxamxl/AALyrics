# Translation Architecture

## Purpose

Define AALyrics Translation as an additive derived capability over canonical lyrics, with a stable boundary between lyrics retrieval, translation orchestration, translation engines/providers, background model infrastructure, and presentation.

The current working fork `whoxamxl/auto-lyrics` remains the behavioral reference for mature Translation settings and Google ML Kit model lifecycle. AALyrics should preserve or refactor that proven behavior rather than rewrite it without evidence.

## Current execution implementation

The Translation execution slice implements the approved design through three boundaries:

```text
:translation:api
  engine-independent language and Translation Provider contracts

:translation:core
  profiling, planning, structural fallback, artifact assembly,
  request ownership, and atomic state

:translation:mlkit
  ML Kit language evidence, model preparation, and Translation sessions
```

`:app` observes completed canonical `LyricsState` plus persisted Translation settings, assigns the exact lookup/content identity, and hands that input to `TranslationCoordinator`. It exposes the coordinator's atomic `TranslationState` for Phone presentation without changing Android Auto UI.

The default `LanguageProfilerPolicy` is named and testable:

- Latin/identifier-only lines require at least 4 substantive characters;
- distinctive script evidence requires at least 2 characters where applicable;
- ordinary line evidence requires confidence `0.45`;
- complete-document Language ID contributes `0.20` of aggregate substantive-character weight;
- Primary requires evidence weight `4.0`;
- a Secondary candidate requires evidence weight `0.25`; candidate detection intentionally remains permissive because a candidate is only evidence, not routing authority;
- Secondary becomes ACTIVE only with at least 3 meaningful lines, 18 substantive characters, `0.20` character share, average candidate-line confidence `0.75`, and either a 2-line contiguous run or presence in at least 2 song regions;
- common borrowed phrases (`Oh`, `Ooh`, `Yeah`, `Baby`, `Hey`, `La`, `Na`) contribute only `0.10` evidence weight and cannot activate Secondary by themselves;
- the ACTIVE gate is language-agnostic. Do not add one-off rules such as "reject Arabic on Latin text" or language-specific false-positive patches unless a later architecture decision explicitly requires them.

The default `TranslationBlockPolicy` uses 3 Core lines, a 240-character Core limit, a one-line Context Halo, and a 10-second timestamp-gap hard boundary. Blank/preserved lines and language changes also split hard groups. The current canonical model has no reliable verse/chorus marker, so the planner does not invent one.

Block text uses indexed `AALYRICS_LINE` markers. Results are accepted only when every expected marker appears exactly once and in order with nonblank mapped text. Invalid contextual output is retried in smaller Core chunks and then per line. An unrecoverable line retains its canonical text. No partial map is published.

`TranslationCoordinator` cancels superseded work and guards completion by canonical identity, target language, and a monotonic request id. A completed artifact records exactly one Translation Provider id. If one provider cannot prepare every required route or produce any acceptable translated line, the whole candidate is rejected before the next provider is tried.

## Phone presentation integration

The Phone presentation integration on `feature/translation-runtime` consumes the execution architecture above; it does not redesign it.

Production presentation path:

```text
AALyricsApplication.translationState
             ↓
       Phone lyrics mapper
             ↓
   LyricsViewport lyric rows
```

The Phone integration closes only that downstream gap:

- `PhoneRuntimeHost` observes the existing application-owned `TranslationState`;
- the app-owned Phone lyrics mapper combines canonical lyrics with Translation presentation facts;
- `:ui:phone` receives optional presentation-ready translated text per canonical lyric row plus a Phone-local Track Card Translation status;
- `:ui:phone` does not import Translation core, ML Kit, persistence, or provider execution types.

A `TranslationState.Ready` artifact is eligible for display only when current Translation settings are enabled, its `request.targetLanguage` equals the current normalized target language, and its `request.canonicalLyrics` exactly matches the canonical lyrics currently being projected. The Phone mapping reuses the canonical-identity construction from `TranslationExecutionRuntime` rather than reimplementing owner/fingerprint semantics independently. Disabled state, target mismatch, or canonical-identity mismatch fails closed to original-only presentation. This extra downstream gate prevents brief propagation windows from surfacing an old-target Ready artifact while settings changes are reaching the coordinator.

Track Card runtime feedback is a downstream presentation concern. The app-owned Phone mapper combines Translation state/settings with model lifecycle state and emits a permanent Track Card status row. Required route-model acquisition stays automatic: active download/system-wait phases are presented as `Downloading language models…`, subsequent execution as `Translating…`, Ready as a concise source → target route, and failure as `Translation failed` with one semantic Retry callback. The Retry callback is handled by `:app`: latched failed/timed-out models are retried through the model-manager boundary and the current Translation execution is republished. This does not move model management or retry policy into `:ui:phone`.

Artifact projection rules:

- canonical/source text always remains the primary displayed lyric;
- only artifact lines with `translated == true` and nonblank translated text create secondary translated presentation;
- a preserved artifact line with `translated == false` must not duplicate its canonical text;
- `Disabled`, `Idle`, `Translating`, `NotRequired`, and `Failed` all leave usable canonical lyrics visible without a Translation-specific Lyrics failure state;
- no partial line map is exposed. Phone consumes the existing atomically published `Ready` artifact only.

The translated text is additive content inside the same logical Phone lyric row. Canonical timing/current-line ownership remains unchanged. The canonical + translated pair is measured and moved as one viewport row so the existing Follow/Browse geometry remains the only scrolling authority. Translated text does not gain independent WORD progress, current-line calculation, or timing.

This slice intentionally does **not** add Android Auto Translation presentation, Musixmatch native Translation, persistent Translation Cache, new Translation algorithms, or Translation-specific Lyrics error chrome.

The executable scope and acceptance criteria are recorded in `TASK.md`; the visual row contract is defined in `docs/PHONE_LYRICS_VIEWPORT.md`.

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

`LanguageProfiler` inspects the complete canonical lyric content and produces a profile containing:

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

A Secondary candidate being detected does **not** mean it must be translated. Language ID false positives are expected at candidate level; the product protects routing by making ACTIVE promotion deliberately conservative.

Secondary activation considers only generic evidence such as:

- line coverage;
- substantive text/token coverage;
- average line-level confidence;
- contiguous runs;
- distribution across the song;
- whether the evidence is mostly short borrowed phrases such as `Oh`, `Yeah`, or `Baby`.

The implemented thresholds are documented above and remain explicit policy values covered by synthetic tests. False negatives are preferred over false-positive ACTIVE promotion because an ACTIVE Secondary changes Translation routing and model acquisition, while an INCIDENTAL candidate remains diagnostic-only.

Routing intent:

```text
PRIMARY, model-supported          -> translation eligible
SECONDARY / ACTIVE, model-supported -> translation eligible
PRIMARY or ACTIVE Secondary, unsupported -> preserve original
SECONDARY / INCIDENTAL            -> preserve original
UNCERTAIN                         -> preserve original
TARGET LANGUAGE                   -> preserve original
```

For the current alpha product policy, the Translation-model-supported language set is the same nine-language set exposed for targets: `EN / JA / FR / DE / ES / KO / ZH / IT / PT`. Language identification may still report other normalized languages (for example Arabic), but detection does not imply model support and must not by itself trigger model preparation outside this product set.

A third language is not promoted into another routing lane in the initial design. Small or uncertain third-language passages remain original unless a later explicit decision expands the model.

The governing principle is:

> Translate meaningful foreign-language passages, not every foreign-language token.

## Context-aware block translation

AALyrics uses contextual blocks where deterministic line ownership can be preserved.

Block planning uses the available canonical evidence for:

### Hard boundaries

- language change;
- verse/chorus or equivalent structural boundary where available;
- large timestamp gap;
- explicit blank separation.

### Soft boundaries

- preferred block size;
- character/token limit.

Hard boundaries are normally not crossed for context. Soft boundaries may use adjacent context.

The current threshold policy is documented above. Verse/chorus boundaries remain unused until canonical lyrics expose reliable structural evidence.

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

The implementation degrades through smaller blocks and ultimately per-line translation:

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

The execution slice now defines the minimal provider id, route, and prepared-session contracts required by adaptive block fallback. Artifact assembly and provider selection remain in pure Translation core.

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

The background scaffold implements ML Kit model lifecycle:

- target-language model planning;
- model availability checks;
- startup reconciliation against ML Kit's persisted downloaded-model inventory;
- model download;
- active-download reuse;
- failure/timeout state that remains latched until an explicit retry;
- thermal waiting;
- timeout based on active rather than thermally blocked download time.

The concrete ML Kit manager's in-memory lifecycle map is process-local, while ML Kit language packs can survive a normal app process restart or an Android Studio update install. On manager startup, supported non-English targets therefore begin in CHECKING state while `RemoteModelManager.getDownloadedModels(...)` restores which packs are actually present. Downloaded packs become READY; absent packs fall back to NOT_DOWNLOADED presentation. A missing process-local state entry must not by itself be treated as evidence that a previously downloaded ML Kit pack was removed. `TranslationModelManager.inventoryReconciled` becomes true only after the startup persisted-model inventory pass succeeds; Details uses that fact before interpreting a missing remote-model entry as confirmed absence.

The execution slice adds actual text/block translation and LanguageProfiler integration while reusing this model lifecycle unchanged.

ML Kit infrastructure belongs outside pure Translation contracts because it depends on Android/Google SDK APIs.

## Settings ownership

Translation preferences are application/capability state, not a foreground View responsibility.

The background scaffold persists:

- Translation enabled/disabled;
- selected target language.

For application persistence, Translation is **opt-in**: when no persisted user choice exists, `translation_enabled` resolves to disabled. English remains the built-in/default target and may be prepared/selectable without enabling Translation execution. Existing installs with an explicit persisted enabled value keep that user choice.

The first Phone Settings presentation contract is defined in `docs/PHONE_SETTINGS.md`.

Foreground Settings must consume/update the existing Translation settings boundary through application-provided presentation state and callbacks rather than owning SharedPreferences keys directly. `:ui:phone` must not add a direct dependency on the concrete SharedPreferences store or ML Kit lifecycle in order to render these rows.

The Settings presentation may show model readiness and emit explicit manual preparation/retry requests. Application/runtime wiring maps those presentation requests onto `TranslationModelManager.ensureAvailable` / `retry` and maps lifecycle phases back into presentation state. When a model is `FAILED` or `TIMED_OUT`, the manager's diagnostic `error` may be adapted into presentation-ready failure text for an on-demand Settings tooltip; raw engine exceptions remain outside `:ui:phone`. The UI must not invoke the concrete ML Kit manager directly.

English is the built-in model language in the current ML Kit adapter. It requires no remote language-pack download and should present as ready without network preparation. ML Kit explicitly treats English as built in rather than a downloadable/deletable remote model. Built-in readiness is capability availability only; it must not be interpreted as Translation being enabled.

Advanced Settings may explicitly clear downloaded Translation models through the abstract `TranslationModelManager` boundary. The concrete ML Kit adapter enumerates engine-managed downloaded `TranslateRemoteModel` instances and deletes them through `RemoteModelManager`; `:ui:phone` does not depend on ML Kit. Before cleanup, application-owned Translation settings return to their safe defaults: Translation OFF and Target language English. The built-in English capability remains available. Cleanup also clears stale non-English model lifecycle presentation after successful deletion.

Model cleanup must be race-safe with process-level model preparation. Cleanup start and preparation registration share a lifecycle barrier/generation so a preparation cannot pass the cleanup snapshot in the pre-monitor window and then start a surviving download afterward. Any language with an active download task or monitor when cleanup begins is marked for deletion; if that model finishes after the initial inventory pass, its download-success path deletes it instead of publishing a surviving READY model.

The user-confirmed cleanup runs in the application-owned process scope rather than a foreground Composable scope, so destination changes or Activity recreation do not cancel it. The application owns a small IDLE/RUNNING/FAILED cleanup lifecycle; immediate inventory/deletion failures propagate through that lifecycle so Settings can show an explicit retryable failure rather than silently treating cleanup as complete.

Changing or resetting Translation settings must not refetch lyrics providers merely because Translation configuration changed.

## Background target-model preparation

When Translation is enabled, the application may prepare the selected target model independently of foreground UI.

English requires no downloaded ML Kit language pack.

Preparing a target model does not imply that source-language models are known yet. Once `LanguageProfiler` resolves an actual route, Translation execution ensures any additional route-specific model(s).

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

## Details diagnostic evidence

Phone Details consumes Translation diagnostics as a read-only projection of existing Translation work. It must not run LanguageProfiler independently, open Translation sessions, prepare models, or retain stale per-track evidence merely to populate Details.

The compact Details contract requires two pieces of execution evidence that the original atomic presentation state did not preserve in every phase:

1. the current request's authoritative `LanguageProfile` once profiling has completed, including while the request is still Translating or later fails; and
2. a framework-neutral failure diagnostic when `TranslationState.Failed` is published.

The implementation extends the existing Translation lifecycle state rather than creating an adjacent parallel runtime. `TranslationState.Translating` carries an optional current-request `LanguageProfile` after profiling completes; `TranslationState.Failed` carries that profile when available plus a stable `TranslationFailureReason`. The ownership rules remain fixed:

- profiling remains performed exactly once by the existing Translation execution path;
- Details reuses that profile; it does not re-profile lyrics;
- profile evidence is keyed to the same canonical identity + target/request ownership as Translation execution and is cleared/superseded with that request;
- `Ready` continues to obtain its profile from the atomic artifact and `NotRequired` from its existing profile;
- a Translating state may expose the profile after profiling has completed without implying that a partial Translation artifact is displayable;
- a Failed state may expose the current profile when failure happened after profiling;
- runtime failure diagnostics use `TranslationFailureReason`: `LANGUAGE_PROFILING_FAILED`, `TRANSLATION_PLANNING_FAILED`, `PROVIDER_EXECUTION_FAILED`, or `UNEXPECTED`; raw ML Kit/Google exceptions must not cross into `:ui:phone`;
- model-specific failure/timeout details continue to come from `TranslationModelState.error` through application-owned presentation mapping.

The exact internal Kotlin shape is implementation-level, but it must support a Phone-local Details projection equivalent to:

```text
current request identity
current LanguageProfile?      // null before/if profiling unavailable
TranslationFailureReason?     // present for Failed
model lifecycle states        // existing model-manager boundary
```

This diagnostic evidence is observational only. It must not change cancellation, provider fallback, atomic artifact publication, or retry behavior.

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

The Translation execution lifecycle distinguishes disabled, idle, translating, not-required, ready, and failed. Phone Details renders those as `Disabled`, `Idle`, `Translating`, `Not required`, `Ready`, and `Failed`; it does not invent extra runtime states.

For model diagnostics, built-in capability and confirmed downloaded inventory are both `Ready`. Details-only `Not required` means Translation is OFF and the relevant remote model is confirmed absent, so no preparation is currently required. It must not be used as a synonym for built-in or "not used by this exact route."

Stable rules:

- original lyrics remain usable during all Translation work;
- Translation or model failure is not lyrics lookup failure;
- an engine exception during meaningful language identification is a Translation failure, not a false `NotRequired` result; genuinely undetermined/uncertain text may still be preserved without failure;
- target-language changes may restart Translation without refetching Lyrics Providers;
- stale work cannot publish after lyrics/target/request supersession;
- foreground presentation does not own the model download lifecycle.

## Timing and karaoke relationship

Translation does not own provider timestamps, calibration, playback position, or karaoke progression.

Translated text references canonical lyric line identity; timing truth remains in the lyrics/timing domains.

WORD timing does not imply word-by-word Translation.

## Scaffold boundary (historical)

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

Those exclusions defined Phase 11.2a only. Phase 11.2b subsequently implemented the execution responsibilities described in "Current execution implementation" while preserving the scaffold's settings and model-lifecycle ownership. The active Phase 11.2c now explicitly authorizes **Phone** Translation presentation integration under the downstream rules above; Android Auto Translation presentation remains deferred.

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
