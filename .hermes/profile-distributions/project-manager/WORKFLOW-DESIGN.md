# Project Manager Workflow Design

> **Status: draft.** 이 문서는 현재까지 합의한 workflow 설계를 사람이 검토하기 위해
> 기록한다. 아직 `SOUL.md`, runtime Skill, `board-contract.md`, validator에 적용된
> 동작이 아니다. 용어는 [TERMINOLOGY.md](TERMINOLOGY.md)를 따른다.

## 0. Existing project reference baseline

PM의 기본 planning 입력은 사용자 승인 requirement, freshness가 확인된 `current-state.md`, GitHub
Issue다. requirement는 목표 계약이고, current-state는 구현 상태의 요약 기준이며, GitHub Issue는
delivery tracking이다. PM은 이 문서들이 충분할 때 source·test·migration을 전수 조사하지 않는다.
문서 부족·충돌 또는 Coder/Reviewer의 구현 불일치 보고가 있을 때만 필요한 범위의 source 조사를
수행하거나 `needs-input`으로 사용자에게 올린다.

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
→ Project Manager: planning, task contract, branch/lifecycle, checkpoint
→ implementation-coder: child card의 구현·테스트
→ Project Manager: commit checkpoint와 routing
→ Reviewer: PM이 작성한 root review card를 기준으로 aggregate review
→ Project Manager: finding 기반 rework contract와 다음 lifecycle 전이
```

- PM은 requirement/API/business semantics를 새로 정하지 않는다. 근거가 모순·누락되면
  `needs-input`으로 사용자에게 올린다.
- Coder는 child card와 `docs/testing-guide.md`를 기본 실행 입력으로 사용한다. requirement를
  다시 기획하거나 task contract를 독자적으로 확장하지 않는다.
- PM이 child card와 root review card를 모두 작성한다.
- Issue aggregate review에는 Reviewer Profile 하나만 사용한다.
- Reviewer가 PM의 review question 밖의 defect를 제기할 수 있는지는 **TBD**다.

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

## 2. Planning admission과 current-state 경계

새 leaf Issue planning, delivery branch 생성, implementation task graph 생성은 repository가
clean일 때만 시작한다.

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
- `stale` 또는 `insufficient`이면 `update-current-state` 또는 `needs-input`으로 routing한다.
- active workflow가 존재하면 `controll-task-graph`가 recovery 가능 여부를 판정한다. 동일 Issue에만
  귀속 가능한 dirty delta와 유실된 Kanban 기록은 `restart-task`로 clean checkpoint를 복원할 수 있다.
  unrelated 또는 unknown dirty 변경이 있으면 `needs-input`이다.

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
`needs-input`으로 사용자에게 올린다.

base-sync의 정확한 필요 판정, rebase/merge 선택, impact-analysis evidence, Coder conflict context,
conflict task body는 **TBD**다. 이미 공유·review된 branch는 merge 또는 blocked 판단, 아직
공유·review되지 않은 child branch는 rebase 가능이라는 방향만 합의했다. docs/policy 변경이 B task
contract에 영향을 주면 source conflict가 없어도 영향을 받은 unfinished B task를 refresh한다.

## 4. build-task-graph

PM은 Issue를 요약하는 데 그치지 않고 requirement의 domain index·policy·model·관계·API·error·state
규칙과 fresh current-state의 관련 section을 Coder가 실행 가능한 card contract로 투영한다.

### 4.1 문서 우선 planning

`build-task-graph`의 기본 입력은 requirement, current-state, Issue다. 문서가 충분하면 PM은 구현
충족 여부를 source/test 존재만으로 재판정하지 않는다. 문서가 부족하거나 서로 충돌할 때, 또는
Coder/Reviewer가 실제 구현과의 불일치를 보고할 때만 source locator를 제한적으로 조사한다.
그 결과도 policy 해석이 필요한 경우에는 `needs-input`으로 routing한다.

### 4.2 Decomposition과 dependency

- data model/repository/migration이 필요하면 첫 child task로 만든다.
- API task는 requirement의 API 순서로 chain dependency를 둔다.
- graph에는 한 시점에 하나의 implementation child만 eligible하도록 dependency를 구성한다.
  dispatcher가 전역적으로 하나만 ready로 승격한다는 보장은 전제하지 않는다.
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

PM은 child card들과 함께 Reviewer-assigned `root-review-1` card를 만든다. root card는 Coder
작업 본문이 아니라 Reviewer용 aggregate review contract다.

```text
root card body
- 대상 child task IDs
- aggregate contract
- cross-API consistency와 module boundary
- aggregate exclusions
- child evidence read-back 요구
- PM-authored review questions
- verdict protocol: approved | changes-requested | needs-input
```

모든 child가 done이면 `root-review-1`이 ready가 된다. root body는 immutable aggregate contract이며,
child 완료마다 source locator를 중복 기록하지 않는다. 상세 execution evidence는 child card의
body/run/comment에서 read-back한다.

review finding은 structured finding으로 남긴다. script는 provenance·affected task·기본 AC가 담긴
zero-ready rework draft를 생성할 수 있고, PM이 scope·out-of-scope·source context·AC·verification을
보완한 뒤 actual rework task를 만든다. rework 뒤에는 `root-review-2`를 만든다.

Reviewer scope와 finding schema의 상세는 **TBD**다.

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
→ root-review-{n} approved
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

- `service-planning`, `build-task-graph`, `controll-task-graph`, `sync-docs`, `update-current-state`의
  SKILL.md 절차와 frontmatter
- update-current-state의 정확한 re-investigation 범위와 draft script CLI/output schema
- base-sync의 detailed procedure, conflict report body, impact-analysis evidence
- Reviewer가 PM-authored review question 밖의 finding을 제기할 수 있는지
- structured finding과 rework draft의 final schema/script
- v4 board contract와 validator regression tests
- PM-focused Semble 절차와 `references/github-guide.md`

## 9. 문서와 runtime 적용 순서

이 design을 확정한 뒤에만 다음을 적용한다.

```text
1. `service-planning`, `build-task-graph`, `controll-task-graph` Skill 확정
2. `sync-docs`, `update-current-state`는 각 별도 session/branch에서 상세 설계
3. board-contract-v4 및 validator regression test
4. PM-focused Semble와 GitHub guide 정렬
5. distribution manifest/README 정렬
6. fresh profile 설치·runtime native Kanban 호출·read-back 검증
```
