---
name: review-maintainability
description: "Review changed code for concrete maintenance costs."
version: 0.1.0
author: "Amaazon project, Hermes Agent"
license: MIT
platforms: [linux, macos, windows]
metadata:
  hermes:
    tags: [code-review, maintainability, refactoring]
    related_skills: [codebase-memory-mcp, semble-search]
---

# Maintainability Review

Changed behavior와 그 영향 closure에서 실제 change cost를 찾는다. `/code-review`의 smell baseline을
heuristic으로 사용하되 documented repository rule이 우선하며, smell 이름 자체는 위반 증거가 아니다.

`run-review/references/axis-result-contract.md`의 invocation packet을 소비하고 raw axis result를
`run-review`에 반환한다. PM에게 직접 handoff하지 않는다.

## Code discovery 선택

중복 policy, existing utility 또는 유사 implementation의 위치를 모르면
[`../run-review/references/code-discovery-guide.md`](../run-review/references/code-discovery-guide.md)의
**Semble branch**를 사용한다. Known changed symbol의 caller/callee, fan-out, dependency path와 blast radius가
필요하면 **Codebase Memory branch**를 사용한다. Exact symbol occurrence나 literal flag는 local search를
사용한다. Similar-looking code는 source에서 responsibility와 contract가 실제로 같은지 확인한 뒤 reuse
evidence로 채택한다.

## 절차

1. Diff의 책임과 change reason을 caller, callee, sibling implementation과 함께 읽는다. 한 behavior 수정이
   흩어진 conditional·mapping·validation을 함께 바꾸게 하는지 확인한다.
2. 다음 후보에 삭제·변경 실험을 적용한다.
   - duplicated policy 또는 같은 logic shape
   - mixed responsibility와 divergent change
   - feature envy, message chain과 caller knowledge
   - shallow/pass-through abstraction과 middle man
   - data clump, primitive obsession과 repeated switch
   - shotgun surgery, refused bequest와 speculative generality
3. 후보마다 구체적인 비용을 입증한다: 변경해야 할 위치 수, 누출된 ordering/invariant, 반복된 policy,
   불필요한 public surface 또는 삭제해도 behavior가 같은 logic.
4. Reuse 후보는 repository의 existing utility, policy 또는 sibling implementation을 실제로 찾아 exact
   locator를 제시한다. “공통 helper가 있을 것”이라는 추정은 finding이 아니다.
5. 제거·축소 후보는 surrounding comment, caller와 필요한 경우 Git history로 존재 이유를 확인한다.
   목적을 확인하지 못한 compatibility shim, migration 단계 또는 isolation wrapper는 낮은 confidence의
   관찰로 남기고 제거를 요구하지 않는다.
6. 가장 작은 correction을 제안한다. Existing code를 단순화할 수 있으면 새 pattern/interface를 요구하지
   않는다. 실제 variation이나 두 번째 adapter가 없으면 extension seam을 요구하지 않는다.
7. Test가 implementation detail에 과도하게 결합하거나 중요한 behavior를 interface를 통해 검증하지
   못하는지 확인하되 test style preference와 defect를 구분한다.

## 관점과 correction risk

- **Reuse:** 기존 기능을 중복 구현했는가.
- **Quality:** redundant state, copy-paste variation, leaky abstraction 또는 stringly-typed policy가 있는가.
- **Efficiency:** 반복 I/O/query/computation, unbounded growth 또는 silent failure가 maintenance cost를
  만드는가. 측정되지 않은 micro-performance preference는 제외한다.
- **Altitude:** shared mechanism의 원인을 고치지 않고 한 caller에 special case, flag 또는 wrapper를
  덧댔는가. Deliberate compatibility/migration seam과 구분한다.

각 finding에 `confidence: high | medium | low`와 correction risk를 붙인다.

- `SAFE`: behavior 불변이 evidence로 입증된 삭제·중복 제거
- `CAREFUL`: semantics 보존 의도지만 targeted regression test가 필요한 변경
- `RISKY`: public contract, persistence, concurrency 또는 shared infrastructure에 영향을 줄 수 있는 변경

Risk는 finding severity가 아니라 PM/Coder가 correction scope를 정하기 위한 정보다. Reviewer가 직접
적용하지 않는다.

## Finding이 아닌 신호

다음 값만으로 finding을 만들지 않는다.

- 파일·메서드·클래스 또는 parameter 개수
- `for` loop나 Stream 선택
- constructor collaborator 개수
- 익숙하지 않은 naming/style
- 미래에 바뀔 수도 있다는 가능성

Line count나 smell은 조사 시작점이다. Finding에는 concrete change amplification, low cohesion, duplicated
policy, caller knowledge, shallow interface 또는 behavior-preserving deletion evidence가 필요하다.

## 범위와 판정

Changed code와 이를 이해하는 데 필요한 direct caller/callee/sibling까지 검토한다. unrelated legacy debt는
`non_blocking_observations`에만 두고 현재 change가 새로 만들거나 악화했거나 contract 수행을 위협할 때만
blocking 후보가 된다. Code change가 없고 generated/config-only change임을 확인한 경우 `not-applicable`이
가능하다.

## 완료 기준

모든 changed responsibility를 검토했고, 각 finding이 측정 가능한 maintenance impact, confidence,
correction risk와 최소 correction을 가지며, preference와 pre-existing debt가 분리되어 있다. Discovery
후보의 responsibility와 caller impact가 checked-out source로 확인됐다.