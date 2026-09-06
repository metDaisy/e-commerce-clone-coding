---
name: write-task
description: Use when creating evidence-backed Amaazon Kanban task graphs.
version: 0.1.0
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

Use native `kanban_*` tools when present. Otherwise inspect `hermes kanban --help` and
use the official CLI. Select the board explicitly for every list/create/link/promote/
comment/read-back. If neither surface exists, stop without claiming a graph was created.

Write titles and human-readable text in Korean. Preserve literal IDs, URLs, paths,
SHAs, symbols, CLI commands, status enums, and structured keys such as `goal`, `state`,
`constraint`, `CHECK`, and `EXPECT`.

## Task body schema

Pass a real multiline body; literal `\\n` is invalid. Every executable task contains:

```text
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

goal/state/constraint evidence:
- source: <direct file heading/line range or Issue URL>
  claim: <supported fact>

scope:
- <owned outcome>
out_of_scope:
- <explicit exclusion>
acceptance_criteria:
- 성공: <observable result>
- 실패: <rejection/error result>
- 경계: <preserved contract>
verification:
- CHECK: <actual runner + exact command/test identifier + working directory>
  EXPECT: <success-specific condition>
```

Implementation tasks require at least one `goal` and one `state` evidence item and add:

```text
review_handoff:
- focused verification 후 native request-review를 호출한다.
- metadata: verified_sha, validator, test identifier or check category, result,
  changed_paths, residual_risk
- Reviewer는 show, runs, comment read-back 후에만 complete 또는 request-changes를 선택한다.
```

`CHECK` must be executable and `EXPECT` decisive; use a blocked investigation task when
the exact oracle is unknown. Gradle validation uses `gradle-mcp`, never terminal Gradle.

`dir:<path>` and `worktree:<path>` are only `--workspace` selectors. After `create`,
copy the native task record's raw `workspace_kind` and `workspace_path` into the body
and compare both values exactly during read-back.

### Verification contracts

Do not use a prose sentence as `CHECK`. A valid Gradle contract identifies the MCP runner,
the Gradle task, and every test target the worker must request through `gradle-mcp`:

```text
- CHECK: runner=gradle-mcp; tasks=:test; tests=<fully-qualified.TestOne>,<fully-qualified.TestTwo>; CWD=<repository-root>
  EXPECT: Gradle exit code 0 and named tests pass.
```

For a shell/read-back check, store complete commands rather than a joined label such as
`gh pr view/checks` or `show/runs`. If a later value is intentionally unknown, name its
producer and durable location, not a fake command:

```text
manual_review:
  reviewer: <Profile>
  subject: <fixed code range or task IDs>
  evidence: <comment/run/metadata fields to read>
  verdict: approved | request-changes | blocked
```

`EXPECT` describes a success-only observable result. It must not claim that the worker
will choose tests, a SHA, or evidence later.

## Graph construction

1. Reconcile snapshot SHA and HEAD. A stale graph may expose only a reconciliation or
   investigation leaf; implementation/finalization stays `blocked`/`needs-input`.
2. Make the smallest evidence-backed one-to-three-level graph. Give each leaf one owner,
   coherent result, scope/out-of-scope boundary, and independently decisive verification.
   For every `goal` that requires another module or public contract, compare its `state`
   evidence with the source. If the prerequisite is absent, add its producer task before
   the consumer, or record a blocked decision. A consumer must not cite an absent contract
   while excluding its implementation from scope.
3. Select workspace before `create`:
   - `dir:<repository-root>` for current repository inspection, implementation, commit,
     review, or validation; every `CHECK` uses that tree.
   - `scratch` only for research/independent artifacts with no repository input or mutation.
   - `worktree` only for explicitly approved isolated work from a clean committed base.
4. Create prerequisites before dependents that need a body-level reference. Read back the
   generated `t_<hex>` ID, then write that literal ID in
   `depends_on_task_id: t_<actual-id>`. Parent/child graph links remain authoritative;
   omit a body reference if the ID is not yet known. Never store `t01`, `step-1`, title
   aliases, or placeholders.
5. Link actual IDs and read back both envelopes. Verify body line breaks, body references,
   parents/children, assignee, status, workspace kind/path, and `CHECK` CWD.
6. Promote one dependency-satisfied executable leaf unless disjoint parallel work is
   explicitly justified.

### Quality freeze contract

Do not predeclare an unknown `final_review_base_sha` as if it were fixed. Add a final
implementation commit/freeze predecessor. Its acceptance criteria require a comment or
metadata record containing:

```text
final_review_base_sha: <literal committed SHA>
workspace: <clean review tree>
```

Every quality task names that producer task ID and reads the same literal SHA from its
comment/metadata before review. Its manual-review contract identifies the reviewer, review
axis, fixed SHA, clean workspace check, evidence format, and allowed verdict. A different
SHA, dirty tree, or missing producer evidence is `blocked`, not an implicit new baseline.

## Routing and review

Default owners: implementation `prototype-coder`; spec review `reviewer-general`; quality
review `reviewer-general`/`reviewer-deep`; findings `reviewer-coordinator`; approved fixes
`refactor-coder`; commit/push/PR/CI/finalization `project-manager`.

Use same-card review for implementation. Create a separate verification card only for
independently owned work such as security review, integration-test implementation, or final
regression. Quality review uses one clean workspace and `final_review_base_sha` across the
fixed sequence: maintainability → persistence → architecture → compatibility → coordinator.

For a contract deviation, add a concise Kanban comment with `deviation_kind`, expected and
observed state, evidence locator, and next action; choose `block`, `needs-input`, or
`request-changes` truthfully. Never record credentials, raw command/output, prompts, or
reasoning.

## Handoff checklist

Before reporting graph creation, confirm:

- all outcomes are mapped or explicitly blocked;
- evidence, scope, acceptance criteria, `CHECK`/`EXPECT`, owner, and dependencies exist;
- every absent prerequisite cited by a consumer has a producer task or explicit blocked decision;
- all stored task references are existing literal `t_<hex>` IDs;
- no literal `\\n`, planning alias, prose `CHECK`, stale ready implementation, or workspace/CWD mismatch exists;
- body workspace kind/path exactly match native read-back; selectors are not persisted paths;
- each implementation task includes `review_handoff`;
- each quality task has a final-review SHA producer and complete manual-review contract;
- task/graph mutations were read back; only one leaf is `ready` unless justified.

## Detailed evidence rules

Evidence is not a title, search-result summary, or remembered implementation fact. Read the
cited source before writing the claim. Use a separate entry for each purpose:

```yaml
evidence:
  - type: goal
    source: docs/requirement/<path>#<heading> or Issue URL
    claim: required behavior
  - type: state
    source: docs/current-state.md#<heading> or path:Lx-Ly
    claim: observed gap or prerequisite
  - type: constraint
    source: docs/architecture.md#<heading> or ADR/path:Lx-Ly
    claim: architecture, API, data, or security constraint
```

An implementation card needs `goal` and `state`. A task may cite an Issue for outcome scope,
but not instead of a requirement document when one exists. Search results are locators only;
read the file heading or line range before recording it. Preserve competing evidence in
`unknowns`; do not silently select the more convenient claim.

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
“and”, “관련”, “필요한”, `show/runs`, or `view/checks`. Use separate checks when independent
operations are required.

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
exist yet because the task creates it, state the planned identifier and require its presence in
the changed paths/review evidence; never use “relevant tests”. For Flyway/JPA work name the
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

PR commands may use a named value only after the producer records it, for example
`pr_number` from the PR creation result. The body must say where that value is recorded. A
manual check has the structured `manual_review` form from this Skill, not a single sentence.

## Same-card review evidence

Implementation work stays on the same card:

```text
ready → running → review → done
changes-requested → running
```

After focused verification, the Coder calls native `request-review` and leaves sanitized
metadata/comment evidence:

```text
verified_sha: <literal SHA>
validator: <runner and task/test identifier>
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

Each quality card names `final_review_base_sha_producer_task_id: t_<hex>` and uses:

```text
manual_review:
  reviewer: reviewer-general | reviewer-deep | reviewer-coordinator
  subject: <fixed SHA and task/diff range>
  evidence: show/runs/comments plus named validator output
  verdict: approved | request-changes | blocked
```

Quality axes run in order: maintainability, persistence, architecture, compatibility, then
coordinator. The coordinator reads all four cards and preserves findings, severity, confidence,
path/line, evidence, impact, recommendation, and next routing. Code does not change between
quality axes. If a finding requires a fix, create a bounded refactor task, reverify it, and
repeat only the affected review axes against a newly frozen SHA.

## Board read-back evaluator

After generation, evaluate the entire board programmatically rather than trusting creator
prose. For every live task, read `show --json` and aggregate:

- status counts and executable ready leaves;
- parent/child symmetry, cycles, and actual dependency path length;
- body newline serialization and required fields;
- raw workspace kind/path and body CWD agreement;
- literal dependency IDs, absent producer coverage, owner, evidence, and scope consistency;
- CHECK/EXPECT completeness, manual-review schema, review handoff, and quality SHA producer;
- task runs, comments, events, and done tasks without successful evidence.

A stale snapshot is valid only when implementation/finalization remains blocked and a single
reconciliation/investigation leaf is ready. `task_runs=[]` is normal for a fresh graph; a done
task without accepted evidence is not. Keep inspection read-only unless the user explicitly
requests repair.

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
