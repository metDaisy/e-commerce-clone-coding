---
name: create-triage
description: Create an Issue triage plan before executable task graph.
version: 0.1.0
author: Amaazon project, Hermes Agent
license: MIT
platforms: [windows, linux, macos]
metadata:
  hermes:
    tags: [project-management, triage, kanban, requirements]
    related_skills: [service-planning, sync-docs, build-task-graph, update-current-state]
requires_toolsets: [kanban]
---

# Create Triage Skill

Create one PM-owned Kanban triage card for a leaf Issue before creating an executable
Coder graph. The card captures the implementation idea, document impact, policy
questions, and handoff context. It is a planning record, not a Coder contract and
not a substitute for `service-planning`, `sync-docs`, or `build-task-graph`.

## When to use

Use this Skill for every new leaf Issue before `build-task-graph`.

Do not use it to implement source, tests, migrations, or to decide business policy.
If an existing active triage card exists for the Issue, resume it instead of creating
a duplicate.

## Prerequisites

- Read the repository `AGENTS.md` and this Profile's PM workflow documents.
- Confirm a clean repository and a selected leaf Issue.
- Confirm `current-state.md` is `fresh` before creating the Kanban card. Route
  `stale` or `insufficient` state to `update-current-state` instead.
- Read the Issue lineage, approved requirement sections, and relevant reference
  documents.
- Confirm the native Kanban plugin surface is available. Do not create a partial
  card through an unsupported mutation path.

## Kanban lifecycle

Use the native Kanban statuses exactly as read back from the plugin:

```text
triage → running → done
          ├──────→ blocked → running
          └──────→ done
```

`todo` and `ready` are valid native queue states when the plugin requires an
explicit promotion before claiming. The initial create-triage card should use
`triage` when available. If a runtime cannot expose `triage`, create `todo`, validate
the body, then promote it to `ready` before claiming.

`blocked` means a concrete blocker is recorded in the body. Do not invent a
`needs-input` Kanban status: represent the reason as `blocker.kind` and keep the
native status `blocked`.

A completed triage card is frozen. The card is not a runtime dependency of the
execution graph; its ID is provenance for PM and `build-task-graph` only.

## Required body

Create a canonical JSON body. Use the schema in
[`references/triage-contract.md`](references/triage-contract.md) and generate the
initial draft with `scripts/triage.py template`. The JSON is PM and
`build-task-graph` input; it is not a user-facing policy discussion surface.
The body must include:

- Issue identity, planning baseline SHA, and fresh current-state snapshot;
- goal, scope, and explicit out-of-scope;
- current and desired behavior;
- implementation idea, candidate vertical slices, candidate dependencies, and
  verification direction;
- a document-impact object for each candidate `requirement`, `architecture`, `ADR`,
  `glossary`, `ERD`, and `index` document;
- a separate read-only current-state entry;
- policy findings and structured decision requests when needed;
- a `build-task-graph` handoff section.

The template takes only facts PM has already read; it does not infer an Issue's
requirements. Write its generated artifact under the current repository's `.temp`:

```text
TRIAGE_PY=.hermes/profile-distributions/project-manager/skills/create-triage/scripts/triage.py
python "$TRIAGE_PY" template --issue 138 --title "Issue #138 triage" \
  --issue-url <Issue-URL> --planning-sha <SHA> --current-state-sha <SHA> \
  --requirement-locator <path#locator> --current-state-locator <path#locator> \
  --output .temp/triage/issue-138/draft.json
```

For each reviewed document, use exactly one of `update`, `no-change`,
`not-applicable`, or `blocked`. `no-change` means the document was reviewed and
needs no edit. `current-state` uses `read-only` and is never edited by this Skill.

## `triage.py`

`scripts/triage.py` is a deterministic helper, not a Kanban controller.

| Command | Input / output | Responsibility |
|---|---|---|
| `template` | literal Issue context → `.temp/triage/issue-<n>/draft.json` | Create a JSON draft without inventing requirement content. |
| `validate <body> --status <status>` | JSON body → stdout JSON verdict | Check the triage-v1 schema, document-impact dispositions, current-state read-only boundary, and status gate. |
| `validate-card --task-id <id> --board <slug>` | native card read-back → stdout/report verdict | Use only official `hermes kanban ... show --json` to compare native identity/status/assignee with the JSON body. |

`template` and optional `validate-card --report` artifacts must use a relative
`.temp/...` output path. `validate` writes no artifact. The helper never invokes
Kanban create, edit, link, claim, block, unblock, or complete; PM performs every
Kanban mutation through native `kanban_*` tools and reads it back with
`kanban_show`. In this repository distribution, set `TRIAGE_PY` to the path above;
when installed, resolve it as `<installed-skill-root>/scripts/triage.py` rather than
assuming the process working directory is the skill directory.

## Procedure

1. **Admit the planning baseline.** Read back Issue scope and lineage, approved
   requirements, clean repository status, planning HEAD, and current-state
   freshness. Completion criterion: all locators and the fresh snapshot are
   recorded, or a blocker is routed without card mutation.
2. **Draft and validate the plan.** Generate an Issue-specific JSON draft under
   `.temp/triage/issue-<number>/` with literal Issue/requirement/current-state
   inputs, then populate it from the contract reference. Keep source
   locators for PM traceability, but do not instruct a future Coder to rediscover
   or reinterpret those documents. Completion criterion: the body passes the
   deterministic `python "$TRIAGE_PY" validate <body.json> --status triage` command and
   every candidate document has a disposition.
3. **Reuse or create and read back.** Find an active triage card for the leaf Issue.
   Reuse exactly one; otherwise create `Issue #<number> triage` assigned to
   `project-manager` with the validated JSON body in native `triage` status. Read
   back the actual task ID, status, assignee, workspace, and body. A read-only
   `python "$TRIAGE_PY" validate-card --task-id <actual-id> --board <board>` check may verify
   the same envelope through the official CLI. Completion criterion: no duplicate
   active triage card exists.
4. **Claim and inspect.** Claim the card through the native lifecycle so it is
   `running`. Compare the requirement, Issue, current-state, and candidate
   documents. Use limited source investigation only for a documented conflict,
   missing structural constraint, or current-state discrepancy. Completion
   criterion: every finding has evidence and an impact classification.
5. **Handle a clean plan.** If no policy decision or unresolved requirement gap
   remains, set `build_task_graph.allowed: true`, record the selected handoff,
   complete the card through the native Kanban action, and read it back as
   `done`. Freeze the body. Completion criterion: the frozen body is sufficient
   for PM to author the Coder graph without reopening the same planning question.
6. **Handle a policy or requirement blocker.** Set native status `blocked` and
   record structured blocker and decision-request data in JSON. Create a linked
   `Issue #<number> service-planning` card with a human-readable Markdown body.
   That card presents why the decision is needed, evidence, options, impacts, and a PM
   recommendation.
   Do not select an option or update the requirement autonomously. Completion
   criterion: the triage card is blocked and the service-planning handoff is
   read back.
7. **Resume after the decision.** When service-planning is complete, read back the
   user decision, requirement change, affected document updates, and Issue
   synchronization. Resume the same triage card as `running`, update and revalidate
   its body, then complete it only after the handoff is unblocked. Completion
   criterion: no unresolved policy finding remains and all external mutations are
   read back.
8. **Hand off.** Invoke `build-task-graph` with the frozen triage body, approved
   requirement, fresh current-state, and Issue. The Coder receives only the
   resulting self-contained task card. Completion criterion: graph authoring has
   the triage task ID as provenance and does not use the triage card as a runtime
   dependency.

## Policy decision request

Copy the JSON decision-request data into the linked `service-planning` card and show
the user a human-readable Markdown request containing:

```yaml
decision_request:
  id: DR-001
  problem: <what is contradictory or missing>
  why: <why implementation cannot safely proceed>
  evidence:
    - locator: <repository-relative path and heading or line range>
      supports: <claim>
  options:
    - id: A
      choice: <option>
      impact: <behavior, API, data, UI, or delivery impact>
  recommendation:
    option: <optional PM recommendation>
    reason: <evidence-based reason>
  decision_owner: user
  decision_status: pending
  approved_change: null
```

A recommendation is not a decision. The requirement is updated only after the user
approves a policy choice. Then use `sync-docs` for affected derived documents and
Issue metadata, read both targets back, and resume the same card.

## Verification

Before reporting triage completion, read back:

- native task ID, title, status, assignee, JSON body, linked service-planning card if any;
- current-state snapshot SHA and freshness;
- every document-impact disposition;
- decision and requirement synchronization evidence when applicable;
- frozen JSON body and `build_task_graph.allowed` value.

Report facts, user decisions, blockers, verification, external-state read-back, and
next transition separately. Never report `done` while a policy decision, document
sync, or required read-back is unresolved.

## Pitfalls

- Do not create a second active triage card for the same leaf Issue.
- Do not treat `no-change` as unreviewed; use `not-applicable` for excluded documents.
- Do not edit `current-state.md` during triage.
- Do not turn triage ideas into executable `CHECK`/`EXPECT` contracts; that belongs
  to `build-task-graph`.
- Do not let a Coder infer missing requirements from source documents.
- Do not use direct messages as the authoritative handoff.
- Do not claim a native transition succeeded without reading the exact card back.
