# Project Skill Map

이 문서는 개인 환경의 Skill 설치 목록이나 경로를 기록하지 않는다. 이 프로젝트에서 작업 유형에 따라 어떤 Skill을 선택하고, 어떤 tool과 결정론적 validator를 연결하는지 설명하는 포트폴리오용 지도다.

## 선택 규칙

1. 먼저 `AGENTS.md`, `docs/index.md`, 작업에 해당하는 기준 문서와 코드를 확인한다.
2. 작업을 직접 설명하는 최소 Skill만 선택한다.
3. 선택한 Skill은 `skill_view`로 전문을 읽고, 현재 프로젝트 규칙과 충돌하는 부분은 프로젝트 규칙을 따른다.
4. Skill은 절차와 탐색을 보완할 뿐, 코드·테스트·설정·공식 문서가 프로젝트 사실의 원본이다.
5. Skill lifecycle event에는 이름·action·provenance·opaque correlation ID만 남기며 prompt나 reasoning은 저장하지 않는다.

## 작업 유형별 Map

| 작업 유형 | 대표 Skill | 대표 tool | 결정론적 validator |
|---|---|---|---|
| Spring Boot 계층·transaction·DI | `java-springboot` | `read_file`, `search_files`, `patch` | 관련 JUnit/integration test, Checkstyle |
| Modulith 모듈·공개 seam·이벤트 | `305-frameworks-spring-boot-modulith` | `read_file`, `search_files`, `mcp__gradle_mcp__gradle_mcp__gradle` | `ApplicationModules.verify()`, `ModularityTest` |
| JUnit 5 테스트 설계 | `java-junit` | `read_file`, `search_files`, `patch` | 관련 test task, assertion 결과 |
| 계층·port·adapter 설계 | `clean-architecture`, `codebase-design` | `search_files`, `read_file`, `patch` | ArchUnit, Modulith, 관련 test |
| 도메인 규칙·상태·용어 | `domain-modeling` | `read_file`, `search_files`, `patch` | 도메인 test, 요구사항·glossary 대조 |
| Java 리팩터링 | `java-refactoring-extract-method`, `java-refactoring-remove-parameter` | `search_files`, `patch` | 호출자 컴파일, 관련 test, Checkstyle |
| React·TypeScript UI | `frontend-design` | `read_file`, `search_files`, `patch` | `npm run lint`, `npm run build` |
| 브라우저 QA | `agent-browser` | Browser Use CLI | 재현 시나리오와 화면 결과 |
| Harness·Hermes plugin | `hermes-agent`, `hermes-desktop-plugin-engineering` | `skill_view`, `read_file`, `write_file`, `terminal` | Plugin Doctor, runtime discovery, callback/runtime test |
| PM task graph authoring | `build-task-graph` | `read_file`, `clarify`, `code_execution`, `kanban` | versioned draft validator/fixture regression test, manual native create/read-back, Review finding rework routing |
| 변경 review | `code-review` | `git diff`, `read_file`, `search_files` | diff 검토, 관련 validator, CI |

## Harness 연결

```text
작업 유형
  → 최소 Skill 선택
  → Skill/tool lifecycle metadata 기록
  → 변경 경로로 Rule ID 선택
  → 실제 validator 실행 결과 관찰
  → pre_verify nudge 또는 CI 품질 게이트
```

`agent-audit`는 Skill을 대신 선택하거나 validator를 대신 실행하지 않는다. 다음 metadata만 연결한다.

- `skill_lifecycle`: Skill 이름, lifecycle action, provenance, 재사용 여부
- `tool_call`: tool 이름, 성공 여부, duration, 안전한 project-relative path
- `validation_trigger`: trigger Rule과 required Rule ID
- `validation_result`: validator category, status, matched Rule ID, generation
- `verification_gate`: missing/failed Rule ID와 nudge 필요 여부

## 공개하지 않는 정보

포트폴리오 문서와 `.hermes/events.jsonl`에는 다음을 저장하지 않는다.

- prompt·대화 전문·reasoning
- terminal command와 raw tool argument/result
- 비밀번호·token·API key·credential
- 개인 절대 경로와 민감한 세션 데이터

결정론적 판정은 Skill Map이나 이벤트 로그가 아니라 실제 validator와 CI가 담당한다. Rule ID와 상태 계약은 [`../validator-contract.md`](../validator-contract.md)를 따른다.
