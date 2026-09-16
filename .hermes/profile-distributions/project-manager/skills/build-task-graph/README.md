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
G<N>-Issue<M>-Impl<P> ─┐
G<N>-Issue<M>-Impl<P> ─┼→ G<N>-Issue<M>-Review<Q> → G<N>-Issue<M>
G<N>-Issue<M>-Impl<P> ─┘
```

`G<N>-Issue<M>`은 semantic root이며 effective behavior와 inherited evidence를 갖는 final
aggregate child다. native parent link는 prerequisite만 표현한다. 모든 신규 card는 `todo`로
생성·read-back하고, 전체 graph가 검증된 뒤 첫 eligible Impl 하나만 `ready`로 만든다.

## Mode

| Mode | 결과 |
|---|---|
| `new` | 최초 G1 root, Impl, Review graph |
| `requirement-rework` | 승인 requirement revision에 따른 G{N+1} superseding graph |
| `review-rework` | Reviewer finding schema 확정 전 미지원 |

rework root는 delta가 아니라 A′ 같은 effective behavior model을 기록합니다. PM은 현재
committed code가 A′ 전체를 `implemented`, `partial`, `absent`, `unknown` 중 어디까지
충족하는지 조사합니다. `partial`/`absent`만 새 Impl이 되며, Coder card의 goal은 AA 같은
변경 조각이 아니라 A′ 전체입니다.

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
