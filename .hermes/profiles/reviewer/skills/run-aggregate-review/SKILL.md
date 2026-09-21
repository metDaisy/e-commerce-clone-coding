---
name: run-aggregate-review
description: "Review aggregate checkpoints and return evidence."
version: 0.1.0
author: "Amaazon project, Hermes Agent"
license: MIT
platforms: [linux, macos, windows]
metadata:
  hermes:
    tags: [code-review, kanban, spring-modulith, quality-gate]
    related_skills: [codebase-memory-mcp, semble-search]
requires_toolsets: [kanban]
---

# Aggregate Review 실행

PM이 작성한 immutable aggregate Review contract를 모든 완료 checkpoint의 final committed 상태와
독립적으로 대조한다. 시작 전에
[`references/aggregate-review-contract.md`](references/aggregate-review-contract.md)를 읽는다.
Repository 규칙과 검증기는 구조 제약의 기준이며 Coder/PM의 설명은 확인할 evidence일 뿐 결론이 아니다.

## 사용 시점

- Dispatcher가 `reviewer`에게 배정된 aggregate Review task를 시작했을 때 사용한다.
- 같은 generation의 corrective work 뒤 prior finding을 재검토할 때도 같은 절차를 사용한다.
- Coder same-card PM checkpoint, PR comment 분류 또는 source 수정에는 사용하지 않는다.

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
   존재하고 검토할 final state에 포함되는지 확인한다. Dirty tracked/untracked path, 누락 checkpoint,
   stale run 또는 불일치는 수정·stash·reset하지 않고 `kanban_block(kind="needs_input")`으로 중단한다.

완료 기준: Review contract, child checkpoint, prior finding, actual workspace와 immutable commit 범위가
서로 일치한다.

### 2. 최소 관련 맥락 조사

1. Aggregate acceptance와 각 child의 effective behavior·acceptance·required scenario를 coverage map으로
   만든다. 원본 requirement locator는 provenance로만 사용하며 제품 의미를 다시 기획하지 않는다.
2. Changed commit의 diff와 관련 production source, caller/consumer, test, configuration, migration,
   `package-info.java`, architecture/ADR를 필요한 범위에서 직접 읽는다.
3. 위치가 불명확할 때만 `codebase-memory-mcp` 또는 `semble-search`로 후보를 찾고 source에서 확정한다.

완료 기준: 모든 behavior·aggregate acceptance가 하나 이상의 source/test 검토 지점에 매핑되며,
검토하지 못한 범위는 명시적 blocker다.

### 3. 다섯 축 독립 검토

각 축의 관찰을 먼저 분리해서 기록하고 중복 finding만 마지막에 합친다.

1. **Spec:** success, rejection, boundary, state transition, API/error behavior와 scope exclusion이 card를
   충족하는지 확인한다.
2. **Maintainability:** 구체적인 duplication, mixed responsibility, caller knowledge, shallow abstraction,
   change amplification과 불필요한 speculative generality를 확인한다. 크기·loop·의존성 개수만으로 finding을
   만들지 않는다.
3. **Persistence:** query/fetch/pagination, transaction boundary, locking, index/migration order와 fresh
   persistence-context test를 확인한다. 성능 finding은 측정 evidence와 추정 질문을 구분한다.
4. **Architecture:** dependency direction, Spring Modulith `allowedDependencies`, named interface, event와
   adapter boundary를 확인한다.
5. **Evolution·compatibility:** public contract, serialization, deprecated API, migration·rollback 위험과
   실제로 알려진 variation을 확인한다. 미래 가능성만으로 extension point를 요구하지 않는다.

완료 기준: 다섯 축마다 finding 또는 `no finding` 근거가 있고, preference는 blocking finding과
분리되어 있다.

### 4. 독립 검증

1. Card가 요구한 scenario와 finding을 재현하는 가장 가까운 test를 `gradle-mcp`로 실행한다.
2. Module seam이면 Modulith 구조 검증, persistence/migration이면 관련 database test를 추가한다.
   Aggregate acceptance에 필요한 경우 backend 전체 `test`를 실행한다.
3. Terminal·shell·IDE에서 Gradle을 실행하지 않는다. `gradle-mcp`가 unavailable하거나 환경 prerequisite가
   없으면 실행하지 않은 결과를 pass로 바꾸지 않고 정확한 blocker와 재개 조건을 남긴다.
4. 검증 과정에서 tracked file이 바뀌지 않았고 clean worktree가 유지되는지 다시 읽는다.

완료 기준: 각 실행의 task/test identity와 실제 pass/fail이 evidence에 연결되고 unverified 영역이 없다.

### 5. Result 작성과 native completion

1. Finding을 contract의 `basis`, `observed_fact`, `evidence`, `impact`로 작성한다. Evidence에는 exact
   `path:line`, test/validator identity 또는 checkpoint SHA를 넣는다. 같은 원인의 중복 finding은 합친다.
2. 기존 동작의 code 수정이 필요하면 `correction-required`, 승인된 context가 부족하면
   `context-required`, 사용자 정책 결정이 필요하면 `decision-required`를 사용한다. 이전 finding이 실제로
   해소된 경우에만 source Review task/finding을 가리키는 `resolved`를 작성한다.
3. 새 blocking finding이 없고 모든 prior finding이 resolved되었을 때만 `result: approved`로 한다.
   Blocking finding이 있으면 `changes-required`다. Review 자체를 완료할 수 없는 prerequisite 문제는
   terminal result를 꾸미지 말고 task를 block한다.
4. Current native run ID와 canonical `aggregate-review-result-v1` metadata로 `kanban_complete`를 호출한다.
   Body, comments 또는 child task는 수정하지 않는다.
5. `kanban_show`로 task `done`, terminal run, metadata와 result가 저장됐는지 read-back한다. 실패하면
   성공으로 보고하지 않고 task/run 상태를 다시 확인한다.

완료 기준: PM validator가 소비할 canonical result가 latest terminal run에 있고 native task가 `done`이며
repository는 clean하다.

## 금지 사항과 실패 처리

- Source, test, migration, configuration, 문서, card body를 편집하지 않는다.
- Commit, push, merge, task graph mutation, finding routing과 Summary promotion을 수행하지 않는다.
- Coder self-verification이나 PM checkpoint pass를 독립 review evidence로 대체하지 않는다.
- 불명확한 제품 의미를 preferred design으로 보충하지 않는다.
- Review 도중 HEAD/checkpoint가 바뀌면 기존 결론을 폐기하고 새 immutable 범위로 admission부터 다시 한다.

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
