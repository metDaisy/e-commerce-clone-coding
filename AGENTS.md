# 프로젝트 Agent 가이드

## 적용 범위

- 이 파일은 이 저장소의 코드·테스트·설정·문서를 변경할 때 적용되는 프로젝트 규칙이다.
- 작업 전에 현재 Git 상태와 요청 범위를 확인하고, 관련 코드·테스트·문서를 먼저 조사한다.
- 작업 유형별 조사·도구 선택·수정·검증 절차는 `docs/agent-workflow.md`를 따른다.

## 프로젝트 개요

- 백엔드: Java 17, Spring Boot, Spring Modulith, PostgreSQL, Flyway.
- 프런트엔드: `amaazon-front/`의 React, TypeScript, Vite 애플리케이션.
- 문서 지도는 `docs/index.md`를 사용한다.
- 현재 구현 구조와 모듈 목록은 `docs/architecture.md`, 각 `package-info.java`, `docs/current-state.md`를 기준으로 확인한다. 이 파일에 모듈 목록을 중복해서 기록하지 않는다.

## 기준 문서

1. 현재 동작: 커밋된 코드, 테스트, 설정, Flyway 마이그레이션.
2. 목표 동작: `docs/requirement/index.md`와 해당 P1~P12 요구사항 문서.
3. 구조 원칙: `docs/architecture.md`와 관련 `docs/adr/` 문서.
4. 현재 구현 상태: `docs/current-state.md`. 요구사항 문서만으로 구현 완료를 판단하지 않는다.

요구사항·코드·테스트·문서 사이에 불일치가 있으면 추측하지 말고 근거와 불일치 내용을 보고한다.

Project Manager가 `backend-implementation-card-v1`으로 위임한 작업에서는 PM이 승인 요구사항을
self-contained 실행 계약으로 materialize한다. `implementation-coder`는 그 card를 목표 동작의
입력으로 사용하고 요구사항을 다시 기획·해석하지 않는다. 이 파일의 구조·보안·검증 규칙과 실제
코드·테스트는 계속 적용하며, card와 충돌하면 임의로 보정하지 않고 작업을 block해 PM에 보고한다.

## 커밋 메시지와 커밋 단위

- 커밋 메시지 형식과 논리적 커밋 단위는 `docs/commit-message-convention.md`를 따른다.
- 하나의 커밋에는 하나의 논리적 변경만 포함하고, 기능·버그 수정·리팩터링·무관한 문서 변경을 섞지 않는다.
- 기능 구현과 관련 테스트, 버그 수정과 회귀 테스트는 같은 커밋에 포함한다.
- 사용자가 명시적으로 요청하지 않은 commit, amend, push, merge, rebase, reset은 실행하지 않는다.

## 변경 원칙

- 요청받은 범위만 수정하고, 관련 없는 리팩터링·이름 변경·포맷 변경은 하지 않는다.
- 변경 전에 관련 심볼의 정의·호출 경로·테스트를 확인한다. 존재를 확인하지 않은 파일·API·의존성·설정을 만들어내지 않는다.
- 기존 프로젝트 스타일과 공개 계약을 유지한다. API·DB 계약을 변경하면 영향을 받는 호출자와 테스트를 함께 확인한다.
- 버그 수정은 가능하면 실패를 재현하는 테스트를 먼저 추가하고, 테스트의 assertion을 약화하거나 삭제해서 문제를 숨기지 않는다.
- 기존 미커밋 변경을 덮어쓰지 않는다.

## 아키텍처 규칙

- 모듈 내부는 `presentation`, `application`, `domain`, `infra`의 책임을 분리한다.
- 모듈 간 통신은 공개된 `@NamedInterface` 또는 이벤트를 사용한다. 다른 모듈의 내부 패키지·구현·저장소·JPA 엔티티를 직접 참조하지 않는다.
- `package-info.java`의 `allowedDependencies`를 모듈 의존성의 기준으로 따른다.
- 이벤트는 발생한 사실을 표현한다. 명령 성격으로 사용하는 경우 트랜잭션 결합과 실패 의미를 ADR에 기록한다.
- 외부 시스템은 port와 adapter 뒤에 격리한다. 도메인 코드가 외부 SDK나 저장 기술에 직접 의존하지 않게 한다.
- 데이터베이스 스키마 변경은 새 Flyway 마이그레이션으로 추가한다. 기존 마이그레이션을 임의로 수정하지 않는다.
- 비밀번호, 토큰, API key와 기타 credential을 코드·로그·응답·문서에 노출하지 않는다.

## 검증

- 백엔드 변경: 가장 가까운 관련 테스트를 실행한다.
- 모듈 경계·공개 계약·이벤트 변경: Modulith 구조 검증과 관련 통합 테스트를 추가로 실행한다.
- Flyway·JPA·저장소 변경: 관련 데이터베이스 테스트와 migration 검증을 실행한다.
- 프런트엔드 변경: `amaazon-front/`에서 `npm run lint`와 `npm run build`를 실행한다.
- 문서만 변경: 링크·경로·SHA·사실관계를 확인한다.
- 실행하지 않은 테스트·빌드·검증은 통과했다고 보고하지 않는다. 실패하면 원인, 영향 범위, 필요한 다음 조치를 보고한다.

## 문서 유지보수

- 구조·모듈 경계·공개 계약이 바뀔 때 `docs/architecture.md`와 필요한 ADR을 갱신한다.
- 용어·상태값이 바뀌면 `docs/domain-glossary.md`와 관련 요구사항 문서를 갱신한다.
- `docs/current-state.md`에는 검증된 커밋 상태만 기록한다. 미커밋 작업을 완료된 구현으로 기록하지 않는다.
- 코드에서 자동으로 추출할 수 있는 클래스·메서드 목록을 문서에 중복해서 관리하지 않는다.
