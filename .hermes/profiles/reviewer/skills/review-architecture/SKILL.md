---
name: review-architecture
description: "Review requirement-scoped architecture and module seams."
version: 0.1.0
author: "Amaazon project, Hermes Agent"
license: MIT
platforms: [linux, macos, windows]
metadata:
  hermes:
    tags: [code-review, architecture, spring-modulith, deep-modules]
    related_skills: [improve-codebase-architecture, codebase-memory-mcp, semble-search]
---

# Architecture Review

`improve-codebase-architecture`를 `checkpoint` mode로 사용해 현재 requirement가 변경하거나 의존하는
architecture만 검토한다. Changed lines에 갇히지 않되 전체 repository redesign으로 확대하지 않는다.

## 기본 범위: requirement 영향 closure

다음을 포함한다.

- reviewed diff와 changed symbols
- changed behavior의 direct caller/consumer와 callee/dependency
- 영향을 받는 application module, public/named interface와 adapter
- touched event, transaction seam, package dependency와 test surface
- card acceptance를 구현하기 위해 함께 변해야 하는 sibling path

관계가 없는 module, historical debt와 hypothetical future design은 제외한다. `improve-codebase-architecture`에
위 범위를 명시적으로 전달하고 scope inference나 repository hotspot scan을 사용하지 않는다.

## Code discovery 선택

Requirement를 구현하는 module/seam 후보가 불명확하면
[`../run-review/references/code-discovery-guide.md`](../run-review/references/code-discovery-guide.md)의
**Semble branch**로 domain capability와 관련 code/docs를 찾는다. Known module·interface·event에서 dependency,
caller/consumer, cycle 또는 cross-module path를 확인할 때는 **Codebase Memory branch**를 사용한다. Exact
`allowedDependencies`, `@NamedInterface`, package 또는 ADR identifier는 local literal search를 사용한다.
Graph index freshness를 확인하고 모든 boundary 판정은 현재 `package-info.java`, imports, event code와
Modulith test에서 확정한다.

## 절차

1. Repository의 `docs/architecture.md`, 관련 ADR, `package-info.java`와 module verification test를 먼저 읽고
   current executable boundary를 고정한다.
2. `improve-codebase-architecture`의 checkpoint mode로 dependency direction, interface depth, seam, adapter,
   leverage, locality와 deletion test를 적용한다. Aggregate review에서는 HTML report, grilling, design
   mutation과 glossary/ADR 수정을 실행하지 않는다.
3. Spring Modulith를 확인한다: application module ownership, `allowedDependencies`, `@NamedInterface`, internal
   package access, cycle, event publication/consumption과 module test coverage.
4. Layer 방향을 확인한다: presentation → application → domain, infra adapter → inward contract. Framework,
   persistence와 transport detail이 domain policy 또는 public contract에 누출되는지 확인한다.
5. Event가 완료 사실인지 사실상 command인지, transaction coupling·failure semantics·idempotency가 기존
   ADR/contract와 일치하는지 확인한다.
6. Cross-module seam은 consumer가 요구하는 capability에서 시작해 published contract인지 absent/partial인지
   판정한다. Current synchronous answer가 필요하면 named-interface query, eventual consistency가 승인되고
   consumer-owned read model이 있으면 fact event/projection이 적합한지 확인한다. Internal entity, repository,
   controller 또는 schema를 public contract처럼 사용하면 boundary leak 후보다.
7. Public seam의 owner, request/event, response/projection, authorization, not-found/error, consistency/transaction,
   ordering/paging/idempotency와 evolution rule이 requirement와 ADR에 의해 충분히 결정됐는지 확인한다.
   결정이 없으면 preferred integration mode를 finding으로 강제하지 않고 context/decision 후보로 돌린다.
8. Seam마다 실제 variation과 adapter 수를 확인한다. One adapter와 future possibility만으로 abstraction을
   요구하지 않고, caller가 알아야 하는 invariant/order/error가 늘었는지로 depth를 판단한다.
9. `ApplicationModules.verify()` 또는 project `ModularityTest`와 관련 module integration test를 root에
   요청한다. 모든 Gradle 실행은 `gradle-mcp`만 사용한다.

## Finding 경계

현재 change가 boundary를 위반하거나 새 coupling을 만들거나 requirement 수행을 위험하게 할 때 finding
후보다. 기존 unrelated architecture debt와 더 나은 redesign 아이디어는 `non_blocking_observations`에 두며
aggregate verdict를 변경하지 않는다. Architecture preference나 pattern 부재만으로 finding을 만들지 않는다.

## Repository-wide audit 시점

전체 codebase 분석은 regular aggregate review에 섞지 않고 PM이 별도 architecture-audit task와 immutable
scope를 승인했을 때 `improve-codebase-architecture`의 `repository-audit` mode로 수행한다. 다음 trigger가
있으면 별도 audit을 제안한다.

- 새 application module/bounded context 또는 대규모 cross-module integration 전
- 여러 Review에서 같은 coupling, shotgun surgery 또는 boundary finding이 반복될 때
- major release/milestone, framework·database·messaging 전환 또는 대규모 migration 전
- `allowedDependencies`, cycle, public contract 변화가 여러 module에 퍼질 때
- owner가 정한 정기 architecture health checkpoint

Audit 결과는 현재 Review finding으로 소급하지 않고 별도 backlog/decision evidence가 된다.

## 완료 기준

Requirement 영향 closure의 module·interface·seam·adapter와 executable Modulith boundary를 검토했고,
현재 regression과 unrelated debt가 분리된 axis result가 있다. Graph/semantic discovery에서 얻은 모든
dependency와 boundary claim이 checked-out source로 확인됐다.