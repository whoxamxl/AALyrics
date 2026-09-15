# Core Readiness Gate Task

This branch implements only Phase 5 of `docs/ROADMAP.md`: final core-readiness validation before the concrete-provider / previous-fork adaptation STOP GATE.

Phase 4 was merged in PR #12. The working fork was re-checked again before this phase and `whoxamxl/auto-lyrics` `main` is still at `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`). No previous-fork implementation code is adapted on this branch.

## Scope guard

This phase is validation-first. Do not add concrete providers, resolver/scoring logic, cache, translation, timing/karaoke implementation, phone UI, Android Auto UI, or bulk migration code.

Production changes are allowed only if a readiness check exposes a genuine architecture boundary defect. Any such fix must be isolated from the test/audit that exposes it.

## Commit strategy

Keep commits small and single-purpose. PR-level squash remains separate from branch history.

Prefer this sequence:

1. validate pure-core dependency boundaries,
2. validate shared-state / feature consumption boundaries,
3. validate provider and UI ownership boundaries,
4. re-review migration inventory against the current fork,
5. record final gate evidence and update durable roadmap docs,
6. open one Phase 5 PR, run CI and Codex review, then stop at the STOP GATE.

## Phase 5 checklist

- [x] `:core:model`, `:provider:api`, and `:core:lyrics` contain no Android framework dependency.
- [x] Core orchestration is exercised entirely with fake providers.
- [x] Selection remains behind a stable provider-independent boundary.
- [x] Provider execution order cannot accidentally determine the selected result.
- [x] Track changes cannot allow stale results to overwrite current state.
- [x] One provider failure is isolated from healthy providers.
- [x] Playback position/status changes cannot accidentally restart lyrics lookup.
- [x] Android MediaSession / MediaController types remain inside `:platform:media`.
- [x] Phone and automotive layers can consume one shared `LyricsState` contract.
- [x] No UI layer performs provider fetching or ranking.
- [x] Concrete provider quirks are not represented as core application behavior.
- [x] Architecture and tests make the intended dependency direction difficult to violate accidentally.
- [x] `docs/MIGRATION_INVENTORY.md` has been reviewed against the current fork.
- [ ] CI debug build and all tests pass on the final Phase 5 head.
- [ ] Codex review is complete on the final Phase 5 head.
- [ ] Stop for explicit architecture/migration review before any provider/resolver adaptation.

## Evidence

- `scripts/verify-architecture.sh` makes the dependency/ownership rules executable and CI-enforced.
- Existing core integration tests cover provider ordering, fake-provider orchestration, failure isolation, stale-result rejection, and selector ownership.
- Phase 4 playback tests cover identity-driven lookup ownership independently from timeline/status churn.
- `docs/CORE_READINESS_GATE.md` records the evidence matrix and STOP GATE review topics.
- `docs/MIGRATION_INVENTORY.md` was re-reviewed against fork commit `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f`.

## Exit criteria

Phase 5 is complete only when every readiness item is either demonstrated by code/tests/module configuration or explicitly identified as a blocker. Passing the checklist does not authorize automatic provider migration.

## Next action

Open the Phase 5 PR, run CI and Codex review on the final head, address any substantive finding in a separate commit, then stop before merge for explicit architecture/migration review.
