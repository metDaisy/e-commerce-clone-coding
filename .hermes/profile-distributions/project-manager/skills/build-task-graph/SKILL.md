---
name: build-task-graph
description: "Use when a completed PM triage, approved requirement, fresh current-state, and leaf Issue must become a validated Coder task graph. Create only the v0.1 new-delivery JSON draft; block native graph mutation until the atomic Kanban graph-create capability exists."
version: 0.1.0
license: MIT
metadata:
  hermes:
    tags: [project-management, kanban, task-graph, requirements, verification]
    related_skills: [create-triage, service-planning, sync-docs, update-current-state]
requires_toolsets: [kanban]
---

# Build Task Graph

Turn a **frozen, completed `create-triage` card** plus an approved requirement,
fresh `current-state.md`, and selected leaf Issue into a self-contained Coder graph.
The Coder must be able to implement from its own card body; locators are PM evidence,
not an instruction to rediscover or reinterpret documents.

This is a model-invoked workflow. A completed triage handoff may name this Skill as
its next transition, but handoff does not itself mutate Kanban.

## v0.1 boundary

Only `new-delivery` is supported. Reject `requirement-rework` and `review-rework`
with `blocked`, name the unsupported mode, and preserve the input evidence for the
future mode-specific procedure.

v0.1 creates and validates a JSON graph **draft**. It does not sequentially create
native cards. Native graph mutation requires a runtime atomic graph-create action
that validates and persists the entire graph before exposing exactly one ready child.
If that action is absent from the current Kanban schema, report `blocked`; do not use
repeated `kanban_create`, temporary false blockers, an assembly gate, direct database
writes, or a CLI fallback to imitate atomicity.

Read [`references/board-contract.md`](references/board-contract.md) before drafting.
It owns JSON shape, typed locators, child/root fields, verification model, and all
deterministic validator rules. `SOUL.md` owns PM identity and approval boundaries;
`create-triage` owns the prior raw planning record; `controll-task-graph` owns
execution checkpoints and closure.

## Admission

Read back all of the following before creating a draft:

- selected leaf Issue and root-to-leaf lineage;
- completed `create-triage` card body, status, and `build_task_graph.allowed: true`;
- approved requirement locator and relevant section;
- fresh `current-state.md` locator and snapshot SHA;
- clean repository status, planning baseline SHA, delivery branch, and active
  workflow marker;
- existing cards/dependencies when the issue has prior graph history;
- runtime Kanban schema, including whether an atomic graph-create capability exists.

Route instead of guessing:

- missing or unfrozen triage, or `allowed: false` → triage/service-planning;
- requirement gap or policy conflict → user decision, then requirement and
  `sync-docs` read-back;
- stale/insufficient current state → `update-current-state`;
- dirty tree, marker, or branch mismatch → `controll-task-graph`;
- absent atomic graph-create capability → `blocked` after draft validation.

Use limited committed-source investigation only for a document conflict, required
structural constraint, or Coder/Reviewer-reported implementation discrepancy.

## Draft workflow

Set the helper path relative to this distribution during repository work:

```text
GRAPH_PY=.hermes/profile-distributions/project-manager/skills/build-task-graph/scripts/build_task_graph.py
```

1. **Create the scaffold.** Generate an Issue-specific draft below `.temp` using
   literal facts already read. The helper does not infer requirements or mutate
   GitHub/Kanban.

   ```text
   python "$GRAPH_PY" template \
     --issue <number> --title <Issue-title> --issue-url <Issue-URL> \
     --planning-sha <40-hex-SHA> --current-state-sha <40-hex-SHA> \
     --output .temp/task-graphs/issue-<number>/draft.json
   ```

2. **Refine triage slices.** Treat triage candidate vertical slices as raw planning.
   Merge or split them only from the approved requirement and current-state facts.
   Build the smallest complete graph: every child has one independently verifiable
   outcome; the root depends on every child; exactly one implementation child is
   initially eligible.

3. **Author independent JSON bodies.** Add one draft card per Coder child and one
   root-review card. Use the common envelope, typed goal/state evidence, scope,
   explicit exclusions, applicability objects, acceptance criteria, coverage, and
   verification contracts from the board contract. Assign children to
   `implementation-coder` and the root to `reviewer-general` unless a future
   exception contract authorizes another Profile.

4. **Write verification contracts.** Each check describes one operation. Every
   acceptance criterion maps to one or more behavioral check IDs. Structural, style,
   and state checks add supporting evidence but do not alone satisfy observable
   acceptance. Record manual observations only in `manual_verification`; in v0.1
   they cannot cover an acceptance criterion.

5. **Validate.** The draft must pass before any native mutation is considered.

   ```text
   python "$GRAPH_PY" validate \
     .temp/task-graphs/issue-<number>/draft.json
   ```

   The validator hard-fails malformed JSON, invalid locators, absent goal/state
   evidence, incomplete applicability objects, uncovered acceptance criteria,
   invalid executor vocabulary, duplicate keys, cycles, incomplete root coverage,
   and multiple initially eligible children.

6. **Handle the runtime boundary.** If the atomic native capability is absent,
   retain the validated draft at its `.temp` path and report `blocked` with the
   precise missing capability. Do not claim a Kanban graph exists. If a future
   runtime exposes the documented action, load its capability contract before
   submitting the validated draft, then read back every persisted card, actual ID,
   dependency, assignee, and ready state.

## Card-output rules

- Child and root bodies are independent JSON objects, not copies of the whole graph.
- Draft links use stable `card_key`; only the future atomic creator resolves them to
  actual native task IDs for persisted bodies.
- Do not copy the triage task ID or raw triage plan into Coder cards.
- Persist only source-backed, repository-relative decisions; keep credentials, raw
  tool output, prompts, and reasoning out of drafts and cards.
- Card bodies declare a review handoff contract. Actual `verified_sha`, check
  results, changed paths, and residual risk are created later in native review
  metadata/comments, never prefilled in a draft.

## Report

Report in Korean and distinguish facts from blocked work:

```text
## build-task-graph report
- mode: new-delivery | blocked
- issue:
- triage_admission:
- planning_baseline_sha:
- current_state_snapshot_sha:
- draft: <relative path> | none
- validator:
- atomic_graph_create: available | missing
- native_graph_readback: none | result
- blockers_or_unknowns:
- next_transition:
```

Do not report graph creation, a ready Coder child, or task IDs until the atomic native
creator has persisted and read back the graph.
