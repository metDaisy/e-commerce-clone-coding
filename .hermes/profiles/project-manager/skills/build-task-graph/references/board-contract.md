---
name: build-task-graph-board-contract
version: 5.0.0-draft
---

# build-task-graph board contract v5

이 문서는 **현재 합의된** `build-task-graph` 계약만 소유한다. JSON field·helper CLI·native
mutation response처럼 아직 결정되지 않은 상세는 이 문서에 추측으로 추가하지 않고
[`../plan.md`](../plan.md)의 `아직 결정하지 않은 사항`에서 결정한다.

Backend Impl card body, Coder handoff, PM checkpoint field schema는 project root의 Coder contract가
소유한다.

- `.hermes/profiles/coder/skills/run-impl-card/references/execution-contract.md`
- `.hermes/profiles/coder/skills/run-impl-card/references/implementation-card-contract.md`

PM은 Impl card를 작성하기 전에 두 contract를 읽는다. 이 board contract는 graph identity,
generation, topology와 promotion만 소유하며 worker contract schema를 복제하지 않는다.

Native Kanban은 status, task ID, assignee, parent link, run, comment, event를 소유한다. PM은
behavior contract, generation lineage, evidence, task promotion 결정을 소유한다.

## Mode

| Mode | 시작 조건 | 결과 |
|---|---|---|
| `new` | 승인된 leaf Issue와 planning admission | G1 graph |
| `requirement-rework` | aggregate Review 승인 전, 승인된 requirement revision | G{N+1} superseding graph |
| `review-rework` | 완료된 aggregate Review의 structured finding | 같은 G에 corrective work를 append |

aggregate Review가 승인된 뒤 requirement가 바뀌면 기존 graph를 바꾸지 않는다. 새 Issue와
`new` graph를 만든다.

## Card identity와 native topology

```text
G<N>-Issue<M>-Triage ─→ first eligible G<N>-Issue<M>-Impl<P>
G<N>-Issue<M>-Impl<P> ─→ G<N>-Issue<M>-Review<Q> ─→ G<N>-Issue<M>-Summary
G<N>-Issue<M>-Decision<R> ─────────────────────────→ G<N>-Issue<M>-Summary
```

- `G<N>-Issue<M>-Triage`: freeze된 PM planning provenance다.
- `G<N>-Issue<M>-Summary`: semantic Issue root. immutable aggregate acceptance와 finalization
  contract를 보존하며 PM finalization 뒤 `done`이 된다. graph membership은 body 목록이 아니라
  native direct parent read-back으로 계산한다.
- `G<N>-Issue<M>-Impl<P>`: Coder가 수행하는 하나의 self-contained implementation contract.
- `G<N>-Issue<M>-Review<Q>`: Impl과 inherited behavior를 aggregate review하는 Reviewer contract.
- `G<N>-Issue<M>-Decision<R>`: finding이 요구한 사용자 정책 결정을 보존하는 PM card다.
- `G`는 승인 requirement revision마다 증가한다. `Impl`과 `Review`의 numbering 상세 규칙은
  generation 내부의 monotonic identity를 유지해야 한다.

native parent link는 scheduling prerequisite다. Triage는 PM creator-session이 graph를 작성하는 동안
`running`인 planning gate이며 first eligible Impl의 parent다. initial Impl은 Review1과 Summary의 parent다.
Review finding으로 만든 Impl은 source Review의 child이며 다음 Review와 Summary의 parent다.
모든 Review와 Decision은 Summary의 parent다. semantic containment를 표현하려고 이 방향을
역전하지 않는다.

## Manual graph construction

1. PM은 built-in decomposer를 사용하지 않는다. Triage를 PM-owned `running` creator-session
   gate로 유지한다. parent 없는 일반 task는 생성 즉시 `ready`가 되므로 first eligible Impl도
   반드시 Triage를 parent로 가진다.
2. 신규 Impl과 Review를 하나씩 parent와 함께 `todo`로 만들고, 각각의 native
   body/ID/assignee/status를 read-back한다.
3. parent link를 만든 뒤 native read-back으로 topology를 확인한다.
4. replacement graph가 완성될 때까지 신규 card를 `ready`로 만들지 않는다.
5. graph가 검증된 뒤 PM은 Triage를 완료한다. native dependency promotion read-back에서 first
   eligible Impl 하나만 `ready`이고 나머지 execution card는 `todo`여야 한다.

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

## Review-rework

Reviewer는 immutable Review body를 읽고 `kanban_complete` run metadata의 `findings`에만
blocking 결과를 기록한다. PM은 terminal event로 재개된 뒤 해당 task/run/metadata를 native
read-back한다. comment는 보조 설명이며 PM routing의 canonical input이 아니다.

각 finding은 native Review ID 범위의 `finding_id`, `basis`, `observed_fact`, `evidence`,
`impact`와 다음 verdict 하나를 가진다.

| finding verdict | PM route |
|---|---|
| `correction-required` | 기존 effective behavior를 충족시키는 corrective Impl 생성 |
| `context-required` | approved source를 read-back해 context를 보강한 다음 Review 생성 |
| `decision-required` | 같은 G의 Decision card를 native `blocked`로 생성 |
| `resolved` | 후속 Review가 이전 finding 해결을 확인 |

`needs-input`은 native status가 아니다. Decision card의 semantic reason이며 native status는
`blocked`다. PM은 policy를 결정하지 않는다. 사용자 결정이 완료된 contract/AC/observable
behavior를 무효화하면 `requirement-rework`로 G{N+1}을 만들고, 그렇지 않은 clarification은 같은
G에서 Impl/Review를 append한다.

Review<Q+1> body는 모든 blocking finding의 후속 contract가 확정된 뒤에만 만든다. 이전
unresolved finding은 새 disposition 없이 사라질 수 없다. Summary promotion은 모든 direct
parent가 `done`이고 latest Review의 재검사 finding이 모두 `resolved`이며 새 blocking finding이
없을 때만 가능하다.

PM은 Review의 native child와 Summary direct parent를 read-back해 idempotently recover한다.
파생 card는 source Review/finding provenance와 native idempotency key를 갖고, 이미 존재하는
work를 재생성하지 않는다.

## Blocker와 checkpoint

구현 위치 또는 충족 범위를 확인하기 위한 source investigation은 normal planning work다.
요구사항 policy, ownership, authorization, consistency, error semantics, external prerequisite를
조사만으로 결정할 수 없을 때만 native `blocked`와 사용자 결정을 사용한다.

Coder의 `running → review` handoff 뒤 PM checkpoint는 verification evidence, changed paths와
commit boundary를 확인하고 commit한다. PM은 result commit SHA, committed paths와 clean worktree를
read-back한다. 이 checkpoint는 independent aggregate Review를 대체하지 않는다.

## Evidence boundary

- Backend Coder card는 `backend-implementation-card-v1`을 사용한다. Goal, scope, explicit
  out-of-scope, current effective behavior, confirmed implementation context, applicable contract
  dimensions, acceptance criteria, focused verification, full backend verification과 traceability를
  self-contained하게 가진다.
- Requirement/source/task locator는 PM traceability evidence다. Coder에게 다른 문서를 다시
  해석하라고 지시하지 않는다.
- Baseline/planning SHA, assignee, status, dependency, workspace와 실행 결과는 Impl body에
  저장하지 않는다. PM admission과 native Kanban이 이 값을 소유한다.
- 모든 acceptance criterion은 하나 이상의 focused verification에 연결한다. PM은 behavior,
  test level과 required scenario를 정의하고, Coder는 실제 source를 조사한 뒤 정확한 test FQCN을
  선택하거나 작성한다.
- Full backend verification은 `gradle-mcp` backend `test` task다. Frontend/npm/browser 검증은
  이 contract 범위가 아니다.
- Credential, raw tool output, prompt, hidden reasoning은 card나 draft에 저장하지 않는다.
- Deterministic validator는 draft syntax/topology만 확인한다. Native read-back은 persisted
  status/link/ID를 확인한다. Behavior completion은 PM checkpoint와 Review evidence로 확인한다.

Coder는 focused와 full backend verification이 모두 통과한 뒤 native same-card review를 요청한다.
PM은 `review` 상태에서 commit하고 result SHA, committed paths와 clean worktree를 read-back한 뒤에만
card를 완료한다. Canonical field shape는 위 Coder contract 한 곳에서만 관리한다.
