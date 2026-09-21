---
name: run-review
description: "Run five-axis aggregate review and return evidence."
version: 0.2.0
author: "Amaazon project, Hermes Agent"
license: MIT
platforms: [linux, macos, windows]
metadata:
  hermes:
    tags: [code-review, kanban, spring-modulith, quality-gate]
    related_skills: [review-spec, review-maintainability, review-persistence, review-architecture, review-evolution-compatibility]
requires_toolsets: [kanban]
---

# Review 실행

PM이 작성한 immutable aggregate Review contract를 모든 완료 checkpoint의 final committed 상태와
독립적으로 대조한다. 시작 전에
[`references/aggregate-review-contract.md`](references/aggregate-review-contract.md)와
[`references/axis-result-contract.md`](references/axis-result-contract.md)를 읽는다. Rubric의 출처와
의도적인 제외 범위를 감사할 때만
[`references/review-rubric-provenance.md`](references/review-rubric-provenance.md)를 읽는다. Repository
규칙과 검증기는 사실의 기준이며 Coder/PM 설명은 확인할 evidence일 뿐 결론이 아니다.

이 Skill은 review lifecycle과 통합만 소유한다. 다섯 leaf Skill이 축별 판단을 소유하며, root는
finding을 미리 알려 주거나 한 축의 결론으로 다른 축을 대체하지 않는다. 이 분리는 `/code-review`의
fixed-point 고정, Spec/품질 판단 분리, 독립 보고 원칙을 aggregate lifecycle에 맞게 적용한 것이다.

## 사용 시점

- Dispatcher가 `reviewer`에게 배정한 aggregate Review task를 시작했을 때 사용한다.
- 같은 generation의 corrective work 뒤 prior finding을 재검토할 때도 같은 절차를 사용한다.
- Coder same-card checkpoint, PR comment 분류, source 수정 또는 repository-wide architecture audit에는
  사용하지 않는다.

## 절차

### 1. Admission과 검토 기준 고정

1. `kanban_show`로 actual task ID, assignee=`reviewer`, `running` status, current run ID, immutable body,
   parent·child link, comments와 prior Review history를 읽는다.
2. Body가 closed `aggregate-review-card-v1`이고 complete behavior IDs, implementation card keys,
   inherited behavior IDs와 aggregate acceptance를 모두 가지는지 확인한다.
3. 참조 Impl task마다 body, latest terminal checkpoint metadata, task ID와 40자리 commit SHA를 읽는다.
   모든 key가 정확히 한 번 대응하고 task가 `done`인지 확인한다. 이전 blocking finding이 있으면 source
   Review task/run과 finding을 모두 수집한다.
4. 실제 workspace/cwd, branch, clean Git status와 `HEAD`를 읽는다. 각 checkpoint SHA가 현재 history에
   존재하고 검토할 final state에 포함되는지 확인한다. Dirty path, 누락 checkpoint, stale run 또는
   불일치는 수정·stash·reset하지 않고 `kanban_block(kind="needs_input")`으로 중단한다.

완료 기준: Review contract, child checkpoint, prior finding, actual workspace와 immutable commit 범위가
서로 일치한다.

### 2. 공통 Review context 고정

1. Aggregate acceptance와 각 child의 effective behavior·acceptance·required scenario를 coverage map으로
   만든다. 원본 requirement locator는 provenance로만 사용하며 제품 의미를 다시 기획하지 않는다.
2. Fixed base, reviewed HEAD, checkpoint SHA, commit range, changed paths, repository rules, 관련 requirement와
   prior finding을 `axis-result-contract`의 공통 입력으로 고정한다.
3. Changed diff와 관련 production source, caller/consumer, test, configuration, migration,
   `package-info.java`, architecture/ADR의 후보를 찾는다. Exact locator가 없을 때
   [`code-discovery-guide.md`](references/code-discovery-guide.md)를 읽는다. Behavior 위치 질문은 Semble,
   known symbol의 관계·영향 질문은 Codebase Memory branch를 선택하고 source에서 확정한다.

완료 기준: 모든 behavior·aggregate acceptance가 하나 이상의 검토 지점에 매핑되고, 다섯 축이 같은
immutable 입력을 받는다.

### 3. 다섯 축 분리 실행

다음 Skill을 나열된 순서로 하나씩 로드하고 실행한다. 순서는 누락 방지를 위한 orchestration 순서이며
판정 우선순위가 아니다.

1. `review-spec`
2. `review-maintainability`
3. `review-persistence`
4. `review-architecture`
5. `review-evolution-compatibility`

각 leaf마다 다음 protocol을 끝낸 뒤 다음 leaf로 이동한다.

1. `skill_view`로 정확한 leaf Skill 이름을 로드한다. Catalog description이나 root의 한 줄 요약으로 leaf
   절차를 대체하지 않는다.
2. `axis-result-contract`의 immutable 공통 입력에서 다음 invocation packet을 만든다.
   - `axis`, review task/run ID와 generation
   - fixed base, reviewed HEAD, ordered checkpoint key/task/SHA와 commit range
   - effective behavior, aggregate acceptance, scope exclusion과 changed paths
   - requirement·repository rule·architecture/ADR locator
   - 이 축의 source/test/configuration/migration 후보와 unresolved prior finding identity
3. Packet에는 Coder/PM의 결론을 넣지 않고, 다른 축의 finding은 입력으로 전달하지 않는다. 위치 후보는
   discovery hint로 표시하고 leaf가 source에서 확인하게 한다.
4. Leaf 절차와 완료 기준을 끝까지 수행해 `axis-result-contract` 형식의 raw result를 별도 axis block에
   기록한다. Leaf가 요청한 validator는 identity와 목적만 기록하고 root 검증 단계에서 실행한다.
5. 다음 leaf로 이동하기 전에 axis name, scope identity, applicability, examined evidence, verification,
   findings, observation, blocker와 conclusion basis가 모두 있는지 검사한다. 누락 필드는 추측해 채우지
   않고 같은 leaf를 보완한다.

같은 session에서는 위 packet·별도 result block으로 **논리적 독립성**만 보장한다. 실제 context 격리를
주장하지 않는다. 별도 agent/session은 capability와 lifecycle이 승인된 경우에만 사용한다. Shared scope
identity가 깨진 blocker면 즉시 중단하고, 특정 축에만 국한된 blocker면 나머지 축을 계속 수집하되 final
result를 완료하지 않는다. 각 축은 `reviewed`, `not-applicable`, `blocked` 중 하나이며, 단순
`no finding`은 `reviewed`다. 다섯 raw result가 모두 닫힌 뒤에만 중복 finding을 통합한다.

완료 기준: 다섯 축의 result가 모두 존재하고 각 result가 공통 입력 identity, evidence, verification,
finding 또는 근거 있는 no-finding/not-applicable을 포함한다. 하나라도 `blocked`면 aggregate result를
완료하지 않고 native task를 block한다.

### 4. Cross-cutting security baseline

다섯 축과 별도로 changed trust boundary를 다음 항목으로 검사한다.

1. Credential·token·password·개인정보가 source, log, response, configuration 또는 fixture에 노출되는가.
2. Authentication 이후 authorization와 resource ownership이 실제 실행 경로에서 강제되는가.
3. Untrusted input이 SQL, shell, path, template/HTML 또는 dynamic evaluation 경계에 안전하게 전달되는가.
4. Deserialization, file/path 처리, redirect·URL과 external call이 허용 범위와 실패 처리를 갖는가.
5. Error response와 logging이 내부 구조나 sensitive data를 노출하지 않는가.

Security 후보는 exact source path와 공격·오용 가능한 data flow 또는 재현 test가 있어야 한다. 제품의
authorization 의미가 card에 없으면 `decision-required`, 승인된 contract 확인이 필요하면
`context-required` 후보다. Evidence를 읽거나 재현할 수 없는 security concern은 승인으로 간주하지 않고
미검증 blocker로 남긴다. Reviewer는 scan 결과를 근거로 source를 수정하거나 auto-fix하지 않는다.

완료 기준: changed trust boundary별로 finding 또는 근거 있는 no-finding이 있고, 실행하지 않은 security
검증이 명시되어 있다.

### 5. 독립 검증과 통합

1. 축별 요청을 합쳐 card scenario와 finding을 재현하는 가장 가까운 test를 `gradle-mcp`로 실행한다.
2. Module seam이면 Modulith 구조 검증, persistence/migration이면 관련 database test를 추가한다.
   Aggregate acceptance에 필요한 경우 backend 전체 `test`를 실행한다.
3. Terminal·shell·IDE에서 Gradle을 실행하지 않는다. `gradle-mcp`가 unavailable하거나 환경 prerequisite가
   없으면 실행하지 않은 결과를 pass로 바꾸지 않고 정확한 blocker와 재개 조건을 남긴다.
4. 같은 `basis + observed_fact + affected behavior`인 finding만 합친다. 축이 다른 위험이나 서로 충돌하는
   판단은 각각 보존하고, preference·question·repository-wide debt는 blocking finding으로 승격하지 않는다.
5. 검증 과정에서 tracked file이 바뀌지 않았고 clean worktree와 reviewed HEAD가 유지되는지 다시 읽는다.

완료 기준: 각 검증의 task/test identity와 실제 pass/fail이 evidence에 연결되고 모든 축과 prior finding이
설명된다.

### 6. Result와 native completion

1. Finding을 contract의 `basis`, `observed_fact`, `evidence`, `impact`로 작성한다. Evidence에는 exact
   `path:line`, test/validator identity 또는 checkpoint SHA를 넣는다.
2. 기존 동작 수정이 필요하면 `correction-required`, 승인 context가 부족하면 `context-required`, 사용자
   정책 결정이 필요하면 `decision-required`를 사용한다. 이전 finding이 실제로 해소된 경우에만 source
   Review task/finding을 가리키는 `resolved`를 작성한다.
3. 새 blocking finding이 없고 모든 prior finding이 resolved되었을 때만 `result: approved`로 한다.
   Blocking finding이 있으면 `changes-required`다. Review prerequisite 문제는 incomplete terminal result로
   꾸미지 말고 task를 block한다.
4. Current native run ID와 canonical `aggregate-review-result-v1` metadata로 `kanban_complete`를 호출한다.
   Body, comments 또는 child task는 수정하지 않는다.
5. `kanban_show`로 task `done`, terminal run, metadata와 result가 저장됐는지 read-back한다.

완료 기준: PM validator가 소비할 canonical result가 latest terminal run에 있고 native task가 `done`이며
repository는 clean하다.

## 금지 사항

- Source, test, migration, configuration, 문서, card body를 편집하지 않는다.
- Commit, push, merge, task graph mutation, finding routing과 Summary promotion을 수행하지 않는다.
- Coder self-verification이나 PM checkpoint pass를 독립 review evidence로 대체하지 않는다.
- Review 도중 HEAD/checkpoint가 바뀌면 기존 결론을 폐기하고 admission부터 다시 한다.

## 보고

```text
status: approved | changes-required | blocked
review_task/run:
checkpoint_shas:
axis_results:
verification:
findings:
unverified_or_blocker:
native_readback:
next_owner: project-manager
```