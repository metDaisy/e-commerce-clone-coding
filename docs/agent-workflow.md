# Agent 작업 절차

## 목적과 적용 범위

Agent가 코드·테스트·설정·문서 작업을 수행할 때의 조사, 도구 선택, 수정, 검증, 보고 순서를 정의한다.

- 이 문서는 작업 절차를 정의하며 현재 구현 사실을 중복하지 않는다.
- 현재 동작은 체크아웃된 코드·테스트·설정·Flyway 마이그레이션을 우선한다.
- 목표 계약은 `docs/requirement/`을, 구조 원칙은 `docs/architecture.md`와 ADR을, 테스트 절차는 `docs/testing-guide.md`를, Agent 행동 검증 계약은 `docs/validator-contract.md`를 따른다.
- `AGENTS.md`의 강제 규칙이 이 문서보다 우선한다. 일반 기술 절차는 [`docs/skills/index.md`](skills/index.md)의 Project Skill Map에서 필요한 로컬 Skill을 골라 읽어 보완한다.
- 세션 요약, Built-in Memory, 외부 Memory Provider, 검색 인덱스 결과는 탐색 보조 수단이다. 현재 코드·공식 문서·Git 상태를 대체하지 않는다.

## Hermes 문서 컨텍스트 로딩

- Hermes는 세션의 작업 디렉터리에서 프로젝트 컨텍스트 파일을 찾는다. 이 저장소에서는 `AGENTS.md`가 프로젝트 규칙의 자동 주입 대상이다.
- `docs/`의 문서는 자동으로 모두 주입되지 않는다. Agent는 `AGENTS.md`의 지시에 따라 이 문서와 `docs/index.md`를 읽고, 작업에 필요한 기준 문서를 선택해서 읽는다.
- `docs/agent-workflow.md`를 읽는 것과 그 절차를 실제로 따르는 것은 별개의 단계다. 작업 시작 시 관련 기준 문서·코드·테스트를 직접 확인한다.
- 실제 로딩 여부는 세션의 resolved cwd에 좌우된다. Desktop이 프로젝트 디렉터리를 작업 디렉터리로 전달하지 않으면 `AGENTS.md`가 로딩되지 않을 수 있다.
- `hermes prompt-size --json`의 `context (AGENTS.md/cwd files)` 값은 진단 경로의 결과다. 값이 `0`이면 Desktop 세션이 규칙을 읽었다고 가정하지 말고, cwd와 실제 prompt/context를 별도로 확인한다.

## 작업 시작 공통 절차

모든 작업은 다음 순서를 따른다.

1. 현재 Git 상태와 브랜치를 확인하고, 기존 staged·unstaged·untracked 변경을 작업 범위에서 분리한다.
2. 요청을 구현, 버그 수정, 리팩터링, 조사, 문서, 검증, 커밋 메시지 중 하나 이상으로 분류한다.
3. `docs/index.md`의 작업별 읽기 순서와 아래 결정표에서 필요한 최소 문서·도구를 선택한다.
4. 수정 전 관련 구현, 호출 경로, 테스트, 공개 계약을 실제 파일에서 확인한다.
5. 요청 범위에만 최소 변경을 적용한다. 기존 미커밋 변경을 덮어쓰거나 무관한 정리를 섞지 않는다.
6. 변경 종류에 맞는 검증을 실행하고, 실행하지 못한 검증은 통과한 것처럼 보고하지 않는다.

요구사항·코드·테스트·문서가 충돌하면 추측으로 맞추지 않는다. 각 근거와 불일치 범위를 보고하고, 필요한 결정을 요청한다.

## 탐색 도구 결정표

도구 결과는 후보와 관계를 빠르게 찾기 위한 보조 정보다. 수정 또는 확정적인 결론 전에는 해당 파일과 테스트를 읽어 확인한다.

| 상황 | 먼저 사용할 수단 | 다음 단계와 제한 |
|---|---|---|
| 구현 위치·관련 기능을 모름 | Semble 의미 기반 검색 | 반환된 파일·행을 읽고, 필요하면 `find-related`로 유사 구현을 찾는다. |
| 클래스·메서드·설정 키·오류 문구·migration 번호를 정확히 앎 | `search_files` 같은 literal 검색 | 모든 관련 위치를 확인한 뒤 필요한 파일만 읽는다. |
| caller, callee, 모듈 간 경로, 영향 범위를 파악해야 함 | codebase-memory graph 검색 | index의 최신성을 확인하고 실제 source와 대조한다. graph와 source가 다르면 source를 우선한다. |
| 요구사항·용어·현재 구현 상태를 확인 | `docs/index.md`에서 연결한 문서 | 요구사항만으로 구현 완료를 판단하지 않고 코드·테스트와 `current-state.md`를 함께 확인한다. |
| Spring·라이브러리·도구의 현재 API를 확인 | 공식 문서 또는 Context7 | 프로젝트의 실제 버전과 `build.gradle`/manifest를 함께 확인한다. |
| 브라우저 동작, form, 시각적 회귀를 확인 | 브라우저 자동화 | 로그인·credential이 필요하면 추측하거나 우회하지 않고 사용자에게 요청한다. |
| Git 상태·diff·커밋 이력을 확인 | Git 명령 | 현재 작업 트리 기준으로 다시 확인한다. |

### Semble과 codebase-memory의 역할

- **Semble**: “어디에 어떤 구현이 있는가”를 의미와 동작으로 찾는다.
- **codebase-memory**: “무엇이 무엇을 호출·의존하는가”와 변경 영향을 찾는다.
- **literal 검색**: 이미 정확한 식별자나 문구를 알 때 빠짐없이 찾는다.
- 같은 넓은 질문을 세 도구에 반복하지 않는다. 한 도구의 후보를 실제 파일로 확인한 뒤 다음 질문을 좁힌다.
- MCP나 index가 없거나 최신성을 확인할 수 없으면 로컬 파일 탐색으로 계속 진행한다. MCP의 부재가 안전한 조사 작업을 중단시키는 이유가 되지 않는다.
- 새 index 생성, 기존 graph 재색인, 외부 서비스 설치·설정은 별도 상태 변경이다. 사용자가 요청하거나 승인한 경우에만 수행한다.

## 작업 유형별 절차

### 1. 구현 또는 요구사항 변경

1. `docs/requirement/index.md`와 해당 도메인 요구사항에서 목표 계약·예외·상태 전이를 확인한다.
2. `docs/current-state.md`, 관련 production code, 기존 테스트로 실제 구현 범위를 확인한다.
3. 모듈 경계가 관련되면 `docs/architecture.md`, 관련 ADR, 각 모듈의 `package-info.java`를 읽는다.
4. 성공 경로와 거절·오류 경로를 포함하는 가장 작은 적절한 테스트를 먼저 추가하거나 수정한다.
5. production code, API 계약, 설정, 문서를 함께 변경해야 하는 실제 영향만 반영한다.
6. 아래 검증 매트릭스의 해당 항목을 실행한다.

### 2. 버그 수정

1. 재현 조건, 실제 결과, 기대 결과와 영향 범위를 분리해 확인한다.
2. 로그·오류 문자열은 literal 검색으로, 원인 구현 위치는 의미 기반 검색으로 찾는다.
3. 가능하면 먼저 실패를 재현하는 회귀 테스트를 추가한다.
4. 증상이 아니라 원인을 수정하고, 같은 경로를 공유하는 호출자도 확인한다.
5. assertion을 약화하거나 테스트를 삭제해 실패를 숨기지 않는다.

### 3. 리팩터링

1. 동작을 변경하지 않는다는 범위와 현재 테스트 표면을 확인한다.
2. public API, 이벤트, DB schema, 모듈 seam에 영향이 있는지 먼저 확인한다.
3. 리팩터링과 독립적인 기능·문서·포맷 변경을 섞지 않는다.
4. 변경 전후 관련 테스트와 구조 검증을 실행해 동작 보존을 확인한다.

### 4. 모듈 경계·공개 계약·이벤트 변경

1. 관련 모듈의 `package-info.java`와 `allowedDependencies`를 확인한다.
2. 내부 package, JPA entity, repository, infra 구현을 다른 모듈에서 직접 참조하지 않는다.
3. 동기 통신은 작은 `@NamedInterface` 계약으로 제한하고, 사실 통지는 이벤트로 표현한다.
4. 명령 성격 이벤트나 트랜잭션 결합의 의미가 바뀌면 ADR 필요성을 검토한다.
5. 관련 모듈 테스트와 Spring Modulith 구조 검증을 추가로 실행한다.
6. 구조·seam·허용 의존성이 바뀐 경우에만 `docs/architecture.md`와 필요한 ADR을 갱신한다.

### 5. JPA·Repository·Flyway 변경

1. 기존 migration, JPA mapping, domain repository 계약, adapter 테스트를 모두 확인한다.
2. schema 변경은 새 `src/main/resources/db/migration/V<다음 번호>__<설명>.sql`만 추가한다. 적용된 migration을 수정하지 않는다.
3. 모듈 간 식별자에는 DB 외래 키를 추가하지 않는다. 공개 seam 또는 이벤트로 검증한다.
4. repository 테스트는 `docs/testing-guide.md`의 fresh query·query count·성공/제약 위반 규칙을 따른다.
5. PostgreSQL Testcontainers 기반 관련 DB 테스트와 migration 검증을 실행한다.

### 6. API·보안 변경

1. controller, request/response DTO, service 계약, 예외 응답, security configuration과 관련 테스트를 함께 확인한다.
2. HTTP contract가 바뀌면 호출자와 실패 응답까지 확인한다.
3. web test URL은 `WebConstants.SERVLET_PREFIX` 또는 테스트 상수를 사용한다.
4. 토큰, 비밀번호, 인증 header, 개인 정보를 코드·로그·응답·문서·memory에 기록하지 않는다.

### 7. 프런트엔드 변경

1. `amaazon-front/`의 관련 화면, route, API client, type, 테스트·기존 UI 패턴을 확인한다.
2. 서버 계약을 추측하지 않고 backend DTO와 controller를 대조한다.
3. lint와 build를 실행한다. 필요할 때 브라우저로 대표 사용자 흐름을 확인한다.
4. 프런트와 백엔드가 하나의 사용자 기능을 함께 완성하면 같은 논리적 변경으로 취급한다.

### 8. 문서 변경

1. `docs/index.md`에서 해당 문서의 역할과 인접 기준 문서를 확인한다.
2. 코드에서 자동 추출 가능한 클래스·메서드 목록을 중복 작성하지 않는다.
3. `current-state.md`에는 검증된 커밋 상태만 기록한다. 현재 미커밋 변경을 완료 구현으로 쓰지 않는다.
4. 내부 링크·상대 경로·문서 제목·SHA·기술적 사실을 실제 파일 또는 Git 정보로 확인한다.
5. 구조, 용어, 요구사항 상태가 달라지면 영향받는 기준 문서도 함께 갱신한다.

### 9. 조사·리뷰만 요청된 경우

1. 작업 트리를 변경하지 않는다.
2. 발견 사항은 심각도 순으로 정리하고 각각 근거 파일·행, 영향, 권장 조치를 기록한다.
3. 사실과 추론을 구분한다. 테스트·빌드를 실행하지 않았다면 그 사실을 명시한다.
4. 별도 요청 없이는 commit, amend, push, merge, rebase, reset을 실행하지 않는다.

### 10. 커밋 메시지만 요청된 경우

1. `docs/commit-message-convention.md`를 읽는다.
2. 먼저 `git diff --cached`를 확인한다. staged 변경이 없으면 작업 트리 diff를 확인하고 대상 범위를 명시한다.
3. diff를 근거로 type·한국어 subject·본문·명시된 Issue만 결정한다.
4. 실제 commit은 실행하지 않는다. 커밋 메시지 요청의 최종 출력 형식은 해당 문서의 raw 메시지 규칙을 따른다.

## 테스트 선택과 검증

테스트는 가장 작은 범위부터 선택한다. 테스트 작성 형식과 세부 assertion 규칙은 `docs/testing-guide.md`가 기준이다.

| 변경 또는 검증 대상 | 최소 검증 | 추가 검증 |
|---|---|---|
| domain rule, validator, service | 관련 unit test | 성공과 거절 경로 |
| controller, 요청 검증, JSON | `@WebMvcTest` | service delegation과 error response |
| JPA adapter, query, constraint | 관련 repository test | fresh entity, query count, migration 영향 |
| transaction, security, 모듈 seam, 외부 협업 | 관련 integration test | rollback·event·권한 경로 |
| 모듈 경계, named interface, event | Modulith structure verification | 관련 module/integration test |
| Flyway 또는 persistence | 관련 PostgreSQL Testcontainers test | migration 검증 |
| frontend | `npm run lint`, `npm run build` | 대표 browser flow |
| 문서 | 링크·경로·사실 확인 | 영향받은 기준 문서 동기화 |

### 실행 경로

- 모든 Gradle 작업(`test`, `check`, Checkstyle, JaCoCo, build 포함)은 **gradle-mcp**로 실행한다. 터미널에서 `gradle`, `gradlew`, `gradlew.bat`을 직접 실행하지 않는다.
- gradle-mcp를 사용할 수 없으면 Gradle 의존 검증을 중단하고, 실패 원인과 필요한 다음 조치를 보고한다. 다른 Gradle 실행 경로로 우회하지 않는다.
- 프런트엔드의 `npm run lint`, `npm run build`와 Git·문서 확인은 프로젝트 규칙에 맞는 일반 실행 도구를 사용한다.
- 선택한 검증이 실행 불가하거나 실패하면 실패 원문, 영향 범위, 완료되지 않은 검증을 분명히 보고한다.

## 수정 전·후 점검표

### 수정 전

- [ ] 현재 Git 상태와 요청 범위를 확인했다.
- [ ] 관련 코드, 호출 경로, 테스트와 기준 문서를 읽었다.
- [ ] API·DB·모듈 경계·문서의 영향을 확인했다.
- [ ] 기존 미커밋 변경을 보존할 수 있다.

### 수정 후

- [ ] 요청 범위 밖의 변경을 섞지 않았다.
- [ ] 성공과 실패 또는 거절 경로를 검증했다.
- [ ] 해당되는 Gradle·프런트엔드·문서 검증을 실제 실행했다.
- [ ] 구조·요구사항·용어·현재 상태 문서 갱신 필요성을 확인했다.
- [ ] 실행 결과와 미실행 항목을 구분해 보고할 수 있다.

## 결과 보고 형식

완료 보고는 짧고 사실 기반으로 작성한다.

1. 변경 또는 조사 결론을 먼저 한두 문장으로 제시한다.
2. 변경 파일 또는 근거를 `path:line`으로 제시한다.
3. 실제 실행한 검증과 결과를 명시한다.
4. 실패·미실행 검증·남은 위험이 있으면 원인과 필요한 다음 조치를 분리해 적는다.

"완료"는 요청 범위의 변경과 필요한 검증이 실제로 끝난 경우에만 사용한다.
