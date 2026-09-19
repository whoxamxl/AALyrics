# Translation Background Scaffold

## Branch and baseline

- Branch: `feature/translation-scaffold`.
- Base: `main` at `7fe0c84372a11fb898cd0fc6e2036c4c88ab6326` after PR #40 merged.
- Classification: **TRANSLATION BACKGROUND SCAFFOLD**.
- Authoritative references: `AGENTS.md`, `docs/LYRICS_PIPELINE_ARCHITECTURE.md`, `docs/TRANSLATION_ARCHITECTURE.md`, and `docs/MIGRATION_INVENTORY.md`.
- The user explicitly authorized documentation updates and background/scaffold implementation, but not the large translation algorithm implementation.

## Goal

Prepare the production Translation capability so a later implementation slice can add language profiling, contextual block translation, Translation Provider selection, and presentation wiring without reopening ownership boundaries.

This slice may implement background-only infrastructure that does not alter the unfinished Phone or Android Auto foreground:

- translation settings state/persistence;
- the approved target-language set and normalization;
- ML Kit language-model planning, availability checks, download/retry monitoring, and background target-model preparation;
- minimal capability contracts justified by those concrete background responsibilities;
- tests and architecture guardrails for the new boundaries.

## Migration rule

The current `whoxamxl/auto-lyrics` fork at `v1.13.0` remains the implementation reference.

Preserve or refactor mature behavior instead of rewriting it:

- **PRESERVE** the nine-language target set and normalization semantics where applicable.
- **REFACTOR** the proven ML Kit model download/reuse/thermal-wait/cancellation behavior out of the legacy monolithic `LyricsTranslator`.
- **REFACTOR** translation settings persistence out of legacy View/MediaTracker ownership.
- Do not import legacy foreground Views, the legacy monolithic `MediaTracker` ownership model, or the old first-five-lines source-language detector.

New decisions from the current Translation design discussion are documented as contracts/invariants only unless they are required by the scaffold. Do not prematurely implement heuristic thresholds, contextual block planning, Musixmatch Translation alignment, or Translation Provider winner logic in this branch.

## Accepted Translation direction

- Original canonical lyrics remain immutable and authoritative.
- Lyrics Provider selection and Translation Provider selection remain independent.
- Primary and Secondary language profiling will be derived from the complete canonical lyrics, not Provider language metadata and not `take(5)`.
- A detected Secondary language does not automatically become translatable. Incidental and uncertain foreign-language text remains original by default.
- Future contextual translation uses block boundaries plus non-overlapping authoritative Core spans and optional overlapping Context Halos.
- Every translated lyric line has exactly one authoritative Core owner.
- Translation results publish atomically; partial blocks do not replace text in front of the user.
- Persistent Translation Cache is forbidden through stable `v1.0.0`; after `v1.0.0` it remains disabled unless explicitly authorized.
- One completed Translation Artifact uses one Translation Provider unless a later explicit decision permits provider mixing.
- The unexplained Korean -> Japanese case that produced an English output remains diagnostic evidence only; do not add speculative corrective routing in this scaffold.

## Explicitly deferred to the large implementation slice

- LanguageProfiler algorithm and thresholds.
- Primary/Secondary/INCIDENTAL/UNCERTAIN routing implementation.
- Context block splitting and Core + Context Halo planner.
- Line-marker/alignment validation and block-to-line fallback.
- TranslationCoordinator lifecycle and canonical-lyrics identity binding.
- Concrete Translation Provider resolver/selector.
- Musixmatch native Translation adapter/alignment.
- ML Kit block/text translation execution.
- Translation Artifact publication to Phone/Android Auto.
- Translation foreground Settings UI/status UI.
- Persistent Translation Cache.

## Acceptance criteria

- [x] Update Translation architecture and roadmap with the approved policy.
- [x] Re-check current Auto-Lyrics Translation implementation before code migration.
- [x] Add a pure Translation API/configuration module only for contracts required by this scaffold.
- [x] Add an Android ML Kit adapter module for model lifecycle only.
- [x] Preserve target-language/model-download behavior by refactoring mature fork logic.
- [x] Add app-owned translation settings persistence without adding Settings UI.
- [x] Start background target-model preparation from persisted settings without touching Lyrics/UI state.
- [x] Add deterministic tests for language normalization/model planning/background settings reaction.
- [x] Extend architecture guardrails to cover the new pure Translation boundary.
- [x] Run CI and bounded review; final-head CI is required before merge.
- [x] Open PR #41 and stop before merge for explicit approval.

## Scope guard

This branch must not change lyrics lookup/provider ranking, canonical lyrics, Phone LyricsViewport rendering, Android Auto rendering, media-session behavior, timing/karaoke behavior, or introduce persistent Translation Cache.