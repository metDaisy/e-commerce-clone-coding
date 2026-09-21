# create-triage JSON 계약 v1

Kanban task의 `body`는 UTF-8 JSON object 하나다. Native Kanban은 task identity, status,
assignee, link, event, comment와 run을 소유하고, 이 body는 PM planning data를 소유한다. Body에
`task_id`나 native lifecycle 상태를 복제하지 않는다.

## 필수 field

```json
{
  "schema": "triage-v1",
  "planning_state": "planning",
  "frozen_digest": null,
  "issue": {"number": 138, "title": "G1-Issue138-Triage", "url": "https://.../issues/138"},
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

`current_state.usage`는 항상 `read-only`다. Triage는 `current-state.md`를 갱신하지 않는다.
`planning_state`는 작성 중 `planning`, handoff 준비가 끝나면 `frozen`이다. `frozen`은
`build_task_graph.allowed: true`, null blocker, 해결된 policy/document 상태와 함께 사용한다.
`frozen_digest`는 해당 field를 제외한 canonical JSON의 SHA-256이며 body 변경을 검출한다. Card는
`build-task-graph`가 graph 전체를 검증·read-back할 때까지 `running`으로 남는다.

## 문서 영향

`requirement`, `architecture`, `ADR`, `glossary`, `ERD`, `index`마다 `decision`, `locators`,
`reason`, `follow_up`을 둔다. `decision`은 `update | no-change | not-applicable | blocked` 중
하나다. `update`, `no-change`, `blocked`는 하나 이상의 locator가 필요하며, `update`는 non-empty
`follow_up`도 필요하다.

## 정책 finding과 결정 요청

Policy finding은 `id`, `problem`, 하나 이상의 evidence locator, `status: open | resolved`를 가진다.
Decision request는 다음 field를 가진다.

```json
{
  "id": "DR-001",
  "problem": "...",
  "why": "...",
  "evidence": ["docs/requirement/p2.md#..."],
  "options": [{"id": "A", "choice": "...", "impact": "..."}],
  "decision_owner": "user",
  "decision_status": "pending",
  "approved_change": null
}
```

`decision_status`는 `pending | approved | rejected | deferred` 중 하나다. `pending`과 `deferred`는
미해결 상태이며 graph gate를 닫는다. `approved`에는 non-empty `approved_change`가 필요하다.
Native `blocked` card에는 `blocker.kind`와 미해결 decision request가 모두 필요하다.

## Graph gate 검증

`build_task_graph.allowed: true`는 다음을 모두 만족할 때만 유효하다.

- `planning_state: frozen`
- Canonical body와 일치하는 `frozen_digest`
- `blocker: null`
- open policy finding과 pending/deferred decision request가 없음
- `document_impact`에 `blocked`가 없음

## Native read-back 검증

실제 native ID는 body 밖의 provenance로 사용한다. `triage.py validate-card`는 공식 read-only
`hermes kanban ... show --json`으로 card 하나를 읽고 ID, status, assignee와 JSON body를 검증한다.
Active-card uniqueness, native link, clean tree와 leaf lineage는 PM이 native read-back으로 별도
확인한다. Frozen 이후에는 read-back body의 digest가 변하지 않았는지도 재검증한다. Helper는
Kanban을 생성·수정·연결하거나 상태를 바꾸지 않는다.