---
name: implement
description: 승인된 backend Impl card를 구현하고 검증할 때 사용한다.
version: 0.4.0
author: "Amaazon project, Hermes Agent"
license: MIT
platforms: [linux, macos, windows]
metadata:
  hermes:
    tags: [implementation, backend, java, testing, cleanup]
    related_skills: [codebase-memory-mcp, semble-search]
requires_toolsets: [file, terminal]
---

# 구현

Admission을 통과한 `backend-implementation-card-v1` 하나를 검증된 최소 backend code 변경으로 만든다.
`run-impl-card`는 admission, Kanban 전이, blocker routing과 handoff를 소유하고, 이 Skill은 admission과
handoff 사이의 coding loop를 소유한다.

## 사용 시점

- `run-impl-card`가 initial run 또는 changes-requested run을 admission한 뒤 사용한다.
- Java/Spring backend production code, test, configuration, migration과 직접 필요한 code artifact에 사용한다.
- Requirement 재해석, card 작성, 문서 편집, commit 또는 Kanban task 완료에는 사용하지 않는다.

## 사전 조건

- 활성 native task가 `coder`에게 배정되어 있고 status가 `running`이다.
- Immutable card와 rework의 correlated change-request가 `run-impl-card` admission을 통과했다.
- Worktree가 initial-run 또는 rework dirty-path 규칙을 만족한다.
- `gradle-mcp`가 `gradle`과 `query_build`를 제공한다. 사용할 수 없으면 `terminal`로 Gradle을 실행하지
  않고 block한다.

## 실행 모드

- **Initial run:** 검증된 card의 모든 behavior와 acceptance를 구현한다.
- **Changes-requested rework:** correlated finding마다 정확한 `path`, `symbol`, `allowed_scope`만 수정한다.
  원래 card는 invariant를 보존하고 전체 verification contract를 결정하는 용도로만 사용한다. 관련 없는
  card code를 다시 mapping하거나 개선·단순화하지 않는다. Finding이 더 좁은 test를 지정하더라도
  card가 요구하는 모든 focused verification과 full backend verification을 다시 실행한다.

## 보조 Skill과 자료 선택

구현 map을 작성하기 전에 `docs/backend-development-guide.md`와
`docs/backend-test-guide.md`에서 변경에 해당하는 규칙을 읽는다. 전자는 Java/Spring/Modulith 구현
convention을, 후자는 test level·fixture·assertion·검증 convention을 소유한다. `docs/architecture.md`는
module seam, event 또는 계층 방향을 변경할 때 추가로 읽는다.

`related_skills`는 항상 load하는 목록이 아니다. 아래 조건이 성립할 때 해당 Skill 하나를 load하고,
결과를 현재 checkout의 repository file로 확인한다.

| 조건 | 먼저 사용할 자료 | 완료 기준 |
|---|---|---|
| 정확한 symbol, path 또는 오류 문구를 안다 | `search_files`, `read_file` | definition과 모든 관련 usage를 직접 확인했다. |
| 구현 위치나 유사 behavior가 불명확하다 | `semble-search` Skill | 의미 기반 후보를 찾고 실제 file과 가장 가까운 test를 읽었다. |
| public seam, caller/callee 또는 cross-domain 영향이 있다 | `codebase-memory-mcp` Skill | index 최신성을 확인하고 graph의 영향 경로를 실제 source와 대조했다. |
| Spring 또는 library API가 불명확하다 | project manifest, repository의 기존 usage와 checked-out source | 실제 project version에서 동작하는 local pattern을 확인했거나 문서 접근 필요를 blocker로 기록했다. |
| Java test 구조가 불명확하다 | `docs/backend-test-guide.md`와 가장 가까운 기존 test | project fixture, assertion과 test level을 확인했다. |

같은 넓은 질문을 여러 탐색 Skill에 반복하지 않는다. 보조 MCP가 없거나 index가 오래됐으면
`search_files`와 `read_file`로 계속 조사한다. `run-impl-card`는 이 Skill을 호출한 lifecycle owner이므로
여기서 역으로 load하지 않고, 구현 결과만 반환한다.

## 절차

### 1. 구현 map 작성

1. Card의 모든 entry point와 가장 가까운 test를 읽은 다음 `search_files`와 `read_file`로 definition,
   caller, consumer, DTO, mapping, adapter, repository, event와 `package-info.java` boundary를 추적한다.
2. 위 선택표에서 현재 질문에 맞는 가장 좁은 탐색 경로 하나를 선택하고, 도구 결과를 repository
   file에서 직접 확인한다.
3. Initial run에서는 각 `effective_behavior.id`와 acceptance criterion을 code seam과 focused
   verification ID에 연결한다. Rework에서는 card의 전체 verification map을 유지하면서 검증된 finding만
   수정 seam에 연결한다.

완료 조건: 모든 initial-run behavior 또는 rework finding에 구체적인 수정·test 위치가 있거나, 누락된
contract 또는 결정 사항을 정확히 기록하고 task를 block했다.

### 2. 다음 behavior의 실패 test 확립

1. 구현 map에서 아직 검증되지 않은 가장 작은 behavior 하나를 선택한다. Initial run에서는 card가
   요구한 success, rejection, failure, boundary와 persistence scenario를 빠짐없이 순서대로 선택하고,
   rework에서는 검증된 finding이 요구하는 coverage만 선택한다.
2. Production code보다 먼저 누락된 behavior를 public contract 또는 관찰 가능한 seam으로 재현하는
   가장 작은 test를 추가한다. Private method나 내부 구조를 고정하지 않고, 별도 test framework 대신
   기존 fixture와 `docs/backend-test-guide.md`의 convention을 재사용한다.
3. 새 test 또는 가장 가까운 focused test만 `gradle-mcp`로 실행해 예상한 behavior gap 때문에 실패하는지
   확인한다.
   Seam에서 의미 있는 red test를 만들 수 없다면 이유를 기록하고, card의 verification contract가 계속
   결정론적 coverage를 제공할 때만 진행한다.

완료 조건: 다음 behavior 하나의 gap을 관련 failure로 입증했거나, red test가 적합하지 않은 이유와
이를 대체하는 결정론적 coverage를 명시했다.

### 3. 완전한 최소 변경 구현

1. 현재 red test를 green으로 만드는 범위에서, initial run은 caller별 symptom이 아니라 card가 정의한
   behavior를 바로잡는 가장 깊고 올바른 seam을 수정한다. Rework에서도 caller별 symptom이 아니라
   finding이 정의한 정확한 defect를 수정한다.
2. Compilation과 확인된 contract가 요구하는 변경만 caller, DTO, mapping, adapter, persistence,
   configuration, migration과 test에 전파한다.
3. Card가 명시적으로 변경하지 않는 한 public contract, transaction semantics, module boundary,
   exception convention과 기존 style을 유지한다.
4. Card에 없는 speculative abstraction, 관련 없는 defect repair, 광범위한 refactor 또는 compatibility
   behavior를 추가하지 않는다. Test를 약화하거나 제거하지 않는다.

완료 조건: production과 test 변경으로 모든 changed path를 설명할 수 있고, 어떤 수정도 임의로 만든
product decision에 의존하지 않는다.

### 4. 긴밀한 검증 loop 실행

1. Failure message와 call/data path를 따라 root cause를 확인하고, 한 번에 하나의 falsifiable cause만
   최소 변경으로 검증한다. 가장 가까운 affected test를 `gradle-mcp`로 실행해 green이 될 때까지
   반복하며 여러 추측성 수정을 쌓지 않는다.
2. Initial run에 아직 구현하지 않은 behavior가 있으면 2단계로 돌아가 다음 behavior의
   RED→GREEN vertical slice를 수행한다. 모든 test를 먼저 쌓은 뒤 production code를 한꺼번에 작성하지
   않는다. Rework에서는 allowed finding scope 밖으로 cycle을 넓히지 않는다.
3. Card의 모든 `focused_verification`을 지정된 test와 required scenario로 `gradle-mcp`에서 실행한다.
4. 통과한 focused result를 coverage 대상인 모든 acceptance ID와 연결한다. Compilation이나 인접 test를
   지정된 scenario 대신 사용하지 않는다.

완료 조건: 모든 focused verification과 required scenario가 `gradle-mcp`에서 통과했다.

### 5. 범위가 제한된 단순화 수행

Initial run에서는 현재 task diff와 최소한의 주변 code만 검토한다. Rework에서는 검증된 finding 때문에
변경한 code만 검토하고 관련 없는 단순화는 모두 건너뛴다.

- **재사용:** behavior가 같다면 새 중복을 이미 검증된 local helper로 대체한다.
- **품질:** 불필요한 state, 불필요한 nesting, copy-paste variation과 code를 반복 설명하는 comment를 제거한다.
- **효율:** diff가 새로 만들었거나 card가 요구한 경우에만 반복 작업 또는 불필요한 query를 제거한다.
- **추상화 수준:** card scope 안에서 유지된다면 얕은 special case를 올바른 shared-seam 수정으로 바꾼다.

광범위한 cleanup을 시작하거나 behavior를 변경하거나 public contract 이름을 바꾸거나 관련 없는 file로
범위를 넓히지 않는다. 단순화 뒤 affected focused test를 다시 실행한다. Behavior 보존을 증명할 수 없는
단순화는 되돌린다.

완료 조건: scope 안에 실질적인 단순화 항목이 남지 않았고 focused verification이 계속 green이다.

### 6. 전체 backend 검증 실행

Focused verification이 green인 뒤에만 card의 정확한 `full_backend_verification` task를 `gradle-mcp`로
실행한다. 실패하면 원인이 scope 안인지 판정한다.

- Scope 안의 regression을 수정한 다음 affected focused verification과 full task를 반복한다.
- External prerequisite, policy gap, 예상하지 못한 기존 failure 또는 scope 밖 수정이 필요하면 block한다.

완료 조건: full backend task가 통과했거나, 관찰한 evidence와 recovery owner를 기록하고 task를 block했다.
실패했거나 실행하지 않은 full verification을 통과한 것으로 handoff하지 않는다.

### 7. 자체 검토 후 workflow로 반환

1. Git status와 전체 diff를 읽는다. Behavior, security, module boundary, persistence, migration 순서,
   test와 의도하지 않은 generated file 또는 document file을 확인한다.
2. Changed path가 정확히 task code artifact인지, 모든 card behavior, acceptance와 scenario에 통과 evidence가
   있는지 확인한다. 문서를 수정하지 않고 documentation impact만 보고한다.
3. Canonical handoff를 작성하고 PM checkpoint를 요청하도록 제어를 `run-impl-card`에 반환한다.

완료 조건: diff가 scope 안에 있고 review 가능하며 두 verification 단계가 통과했고 unresolved risk를
숨기지 않았다. Coder는 stage, commit, push 또는 card 완료를 수행하지 않았다.

## 추측하지 말고 Block할 조건

구현에 명시되지 않은 business, authorization, transaction, consistency, API, event 또는 error contract가
필요하거나, 예상하지 못한 dirty path가 있거나, card가 repository architecture와 충돌하거나, 필수
verification을 실행할 수 없으면 `run-impl-card`를 통해 block한다. Workspace를 보존하고 reset, stash,
clean, validator 우회 또는 약화된 assertion을 recovery 수단으로 사용하지 않는다.

## 검증

`run-impl-card`로 반환하기 전에 다음을 확인한다.

- 모든 effective behavior와 acceptance criterion이 구현 code와 test에 연결됐다.
- 모든 필수 focused scenario가 `gradle-mcp`에서 통과했다.
- Card의 full backend task가 `gradle-mcp`에서 통과했다.
- 전체 diff에 document 또는 관련 없는 path가 없다.
- 단순화가 card contract를 변경하지 않았다.
- Commit, push 또는 native completion을 수행하지 않았다.
