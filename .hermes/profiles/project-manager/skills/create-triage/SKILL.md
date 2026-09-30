---
name: create-triage
description: 새 leaf Issue의 실행 graph 전 계획을 검증한다.
version: 0.2.1
author: Amaazon project, Hermes Agent
license: MIT
platforms: [windows, linux, macos]
metadata:
  hermes:
    tags: [project-management, triage, kanban, requirements]
    related_skills: [service-planning, sync-docs, build-task-graph]
requires_toolsets: [kanban]
---

# Issue Triage 작성

실행 가능한 Coder graph를 만들기 전에 leaf Issue마다 PM 소유 Triage card 하나를 작성한다. 이
card는 구현 아이디어, 문서 영향, 정책 질문과 다음 단계의 근거를 보존하는 계획 기록이다. Coder
실행 계약은 `build-task-graph`가 별도로 작성한다.

## 사용 조건

- 새 leaf Issue의 G1 graph를 처음 계획할 때 사용한다.
- 같은 Issue에 active Triage가 있으면 새 card를 만들지 않고 재개한다.
- G{N+1} requirement-rework Triage는 `build-task-graph`가 작성한다.
- Source, test, migration 구현이나 business policy 결정에는 사용하지 않는다.

## 사전 조건

1. 저장소 `AGENTS.md`와 이 Profile의 workflow 문서를 읽는다.
2. 선택한 leaf Issue와 ancestor lineage, clean repository, planning HEAD를 read-back한다.
3. 승인 requirement와 관련 reference를 읽는다.
4. `current-state.md`가 `fresh`인지 확인한다. `stale` 또는 `insufficient`이면 card를 만들지 않고
   `snapshot-refresh-required` blocker를 root `run-workflow`에 반환한다.
5. Native Kanban의 create, show, claim, block, complete surface가 사용 가능한지 확인한다.

완료 기준: Issue, requirement, planning SHA, current-state snapshot과 사용할 Kanban surface가 모두
확정되었거나, mutation 없이 blocker가 올바른 Skill로 전달되었다.

## 연관 Skill 사용 가이드

각 Skill은 아래 조건에서만 읽고, 그 완료 기준을 충족한 뒤 같은 Triage를 재개한다.

| Skill | 사용하는 경우 | 사용하는 방법과 복귀 조건 |
|---|---|---|
| `service-planning` | Policy, authorization, consistency, 오류 의미 또는 UI 의미를 source만으로 결정할 수 없다. | Triage는 native `triage`로 유지하고 linked Planning Decision을 만든다. 사용자의 결정과 requirement 반영을 read-back한 뒤 재개한다. |
| `sync-docs` | 승인 requirement와 파생 문서 또는 GitHub Issue가 불일치한다. | `current-state.md`를 제외한 영향 대상을 동기화하고 외부 mutation을 read-back한다. 모든 불일치가 해소되면 재검증한다. |
| `build-task-graph` | Triage가 frozen이고 graph gate가 열렸다. | Frozen body, 승인 requirement, fresh snapshot과 Issue를 넘긴다. Graph 전체 검증 뒤 이 Skill이 Triage를 완료하고 첫 실행 대상 하나만 `ready`로 만든다. |

세 Skill은 모두 실제 workflow handoff다. Source 위치 조사나 일반 구현에는 대신 사용하지 않는다.

## Kanban lifecycle

```text
triage planning → frozen triage graph gate → done
       └→ Planning Decision blocked → user comment + running → PM reply → approved → done
```

- Triage는 planning 내내 native `triage`다. PM은 Triage를 claim하거나 block하지 않는다.
- 사용자 입력은 조건부 Planning Decision의 `blocked → running`과 comment로 표현한다. 그 card만
  `needs-input` reason을 보존하며 Triage에는 open finding/decision request와 graph gate false를 기록한다.
- Frozen Triage는 graph 작성 중 첫 실행 대상의 scheduling parent다. 전체 graph read-back과 activation
  preflight 전에는 `done`으로 바꾸지 않는다.

## 정본 body

Body는 [`references/triage-contract.md`](references/triage-contract.md)의 UTF-8 JSON 한 개다.
`scripts/triage.py template`은 PM이 이미 읽은 literal 입력만 받아 repository의
`.temp/triage/issue-<number>/` 아래에 draft를 만든다.

필수 내용:

- Issue identity, planning baseline SHA와 fresh current-state snapshot
- goal, scope, explicit out-of-scope, current/desired behavior
- implementation idea, candidate vertical slices/dependencies, verification direction
- requirement, architecture, ADR, glossary, ERD, index의 disposition과 locator
- 소비 module, producer module, capability와 실제 public surface를 구분한 `cross_domain_contracts`
- 구조화된 policy finding, decision request, blocker
- `planning_state`와 `build_task_graph` handoff

문서 disposition은 `update | no-change | not-applicable | blocked` 중 하나다. `update`,
`no-change`, `blocked`에는 근거 locator가 필요하다. `current-state`는 별도 `read-only` 항목이다.

## 검증 Helper

`scripts/triage.py`는 결정론적 draft/validator이며 Kanban controller가 아니다.

```text
TRIAGE_PY=.hermes/profiles/project-manager/skills/create-triage/scripts/triage.py
python "$TRIAGE_PY" template --issue <number> --title "G1-Issue<number>-Triage" --issue-url <url> --planning-sha <sha> --current-state-sha <sha> --requirement-locator <path#locator> --current-state-locator <path#locator> --output .temp/triage/issue-<number>/draft.json
python "$TRIAGE_PY" validate .temp/triage/issue-<number>/draft.json --status <status>
python "$TRIAGE_PY" freeze .temp/triage/issue-<number>/draft.json --output .temp/triage/issue-<number>/frozen.json
python "$TRIAGE_PY" validate-card --task-id <actual-id> --board <board> --expected-status <status> --expected-assignee project-manager
```

- `template`: 제공된 사실만 보존하는 draft를 만든다.
- `validate`: schema, locator, 정책 결정 구조, freeze와 graph gate 일관성을 검사한다.
- `freeze`: 해결된 planning body를 frozen copy로 만들고 canonical SHA-256 digest와 열린 graph gate를
  기록한다. 이후 body가 바뀌면 validation이 실패한다.
- `validate-card`: 공식 `hermes kanban ... show --json`으로 card 하나를 읽어 native envelope와 body를
  검사한다.
Helper는 Kanban을 생성·수정·연결·claim·block·complete하지 않는다. 일반 graph activation은
`references/triage-decision-collaboration.md`의 PM-only `activate_graph.py`만 사용하며, 그 외 mutation은
native `kanban_*` surface와 exact read-back으로 수행한다.

## 절차

1. **기준을 고정한다.** 사전 조건을 read-back하고 locator를 기록한다. 완료 기준: 모든 입력이
   하나의 planning HEAD에 연결된다.
2. **Draft를 작성한다.** Issue-specific template을 채우고 모든 문서에 disposition과 근거를
   기록한다. 완료 기준: `validate ... --status triage`가 오류 없이 끝난다.
3. **재사용하거나 생성한다.** Active Triage를 찾아 정확히 하나를 재사용한다. 없으면
   `G1-Issue<number>-Triage`, assignee `project-manager`, validated JSON body와 exact `--workspace` 인자로
   생성한다. 완료 기준: 실제 ID, status, assignee와 body read-back 결과가 일치하고 중복 active card가
   없다. Native show가 workspace를 반환한다고 주장하지 않고 create intent와 actual process cwd를
   별도로 확인한다.
4. **조사한다.** Triage를 native `triage`로 유지한 채 requirement, Issue, snapshot과
   후보 문서를 대조한다. Source 조사는 충돌, 구조 제약 또는 snapshot 불일치가 있을 때만 좁게
   수행한다. 소비하는 cross-domain capability는 producer의 named public interface/query/event와 실제
   error·consistency 의미를 확인한다. consumer-owned port, 항상 empty/default를 반환하는 adapter 또는
   producer 내부 구현은 published contract가 아니다. producer 전체 구현을 선행 조건으로 만들지 말고,
   consumer acceptance에 필요한 최소 capability를 `cross-domain-minimum-capability.md`의 handoff로 고정한다.
   승인된 target은 실제 seam이 생기기 전까지 `absent-or-partial`이다. 완료 기준: 모든 finding과
   cross-domain contract에 evidence·관계·minimum capability·disposition이 있다.
5. **사용자 결정을 요청한다.** public contract가 absent/partial이거나 정책 결정이 필요하면 `planning_state: planning`, graph gate false,
   구조화된 open finding·pending decision request·blocker를 기록하고 Triage와 함께 PM-owned Planning
   Decision을 native link로 만든다. Decision은 질문·선택지·evidence를 담은 `blocked` card다. 사용자가
   comment를 남기고 `running`으로 바꾸면 PM은 next workflow run에서 comment를 read-back하고 답변을 남긴다.
6. **결정 뒤 고정한다.** Decision body와 Triage의 같은 `decision_request_id`가 approved change,
   minimum capability, producer·consumer·integration follow-up까지 일치하는지 확인한다. 사용자 결정,
   requirement 변경, 필요한 `sync-docs` 결과와 Issue를 read-back한 뒤 finding/request를 해결 상태로 갱신한다.
   완료 기준: validator가 unresolved blocker를 보고하지 않는다.
7. **계획을 고정한다.** 모든 blocker가 해소되면 `planning_state: frozen`,
   `build_task_graph.allowed: true`, `blocker: null`인 copy와 `frozen_digest`를 `freeze` 명령으로
   생성해 검증한다. Native card는 `triage`로 유지한다. 완료 기준: digest가 일치하는 frozen
   body와 열린 graph gate를 read-back했다.
8. **Graph로 넘긴다.** `build-task-graph`에 frozen body와 승인 근거를 전달한다. graph 전체의 native
   read-back 뒤 `activate_graph.py` preflight를 통과한다. 현재 native runtime은 `triage → done`을 지원하지
   않으므로 `--apply` 실패 뒤 promote하지 않는 것이 안전 결과다. core transition이 제공된 뒤에만 Planning
   Decision/Triage 완료와 첫 Impl 또는 no-Impl Review의 single `ready`를 수행한다.

## 사용자 결정 요청

JSON request에는 `id`, `problem`, `why`, evidence locator, 하나 이상의 option,
`decision_owner`, `decision_status`, `approved_change`를 기록한다. Pending 또는 deferred request는
graph gate를 닫는다. Approved request에는 승인된 변경을 기록하고 requirement와 파생 상태를
read-back한다. 추천은 결정과 구분한다.

## 검증

완료를 보고하기 전에 다음을 모두 확인한다.

- Native task ID, title, status, assignee, JSON body와 조건부 service-planning link
- Current-state snapshot SHA와 freshness
- 모든 문서의 disposition, locator와 follow-up
- 정책 결정과 requirement/document/Issue 동기화 evidence
- `planning_state: frozen`, 열린 graph gate와 native `triage` creator-gate
- Graph 작성 뒤 Triage `done`과 exactly-one-ready 결과

Helper가 증명하지 않는 clean tree, leaf lineage, active-card uniqueness와 native link는 절차의
native read-back으로 증명한다. Frozen body는 persisted `frozen_digest`를 재검증하고 graph 작성 동안
digest가 바뀌지 않았는지 확인한다. 사실, 사용자 결정, blocker, 검증과 다음 transition을 분리해
한국어로 보고한다.

## 주의점

- `no-change`는 검토 완료를 뜻한다. 제외 대상은 `not-applicable`로 기록한다.
- Triage에서 `current-state.md`를 수정하지 않는다.
- 실행 가능한 `CHECK`/`EXPECT` 계약은 `build-task-graph`에서 작성한다.
- Coder가 Triage나 source 문서에서 누락 요구사항을 추론하게 하지 않는다.
- Direct message 대신 Kanban card와 native relationship을 authoritative handoff로 사용한다.
- Read-back하지 않은 transition을 성공으로 보고하지 않는다.