---
name: run-review
description: "Run five-axis aggregate review and return evidence."
version: 0.3.0
author: "Amaazon project, Hermes Agent"
license: MIT
platforms: [linux, macos, windows]
metadata:
  hermes:
    tags: [code-review, kanban, spring-modulith, quality-gate]
    related_skills: [review-spec, review-maintainability, review-persistence, review-architecture, review-evolution-compatibility, codebase-memory-mcp, semble-search]
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
Aggregate Review의 axis isolation, compact packet, durable progress와 resume은
[`references/bounded-context-review.md`](references/bounded-context-review.md)가 소유한다.

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
2. Body가 closed `aggregate-review-card-v2`이고 40자리 `baseline_sha`, complete behavior IDs,
   implementation card keys, inherited behavior IDs, aggregate acceptance와 explicit `scope_exclusions` 목록을
   모두 가지는지 확인한다.
3. 참조 Impl task마다 body, latest terminal checkpoint metadata, task ID와 40자리 commit SHA를 읽는다.
   모든 key가 정확히 한 번 대응하고 task가 `done`인지 확인한다. 이전 blocking finding이 있으면 source
   Review task/run과 finding을 모두 수집한다.
4. 실제 workspace/cwd, branch, clean Git status와 `HEAD`를 읽어 `reviewed_head_sha`로 고정한다. Card의
   `baseline_sha`와 HEAD가 실제 commit이고 baseline이 HEAD의 ancestor인지 확인한다. 각 checkpoint SHA가
   `baseline_sha..reviewed_head_sha` history에 존재하고 final state에 포함되는지도 확인한다. Dirty path,
   누락 checkpoint, stale run 또는 불일치는 수정·stash·reset하지 않고
   `kanban_block(kind="needs_input")`으로 중단한다.
5. Admission이 닫히면 `aggregate-review-progress-v1` 시작 comment를 남긴다. Comment에는 actual task/run,
   baseline, reviewed HEAD와 고정 TODO ID 전체를 넣고 `admission=done`, `diff-inventory=in-progress`, 나머지는
   `pending`으로 기록한다. Source symbol 목록을 card나 comment에 미리 복제하지 않는다.

완료 기준: Review contract, child checkpoint, prior finding, actual workspace와 immutable
`baseline_sha..reviewed_head_sha` 범위가 서로 일치하고 시작 TODO comment가 read-back된다.

### 1.5 Problem escalation to PM

Review 중 입력·checkpoint·workspace·tool·environment 또는 공개 계약이 불완전하여 결론을 낼 수 없으면,
이를 일반 finding이나 승인으로 위장하지 않고 PM review 요청으로 올린다.

1. 현재 Review task에 관찰 사실, 영향, 재현 근거와 PM에게 필요한 확인·조치를 comment로 남긴다. Comment에는
   `PM review requested`를 명시한다.
2. PM의 판단·환경 조치·계약 확인이 필요하면 native `kanban_block(kind="needs_input")`으로 Review task를
   block한다. `needs_input`은 별도 status가 아니라 `blocked` task의 typed reason이다.
3. 사용자 정책·authorization·consistency·error/public contract 판단이 필요하다는 finding은
   `decision-required`로 명시한다. Reviewer는 정책을 결정하거나 Decision card를 임의로 만들지 않고,
   PM이 PM-owned blocked Decision으로 routing하도록 한다.
4. 정상적인 구현 결함은 Review 결과 `changes-required`로 남긴다. `request-review`는 문제 알림용으로
   사용하지 않으며, Coder의 완료 구현을 검토 lane으로 넘기는 의미만 유지한다.

### 2. 공통 Review context 고정

1. Aggregate acceptance와 각 child의 effective behavior·acceptance·required scenario를 coverage map으로
   만든다. 원본 requirement locator는 provenance로만 사용하며 제품 의미를 다시 기획하지 않는다.
2. `git diff --name-status --find-renames baseline_sha reviewed_head_sha`와 전체 diff로 모든 changed path를
   산출한다. 각 path를 `in-scope | supporting | unexpected | excluded-with-basis`로 분류한다. Scope exclusion은
   제품 범위를 제한할 뿐 실제 changed path를 숨기는 근거가 아니다. `unexpected`는 finding 또는 PM 확인
   대상으로 disposition한다.
3. Baseline, reviewed HEAD, checkpoint SHA, 전체 commit range, changed-path inventory, repository rules,
   관련 requirement와 prior finding을 `axis-result-contract`의 공통 입력으로 고정한다.
4. Changed diff와 관련 production source, caller/consumer, test, configuration, migration,
   `package-info.java`, architecture/ADR의 후보를 찾는다. Exact locator가 없을 때
   [`code-discovery-guide.md`](references/code-discovery-guide.md)를 읽는다. Behavior 위치 질문은 Semble,
   known symbol의 관계·영향 질문은 Codebase Memory branch를 선택하고 source에서 확정한다. external_system
   behavior마다 producer public contract, consumer port, adapter와 실제 unavailable/error path를 inventory에
   포함한다. placeholder adapter 또는 public contract 부재를 Coder 설명·mock만으로 pass 처리하지 않는다.
5. Diff inventory가 닫히면 두 번째 progress comment를 남겨 `diff-inventory=done`과 changed/unexpected path
   수를 기록하고 `spec=in-progress`로 전환한다.

완료 기준: 모든 changed path가 disposition되고 모든 behavior·aggregate acceptance가 하나 이상의 검토
지점에 매핑되며, 다섯 축이 같은 immutable 입력을 받는다.

### 3. 다섯 축의 bounded-context 실행

`bounded-context-review.md`의 **one axis per session** 절차를 적용한다. Root는 현재 Review card 하나와
terminal result 하나를 유지하지만, 축별 source discovery와 판단을 하나의 긴 worker conversation에 누적하지
않는다.

1. `review-spec`, `review-maintainability`, `review-persistence`, `review-architecture`,
   `review-evolution-compatibility` 순서로 한 axis씩 `delegate_task`에 위임한다. 각 child는 fresh isolated
   axis session이다. **항상 한 번의 `delegate_task` 호출에 child 하나만** 넣고, 그 child의 terminal result를
   받아 axis comment를 read-back한 뒤에만 다음 axis를 위임한다. 여러 axis를 한 호출의 `tasks` 배열에 넣거나,
   실행 중인 child가 있는 동안 다음 axis를 dispatch하지 않는다. `delegation.max_concurrent_children=1`은
   병렬 실행을 막는 runtime safety cap이며, parent의 순차 dispatch 절차를 대체하지 않는다.
2. Root는 `axis-result-contract`의 immutable 공통 입력에서 compact invocation packet만 만든다. Packet에는
   task/run identity, fixed base/head, checkpoints, behavior/acceptance/scope exclusions, axis locator와 prior
   finding identity만 둔다. 전체 `kanban_show`, raw diff, raw tool output, full file content, 다른 axis result,
   Coder/PM conclusion은 child context에 넣지 않는다.
3. Child는 정확한 leaf Skill을 `skill_view`로 load하고 read-only 조사만 수행한다. Kanban mutation, Gradle
   execution, source edit, commit 및 다른 axis 재검토는 child 범위 밖이다. Leaf가 요청한 validator는 identity와
   목적만 반환하고 root 검증 단계에서 실행한다.
4. Child가 반환한 `axis-result-contract`를 Root가 검사한다. axis name, scope identity, applicability,
   examined evidence, verification, finding/observation, blocker와 conclusion basis가 누락되면 Root가 추측해
   채우지 않는다. 같은 axis를 새 fresh isolated axis session에서 한 번만 보완한다.
5. 유효한 raw result마다 Root는 concise `aggregate-review-axis-v1` comment를 한 번 남기고 read-back한다.
   이 comment는 durable intermediate evidence이며 canonical result metadata에 복사하지 않는다.
6. 재개 시 Root는 같은 base/head scope의 `aggregate-review-axis-v1` comments를 먼저 읽어 완료 축을
   재사용하고, 없는 축만 fresh session으로 실행한다. Root가 죽거나 context capacity가 부족해도 기존 축을
   처음부터 다시 읽거나 실행하지 않는다.

Delegation capability 또는 immutable scope가 없거나 child가 capacity/tool/source blocker를 반환하면
`PM review requested` comment와 native `kanban_block(kind="needs_input")`으로 중단한다. 정상 axis 경계는
block 사유가 아니다. 각 축은 `reviewed`, `not-applicable`, `blocked` 중 하나이며, 단순 `no finding`은
`reviewed`다. 다섯 raw result가 모두 닫힌 뒤에만 중복 finding을 통합한다.

다섯 축이 닫히면 세 번째 progress comment로 해당 TODO를 모두 `done`, `security=in-progress`로 기록한다.

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
6. 검증이 닫히면 마지막 progress comment로 `verification=done`, `result-readback=in-progress`를 기록한다.

완료 기준: 각 검증의 task/test identity와 실제 pass/fail이 evidence에 연결되고 모든 축과 prior finding이
설명된다.

### 6. Result와 native completion

1. Finding을 contract의 `basis`, `observed_fact`, `evidence`, `impact`로 작성한다. Evidence에는 exact
   `path:line`, test/validator identity 또는 checkpoint SHA를 넣는다.
2. 기존 동작 수정이 필요하면 `correction-required`, 승인 context가 부족하면 `context-required`, 사용자
   정책 결정이 필요하면 `decision-required`를 사용한다. 이전 finding이 실제로 해소된 경우에만 source
   Review task/finding을 가리키는 `resolved`를 작성한다. 동일 결함이 계속되면 새 blocking finding의
   `continues`로 prior identity를 정확히 이어 간다.
3. 새 blocking finding이 없고 모든 prior finding이 resolved되었을 때만 `result: approved`로 한다.
   Blocking finding이 있으면 `changes-required`다. Review prerequisite 문제는 incomplete terminal result로
   꾸미지 말고 task를 block한다.
4. Current native run ID, card `baseline_sha`와 고정한 `reviewed_head_sha`를 포함한 canonical
   `aggregate-review-result-v2` metadata로 `kanban_complete`를 호출한다.
   Body, comments 또는 child task는 수정하지 않는다.
5. `kanban_show`로 task `done`, terminal run, metadata와 result가 저장됐는지 read-back한다.

완료 기준: PM validator가 소비할 canonical result가 latest terminal run에 있고 native task가 `done`이며
repository는 clean하고 HEAD가 `reviewed_head_sha`와 같다.

## 금지 사항

- Source, test, migration, configuration, 문서, card body를 편집하지 않는다.
- Commit, push, merge, task graph mutation, finding routing과 Summary promotion을 수행하지 않는다.
- Coder self-verification이나 PM checkpoint pass를 독립 review evidence로 대체하지 않는다.
- Review 도중 HEAD/checkpoint가 바뀌면 기존 결론을 폐기하고 admission부터 다시 한다.

## 보고

```text
status: approved | changes-required | blocked(native task only)
review_task/run:
checkpoint_shas:
axis_results:
verification:
findings:
unverified_or_blocker:
native_readback:
next_owner: project-manager
```