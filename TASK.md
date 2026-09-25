# Effective Timing Foundation + Existing Line Integration

## Branch and baseline

- Branch: `feature/effective-timing-foundation`.
- Base: `main` at `ae9ed3f27097388b32537ad4b40147679567efaf` (PR #79 merged).
- Classification: TIMING / ARCHITECTURE / PURE CORE FOUNDATION.
- Status: Phases 11.3a and 11.3b are implemented and validated on this branch; the existing Draft PR remains the stop point.
- Authoritative references: `AGENTS.md`, `docs/ARCHITECTURE.md`, `docs/LYRICS_PIPELINE_ARCHITECTURE.md`, `docs/TIMING_ARCHITECTURE.md`, `docs/KARAOKE_ARCHITECTURE.md`, `docs/PRESENTATION_STATE_ARCHITECTURE.md`, `docs/MIGRATION_INVENTORY.md`, and current `main` code/tests.

## Goal

Complete the framework-independent Phase 11.3a foundation and the separately authorized Phase 11.3b integration that routes the existing Phone current-line decision through that foundation.

The completed foundation must provide a tested, reusable engine for one lyrics-specific virtual clock:

```text
effectiveLyricsPositionMs
    = projectedPlaybackPositionMs + lyricsTimingOffsetMs
```

with these product semantics:

```text
positive offset -> advance lyrics
negative offset -> delay lyrics
zero offset     -> preserve current behavior
```

Phase 11.3b is now explicitly authorized on this branch. It may connect the existing Phone current-line decision to `EffectiveLyricsPosition`, while the production offset remains zero by default. It must **not** add Sync UI, persistence, Android Auto timing integration, or Karaoke/WORD projection.

## User-facing sign contract

The sign convention is fixed and must not be inverted by implementation details.

At the same real playback position:

```text
current:
She'd take the world off | my shoulders if it was ever hard to move

positive:
She'd take the world off my shoulders | if it was ever hard to move

negative:
She'd take the | world off my shoulders if it was ever hard to move
```

Therefore:

- lyrics behind the music -> positive adjustment;
- lyrics ahead of the music -> negative adjustment.

Future Phone/Android Auto controls may choose their own visual layout, but they must preserve this meaning.

## Current production baseline to preserve

Current `main` already has:

- canonical normalized LINE/WORD timestamps in `:core:model`;
- existing playback position projection in `PhoneLyricsMapper.projectedPlaybackPosition(...)`;
- existing LINE current-row selection through `LyricsDocument.currentTimedLineIndex(positionMs)`;
- Phone Translation presentation/diagnostics merged in PR #79 without changing timing ownership;
- `SyncScreen` as an explicit non-functional placeholder;
- no authorized timing offset persistence;
- no effective-timing core module yet;
- no production Karaoke/WORD projection.

Phase 11.3b keeps the existing Phone playback projection helper in place. `PhoneLyricsMapper` derives `EffectiveLyricsPosition` from that projected position and a default-zero `LyricsTimingOffset`, then feeds only the existing current-line selector through the effective position. Playback progress continues to use the real projected playback position.

## Working-fork re-check

Working-fork `whoxamxl/auto-lyrics` `main` was re-checked at `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`) on 2026-09-25.

Relevant evidence:

### `util/SyncCalibration.kt`

The mature helper uses:

```kotlin
offsetForTap(targetTimeMs, rawPositionMs) = targetTimeMs - rawPositionMs
```

Its regression test proves:

```text
target 10,500 - raw 10,000 = +500
target 10,000 - raw 10,500 = -500
```

This is compatible with the newly approved AALyrics convention when effective lyrics position is `raw/projected position + offset`:

- positive advances the lyrics clock;
- negative delays it.

**PRESERVE / REFACTOR:** preserve this sign behavior.

Do **not** migrate the old three-tap workflow or `upcomingTimedLineIndices(...)` in Phase 11.3a. Those are Sync calibration UX/policy and remain deferred.

### `lyrics/KaraokeTiming.kt`

The mature fork has tested active-word boundary semantics, including explicit end-time gaps and backward seeking.

**PRESERVE / REFACTOR later:** keep this as Phase 11.4 evidence.

Do **not** migrate active-word selection in Phase 11.3a. The foundation only creates the effective lyrics clock that a later Karaoke projection may consume.

## Architectural ownership

Phase 11.3a authorizes one new pure Kotlin/JVM capability module:

```text
:core:timing
```

It owns only framework-neutral timing offset semantics and effective-lyrics-position calculation.

It must not own:

- MediaSession or Android playback objects;
- playback projection/monotonic ticking;
- canonical lyrics mutation;
- provider lookup/selection;
- persistence;
- app lifecycle;
- Phone/Android Auto UI;
- current-line/current-word selection;
- Karaoke projection.

The initial module should have **no production dependency** on Android, networking, providers, Translation, UI, or `:app`. It should also avoid depending on `:core:model` unless implementation evidence demonstrates a real need; the first transform only requires normalized millisecond values.

## Canonical data invariant

Provider/parser timestamps remain canonical.

Phase 11.3a must not implement:

```text
sourceTimestamp += offset
```

or produce a copied lyrics document with rewritten timestamps.

Instead:

```text
canonical source timestamp -----------┐
                                      │ compare later
projected playback position           │
        + lyrics offset               │
        ↓                             │
effective lyrics position ------------┘
```

The pure timing engine does not need a `LyricsDocument` input.

## Minimal production shape

Use the smallest repository-idiomatic API that keeps signed offset and effective position semantically distinct.

A preferred shape is conceptually:

```kotlin
@JvmInline
value class LyricsTimingOffset(val milliseconds: Long)

@JvmInline
value class EffectiveLyricsPosition(val milliseconds: Long)

fun effectiveLyricsPosition(
    projectedPlaybackPositionMs: Long,
    offset: LyricsTimingOffset,
): EffectiveLyricsPosition
```

Equivalent naming/organization is acceptable if it remains equally small and preserves the documented semantics.

Requirements:

- provide an explicit zero/neutral offset;
- positive values advance the effective lyrics position;
- negative values delay it;
- do not silently clamp to zero or track duration;
- a negative effective position is valid and means "before timed lyrics";
- do not add current-line/current-word logic to prove the API;
- do not add coroutine/state machinery for a pure arithmetic transform.

## Build/module integration

The foundation is considered architecturally integrated when:

- `:core:timing` is included in `settings.gradle.kts`;
- it uses the Kotlin/JVM plugin and JDK 17, matching existing pure-core modules;
- its production dependency surface is empty unless a concrete requirement proves otherwise;
- `scripts/verify-architecture.sh` treats `core/timing` as a pure module and rejects Android/network/unauthorized production dependencies;
- its unit tests run in the normal repository test/build path.

Phase 11.3b introduces the first real consumer: `:app` may depend on `:core:timing` because `PhoneLyricsMapper` now routes current-line selection through the effective position. Do not add any other consumer merely to manufacture usage.

## Deterministic test contract

Add focused tests for at least:

1. zero offset returns the projected position unchanged;
2. positive offset advances the effective lyrics position;
3. negative offset delays the effective lyrics position;
4. a positive example matches the documented human sign convention;
5. a negative example matches the documented human sign convention;
6. a result may be negative rather than silently clamped;
7. source timestamps are not part of mutable timing state and are not rewritten by this API.

Suggested concrete examples:

```text
31,200 +    0 = 31,200
31,200 +  800 = 32,000
31,200 + -800 = 30,400
   200 + -500 =   -300
```

Do not add LINE/WORD boundary tests here unless production integration is also changed, which is outside this slice.

## Scope guardrails

Do **not** implement or modify any of the following in Phase 11.3a:

- `PhoneLyricsMapper` current-line behavior;
- `projectedPlaybackPosition(...)` behavior;
- `LyricsDocument.currentTimedLineIndex(...)`;
- `SyncScreen` UI;
- +/- controls, slider, step sizes, labels, or reset button;
- SharedPreferences/DataStore timing state;
- global/per-track/provider/Phone/Android Auto offset scope;
- automatic calibration;
- three-tap calibration;
- drift/rate correction;
- audio/waveform analysis;
- provider-specific timestamp correction;
- Android Auto timing presentation;
- Karaoke active-word/progress semantics;
- WORD sweep/rendering;
- Translation timing behavior.

If implementation reveals a defect in an existing area, record it separately unless it directly blocks this foundation.

## Reset AALyrics contract

Phase 11.3a adds no persisted state.

Therefore:

- `Reset AALyrics` behavior must remain unchanged;
- no reset copy change is required;
- no timing preference should be added preemptively.

Phase 11.3c must re-evaluate Reset if/when a durable offset is introduced.

## Expected implementation touch points

Expected:

- `settings.gradle.kts`
- `core/timing/build.gradle.kts` (new)
- `core/timing/src/main/kotlin/io/github/whoxamxl/aalyrics/core/timing/...kt` (new)
- `core/timing/src/test/kotlin/io/github/whoxamxl/aalyrics/core/timing/...Test.kt` (new)
- `scripts/verify-architecture.sh`
- `TASK.md` / timing architecture docs only if implementation evidence requires a small correction

Not expected:

- `app/src/main/**`
- `ui/**/src/main/**`
- provider modules
- Translation modules
- Android manifests/resources

## Implementation checkpoints

Implement in small coherent commits.

1. [x] **Pure timing module + transform**
   - add `:core:timing`;
   - add the signed offset/effective-position model;
   - add the pure `projected + offset` transform;
   - no app/UI integration.

2. [x] **Tests + architecture guard**
   - add deterministic sign/zero/negative-position tests;
   - register `core/timing` as a pure module in `verify-architecture.sh`;
   - ensure no unauthorized production dependencies are introduced.

3. [x] **11.3a validation + final alignment**
   - architecture checks, unit tests, and debug APK build passed;
   - the initial Draft PR was opened after 11.3a validation.

4. [x] **11.3b existing line integration**
   - add the real `:app -> :core:timing` consumer;
   - keep the existing playback projection helper unchanged;
   - derive effective lyrics position with a default-zero offset;
   - route only `currentTimedLineIndex(...)` through effective lyrics position;
   - keep playback progress on the real projected playback position;
   - add positive/negative boundary-crossing regression coverage.

5. [x] **11.3b final re-validation**
   - run architecture checks;
   - run unit tests;
   - build the debug APK/repository build path;
   - inspect the branch-wide diff for scope drift;
   - confirm no Sync UI, persistence, Reset, Android Auto, or Karaoke behavior was added;
   - update the existing Draft PR evidence, then stop.

Phase 11.3a validation previously passed in Build runs 36098879658 and 36099140235. Phase 11.3b validation passed in [Build run 36100805321](https://github.com/whoxamxl/AALyrics/actions/runs/36100805321): branch/commit checks, architecture guard, debug APK build, and repository unit tests all succeeded. Phase 11.3b adds no persisted state, so `Reset AALyrics` still needs no change.

## Documentation alignment checkpoints

The documentation preparation for this implementation is complete in three tasks:

1. [x] remove stale completed-slice status after Update and PR #79 Translation merge;
2. [x] fix the Timing/effective-position semantics across Timing, Karaoke, pipeline, presentation, and Phone boundary docs;
3. [x] record the working-fork evidence, concrete module boundary, implementation scope, acceptance criteria, and Codex-ready task contract.

## Acceptance criteria

Phase 11.3a is complete when:

- a pure, tested effective-lyrics-position engine exists;
- the sign convention is unambiguous and regression-tested;
- canonical lyrics/source timestamps remain untouched;
- zero offset is behaviorally neutral;
- `:core:timing` is part of the build and architecture guard;
- there is no user-visible behavior change;
- no persistence/reset scope is added;
- the existing Phone current-line call site explicitly consumes effective lyrics position with a default-zero offset;
- playback progress remains based on real projected playback position;
- Android Auto and Karaoke call sites remain unchanged;
- CI/build/tests are green;
- review finds no current-scope blocking issue.

## Stop point

After Phase 11.3b is re-validated, **stop** at the existing Draft PR.

Do not continue automatically into Sync UX/persistence. Phase 11.3c remains deferred. Karaoke/WORD timing semantics remain a separate engine slice even if later authorized on this branch.
