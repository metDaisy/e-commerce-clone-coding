# create-triage JSON contract v1

The Kanban task's `body` is one UTF-8 JSON object. Native Kanban owns task identity,
status, assignee, links, events, comments, and runs; this body owns PM planning data.
It must not duplicate `task_id` or a lifecycle state.

## Required fields

```json
{
  "schema": "triage-v1",
  "issue": {"number": 138, "title": "Issue #138 triage", "url": "https://.../issues/138"},
  "planning_baseline_sha": "<40 lowercase hex>",
  "current_state": {"snapshot_sha": "<40 lowercase hex>", "freshness": "fresh", "usage": "read-only", "locators": ["docs/current-state.md#..."]},
  "goal": "...",
  "scope": ["..."],
  "out_of_scope": ["..."],
  "current_behavior": "...",
  "desired_behavior": "...",
  "implementation_idea": "...",
  "candidate_task_slices": ["..."],
  "candidate_dependencies": [],
  "verification_direction": ["..."],
  "document_impact": {},
  "policy_findings": [],
  "decision_requests": [],
  "blocker": null,
  "build_task_graph": {"allowed": false, "provenance_only": true, "coder_reads_triage": false}
}
```

`current_state.usage` is always `read-only`; triage never updates `current-state.md`.
`build_task_graph.allowed` stays `false` until the running triage plan has no blocker and its body is ready
to freeze. The card remains `running` as the first Impl's scheduling parent until `build-task-graph`
validates the complete graph and completes it.

## Document impact

Every `requirement`, `architecture`, `ADR`, `glossary`, `ERD`, and `index` entry has
`decision`, `locators`, `reason`, and `follow_up`. `decision` is one of `update`,
`no-change`, `not-applicable`, or `blocked`. `update` requires a non-empty
`follow_up`; `no-change` records that the document was reviewed.

## Native read-back

Use the actual native ID outside the body as provenance. `triage.py validate-card`
may read one card through the official read-only `hermes kanban ... show --json`
surface and verifies that the returned ID, status, assignee, and JSON body agree.
It never creates, edits, links, or changes a Kanban card.
