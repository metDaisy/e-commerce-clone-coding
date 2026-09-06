# Agent 행동 검증 계약

## 목적

Agent가 수행한 행동을 프로젝트 규칙과 연결하고, 규칙 충족 여부를 결정론적으로 판정하기 위한 계약이다.

이 문서의 기준은 다음과 같다.

- 이벤트는 Agent 행동과 검증 시도를 추적하는 증거다.
- 실제 판정은 Checkstyle, JUnit, ArchUnit, Spring Modulith 검증, 문서 검사기가 담당한다.
- Hook은 행동을 관찰하고 필요한 검증을 안내한다. Hook이 Gradle을 직접 실행하거나 검증 결과를 임의로 판정하지 않는다.
- CI는 이벤트 로그와 무관하게 최종 품질 게이트를 수행한다.
- 검증 결과에 prompt, reasoning, terminal 명령 원문, raw tool output, credential을 포함하지 않는다.

## Rule 계약

각 규칙은 다음 필드를 가진다.

| 필드 | 의미 |
|---|---|
| `rule_id` | 변경하지 않는 안정적인 식별자 |
| `trigger` | 검증이 필요한 Agent 행동 또는 변경 |
| `scope` | 규칙이 적용되는 파일·모듈·작업 범위 |
| `validator` | 실제 사실을 판정하는 결정론적 도구 또는 테스트 |
| `evidence` | 통과를 인정하기 위한 최신 실행 증거 |
| `failure_action` | 실패·미실행 시 Agent에게 전달할 안내 |
| `enforcer` | 로컬 Hook, CI 또는 사람 review의 책임 |

검증은 마지막 코드 변경 이후에 실행되어야 한다. 이전에 통과한 결과는 새 변경이 발생하면 `stale`로 취급한다.

## 상태 모델

```text
not_required
    └─ 행동이 규칙 범위에 해당하지 않음

stale
    └─ 대상 변경이 발생했거나 이전 결과가 최신 변경보다 오래됨

not_run
    └─ 규칙은 필요하지만 최신 실행 증거가 없음

running
    └─ validator 실행이 관찰되는 중

passed ─────┐
failed ─────┴─ 다음 대상 변경 시 stale
```

`passed`는 단순히 Agent가 실행했다고 말한 상태가 아니다. 동일 session에서 대상 변경 이후 실행되었고, 규칙의 scope를 포함하며, validator가 성공을 반환한 경우에만 인정한다.

## Rule registry

### `JAVA-CHANGE-001` — Java 변경 검증 bundle

- **Trigger**: `src/main/java` 또는 `src/test/java`의 Java 파일 변경
- **Scope**: 변경된 Java 파일과 변경 모듈
- **Validator**: 독립 validator가 아니라 아래 개별 Rule을 선택하는 조정 규칙
- **Required Rules**: 기본적으로 `STYLE-JAVA-001`, production behavior 변경이면 `TEST-JAVA-001`, 구조 경로 변경이면 해당 architecture Rule
- **Evidence**: 모든 Required Rule이 같은 변경 generation에서 `passed`
- **Failure action**: 통과하지 못한 하위 Rule ID를 Agent에게 전달
- **Enforcer**: `agent-audit`의 변경 감지·`pre_verify` 안내 및 CI

### `STYLE-JAVA-001` — Java 정적 규칙

- **Trigger**: `src/main/java` 또는 `src/test/java`의 Java 파일 변경
- **Scope**: 변경된 Java 파일과 해당 source set
- **Validator**: `checkstyleMain`, `checkstyleTest`
- **현재 규칙**: `NoFullyQualifiedTypeCheck`, `NoReplacementCharacterCheck`, `SuspiciousKoreanEncodingCheck`
- **Evidence**: 변경 이후 해당 Checkstyle task의 성공 결과
- **Failure action**: 파일·라인·Checkstyle rule을 확인하도록 안내하고 수정 후 재실행
- **Enforcer**: `agent-audit`의 `pre_verify` 안내 및 CI
- **범위 제외**: Google Java Format/일반 formatter lint는 현재 보류

### `ARCH-MOD-001` — Modulith 모듈 의존성

- **Trigger**: 인식된 production module의 Java 변경. package, 공개 seam, 이벤트 또는 모듈 간 참조 변경을 path만으로 완전히 판별할 수 없으므로 보수적으로 module 전체를 대상으로 한다.
- **Scope**: `auth`, `user`, `address`, `catalog`, `seller`, `common`, `global`
- **Validator**: `ModularityTest`의 `ApplicationModules.of(Amaazon.class).verify()`
- **Evidence**: 변경 이후 `ModularityTest` 성공 결과
- **Failure action**: 위반 모듈과 의존 방향을 확인하고 허용된 공개 seam으로 수정
- **Enforcer**: Hook 안내와 CI

### `ARCH-LAYER-001` — 계층 방향

- **Trigger**: `presentation`, `application`, `domain`, `infra` package 변경
- **Scope**: 현재 `ModularityTest`가 검사하는 `auth`, `user`, `catalog`
- **Validator**: `ModularityTest`의 ArchUnit 규칙
- **Evidence**: 변경 이후 관련 구조 테스트 성공 결과
- **Failure action**: 계층 방향 위반 위치와 허용된 의존 방향을 전달
- **Enforcer**: Hook 안내와 CI
- **범위 결정**: `seller`, `global`, `common`, `address`는 Modulith 경계 검사의 대상이지만 현재 ArchUnit 계층 검사의 대상은 아니다. 이 모듈들을 계층 검증 완료로 간주하지 않는다.

### `TEST-JAVA-001` — 관련 Java 테스트

- **Trigger**: production Java behavior 또는 public contract 변경
- **Scope**: 변경 모듈의 unit, slice, integration test
- **Validator**: 변경 영향에 맞게 선택한 JUnit task
- **Evidence**: 관련 성공·거절·오류 경로 테스트의 성공 결과
- **Failure action**: 가장 작은 적절한 테스트를 추가·수정하고 재실행
- **Enforcer**: Agent workflow, Hook 안내, CI
- **주의**: 테스트 존재 여부와 시나리오 충분성은 현재 자동 판정 범위가 아니며 review가 보완한다.

### `DOC-LINK-001` — 문서 링크

- **Trigger**: `docs/` 또는 README의 링크·경로 변경
- **Scope**: 변경 문서와 내부 상대 링크
- **Validator**: 결정론적 Markdown link checker
- **Evidence**: 변경 이후 link checker 성공 결과
- **Failure action**: 깨진 링크와 실제 대상 경로를 전달
- **Enforcer**: 문서 workflow와 CI 연계 대상
- **현재 상태**: Rule registry에 예약되어 있지만 전용 link-checker callback과 CI task 연결 전까지 `agent-audit`의 자동 요구 Rule로 선택하지 않는다.

## 이벤트와 검증 결과

`agent-audit`는 다음과 같은 compact metadata를 기록한다.

```json
{
  "event": "tool_call",
  "tool": "terminal",
  "status": "ok",
  "duration_ms": 451,
  "session_id": "opaque-id",
  "turn_id": "opaque-id",
  "paths": ["src/main/java/.../UserService.java"]
}
```

검증 결과는 다음 의미를 가진다.

```json
{
  "event": "validation_trigger",
  "trigger": "JAVA-CHANGE-001",
  "required_rules": ["STYLE-JAVA-001", "TEST-JAVA-001"],
  "changed_paths": ["src/main/java/.../UserService.java"],
  "generation": 3,
  "session_id": "opaque-id"
}
```

```json
{
  "event": "validation_result",
  "validator": "codestyle",
  "status": "passed",
  "rules": ["STYLE-JAVA-001"],
  "changed_paths": ["src/main/java/.../UserService.java"],
  "generation": 3,
  "session_id": "opaque-id",
  "turn_id": "opaque-id"
}
```

구체적인 명령어·결과 원문 대신 validator category, 상태, scope와 sanitized summary만 저장한다. 이벤트 로그는 규칙을 대체하지 않으며, 검증 도구의 실제 결과와 CI가 최종 근거다.

`workflow_deviation`은 관찰 가능한 workflow 이탈을 별도로 표시한다. 현재 대상은 최신
검증의 누락·실패, 변경 경로 미확인, 그리고 직접 Gradle 실행이다. 이 이벤트에도 raw
command, raw output, prompt, credential은 기록하지 않는다.

```json
{
  "event": "workflow_deviation",
  "category": "verification",
  "reason": "missing_validation",
  "rules": ["STYLE-JAVA-001", "TEST-JAVA-001"],
  "generation": 3,
  "session_id": "opaque-id"
}
```

## 처리 흐름

```text
1. Agent가 파일·설정·문서를 변경한다.
2. post_tool_call이 변경 경로를 기록하고 관련 Rule을 stale로 만든다.
3. Agent가 gradle-mcp 또는 프로젝트 검증기를 실행한다.
4. post_tool_call이 validator 실행과 결과를 관찰한다.
5. pre_verify가 최신 passed 증거가 없는 Rule을 Agent에게 안내한다.
6. 누락·실패·도구 정책 이탈이면 `workflow_deviation`을 추가 기록하고, 실패하면
   sanitized summary와 수정·재검증 지침을 전달한다.
7. CI가 동일한 품질 게이트를 독립적으로 최종 판정한다.
```

Hook은 기본적으로 fail-open이다. 로그 기록 실패가 코딩 작업을 중단시키지는 않는다. 품질 실패는 validator와 CI가 판정한다.

## 구현 순서

1. `STYLE-JAVA-001`의 Checkstyle 결과 상관관계를 완성한다.
2. 여러 Rule을 동시에 추적할 수 있도록 `agent-audit` 상태를 확장한다.
3. Modulith·ArchUnit·관련 테스트 결과를 Rule ID별로 연결한다.
4. 실제 편집 세션에서 `pre_verify` 수정·재검증 loop를 검증한다.
5. 문서 link checker와 포트폴리오용 Skill Map을 연결한다.
