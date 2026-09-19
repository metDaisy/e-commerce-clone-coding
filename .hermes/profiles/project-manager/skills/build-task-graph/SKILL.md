---
name: build-task-graph
description: "Build manual Kanban graphs from approved Issue contracts."
version: 0.4.0
license: MIT
metadata:
  hermes:
    tags: [project-management, kanban, task-graph, requirements, verification]
    related_skills: [create-triage, service-planning, sync-docs, update-current-state]
requires_toolsets: [kanban]
---

# Build Task Graph

Turn an approved Issue contract into a manually authored native Kanban graph. The
Project Manager owns graph membership and promotion; native Kanban owns lifecycle,
dependencies, task IDs, assignees, runs, comments, and evidence. Coder cards remain
self-contained: they implement the current behavior contract, never a history of its
requirement deltas.

Read [`references/board-contract.md`](references/board-contract.md) before drafting. For every backend
Impl, also read the PM-owned
[`references/implementation-card-contract.md`](references/implementation-card-contract.md). The board
contract owns graph identity, topology and promotion; the implementation-card contract owns PM authoring
semantics and body validation.
`SOUL.md` owns role boundaries; `controll-task-graph` owns checkpoint, review routing, and release closure.

## Modes

- `new`: create generation 1 for a selected leaf Issue.
- `requirement-rework`: replace an active, unfinished generation after an approved
  requirement revision. Do not use after its aggregate review has completed; create a
  new Issue instead.
- `review-rework`: completed aggregate Review finding을 읽어 같은 generation에 필요한 corrective
  work, context re-review, 또는 policy Decision을 append한다.

## Native graph model

Use native links only for execution dependencies. A Kanban parent must be `done`
before its child becomes eligible, so the semantic Issue root is a native aggregate
child rather than the native parent of its implementation cards.

```text
G<N>-Issue<M>-Triage
Triage → first eligible Impl → Review<Q> → Summary
Review<Q> → corrective Impl → Review<Q+1> → Summary
Review<Q> → Decision → corrective Impl
```

- `G<N>-Issue<M>-Triage` is immutable planning provenance after completion.
- `G<N>-Issue<M>-Summary` is the semantic Issue root and PM finalization card.
- `G<N>-Issue<M>-Impl<P>` is one Coder contract.
- `G<N>-Issue<M>-Review<Q>` is one aggregate Reviewer task.
- `G` increments only for a user-approved requirement revision. `Impl`, `Decision`, and `Review` increment
  for new work within that generation; `Review` increments for aggregate review
  rounds. Do not put `rework`, `replacement`, or `corrective` in titles.

Card bodies preserve immutable semantic contracts and generation lineage. Summary membership is
computed from native direct-parent read-back, while native links express actual prerequisites. Backend
Impl bodies use the canonical `backend-implementation-card-v1` schema linked from the board contract.

## Manual authoring rule

Do not use the built-in Kanban decomposer. Create cards manually through native
Kanban actions. Atomic graph creation is not required when every new Impl/Review
card is created in `todo`, read back, and no card is promoted before the entire graph
has passed the draft and native read-back checks.

Use `scripts/build_task_graph.py template` to create a fact-only backend Impl scaffold under `.temp`,
then author it from approved inputs and run `validate`. The helper enforces the canonical
`backend-implementation-card-v1` body only; graph topology and native persisted state remain separate
manual read-back gates.

1. Keep Triage as the PM-owned `running` creator-session gate. Create the first eligible Impl with
   Triage as its parent; a parentless native task becomes `ready` immediately. Create every other new
   Impl and Review with its actual prerequisite parent so it starts in `todo`. Use task IDs only after
   native read-back.
2. Link initial Impl to Review1 and Summary. Link every Review to Summary. A Review-originated
   Impl is its child and is a parent of the next Review and Summary; Decision is a Review child and
   Summary parent.
3. Verify all bodies, assignees, links, and `todo` statuses through native read-back.
4. Complete Triage only after the graph passes read-back. Verify its dependency promotion makes exactly
   one eligible Impl `ready` and leaves every other execution card in `todo`.

The PM decides `todo → ready`. The native dispatcher claims a ready assigned card and
spawns its worker, causing `ready → running`; a model override affects that dispatch
but is not the lifecycle transition itself.

## New generation procedure

1. **Admit.** Read the leaf Issue, approved requirement, clean repository and
   baseline, active workflow marker, fresh `current-state` snapshot, and existing
   native cards. Completion: all input facts are read back or routed to their owner.
2. **Author triage.** Create or claim `G1-Issue<M>-Triage` as the PM creator-session's `running`
   planning gate with the complete delivery
   behavior model, document evidence, scope, exclusions, aggregate acceptance, and
   graph membership plan. Completion: its body is validated and read back.
3. **Classify each behavior.** When its domain tools are exposed in the fresh PM
   session, use Codebase Memory to inspect index state and trace candidates; confirm
   results with committed source/test/migration snippets. Use Semble to locate related documents
   or unknown behavior, then confirm candidates directly. Classify each behavior as
   `implemented`, `partial`, `absent`, or `unknown`. Completion: every behavior has
   source-backed state evidence or an explicit unresolved fact.
4. **Author work.** Create an Impl card only for `partial` or `absent` behavior. Give it the full
   desired behavior contract, confirmed implementation context, explicit scope/exclusions, every backend
   applicability dimension, acceptance criteria, focused verification, full backend verification, and
   traceability. Do not add a baseline SHA or frontend contract. Completion: every new card validates as
   `backend-implementation-card-v1`, is self-contained, and is assigned.
5. **Add review and start.** Create `Review1` and `Summary`, link the dependency graph,
   complete the manual authoring rule, then complete the Triage artifact so native dependency promotion
   releases the first Impl. Completion: native read-back shows one ready Impl and every
   other new execution card in `todo`.

## Requirement-rework procedure

1. **Freeze the new requirement.** Require an approved requirement revision plus
   derived-document and Issue read-back. Build a Git diff artifact from the active
   generation's requirement basis revision to the new revision; normalize it into
   behavior deltas, not file or hunk tasks. Completion: each delta states the prior
   rule, new rule, observable consequence, dimensions, and diff evidence.
2. **Stop obsolete work truthfully.** Preserve `done` cards unchanged. Block an
   affected running card with the requirement change as the actual blocker and wait
   for its run to terminate. Archive unfinished Impl cards and an unfinished Review
   after their replacement plan is ready. Completion: no obsolete task remains
   dispatchable.
3. **Create the next triage artifact.** Create `G<N+1>-Issue<M>-Triage` in `triage`.
   Its body records the prior Summary, archived unfinished tasks, inherited done evidence, new requirement
   revision, and an effective behavior model such as `A′ = A + AA`. Completion: the
   new root can explain every desired behavior without asking a Coder to read G<N>.
4. **Classify effective behaviors.** For every behavior in the new root, inspect the
   current committed code against the whole desired contract (`A′`), not merely its
   delta (`AA`). Reuse done evidence only when confirmed source/test evidence still
   satisfies the new behavior. Create new Impl cards for confirmed `partial` or
   `absent` behavior. Completion: every behavior is inherited, planned, or explicitly
   unknown.
5. **Build and start the next graph.** Follow the manual authoring rule, complete the
   new Triage artifact, then archive only superseded unfinished execution cards after
   the replacement graph is fully read back. Preserve done history. Promote only the
   first eligible new Impl card. Completion: the new generation is the only
   dispatchable active plan.

## Review-rework procedure

1. **Read the handoff.** PM creator-session resume 또는 fresh recovery에서 done Review의 latest
   run metadata, task body, comments, children, Summary parents를 read-back한다. `findings`만
   routing input으로 사용한다.
2. **Route every finding.** `correction-required`는 기존 effective behavior A를 충족하는
   self-contained Impl로 materialize한다. A의 구현 결함 AA는 requirement delta A′가 아니다.
   `context-required`는 approved source를 직접 확인한다. `decision-required`는 같은 G의
   `Decision<R>`을 native `blocked`로 만든다; PM은 정책을 채우지 않는다.
3. **Append without rewriting history.** completed Review/Impl/Summary body를 수정하거나 link를
   제거하지 않는다. source Review→Impl/Decision, new Impl→next Review/Summary, every
   Review/Decision→Summary link를 create/read-back한다.
4. **Create the next review last.** 모든 blocking finding의 context와 corrective contract가
   확정된 뒤 Review<Q+1>을 만든다. 이전 finding마다 새 disposition을 요구하며 aggregate scope는
   complete effective behavior다.
5. **Recover idempotently.** source Review/finding provenance와 idempotency key로 기존 children과
   Summary parents를 확인한다. 없는 card만 만들고, graph read-back 뒤 하나의 eligible Impl만
   `ready`로 promotion한다.

## Blockers and review

- Continue source investigation while evidence is merely incomplete. Use native
  `blocked` only when requirement policy, ownership, authorization, consistency,
  error semantics, or an external prerequisite cannot be resolved by investigation.
- A Coder uses `running → review` only after focused tests and the backend full test suite pass. PM reads
  the canonical handoff, verifies the diff and commit boundary, commits, and reads back the result SHA,
  committed paths, and clean worktree; only then does the Impl card become `done`.
- `Review<Q>` is a Reviewer-owned aggregate task, normally `todo → ready → running → done`.
  `done` means review execution finished; finding disposition, not status alone, determines Summary eligibility.
- Summary becomes `ready` only after every direct parent is done and latest Review has no unresolved or
  new blocking finding. PM claims it, verifies final delivery conditions, then completes it.

## Report

Report in Korean:

```text
## build-task-graph report
- mode: new | requirement-rework | review-rework | blocked
- generation / root:
- requirement_basis / revised_requirement:
- behavior_classification:
- inherited_done_evidence:
- created_cards / archived_cards:
- native_readback:
- ready_card:
- blockers_or_unknowns:
- next_transition:
```

Never report a behavior as implemented, a card as done, or a graph as dispatchable
without the corresponding source/evidence and native state read-back.
