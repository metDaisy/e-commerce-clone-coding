# Project Manager Workflow Design

> **Status: partial implementation.** 수동 native graph와 세 rework mode의 procedure·finding
> contract는 합의됐다. helper/validator와 native E2E는 별도 구현·검증 항목이다.
> 용어는 [TERMINOLOGY.md](TERMINOLOGY.md)를 따른다.

## 0. Existing project reference baseline

PM의 기본 planning 입력은 사용자 승인 requirement, freshness가 확인된 `current-state.md`, GitHub
Issue다. requirement는 목표 계약이고, current-state는 구현 상태의 요약 기준이며, GitHub Issue는
delivery tracking이다. PM은 이 문서들이 충분할 때 source·test·migration을 전수 조사하지 않는다.
문서 부족·충돌 또는 Coder/Reviewer의 구현 불일치 보고가 있을 때만 필요한 범위의 source 조사를
수행하거나 native `blocked`와 decision request로 사용자에게 올린다.

이 workflow를 처음 도입할 때는 normal Issue planning보다 먼저 PM-owned
`establish-reference-baseline`을 수행한다.

```text
clean repository
→ requirement / current-state / derived reference docs / GitHub Issue tree inventory
→ 문서 간 divergence를 사용자 decision 또는 제한적 source 조사로 해소
→ requirement를 기준으로 architecture, ADR, glossary, ERD, index 등 derived docs 갱신
→ GitHub Issue tree의 scope·status·dependency를 requirement와 대조해 갱신 및 read-back
→ current-state.md를 제외한 reference docs commit
→ update-current-state
→ 이후 normal leaf Issue planning 허용
```

이 baseline task는 모든 문서를 한 번에 다시 쓰는 작업이 아니라, 불일치를 `user decision`,
`documentation update`, `tracker update`, `no change`로 분류하고 필요한 작업을 routing하는
reconciliation gate다. 문서·tracker 갱신이 완료되기 전에는 `current-state.md`를 최신 snapshot으로
갱신하지 않는다. source와 requirement의 의미가 충돌하면 PM은 어느 쪽을 우선한다고 추측하지 않고
사용자에게 결정 요청을 올린다.

기준 baseline이 성립한 뒤의 일상 workflow에서는 PM이 매번 documentation readiness를 확인하고,
불일치가 있을 때만 `sync-docs` task를 만든다. 이 task는
`docs/current-state.md`를 제외한다.

## 1. 목표와 역할 경계

Project Manager는 **서비스 기획과 개발 작업 흐름을 정리하는 조율자**다. 요구사항, 현재 구현,
작업 카드, 검토, release 흐름을 연결해 다음 작업이 무엇인지와 왜 필요한지를 분명하게 만든다.

```text
user-approved requirements + fresh current-state + GitHub Issue tree
→ Project Manager: current-state admission
→ create-triage: Issue implementation idea, document impact, policy-gap review
→ service-planning: user decision when triage finds a business-policy problem
→ Project Manager: task contract, branch/lifecycle, checkpoint
→ implementation-coder: child card의 구현·테스트
→ Project Manager: commit checkpoint와 routing
→ Reviewer: PM이 작성한 root review card를 기준으로 aggregate review
→ Project Manager: finding 기반 rework contract와 다음 lifecycle 전이
```

- PM은 requirement/API/business semantics를 새로 정하지 않는다. 근거가 모순·누락되면
  native `blocked` 상태와 decision request로 사용자에게 올린다.
- Coder는 self-contained child card만 실행 입력으로 사용한다. testing-guide 또는 다른
  문서의 의무가 있으면 PM이 child card에 구체적인 계약으로 materialize한다. Coder는
  requirement·source·current-state를 다시 찾아 기획하거나 task contract를 독자적으로
  확장하지 않는다.
- PM이 child card와 root review card를 모두 작성한다.
- Issue aggregate review에는 Reviewer Profile 하나만 사용한다.
- Reviewer는 명시 AC/review question과 task에 materialize된 security, compatibility,
  module-boundary constraint 위반을 finding으로 기록한다. business policy를 독자 결정하지 않는다.

### 1.1 서비스 기획과 UI 논의

PM은 Amazon과 다른 e-commerce 서비스 조사, 기능 비교, 사용자 흐름, 화면 목적, UI 우선순위,
불명확한 business policy의 선택지와 영향을 정리할 수 있다. UI와 frontend 방향도 PM과 논의한다.

다만 PM은 조사 결과나 선호만으로 product policy·API semantics·UI 요구사항을 확정하지 않는다.
사용자가 최종 결정을 내리면 PM은 그 결정을 requirement, derived docs, GitHub Issue, task contract로
전파한다.

별도 frontend manager/Profile은 현재 만들지 않는다. PM이 UI 기획을 맡고 Coder가 task contract에
따라 구현한다. 반복적으로 design system, accessibility, visual QA, frontend architecture가 병목이
될 때만 specialized Profile 도입을 재검토한다.

### 1.2 Policy change와 requirement-rework

사용자와 PM이 policy 변경을 논의하면, 사용자가 먼저 requirement를 갱신한다. PM은 승인된 변경을
`build-task-graph`의 `requirement-rework` mode로 Coder contract에 투영한다. 파생 문서와 GitHub
tracker 영향은 `sync-docs`가 다루며, PM은 requirement가 갱신되기 전 policy 변경을 구현 card로
전환하지 않는다.

`create-triage`가 policy 모순·누락을 발견하면 PM은 정책을 간단히 결정하지 않는다. triage card를
`blocked`로 전환하고, 문제·근거·선택지·추천을 담은 별도 `service-planning` card를 linked
relationship으로 만든다. 사용자가 결정한 뒤에만 requirement를 수정하고, `sync-docs`로 영향받은
파생 문서와 Issue를 동기화한 다음 기존 triage card를 재개한다.

### 1.3 create-triage

모든 새 leaf Issue의 Kanban task 생성은 `create-triage`에서 시작한다. 이는 짧은 preflight가
아니라 Issue를 구현하기 위한 아이디어·후보 vertical slice·후보 dependency·검증 방향과 문서
영향을 정리하는 PM-owned planning card다. 구현 가능한 실행 계약은 `build-task-graph`가
작성한다.

native Kanban plugin의 상태는 다음과 같이 사용한다.

```text
triage → running → done
          └──────→ blocked → running
```

`todo`와 `ready`는 plugin이 claim 전에 명시적 promotion을 요구하는 경우의 queue 상태다.
`needs-input`은 Kanban 상태가 아니라 triage body의 `blocker.kind`와 decision request로
기록하며 native 상태는 `blocked`로 유지한다.

active leaf Issue당 active triage card는 하나만 허용한다. 문제가 없으면 body를 freeze하고
`done` 처리한 뒤 `build-task-graph`로 넘긴다. 정책·요구사항 문제가 있으면 `blocked`로 유지하고
service-planning 완료, requirement·문서·Issue read-back 뒤 같은 card를 재개한다. 완료된
triage card는 graph runtime dependency가 아니며 graph provenance로만 참조한다.

triage body의 정본은 JSON이다. requirement, architecture, ADR, glossary, ERD, index는 각각
`update | no-change | not-applicable | blocked` 중 하나로 판정한다. `no-change`는 검토했지만
수정할 필요가 없다는 뜻이다. `current-state.md`는 영향 문서 표가 아닌 read-only snapshot
객체이며 triage에서 수정하지 않는다. 정책 판단이 필요할 때만 linked `service-planning` card가
문제·근거·선택지·추천을 사람이 검토할 수 있는 Markdown으로 제공한다.

`triage.py template`은 PM이 이미 read-back한 Issue identity, planning/current-state SHA,
requirement/current-state locator만 받아 repository `.temp/triage/issue-<n>/`에 JSON draft를
쓴다. 실제 native task ID는 body의 중복 field가 아니라 Kanban provenance다. 생성 뒤 PM은
native `kanban_show`로 read-back하며, 필요할 때 `triage.py validate-card`가 공식 CLI를
read-only로 호출해 ID·status·assignee·body JSON의 일치를 검사한다. script는 Kanban mutation을
수행하지 않는다.

## 2. Planning admission과 current-state 경계

새 leaf Issue planning, create-triage card 생성, delivery branch 생성, implementation task
graph 생성은 repository가 clean일 때만 시작한다.

```text
clean = staged 변경 없음 + unstaged 변경 없음 + untracked 변경 없음
        (ignored 파일 제외)
```

source/test뿐 아니라 docs, `.hermes`, scripts, configuration, untracked 파일도 이 조건에
포함한다. PM은 existing dirty 변경을 자동 commit, stash, reset, discard하지 않는다.

`update-current-state`의 schema, freshness 판정, inspection 범위, draft generator, active workflow
marker의 field와 start/end transition은 [`skills/update-current-state/plan.md`](skills/update-current-state/plan.md)가
소유하며 별도 session/branch에서 상세 설계한다. 이 workflow가 소비하는 integration fact는 다음뿐이다.

```text
current-state: fresh | stale | insufficient
active workflow: absent | present
```

- `fresh`일 때만 새 leaf planning을 시작한다.
- `stale` 또는 `insufficient`이면 `update-current-state` 또는 native `blocked` 상태로 routing한다.
- active workflow가 존재하면 `controll-task-graph`가 recovery 가능 여부를 판정한다. 동일 Issue에만
  귀속 가능한 dirty delta와 유실된 Kanban 기록은 `restart-task`로 clean checkpoint를 복원할 수 있다.
  unrelated 또는 unknown dirty 변경이 있으면 native `blocked`와 decision record로 올린다.

## 3. Issue·branch 선택

1. PM은 domain root Issue부터 open descendant를 탐색한다.
2. ancestor Issue 본문을 읽고, 구현할 open leaf Issue를 Issue 번호 오름차순으로 선택한다.
3. PM은 해당 domain index, policy, resource requirement와 공통 requirement를 읽는다.
4. delivery branch는 `{domain}/issue{number}` 형식이다. 예: `p2/issue138`.

같은 domain은 stacked branch를 사용한다.

```text
main → p2/issue137 → p2/issue138 → p2/issue139
```

- target Issue branch가 아직 없으면, target 직전 canonical delivery branch의 committed HEAD를
  base로 사용한다.
- target delivery branch가 이미 존재하면 새 branch를 만들지 않고, current-state integration fact·Issue
  상태·필요한 범위의 evidence를 기준으로 재개, recovery, 또는 완료 여부를 판정한다.
- 임시·test·experiment branch는 canonical delivery stack base가 아니다.
- target branch 생성 뒤 base가 전진한 경우에만 PM이 base-sync를 소유한다.

### 3.1 Upstream correction과 B synchronization

B 구현 중 발견한 결함이 A가 소유하는 공통 behavior·public contract·domain rule에 속하면, PM은
B의 현재 task를 blocked로 전이하고 A rework task를 만든다. A rework가 committed checkpoint를
만든 뒤 PM은 B base-sync task를 만든다. B 자체의 consumer/API-specific 처리만 필요한 경우에는
B task에서 수정한다. requirement/policy의 owner 또는 의미가 불명확하면 사용자 판단으로 올린다.

PM은 A correction의 의도, A/B requirement·source locator, 유지할 combined behavior, sync SHA와
영향받는 B task를 context로 작성한다. Coder가 rebase/merge의 technical conflict를 해결하고
검증한 뒤 PM checkpoint를 받는다. Coder가 semantic conflict를 독자적으로 결정하지 못하면
native `blocked`와 decision request로 사용자에게 올린다.

base-sync의 정확한 필요 판정, rebase/merge 선택, impact-analysis evidence, Coder conflict context,
conflict task body는 **TBD**다. 이미 공유·review된 branch는 merge 또는 blocked 판단, 아직
공유·review되지 않은 child branch는 rebase 가능이라는 방향만 합의했다. docs/policy 변경이 B task
contract에 영향을 주면 source conflict가 없어도 영향을 받은 unfinished B task를 refresh한다.

## 4. build-task-graph

PM은 frozen `create-triage` body를 바탕으로 Issue를 요약하는 데 그치지 않고 requirement의
domain index·policy·model·관계·API·error·state
규칙과 fresh current-state의 관련 section을 Coder가 실행 가능한 card contract로 투영한다.

### 4.1 문서 우선 planning

`build-task-graph`의 기본 입력은 frozen `create-triage` body, 승인 requirement, current-state,
Issue다. 문서가 충분하면 PM은 구현
충족 여부를 source/test 존재만으로 재판정하지 않는다. 문서가 부족하거나 서로 충돌할 때, 또는
Coder/Reviewer가 실제 구현과의 불일치를 보고할 때만 source locator를 제한적으로 조사한다.
그 결과도 policy 해석이 필요한 경우에는 `create-triage`와 `service-planning`으로 routing한다.

### 4.2 Decomposition과 dependency

- data model/repository/migration이 필요하면 첫 child task로 만든다.
- API task는 requirement의 API 순서로 chain dependency를 둔다.
- initial Impl은 Review1과 Summary의 parent다. Review finding으로 생긴 Impl/Decision은 source
  Review의 child이고, corrective Impl은 다음 Review와 Summary의 parent다. 모든 Review/Decision은
  Summary의 parent다. PM은 모든 신규 card를 `todo`로 수동 생성·read-back한 뒤 graph 전체가
  검증되었을 때 하나의 eligible Impl만 `ready`로 올린다.
  atomic graph-create나 decomposer를 사용하지 않는다.
- Coder child task의 exact changed-file allowlist는 강제하지 않는다. PM은 HTTP,
  application, domain, error, persistence, test entry surface를 context로 제공한다.

### 4.3 Child implementation card

PM이 작성하며 다음을 포함한다.

```text
- Goal과 Scope
- literal request/response JSON
- actor/authorization
- field semantics, state/invariant, error contract
- explicit out_of_scope
- acceptance criteria와 verification 목표
- requirement/current-state locator와 dependency
- 예외적으로 확인한 source/test/migration/module boundary locator
```

requirements가 명시한 `attributes` normalization, error code, authorization, transaction 범위는
PM이 결정하는 것이 아니라 contract에 누락 없이 투영한다.

## 5. controll-task-graph

### 5.1 Child checkpoint

```text
Coder 구현·self-verification
→ native kanban_request_review(reviewer=project-manager)
→ PM이 same-card operational checkpoint 수행
→ changed path, evidence, staged diff, commit boundary, dirty/untracked contamination 확인
→ PM commit과 Git read-back
→ PM이 같은 child card를 done 처리
→ 다음 child가 eligible
```

PM checkpoint는 specialist code review를 대체하지 않는다.

### 5.2 Root review

PM은 semantic `G{N}-Issue{M}` root, Reviewer-assigned `G{N}-Issue{M}-Review{Q}`, Coder Impl
card를 만든다. root에는 effective behavior, generation, inherited evidence를 기록하고 Review에는
aggregate review contract를 기록한다.

```text
aggregate Review body
- 대상 implementation task IDs와 inherited completed behavior
- aggregate contract
- cross-API consistency와 module boundary
- aggregate exclusions
- child evidence read-back 요구
- PM-authored review questions
- immutable Review contract; Reviewer completion metadata의 per-finding verdict
```

모든 initial Impl이 done이면 Review가 ready가 된다. Review 완료 뒤 Summary가 바로 ready가 되는 것이
아니다. 모든 Summary direct parent가 done이고 latest Review의 blocking finding이 모두 resolved일 때만
PM이 Summary promotion/finalization을 수행한다. 상세 execution evidence는 child body/run/comment에서
read-back한다.

Review는 `done` completion metadata의 `findings[]`로 PM에 handoff한다. PM은 creator-session
terminal wake-up 또는 recovery에서 task/run/metadata를 read-back한다. `correction-required` finding은
기존 behavior를 충족시키는 corrective Impl로, `context-required`는 source read-back 뒤 새 Review로,
`decision-required`는 same-G blocked Decision card로 routing한다. 이전 finding은 후속 Review에서
`resolved` 또는 다시 열린 verdict를 가져야 하며 조용히 사라질 수 없다. Review body·done history는
수정하지 않고 새 work와 link를 append한다.

### 5.3 GitHub guide

GitHub Issue tree, PR, CI, merge, closing keyword, Issue auto-close와 mutation read-back의 PM 전용
규칙은 향후 `controll-task-graph`의 `references/github-guide.md`가 소유한다. generic `github` Skill은
이 workflow의 source of truth가 아니다.

## 6. Finalization

각 child implementation은 root review 전에 이미 PM checkpoint로 committed checkpoint가 된다.
따라서 root review가 approved일 때 새 code commit은 만들지 않는다. PM은 그 HEAD를
`final_implementation_sha`로 freeze한다.

```text
모든 child done
→ latest aggregate Review의 모든 blocking finding resolved
→ final_implementation_sha freeze + clean worktree 확인
→ PM이 final implementation SHA를 조사해 current-state.md 갱신
→ docs-only commit
→ branch push
→ main을 base로 PR 생성
→ PR body에 Closes #<leaf-issue-number> 포함
→ PR head의 CI 확인
→ merge read-back
→ GitHub가 leaf Issue를 자동 close했는지 read-back
```

`current-state.md` 문서 commit은 implementation을 바꾸지 않으므로 root review의 code verdict를
무효화하지 않는다. 다만 PR head SHA는 docs commit을 포함하므로 `final_implementation_sha`와
PR head SHA를 구분해 기록한다. CI는 실제 PR head에 대해 확인한다.

GitHub closing keyword는 PR이 repository default branch를 base로 할 때만 자동 Issue close에
사용한다. PM은 별도 Issue close mutation을 하지 않고 merge 뒤 자동 close 상태를 read-back한다.

## 8. 아직 확정하지 않은 항목

- `build-task-graph` v5 helper/validator와 native rework E2E
- `create-triage`, `service-planning`, `controll-task-graph`, `sync-docs`, `update-current-state`의
  남은 SKILL.md 절차와 frontmatter
- update-current-state의 정확한 re-investigation 범위와 draft script CLI/output schema
- base-sync의 detailed procedure, conflict report body, impact-analysis evidence
- Reviewer가 PM-authored review question 밖의 finding을 제기할 수 있는지
- structured finding과 rework draft의 final schema/script
- PM-focused Semble 절차와 `references/github-guide.md`

## 9. 문서와 runtime 적용 순서

이 design을 확정한 뒤에만 다음을 적용한다.

```text
1. `build-task-graph` v5 helper/validator와 manual native create/read-back 절차를 연결
2. `create-triage`, `service-planning`, `controll-task-graph` Skill 확정
3. `sync-docs`, `update-current-state`는 각 별도 session/branch에서 상세 설계
4. PM-focused Semble와 GitHub guide 정렬
5. distribution manifest/README 정렬
6. fresh profile 설치·runtime native Kanban 호출·read-back 검증
```
