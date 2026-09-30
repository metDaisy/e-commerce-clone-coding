# create-triage JSON 계약 v2

Kanban task의 `body`는 UTF-8 JSON object 하나다. Native Kanban은 task identity, status,
assignee, link, event, comment와 run을 소유하고, 이 body는 PM planning data를 소유한다. Body에
`task_id`나 native lifecycle 상태를 복제하지 않는다.

## 필수 field

```json
{
  "schema": "triage-v2",
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
  "cross_domain_contracts": [],
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

## Cross-domain public contract

`cross_domain_contracts`는 이번 Issue가 소비하는 외부 capability마다 한 항목을 가진다. 단순 port 이름,
adapter 클래스 또는 producer 내부 entity/repository는 public contract 증거가 아니다. 각 항목은
`consumer_module`, `producer_module`, `relationship: consumer | producer`, `capability`, non-empty `evidence`와
다음 `status` 중 하나를 가진다. 상세 판단·예시는
[`cross-domain-minimum-capability.md`](cross-domain-minimum-capability.md)를 따른다.

- `published`: `public_contract`, `producer_evidence`, 완전한 `minimum_capability`를 기록한다. `producer_evidence`는
  committed producer public surface와 실제 producer 경로 locator다. 이 경우 policy finding, decision request,
  follow-up은 없다.
- `absent-or-partial`: public seam의 현재 사실이 없다거나 부분 구현임을 뜻한다. open finding과 pending/deferred
  decision이면 Graph gate는 닫힌다. 승인된 최소 contract가 있으면 finding은 `resolved`, decision은 `approved`이며,
  `minimum_capability`와 producer·consumer·integration verification `follow_up`을 기록한다. 이 상태는 목표가
  승인됐어도 실제 seam은 아직 없다는 뜻이므로 `published`로 바꾸지 않는다.
- `not-applicable`: 구체적인 `not_applicable_reason`을 기록한다.

`triage-v1`은 이미 완료된 historical card read-back을 위한 legacy schema다. 새 Triage는 반드시
`triage-v2` template으로 만들며, external capability를 `application.port.out`에 두었다는 이유만으로
published로 판정하지 않는다.

## Graph gate 검증

`build_task_graph.allowed: true`는 다음을 모두 만족할 때만 유효하다.

- `planning_state: frozen`
- Canonical body와 일치하는 `frozen_digest`
- `blocker: null`
- open policy finding과 pending/deferred decision request가 없음
- `document_impact`에 `blocked`가 없음
- 미해결 Decision이 없는 `absent-or-partial` 항목은 approved minimum contract와 complete follow-up을 가진다.
  실제 producer seam은 follow-up 완료·검증 뒤에만 `published`가 된다.

## Native read-back 검증

실제 native ID는 body 밖의 provenance로 사용한다. `triage.py validate-card`는 공식 read-only
`hermes kanban ... show --json`으로 card 하나를 읽고 ID, status, assignee와 JSON body를 검증한다.
Active-card uniqueness, native link, clean tree와 leaf lineage는 PM이 native read-back으로 별도
확인한다. Frozen 이후에는 read-back body의 digest가 변하지 않았는지도 재검증한다. Helper는
Kanban을 생성·수정·연결하거나 상태를 바꾸지 않는다.