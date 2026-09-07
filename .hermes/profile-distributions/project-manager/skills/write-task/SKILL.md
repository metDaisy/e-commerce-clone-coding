---
name: write-task
description: Use when creating evidence-backed Amaazon Kanban task graphs.
version: 0.3.0
author: leee, Hermes Agent
license: MIT
platforms: [linux, macos, windows]
metadata:
  hermes:
    tags: [planning, kanban, requirements, verification]
    related_skills: []
---

# Write Task

Use when Project Manager creates or repairs an Issue task graph. This Skill defines
**how to write and validate task contracts**; `SOUL.md` defines PM authority and
non-negotiable lifecycle policy. Do not implement source code or replace Kanban state.

## Inputs and surface

Read `AGENTS.md`, `docs/index.md`, `docs/current-state.md`, applicable requirement and
architecture/ADR documents, committed code/test/migration, Git status/HEAD, the Issue,
and existing board before creating tasks. Requirements define target behavior; committed
artifacts define actual behavior. Preserve conflicts as `unknowns`, `blocked`, or
`needs-input`.

## Repository discovery before task authoring

Do not infer that a requirement is unimplemented from the Issue, requirement documents, or
`docs/current-state.md` alone. Before the first Kanban `create` for an Issue, perform this bounded
discovery workflow against `planning_head_sha`:

1. Extract the Issue's observable behaviors, affected domain terms, state transitions, public
   contracts, and persistence concerns. Give each question a short local `query_id`; do not store
   the raw query in Kanban or audit logs.
2. Call codebase-memory `index_status`. If the index is usable, use `search_graph` for the relevant
   symbols/modules and `trace_path` when a caller, callee, dependency, or cross-module path matters.
3. Use Semble `search` for each behavior whose implementation location is not already known. Use
   `find_related` only to expand a promising source/test result; do not full-scan unrelated domains.
4. Treat MCP results only as candidates. Open the returned production source, tests,
   `package-info.java`, configuration, JPA mapping, and Flyway migration that are relevant to the
   requirement. Confirm them from the committed Git object at `planning_head_sha`, not from dirty
   working-tree content.
5. Build a private, temporary requirement-gap matrix with one row per behavior:

   ```text
   requirement_id | status=implemented|partial|absent|unknown
   confirmed_source_locators | confirmed_test_locators | constraint_locators | task_decision
   ```

   `absent` means the expected entry point/contract and related implementation were searched for
   and no satisfying committed behavior was found. `unknown` is required when the search scope or
   index/source evidence is insufficient. Do not convert `unknown` into an implementation task.
6. Create tasks only for `partial` or `absent` rows. Do not create work for an `implemented` row;
   cite its source/test evidence in the planning report. Route `unknown` to an investigation or
   `needs-input` decision.

For a broad cross-module Issue, both Semble and codebase-memory are expected before creation:
Semble locates behavior, while codebase-memory checks relationships and impact. Their use does not
prove correctness; direct committed-file confirmation does. For a narrow literal lookup, the PM may
use exact repository search after the initial orientation instead of repeating the same MCP query.

If either MCP is unavailable, stale, or fails, do not fabricate its result and do not stop safe
read-only planning. Fall back to literal repository search plus direct committed-file reads, preserve
the limitation as `unknowns` or a concise planning note, and accept the agent-audit deviation as a
debugging signal. Reindexing, installing, or changing an MCP server remains a separate user-approved
state change.

The project-local `agent-audit` plugin observes successful/failed Semble and codebase-memory
operations and records `kanban_create_without_expected_discovery` when task creation occurs before
both providers are observed. It stores only provider, operation, status, session/task/turn
correlation—not queries, arguments, results, prompts, reasoning, credentials, or absolute paths.
This observation is fail-open and does not replace direct source evidence or the board validator.

### Evidence threshold

- A functional implementation task must cite a requirement `goal`, at least one confirmed
  production-code `state` locator, and the relevant existing test locator or a justified planned
  test FQCN. `docs/current-state.md` alone cannot establish that behavior is absent.
- A cross-module task must cite the involved `package-info.java` or public `@NamedInterface`/event
  contract. A persistence task must cite the relevant JPA mapping/migration and repository test
  surface. An API task must confirm controller, DTO/application contract, security path, and test
  surface as applicable.
- When no production code exists, cite the nearest confirmed module seam, schema, and test surface
  that define where the new behavior belongs, and describe the searched absence in the `state`
  claim. Never invent a nonexistent source locator.
- Keep MCP queries and raw results out of task bodies. Persist only source-backed decisions and
  repository-relative locators.

## Frozen contract and executable oracle

All newly generated boards use [`references/board-contract-v1.md`](references/board-contract-v1.md).
Do not add an ad-hoc evaluation rule after generation. A new rule requires a new contract version
and a failing regression test first. Natural-language self-review is not the preflight oracle;
`scripts/validate_board.py` is authoritative for deterministic contract checks.

### Dirty working tree isolation

Generate the Issue graph from the GitHub Issue and artifacts committed at
`planning_head_sha`. Working-tree modifications and untracked files are not planning
evidence and must not add, remove, merge, split, complete, block, or rewrite tasks. When a
tracked planning source is dirty, read its committed form at `planning_head_sha` (for example,
with an exact Git object read) instead of treating the filesystem copy as authoritative.

Dirty status alone:

- does not make `state_freshness` stale;
- does not require a reconciliation task;
- does not prevent graph creation or the normal single-ready-leaf result;
- does not permit treating uncommitted implementation as completed state.

Collect Git status only for execution safety. Immediately before a worker claims or edits a
task, compare existing dirty paths with that task's expected changed-path scope. Preserve
non-overlapping changes. On overlap, do not edit, delete, stage, or absorb the existing change;
route the task to `blocked`/`needs-input` for a user decision. Commit/freeze and quality review
still require their declared clean workspace.

Use native `kanban_*` tools when present. Otherwise inspect `hermes kanban --help` and
use the official CLI. Select the board explicitly for every list/create/link/promote/
comment/read-back. If neither surface exists, stop without claiming a graph was created.

Write titles and human-readable text in Korean. Preserve literal IDs, URLs, paths,
SHAs, symbols, CLI commands, status enums, and structured keys such as `goal`, `state`,
`constraint`, `CHECK`, and `EXPECT`. Evidence `claim`, scope/out-of-scope, acceptance criteria,
manual-review descriptions, and explanatory text must not be English-only.

## Task body schema

Pass a real multiline body; literal `\\n` is invalid. Every executable task contains:

```text
contract_version: board-contract-v1
task_type: reconciliation | investigation | implementation | commit | quality-review | coordinator | pr-create | finalization
issue: #<number>
issue_url: <URL>
current_state_sha: <snapshot SHA>
planning_head_sha: <planning HEAD>
state_freshness: fresh | stale | blocked
decomposition_depth: 1..3
dependency_path_length: <number>
workspace_kind: dir | scratch | worktree
workspace_path: <raw native Kanban path; omit the workspace selector prefix>
assignee: <Profile>

evidence: <typed goal/state/constraint entries defined below>

scope:
- <owned outcome>
out_of_scope:
- <explicit exclusion>
acceptance_criteria:
- id: AC-<stable-id>
  outcome: <observable Korean result>
acceptance_coverage:
- acceptance_id: AC-<stable-id>
  check_ids: <one or more check IDs>
verification:
- check_id: <stable check ID>
  kind: behavioral | structural | style | state | manual
  CHECK: <actual runner + exact command/test identifier + working directory>
  EXPECT: <success-specific condition>
```

Implementation tasks require at least one `goal`, one `state`, and at least one behavioral
check covering every acceptance ID. At creation they declare only the future handoff contract:

```text
review_handoff_contract:
  action: request-review
  required_metadata: verified_sha, validator, check_id, result, changed_paths, residual_risk
```

Do not put actual `verified_sha`, `result`, or `changed_paths` in a new task. Those values belong
to native `request-review` metadata after execution.

## Graph construction

1. Reconcile snapshot SHA and HEAD using committed evidence. If stale, create a board containing
   only one reconciliation/investigation task. After it completes, generate a new fresh graph
   from the confirmed committed SHA and gap. Never pre-create immutable stale downstream bodies.
   Working-tree dirtiness alone is not stale. The stale mismatch is the reconciliation task's input,
   not a reason for that task to block itself: claim it, execute the discovery workflow above, and
   block only on an independent infrastructure/evidence failure.
2. Make the smallest evidence-backed one-to-three-level graph. Give each leaf one owner,
   coherent result, scope/out-of-scope boundary, and independently decisive verification.
   For every `goal` that requires another module or public contract, compare its `state`
   evidence with the source. If the prerequisite is absent, add its producer task before
   the consumer, or record a blocked decision. A consumer must not cite an absent contract
   while excluding its implementation from scope.
3. Select workspace before `create` using the exact workspace procedure below.
4. Create prerequisites before dependents that need a body-level reference. Read back the
   generated `t_<hex>` ID, then write that literal ID in
   `depends_on_task_id: t_<actual-id>`. Parent/child graph links remain authoritative;
   omit a body reference if the ID is not yet known. Never store `t01`, `step-1`, title
   aliases, or placeholders.
5. Link actual IDs and read back both envelopes. Verify body line breaks, body references,
   parents/children, assignee, status, workspace kind/path, and `CHECK` CWD.
6. After all links exist, calculate the longest actual dependency path from the native
   `parents` graph for every live task. Compare it with the body's
   `dependency_path_length`; do not derive it from `decomposition_depth`, titles, creation
   order, or an earlier partial graph.
7. Before mutation, write a temporary `show --json`-shaped draft with zero `ready` tasks and run
   `--input <draft.json> --phase draft`. Create/link every task as `todo` or `blocked`, then run
   `--board <slug> --phase draft` against native read-back. Exit code 1 or 2 means graph creation
   failed; do not reinterpret findings in prose.
8. Only after the native draft validator exits 0, promote one dependency-satisfied executable leaf.
   Run `--board <slug> --phase post` and require exit code 0 before reporting completion. Parallel
   ready work requires an explicitly versioned contract; v1 permits exactly one `ready` task.

## Detailed evidence rules

Evidence is not a title, search-result summary, or remembered implementation fact. Read the
cited source before writing the claim. Use a separate entry for each purpose:

Use only the structured `source.kind` forms in `board-contract-v1.md`. Repository sources contain
`path` and exactly one of `heading` or `lines`; Issue sources contain `url`; task sources contain
an actual `task_id`. Legacy `path#heading`, `path:Lx-Ly`, combined locator strings, and conjunctions
such as `and #heading` are invalid.

An implementation card needs `goal` and `state`. A task may cite an Issue for outcome scope,
but not instead of a requirement document when one exists. Search results are locators only;
read the file heading or line range before recording it. Preserve competing evidence in
`unknowns`; do not silently select the more convenient claim.

Resolve every repository source against the committed tree at `planning_head_sha`, not the
dirty filesystem copy. `heading` requires that exact heading in the committed file; `lines`
requires an existing range supporting the claim. A Profile/SOUL rule governs
execution but is not task evidence unless its actual committed repository path is cited and
supports the claim. Never invent an `AGENTS.md` heading or repair a missing locator by name.

Before graph creation, compare every required public contract in a consumer's `goal` or
`constraint` with the cited state. If the state says that producer is absent, create its
producer task first. Link producer → consumer and include the producer's literal ID only after
read-back. Do not replace a missing producer with an English placeholder or a generic
“integration” task.

## Exact workspace and identity procedure

1. Select `--workspace` before `create`.
2. Create the task using `dir:<repository-root>`, `scratch`, or an authorized worktree selector.
3. Read the resulting task envelope.
4. Copy the envelope's raw `workspace_kind` and `workspace_path` values into the task contract
   only when the native surface supports it; otherwise include the intended raw values before
   create and compare them after read-back.
5. Reject any task where selector text (`dir:` or `worktree:`) appears in `workspace_path`,
   body CWD targets a different tree, or the worker needs uncommitted inputs in scratch/worktree.

The generated `t_<hex>` value is authoritative. Create dependency producers before consumers
that require an explicit body reference. Keep temporary aliases only in local planning notes;
they cannot appear in a persisted body, comment, metadata, CHECK, EXPECT, or acceptance
criterion. Read back both parent and child after every link. An archived task is historical
evidence, not an executable dependency or a replacement for a live task unless its status and
link are explicitly reconciled.

## Runnable verification contracts

Each `CHECK` is one executable operation. Do not combine a shell command with prose using
“and”, “관련”, “필요한”, “선택한”, “대조”, “실행한다”, “검증한다”, `show/runs`, or
`view/checks`. Use separate checks when independent operations are required.

### Gradle MCP

The project never runs Gradle through terminal. A task body records the intended MCP request,
not a fake shell command:

```text
- CHECK: runner=gradle-mcp; tasks=:test; tests=io.example.MediaServiceTest,io.example.MediaRepositoryTest; CWD=C:/repo
  EXPECT: process exit code 0; both named tests pass.
- CHECK: runner=gradle-mcp; tasks=:test; tests=io.example.ModularityTest; CWD=C:/repo
  EXPECT: process exit code 0; Modulith structure test passes.
```

Use actual fully-qualified test identifiers discovered in the repository. When a test does not
exist yet because the task creates it, state its planned Java FQCN and require its presence in
the changed paths/review evidence. A live card may not use `planned-...`, a hyphenated label,
an unqualified class name, or “relevant tests” as `tests=`. If the package/FQCN cannot be
determined, create a blocked investigation rather than an executable implementation card.
For Flyway/JPA work name the
repository/integration test and migration verification target. `EXPECT` must state exit code 0
and a result tied to the named test/check, not merely “validation succeeds”.

### Shell and GitHub checks

Store complete commands with their options and operands:

```text
- CHECK: git -C C:/repo rev-parse HEAD
  EXPECT: output equals final_review_base_sha.
- CHECK: git -C C:/repo status --porcelain
  EXPECT: output is empty.
- CHECK: hermes --profile project-manager kanban --board <slug> show t_<hex> --json
  EXPECT: task status, assignee, comments, and run evidence match the handoff.
```

PR commands may use a named value only after a separate producer task records it. Split PR
creation from CI/merge finalization; `producer_task_id: self` is invalid:

```text
runtime_bindings:
  pr_number:
    producer_task_id: t_<actual-pr-create-task-id>
    evidence: comment.pr_number
- CHECK: gh pr checks ${pr_number} --repo <owner/repository>
```

The producer card must contain an exact `gh pr create` contract and persist the literal PR number
and URL before the consumer becomes ready. At execution, resolve the binding from that evidence.
`self`, `recorded-pr-number`, `planned-*`, an undeclared `${name}`, or another prose token is
not an executable operand. A new task declares a manual review contract, not a verdict:

```text
manual_review_contract:
  reviewer: <Profile>
  subject: <fixed SHA, code range, or task IDs>
  evidence: <durable comment/run/metadata fields>
  allowed_verdicts: approved | request-changes | blocked
```

The actual verdict is written to native comment/run/metadata only after review. A body-level
`manual_review.verdict` on an unexecuted task is false evidence.

## Same-card review evidence

Implementation work stays on the same card:

```text
ready → running → review → done
changes-requested → running
```

The body contains only `review_handoff_contract`. After focused verification, the Coder calls
native `request-review` and leaves sanitized metadata/comment evidence:

```text
verified_sha: <literal SHA>
validator: <runner>
check_id: <exact task/test identifier or check category>
result: pass | fail
changed_paths: <repository-relative paths>
residual_risk: <none or bounded risk>
```

The reviewer reads task `show`, `runs`, comments, and the declared source workspace before a
verdict. Missing evidence means `request-changes` or `blocked`; natural-language “done” is not
evidence. A separate verification task is reserved for independent work such as security
review, an integration-test implementation, or Issue-final regression.

## Quality review template

Create quality cards only after a final implementation commit/freeze producer exists. It owns:

```text
final_review_base_sha: <literal committed SHA>
review_workspace: <clean workspace path>
```

Each quality and coordinator card names its actual predecessor and freeze producer; a prose
placeholder such as `final_review_base_sha: set by ...` or `recorded by ...` is invalid. Until
the producer runs, omit that value from the consumer body and keep only its producer ID:

```text
depends_on_task_id: t_<actual-predecessor-id>
final_review_base_sha_producer_task_id: t_<actual-freeze-task-id>
manual_review_contract:
  reviewer: reviewer-general | reviewer-deep | reviewer-coordinator
  subject: <fixed SHA and task/diff range>
  evidence: show/runs/comments plus named validator output
  allowed_verdicts: approved | request-changes | blocked
```

Quality axes run in order: maintainability, persistence, architecture, compatibility, then
coordinator. The coordinator reads all four cards and preserves findings, severity, confidence,
path/line, evidence, impact, recommendation, and next routing. Code does not change between
quality axes. If a finding requires a fix, create a bounded refactor task, reverify it, and
repeat only the affected review axes against a newly frozen SHA.

## Fail-closed board read-back

After all links exist, read every live task with `show --json` and evaluate the board
programmatically. Reject promotion and a “graph created” report when any condition fails:

- all required outcomes are mapped or explicitly blocked; evidence, scope/out-of-scope,
  acceptance criteria, owner, dependency and verification exist;
- `contract_version` is exactly `board-contract-v1`, `task_type` is valid, and every
  implementation acceptance ID maps to a real behavioral check; structural/style checks do not
  satisfy behavioral outcomes;
- every repository evidence source uses structured `kind/path/heading|lines` and resolves in the
  committed `planning_head_sha`; each named
  heading or line range exists and supports its claim;
- titles and human-readable fields are Korean rather than English-only, while literal IDs,
  paths, SHAs, symbols, commands, enum values and field keys remain unchanged;
- body uses real newlines and its workspace kind/raw path/CWD match the native envelope;
- every stored task reference is an existing literal `t_<hex>` ID; aliases and placeholders
  such as `t01`, `step-1`, or titles do not appear in persisted contracts;
- parent/child links are symmetric and acyclic; `decomposition_depth` is `1..3`; the declared
  `dependency_path_length` equals the longest path calculated from native parents after linking;
- every absent prerequisite has a producer or blocked decision and no consumer excludes a
  required missing producer from scope;
- each `CHECK` has a runner or complete command, exact targets, matching CWD and success-only
  `EXPECT`; prose targets such as `관련`, `필요한`, `선택한`, `대조`, `실행한다`, or `검증한다`
  are invalid; Java `tests=` values are FQCNs without `planned-`/hyphen placeholders;
- every non-literal runtime operand uses a declared `runtime_bindings` entry with a separate live
  producer and durable evidence location; `final_review_base_sha`, when present, is a literal
  SHA rather than prose; self-produced runtime bindings are invalid;
- each manual review contract has reviewer, subject, evidence fields and allowed verdicts;
  implementation bodies have `review_handoff_contract` but no pre-execution result metadata;
  actual review metadata after execution has `verified_sha`, `validator`, `check_id`, `result`,
  `changed_paths`, and `residual_risk`; quality/coordinator cards have a live
  `final_review_base_sha_producer_task_id`;
- a stale board contains exactly one reconciliation/investigation task; a fresh graph has one
  executable ready leaf unless parallelism is justified; no done task lacks accepted evidence.

Run both gates from the repository root:

```text
python .hermes/profile-distributions/project-manager/scripts/validate_board.py --input <draft.json> --repository <repository-root>
python .hermes/profile-distributions/project-manager/scripts/validate_board.py --board <slug> --profile project-manager --repository <repository-root>
```

`task_runs=[]` and a dirty working tree are normal for a fresh graph; dirty-path overlap is a
claim/edit concern, not a generation failure. Keep evaluation read-only unless repair is
explicitly requested.

## Failure routing

- Missing or conflicting source evidence: `blocked` or `needs-input` with the locator.
- Wrong workspace/body contract before execution: do not patch SQLite; obtain authorization for
  archive/recreate through supported Kanban lifecycle.
- Failed validator: record the rule/check, sanitized result, and next action; do not weaken it.
- Missing review evidence: `request-changes` or `blocked`.
- Stale or mixed quality SHA: block the sequence and create/reuse the freeze/review path.
- Observable workflow deviation: comment with `deviation_kind`, expected state, observed state,
  evidence locator, and next action. The audit plugin logs only sanitized facts.

Never put credentials, tokens, passwords, connection strings, raw tool output, complete prompts,
or reasoning in task text, comments, metadata, or audit evidence.
