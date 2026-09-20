# Project Manager Workflow Design

> **Status: 결정된 workflow 기준 / runtime은 부분 구현.** 이 문서는 사용자가 PM의 역할,
> handoff, lifecycle을 이해하고 다른 Profile의 경계를 설계하기 위한 안내서다.
> 실제 Skill·helper·native Kanban E2E의 지원 여부는 각 Skill과 contract의 검증 상태를 따른다.
> 용어는 [TERMINOLOGY.md](TERMINOLOGY.md)를 따른다.

## 1. PM이 해결하는 문제

PM은 서비스 기획, 요구사항, 현재 구현 상태, Issue, 작업 카드, 검토와 release를 연결한다.
PM의 산출물은 코드가 아니라 **다음 역할이 재해석 없이 실행할 수 있는 계약과 사실에 맞는 전이**다.

```text
사용자 승인 requirement + fresh current-state + leaf Issue
→ PM triage와 policy/document routing
→ PM authored, self-contained Coder graph
→ Coder implementation + self-verification
→ PM checkpoint
→ independent aggregate Review
→ finding rework 또는 Summary finalization
→ current-state snapshot + PR/CI/merge + Issue auto-close read-back
```

### 역할 경계

| 역할 | 책임 | 하지 않는 일 |
|---|---|---|
| User | business policy, API 의미, authorization·consistency·error semantics, UI 요구사항의 최종 결정 | 미결정 정책을 PM에게 암묵 위임 |
| PM | planning admission, service planning, triage, task contract, graph routing, checkpoint, release 사실 확인 | source·test·migration 구현, Reviewer finding의 대체·삭제 |
| Coder | self-contained Impl card의 구현·검증 | requirement를 독자 재기획하거나 product policy 결정 |
| Reviewer | aggregate contract 기반 독립 review와 structured finding | business policy의 독자 결정 |

PM은 Amazon 등 외부 서비스 조사, 사용자 흐름, 화면 목적, UI 우선순위와 정책 선택지를 정리할 수 있다.
그러나 조사 결과나 선호는 사용자 결정을 대체하지 않는다. 결정된 내용은 requirement를 먼저 갱신한 뒤
Issue, 파생 문서, Coder contract에 전파한다.

## 2. 계획 admission과 reference baseline

새 leaf Issue planning, triage, delivery branch, implementation graph 생성은 모두 clean repository에서만
시작한다. docs, `.hermes`, scripts, configuration, untracked 파일도 clean 판정에 포함한다. PM은 dirty
변경을 자동 commit, stash, reset, discard하지 않는다.

PM의 정상 planning 입력은 다음 세 가지다.

1. 사용자 승인 requirement — 목표 behavior의 기준
2. freshness가 확인된 `current-state.md` — 현재 구현 상태의 요약
3. GitHub leaf Issue — delivery tracking과 scope identity

문서가 충분하면 PM은 source·test·migration을 전수 조사하지 않는다. 문서 부족·충돌 또는
Coder/Reviewer의 구현 불일치 보고가 있을 때만 필요한 범위의 committed source를 조사한다. 조사로도
policy·ownership·authorization·consistency·error semantics를 결정할 수 없으면 native `blocked`와
사용자 decision request로 올린다.

### 최초 도입 또는 불일치가 큰 경우

일상 planning 전에 `establish-reference-baseline`을 수행한다.

```text
clean repository
→ requirement / current-state / derived docs / Issue tree inventory
→ 불일치를 user decision · documentation update · tracker update · no change로 분류
→ 승인 requirement에 맞춰 파생 문서와 Issue tree를 동기화하고 read-back
→ current-state 외 reference docs commit
→ update-current-state
→ normal leaf Issue planning 허용
```

이는 모든 문서를 다시 쓰는 작업이 아니라 불일치의 owner와 다음 행동을 확정하는 reconciliation gate다.
`current-state.md`는 마지막 구현 snapshot이며, requirement·정책·파생 문서의 불일치를 고치는 문서가 아니다.

## 3. 새 Issue의 lifecycle

### 3.1 Issue 선택과 branch

PM은 domain root에서 open descendant를 읽고, 선택 가능한 open leaf Issue를 결정한다. delivery branch는
`{domain}/issue{number}` 형식이다. 같은 domain은 필요한 경우 선행 canonical delivery branch를 base로
stacked chain을 사용한다.

```text
main → p2/issue137 → p2/issue138 → p2/issue139
```

이미 target branch가 있으면 중복 branch를 만들지 않는다. marker, committed state, Issue 상태를 read-back해
재개·recovery·완료 중 무엇인지 판정한다. temporary/test/experiment branch는 canonical base가 아니다.

### 3.2 `create-triage`

모든 새 leaf Issue는 하나의 PM-owned triage card에서 시작한다. triage는 Coder contract가 아니라 다음을
정리하는 mutable planning record다.

- 구현 아이디어, 후보 vertical slice와 dependency, 검증 방향
- scope와 explicit out-of-scope
- requirement, architecture, ADR, glossary, ERD, index의 document-impact
- policy contradiction·omission과 user decision request
- `build-task-graph` handoff에 필요한 baseline evidence

완료된 triage body는 freeze되며 planning provenance로 남는다. Native Kanban에서 parent 없는 task가
즉시 `ready`가 되므로 graph authoring 동안에는 first eligible Impl의 temporary scheduling parent이기도
하다. PM이 graph 전체를 read-back한 뒤 triage를 완료하면 그 Impl 하나만 `ready`로 승격된다. active
leaf Issue당 active triage card는 하나만 허용한다.

```text
triage → running → done
          └──────→ blocked → running
```

`needs-input`은 native Kanban status가 아니다. semantic 이유를 card body에 기록하고 native 상태는
`blocked`로 유지한다. policy 문제가 있으면 PM은 triage를 blocked로 전이하고 linked
`service-planning` card를 만든다. 사용자 결정 → requirement update → 영향 문서·Issue read-back 뒤
**같은** triage를 재개한다.

### 3.3 executable graph

frozen triage, approved requirement, fresh current-state, Issue를 입력으로 PM이 manual native graph를 작성한다.
Coder가 다른 문서를 다시 해석하지 않도록 Impl card에는 다음을 materialize한다.

- goal, scope, explicit out-of-scope
- actor/authorization와 input/output contract가 적용되는 경우 그 literal contract
- state invariant·error semantics, API/persistence/transaction/event/module/external-system constraint
- acceptance criteria와 실제 verification 목표
- PM traceability locator; dependency는 native Kanban link로 관리

Backend Impl body의 authoring semantics와 validation은 PM의
`build-task-graph/references/implementation-card-contract.md`가 소유한다. Coder의 consumption contract는
같은 body를 admission하고 handoff를 작성하는 데 필요한 의미만 소유한다. PM은 baseline SHA, native
assignee/status/link/workspace와 실행 결과를 body에 복제하지 않는다.

PM은 built-in decomposer를 사용하지 않는다. 새 Impl·Review는 `todo`로 하나씩 생성·read-back하고,
body·assignee·link·status를 검증한 뒤 첫 eligible Impl **하나만** `ready`로 promotion한다.

```text
G<N>-Issue<M>-Triage → first eligible Impl
Impl → Review<Q> → Summary
Review<Q> → corrective Impl → Review<Q+1> → Summary
Review<Q> → Decision → corrective Impl 또는 user decision
```

- `Summary`는 semantic Issue root·finalization card다.
- initial Impl은 Review1과 Summary의 parent다.
- Review-originated Impl은 source Review의 child이자 next Review·Summary의 parent다.
- 모든 Review·Decision은 Summary의 parent다.
- native links는 scheduling prerequisite이며 semantic containment 방향을 표현하지 않는다.

## 4. 구현·checkpoint·review

### Child checkpoint

```text
Coder implementation + focused tests + backend full test
→ native same-card PM review 요청 (`running → review`)
→ PM: evidence, changed paths, staged diff, commit boundary 확인
→ PM commit
→ PM: result commit SHA, committed paths, clean worktree read-back
→ PM이 동일 Impl card를 `review → done`
→ 다음 eligible Impl promotion
```

PM checkpoint는 clean committed checkpoint와 truthful lifecycle을 보장한다. specialist aggregate review를
대체하지 않는다.

### Aggregate review와 finding

모든 initial Impl이 완료되면 PM-authored aggregate Review가 Reviewer에게 ready 된다. Review body에는
대상 Impl, inherited completed behavior, aggregate invariant, cross-API·module boundary, exclusion,
child evidence read-back 요구와 review question을 담는다.

Reviewer는 immutable Review contract를 읽고 completion metadata에 structured finding을 남긴다. PM은 task,
run, metadata를 native read-back한 사실만 다음 전이의 근거로 삼는다.

| finding verdict | PM의 다음 전이 |
|---|---|
| `correction-required` | 현재 effective behavior를 충족하는 self-contained corrective Impl을 같은 generation에 append |
| `context-required` | 필요한 committed source context를 read-back하고 다음 Review를 생성 |
| `decision-required` | 같은 generation에 native `blocked` Decision card 생성, 사용자 결정 대기 |
| `resolved` | 다음 Review가 기존 finding의 해결을 확인 |

완료된 Impl·Review·Summary의 body, run, comment, ID는 history다. PM은 이를 고치거나 finding을 조용히
삭제하지 않고 새 work와 link를 append한다. Summary는 모든 direct parent가 done이고 latest Review의
blocking finding이 resolved이며 새 blocking finding이 없을 때만 finalization한다.

## 5. requirement change와 recovery

### Requirement rework

aggregate Review 승인 전 사용자가 requirement를 바꾸면 PM은 old/new requirement의 Git diff를
behavior delta로 정리한다. hunk 자체는 task가 아니다. 새 generation G{N+1}은 `A′ = A + AA`처럼
**완전한 effective behavior**를 Coder contract로 제공한다.

- done card는 immutable history로 보존한다.
- affected running work는 실제 blocker를 기록하고 worker 종료를 read-back한 뒤 archive한다.
- inherited done behavior는 새 requirement를 여전히 만족하는지 committed evidence로 재확인한다.
- partial 또는 absent behavior만 새 Impl로 만든다.
- replacement graph 전체의 native read-back 뒤 첫 eligible Impl 하나만 ready로 만든다.

aggregate Review 승인 후 requirement가 바뀌면 기존 graph를 변경하지 않는다. 새 Issue의 `new` graph로
추적한다.

### Review rework

done Review의 finding은 같은 generation에 append-only로 처리한다. 이전 finding은 다음 Review에서
`resolved` 또는 다시 열린 verdict를 가져야 하며 사라질 수 없다. 완료 contract를 무효화하는 사용자
결정만 requirement-rework로 G를 증가시킨다.

### Interrupted workflow와 base sync

active workflow marker가 있고 dirty delta가 동일 Issue에만 명확히 귀속되는데 Kanban 기록이 유실된 경우,
PM은 하나의 `restart-task`로 clean committed checkpoint를 복원한다. unrelated 또는 unknown dirty 변경은
blocked와 사용자 decision으로 올린다.

B 작업 중 A가 소유한 public contract·domain rule 결함을 발견하면 B를 block하고 A rework → checkpoint →
B base sync 순서로 진행한다. `run-workflow`의 `base-sync-v1`은 rebase/merge 선택 근거,
impact-analysis evidence, affected downstream card와 post-sync verification을 검증한다. Semantic conflict는
Decision으로 올리며 Coder나 PM이 독자 결정하지 않는다.

## 6. Finalization과 release

```text
all required Impl done
→ latest Review의 blocking finding resolved
→ final implementation SHA freeze + clean worktree 확인
→ final implementation SHA를 조사해 current-state 갱신 및 marker 종료
→ docs-only commit
→ branch push
→ default branch 대상 PR 생성 (Closes #<leaf-issue>)
→ 실제 PR head SHA의 CI 확인
→ merge read-back
→ GitHub가 leaf Issue를 auto-close했는지 read-back
```

current-state docs-only commit은 implementation을 바꾸지 않으므로 Review의 code verdict를 무효화하지 않는다.
다만 `final implementation SHA`와 docs commit을 포함한 PR head SHA는 구분한다. PM은 별도 Issue close
mutation 대신 default-branch PR의 closing keyword와 merge 뒤 auto-close read-back을 사용한다.

## 7. 다른 Profile을 설계할 때 재사용할 기준

이 workflow는 PM에만 필요한 권한과, 역할 간 재사용 가능한 handoff를 구분한다.

| 재사용 가능한 원칙 | PM 전용으로 남길 것 |
|---|---|
| 승인된 입력에서 시작, 불확실성을 명시적으로 block, self-contained handoff, evidence와 read-back으로 완료 판정, immutable history | Issue 선택, planning baseline, task graph authoring·promotion, active marker, commit, PR·CI·merge·Issue lifecycle |
| User가 policy 결정, 실행자와 reviewer를 분리, implementation review와 aggregate review를 분리 | Kanban title/topology, Summary membership, PM checkpoint의 commit 책임 |
| Skill은 procedure, reference contract는 persisted schema, SOUL은 정체성·권한 경계를 소유 | PM의 specific task-body schema와 GitHub/Kanban write capability |

새 Profile은 이 문서를 복사해 tool checklist를 만들기보다 다음을 먼저 결정한다.

1. 어떤 입력을 사실의 기준으로 삼는가?
2. 어떤 결정은 사용자 또는 다른 역할로 escalation하는가?
3. 누구에게 어떤 self-contained contract를 handoff하는가?
4. 어떤 상태·외부 mutation을 read-back해야 완료라고 말할 수 있는가?
5. 그 역할에 필요한 최소 capability와 독립 state는 무엇인가?

## 8. 현재 구현·검증 상태

다음은 **결정된 workflow**이지만 아직 모두 runtime으로 검증된 것은 아니다.

- manual native graph, `new`·`requirement-rework`·`review-rework` procedure와 finding contract는 설계 기준으로 합의됐다.
- `build-task-graph` helper/validator는 `backend-implementation-card-v1`, graph-level Review·Summary·Decision body/topology와 `new`·rework mode를 지원하며 native Triage gate E2E를 제공한다.
- Root `run-workflow`는 runtime frontier, backend Impl handoff·changes-request·checkpoint, Review finding closure, Summary admission, release, base-sync, interrupted-workflow와 CI/PR finding contract·validator를 지원한다. Native graph E2E는 Review finding·idempotent rework·Summary admission frontier까지 검증하며 실제 GitHub release E2E는 외부 Issue에서 수행해야 한다.
- `update-current-state`는 `src/**` freshness, 문서 schema, recovery marker와 read-only inspection helper를 지원한다.
- `service-planning`은 근거 분류, 선택지 비교, UI 결정 필드, requirement read-back과 보류 규칙을 지원한다.
- `sync-docs`는 impact matrix, disposition, 파생 문서와 GitHub Issue 동기화·read-back 절차를 지원한다.
- `service-planning`, `sync-docs`의 실제 외부 서비스·GitHub mutation E2E와 PM-focused Semble runtime E2E는 아직 미검증이다.

따라서 이 문서를 Profile topology와 ownership의 기준으로 사용하되, 실제 mutation 전에 해당 Skill,
reference contract, enabled capability, native tool schema를 read-back해야 한다.
