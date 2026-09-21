---
name: review-persistence
description: "Review changed persistence behavior with measured evidence."
version: 0.1.0
author: "Amaazon project, Hermes Agent"
license: MIT
platforms: [linux, macos, windows]
metadata:
  hermes:
    tags: [code-review, persistence, jpa, postgresql, flyway]
    related_skills: [codebase-memory-mcp, semble-search]
---

# Persistence Review

Changed behavior의 storage correctness, transaction semantics와 database cost를 검토한다. ORM query count와
실제 database behavior를 분리하고, 측정되지 않은 성능 우려를 확정 finding으로 표현하지 않는다.

## Applicability

Entity/repository/query, association traversal, transaction, cache, pagination, migration, index, lock 또는
persistent state를 읽고 쓰는 call path가 있으면 `reviewed`다. Changed path에 repository 파일이 없더라도
application behavior가 persistence를 호출하면 적용된다. Call/dependency inspection으로 해당 경로가 없음을
입증한 경우에만 `not-applicable`이다.

## Code discovery 선택

어떤 repository/query가 behavior를 저장하거나 조회하는지 모르면
[`../run-review/references/code-discovery-guide.md`](../run-review/references/code-discovery-guide.md)의
**Semble branch**로 intent 기반 후보를 찾는다. Known application/repository symbol에서 entity, transaction,
event 또는 downstream query까지의 호출·dependency 경로가 필요하면 **Codebase Memory branch**를 사용한다.
Exact `@Query`, migration version, table/column, test ID와 configuration key는 local literal search를 사용한다.
Graph나 semantic 후보만으로 query count, transaction 또는 index 사실을 단정하지 않고 mapping, migration,
test와 실제 source에서 확정한다.

## 절차

1. Changed entry point에서 persistent read/write와 transaction 시작·종료까지 추적한다. Validation failure,
   retry, event publication과 exception path도 포함한다.
2. Repository public contract의 declared save/find/delete/custom query가 구현·test evidence와 일치하는지
   확인하고, domain/application layer가 JPA entity나 infrastructure detail에 불필요하게 결합하지 않는지 본다.
3. Correctness를 확인한다: aggregate invariant, write ordering, atomicity, rollback, isolation/locking,
   concurrent update, idempotency, cache consistency와 fresh persistence-context 결과.
4. Fetch behavior를 확인한다: N+1, required association traversal, projection/over-fetch, pagination의
   content/count query, sort 안정성, lazy access와 zero-query rejection path.
   Page/cursor test는 서로 다른 입력이 distinct하고 non-overlapping result를 만드는지, join fetch나
   collection join이 row multiplication과 잘못된 count를 만들지 않는지 확인한다.
5. Database behavior를 확인한다: expected row volume, predicate selectivity, join cardinality, index prefix,
   uniqueness/foreign key/check constraint, lock 범위와 migration order. Flyway migration은 forward 적용,
   mixed-version window, data backfill과 운영 rollback 위험을 구분한다.
6. 프로젝트의 기존 `QueryInspector` 또는 repository test base가 있으면 재사용한다. Setup data를 flush하고
   persistence context를 clear한 뒤 fresh operation을 실행하며, contract가 요구하는 field/association을
   실제로 traverse하고 즉시 query count를 assert한다. 별도 counter를 새로 요구하지 않는다.
7. 성능 판정은 query count, representative data, execution plan 또는 metric과 연결한다. 근거가 없으면
   필요한 측정과 가설을 `non_blocking_observations`에 기록한다. `EXPLAIN ANALYZE`처럼 query를 실행하는
   검증은 안전성과 승인이 없는 review 환경에서 임의 실행하지 않는다.

## Finding 기준

- correctness/data loss, race, transaction 또는 migration defect는 재현 evidence와 함께 blocking 후보다.
- Low query count는 optimality 증명이 아니며, high count도 workload/contract 없이 자동 defect가 아니다.
- SQL 문자열 모양이나 vendor-specific plan을 project contract 없이 강제하지 않는다.

관련 repository/database/integration test와 migration validation을 root에 요청한다. 모든 Gradle 작업은
`gradle-mcp`만 사용한다.

## 완료 기준

Persistence call path, transaction, fetch/query, database design과 migration concern을 각각 판정하고,
측정 사실·추정·미검증 영역을 분리한 axis result가 있다. Discovery 후보에서 persistence source와 test까지
이어지는 경로가 checked-out files로 확인됐다.