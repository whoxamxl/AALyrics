# AALyrics Agent Instructions

These instructions apply to AI-assisted repository work, including implementation, pull-request review, and review follow-up.

## Repository workflow

- Work on a topic branch. Do not commit directly to `main`.
- Keep commits small and single-purpose.
- Use `TASK.md` to record the active plan/status for substantial planned work.
- Open a pull request, run CI, and review the resulting diff before integration.
- Always stop before merge. Merge only after explicit user authorization.
- Prefer squash merge after approval unless a different strategy is explicitly requested.

## Scope and acceptance criteria

Before review begins, treat the pull request description, the current `TASK.md` when it describes that PR's active work, and the relevant roadmap/architecture documents as the acceptance criteria for that PR. Do not inherit a stale or unrelated `TASK.md` as scope for a different pull request.

Do not silently expand the scope because a reviewer can imagine additional hardening, future configurations, exotic syntax, or adversarial bypasses. A review finding should change the current PR only when it materially affects the current PR's intended behavior or stated guarantees.

If a useful improvement falls outside the current scope, record or propose it as follow-up work instead of growing the current PR indefinitely.

## Review severity policy

Treat review findings as follows:

- **P0 / P1:** normally blocking. Fix before merge unless the finding is demonstrably incorrect or explicitly accepted as a known risk.
- **P2:** blocking only when it exposes a defect in current code, a current regression, a meaningful false positive/false negative in an intended CI check, or a direct violation of the PR's explicit acceptance criteria.
- **P2 theoretical bypasses:** non-blocking by default. Examples include deliberately adversarial syntax, hypothetical future source sets, an arbitrary future library not used by the repository, or a way a developer could intentionally evade a best-effort guardrail.
- **P3 / style / speculative hardening:** non-blocking unless explicitly in scope.

Do not equate "technically valid observation" with "must fix in this PR."

## Review-round limit

Use at most **two normal Codex review rounds** for a PR.

After the second round, newly discovered P2 findings are not merge blockers unless they reveal a current-scope defect, regression, safety/security issue, CI breakage, or violation of an explicit acceptance criterion.

A targeted re-review after a fix should validate the changed area and the original concern. Do not restart an open-ended adversarial search of the whole repository unless the user explicitly requests a deeper audit.

## Guardrails are not formal proofs

Repository scripts such as architecture checks are **best-effort regression guardrails**. Their purpose is to catch common accidental boundary violations in the repository's normal development style.

Do not turn shell/regex checks into a general Kotlin/Gradle static analyzer by repeatedly adding patterns for every conceivable syntax, library, source-set layout, or intentional bypass.

When stronger enforcement is genuinely required, propose it as separate work using structural tooling such as Gradle convention plugins/dependency rules, dependency analysis, or a proper static-analysis rule. Do not continuously enlarge the current PR to simulate formal enforcement with regexes.

Use wording consistent with this guarantee level: prefer statements such as "CI guards the documented architecture against common accidental regressions" over claims that a lightweight script completely enforces all possible architecture violations.

## Review exit condition

A PR is ready to leave the review loop when:

1. its stated acceptance criteria are satisfied,
2. CI is green,
3. no unresolved P0/P1 or current-scope blocking P2 remains,
4. two normal review rounds have completed, or the latest review has no material in-scope findings, and
5. remaining comments are theoretical, adversarial, future-scope, stylistic, or better handled by dedicated tooling/follow-up work.

At that point, stop requesting repeated broad reviews and proceed to the normal pre-merge approval gate.
