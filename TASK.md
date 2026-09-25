# Phone Translation Integration

## Branch and baseline

- Branch: `feature/translation-runtime`.
- Base: `main` at `4083588a250092e47f1efeb01e06a099b72a3604`.
- Classification: TRANSLATION / PHONE PRESENTATION / RUNTIME COMPOSITION.
- Status: Phone Translation works on-device once required route models are available. Device testing confirmed missing route models auto-download in the background; the active follow-up is a permanent Track Card Translation status row that makes model download, Translation execution, active route, and failure/retry visible without changing Track Card height between Translation states.
- Authoritative references: `AGENTS.md`, `docs/TRANSLATION_ARCHITECTURE.md`, `docs/PHONE_LYRICS_VIEWPORT.md`, `docs/PHONE_RUNTIME_HOST.md`, `docs/PHONE_UI_SPEC.md`, `docs/PRESENTATION_STATE_ARCHITECTURE.md`, and the current production code/tests on this branch.

## Goal

Connect the already-implemented atomic Translation execution result to the Phone Lyrics presentation without changing Translation algorithms, canonical lyrics ownership, playback timing, or provider selection.

The intended production path is:

```text
LyricsState ---------------------------┐
TranslationCoordinator.state ----------┤
TranslationSettings --------------------┼─> Phone lyrics mapper
PlaybackSnapshot -----------------------┤
Phone viewport interaction/settings ----┘
                                         ↓
                                LyricsScreenUiState
                                         ↓
                                  LyricsViewport
                                         ↓
                         canonical text + optional
                          translated secondary text
```

This is a presentation-integration slice, not a Translation-engine rewrite.

## Established baseline

The following already exists and is the baseline to preserve:

- `:translation:api`, `:translation:core`, and `:translation:mlkit` are implemented.
- `TranslationExecutionRuntime` observes completed canonical `LyricsState` plus persisted Translation settings and updates `TranslationCoordinator`.
- `AALyricsApplication.translationState` exposes the coordinator's atomic `StateFlow<TranslationState>`.
- `TranslationCoordinator` already owns profiling, contextual block planning, provider execution, fallback, stale-request rejection, cancellation, and atomic publication.
- `TranslationArtifact` contains exactly one line entry for each canonical lyric line and preserves canonical line index identity.
- `TranslationArtifactLine.translated` distinguishes an actual translated line from a preserved canonical line.
- Translation Settings, target model lifecycle, manual download/retry, model cleanup, and Reset semantics are already implemented.
- Phone `PhoneRuntimeHost` currently does **not** collect `translationState`.
- `mapPhoneLyricsState` currently maps canonical lyrics only.
- `LyricsViewportLineUiState` currently has canonical `text` and optional karaoke `words`, but no translated presentation field.
- `LyricsViewport` currently renders one canonical text block per lyric row.

## Scope

Implement Phone Translation presentation from the existing application-owned `TranslationState`.

### Runtime composition

- Collect `application.translationState` lifecycle-aware in `PhoneRuntimeHost`.
- Reuse the already-collected current `application.translationSettings` as part of presentation eligibility.
- Pass the current Translation state and Translation settings into the Phone lyrics presentation mapper.
- Do not make `:ui:phone` depend on Translation core, ML Kit, persistence, providers, or Android framework Translation objects.
- Keep Translation execution application/capability-owned. The Phone host observes state; it does not start or own translator jobs.

### Translation presentation eligibility gate

A `TranslationState.Ready` artifact may be presented only when **all** of the following are true:

- current persisted Translation settings are enabled;
- the artifact `request.targetLanguage` equals the current normalized `TranslationSettings.targetLanguage`;
- the artifact `request.canonicalLyrics` exactly matches the canonical lyrics currently being mapped.

Reuse or extract the existing canonical-identity construction used by `TranslationExecutionRuntime`; do not create a second subtly different owner/fingerprint algorithm.

A stale/mismatched artifact, an old-target artifact during target switching, or a Ready state observed during disable propagation fails closed to original-only presentation. Playback position or current-line changes do not change Translation identity. A target-language change may supersede/restart Translation without refetching Lyrics Providers.

### Phone line projection

Extend the Phone-local lyric-row presentation model with optional translated text.

For every canonical line:

- canonical/source text remains authoritative and always remains the primary text;
- if the matching `TranslationArtifactLine` has `translated == true` and nonblank translated text, expose that text as the row's translated secondary text;
- if `translated == false`, do not render the artifact text a second time;
- `Disabled`, `Idle`, `Translating`, `NotRequired`, `Failed`, missing state, and identity mismatch all render valid original lyrics normally with no translated secondary text;
- Translation failure must never map the Lyrics destination to loading/not-found/failed or otherwise hide usable canonical lyrics;
- no partial Translation map is presented. Presentation consumes only the existing atomic `Ready` artifact.

Do not add a Translation status/error banner inside `LyricsViewport`. Translation runtime feedback belongs to the Track Card's dedicated permanent status row defined below.

### Track Card Translation status

Reserve one permanent fourth status row in the Lyrics Track Card for Translation state. The row exists in every Translation state so toggling Translation, downloading models, completing Translation, or failing/retrying does not move the Track Card boundary or LyricsViewport.

Approved presentation states:

- OFF -> `Translation off`;
- enabled but no active canonical route yet -> `Translation enabled`;
- required route models are checking/downloading/waiting for the system -> compact spinner + `Downloading language models…`;
- route models are ready and Translation execution is still running -> compact spinner + `Translating…`;
- an eligible atomic artifact is active -> `Translation EN → JA`-style short source/target labels;
- Translation determines no work is required -> `Translation not required`;
- current Translation attempt fails -> `Translation failed` plus the shared Retry icon and compact trailing `Retry` text action.

Model acquisition remains automatic. Do not add a normal pre-download confirmation dialog. The user intervention path is failure recovery: the Track Card emits one semantic Retry action and the application/capability layer decides which failed model/route work must be retried. Canonical lyrics remain visible and usable throughout model preparation, Translation execution, and failure.

The Track Card status row is presentation feedback only. It must not become a second Translation executor, model manager, or timing owner. Translation runtime colors stay distinct from the cyan provider/sync row: Enabled plus processing states use AccentBlue, Ready uses Success, inactive states use neutral text colors, failure uses Error, and Retry retains AccentCyan.

### LyricsViewport rendering

Render translated text as additive secondary content inside the same logical lyric row.

Stable visual/geometry contract:

- preserve the existing canonical source typography, WORD/LINE/PLAIN behavior, current-line emphasis, and source word-progress behavior;
- translated text is a chrome-free typographic annotation: no cards, backgrounds, badges/pills, language labels, icons, separators, or dividers;
- translated text appears directly below the canonical line with an initial 4dp intra-row gap;
- use an initial translated-text target of approximately 15sp Medium / compact supporting line height / TextSecondary-class emphasis at about 0.76 local opacity, while keeping canonical typography unchanged;
- translated text never receives independent word highlighting, independent current-line logic, or separate current/past/future focus animation;
- the canonical + translated pair is measured as one row;
- for timed lyrics, the complete row receives the existing shared focus scale/alpha transform as one unit;
- follow/browse scroll calculations use the measured height/center of that complete row;
- the existing approximately 45% timed focus target, opening `♪` row, edge fades, return-to-playback behavior, and PLAIN auto-scroll contract remain intact;
- Translation appearing atomically may change row heights, but must not introduce a second scroll/timing owner;
- do not animate row height merely to reveal Translation; if an appearance transition is used, re-measure geometry immediately and limit animation to a short translated-text alpha fade (initial target about 150ms).

### Tests and Previews

Add focused coverage for at least:

- matching `Ready` artifact projects translated text onto the correct canonical rows;
- `translated == false` does not duplicate canonical text;
- stale/mismatched canonical identity is ignored;
- a Ready artifact for an old target language is ignored;
- current Translation OFF suppresses translated presentation even if an older Ready state is still observed during propagation;
- `Disabled`, `Translating`, `NotRequired`, and `Failed` remain original-only;
- translation state does not change current-line index, sync type, provider label, or lyrics-status mapping;
- Ready/Degraded canonical lyrics follow the same identity/presentation rules;
- translated row rendering for LINE and PLAIN;
- long/wrapped translated text;
- mixed artifact containing translated and preserved lines;
- narrow width and enlarged-font presentation where practical.

Existing non-Translation Phone lyrics mapper/viewport tests must continue to pass.

## Scope guardrails

Do **not** implement or redesign any of the following in this slice:

- LanguageProfiler heuristics or thresholds;
- contextual block planning, marker parsing, fallback, or artifact assembly;
- ML Kit model lifecycle or Translation provider execution;
- Musixmatch native Translation;
- Translation Provider selection UI;
- persistent Translation Cache;
- lyrics-provider ranking, lookup, or retry behavior;
- timing/calibration or Sync behavior;
- Karaoke/WORD Translation semantics;
- Android Auto Translation presentation;
- new durable Translation settings;
- Translation error UI beyond the approved Track Card `Translation failed` + `Retry` recovery row.

If implementation evidence reveals a real defect in the existing Translation execution path, record it separately rather than silently expanding this presentation PR unless it directly blocks the stated acceptance criteria.

## Expected implementation touch points

The likely production/test files are:

- `app/src/main/java/io/github/whoxamxl/aalyrics/PhoneRuntimeHost.kt`
- `app/src/main/java/io/github/whoxamxl/aalyrics/PhoneLyricsMapper.kt`
- `app/src/main/java/io/github/whoxamxl/aalyrics/TranslationExecutionRuntime.kt` only if extracting/reusing canonical identity mapping is the cleanest option
- `ui/phone/src/main/java/io/github/whoxamxl/aalyrics/ui/phone/lyrics/LyricsUiState.kt`
- `ui/phone/src/main/java/io/github/whoxamxl/aalyrics/ui/phone/lyrics/LyricsViewport.kt`
- `app/src/test/java/io/github/whoxamxl/aalyrics/PhoneLyricsMapperTest.kt`
- `ui/phone/src/debug/java/io/github/whoxamxl/aalyrics/ui/phone/preview/LyricsViewportPreviews.kt`
- `ui/phone/src/debug/java/io/github/whoxamxl/aalyrics/ui/phone/preview/LyricsScreenPreviews.kt` where useful

This list is guidance, not permission to restructure unrelated code.

## Implementation checkpoints

Use small, reviewable commits and keep each checkpoint independently coherent.

1. [x] **Presentation contract and mapper**
   - add optional translated text to the Phone-local lyric-row state;
   - accept Translation state plus current Translation settings in `mapPhoneLyricsState`;
   - enforce enabled + target-language + canonical-identity matching;
   - project only `translated == true` artifact lines;
   - add mapper tests;
   - do not change Compose rendering yet.

2. [x] **Runtime connection**
   - lifecycle-collect `application.translationState` in `PhoneRuntimeHost`;
   - feed it to the mapper;
   - verify no Translation execution ownership moves into Phone UI.

3. [x] **LyricsViewport rendering**
   - render canonical + optional translation as one measured row;
   - preserve all existing sync/focus/browse/PLAIN behavior;
   - keep source typography/word progress unchanged;
   - add focused viewport helper tests if geometry helpers change.

4. [x] **Preview coverage**
   - add deterministic translated LINE/PLAIN and mixed translated/preserved fixtures;
   - include long wrapping and narrow/enlarged-font cases where the current Preview structure supports them;
   - verify the secondary hierarchy does not overpower canonical lyrics.

5. [x] **Regression and documentation alignment**
   - re-check Phone lyrics mapper/viewport behavior with Translation OFF and unavailable;
   - align implementation details back into the Translation/Phone docs only where implementation evidence required a change;
   - verify no stale documentation still describes Phone Translation presentation as unimplemented after the code lands.

6. [ ] **Track Card Translation runtime feedback**
   - [x] document the permanent fourth-row state contract;
   - [x] prepare Phone-local presentation state/rendering and deterministic Previews;
   - [ ] map live Translation + model lifecycle state into the Track Card without moving execution ownership;
   - [ ] wire the semantic Retry callback through `:app`;
   - [ ] add focused mapper/runtime coverage.

7. [ ] **Validation before PR readiness**
   - run the repository architecture checks;
   - run focused Translation/Phone unit tests;
   - run the normal JVM/unit test suite required by the repository;
   - build the debug APK;
   - inspect the complete branch diff for scope/regressions;
   - run bounded Codex review under `AGENTS.md`;
   - perform a physical-device smoke test for Translation ON/OFF and at least one actual translated song before merge.

Local validation on the implementation head:

- [x] `scripts/verify-architecture.sh` passed.
- [x] Focused `PhoneLyricsMapperTest` passed.
- [x] Repository `test` task passed.
- [x] `:app:assembleDebug` passed.
- [x] Final implementation diff and commit-message CI gate checked; no accidental scope expansion found.
- [x] One bounded local Codex review found no actionable correctness issue.
- [x] Physical-device Translation ON/OFF and actual translated-song smoke test confirmed translated rows appear once the required source/target route models are available.
- [x] Device finding documented and Target language info tooltip added: both source and target language models are required for a route; English is built in.
- [x] Missing-model UX decision revised after device testing: keep automatic model acquisition; use the permanent Track Card status row instead of a normal pre-download dialog.
- [ ] Live Track Card model-download / translating / route / failed-retry mapping remains to be implemented after the Doc + Preview checkpoint.

Do not merge without explicit user authorization.

## Acceptance criteria

The slice is complete when all of the following are true:

- Translation OFF keeps canonical lyrics behavior unchanged and the permanent Track Card Translation row reads `Translation off`.
- While Translation is pending or fails, canonical lyrics remain visible and usable; the Track Card alone exposes `Downloading language models…`, `Translating…`, or `Translation failed` + `Retry` without turning Lyrics into a failure state.
- A matching atomic Ready artifact displays translated text only on lines actually marked translated.
- Preserved/uncertain/target-language lines are not duplicated.
- A stale Ready artifact from another canonical lyrics identity is never shown.
- A Ready artifact for a superseded target language, or one observed after Translation has been turned OFF, is not shown.
- Translation does not change canonical timing, current-line selection, sync type, provider attribution, lyrics status, provider lookup, or playback ownership.
- Canonical and translated text form one scroll/focus geometry row.
- Translation remains visually subordinate to canonical lyrics and uses the documented chrome-free annotation treatment rather than introducing a second subtitle UI.
- Existing 45% focus, edge fading, Browse/Follow, opening `♪`, return control, and PLAIN auto-scroll behavior remain intact.
- `:ui:phone` remains presentation-only and has no direct ML Kit/Translation runtime dependency.
- Android Auto behavior is unchanged.
- No persistent Translation cache is introduced.
- Track Card Translation status transitions do not change Track Card height or shift the LyricsViewport.
- Ready presentation uses concise source → target language labels such as `Translation EN → JA`.
- Tests, Previews, implementation, and documentation describe the same behavior.
- CI/build/review requirements in `AGENTS.md` are satisfied before merge.

## Local Codex handoff

A Local Codex session should begin by reading, in order:

1. `AGENTS.md`
2. this `TASK.md`
3. `docs/TRANSLATION_ARCHITECTURE.md`
4. `docs/PHONE_LYRICS_VIEWPORT.md`
5. `docs/PHONE_RUNTIME_HOST.md`
6. `docs/PHONE_UI_SPEC.md`
7. the current implementation/tests named under **Expected implementation touch points**

Then implement the checkpoints in order. Treat the existing Translation coordinator/model lifecycle as a completed dependency. Do not redesign it merely because a different architecture is possible.

Stop and report rather than broadening scope if an actual implementation contradiction prevents the documented Phone integration contract.
