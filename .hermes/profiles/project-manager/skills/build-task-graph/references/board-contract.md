---
name: build-task-graph-board-contract
version: 6.0.0
---

# build-task-graph board contract v6

이 문서는 persisted graph의 schema와 machine-checkable invariant를 소유한다. Runtime 절차는
[`../SKILL.md`](../SKILL.md), Backend Impl body는
[`implementation-card-contract.md`](implementation-card-contract.md), checkpoint는
[`../../controll-task-graph/references/execution-contract.md`](../../controll-task-graph/references/execution-contract.md)가 소유한다. Native Kanban read-back이 task ID,
assignee, status, parent, run, comment와 event의 원본이다.

## Graph wrapper

Schema는 `build-task-graph-v1`이다.

| Field | 계약 |
|---|---|
| `mode` | `new | requirement-rework | review-rework` |
| `issue` | Issue number와 canonical URL |
| `generation` | 양의 정수 G |
| `requirement_basis` | path와 Git SHA |
| `revised_requirement` | `requirement-rework`에서 필수인 path와 Git SHA |
| `lineage` | Rework에서 필수인 prior generation, prior Summary, archived unfinished keys |
| `source_review` | `review-rework`에서 필수인 review key, finding별 disposition과 card별 idempotency map |
| `behaviors` | Complete effective behavior 집합 |
| `cards` | Native read-back을 정규화한 graph membership |
| `ready_candidate` | Activation 대상 Impl/Review key 또는 Decision-only rework의 `null` |
| `archived_card_keys` | Native archive 상태와 양방향으로 일치하는 key 목록 |

`requirement-rework`의 prior generation은 G-1이다. `review-rework`는 같은 G를 유지하고 source Review와
append된 card의 provenance를 보존한다. 각 disposition은 `finding_id`, 원래 verdict와
`appended_card_keys`를 가지며, idempotency map은 모든 appended key를 정확히 한 번 포함한다.
Verdict별 disposition은 각각 Impl(`correction-required`), Review(`context-required`),
Decision(`decision-required`)을 최소 하나 포함한다.

이 wrapper는 validator 입력이며 별도 workflow ledger가 아니다.

## Behavior contract

각 behavior는 stable `id`, 현재 전체 `outcome`, `state`, `disposition`, evidence와 선택적 Impl key를
가진다.

| state | disposition | 필수 계약 |
|---|---|---|
| `implemented` | `inherited` | 새 contract를 계속 충족한다고 재확인한 source/test evidence |
| `partial` / `absent` | `planned` | 실제 Implementation card를 가리키는 `implementation_card_key` |
| `unknown` | `blocked` | graph 외부 owner가 해결할 blocker |

Planned behavior의 ID/outcome은 참조 Impl body의 effective behavior와 일치한다. Behavior ID는 중복될 수
없다.

## Card identity와 body

Title은 다음 grammar를 따른다.

```text
G<N>-Issue<M>-Triage
G<N>-Issue<M>-Impl<P>
G<N>-Issue<M>-Review<Q>
G<N>-Issue<M>-Decision<R>
G<N>-Issue<M>-Summary
```

Issue는 positive number와 canonical HTTPS URL을 가지며 requirement revision은 40자리 Git SHA다.
Title의 generation, Issue, kind는 wrapper와 `card_type`에 일치한다. Card key와 title은 고유하고
assignee는 비어 있지 않다. Parent는 존재하는 다른 card를 가리키며 graph는 cycle이 없다.

Persisted body schema는 다음과 같다.

- Impl: `backend-implementation-card-v1`. 필드 의미와 validation은
  `implementation-card-contract.md`가 소유한다.
- Review: `aggregate-review-card-v1`. Complete behavior IDs, 참조 Impl keys, inherited behavior IDs와
  aggregate acceptance를 보존한다.
- Summary: `issue-summary-card-v1`. Complete behavior IDs, aggregate acceptance와 finalization checks를
  보존한다.
- Decision: `policy-decision-card-v1`. Source Review key, finding ID, 사용자 질문과
  `decision_owner: user`를 보존한다.

모든 Review의 behavior 집합은 wrapper behavior 집합과 정확히 같다. Latest Review의 Impl 참조는 graph의
모든 Impl key와, inherited behavior 참조는 모든 `implemented` behavior ID와 정확히 같다. Summary의
behavior 집합도 wrapper behavior 집합과 정확히 같다. Native task 값은 body에 복제하지 않는다.

## Topology

Native parent는 scheduling prerequisite다.

### `new`와 `requirement-rework`

- Triage는 graph 작성 중 `running`인 creator gate다.
- Planned behavior가 있으면 `ready_candidate`는 Implementation이며 Triage를 parent로 가진다.
- Planned behavior가 없으면 `ready_candidate`는 aggregate Review이며 Triage를 parent로 가진다.
- 각 Impl은 최소 하나의 Review와 Summary의 parent다.
- 모든 Review와 Decision은 Summary의 parent다.

### `review-rework`

- Triage, source Review와 기존 history는 `done`이다.
- Append된 첫 Impl 또는 context Review는 source Review를 parent로 가진다.
- Corrective Impl은 next Review와 Summary의 parent다.
- Append된 Review와 Decision은 Summary의 parent다.
- Decision-only rework는 `ready_candidate: null`이며 Decision status는 `blocked`다.

모든 mode에서 Summary는 하나이며 semantic Issue root다. Graph membership은 Summary body 목록이 아니라
native direct parent read-back으로 검증한다. Summary direct parents는 모든 Impl, Review와 Decision의
집합과 정확히 같다.

## Phase status invariant

### Draft

- `new`와 `requirement-rework`: Triage는 `running`; 다른 execution card는 `todo`, Decision은
  `todo | blocked`; `ready` card는 없다.
- `review-rework`: Triage와 pre-existing history는 `done`; Summary와 appended execution card는
  `todo`, Decision은 `todo | blocked`; `ready` card는 없다.

### Native

- `new`와 `requirement-rework`: Triage는 `done`; `ready_candidate` 하나만 `ready`; 나머지 execution
  card는 `todo`, Decision은 `todo | blocked`다.
- `review-rework`: Triage, source Review와 pre-existing history는 `done`; candidate가 있으면 그 card
  하나만 `ready`; appended 후속 card와 Summary는 `todo`다. Decision-only면 ready card가 없고
  Decision은 `blocked`다.

`archived_card_keys`와 native `archived` status는 양방향으로 일치한다. Done history는 archive하지
않는다. Requirement supersession으로 obsolete한 unfinished card만 archive할 수 있다.

## Review finding contract

Canonical finding은 completed Review의 latest run metadata `findings`에 있다. Comment는 canonical
source가 아니다. 각 finding은 source Review 범위에서 고유한 `finding_id`, `basis`, `observed_fact`,
`evidence`, `impact`와 다음 verdict를 가진다.

| verdict | graph 결과 |
|---|---|
| `correction-required` | 기존 effective behavior를 충족하는 corrective Impl |
| `context-required` | approved source를 반영한 next Review |
| `decision-required` | 같은 G의 blocked Decision |
| `resolved` | 후속 Review가 이전 finding의 해결을 확인 |

Unresolved finding은 disposition 없이 사라질 수 없다. `resolved` finding은 해결한 source Review ID와
source finding ID를 명시한다. 파생 card는 source Review/finding provenance와 idempotency key를 가지며
같은 provenance의 work는 하나만 존재한다. Summary는 모든 direct parent가 `done`이고 latest Review
finding이 이전 blocking finding을 명시적으로 resolved했으며 새 blocking finding이 없을 때만
promotion될 수 있다.

## Validation boundary

`scripts/build_task_graph.py validate-graph`는 wrapper schema, identity, lineage, behavior reference,
Review/Summary coverage, cycle, mode별 topology, phase status, archive invariant를 검증한다. Validator
통과는 semantic behavior 완료 증거가 아니다. Native persistence는 read-back으로, behavior 완료는 PM
checkpoint와 독립 Reviewer evidence로 증명한다.
