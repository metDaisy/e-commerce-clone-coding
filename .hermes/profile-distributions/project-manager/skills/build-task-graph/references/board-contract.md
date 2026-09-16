---
name: build-task-graph-board-contract
version: 5.0.0-draft
---

# build-task-graph board contract v5

이 문서는 **현재 합의된** `build-task-graph` 계약만 소유한다. JSON field·helper CLI·native
mutation response처럼 아직 결정되지 않은 상세는 이 문서에 추측으로 추가하지 않고
[`../plan.md`](../plan.md)의 `아직 결정하지 않은 사항`에서 결정한다.

Native Kanban은 status, task ID, assignee, parent link, run, comment, event를 소유한다. PM은
behavior contract, generation lineage, evidence, task promotion 결정을 소유한다.

## Mode

| Mode | 시작 조건 | 결과 |
|---|---|---|
| `new` | 승인된 leaf Issue와 planning admission | G1 graph |
| `requirement-rework` | aggregate Review 승인 전, 승인된 requirement revision | G{N+1} superseding graph |
| `review-rework` | Reviewer finding | finding contract 미결정으로 아직 지원하지 않음 |

aggregate Review가 승인된 뒤 requirement가 바뀌면 기존 graph를 바꾸지 않는다. 새 Issue와
`new` graph를 만든다.

## Card identity와 native topology

```text
G<N>-Issue<M>-Impl<P> ─┐
G<N>-Issue<M>-Impl<P> ─┼→ G<N>-Issue<M>-Review<Q> → G<N>-Issue<M>
G<N>-Issue<M>-Impl<P> ─┘
```

- `G<N>-Issue<M>`: semantic Issue root. active Issue implementation data와 effective behavior를
  보존하고 PM finalization 뒤 `done`이 된다.
- `G<N>-Issue<M>-Impl<P>`: Coder가 수행하는 하나의 self-contained implementation contract.
- `G<N>-Issue<M>-Review<Q>`: Impl과 inherited behavior를 aggregate review하는 Reviewer contract.
- `G`는 승인 requirement revision마다 증가한다. `Impl`과 `Review`의 numbering 상세 규칙은
  generation 내부의 monotonic identity를 유지해야 한다.

native parent link는 scheduling prerequisite다. 따라서 모든 Impl은 Review의 native parent이고,
Review는 Issue root의 native parent다. semantic containment를 표현하려고 이 방향을 역전하지
않는다.

## Manual graph construction

1. PM은 built-in decomposer를 사용하지 않는다.
2. 신규 Impl과 Review를 하나씩 `todo`로 만들고, 각각의 native body/ID/assignee/status를
   read-back한다.
3. parent link를 만든 뒤 native read-back으로 topology를 확인한다.
4. replacement graph가 완성될 때까지 신규 card를 `ready`로 만들지 않는다.
5. graph가 검증된 뒤 PM은 첫 eligible Impl 하나만 `ready`로 promotion한다.

`ready → running`은 dispatcher의 claim/spawn lifecycle이다. model 선택은 assignment/dispatch
설정일 수 있지만 전이 자체의 대체가 아니다.

## Requirement-rework

### Admission

active graph의 aggregate Review가 아직 승인되지 않았고, user-approved revised requirement,
관련 Issue/derived-document read-back, active graph의 `requirement_basis_sha`가 있어야 한다.
기준 requirement revision과 revised revision의 deterministic Git diff를 만든다. diff hunk는
behavior delta를 뒷받침하는 evidence이며 Coder task boundary가 아니다.

### Effective behavior

PM은 새 root에 requirement의 complete effective behavior를 작성한다. 예를 들어 AA가 A를
변경하면 새 behavior는 `A′ = A + AA`다. Coder에게 A/AA history를 읽게 하지 않으며, 새 Impl의
contract는 A′ 전체를 구현하도록 쓴다.

PM은 각 effective behavior를 current committed code와 비교해 다음 중 하나로 판정한다.

| Implementation state | 조치 |
|---|---|
| `implemented` | new requirement를 계속 충족함을 re-confirm한 done evidence로 inherited 처리 |
| `partial` | A′ 전체를 목표로 하는 새 Impl 생성 |
| `absent` | A′ 전체를 목표로 하는 새 Impl 생성 |
| `unknown` | 제한적 investigation을 계속하거나 실제 policy blocker로 route |

Codebase Memory와 Semble은 후보 locator를 찾는 데 사용한다. 결과만으로 판정하지 않고 PM이
committed source/test/migration을 직접 read-back해 state evidence를 쓴다.

### Supersession history

- `done` card의 body, ID, run, comment, checkpoint evidence는 immutable이다.
- affected running card는 requirement superseded라는 실제 사유로 block하고 worker 종료를
  read-back한 뒤 archive한다.
- unfinished obsolete Impl, Review, root만 archive한다.
- 새 root에는 prior root, requirement revision, effective behavior, inherited done evidence,
  archived unfinished work의 lineage를 기록한다.
- `current-state.md`는 requirement rework 중간에 갱신하지 않는다.

## Blocker와 checkpoint

구현 위치 또는 충족 범위를 확인하기 위한 source investigation은 normal planning work다.
요구사항 policy, ownership, authorization, consistency, error semantics, external prerequisite를
조사만으로 결정할 수 없을 때만 native `blocked`와 사용자 결정을 사용한다.

Coder의 `running → review` handoff 뒤 PM checkpoint는 verification evidence, changed paths,
commit boundary, verified SHA, clean worktree를 read-back한다. 이 checkpoint는 independent
aggregate Review를 대체하지 않는다.

## Evidence boundary

- Coder card는 goal, scope, explicit out-of-scope, current effective behavior, current state
  evidence, applicable contract details, acceptance criteria, verification을 self-contained하게
  가진다.
- requirement/source/task locator는 PM traceability evidence다. Coder에게 다른 문서를 다시
  해석하라고 지시하지 않는다.
- credential, raw tool output, prompt, hidden reasoning은 card나 draft에 저장하지 않는다.
- deterministic validator는 draft syntax/topology만 확인한다. native read-back은 persisted
  status/link/ID를 확인한다. behavior completion은 PM checkpoint와 Review evidence로 확인한다.

구체적인 persisted JSON schema와 validator field rules는 plan의 미결 항목이 확정된 뒤에만 이
문서에 추가한다.
