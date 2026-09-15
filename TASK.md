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

- [ ] `:core:model`, `:provider:api`, and `:core:lyrics` contain no Android framework dependency.
- [ ] Core orchestration is exercised entirely with fake providers.
- [ ] Selection remains behind a stable provider-independent boundary.
- [ ] Provider execution order cannot accidentally determine the selected result.
- [ ] Track changes cannot allow stale results to overwrite current state.
- [ ] One provider failure is isolated from healthy providers.
- [ ] Playback position/status changes cannot accidentally restart lyrics lookup.
- [ ] Android MediaSession / MediaController types remain inside `:platform:media`.
- [ ] Phone and automotive layers can consume one shared `LyricsState` contract.
- [ ] No UI layer performs provider fetching or ranking.
- [ ] Concrete provider quirks are not represented as core application behavior.
- [ ] Architecture and tests make the intended dependency direction difficult to violate accidentally.
- [ ] `docs/MIGRATION_INVENTORY.md` has been reviewed against the current fork.
- [ ] CI debug build and all tests pass on the final Phase 5 head.
- [ ] Codex review is complete on the final Phase 5 head.
- [ ] Stop for explicit architecture/migration review before any provider/resolver adaptation.

## Exit criteria

Phase 5 is complete only when every readiness item is either demonstrated by code/tests/module configuration or explicitly identified as a blocker. Passing the checklist does not authorize automatic provider migration.

## Next action

Start with the pure-core dependency-boundary audit only. Keep that validation in its own commit, then stop and review the result before moving to the next readiness group.
