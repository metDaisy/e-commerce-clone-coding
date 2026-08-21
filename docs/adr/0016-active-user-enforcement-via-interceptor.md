# ADR-0016: 활성 사용자 접근 강제 책임을 인터셉터로 이동

- Status: Accepted
- Date: 2026-08-21
- Deciders: 사용자와 Codex
- Supersedes: 없음
- Superseded by: 없음

## Context

P1의 현재 User를 대상으로 하는 API는 활성 사용자만 사용할 수 있어야 한다. 초기 구현에서는
각 도메인 서비스(예: AddressService)가 사용자 활성 상태를 직접 확인했다. 그러나 활성 상태
검증은 HTTP 인증·권한 경계의 공통 관심사이고, 여러 서비스에서 반복될 수 있는 패턴이었다.

활성 사용자 체크를 서비스 레이어로 두면 세 가지 문제가 생긴다:

- 검증 로직이 여러 서비스에 분산되어 적용 누락이나 일관성 문제가 발생할 수 있다.
- 서비스는 "사용자가 활성화되어 있는가?"라는 인증 원칙을 알 필요가 없고, 호출자 정보(principal)를
  직접 다루어야 한다.
- 활성화 조건이 바뀌면(예: 세션 무효화 정책 추가) 각 서비스를 직접 수정해야 한다.

## Decision Drivers

- 활성 사용자 강제를 컨트롤러의 반복 로직이 아니라 프레임워크의 공통 경계에서 적용한다.
- 인증·권한 원칙을 서비스에서 분리하고, 서비스는 도메인 비즈니스만 다룬다.
- 어노테이션으로 의도를 드러내어 가독성과 유지보수성을 높인다.
- 보호가 필요한 모든 HTTP 핸들러에 동일한 규칙을 적용한다.

## Considered Options

### Option A: 서비스에서 활성 사용자 직접 검증

각 도메인 서비스가 `UserRepository.existsByIdAndIsEnabledTrue` 또는 `UserQueryApi`를 호출한다.
초기 구현이 그랬다.

- 장점: 호출이 명시적이고 디버깅이 쉽다.
- 단점: 검증 로직이 여러 서비스에 분산되고 적용이 일관되지 않으며, 인증 원칙이 서비스에 스며든다.
  호출자 식별을 각 서비스가 직접 처리해야 한다.

### Option B: 어노테이션 + 인터셉터로 전역 강제

`common`에 `@RequireEnabledUser` 어노테이션을 정의하고, `global`의 `EnabledUserInterceptor`가
핸들러에 해당 어노테이션이 붙었는지 확인한 뒤 `UserQueryApi.requireEnabled`를 호출한다.
`WebMvcConfig`가 인터셉터를 등록하고, 어노테이션이 붙은 Controller 또는 핸들러만 검증한다.

- 장점: 활성 사용자 강제가 하나의 경계에서 적용되고, 보호 대상 Controller에 어노테이션을 붙여 의도를 드러낼 수 있다.
  인증 원칙이 서비스에서 분리되며, 조건이 바뀌면 인터셉터만 수정하면 된다.
- 단점: 어노테이션을 붙이지 않은 핸들러는 강제를 놓칠 수 있어, 붙여야 하는 핸들러 목록을
  관리해야 한다. 인증이 실패하면 `AuthenticationCredentialsNotFoundException`이 발동되어
  전역 예외 처리가 이를 매핑한다.

### Option C: AOP로 서비스·컨트롤러 호출을 가로채기

공통 어노테이션을 기준으로 Spring Bean 메서드 호출 시 활성 User를 검증한다.

- 장점: 여러 호출 지점에 횡단 관심사를 적용할 수 있다.
- 단점: HTTP 요청 전처리보다 넓은 범위에 적용되어 서비스 직접 호출·배치·이벤트 처리까지 같은
  경계를 요구하게 된다. 프록시 적용 여부와 호출 경계에 따라 동작 범위가 달라지고, HTTP 인증 실패를
  MVC 예외 처리와 연결하는 책임도 불명확해진다.
- 이 결정에서는 활성 User 검증을 HTTP 요청 경계에 한정하므로 선택하지 않는다.

## Decision

Option B를 선택한다.

- 활성 사용자 강제 책임은 `global`의 `EnabledUserInterceptor`가 소유한다.
- `@RequireEnabledUser`는 `common` 모듈에 정의되며, 컨트롤러(Class) 또는 핸들러(Method)에 적용한다.
- 인터셉터는 `UserQueryApi.requireEnabled(principal.getId())`를 통해 `user` 모듈의 공개
  인터페이스를 통해 활성 상태를 확인한다. `global`은 `user`의 내부 구현을 직접 참조하지 않는다.
- `WebMvcConfig`는 인터셉터를 MVC 핸들러 전체에 등록한다. 실제 활성 User 검증은
  `@RequireEnabledUser`가 붙은 Controller 또는 핸들러에만 수행한다.
- 이 인터셉터는 활성 상태만 확인하며, Address 소유권·역할·관리자 권한 같은 리소스 권한 검증을
  대체하지 않는다.
- 인증 정보가 없으면 `AuthenticationCredentialsNotFoundException`을 통해 `AUTH-001`·401로 처리한다.
- 인증된 User가 비활성 상태면 `UserException(USER-004)`를 통해 403으로 처리한다.

## Consequences

### Positive

- 활성 사용자 강제 적용이 일관되고 적용 누락이 줄어든다.
- 인증 원칙이 서비스에서 분리되어 도메인 서비스가 비즈니스에만 집중한다.
- 활성 조건이나 인증 실패 시 응답 변경이 인터셉터 하나로 가능하다.
- 인터셉터는 `AmaazonPrincipal`에서 User ID를 읽고 공개 `UserQueryApi`만 호출한다. 서비스는 Controller가 전달한 User ID와 도메인 규칙에 집중한다.

### Negative

- 어노테이션을 붙이지 않은 핸들러는 강제를 놓칠 수 있어, 적용 대상 목록을 인식하고 관리해야 한다.
- 인증 실패 시 사용되는 예외가 전역 예외 전략에 종속되어, 매핑 로직을 함께 이해해야 한다.
- 인터셉터가 `UserQueryApi`에 의존하므로 `global` → `user` 공개 인터페이스 방향이 생긴다.
- 이 결정은 Spring MVC HTTP 요청 경계에만 적용된다. 배치·이벤트 리스너·서비스 직접 호출에는 별도의 User 상태 검증이 필요하다.

### Follow-up

- 현재 보호 대상 Controller와 메서드에 `@RequireEnabledUser`가 빠짐없이 적용되었는지 기능 추가 때마다 확인한다.
- 인증 정보 없음은 401, 비활성 User는 `USER-004`·403으로 매핑되는 계약을 유지한다.
- 세션 무효화와 재인증은 P11 정책이 구현될 때 별도 통합 테스트로 연결한다.

## Evidence

- `src/main/java/io/github/metdaisy/amaazon/global/web/interceptor/EnabledUserInterceptor.java`
- `src/main/java/io/github/metdaisy/amaazon/common/auth/RequireEnabledUser.java`
- `src/main/java/io/github/metdaisy/amaazon/global/web/config/WebMvcConfig.java`
- `src/main/java/io/github/metdaisy/amaazon/global/package-info.java`
- `src/main/java/io/github/metdaisy/amaazon/user/application/port/in/UserQueryApi.java`
- `src/test/java/io/github/metdaisy/amaazon/global/web/interceptor/EnabledUserInterceptorTest.java`
- 커밋 `3da2d7b feat: 활성 사용자만 접근 가능한 어노테이션 추가`, `c77f2eb feat: 활성 사용자 인터셉터 및 모듈 의존성 추가`
