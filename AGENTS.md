# Project Agent Guide

## Project

- Amazon.com 클론 이커머스 시스템이다.
- 백엔드는 Java 17, Spring Boot 3.5.16, Spring Modulith, PostgreSQL, Flyway 기반 모듈러 모놀리스다.
- 프론트엔드는 `amaazon-front/`의 React, TypeScript, Vite 애플리케이션이다.
- 전체 목표와 비즈니스 규칙은 `docs/requirement.md`, 구현 순서는 `docs/implmentation_plan.md`를 기준으로 한다.
- 문서 진입점은 `docs/index.md`, 현재 구현 스냅샷은 `docs/current-state.md`다.
- 작업별 개인 스킬 목록은 `docs/skills/index.md`다. 작업과 일치하는 스킬만 선택해 해당 `SKILL.md` 전문을 읽는다.
- 새로운 대화가 시작되면 `caveman` skill 을 자동으로 적용한다.

## Source of truth

1. 현재 동작은 코드, 테스트, Flyway 마이그레이션을 우선한다.
2. 목표 동작은 `docs/requirement.md`를 우선한다.
3. `docs/current-state.md`는 기록된 Git SHA 시점의 스냅샷이므로 현재 HEAD와 다르면 코드를 다시 확인한다.
4. `docs/implmentation_plan.md`의 일정과 단계는 목표 순서이며 완료 증거가 아니다.
5. 문서와 코드가 충돌하면 추측으로 통일하지 말고 차이를 보고하고 어느 쪽을 변경할지 확인한다.

## Context-efficient discovery

- 코드 심볼과 관계 탐색은 `codebase-memory-mcp`를 우선한다.
- 탐색 순서: `search_graph` → 필요 시 `trace_path` → 대상에만 `get_code_snippet`.
- 문자열, 오류 메시지, 설정값, 비코드 파일 검색에만 `rg`를 사용한다.
- 저장소 전체 파일이나 큰 파일을 선제적으로 읽지 않는다. 먼저 관련 모듈, 인터페이스, 호출 경로를 좁힌다.
- 인덱스 SHA와 현재 HEAD가 다르면 변경 영향을 확인하고 필요한 범위만 재인덱싱한다.

## Architecture rules

- 백엔드 최상위 모듈은 `auth`, `user`, `common`, `global`이며 향후 도메인은 최상위 패키지로 분리한다.
- 각 도메인 모듈은 `presentation`, `application`, `domain`, `infra` 책임을 섞지 않는다.
- 다른 모듈의 내부 구현 패키지를 직접 참조하지 않는다. 공개된 `@NamedInterface` 또는 이벤트만 사용하고 `package-info.java`의 `allowedDependencies`를 지킨다.
- 요구사항의 기본 통신 방식은 Spring Application Event다. 동기 조회가 꼭 필요하면 작은 공개 인터페이스 seam을 사용하고 새 순환 의존을 만들지 않는다.
- 이벤트는 이미 발생한 사실을 표현한다. 작업을 지시하는 명령처럼 사용할 경우 트랜잭션 결합과 실패 의미를 명시하고 ADR로 결정한다.
- 모듈 인터페이스는 작게 유지하고 비즈니스 규칙, 저장 방식, 외부 연동 세부사항은 구현 내부에 둔다.
- 평문 비밀번호는 `auth` 모듈 밖이나 이벤트에 전달하지 않는다. 토큰, 비밀번호, OAuth 비밀값을 로그·문서·테스트 출력에 남기지 않는다.
- 데이터베이스 변경은 기존 마이그레이션 수정 대신 새 Flyway 마이그레이션 추가를 기본으로 한다.

## Verification

- 모든 Gradle 작업은 터미널이 아니라 `gradle-mcp`만 사용한다. MCP 실패 시 터미널로 우회하지 말고 중단 후 원인을 보고한다.
- 백엔드 변경은 가장 가까운 단위 테스트부터 실행하고, 모듈 seam 변경은 Spring Modulith 구조 검증과 관련 통합 테스트까지 확인한다.
- 전체 백엔드 검증이 필요하면 Gradle MCP로 `test`와 필요 시 `jacocoTestCoverageVerification`을 실행한다.
- 프론트엔드 변경은 `amaazon-front/`에서 `npm run lint`와 `npm run build`로 검증한다.
- 문서만 변경한 경우 Gradle 빌드는 생략할 수 있지만 링크, 경로, Git SHA, 코드와의 사실 관계를 검증한다.

## Documentation maintenance

- 구조나 모듈 seam이 바뀌면 `docs/architecture.md`를 갱신한다.
- 도메인 용어 또는 상태 의미가 바뀌면 `docs/domain-glossary.md`를 갱신한다.
- 의미 있는 기능 커밋이나 구현 단계가 달라지면 Continue의 `/update-current-state` Prompt로 `docs/current-state.md`의 날짜와 Git SHA를 함께 갱신한다. 미커밋 변경은 완료 상태로 기록하지 않는다.
- 되돌리기 어렵고 여러 모듈에 영향을 주는 결정은 `docs/adr/`에 ADR로 기록한다.
- `docs/dev_dairy.md`는 사용자가 작성하는 개발 일지다. 사용자의 명시적 요청 없이 수정, 요약, 재배열하지 않는다.
- 코드에서 자동으로 알 수 있는 클래스·메서드 목록을 문서에 복제하지 않는다.
