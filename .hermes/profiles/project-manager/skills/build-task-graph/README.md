# build-task-graph

`build-task-graph`는 승인된 Issue contract를 수동 native Kanban graph로 바꾸는 Project
Manager Skill입니다. PM은 graph를 작성하고 하나의 다음 Impl만 `ready`로 promotion합니다.
Coder는 자신의 self-contained card에 기록된 현재 behavior를 구현합니다.

## 사용 조건

- 선택된 leaf Issue, 승인 requirement, fresh `current-state.md`, clean planning baseline이 있다.
- `create-triage`는 완료되어 planning provenance를 제공한다.
- `requirement-rework`이면 active graph의 aggregate Review가 아직 승인되지 않았고, 새
  requirement revision과 Issue/derived-document read-back이 있다.

## Graph

```text
G<N>-Issue<M>-Triage
Triage → first eligible Impl → Review<Q> → Summary
Review<Q> → corrective Impl → Review<Q+1> → Summary
```

Triage는 graph 작성 중 PM creator-session이 소유하는 `running` planning gate이며 first eligible
Impl의 native parent입니다. parent 없는 task는 생성 즉시 `ready`가 되므로 이 gate를 완료하기 전에는
실행 card가 dispatchable하지 않습니다. `G<N>-Issue<M>-Summary`는 semantic root이며 immutable finalization contract를 갖는다. task
membership은 body 목록이 아니라 native direct parent read-back으로 확인한다. native parent link는
prerequisite만 표현한다. 모든 신규 card는 `todo`로
생성·read-back하고, 전체 graph가 검증된 뒤 첫 eligible Impl 하나만 `ready`로 만든다.

## Mode

| Mode | 결과 |
|---|---|
| `new` | 최초 G1 root, Impl, Review graph |
| `requirement-rework` | 승인 requirement revision에 따른 G{N+1} superseding graph |
| `review-rework` | done Review의 finding verdict에 따른 same-G corrective/context/decision append |

rework root는 delta가 아니라 A′ 같은 effective behavior model을 기록합니다. PM은 현재
committed code가 A′ 전체를 `implemented`, `partial`, `absent`, `unknown` 중 어디까지
충족하는지 조사합니다. `partial`/`absent`만 새 Impl이 되며, Coder card의 goal은 AA 같은
변경 조각이 아니라 A′ 전체입니다.

Review finding은 requirement delta가 아니다. corrective Impl은 A + AA = A′를 만들지 않고 기존 A를
충족하도록 AA가 보인 구현 결함을 고친다. `decision-required` finding은 PM이 같은 G의 blocked
Decision card로 routing하며, 사용자 결정이 완료 계약을 무효화할 때만 G{N+1} requirement-rework가 된다.

`done` task는 immutable history입니다. requirement가 supersede한 running task는 사실대로
block하고 worker 종료를 read-back한 뒤 archive합니다. unfinished Impl/Review/root만 archive할
수 있습니다. aggregate Review 승인 후 requirement 변경은 새 Issue의 `new` graph로 처리합니다.

## 근거와 검증

Codebase Memory와 Semble은 source/document 후보 탐색에 사용합니다. PM은 committed
source/test/migration을 직접 read-back해 state evidence를 기록합니다. 검색 결과만으로
implemented/unaffected verdict를 확정하지 않습니다.

정책, authorization, consistency, error semantics, ownership, external prerequisite를
조사만으로 결정할 수 없을 때만 `blocked`와 사용자 결정을 사용합니다. 단순한 source 위치 또는
구현 범위 확인은 normal investigation입니다.

정확한 persisted body, draft shape, validator invariant는
[`references/board-contract.md`](references/board-contract.md)를 따릅니다. 실행 순서는
[`SKILL.md`](SKILL.md), tool rollout은 [`plan.md`](plan.md)를 따릅니다.

Backend Impl body의 authoring semantics와 validation은
[`references/implementation-card-contract.md`](references/implementation-card-contract.md)가 소유합니다.
Coder handoff와 PM checkpoint는 각 역할의 execution contract가 소유합니다.

## Backend Impl helper

`scripts/build_task_graph.py`는 canonical backend Impl body의 scaffold와 결정론적 validator를
제공합니다. 출력은 repository의 `.temp` 아래에만 만들 수 있습니다.

```text
python scripts/build_task_graph.py template --issue <number> --issue-url <url> --output .temp/task-graphs/<issue>/impl-1.json
python scripts/build_task_graph.py validate .temp/task-graphs/<issue>/impl-1.json
```

이 helper는 native task ID, assignee, status, link 또는 graph promotion을 생성·검증하지 않습니다.
그 값은 native Kanban create/link/promotion 후 별도로 read-back합니다.
