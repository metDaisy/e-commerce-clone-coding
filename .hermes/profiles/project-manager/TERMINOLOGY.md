# Project Manager 용어집

> 이 문서는 PM workflow를 읽고 다른 Profile을 설계할 때 쓰는 **개념 기준**이다.
> 실행 순서와 도구 호출은 각 Skill, persisted task-body field와 validator 규칙은
> `build-task-graph/references/board-contract.md`가 소유한다.

## 기준과 스냅샷

| 용어 | 의미 |
|---|---|
| requirement | 사용자가 승인한 목표 동작·정책의 계약. Issue와 구현 상태보다 기능 의미의 기준이 된다. |
| current-state | 특정 committed implementation을 조사해 만든 현재 구현 상태의 요약. 목표 요구사항이나 작업 계획을 대신하지 않는다. |
| reference baseline | requirement, current-state, 파생 문서, delivery tracker가 서로 모순되지 않음을 확인한 planning 출발점. 의미 충돌은 PM이 결정하지 않고 사용자에게 올린다. |
| inspection SHA | `current-state.md`를 갱신하기 위해 실제로 조사한 clean committed implementation SHA. |
| implementation snapshot SHA | current-state에 기록하는 inspection SHA. 이를 기록한 docs commit SHA와 구분한다. |
| planning baseline SHA | Issue 계획과 task contract가 근거로 삼는 committed repository 기준점. |
| implementation-covered path | 구현 snapshot에 영향을 주는 저장소 루트 `src/**`. Java source, backend resource·Flyway, backend test를 포함하며 frontend·build/CI/Profile·일반 문서는 제외한다. 정확한 freshness 판정은 `update-current-state`가 소유한다. |
| fresh / stale / insufficient | current-state가 planning에 사용 가능한지 나타내는 integration fact. `fresh`만 새 leaf Issue planning을 허용한다. `stale`은 구현-covered committed 변경 뒤 갱신되지 않은 상태, `insufficient`는 신뢰할 근거가 부족한 상태다. |
| clean working tree | ignored 파일을 제외하고 staged·unstaged·untracked 변경이 없는 상태. 새 planning·branch·graph 생성의 admission 조건이다. |
| dirty working tree | clean이 아닌 상태. 변경의 소속은 이 상태만으로 판정할 수 없다. |

### SHA를 구분하는 예

```text
H0  clean implementation을 조사
    implementation snapshot SHA = H0

H1  active workflow marker만 기록한 docs commit
    planning baseline SHA = H1
    implementation snapshot SHA = H0  (구현은 바뀌지 않아 fresh)

H2  child implementation checkpoint commit
    H0 snapshot은 stale

H3  H2를 조사해 current-state를 갱신한 docs-only commit
    implementation snapshot SHA = H2
```

## 진행·복구

| 용어 | 의미 |
|---|---|
| active workflow marker | 진행 중 implementation workflow를 복구하기 위해 `current-state.md`에 committed로 남기는 platform-independent 표식. board/task/profile ID와 상세 task body는 넣지 않는다. |
| start checkpoint | clean delivery branch에서 active workflow marker를 기록한 docs commit. |
| final implementation SHA | aggregate review 조건을 통과한 뒤 PM이 freeze하는 마지막 code implementation SHA. 이후 current-state docs commit이나 PR head SHA와 구분한다. |
| end checkpoint | final implementation SHA를 조사해 current-state를 갱신하고 active marker를 종료한 docs-only commit. |
| interrupted workflow | active marker는 남아 있으나 정상적인 Kanban 실행·handoff가 이어지지 않은 상태. |
| recovery | active marker, committed checkpoint, working-tree evidence를 read-back해 동일 작업을 안전하게 재개하는 과정. |
| restart-task | Kanban 기록이 유실되고 dirty delta가 같은 Issue에만 명확히 귀속될 때 PM이 만드는 단일 Coder recovery contract. 정상 graph를 새로 만들기 전에 clean committed checkpoint를 복원한다. |
| unattributed change | active marker 또는 확인 가능한 task contract에 연결할 수 없는 dirty 변경. PM은 이를 commit·stash·discard하거나 다른 task에 귀속하지 않고 `blocked`로 올린다. |
| sync-docs | `current-state.md`를 제외한 requirement 파생 문서와 delivery tracker의 불일치를 정리하는 PM workstream. |

## 카드·graph·review

| 용어 | 의미 |
|---|---|
| root Issue / leaf Issue | domain 또는 delivery 범위의 최상위 work item / 하위 work item이 없어 구현 대상으로 선택할 수 있는 work item. |
| delivery branch | 선택된 leaf Issue를 구현하는 canonical branch. 같은 domain에서는 선행 canonical delivery branch를 기반으로 stacked chain을 만들 수 있다. |
| triage card | 새 leaf Issue의 하나뿐인 PM-owned planning provenance. 구현 아이디어, 후보 slice·dependency, 문서 영향, policy blocker를 정리하고 body를 freeze한 뒤 graph authoring 동안 `running`을 유지한다. Coder 입력은 아니지만 first eligible Impl을 `todo`로 유지하는 native scheduling parent이며 graph 검증 뒤 `done`이 된다. |
| child implementation (Impl) card | Coder가 수행하는 하나의 독립적으로 검증 가능한 self-contained 구현 계약. |
| aggregate Review card | 관련 Impl과 inherited completed behavior를 하나의 aggregate contract로 검토하는 Reviewer-owned card. completion metadata의 finding verdict가 Summary eligibility를 결정한다. |
| Decision card | finding이 요구한 사용자 정책 결정을 보존하는 PM-owned native `blocked` card. PM은 정책을 대신 결정하지 않는다. |
| Summary | `G{N}-Issue{M}-Summary`라는 semantic Issue root이자 PM finalization card. immutable finalization contract를 보존하며, native direct-parent read-back으로 membership을 확인한다. |
| aggregate contract | PM이 Review에 작성하는 child contract의 공통 불변조건, 교차 API 일관성, module boundary, 검토 제외 범위의 요약. |
| PM checkpoint | Coder self-verification 후 PM이 같은 Impl card에서 evidence, changed paths, commit boundary, SHA, clean worktree를 read-back하고 commit·done 처리하는 운영 gate. independent aggregate review를 대체하지 않는다. |
| effective behavior | 현재 requirement revision에서 관찰 가능해야 하는 완전한 behavior. 변경 전 A에 AA가 추가되면 Coder contract의 기준은 history가 아닌 `A′ = A + AA`다. |
| inherited completed behavior | 새 generation에서도 requirement를 만족하는지 committed source/test evidence로 재확인되어 새 Review가 참조하는 done behavior. 기존 card를 복제하거나 re-done하지 않는다. |
| finding | Review completion metadata의 구조화된 결과. `correction-required`, `context-required`, `decision-required`, `resolved` verdict로 다음 전이를 결정한다. |

## generation과 rework

| 용어 | 의미 |
|---|---|
| generation (G) | 하나의 승인 requirement revision을 구현하는 graph 단위. 새 Issue의 최초 graph는 G1이다. |
| new | 선택한 leaf Issue의 G1 graph를 만드는 `build-task-graph` mode. |
| requirement-rework | aggregate Review 승인 전, 사용자가 승인한 requirement revision이 active graph의 완료 contract를 바꿀 때 G{N+1} graph로 supersede하는 mode. |
| review-rework | done aggregate Review finding을 읽어 같은 G에 corrective Impl, context re-review, 또는 Decision을 append하는 mode. |
| replacement task | stale 또는 obsolete unfinished task를 대체하는 새 task. |
| archived task | replacement로 더 이상 실행하지 않는 unfinished historical task. done history를 수정·되돌린다는 뜻이 아니다. |
| rework draft | requirement Git diff 또는 finding을 behavior delta와 Coder contract 후보로 정리한 planning artifact. diff hunk 자체는 task 경계가 아니다. |

## 역할과 문서 책임

| 항목 | 소유하는 책임 |
|---|---|
| User | business policy, API 의미, authorization·consistency·error semantics, UI 요구사항의 최종 결정 |
| Project Manager | planning admission, triage, executable contract, graph authoring·promotion, checkpoint, finding routing, release lifecycle의 fact read-back |
| Coder | self-contained Impl card의 구현과 self-verification |
| Reviewer | PM-authored aggregate contract에 대한 독립 finding과 verdict |
| `SOUL.md` | PM identity, 권한 경계, 불확실성·보고 기본값 |
| `WORKFLOW-DESIGN.md` | 사용자가 읽는 workflow topology, handoff, 지원 상태 |
| `create-triage` | triage procedure와 JSON planning record |
| `service-planning` | 사용자 결정 전 서비스·정책·UI 선택지 조사 |
| `build-task-graph` | task graph authoring 및 `new`·rework procedure |
| `run-workflow` | Kanban 중심 root workflow와 checkpoint, promotion, recovery, review routing, PR·CI·merge·Issue close 절차 |
| `sync-docs` | current-state 외 파생 문서와 tracker 동기화 |
| `update-current-state` | current-state schema, freshness, snapshot, marker의 상세 |
| `board-contract.md` | persisted task field, graph invariant, deterministic validator 규칙 |

`WORKFLOW-DESIGN.md`는 topology를 설명하는 안내서다. 다른 Profile은 이 문서의 역할 경계와
handoff를 재사용하되, PM의 Kanban·GitHub·commit 권한이나 task-body schema를 자동으로 상속하지 않는다.
