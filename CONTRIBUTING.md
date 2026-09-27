# Contributing to AALyrics

AALyrics is currently in its foundation phase.

## Development workflow

All repository changes must be developed on a topic branch and merged into `main` through a pull request. Direct development on `main` is not part of the project workflow.

Use one of these branch prefixes:

- `feature/` — new functionality
- `fix/` — bug fixes
- `refactor/` — internal restructuring without an intended behavior change
- `chore/` — maintenance and repository housekeeping
- `docs/` — documentation-only changes
- `test/` — test-only changes
- `ci/` — CI/workflow changes
- `build/` — build-system changes
- `release/` — release preparation
- `hotfix/` — urgent production fixes

Use lowercase, descriptive names after the prefix, for example `feature/provider-api`, `fix/playback-position`, or `refactor/lyrics-state`.

`main` is the integration branch. Changes are merged only after the pull request checks pass.

## Build identities

Debug and release builds intentionally use separate Android application IDs so they can coexist on the same device:

- Debug: `io.github.whoxamxl.aalyrics.debug`
- Release: `io.github.whoxamxl.aalyrics`

The Kotlin/Android namespace remains `io.github.whoxamxl.aalyrics`; only the install-time application ID differs. Debug builds are also labeled `AALyrics Debug` in the launcher and Android Auto so they are distinguishable from an installed release build.

Do not use a normal debug build to validate the production self-update path. Published releases target the release application ID and durable release signing identity; use the dedicated release-signed update-test workflow when production update compatibility must be exercised.

## Pull request review discipline

Keep the pull request's stated scope and acceptance criteria fixed during review. Review feedback should block the current PR when it identifies a current defect, regression, CI failure, security/safety issue, or direct violation of those criteria. Theoretical bypasses, hypothetical future configurations, speculative hardening, and intentionally adversarial ways to evade a best-effort guardrail should normally be deferred rather than expanding the PR indefinitely.

For AI-assisted review, use at most two normal Codex review rounds. After that, new P2 findings are merge blockers only when they materially affect the current scope. Lightweight CI architecture scripts are regression guardrails, not formal static-analysis proofs; stronger enforcement should be implemented separately with structural tooling when it becomes necessary.

AI agents working in this repository must follow the detailed policy in [`AGENTS.md`](AGENTS.md), including the requirement to stop before merge and wait for explicit approval.

## External contributions

External code contributions are not being accepted yet while the architecture, ownership model, and contribution terms are being established. Bug reports, design feedback, and technical discussion are welcome through GitHub Issues.

This policy is temporary. Contribution terms will be documented before external code contributions are opened.
