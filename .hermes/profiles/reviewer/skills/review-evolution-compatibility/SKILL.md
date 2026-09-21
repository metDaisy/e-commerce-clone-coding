---
name: review-evolution-compatibility
description: "Review changed contracts for safe evolution."
version: 0.1.0
author: "Amaazon project, Hermes Agent"
license: MIT
platforms: [linux, macos, windows]
metadata:
  hermes:
    tags: [code-review, compatibility, api, migration]
    related_skills: [codebase-memory-mcp, semble-search]
---

# Evolution and Compatibility Review

Changed contract가 existing caller, stored data와 mixed-version runtime에서 안전하게 진화하는지 검토한다.
Extensibility는 실제 requirement·external integration·known volatility가 있을 때만 요구한다.

`run-review/references/axis-result-contract.md`의 invocation packet을 소비하고 raw axis result를
`run-review`에 반환한다. PM에게 직접 handoff하지 않는다.

## Applicability

Public/named interface, HTTP API, DTO/serialization, event, configuration, database schema/data, dependency version,
deprecated API 또는 externally consumed behavior가 영향받으면 `reviewed`다. Call/consumer/contract inspection으로
외부 또는 persisted contract가 없음을 입증한 internal-only change는 `not-applicable`일 수 있다.

## Code discovery 선택

Contract producer/consumer나 replacement API의 위치를 모르면
[`../run-review/references/code-discovery-guide.md`](../run-review/references/code-discovery-guide.md)의
**Semble branch**로 intent 기반 후보를 찾는다. Known DTO/event/public interface의 callers, consumers,
dependency path와 impact closure가 필요하면 **Codebase Memory branch**를 사용한다. Exact field, serialized
name, config key, deprecated symbol과 migration identifier는 local literal search를 사용한다. Candidate
consumer가 실제 contract에 의존하는지는 import, serialization, fixture, migration 또는 integration source로
확인한다.

## 절차

1. Changed contract와 모든 known producer/consumer/caller를 찾는다. Source, test fixture, migration,
   configuration, generated client와 integration locator를 직접 확인한다.
2. Compatibility surface를 구분한다.
   - source/binary caller compatibility
   - HTTP status, field, nullability와 error body
   - JSON/event name, type, required/default field와 ordering assumption
   - database schema/data 및 rolling/mixed-version compatibility
   - configuration key/default와 deployment compatibility
3. Addition, removal, rename, type/semantic change를 분류하고 old producer/new consumer 및 new producer/old
   consumer 조합을 확인한다. Deprecated API에는 replacement, call-site migration과 removal timing이 있는지
   본다.
4. Cross-module/external contract에는 owner, public surface, authorization, missing/error semantics,
   consistency/transaction, ordering/paging/idempotency와 versioning rule이 있는지 확인한다. Internal JPA entity,
   repository, controller 또는 schema shape가 consumer contract로 누출되면 독립 evolution 위험 후보다.
5. Flyway change는 expand/contract 순서, backfill, default/null handling, old code와 new schema 공존,
   irreversible data transformation과 rollback 전략을 검토한다.
6. 실제 variation이 requirement, domain policy, external integration 또는 반복 change로 입증된 경우 seam의
   locality를 평가한다. 단지 미래 가능성이 있다는 이유로 interface, factory, option 또는 version layer를
   추가하라고 요구하지 않는다.
7. Contract/serialization/integration/migration test를 root에 요청하고 실행되지 않은 compatibility matrix를
   pass로 표현하지 않는다.

## Finding 기준

Known caller/consumer가 깨지거나 stored data를 읽을 수 없거나 rollout/rollback 중 contract가 불일치하는
재현 가능한 위험은 finding 후보다. Versioning preference, undocumented hypothetical consumer와 speculative
future variation은 `non_blocking_observations`에 둔다. 새로운 public policy가 필요하면
`decision-required`, 승인된 compatibility expectation이 없으면 `context-required` 후보로 분류한다.

## 완료 기준

영향받는 contract와 producer/consumer matrix, migration·rollback, deprecated API와 known variation을
판정하고, 실제 compatibility defect와 speculative extensibility가 분리되어 있다. Discovery로 찾은 모든
consumer와 compatibility claim이 checked-out source 또는 committed contract로 확인됐다.