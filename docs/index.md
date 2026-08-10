# 프로젝트 문서 인덱스

도메인 객체 간 논리 관계는 [domain-erd.md](domain-erd.md), 데이터베이스 테이블·컬럼·외래 키는 [README.md](../README.md)의 기본 스키마를 참고합니다.

이 문서는 필요한 컨텍스트만 선택해서 읽기 위한 문서 지도다. 모든 문서를 한 번에 읽지 않는다.

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
