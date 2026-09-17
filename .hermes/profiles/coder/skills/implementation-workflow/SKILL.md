---
name: implementation-workflow
description: Use when implementing an assigned backend Kanban task.
version: 0.1.0
author: "metDaisy"
license: MIT
platforms: [linux, macos, windows]
metadata:
  hermes:
    tags: [implementation, backend, testing, kanban, review-handoff]
    related_skills: [codebase-memory-mcp, semble-search, java-springboot, java-junit]
---

# Implementation Workflow

이 Skill은 `implementation-coder`가 assigned backend task를 구현할 때 필요한
실행·협업 절차다. 테스트 세부 규칙은 `docs/testing-guide.md`, task schema와
acceptance gate는 Project Manager의 board contract가 소유한다. 다른 Profile의
숨은 Memory·대화·추측은 입력으로 사용하지 않고, task·diff·source·test·검증 evidence만
사용한다. 각 source of truth를 다시 정의하지 말고 필요한 범위에서 읽어 적용한다.

## When to Use

- 현재 Profile에 assigned된 `implementation` Kanban task를 구현·테스트·검증할 때 사용한다.
- Reviewer A가 같은 task로 구체적인 changes-requested feedback을 돌려보냈을 때도 사용한다.
- PM의 task contract가 없거나 단순 review, broad refactor, task graph authoring만 필요한 경우에는 사용하지 않는다.

## Procedure

### 1. Preflight와 claim

1. Kanban worker 표면에서 현재 Profile에 assigned된 task를 읽는다.
2. task envelope를 read-back하여 type이 `implementation`이고, 현재 Profile에 assigned이며,
   status가 `ready`이고, workspace와 dependency가 유효한지 확인한다.
3. 조건이 맞지 않으면 task를 임의 선택·재할당·수정하지 않고 PM에 보고한다.
4. 공식 native claim으로만 `ready → running`을 만든 뒤 파일을 수정한다. status와 assignee를
   수동으로 바꾸지 않는다.
5. goal, scope, out_of_scope, requirements source, dependency, workspace, 기존 verification,
   reviewer와 handoff contract를 읽는다. `CHECK/EXPECT`는 참고 정보로만 읽고 acceptance
   pass/fail은 선언하지 않는다.
6. branch, HEAD, status와 변경 경로를 read-only로 확인한다. 기존 staged·unstaged·untracked
   변경을 보존하고 PM이 지정한 workspace 밖은 수정하지 않는다.
7. commit, push, merge, rebase, reset, clean, stash와 다른 release 작업은 수행하지 않는다.

### 2. 요구사항·코드·영향 조사

1. `docs/requirement/index.md`와 task가 지정한 요구사항에서 목표, 예외와 상태 전이를 확인한다.
2. `docs/current-state.md`, 관련 production code/test, configuration, migration,
   `package-info.java`와 ADR을 필요한 범위에서 읽는다.
3. 구현 위치가 불명확하면 `semble-search`를 좁게 사용한 뒤 반환된 실제 파일과 테스트를 읽는다.
4. 다른 domain의 caller/callee, public API 또는 dependency 영향이 필요할 때만
   `codebase-memory-mcp`를 사용한다. 가능하면 project를 확인하고 index freshness를 점검하며,
   graph 결과를 실제 source·test·package-info와 대조한다.
5. graph가 stale하거나 unavailable이면 local search로 fallback하고 그 한계를 기록한다.
   repository 재색인, project 삭제, trace ingest는 승인 없이 수행하지 않는다.
6. API 변경 때문에 기존 consumer의 compile/test가 깨지면 기존 의미를 보존하는 DTO, mapping,
   caller와 adapter propagation만 같은 task에서 수정하고 영향 경로를 comment에 기록한다.
7. 새로운 business semantics, authorization, transaction/consistency, error policy 또는
   cross-domain public contract가 필요하면 추측하지 않는다. 필요한 결정과 owner를 적고
   native `kanban_block`으로 중단한다.

### 3. 구현과 테스트

1. 요구사항에 필요한 production code와 관련 test code를 함께 변경한다.
2. 새 behavior에는 `docs/testing-guide.md`에 따라 success와 필요한 rejection/failure path를
   포함하는 가장 작은 적절한 unit, slice, repository 또는 integration test를 둔다.
3. 해당 guide의 Given-When-Then, Korean `@DisplayName`, BDDMockito, fresh persistence query,
   query-count와 모듈 경계 규칙을 따른다.
4. 기존 public API, DB schema, event contract와 module boundary를 보존한다. 변경의 정확성에
   필요한 consumer propagation과 국소 수정만 수행한다. broad architecture, Modulith 또는
   deep seam refactor는 residual risk로 남긴다.
5. baseline compile/test 실패는 원인과 영향 범위를 확인한다. task 진행에 필수적인 보정만 하고,
   요구사항 밖의 보정은 이유·경로·영향을 기록한다.
6. 해결할 수 없는 환경·외부 dependency·미결정 정책은 `blocked`로 처리한다. reviewer가
   요청하지 않은 acceptance criteria, dependency, assignee와 task body는 수정하지 않는다.

### 4. 결정론적 검증

모든 Gradle 작업은 반드시 `gradle-mcp`로 실행한다. terminal에서 `gradle`, `gradlew`,
`gradlew.bat`를 직접 실행하지 않는다. 세 단계의 순서를 바꾸거나 실패 후 검증 범위를 줄이지 않는다.

1. **Compile/Checkstyle:** 변경된 backend source set 전체를 compile하고 다음 범위를 실행한다.
   ```text
   runner=gradle-mcp; tasks=:compileJava,:compileTestJava,:checkstyleMain,:checkstyleTest
   ```
   실제 project path와 task graph는 gradle-mcp metadata로 확인한다.
2. **Changed tests:** 이번 task에서 작성·수정한 테스트만 정확한 Java FQCN으로 실행한다.
   ```text
   runner=gradle-mcp; tasks=:test; tests=<changed-test-FQCNs>
   ```
   `any(...)`처럼 모호한 대상을 사용하지 않는다. 새 test가 없으면 behavior 변경과 test 누락을
   재확인한다.
3. **Full build:** 앞의 두 단계가 성공한 뒤 전체 build를 실행한다.
   ```text
   runner=gradle-mcp; tasks=:build
   ```

단계가 실패하면 원인을 분석해 task 범위에서 수정하고 1단계부터 전체 순서를 반복한다.
실행하지 못한 검증이나 실패한 검증을 성공으로 보고하지 않는다.

### 5. Review handoff

세 검증 단계가 모두 성공하면 간결한 한국어 Kanban comment를 남긴다.

- task/run 식별자와 구현 요약
- 실제 변경 경로
- compile/checkstyle, changed tests, full build의 실제 결과
- 필수 propagation 또는 baseline 보정과 그 이유
- residual risk 또는 `none`
- `CHECK/EXPECT` acceptance는 Reviewer A가 판정한다는 사실

task contract에 선언된 Reviewer A를 대상으로 native `kanban_request_review`를 호출한다.
review status·assignee를 수동 변경하거나 review child task를 만들지 않는다.
handoff metadata에는 다음과 같은 자체 검증 사실만 기록한다.

```text
verified_sha: read-only HEAD
validator: implementation-workflow
check_id: compile-checkstyle, changed-tests, full-build
result: pass
changed_paths: 실제 변경 경로
residual_risk: 관찰된 위험 또는 none
```

여기서 `result: pass`는 세 가지 deterministic verification만 의미하며,
`CHECK/EXPECT` pass나 Reviewer A의 승인을 의미하지 않는다.
handoff가 실패하면 원인과 재개 조건을 comment로 남기고 공식 native `kanban_block`으로 중단한다.

### 6. Reviewer feedback 재작업

1. same-card lifecycle로 돌아온 feedback의 path/line 또는 symbol, 문제, 기대 결과, 검증 방법과
   변경 범위를 읽는다.
2. 대상·문제·기대 결과·검증 방법·범위가 부족하면 임의 해석하지 말고 다음 형식으로
   Reviewer에게 clarification을 요청한 뒤 중단한다.
   ```text
   feedback_clarification:
   missing: path_or_symbol | observed_problem | expected_result | verification | scope
   reason: <현재 feedback으로 구현 범위를 결정할 수 없는 이유>
   requested_reviewer_action: <보완할 구체 정보>
   ```
3. 구체적인 feedback만 같은 task에서 수정한다.
4. 수정 후 compile/checkstyle → changed tests → full build를 처음부터 반복한다.
5. 구현 comment를 갱신하고 다시 native `kanban_request_review`를 호출한다.

Reviewer B의 broad quality finding은 PM이 별도 improvement/refactor task로 라우팅한 경우에만
그 task의 명시된 scope에서 수행한다.

## Pitfalls

- assigned task가 없거나 precondition이 맞지 않음: 임의 task를 고르지 말고 PM에 보고한다.
- 기존 변경 경로와 충돌함: 기존 변경을 덮어쓰지 않고 blocked 또는 needs-input으로 보고한다.
- 요구사항·cross-domain 의미가 정의되지 않음: business policy를 발명하지 말고 결정 owner와 함께 block한다.
- graph/MCP가 stale 또는 unavailable함: local source/test 탐색으로 fallback하고 한계를 기록한다.
- 환경·외부 dependency 때문에 검증할 수 없음: 미실행 검증을 명시하고 block한다.
- Reviewer feedback이 추상적임: 부족한 입력을 특정하여 clarification을 요청한다.
- broad architecture/refactor 요구가 섞임: 기존 task에 섞지 않고 PM의 별도 task를 기다린다.
- acceptance gate와 deterministic verification을 혼동함: 자체 검증 결과만 pass로 기록하고 acceptance는 Reviewer에게 남긴다.

## Verification

다음 조건을 모두 만족해야 `review-handoff`로 보고한다.

- 요구사항에 맞는 production code와 관련 test가 함께 변경되었다.
- compile/checkstyle, changed tests, full build가 gradle-mcp로 실제 통과했다.
- 구현 comment와 자체 검증 metadata가 기록되었다.
- native `kanban_request_review`가 성공했다.
- task가 `review` 상태이고 지정 Reviewer에게 전달되었음을 read-back했다.

최종 acceptance와 `done` 전환은 Reviewer A와 native Kanban lifecycle의 책임이다. Coder는
`done`을 스스로 선언하지 않는다.

최종 보고는 다음 형식을 사용한다.

```text
status: review-handoff | blocked
task/run:
implementation:
changed_paths:
verification:
propagation_or_baseline_fix:
residual_risk:
reviewer:
readback:
blocker_or_next_action:
```
