# AALyrics Agent Instructions

These instructions apply to AI-assisted repository work, including implementation, pull-request review, and review follow-up.

## Repository workflow

- Work on a topic branch. Do not commit directly to `main`.
- Keep commits small and single-purpose.
- Use `TASK.md` to record the active plan/status for substantial planned work.
- Commit and push coherent checkpoints as implementation progresses.
- Do not open a pull request merely to expose in-progress work.
- Open the pull request only after implementation and final validation are complete; open it as a **Draft pull request** by default.
- After opening that Draft PR, stop. Do not automatically request Codex review, mark the PR ready, merge it, or begin the next implementation slice without explicit user authorization.
- Always stop before merge. Merge only after explicit user authorization.
- Prefer squash merge after approval unless a different strategy is explicitly requested.

## Incremental implementation and PR timing

For non-trivial implementation work, use the following workflow unless the user explicitly requests a different process.

### 1. Work incrementally

- Split implementation into small, coherent checkpoints.
- Each checkpoint should represent one understandable responsibility or behavioral change.
- Do not accumulate unrelated changes into one large commit.

### 2. Commit each completed checkpoint

- Commit after each checkpoint reaches a coherent state.
- Use a focused commit message that describes only that checkpoint.
- Prefer several small reviewable commits over one large implementation commit.
- A checkpoint does not need the entire feature to be finished before it can be committed.

### 3. Push progress without opening a PR

- Push the topic branch as checkpoints are completed so the remote branch remains current and recoverable.
- Pushing the branch does **not** imply that the work is ready for review.
- Do **not** open a pull request during normal implementation progress.

### 4. Keep the PR closed until implementation is complete

- Finish all planned implementation checkpoints first.
- Complete the task-specific acceptance criteria, focused tests, architecture checks, documentation alignment, Preview coverage, regression review, and build validation that are required for the slice.
- Fix validation failures on the existing topic branch using additional focused commits and push them normally.
- Do not create a temporary or progress PR merely to expose the branch or obtain a PR number.

### 5. Perform final validation before PR creation

Before opening the PR:

- run the required final validation for the whole branch;
- inspect the branch-wide diff and confirm that scope has not drifted;
- confirm the branch is based on the intended baseline and reconcile it when necessary;
- confirm documentation and `TASK.md` match the implemented behavior;
- confirm no known acceptance criterion remains incomplete;
- record any intentionally deferred follow-up work rather than silently expanding scope.

### 6. Open a Draft PR only after final validation

Once implementation and final validation are complete:

- push the final branch state;
- open a **Draft pull request**;
- include the completed scope, implementation summary, validation evidence, and explicitly deferred follow-up work in the PR description.

Opening the Draft PR is the default completion point for the implementation workflow.

### 7. Stop after opening the Draft PR

After the Draft PR is created:

- **STOP**;
- do not automatically request Codex review;
- do not convert the PR to Ready for review;
- do not merge;
- do not begin the next implementation slice.

Those actions require explicit user authorization.

The standard flow is therefore:

```text
plan
  ↓
checkpoint
  ↓
implement
  ↓
checkpoint validation
  ↓
commit
  ↓
push
  ↓
next checkpoint
  ↓
...
  ↓
implementation complete
  ↓
final validation
  ↓
branch-wide diff / scope review
  ↓
push final state
  ↓
open Draft PR
  ↓
STOP
```

An early Draft PR is an exception and should be created only when the user explicitly asks for one.

## Provider migration authorization

Provider planning and provider implementation are separate authorization boundaries.

- Read `docs/PROVIDER_ARCHITECTURE.md` and the relevant file under `docs/providers/` before concrete provider migration.
- Treat mature working-fork provider code as **PRESERVE / REFACTOR** by default when the migration inventory says so; do not force a gratuitous rewrite merely to avoid reusing proven project code.
- Do not infer permission to begin provider implementation from discussion, documentation work, migration-order agreement, or approval of a provider profile.
- Begin a concrete provider migration slice only after the user explicitly moves the work from planning/documentation into implementation.
- Experimental provider branches are non-canonical unless the user explicitly authorizes their reuse. Start an approved provider slice from current `main` and the approved migration documents by default.
- PetitLyrics migration must not change `PETITLYRICS_USER_ID`, `PETITLYRICS_APP_NAME`, `PETITLYRICS_PKG_NAME`, or `PETITLYRICS_CLIENT_APP_ID` unless the user explicitly requests a change.

## Scope and acceptance criteria

Before review begins, treat the pull request description, the current `TASK.md` when it describes that PR's active work, and the relevant roadmap/architecture documents as the acceptance criteria for that PR. Do not inherit a stale or unrelated `TASK.md` as scope for a different pull request.

Do not silently expand the scope because a reviewer can imagine additional hardening, future configurations, exotic syntax, or adversarial bypasses. A review finding should change the current PR only when it materially affects the current PR's intended behavior or stated guarantees.

If a useful improvement falls outside the current scope, record or propose it as follow-up work instead of growing the current PR indefinitely.

## Reset contract maintenance

`Reset AALyrics` is an explicit product contract and must stay aligned as application state evolves.

- Any PR that adds or changes app-owned persisted state, onboarding acknowledgement, durable preference, or long-lived Phone setting must explicitly re-evaluate whether `Reset AALyrics` should restore it to default.
- In the same PR, either add the state to the explicit reset path with appropriate coverage, or document why it intentionally survives reset.
- Do not replace the explicit reset path with a blanket SharedPreferences/DataStore clear.
- External/system-owned state such as Android permissions and Android Auto system settings stays outside the reset boundary.
- New caches, downloaded assets, and model files require an explicit keep/delete decision; preserve them by default unless a dedicated storage action or explicit product decision says otherwise.
- When reset semantics change, update `docs/PHONE_SETTINGS.md`, user-visible reset copy when necessary, relevant Previews, and tests together.

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
