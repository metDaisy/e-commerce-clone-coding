# Review rubric provenance

이 문서는 Amaazon Reviewer rubric을 만들 때 참고한 Skill과 의도적으로 제외한 절차를 기록한다.
Runtime 실행 순서나 추가 Skill loading 계약이 아니다. 각 leaf `review-*` Skill이 실행 가능한 규칙의
유일한 소유자이며, repository requirement·code·test·migration·architecture 문서와 공식 framework 문서가
현재 사실의 기준이다.

원문의 문장을 복제하기보다 project-specific read-only 판단 규칙으로 재작성했다. Upstream을 갱신하거나
새 원칙을 채택할 때는 leaf owner를 먼저 정하고 중복 규칙을 제거한다.

## Source mapping

| Owner | 참고 Skill | 채택한 원칙 | 의도적으로 제외한 부분 |
|---|---|---|---|
| `run-review` | `reviewer-quality-gates` | immutable scope, 다섯 축 분리, 중복만 병합, 충돌 보존, evidence-based finding | 별도 Profile topology와 프로젝트 밖 lifecycle |
| `run-review` | `requesting-code-review` | cross-cutting security baseline, incomplete evidence의 fail-closed 처리 | auto-fix, commit, fix agent, shell 기반 build 실행 |
| `review-spec` | `code-review` | fixed point, Spec 독립 판단, missing/partial/incorrect/scope-creep 구분 | 2축 orchestrator, 특정 issue-tracker 전제 |
| `review-spec` | `java-junit`, `systematic-debugging` | observable assertion, error/boundary test, tight reproduction, root-cause evidence | TDD 이력 판정, source 수정과 debugging fix 단계 |
| `review-maintainability` | `simplify-code`, `codebase-design` | reuse/quality/efficiency/altitude, confidence/risk, deletion test, depth/locality | parallel cleanup agents, auto-apply, source mutation |
| `review-persistence` | `reviewer-quality-gates`, `java-springboot` | fresh context, query/fetch/pagination, transaction, projection, measured DB evidence | generic package preference와 build command |
| `review-architecture` | `improve-codebase-architecture`, `codebase-design`, `clean-architecture`, `305-frameworks-spring-boot-modulith`, `cross-domain-contract-planning` | scoped deepening, dependency direction, Modulith boundary, published integration contract | HTML/grilling, score, design mutation, Maven command, framework version 가정 |
| `review-evolution-compatibility` | `cross-domain-contract-planning`, `clean-architecture`, `code-review` | producer/consumer matrix, contract ownership, evolution rule, caller impact | contract 결정·task 생성·ADR 작성 |

## Selection rules

1. 범용 Skill의 workflow를 그대로 호출하지 않는다. Reviewer의 read-only lifecycle과 Gradle MCP 규칙으로
   재작성된 leaf 절차만 실행한다.
2. External Skill의 명령, dependency version과 naming convention이 repository rule과 충돌하면 repository가
   우선한다.
3. Auto-fix, commit, push, GitHub write, task mutation, glossary/ADR/source 수정은 Reviewer에 가져오지 않는다.
4. Heuristic은 조사 시작점이다. Exact contract, source, test, validator 또는 측정 evidence 없이는 blocking
   finding으로 승격하지 않는다.
5. `improve-codebase-architecture`만 Reviewer Distribution에 project variant로 함께 ship한다. 나머지 source
   Skill은 runtime dependency가 아니며 Profile 설치 상태에 의존하지 않는다.
6. 새 source Skill을 참고할 때 채택 원칙, 제외 절차, owner와 license/provenance를 먼저 기록한다.

## 현재 제외된 후보

Browser exploratory QA는 aggregate backend Review의 공통 rubric에 포함하지 않는다. UI behavior 검증이
필요하면 현재 Review contract 밖의 별도 역할·task와 capability로 설계하며, 다섯 축에 암묵적으로 추가하지
않는다.