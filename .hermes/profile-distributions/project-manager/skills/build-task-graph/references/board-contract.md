---
name: build-task-graph-board-contract
version: 4.0.0-draft
---

# build-task-graph board contract v4

This is the single source of truth for `new-delivery` v0.1 draft shape, persisted
card bodies, and deterministic validator rules. Native Kanban owns task IDs, status,
assignee, links, comments, and runs. The card body owns only the PM-authored contract.

## Scope

- Supports only `mode: "new-delivery"`.
- `requirement-rework` and `review-rework` are unsupported in v0.1.
- The graph draft is JSON. Each persisted child/root body is a separate JSON object.
- A frozen triage is an admission input only. No triage task ID or body is copied into
  an execution card.
- Manual verification is recorded but cannot satisfy acceptance coverage in v0.1.

## Typed locator

Every evidence locator is one of these objects.

```json
{"kind":"repository","path":"docs/requirement/p2.md","heading":"상품 등록"}
{"kind":"repository","path":"docs/current-state.md","lines":"10-24"}
{"kind":"issue","url":"https://github.com/owner/repo/issues/138","heading":"완료 기준"}
{"kind":"task","task_id":"t_<native-id>"}
```

A `repository` locator has exactly one of `heading` or `lines`. A `task` locator is
allowed only after native persistence.

## Graph draft

The draft generator and validator consume one UTF-8 JSON object.

```json
{
  "schema": "build-task-graph-draft-v4",
  "mode": "new-delivery",
  "issue": {"number": 138, "url": "https://.../issues/138", "title": "..."},
  "planning": {
    "baseline_sha": "<40 lowercase hex>",
    "current_state_snapshot_sha": "<40 lowercase hex>",
    "state_freshness": "fresh"
  },
  "cards": ["<child or root draft card>"]
}
```

Every draft card has `card_key`, `title`, `assignee`, `depends_on_card_keys`, and
`body`. `card_key` is unique and uses lowercase letters, digits, and `-`. The root
uses child `card_key` values in its draft `target_children`; an atomic native creator
must replace them with actual task IDs before persisting the root body.

## Common persisted body envelope

Every persisted body starts with this shape.

```json
{
  "schema": "build-task-card-v4",
  "card_type": "implementation | root-review",
  "issue": {"number": 138, "url": "https://.../issues/138", "title": "..."},
  "planning": {
    "baseline_sha": "<40 lowercase hex>",
    "current_state_snapshot_sha": "<40 lowercase hex>",
    "state_freshness": "fresh"
  },
  "assignee": "implementation-coder | reviewer-general",
  "evidence": ["<evidence entry>"],
  "scope": ["Korean owned outcome"],
  "out_of_scope": ["Korean explicit exclusion"]
}
```

An evidence entry has `role: "goal" | "state" | "constraint"`, `source`, and
`supports`. Each implementation card requires at least one `goal` and one `state`
entry. `constraint` is optional.

## Applicability object

The following implementation areas always exist and use exactly one shape:
`actor_authorization`, `field_semantics`, `state_invariants`, `error_contract`,
`api_contract`, `persistence`, `ui_flow`, `module_boundary`.

```json
{"applicable":true,"details":["Korean literal contract"]}
{"applicable":false,"not_applicable_reason":"Korean specific reason"}
```

## Implementation card body

An implementation body extends the common envelope with:

```json
{
  "card_type": "implementation",
  "depends_on_task_ids": ["t_<native-id>"],
  "implementation": {
    "observable_behavior": ["..."],
    "actor_authorization": {"applicable": true, "details": ["..."]},
    "field_semantics": {"applicable": false, "not_applicable_reason": "..."},
    "state_invariants": {"applicable": true, "details": ["..."]},
    "error_contract": {"applicable": true, "details": ["..."]},
    "api_contract": {"applicable": true, "details": ["..."]},
    "persistence": {"applicable": false, "not_applicable_reason": "..."},
    "ui_flow": {"applicable": false, "not_applicable_reason": "..."},
    "module_boundary": {"applicable": true, "details": ["..."]}
  },
  "acceptance_criteria": [
    {"id":"AC-CREATE-SUCCESS","outcome":"..."}
  ],
  "acceptance_coverage": [
    {"acceptance_id":"AC-CREATE-SUCCESS","check_ids":["check-create-success"]}
  ],
  "verification": ["<verification check>"],
  "manual_verification": ["<deferred manual check>"],
  "review_handoff_contract": {
    "action":"request-review",
    "reviewer":"project-manager",
    "required_metadata":["verified_sha","check_results","changed_paths","residual_risk"],
    "reviewer_readback":["show","runs","comments"]
  }
}
```

`depends_on_task_ids` is empty for the first persisted child. Draft dependencies are
stored outside the body as `depends_on_card_keys`; the atomic creator replaces them
with actual IDs.

### Verification check

One check describes exactly one execution or observation operation.

```json
{
  "id": "check-create-success",
  "kind": "behavioral | structural | style | state",
  "execution": {
    "executor": "gradle-mcp | npm | terminal | browser",
    "operation": "...",
    "target": "...",
    "cwd": "repository-relative-or-absolute-workspace-path",
    "request": {}
  },
  "expectation": {
    "success_condition": "Korean observable success condition",
    "evidence": "Korean evidence to record after execution"
  }
}
```

The validator checks the executor vocabulary and common fields. Executor-specific
`request` schemas are deferred to a later reference. A `gradle-mcp` request names
Gradle tasks and actual discovered test FQCNs; it never contains a shell Gradle
command.

Each acceptance criterion is covered by one or more check IDs. The referenced check
must be `behavioral`. Structural, style, and state checks are supplementary only.

### Deferred manual verification

```json
{
  "id": "manual-product-form",
  "procedure": ["..."],
  "expected_result": "...",
  "deferral_reason": "manual verification policy is not finalized"
}
```

Manual entries cannot appear in `acceptance_coverage` in v0.1.

## Root-review body

A root body extends the common envelope with:

```json
{
  "card_type": "root-review",
  "target_child_task_ids": ["t_<native-id>"],
  "aggregate_contract": ["..."],
  "cross_boundary_invariants": ["..."],
  "aggregate_exclusions": ["..."],
  "child_evidence_readback": ["show","runs","comments"],
  "review_questions": ["..."],
  "verdict_protocol": ["approved","changes-requested","needs-input"]
}
```

The draft root uses `target_children` with child `card_key` values. A persisted root
uses `target_child_task_ids` with actual native IDs and depends on every child.

## Draft validator invariants

A valid `new-delivery` draft has all of the following.

1. Exactly one root-review card and one or more implementation cards.
2. Unique `card_key` values and no dependency cycle.
3. Every dependency points to another draft card; no implementation card depends on
   the root.
4. The root targets and depends on every implementation child exactly once.
5. Exactly one implementation child has no implementation dependency; that is the
   only initially eligible child.
6. Children use `implementation-coder` and root uses `reviewer-general`, unless a
   future exception contract is added.
7. Every child has goal/state evidence, scope, out-of-scope, all applicability
   objects, non-empty acceptance criteria, behavioral acceptance coverage, and
   unique verification check IDs.
8. Every conditional object is either applicable with non-empty details or
   inapplicable with a non-empty reason.
9. No task ID is invented in a draft or embedded as triage provenance.
10. No secrets, raw tool output, prompt, or reasoning appear in a body.

## Runtime boundary

A draft passing this contract is not permission to create native cards. v0.1 requires
a runtime atomic graph-create capability. If it is absent, report `blocked` with the
missing capability and retain the validated draft outside the board. Do not emulate
atomicity with sequential creation, temporary false blockers, or a mechanical gate.
