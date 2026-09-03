# 프로젝트 문서 지도

이 문서는 `docs/`의 문서 역할과 읽기 경로를 안내하는 **카탈로그**다. Hermes가 자동 주입하는 규칙 파일이 아니며, 이 문서 자체가 업무 규칙·API·구현 상태를 정의하지 않는다. 필요한 문서만 선택해서 읽는다.

## 기준 문서와 책임

| 영역 | 기준 문서 | 이 문서가 정의하는 것 |
|---|---|---|
| Agent 프로젝트 규칙 | [`../AGENTS.md`](../AGENTS.md) | 반드시 지킬 저장소 규칙과 기준 문서 진입점 |
| Agent 작업 절차 | [`agent-workflow.md`](agent-workflow.md) | 조사·도구 선택·수정·검증·보고 순서 |
| 목표 기능 계약 | [`requirement/index.md`](requirement/index.md) | P1~P12 요구사항, 공통 API 계약, 도메인 문서 링크 |
| 장기 구조 원칙 | [`architecture.md`](architecture.md) | 모듈 경계·공개 seam·의존 방향 |
| 설계 결정 이유 | [`adr/README.md`](adr/README.md) | ADR 목록과 결정의 배경·대안·결과 |
| 논리 도메인 관계 | [`domain-erd.md`](domain-erd.md) | SQL과 구분되는 도메인 객체·논리 관계 시각화 |
| 용어·상태 기준 | [`domain-glossary.md`](domain-glossary.md) | 요구사항과 코드에서 공유하는 용어·상태 의미 |
| 현재 구현 사실 | [`current-state.md`](current-state.md) | 특정 Git 기준으로 확인한 코드·테스트·설정·Flyway 상태 |
| 테스트 절차 | [`testing-guide.md`](testing-guide.md) | 테스트 종류 선택, 작성 규칙, assertion 규칙 |
| Agent 행동 검증 계약 | [`validator-contract.md`](validator-contract.md) | 행동별 Rule ID, 결정론적 validator, Hook·CI 책임 |
| Project Skill Map | [`skills/index.md`](skills/index.md) | 작업 유형별 Skill·tool·validator 선택과 공개 범위 |
| Git 커밋 규칙 | [`commit-message-convention.md`](commit-message-convention.md) | 논리적 커밋 단위와 메시지 형식 |
| DB 물리 스키마 | [`V1__init_schema.sql`](../src/main/resources/db/migration/V1__init_schema.sql) 및 후속 migration | 테이블·컬럼·제약 조건·실제 저장 구조 |

## 문서 간 해석 규칙

- 현재 동작은 코드·테스트·설정·Flyway를 기준으로 판단한다. 문서의 목표 계약만으로 구현 완료를 판단하지 않는다.
- 목표 동작과 API 계약은 `requirement/`이 기준이다. 도메인 문서는 해당 도메인 규칙만 보충한다.
- 구조 원칙은 `architecture.md`, 특정 선택의 이유와 대안은 관련 ADR이 기준이다. ADR은 현재 구조 설명을 대신하지 않는다.
- `domain-glossary.md`는 용어 의미의 단일 기준이다. ERD는 관계를 보여 주지만 필드 규칙·API 계약·구현 상태의 원본이 아니다.
- SQL migration은 물리 스키마의 원본이다. `domain-erd.md`와 일치하지 않을 수 있으며, 그 차이는 현재 상태와 설계 문서에서 설명한다.
- 문서와 코드가 충돌하면 충돌 범위와 근거를 보고한다. 추측으로 한쪽을 조용히 덮어쓰지 않는다.

## 작업별 최소 읽기 경로

| 작업 | 먼저 읽을 문서 | 필요할 때 추가 |
|---|---|---|
| 모든 코드·문서 작업 | [`agent-workflow.md`](agent-workflow.md) | 아래 작업별 기준 문서 |
| 현재 구현 파악 | [`current-state.md`](current-state.md) | 관련 코드·테스트·migration |
| 요구사항 구현·API 변경 | [`requirement/index.md`](requirement/index.md) | 해당 P 문서, glossary, current-state |
| 모듈·이벤트·의존성 변경 | [`architecture.md`](architecture.md) | 관련 ADR, `package-info.java`, 테스트 |
| 용어·상태·관계 변경 | [`domain-glossary.md`](domain-glossary.md) | `domain-erd.md`, 관련 requirement·ADR |
| DB·JPA·migration 변경 | 관련 migration과 [`current-state.md`](current-state.md) | requirement, architecture, testing-guide |
| 테스트 작성·검증 | [`testing-guide.md`](testing-guide.md) | 관련 코드·테스트 |
| validator·hook·CI 변경 | [`validator-contract.md`](validator-contract.md) | `agent-audit`, 실제 validator, CI workflow |
| 커밋 메시지 작성 | [`commit-message-convention.md`](commit-message-convention.md) | `git diff --cached` |

문서 구조를 바꾸면 이 지도와 README의 링크를 확인한다. 새 요구사항은 `requirement/index.md`, 새 ADR은 `adr/README.md`에 등록한다. 구현 상태는 검증된 Git 기준으로만 `current-state.md`에 기록한다.
