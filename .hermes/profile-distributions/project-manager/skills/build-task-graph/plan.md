# build-task-graph 구현 계획

## 목적

frozen `create-triage` body, 사용자 승인 requirement, GitHub Issue, freshness가 확인된
`current-state.md`를 Coder가
다른 문서를 다시 찾거나 기획하지 않고 구현할 수 있는 self-contained Kanban task graph로
투영한다. PM은 Issue로 delivery 범위와 lineage를 파악하고, requirement를 구현 계약의
정확한 기준으로 사용한다. `current-state.md`는 목표가 아니라 검증된 committed
implementation snapshot이다.

기존 `write-task`의 코드 전수 조사 중심 authoring recipe를 문서 우선 절차로 대체한다.
새 v4 board contract, deterministic draft tooling, validator, native graph creation surface는
이 Skill이 소유한다.

## v0.1 구현 범위

v0.1은 `new-delivery`만 실행한다. `requirement-rework`와 `review-rework`는 계획을
보존하되 지원되지 않는 mode로 명시적으로 `blocked` 처리한다.

v0.1 산출물은 model-invoked `SKILL.md`, agent-friendly JSON draft generator, draft
validator, fixture regression test, 그리고 `new-delivery` board contract다. native
atomic graph-create plugin은 선행 capability이며 이번 범위에서 구현하지 않는다. 따라서
plugin의 atomic action이 runtime schema에 없으면 PM은 검증된 draft를 남기고 graph mutation을
`blocked`로 보고한다. 기존 `kanban_create` 반복 호출, false `blocked` staging, artificial
graph-assembly gate로 우회하지 않는다.

manual verification은 body의 보류 section에 관찰 절차·기대 결과·보류 이유를 기록할 수 있다.
v0.1에서는 acceptance coverage를 충족하지 않으며, behavioral verification이 없는 acceptance는
graph 생성 blocker다.

## 사용할 때

- open leaf Issue의 최초 구현 graph를 만들 때
- frozen `create-triage` card가 있고 triage handoff가 graph authoring을 허용할 때

v0.1은 이 `new-delivery` branch만 실행한다. requirement 변경, Reviewer finding, graph
recovery는 후속 mode의 설계 입력으로 보존하되 실행하지 않는다.

## mode

| Mode | 시작 조건 | 생성 결과 |
|---|---|---|
| `new-delivery` | clean delivery branch, active workflow marker, fresh current-state, open leaf Issue | **v0.1 지원:** 확정된 모든 child와 `root-review-1`; 첫 child만 ready |
| `requirement-rework` | 사용자가 requirement 변경을 승인하고 requirement/Issue 동기화가 read-back됨 | v0.1 미지원; `blocked` |
| `review-rework` | Reviewer의 completed verdict와 structured finding이 read-back됨 | v0.1 미지원; `blocked` |

## 입력과 admission

다음은 graph mutation 전에 native 또는 Git read-back으로 확인한다.

- GitHub root-to-leaf Issue lineage와 선택한 leaf Issue 본문
- completed `create-triage` card의 frozen body와 native read-back
- 사용자 승인 requirement locator와 관련 requirement section
- fresh `current-state.md`의 관련 section
- 기존 Kanban card, dependency, completed review finding의 native read-back
- clean repository, selected planning/delivery branch, committed active workflow marker
- runtime에 주입된 native Kanban schema와 atomic graph-create action

triage가 없거나 frozen handoff의 `build_task_graph.allowed`가 `false`이면 graph를 만들지 않는다.
`current-state.md`가 `stale` 또는 `insufficient`이거나 active workflow/branch admission이
충족되지 않으면 graph를 만들지 않는다. `controll-task-graph` 또는
`update-current-state`로 정확한 blocker를 routing한다. native Kanban schema 또는 atomic
create action이 없으면 mutation은 `blocked`이며 plugin/runtime 개선으로 routing한다.

## requirement gap과 문서 동기화

requirement에 task contract에 필수인 정보가 없으면 PM은 이를 임의로 메우지 않는다.

1. 누락 사실, 선택지, 영향과 policy 변경 여부를 사용자에게 제시한다.
2. 사용자가 승인하면 requirement를 갱신한다. business policy 변경도 이 사용자 승인이
   있어야 한다.
3. 같은 논리적 planning transition에서 `sync-docs`의 `derived-docs` mode로 직접 영향을
   받는 파생 문서를 동기화하고 repository 문서를 commit한다.
4. `sync-docs`의 `issue-tracker` mode로 Issue scope/body/status/dependency를 동기화하고
   external state를 read-back한다.
5. clean tree, current-state freshness, requirement locator, Issue 본문을 다시 확인한 뒤
   같은 build run을 재개한다.

`current-state.md`는 구현 검증 뒤 `update-current-state`만 갱신한다. requirement 또는
일반 문서 변경만으로 implementation snapshot을 갱신하거나 stale로 판정하지 않는다.

## 제한적 source investigation

문서가 충분하면 source/test/migration을 전수 조사하지 않는다. 다음 경우에만 필요한
범위의 committed source locator를 조사한다.

- requirement, current-state, Issue가 서로 충돌한다.
- task contract에 필수인 정보가 누락되어 사용자 검토만으로 해소되지 않는다.
- Coder 또는 Reviewer가 문서와 구현의 불일치를 보고한다.
- module boundary, public contract, migration 등 명시적 structural constraint 확인이 필요하다.

조사 결과가 business policy, authorization, consistency, error semantics의 결정을 요구하면
`needs-input`으로 사용자에게 routing한다.

## Child implementation contract

각 child는 한 Coder owner와 하나의 독립적으로 검증 가능한 outcome을 갖는다. Coder는
card 본문만 실행 입력으로 사용한다. requirement locator는 PM traceability를 위한 근거이며,
Coder에게 문서를 다시 해석하라는 지시가 아니다.

v4 child schema는 다음 공통 영역을 요구한다.

- planning identity: Issue, requirement/current-state locator, planning baseline SHA, freshness,
  dependency와 assignee
- goal, scope, explicit out-of-scope, observable behavior
- actor/authorization, field semantics, domain state/invariant, error behavior
- acceptance criteria와 각 criterion에 연결된 executable `CHECK`/`EXPECT`
- source/test/migration/module-boundary locator가 예외 조사에 사용된 경우 그 locator

API contract, persistence/migration, UI flow, cross-module boundary 같은 조건부 영역은
적용되는 구체적 계약을 쓰거나 `not_applicable_reason` 중 하나를 반드시 가진다. validator는
field 존재와 형식을 검사하고, PM checklist와 사용자 requirement 검토는 의미적 완결성을
확인한다.

모든 조건부 implementation 영역은 `{applicable: true, details: [...]}` 또는
`{applicable: false, not_applicable_reason: "..."}` 형식을 사용한다. 이는
`actor_authorization`, `field_semantics`, `state_invariants`, `error_contract`,
`api_contract`, `persistence`, `ui_flow`, `module_boundary`에 공통 적용한다.

근거는 typed locator와 evidence role로 기록한다. 각 child에는 requirement를 뒷받침하는
`goal` evidence와 current-state를 뒷받침하는 `state` evidence가 최소 하나씩 필요하며,
architecture/ADR/module rule은 필요한 경우에만 `constraint` evidence로 추가한다.

## Root review contract

새 delivery는 확정된 모든 child와 Reviewer-assigned `root-review-1`을 함께 생성한다.
dependency는 첫 implementation child 하나만 ready가 되도록 구성한다. 추측성 downstream
work는 만들지 않는다.

v4 root schema는 다음을 요구한다.

- draft의 대상 child `card_key` 집합과 persisted body의 actual child task ID 집합
- immutable aggregate contract, cross-boundary/API invariant, aggregate exclusion
- child evidence read-back 요구
- PM-authored review question
- `approved | changes-requested | needs-input` verdict protocol
- 후속 root의 rework/finding provenance

Reviewer의 상세 rubric, requirement/current-state 직접 열람 여부, PM question 밖 finding 허용
범위는 Reviewer Profile 설계가 소유한다.

## Rework와 immutable history

- completed root review의 `changes-requested` finding은 `review-rework` mode의 새 Coder child로
  변환한다. child checkpoint 뒤 `root-review-{n+1}`을 생성하며, 이전 root, finding, 완료 child는
  수정하지 않는다.
- user-approved policy/requirement 변경으로 미완료 child가 obsolete가 되면 해당 child와 아직
  실행되지 않은 root review를 archive한다. archive는 body, run, comment, ID를 바꾸지 않는
  historical replacement이다.
- 예를 들어 A만 변경되면 `rework-A`와 `rework-root-review-1`을 만들고, 영향 없는 B/C/D는
  replacement root의 dependency로 재사용한다.
- 미완료 root replacement는 `rework-root-review-{n}`으로, completed review verdict 뒤 finding
  rework의 다음 root는 `root-review-{n+1}`으로 이름 짓는다.

## Draft, validation, native creation

`scripts/`의 deterministic draft generator는 child/root task body draft에 필요한 정보를
빠뜨리지 않도록 JSON draft를 생성한다. v0.1 validator는 이 draft의 schema, typed locator,
acceptance coverage, conditional section, assignee, dependency topology를 결정적으로 검사한다.
이는 semantic requirement 충족이나 native lifecycle의 판정이 아니다.

PM은 draft를 근거로 v4 task body를 작성한다. native plugin의 build-task-graph 전용 atomic
graph-create action은 전체 candidate body와 topology를 v4 validator로 검사하고, 통과한
graph만 한 번에 persist한다. atomic action은 single ready implementation child만 노출하며,
실패한 candidate card를 dispatcher가 claim할 수 있는 중간 상태를 만들지 않는다.

draft는 stable `card_key`로 dependency와 root target child를 가리킨다. atomic plugin은
persist 직전에 이를 actual native task ID로 치환하며, persisted root body에는 actual child
task ID만 기록한다. v0.1 draft validator는 unique `card_key`, acyclic dependency, 정확히
하나의 root, root의 모든 child dependency, 첫 eligible implementation child 하나,
assignee, evidence, acceptance coverage, 조건부 영역을 hard-fail한다.

생성 뒤에는 native envelope, actual task ID, dependency, assignee, ready state를 read-back하고
post validator로 persisted graph를 확인한다. validator는 task-body/graph 계약 검사기이며,
Coder test, PM checkpoint, CI, requirement의 의미적 충족을 대신 판정하지 않는다.

## 구현 산출물과 migration

- `skills/build-task-graph/SKILL.md`: 이 plan의 ordered authoring procedure와 Korean report contract
- `skills/build-task-graph/references/board-contract.md`: v4 persisted body schema, graph invariant,
  finding schema, validator rule의 유일한 source of truth
- `skills/build-task-graph/scripts/`: draft generator와 v4 draft validator
- `skills/build-task-graph/tests/`: fixture-based regression tests
- Kanban plugin: atomic build-task-graph native creation/read-back surface

v3의 planning SHA/freshness, actual task-ID dependency, typed locator, `CHECK`/`EXPECT`, review
handoff, secret-redaction 장치를 v4에 이식한다. v4 validator와 regression test가 통과한 뒤
`write-task`, v3 board contract, 기존 validator를 한 변경에서 삭제한다.

## 완료 기준

v0.1 완료에서는 다음이 모두 regression result로 증명되어야 한다.

- 모든 실행 card에는 owner, scope/out-of-scope, self-contained implementation contract,
  acceptance, verification, dependency가 있다.
- 모든 root card에는 complete aggregate contract가 있다. rework provenance는 후속 mode에서
  도입한다.
- draft graph는 첫 eligible implementation child가 하나만 되도록 구성된다.
- v4 validator와 fixture regression test가 valid/invalid child, root, requirement-rework,
  review-rework topology를 판정한다. v0.1에서는 valid/invalid `new-delivery` fixture만
  필수이며 rework fixture는 후속 mode 구현에서 추가한다.
- atomic plugin이 없을 때 Skill은 native graph를 만들지 않고 required capability와 blocker를
  정확히 보고한다.

atomic plugin이 구현된 뒤에는 native creation과 post read-back이 persisted body, actual IDs,
dependency, assignee, ready state를 추가로 증명해야 한다.