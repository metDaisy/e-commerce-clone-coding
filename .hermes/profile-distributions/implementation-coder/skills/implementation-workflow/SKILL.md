---
name: implementation-workflow
description: Use when implementation-coder receives an assigned Amaazon backend Kanban task and must implement, test, verify, and hand it to Reviewer A.
version: 0.1.0
author: "Amaazon project"
license: MIT
platforms: [linux, macos, windows]
metadata:
  hermes:
    tags: [implementation, backend, testing, kanban, review-handoff]
    related_skills: [codebase-memory-mcp, semble-search, java-springboot, java-junit]
---

# Implementation Workflow

이 Skill은 `implementation-coder`의 실행 절차다. 역할·권한 경계는 Profile의 `SOUL.md`,
repository 공통 규칙은 `AGENTS.md`, 상세 테스트 규칙은 `docs/testing-guide.md`,
Kanban task schema와 acceptance gate는 Project Manager의 board contract가 소유한다.
중복 규칙을 새로 정의하지 말고 해당 source를 읽어 적용한다.

## 1. Task preflight와 claim

1. Kanban worker 표면에서 현재 Profile에 assigned된 task를 읽는다.
2. task envelope를 read-back하여 task가 `implementation`이고 현재 Profile에 assigned이며
   `ready`인지 확인한다. PM이 graph의 executable ready leaf를 하나만 만든다는 전제를 따른다.
3. 조건이 맞지 않으면 task를 임의 선택·상태 변경·재할당하지 않고 PM에 보고한다.
4. native claim으로 `ready → running`을 만든 뒤에만 파일을 수정한다.
5. task의 goal, scope, out_of_scope, requirements source, dependency, workspace, 기존
   verification, review handoff contract를 읽는다. `CHECK/EXPECT`는 acceptance gate의
   참고 정보로 읽되 최종 pass/fail을 선언하지 않는다.

현재 repository 규칙이 작업 전 Git 상태 확인을 요구하는 동안에는 read-only로 branch,
HEAD, status와 변경 경로를 확인한다. commit, push, merge, rebase, reset, clean, stash는
수행하지 않는다. PM이 지정한 workspace 밖의 파일은 수정하지 않는다.

## 2. 요구사항·코드·영향 조사

1. `docs/requirement/index.md`와 task가 지정한 요구사항에서 목표·예외·상태 전이를 읽는다.
2. `docs/current-state.md`, 관련 production code, 기존 test, configuration, migration,
   `package-info.java`와 ADR을 필요한 범위에서 확인한다.
3. 구현 위치나 관련 동작을 모르면 `semble-search`를 좁게 사용하고 실제 파일을 읽는다.
4. 현재 domain 안에서 끝나는 기능은 local source/test 탐색만으로 진행할 수 있다.
5. 다른 domain의 public API caller/callee·dependency·impact가 필요할 때만
   `codebase-memory-mcp`를 사용한다.
   - 가능한 경우 `list_projects`로 정확한 project를 확인한다.
   - `index_status` 또는 `detect_changes`로 freshness를 확인한다.
   - known symbol은 `search_graph`, 영향은 `trace_path`, unfamiliar subsystem은
     `get_architecture`를 사용한다.
   - graph 결과는 `get_code_snippet`와 실제 source/test/package-info로 대조한다.
   - graph가 stale하거나 MCP가 unavailable이면 local search로 fallback하고 한계를 기록한다.
   - repository 재색인·project 삭제·trace ingest는 승인 없이 하지 않는다.
6. 타 domain API 변경으로 기존 consumer의 compile/test가 깨지면 기존 의미를 보존하는
   필수 DTO·mapping·호출부·adapter propagation까지 같은 task에서 수정한다. 영향 경로와
   변경 파일을 comment에 기록한다.
7. 새로운 business semantics, authorization, transaction/consistency, error policy 또는
   새로운 cross-domain public contract를 결정해야 하면 추측하지 않고 `blocked`로 기록한다.
   필요한 결정과 owner를 명시한 뒤 종료한다.

## 3. 구현과 테스트

1. 요구사항에 필요한 production code와 관련 test code를 함께 변경한다.
2. 새 production behavior에는 `docs/testing-guide.md`에 따라 success와 rejection/failure
   path를 포함하는 가장 작은 적절한 unit, slice, repository 또는 integration test를 둔다.
3. Given-When-Then, Korean `@DisplayName`, BDDMockito, fresh persistence query,
   query-count와 모듈 경계 규칙 등 해당 guide의 세부 조건을 따른다.
4. task의 정확성을 위해 필요한 국소 개선과 필수 propagation은 능동적으로 수행한다.
   변경 영역 밖의 broad architecture·Modulith·deep seam refactor는 수행하지 않고
   residual risk로 기록한다.
5. 구현 중 발견한 baseline compile/test 실패는 원인을 확인하고, workflow를 진행하는 데
   필요한 범위에서 먼저 해결한다. 요구사항 밖의 보정이 있었다면 이유·변경 경로·영향을
   comment에 기록한다. 해결할 수 없는 환경·외부 의존성·미결정 정책은 `blocked`다.
6. reviewer가 명시적으로 요청하지 않은 acceptance criteria·dependency·assignee·task
   body는 수정하지 않는다.

## 4. 결정론적 검증 순서

세 단계는 순서를 바꾸지 않는다. 모든 Gradle 작업은 반드시 `gradle-mcp`로 실행하고,
terminal에서 `gradle`, `gradlew`, `gradlew.bat`를 직접 실행하지 않는다.

### 1차 — 전체 compile과 Checkstyle

변경된 backend source set 전체를 compile하고 프로젝트 Checkstyle을 실행한다.
일반적인 Gradle 요청은 다음 task를 포함한다.

```text
runner=gradle-mcp; tasks=:compileJava,:compileTestJava,:checkstyleMain,:checkstyleTest
```

실제 task graph와 project path는 `gradle-mcp`의 build metadata로 확인한다.

### 2차 — 변경 테스트

이번 task에서 작성·수정한 테스트만 정확한 Java FQCN으로 실행한다.

```text
runner=gradle-mcp; tasks=:test; tests=<changed-test-FQCNs>
```

`any(...)`처럼 대상을 모호하게 지정하지 않는다. 새 테스트가 없다면 production
behavior 변경 여부를 재확인하고, 필요한 테스트가 누락된 경우 보완한다.

### 3차 — 전체 build

1차·2차가 성공한 뒤 전체 build를 실행한다.

```text
runner=gradle-mcp; tasks=:build
```

각 단계가 실패하면 원인을 분석하고 task 변경으로 해결 가능한 문제는 수정한 뒤
1차부터 전체 순서를 다시 실행한다. 실패한 상태를 성공으로 보고하거나 검증 범위를
줄이지 않는다.

## 5. Review handoff

세 단계가 모두 성공하면 다음을 간결한 한국어 Kanban comment에 기록한다.

- task/run 식별자
- 구현 요약
- 변경 경로
- 1차·2차·3차 실제 결과
- 요구사항 구현 중 발생한 필수 propagation 또는 baseline 보정
- residual risk 또는 없음
- `CHECK/EXPECT` acceptance gate는 Reviewer A가 판정한다는 사실

그 다음 task에 선언된 Reviewer A를 명시하여 native `kanban_request_review`를 호출한다.
직접 `review` 상태·assignee를 변경하거나 review child를 만들지 않는다. native handoff
metadata에는 가능한 범위에서 다음 자체 검증 사실만 기록한다.

```text
verified_sha: read-only HEAD
validator: implementation-workflow
check_id: compile-checkstyle, changed-tests, full-build
result: pass
changed_paths: 실제 변경 경로
residual_risk: 관찰된 위험 또는 none
```

`CHECK/EXPECT` pass/fail이나 Reviewer A의 승인 결과를 자체 metadata로 주장하지 않는다.
`request-review`가 실패하면 수동으로 상태를 고치지 말고 handoff 실패 원인과 재개 조건을
기록한 뒤 `blocked`로 라우팅한다.

## 6. Reviewer feedback과 재작업

Reviewer A의 수정 요청이 native same-card lifecycle로 원래 Profile에 돌아오면:

1. feedback의 path/line 또는 symbol, 관찰된 문제, 기대 결과, 검증 방법, 변경 범위를 읽는다.
2. 형식이 추상적이면 임의로 해석하지 않는다. 어떤 대상·문제·기대 결과·범위가 부족한지
   정형화된 요청으로 Reviewer에게 되돌린다.
3. 구체적인 feedback이면 같은 task에서 수정한다.
4. 수정 후 1차 compile/checkstyle → 2차 변경 테스트 → 3차 전체 build를 처음부터 반복한다.
5. 다시 구현 완료 comment와 native `request-review`를 남긴다.

Reviewer B의 broad quality finding은 이 Profile이 임의로 기존 task에 섞지 않는다. PM이
별도 improvement/refactor task로 라우팅한 경우에만 그 task의 명시된 scope를 수행한다.

## 7. Blocked 처리

다음 경우에는 추측하거나 task contract를 고치지 않는다.

- 요구사항 또는 cross-domain semantics가 정의되지 않음
- 환경·외부 의존성 때문에 검증을 완료할 수 없음
- native Kanban handoff가 실패함
- 필요한 reviewer·workspace·task precondition이 없음

`blocked` comment에는 막힌 단계, 관찰된 사실, 원인, 영향, 재개에 필요한 조치를 기록한다.
사용자 또는 PM이 원인을 해결해 task를 다시 assigned + ready로 만든 뒤, 새 run에서
preflight부터 전체 workflow를 재시작한다.

## 완료 조건

다음 조건을 모두 만족할 때만 구현 완료로 보고한다.

- 요구사항에 맞는 production code와 관련 test가 함께 변경됨
- 1차 compile/Checkstyle, 2차 변경 테스트, 3차 전체 build가 실제 통과함
- 구현 comment와 자체 검증 metadata가 기록됨
- native `kanban_request_review`가 성공함
- task가 `review`이고 Reviewer A에게 전달되었음을 read-back함

Coder는 `done`을 선언하지 않는다. 최종 acceptance와 `done` 전환은 Reviewer A와 native
Kanban lifecycle의 책임이다.
