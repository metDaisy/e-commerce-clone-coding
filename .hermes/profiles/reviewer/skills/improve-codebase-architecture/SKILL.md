---
name: improve-codebase-architecture
description: "Analyze scoped code for deeper modules and cleaner seams."
version: 0.1.0
license: MIT
platforms: [linux, macos, windows]
author: "Amaazon project, Hermes Agent; adapted from Matt Pocock"
metadata:
  hermes:
    tags: [architecture, deep-modules, seams, review]
    related_skills: [codebase-memory-mcp, semble-search]
    provenance: "https://github.com/mattpocock/skills"
---

# Improve Codebase Architecture

Deep module, interface, seam, adapter, leverage와 locality vocabulary로 architecture friction을 조사한다.
Upstream `improve-codebase-architecture`의 exploration/deletion-test 접근을 Amaazon의 read-only Reviewer와
Spring Modulith 계약에 맞게 제한한 project distribution variant다.

## Mode를 먼저 고정한다

### `checkpoint`

Aggregate Review의 기본 mode다. Caller가 제공한 requirement 영향 closure만 조사한다. Diff 밖의 code도
caller/consumer, dependency, module interface와 contract를 확인하기 위해 읽을 수 있지만 unrelated module,
Git-history hotspot 또는 전체 repository 후보 탐색으로 넓히지 않는다.

### `repository-audit`

PM이 별도 architecture-audit task로 명시적으로 승인한 경우만 사용한다. Domain/module map, ADR과 충분한
Git history를 읽고 반복 변경 hotspot, cross-module coupling과 shallow interface를 repository 수준에서
탐색한다. Aggregate Review 중 스스로 이 mode로 전환하지 않는다.

Mode 또는 scope가 없으면 추측하지 말고 `blocked`를 반환한다.

## 공통 vocabulary

- **Module:** 하나의 interface 뒤에 behavior를 숨기는 단위
- **Interface:** signature뿐 아니라 caller가 알아야 할 invariant, ordering, error와 performance 특성
- **Depth:** 작은 interface가 제공하는 behavior leverage
- **Seam:** caller 수정 없이 behavior를 바꿀 수 있는 위치
- **Adapter:** seam의 interface를 만족하는 concrete role
- **Leverage:** caller가 작은 interface로 얻는 capability
- **Locality:** change·knowledge·verification이 한곳에 모이는 정도

## 조사 절차

1. Scope 안의 domain term, architecture 문서, ADR, `package-info.java`와 executable verification을 읽는다.
2. Changed/target symbol에서 caller, consumer, dependency와 sibling implementation을 추적해 module interface와
   hidden implementation을 그린다.
3. 다음 friction을 evidence로 확인한다.
   - 한 concept 이해나 수정에 많은 module을 오가야 한다.
   - Interface가 implementation만큼 복잡하거나 pass-through다.
   - Caller가 ordering, flag, data shape와 error rule을 알아야 한다.
   - Tests가 useful interface 대신 internal call sequence를 mock한다.
   - Tightly coupled module이 seam을 가로질러 internal detail을 누출한다.
4. **Deletion test:** 의심 module을 없앴을 때 complexity가 사라지면 shallow 후보이고, 여러 caller로 다시
   퍼지면 module이 depth와 locality를 제공한다.
5. **Variation test:** 한 adapter는 hypothetical seam이다. 두 adapter, external integration 또는 반복된
   policy variation이 있을 때만 새 seam을 강하게 권한다.
6. Candidate마다 exact files/symbols, observed friction, affected caller/test, smallest deepening direction과
   recommendation strength(`strong | worth-exploring | speculative`)를 기록한다.

## Reviewer 제한

- Aggregate Review에서는 HTML report, interactive grilling, source/doc/ADR mutation을 수행하지 않는다.
- Pattern 이름, line count와 SOLID slogan만으로 finding을 만들지 않는다.
- `checkpoint`의 unrelated debt는 현재 verdict가 아니라 별도 audit 후보로 반환한다.
- Existing architecture/ADR를 바꾸려면 Reviewer가 결정하지 않고 `decision-required` 후보로 반환한다.

## 완료 기준

명시된 mode와 scope 안에서 module/interface/seam/adapter를 evidence로 검토했고, current regression,
pre-existing debt와 speculative candidate를 분리했다. `checkpoint` mode 결과는 scoped candidate/evidence
set으로 `review-architecture`에 반환하며, 그 leaf만 axis result와 verdict candidate로 변환한다.