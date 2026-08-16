# 프로젝트 문서 인덱스

도메인 객체 간 논리 관계는 [domain-erd.md](domain-erd.md), 데이터베이스 테이블·컬럼·제약 조건은 [V1 초기 스키마](../src/main/resources/db/migration/V1__init_schema.sql)를 참고합니다.

이 문서는 필요한 컨텍스트만 선택해서 읽기 위한 문서 지도다. 모든 문서를 한 번에 읽지 않는다.

## 문서 기준과 동기화 순서

| 구분 | 기준 문서 | 역할 |
|---|---|---|
| 목표 계약 | [requirement/index.md](requirement/index.md) | P1~P12 업무 규칙, API, 상태 전이, 예외 코드 |
| 구조 원칙 | [architecture.md](architecture.md), [ADR 목록](adr/README.md) | 모듈 경계·공개 seam·되돌리기 어려운 설계 결정 |
| 논리 관계 | [domain-erd.md](domain-erd.md) | 요구사항 기준 도메인 객체와 관계 |
| 용어 기준 | [domain-glossary.md](domain-glossary.md) | 요구사항·코드에서 사용하는 용어와 상태값 |
| 현재 사실 | [current-state.md](current-state.md) | 커밋된 코드·테스트·설정·Flyway로 확인한 구현 상태 |

요구사항이 변경되면 먼저 `requirement/index.md`와 해당 도메인 문서를 갱신한 뒤, 영향을 받는 구조·관계·용어 문서를 동기화한다. 구현 완료 여부는 요구사항 문서에서 추론하지 않고 `current-state.md`와 코드 근거로 갱신한다.

## 작업별 읽기 순서

| 작업 | 먼저 읽을 문서                                         | 필요할 때 추가로 읽을 문서 |
|---|--------------------------------------------------|---|
| 현재 구현 파악 | [current-state.md](current-state.md)             | 관련 코드와 테스트 |
| 모듈 구조·의존성 변경 | [architecture.md](architecture.md)               | `docs/adr/`, 각 모듈 `package-info.java` |
| 비즈니스 규칙 구현 | [requirement/index.md](requirement/index.md)의 공통 계약 및 [도메인별 요구사항](requirement/index.md#도메인-요구사항-문서) | [domain-glossary.md](domain-glossary.md) |
| 용어·상태값 확인 | [domain-glossary.md](domain-glossary.md)         | [requirement/index.md](requirement/index.md), Flyway 마이그레이션 |
| 과거 설계 선택 이유 확인 | `docs/adr/`의 관련 ADR                              | [architecture.md](architecture.md) |
| 사용자 고민·작업 맥락 확인 | [dev-dairy.md](dev-dairy.md)의 관련 날짜              | 확정된 결과는 ADR과 현재 상태에서 재확인 |
| 작업별 로컬 스킬 선택 | [skills/index.md](skills/index.md)               | 선택한 로컬 `SKILL.md`만 전문 읽기 |
| 요구사항 문서 동기화 | [requirement/index.md](requirement/index.md) | 영향받는 [architecture.md](architecture.md), [domain-erd.md](domain-erd.md), [domain-glossary.md](domain-glossary.md), [ADR](adr/README.md) |
