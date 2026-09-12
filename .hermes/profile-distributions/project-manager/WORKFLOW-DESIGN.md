# Project Manager Workflow Design

> **Status: draft.** 이 문서는 현재까지 합의한 workflow 설계를 사람이 검토하기 위해
> 기록한다. 아직 `SOUL.md`, runtime Skill, `board-contract.md`, validator에 적용된
> 동작이 아니다. 용어는 [TERMINOLOGY.md](TERMINOLOGY.md)를 따른다.

## 1. 목표와 역할 경계

```text
requirements + GitHub Issue tree + committed repository evidence
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
- Reviewer가 PM의 review question 밖의 defect를 제기할 수 있는지는 **TBD**다.

## 2. Planning admission

새 leaf Issue planning, delivery branch 생성, implementation task graph 생성은 repository가
clean일 때만 시작한다.

```text
clean = staged 변경 없음 + unstaged 변경 없음 + untracked 변경 없음
        (ignored 파일 제외)
```

source/test뿐 아니라 docs, `.hermes`, scripts, configuration, untracked 파일도 이 조건에
포함한다. PM은 existing dirty 변경을 자동 commit, stash, reset, discard하지 않는다.

### 2.1 `current-state.md` freshness

- `current-state.md`는 committed implementation의 파생 snapshot이며, platform별 task/board
  상태를 소유하지 않는다.
- implementation snapshot이 stale이고 active workflow marker가 없으면, PM은 다음 leaf를
  선택하지 않는다. PM-assigned `update-current-state` task 하나를 먼저 수행한다.
- snapshot freshness는 snapshot SHA 이후 backend/frontend source·test, build/configuration,
  Flyway migration의 committed 변경 여부로 판정한다.
- requirement, 일반 문서, `.hermes`, agent setup script의 committed 변경은 implementation
  snapshot을 stale로 만들지 않는다. 단, 미커밋 상태이면 clean admission은 통과하지 못한다.

### 2.2 `update-current-state` task

`update-current-state`는 특정 delivery Issue에 귀속하지 않는 PM maintenance task다.

```text
입력: clean inspection SHA, 이전 snapshot SHA,
      committed source/test/configuration/migration/frontend evidence
범위: docs/current-state.md만 갱신
제외: source/test/configuration/migration/requirement/ADR 변경
완료: inspection SHA·branch·사실 근거 확인,
      문서만 변경된 docs commit과 post-commit read-back
```

테스트를 새로 실행하지 않았으면 통과했다고 기록하지 않는다.

## 3. 시작·종료 recovery anchor

`current-state.md`는 implementation snapshot과 별도로 platform-independent active workflow
marker를 가진다. marker에는 tracker/board/profile ID를 쓰지 않고 repository에서 복구할 수 있는
사실만 둔다.

```text
- 상태: in-progress
- 작업 추적 참조와 계통
- delivery branch
- 시작 기준 SHA
- 시작 시점 implementation snapshot SHA
- requirement locator
- 짧은 작업 요약
```

### 3.1 정상 시작 순서

```text
clean 확인
→ current-state freshness 확인
→ stale이면 update-current-state 완료
→ root-to-leaf Issue 선택
→ base branch/base SHA와 delivery branch 확정
→ delivery branch 생성·checkout
→ active workflow marker만 current-state.md에 기록
→ marker-only docs commit 및 read-back
→ PM이 Kanban child/root graph 생성
→ validator 통과
→ 첫 child 하나만 ready
→ Coder 구현 시작
```

Kanban graph보다 marker commit을 먼저 남긴다. 따라서 Kanban 실행 기록이 유실되어도 같은
branch·기준 SHA·requirement locator를 recovery 출발점으로 사용한다.

### 3.2 종료

Issue workflow가 최종 구현·review·closure 조건을 만족한 뒤에만 최종 implementation snapshot을
조사하여 `current-state.md`를 갱신하고 active marker를 종료한다. 문서에는 문서 commit SHA가
아닌 조사한 implementation SHA를 기록한다.

### 3.3 Dirty recovery

active marker가 없는 dirty tree에서는 새 graph를 만들지 않는다.

active marker가 있고 Kanban 기록도 유실된 dirty tree의 `restart-task` 예외는 **TBD**다.
후보 방향은 PM이 marker·baseline SHA·dirty diff를 먼저 대조하고, 동일 작업과의 연결 근거가
있을 때만 하나의 restart task로 clean committed checkpoint를 복원하는 방식이다. unrelated 또는
unknown dirty 변경은 사용자 판단으로 올린다.

## 4. Issue·branch 선택

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
- target delivery branch가 이미 존재하면 새 branch를 만들지 않고, marker·Issue 상태·committed
  code를 기준으로 재개, recovery, 또는 완료 여부를 판정한다.
- 임시·test·experiment branch는 canonical delivery stack base가 아니다.
- target branch 생성 뒤 base가 전진한 경우에만 PM이 base-sync를 소유한다.

base-sync의 merge/rebase 선택, 영향 분석 format, conflict task body는 **TBD**다. 이미 공유·review된
branch는 merge 또는 blocked 판단, 아직 공유·review되지 않은 child branch는 rebase 가능이라는
방향만 합의했다. semantic conflict를 독자적으로 해결할 수 없으면 사용자 판단으로 올린다.

## 5. Requirement-to-task graph

PM은 Issue만 요약하지 않는다. requirement의 domain index·policy·model·관계·API·error·state
규칙을 committed implementation과 대조한 뒤 Coder가 실행 가능한 card contract로 투영한다.

### 5.1 Existing implementation

새 leaf Issue planning HEAD에서 requirement와 committed production source, tests, migrations,
module boundary를 대조한다.

```text
결과: satisfied | gap | unknown
```

- 구현된 듯한 behavior는 `verify-existing`으로 확인한다.
- 관련 automated test가 실제 requirement behavior를 exercise하고 현재 실행 가능하면 실행한다.
- test 파일 존재, build 성공, historical prose는 behavior satisfaction의 충분한 증거가 아니다.
- `unknown`은 구현 완료 추정이 아니라 investigation 또는 `needs-input`으로 다룬다.

### 5.2 Decomposition과 dependency

- data model/repository/migration이 필요하면 첫 child task로 만든다.
- API task는 requirement의 API 순서로 chain dependency를 둔다.
- graph에는 한 시점에 하나의 implementation child만 eligible하도록 dependency를 구성한다.
  dispatcher가 전역적으로 하나만 ready로 승격한다는 보장은 전제하지 않는다.
- Coder child task의 exact changed-file allowlist는 강제하지 않는다. PM은 HTTP,
  application, domain, error, persistence, test entry surface를 context로 제공한다.

### 5.3 Child implementation card

PM이 작성하며 다음을 포함한다.

```text
- Goal과 Scope
- literal request/response JSON
- actor/authorization
- field semantics, state/invariant, error contract
- explicit out_of_scope
- acceptance criteria와 verification 목표
- confirmed source/test/migration/module boundary locators
- provenance와 dependency
```

requirements가 명시한 `attributes` normalization, error code, authorization, transaction 범위는
PM이 결정하는 것이 아니라 contract에 누락 없이 투영한다.

## 6. Checkpoint와 aggregate review

### 6.1 Child checkpoint

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

### 6.2 Root review

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

## 7. 아직 확정하지 않은 항목

- `restart-task`의 허용 조건, owner, persisted body schema와 validator rule
- active workflow marker의 최종 Markdown field 이름과 historical 표시 방식
- current-state 갱신의 정확한 re-investigation 범위와 automation prompt/script
- base-sync의 detailed procedure, conflict report body, impact-analysis evidence
- Reviewer가 PM-authored review question 밖의 finding을 제기할 수 있는지
- structured finding과 rework draft의 final schema/script
- root review approved 뒤 final quality review, CI/PR, merge, Issue close sequence
- v4 board contract, validator regression tests, write-task Skill, capability policy의 실제 변경
- PM SOUL의 최종 identity/boundary 문안

## 8. 문서와 runtime 적용 순서

이 design을 확정한 뒤에만 다음을 적용한다.

```text
1. PM workflow Skill과 terminology reference 확정
2. board-contract-v4 및 validator regression test
3. write-task Skill 정렬
4. PM SOUL을 identity·boundary·uncertainty·reporting 중심으로 축약
5. capability/distribution manifest/README 정렬
6. fresh profile 설치·runtime native Kanban 호출·read-back 검증
```
