# 프로젝트 문서 지도

이 문서는 `docs/`의 문서 역할과 읽기 경로를 안내하는 카탈로그다. Hermes가 자동 주입하는 규칙 파일이 아니며 업무 규칙·API·구현 상태를 직접 정의하지 않는다.

## 기준 문서와 책임

| 영역 | 기준 문서 | 책임 |
|---|---|---|
| 저장소 강제 규칙 | [`../AGENTS.md`](../AGENTS.md) | 조사·변경·검증의 공통 guardrail과 기준 문서 진입점 |
| 목표 기능 계약 | [`requirement/index.md`](requirement/index.md) | P1~P12 요구사항, 공통 API contract와 도메인 문서 링크 |
| 장기 구조 원칙 | [`architecture.md`](architecture.md) | module 경계, 계층 의존 방향, 통신·data 원칙 |
| Backend 구현 convention | [`backend-development-guide.md`](backend-development-guide.md) | Java/Spring/Modulith code 배치, transaction, DTO·mapping, port·adapter, persistence 규칙 |
| Backend test convention | [`backend-test-guide.md`](backend-test-guide.md) | test level 선택, JUnit·Mockito·Spring test, assertion과 검증 규칙 |
| 설계 결정 이유 | [`adr/README.md`](adr/README.md) | 구조 결정의 배경, 대안, 결과와 예외 |
| 논리 도메인 관계 | [`domain-erd.md`](domain-erd.md) | SQL과 구분되는 domain object와 논리 관계 |
| 용어·상태 기준 | [`domain-glossary.md`](domain-glossary.md) | 요구사항과 code가 공유하는 용어·상태 의미 |
| 과거 구현 스냅샷 | [`current-state.md`](current-state.md) | 특정 Git SHA에서 확인한 파생 기록; 현재 source of truth가 아님 |
| Git 커밋 규칙 | [`commit-message-convention.md`](commit-message-convention.md) | 논리적 commit 단위와 message 형식 |
| DB 물리 schema | `src/main/resources/db/migration/` | table, column, constraint와 실제 저장 구조 |

Hermes Profile·Skill·plugin의 실행 계약은 `docs/`가 아니라 해당 `.hermes/` source 옆에서 관리한다. Profile 설치·capability는 `.hermes/README.md`와 각 Profile README, Coder lifecycle은 Profile Skill, `agent-audit` behavior는 `.hermes/plugins/agent-audit/README.md`가 소유한다.

## 문서 간 해석 규칙

1. 현재 동작은 checkout된 code, test, configuration과 Flyway migration으로 판단한다.
2. 목표 동작과 API contract는 `requirement/`가 소유한다.
3. 안정적인 구조는 `architecture.md`, 구현 방법은 `backend-development-guide.md`, test 방법은 `backend-test-guide.md`가 소유한다.
4. `package-info.java`, `build.gradle`, Checkstyle와 migration 같은 executable source가 설명 문서와 다르면 source를 우선하고 불일치를 보고한다.
5. ADR은 선택의 이유와 예외를 설명하며 현재 구조·구현 inventory를 대신하지 않는다.
6. `current-state.md`는 기록된 SHA의 snapshot이다. 현재 branch의 구현 범위를 판단할 때 먼저 사용하지 않는다.
7. `domain-glossary.md`는 용어 의미, `domain-erd.md`는 논리 관계, migration은 물리 schema의 기준이다.

## 작업별 최소 읽기 경로

| 작업 | 먼저 읽을 문서·source | 필요할 때 추가 |
|---|---|---|
| Backend 구현·bug fix·refactor | `AGENTS.md`, [`backend-development-guide.md`](backend-development-guide.md) | 관련 requirement, 인접 code·test |
| Backend test 작성·검증 | [`backend-test-guide.md`](backend-test-guide.md) | `build.gradle`, 인접 test와 fixture |
| Module·event·의존성 변경 | [`architecture.md`](architecture.md), 관련 `package-info.java` | backend guide, ADR, `ModularityTest` |
| API·업무 behavior 변경 | [`requirement/index.md`](requirement/index.md)와 해당 P 문서 | glossary, backend guide·test guide |
| DB·JPA·migration 변경 | 관련 migration, entity·repository contract | backend guide·test guide, requirement, architecture |
| 용어·상태·관계 변경 | [`domain-glossary.md`](domain-glossary.md) | ERD, requirement, ADR |
| Hermes Profile·Skill 변경 | 해당 `.hermes/profiles/**/README.md`, `SOUL.md`, Skill | distribution과 capability policy |
| `agent-audit` plugin 변경 | [plugin README](../.hermes/plugins/agent-audit/README.md), 구현·test | `.hermes/README.md`, Profile policy |
| 커밋 메시지 작성 | [`commit-message-convention.md`](commit-message-convention.md) | `git diff --cached` |

문서 구조를 바꾸면 이 지도와 모든 repository link를 확인한다. 새 요구사항은 `requirement/index.md`, 새 ADR은 `adr/README.md`에 등록한다. Code에서 직접 확인할 수 있는 current inventory를 설명 문서에 복제하지 않는다.
