# Local Skills Index

이 문서는 `C:\Users\leee\.agents\skills`에 설치된 개인 로컬 스킬의 카탈로그다. 세부 지침을 매 대화에 주입하지 않고 이름과 사용 조건만 제공한다.

## 사용 규칙

1. 현재 작업과 명확히 일치하는 스킬만 선택한다.
2. 선택한 경우 해당 `SKILL.md` 전체를 읽고 작업 전에 지침을 적용한다.
3. 여러 스킬이 겹치면 작업을 충족하는 최소 조합만 사용한다.
4. 스킬은 작업 절차다. 프로젝트 사실의 원본은 코드와 `docs/`다.
5. 로컬 경로가 없거나 다른 환경이면 스킬을 필수 의존성으로 가정하지 말고 가능한 대안을 보고한다.

## Catalog

| Skill | 읽을 때 | 위치 |
|---|---|---|
| `caveman` | 내부 추론과 사용자 응답을 짧지만 완전하게 유지할 때. | `C:\Users\leee\.agents\skills\caveman\SKILL.md` |
| `codebase-design` | 모듈 인터페이스, seam, adapter, 의존 방향, 테스트 표면을 설계하거나 개선할 때 | `C:\Users\leee\.agents\skills\codebase-design\SKILL.md` |
| `improve-codebase-architecture` | 코드베이스 전체에서 구조 개선 후보를 조사하고 시각적 보고서로 비교할 때 | `C:\Users\leee\.agents\skills\improve-codebase-architecture\SKILL.md` |
| `java-springboot` | Spring Boot 설정, 계층, 트랜잭션, 의존성 주입, 웹·데이터 기능을 구현하거나 검토할 때 | `C:\Users\leee\.agents\skills\java-springboot\SKILL.md` |
| `java-junit` | JUnit 5 단위 테스트, parameterized test, 테스트 구조와 assertion을 작성하거나 검토할 때 | `C:\Users\leee\.agents\skills\java-junit\SKILL.md` |
| `java-refactoring-extract-method` | 긴 Java 메서드에서 응집된 로직을 메서드로 추출하는 리팩터링을 수행할 때 | `C:\Users\leee\.agents\skills\java-refactoring-extract-method\SKILL.md` |
| `java-refactoring-remove-parameter` | 사용하지 않거나 불필요한 Java 매개변수를 안전하게 제거하고 호출부 영향을 검증할 때 | `C:\Users\leee\.agents\skills\java-refactoring-remove-parameter\SKILL.md` |
| `agent-browser` | 웹사이트 탐색, 폼 입력, 브라우저 자동화, 스크린샷, 웹 애플리케이션 QA가 필요할 때 | `C:\Users\leee\.agents\skills\agent-browser\SKILL.md` |

## 유지관리

- `C:\Users\leee\.agents\skills`에 스킬이 추가·삭제되거나 설명이 바뀔 때만 이 표를 갱신한다.
- 스킬 본문을 이 저장소로 복사하지 않는다. 개인 스킬과 프로젝트 문서의 변경 주기를 분리한다.
