# 프로젝트 문서 인덱스

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

## 신뢰도와 최신성

1. 현재 동작은 코드, 테스트, Flyway 마이그레이션이 기준이다.
2. 목표 동작은 `requirement/index.md`와 링크된 도메인별 요구사항 문서가 기준이다.
3. `current-state.md`는 상단의 Git SHA와 현재 HEAD가 같을 때만 현재 상태로 간주한다.
4. 서로 충돌하는 상태값이나 규칙은 임의로 선택하지 않고 `current-state.md`의 충돌 목록에 기록한 뒤 결정한다.

## 문서 유지 규칙

- 문서는 코드가 표현하지 못하는 목적, 책임, 불변조건, 선택 이유를 기록한다.
- 클래스·메서드 목록, 생성 가능한 API 표, 구현 코드를 그대로 복제하지 않는다.
- 현재 상태를 갱신할 때 확인 날짜와 Git SHA를 함께 변경한다.
- 구조적 결정은 개발 일지에 누적하지 않고 ADR로 분리한다.
- 개발 일지는 원문 저널이므로 사용자가 요청한 경우에만 수정한다.
- `current-state.md`는 커밋된 HEAD 기준으로 갱신하며 깨끗하지 않은 작업 트리를 완료 상태에 포함하지 않는다.
