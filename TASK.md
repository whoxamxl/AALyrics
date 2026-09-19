# Translation Execution / Orchestration

## Branch and baseline

- Branch: `feature/translation-execution`.
- Base: `main` at `3484fbcde2b685da351d9f2c0de068c589956d63` after PR #41 merged.
- Classification: **TRANSLATION EXECUTION / ORCHESTRATION**.
- Authoritative references:
  - `AGENTS.md`
  - `docs/ARCHITECTURE.md`
  - `docs/LYRICS_PIPELINE_ARCHITECTURE.md`
  - `docs/TRANSLATION_ARCHITECTURE.md`
  - `docs/MIGRATION_INVENTORY.md`
- The Translation background scaffold is complete and merged.
- This branch is the next large implementation slice intended for Codex-assisted implementation.

## Goal

Implement production Translation execution over canonical lyrics while preserving the architecture fixed by PR #41.

The result should be capable of:

1. profiling the complete canonical lyric document;
2. resolving one Primary language and at most one meaningful Secondary candidate;
3. preserving incidental/uncertain foreign-language text instead of aggressively translating it;
4. planning contextual Translation blocks with exactly one authoritative owner per lyric line;
5. executing Translation through an independent Translation Provider layer;
6. validating structural alignment and degrading safely when block Translation cannot be mapped back to canonical lines;
7. assembling one complete Translation Artifact;
8. rejecting stale work;
9. publishing Translation atomically downstream.

This branch must not make Translation availability influence Lyrics Provider ranking or mutate canonical lyrics.

## Migration rule

Use the mature `whoxamxl/auto-lyrics` fork as implementation reference where behavior already exists.

### Preserve / Refactor

- existing target-language semantics;
- ML Kit source/target route setup;
- model lifecycle integration already extracted into `:translation:mlkit`;
- cancellation semantics;
- stale-result rejection ideas;
- whole-result publication rather than line-by-line foreground replacement;
- original-line fallback behavior only where it remains compatible with the new structural validation policy.

### Do not preserve

- first-five-nonblank-lines source-language detection;
- one global Source Language assumption;
- fully context-free per-line Translation as the primary path;
- legacy `MediaTracker` ownership;
- legacy foreground Translation Views.

### New AALyrics behavior

The following decisions are AALyrics-specific and must be implemented from the approved architecture rather than copied from the fork:

- complete-document LanguageProfiler;
- Primary / Secondary candidate model;
- Secondary activation states;
- INCIDENTAL / UNCERTAIN preservation;
- contextual block planning;
- Core + Context Halo ownership;
- Translation Provider separation;
- structural validation / split / per-line fallback;
- atomic Translation Artifact publication.

## Language profiling

The profiler must inspect the complete canonical lyric content.

Conceptual result:

```text
LanguageProfile
├─ primary
├─ secondaryCandidate?
└─ secondaryActivation
      ├─ NONE
      ├─ INCIDENTAL
      └─ ACTIVE
```

Provider language/region metadata must not control runtime routing.

### Primary

Primary is the dominant meaningful language of the canonical lyric document.

Do not derive it from only the first N lines.

Use evidence appropriate for lyrics, including where useful:

- script evidence;
- aggregate Language ID evidence;
- meaningful text amount;
- local/sliding context;
- short-line confidence handling.

Avoid treating very short lyric tokens as strong standalone evidence.

### Secondary

At most one Secondary candidate participates in v1 Translation routing.

Detecting a Secondary candidate does **not** automatically activate Translation for it.

Activation should consider combined evidence such as:

- line coverage;
- substantive character/token coverage;
- contiguous meaningful runs;
- distribution across the song;
- whether evidence consists mostly of short borrowed phrases.

Short/common insertions such as `Oh`, `Yeah`, `Baby`, etc. should contribute little evidence.

The exact thresholds must be implemented as named/testable policy rather than scattered magic numbers.

### Routing

```text
PRIMARY
  -> Translation eligible unless already target language

SECONDARY + ACTIVE
  -> Translation eligible at meaningful block scope

SECONDARY + INCIDENTAL
  -> preserve original

UNCERTAIN
  -> preserve original

TARGET LANGUAGE
  -> preserve original
```

Do not add a third active source-language lane in this slice.

A small third-language passage should remain original unless a future explicit decision expands the design.

## Context-aware block planning

The planner should form contextual blocks from canonical lyric lines.

Potential hard boundaries:

- language change;
- explicit blank separation;
- large timestamp gap;
- verse/chorus or equivalent structural boundary when reliable evidence exists.

Potential soft boundaries:

- preferred block size;
- character/token limit.

Hard boundaries should normally not be crossed for context.

Soft-boundary splits may use adjacent context.

The exact thresholds must be testable and documented.

## Core + Context Halo

Every translatable canonical lyric line must have exactly one authoritative Core owner.

Context may overlap.

Example:

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

Authoritative ownership:

```text
L12 -> Block A
L13 -> Block B
```

A Halo result must never overwrite another block's Core result.

The planner should make ownership deterministic before Translation starts.

## Translation Provider architecture

Do not reuse `:provider:api` for Translation.

Translation Providers are a separate capability.

The implementation should introduce only the smallest contracts required by this slice, for example conceptual roles such as:

```text
TranslationProvider
TranslationRequest
TranslationCandidate / TranslationArtifact
TranslationProviderResolver
```

Exact names are not fixed by this task if a smaller/cleaner contract is demonstrated.

### Required v1 provider behavior

Google ML Kit is the required local Translation Provider.

Musixmatch native Translation is optional in this branch unless its current endpoint, access/entitlement, payload semantics, and canonical-source alignment can be verified confidently.

If Musixmatch implementation is not sufficiently verified, leave its provider seam ready and keep the branch shippable with ML Kit only.

### Lyrics/Translation provider independence

These are both valid:

```text
Lyrics Provider: LRCLIB
Translation Provider: ML Kit
```

```text
Lyrics Provider: Musixmatch
Translation Provider: ML Kit
```

and, if later verified:

```text
Lyrics Provider: LRCLIB
Translation Provider: Musixmatch
```

Translation availability must never raise a Lyrics Candidate score.

## One provider per Translation Artifact

Do not mix Translation Providers line-by-line in this slice.

A completed Translation Artifact must have one Translation Provider provenance.

If a provider cannot produce an acceptable complete artifact, reject it and try the next provider.

Do not produce:

```text
85% provider A
15% provider B
```

unless a future explicit decision changes the policy.

## ML Kit Translation execution

Use the existing `:translation:mlkit` model lifecycle instead of reimplementing model handling.

Actual lyric Translation execution may now be added behind the Translation Provider boundary.

Requirements:

- route preparation uses the existing model manager;
- source == target should not translate;
- cancellation propagates correctly;
- one block failure must not corrupt canonical lyrics;
- model failure/timeout respects the explicit retry gate already implemented;
- no foreground View owns translator/model lifecycle.

## Structural alignment and fallback

Context translation is only valid if translated output can be mapped back to canonical Core lines deterministically.

A block result must be validated before acceptance.

A marker/newline strategy may be used, but do not assume an engine preserves delimiters without tests.

Required degradation path:

```text
context block
   ↓ invalid structure/alignment
smaller block
   ↓ invalid again
per-line fallback
```

Per-line fallback exists as a reliability path, not the primary Translation strategy.

If a line still cannot be translated reliably, preserve its original text rather than inventing or shifting neighboring Translation.

## Atomic publication

Do not expose partial block results.

```text
Canonical Lyrics
    ↓
LanguageProfile
    ↓
TranslationPlan
    ↓
all required block/provider work
    ↓
validation/fallback
    ↓
complete TranslationArtifact
    ↓
atomic publication
```

Until the artifact is complete, original lyrics remain the usable content.

The user must not see one Translation replaced by another as overlapping blocks finish.

## Identity and stale-result protection

Translation work must be bound to the exact:

- canonical lyric identity;
- selected target language;
- active Translation request generation/identity.

A completed old request must not publish after:

- track change;
- canonical lyrics replacement;
- target-language change;
- Translation disabled;
- newer Translation request.

Reuse existing AALyrics stale-result patterns where appropriate rather than inventing an unrelated mechanism.

## Persistent Translation Cache

Do **not** add persistent Translation Cache.

Policy remains:

- prohibited through stable `v1.0.0`;
- after `v1.0.0`, still disabled unless explicitly authorized.

Transient in-memory state for one active request/provider/model operation is allowed.

## Unexpected English output diagnostic

A Korean -> Japanese Translation previously produced an unexplained English line.

Do not add speculative special-case routing solely for that observation.

If practical, add debug-only anomaly logging that makes future reproduction diagnosable without changing output policy.

For example, suspicious cases may log:

- expected source route;
- target;
- canonical line index;
- output-script/language evidence.

Do not expose real lyric text unnecessarily in release logs.

## Foreground boundary

This branch may expose the smallest application/domain Translation state required for later Phone/Android Auto consumption.

Do not redesign the unfinished Phone or Android Auto UI in this slice.

Specifically do not:

- add new visual Settings rows unless separately authorized;
- redesign LyricsViewport;
- change Android Auto layouts;
- make presentation invoke Translation Providers directly.

Foreground integration should be limited to the minimum seam necessary to prove atomic Translation publication if that is required by the production runtime.

## Tests

Add deterministic coverage for at least:

- whole-document Primary detection;
- Primary not being determined by only the first five lines;
- meaningful bilingual Primary + Secondary ACTIVE case;
- incidental short foreign phrases remaining INCIDENTAL;
- uncertain text preservation;
- target-language no-op;
- block hard/soft boundaries;
- Core ownership uniqueness;
- Context Halo overlap without authoritative conflict;
- structural alignment success;
- structural alignment failure -> smaller-block fallback;
- final per-line fallback;
- stale request rejection;
- target change cancellation/replacement;
- one-provider-per-artifact provenance;
- ML Kit route/model preparation integration;
- failure preserving canonical lyrics.

Use synthetic lyrics in tests. Do not add copyrighted song lyrics.

## Architecture guardrails

Extend executable architecture checks only for concrete boundaries introduced here.

At minimum preserve:

- `:translation:api` remains Android/network independent;
- UI does not depend on concrete `:translation:mlkit`;
- Translation Providers remain separate from Lyrics Provider API;
- Lyrics provider/selection modules do not depend on Translation execution;
- concrete Translation engines do not leak into `:core:lyrics`.

Do not turn shell guards into a general Kotlin parser.

## Acceptance criteria

- [ ] Re-check current Auto-Lyrics Translation execution before migration.
- [ ] Define the smallest justified Translation execution/provider contracts.
- [ ] Implement complete-document LanguageProfiler.
- [ ] Implement Primary + Secondary candidate/activation policy.
- [ ] Preserve INCIDENTAL and UNCERTAIN text.
- [ ] Implement contextual block planning.
- [ ] Guarantee one Core owner per translatable line.
- [ ] Implement Core + Context Halo execution semantics.
- [ ] Implement ML Kit as a Translation Provider using existing model lifecycle.
- [ ] Implement structural validation and split/per-line fallback.
- [ ] Implement complete Translation Artifact assembly.
- [ ] Implement request identity/stale-result rejection.
- [ ] Publish Translation atomically.
- [ ] Keep Lyrics Provider selection independent.
- [ ] Keep one Translation Provider per artifact.
- [ ] Do not introduce persistent Translation Cache.
- [ ] Add deterministic regression coverage.
- [ ] Extend architecture guardrails only as justified.
- [ ] Run CI and bounded review.
- [ ] Open a PR and stop before merge for explicit approval.

## Scope guard

This branch owns Translation execution/orchestration only.

Do not use it to implement persistent Translation Cache, redesign Phone/Android Auto presentation, alter Lyrics Provider scoring, change timing/calibration semantics, change karaoke projection, or broaden the product language list without an explicit decision.
