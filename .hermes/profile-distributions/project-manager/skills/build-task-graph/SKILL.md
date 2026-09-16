---
name: build-task-graph
description: "Build manual Kanban graphs from approved Issue contracts."
version: 0.2.0
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

Read [`references/board-contract.md`](references/board-contract.md) before drafting.
It owns persisted body shapes and validator rules. `SOUL.md` owns role boundaries;
`controll-task-graph` owns checkpoint, review routing, and release closure.

## Modes

- `new`: create generation 1 for a selected leaf Issue.
- `requirement-rework`: replace an active, unfinished generation after an approved
  requirement revision. Do not use after its aggregate review has completed; create a
  new Issue instead.
- `review-rework`: preserve a Reviewer finding and create only the new corrective
  work. Its detailed schema remains unsupported until the finding contract is defined.

## Native graph model

Use native links only for execution dependencies. A Kanban parent must be `done`
before its child becomes eligible, so the semantic Issue root is a native aggregate
child rather than the native parent of its implementation cards.

```text
G<N>-Issue<M>-Impl<P> ─┐
G<N>-Issue<M>-Impl<P> ─┼→ G<N>-Issue<M>-Review<Q> → G<N>-Issue<M>
G<N>-Issue<M>-Impl<P> ─┘
```

- `G<N>-Issue<M>` is the semantic Issue root and PM finalization card.
- `G<N>-Issue<M>-Impl<P>` is one Coder contract.
- `G<N>-Issue<M>-Review<Q>` is one aggregate Reviewer task.
- `G` increments only for a user-approved requirement revision. `Impl` increments
  for new work within that generation; `Review` increments for aggregate review
  rounds. Do not put `rework`, `replacement`, or `corrective` in titles.

Card bodies preserve semantic membership and generation lineage; native links express
only actual prerequisites. The exact persisted field schema is owned by the board
contract after the remaining plan decisions are settled.

## Manual authoring rule

Do not use the built-in Kanban decomposer. Create cards manually through native
Kanban actions. Atomic graph creation is not required when every new Impl/Review
card is created in `todo`, read back, and no card is promoted before the entire graph
has passed the draft and native read-back checks.

1. Create every new Impl and Review card in `todo`; use actual task IDs only after
   each native read-back.
2. Link every Impl as a native parent of its Review card; link the Review card as a
   native parent of the semantic Issue root.
3. Verify all bodies, assignees, links, and `todo` statuses through native read-back.
4. Promote exactly one eligible Impl card to `ready`.

The PM decides `todo → ready`. The native dispatcher claims a ready assigned card and
spawns its worker, causing `ready → running`; a model override affects that dispatch
but is not the lifecycle transition itself.

## New generation procedure

1. **Admit.** Read the leaf Issue, approved requirement, clean repository and
   baseline, active workflow marker, fresh `current-state` snapshot, and existing
   native cards. Completion: all input facts are read back or routed to their owner.
2. **Author the root.** Create `G1-Issue<M>` in `triage` with the complete delivery
   behavior model, document evidence, scope, exclusions, aggregate acceptance, and
   graph membership plan. Completion: its body is validated and read back.
3. **Classify each behavior.** When its domain tools are exposed in the fresh PM
   session, use Codebase Memory to inspect index state and trace candidates; confirm
   results with committed source/test/migration snippets. Use Semble to locate related documents
   or unknown behavior, then confirm candidates directly. Classify each behavior as
   `implemented`, `partial`, `absent`, or `unknown`. Completion: every behavior has
   source-backed state evidence or an explicit unresolved fact.
4. **Author work.** Create an Impl card only for `partial` or `absent` behavior.
   Give it the full desired behavior contract, current state evidence, explicit
   scope/exclusions, conditional API/UI/persistence/state/error contracts,
   acceptance criteria, and behavioral verification. Completion: every new card is
   self-contained and assigned.
5. **Add review and start.** Create `Review1`, link the dependency graph, complete
   the manual authoring rule, then move the root to `todo` and promote one Impl card
   to `ready`. Completion: native read-back shows one ready Impl and every other new
   execution card in `todo`.

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
3. **Create the next root.** Create `G<N+1>-Issue<M>` in `triage`. Its body records
   the prior root, archived unfinished tasks, inherited done evidence, new requirement
   revision, and an effective behavior model such as `A′ = A + AA`. Completion: the
   new root can explain every desired behavior without asking a Coder to read G<N>.
4. **Classify effective behaviors.** For every behavior in the new root, inspect the
   current committed code against the whole desired contract (`A′`), not merely its
   delta (`AA`). Reuse done evidence only when confirmed source/test evidence still
   satisfies the new behavior. Create new Impl cards for confirmed `partial` or
   `absent` behavior. Completion: every behavior is inherited, planned, or explicitly
   unknown.
5. **Build and start the next graph.** Follow the manual authoring rule, then archive
   the superseded root and its unfinished descendants after the replacement graph is
   fully read back. Promote only the first eligible new Impl card. Completion: the
   new generation is the only dispatchable active plan.

## Blockers and review

- Continue source investigation while evidence is merely incomplete. Use native
  `blocked` only when requirement policy, ownership, authorization, consistency,
  error semantics, or an external prerequisite cannot be resolved by investigation.
- A Coder uses `running → review` to request the PM checkpoint. PM reads the
  verification evidence, commit boundary, changed paths, SHA, and clean worktree;
  only then does the Impl card become `done`.
- `Review<Q>` is a Reviewer-owned aggregate task, normally `todo → ready → running
  → done`. It is eligible only after all its Impl parents are done.
- The semantic Issue root becomes `ready` only after Review<Q> is done. PM claims it,
  verifies final delivery conditions, then completes it; requirement rework archives
  an unfinished root rather than reopening or editing historical cards.

## Report

Report in Korean:

```text
## build-task-graph report
- mode: new | requirement-rework | blocked
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
