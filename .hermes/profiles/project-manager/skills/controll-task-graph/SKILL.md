---
name: controll-task-graph
description: "Coder handoff checkpoint, 다음 task promotion, aggregate Review routing, Summary finalization 또는 release lifecycle을 운영할 때 사용한다."
version: 0.3.0
author: "Amaazon project"
license: MIT
platforms: [linux, macos, windows]
metadata:
  hermes:
    tags: [project-management, kanban, checkpoint, review, handoff]
    related_skills: [build-task-graph, sync-docs, update-current-state]
requires_toolsets: [kanban]
---

# Control Task Graph

Handle the active native review run created when `implementation-coder` requests the same-card PM
checkpoint. This Skill owns checkpoint validation, commit, changes-requested routing, and native read-back.
It does not replace aggregate Review or implement source changes.

Before acting, read the PM-owned
[`references/execution-contract.md`](references/execution-contract.md). It owns checkpoint authority,
changes-request and completion metadata. Card authoring semantics remain in
`../build-task-graph/references/implementation-card-contract.md`.

Use the adjacent `checkpoint.py` for deterministic handoff, change-request, and checkpoint evidence
validation. Resolve its installed Skill path instead of assuming the repository-relative path exists.

## Admission

1. Read the current task with `kanban_show`. Require the same Impl task to be `review`, assigned to
   `project-manager`, and associated with this dispatcher-spawned active review run. An arbitrary PM
   session cannot use `request-changes` or complete the review because native Kanban requires an active
   review run.
2. Read the immutable `backend-implementation-card-v1` body and the latest Coder run whose outcome is
   `review_requested`. Use that run's metadata as the canonical `backend-implementation-handoff-v1`;
   comments and prose summaries are not substitutes.
3. Confirm the workspace and branch match the task, then read Git status and diff. Do not switch
   workspaces, reset, stash, clean, or discard Coder changes.
4. Materialize sanitized card and handoff JSON under `.temp/checkpoints/<task-id>/` and run:

   `python checkpoint.py handoff <card.json> <handoff.json>`

   A nonzero result prevents commit and completion.

## Checkpoint review

1. Compare every changed, staged, and untracked path with `handoff.changed_paths`. Require exact task
   ownership, no documentation path, and no unexplained dirty path.
2. Read the diff for behavior, scope, security, module-boundary, migration, and test evidence. Confirm the
   focused result covers every acceptance and required scenario and the full backend result is
   `gradle-mcp` `test: pass`. Do not claim to have rerun Gradle unless it was actually executed through
   `gradle-mcp`.
3. If implementation changes are required, follow **Changes requested**. If policy or contract meaning is
   missing, block and route it to the user rather than inventing an implementation.
4. Stage only the validated `changed_paths`. Read `git diff --cached`, `--name-only`, and `--check`.
   Choose one self-contained commit using `docs/commit-message-convention.md`; do not include an Issue
   footer unless an Issue number was explicitly supplied as commit metadata.
5. Commit, then read back the full commit SHA, committed paths, commit message, and `git status`. Build
   `backend-implementation-checkpoint-v1` only when committed paths exactly match the handoff, full-test
   evidence still reads `pass`, and the worktree is clean.
6. Run `python checkpoint.py checkpoint <card.json> <handoff.json> <checkpoint.json>`. On success, call
   `kanban_complete` on the same Impl task with a short Korean summary and the checkpoint object as
   metadata.
7. Read back task status `done`, the closing PM run metadata, result SHA, committed paths, and clean Git
   state. Only then may the next native dependency become eligible.

## Changes requested

1. Build `backend-implementation-change-request-v1` with the reviewed `handoff_id`, this active native
   review run ID, and stable finding IDs. Every finding contains exact path, symbol, observed problem,
   expected result, allowed scope, and verification to rerun.
2. Validate it with `python checkpoint.py change-request <request.json>`.
3. Append the validated JSON object as a durable Kanban comment on the same task. Then invoke native
   `request-changes` with the finding IDs and a concise reason. Native `request-changes` has no metadata
   field; the validated comment is therefore the canonical structured payload.
4. Read back that the active PM review run ended, the task returned to `ready` or dependency-gated `todo`,
   and assignee ownership returned to `implementation-coder`. Before `request-changes` succeeds, a failure
   remains in the active review run and may be blocked there. After it succeeds, the PM review run has ended;
   if read-back differs, leave the routed task visible and use a PM creator/fresh recovery session to inspect
   events and repair only through supported native routing actions. Do not assume the ended review run can
   block or mutate the task.

Coder reads the latest validated change-request comment, changes only the stated findings, reruns focused
and full backend verification, and submits a new handoff on the same task.

## Blocked and recovery

- Invalid or incomplete Coder metadata: request concrete changes when implementation evidence can fix it;
  otherwise block with the missing PM/user decision.
- Unexpected dirty paths or workspace mismatch: block without modifying Git.
- Failed commit or non-clean post-commit state: do not complete the task; record the observed fact and
  recover the same review run.
- Never copy handoff facts from prose, infer a missing test result, weaken validator output, or complete a
  card from a different session/workspace.

## Graph lifecycle routing

1. Impl completion read-back 뒤 native dependency가 충족한 다음 child 하나만 진행시킨다.
2. 모든 prerequisite Impl이 `done`이면 Reviewer-assigned aggregate Review를 활성화한다. PM은 Review
   body를 작성하지만 verdict를 대신하지 않는다.
3. Done Review의 canonical finding metadata를 읽는다. Corrective Impl, context re-review 또는 Decision이
   필요하면 `build-task-graph`의 `review-rework`에 넘기고 append된 graph를 read-back한다.
4. 모든 Summary direct parent가 `done`이고 latest Review에 unresolved/new blocking finding이 없을 때만
   Summary를 finalization한다.
5. Final implementation SHA를 고정하고 `sync-docs`, `update-current-state`, PR·CI·merge·Issue closure를
   순서대로 수행한다. 모든 external mutation은 정확한 target을 다시 읽는다.

완료 기준: 각 transition의 native/external read-back, relevant SHA, verification, clean worktree와 다음
eligible task가 확인된다. Card schema와 rework topology는 이 Skill이 아니라 `build-task-graph`가
소유한다.

## Report

```text
status: checkpoint-done | changes-requested | blocked
task/review-run:
handoff_validation:
changed_paths:
commit_sha/message:
checkpoint_validation:
native_readback:
next_transition:
```
