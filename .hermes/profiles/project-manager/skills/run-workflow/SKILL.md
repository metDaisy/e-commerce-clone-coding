---
name: run-workflow
description: "Kanban 중심 PM workflow를 시작부터 release까지 실행한다."
version: 1.0.0
author: "Amaazon project, Hermes Agent"
license: MIT
platforms: [linux, macos, windows]
metadata:
  hermes:
    tags: [project-management, kanban, checkpoint, review, release, recovery]
    related_skills: [build-task-graph, update-current-state]
requires_toolsets: [kanban]
---

# 워크플로 실행

Issue admission부터 Kanban graph 실행, PM checkpoint, aggregate Review, Summary release와 recovery까지
하나의 root workflow로 운영한다. Kanban task·run·event·comment와 Git/GitHub read-back이 실행 상태의
기준이며 별도 workflow ledger를 만들지 않는다. PM은 source·test·migration을 구현하거나 Reviewer
finding을 대신 판단하지 않는다.

Persisted runtime schema와 결정론적 invariant는
[`references/workflow-contract.md`](references/workflow-contract.md), same-card checkpoint schema는
[`references/execution-contract.md`](references/execution-contract.md)가 소유한다. Graph/card 작성은
`../build-task-graph/`가 소유한다. `scripts/workflow.py`와 `scripts/checkpoint.py`는 read-only validator이며 Git, Kanban,
GitHub를 변경하지 않는다. 설치된 Skill 경로를 resolve해서 실행한다.

## 시작과 재개

1. Native Kanban tool schema를 확인하고 exact board의 active·archived task를 모두 read-back한다. Native
   tool이 없는 일반 PM session은 공식 `hermes kanban` CLI의 현재 `--help`를 읽은 뒤 사용한다.
2. Branch, `HEAD`, 전체 Git status, `current-state.md` freshness와 active marker를 읽는다.
3. Marker가 없고 repository가 clean이면 새 leaf Issue의 canonical delivery branch를 선택하고
   `create-triage`부터 시작한다. Frozen Triage가 준비되면 `build-task-graph`로 graph 전체를 작성한다.
4. Marker가 있으면 Issue·branch·committed checkpoint·Kanban history를 비교해 정상 재개, `restart-task`,
   base-sync 또는 사용자 결정을 선택한다. Dirty 상태를 새 planning 입력으로 사용하지 않는다.
5. 기존 `build-task-graph-v1` wrapper와 정규화한 native read-back, Git read-back을 각각 JSON으로 저장하고
   다음을 실행한다.

   `python scripts/workflow.py validate-state <graph.json> <board.json> <git.json>`

완료 기준: validator가 현재 phase, eligible/ready task와 **하나의** `allowed_transition`을 반환하고 실제
board·branch·marker와 일치한다. 오류가 있으면 mutation 전에 block한다.

## Kanban 실행 반복

1. `allowed_transition`이 `promote`이면 계산된 task 하나만 promotion하고 task ID, assignee, parents,
   status와 ready count를 다시 읽는다. `claim`이면 해당 assignee의 worker만 claim한다.
2. Impl은 `implementation-coder`가 card contract를 구현·self-verify하고 native same-card PM review를
   요청한다. PM은 dispatcher가 만든 active review run에서만 checkpoint를 수행한다.
3. Impl 완료 뒤 다시 `validate-state`를 실행한다. Cached ready candidate나 title 순서로 다음 task를
   추측하지 않는다.
4. Review는 단일 Reviewer가 immutable aggregate contract와 모든 child checkpoint를 읽고 latest terminal
   run metadata에 `aggregate-review-result-v1`을 남긴다. PM은 다음으로 검증한다.

   `python scripts/workflow.py validate-review-result <review-result.json> <graph.json>`

5. `changes-required` 또는 `blocked` finding은 `build-task-graph review-rework`로 same-generation work를
   append한다. `correction-required`는 corrective Impl, `context-required`는 next Review,
   `decision-required`는 native blocked Decision으로 route한다. Source Review/body/run/finding을 수정하거나
   삭제하지 않는다.
6. Review가 `approved`이고 prior blocking finding이 모두 explicit `resolved`이면 Summary admission으로
   이동한다. Runtime validator와 native read-back을 반복해 매 순간 하나의 정상 frontier만 유지한다.

## 같은 카드의 PM checkpoint

1. Task가 `review`, assignee가 `project-manager`, active run이 dispatcher-created review run인지 확인한다.
2. Immutable `backend-implementation-card-v1`과 latest Coder run의
   `backend-implementation-handoff-v1` metadata를 read-back한다. Comment/prose는 대체 증거가 아니다.
3. Workspace·branch·Git status와 전체 diff를 읽고 다음을 실행한다.

   `python scripts/checkpoint.py handoff <card.json> <handoff.json>`

4. 실제 changed/staged/untracked path가 handoff와 정확히 같고 문서가 포함되지 않으며,
   acceptance·focused verification·required scenario·`gradle-mcp test` evidence가 모두 pass인지 검수한다.
5. 수정이 필요하면 `backend-implementation-change-request-v1`을 검증해 comment로 남기고 native
   `request-changes` 후 original Coder ownership과 종료된 PM run을 read-back한다.
6. 통과하면 `git config --get core.hooksPath`가 `.githooks`인지 확인하고 검증된 path만 stage한다. staged
   diff/name/check를 읽은 뒤
   [`docs/commit-message-convention.md`](../../../../../docs/commit-message-convention.md)를 다시 읽는다.
   실제 staged diff를 근거로 type·50자 이내 한국어 명령형 subject·본문·명시된 Issue만 작성하고 하나의
   self-contained commit을 만든다. `post-commit`이 Python script를 통해 `codebase-memory-mcp` MCP stdio server의 `index_repository` tool을 동기 실행한
   뒤 SHA, committed path, message와 clean worktree를 read-back하고, `.githooks/post-commit`의
   `codebase-memory/last-indexed-head`가 result SHA와 정확히 같은지 확인한다. hook 또는 freshness record가
   없거나 다르면 checkpoint를 block하고 `backend-implementation-checkpoint-v1`을 작성하지 않는다.
7. `python scripts/checkpoint.py checkpoint <card.json> <handoff.json> <checkpoint.json>` 통과 후 같은 Impl을
   complete하고 closing run metadata와 `done`을 read-back한다.

완료 기준: task `done`, checkpoint SHA/path, clean Git state가 같은 native run과 일치한다. PM checkpoint는
aggregate Review를 대체하지 않는다.

## Summary와 release

Summary는 release 후 완료되는 finalization card다. Native dependency가 Summary를 `ready`로 만들었다는
사실만으로 완료하지 않는다.

1. 모든 direct parent가 `done`, latest Review가 `approved`, prior blocking finding이 모두 resolved인지
   read-back한다. Clean `HEAD`를 `final_implementation_sha`로 freeze하고 `summary-admission-v1`을 검증한다.

   `python scripts/workflow.py validate-summary-admission <admission.json>`

2. `update-current-state`로 frozen implementation SHA의 `src/**`를 조사하고 active marker를 제거한다.
   `docs/current-state.md`만 변경한 docs-only commit, inspection SHA, docs commit SHA와 clean tree를 읽는다.
3. Delivery branch를 push하고 repository default branch 대상 PR을 만든다. PR body에 정확한
   `Closes #<leaf-issue>`를 넣고 PR number, URL, base/head branch와 actual head SHA를 읽는다.
4. **실제 PR head SHA**의 required CI check가 모두 terminal success/neutral인지 확인한다. Pending,
   skipped, cancelled, failed, 다른 SHA의 결과는 통과가 아니다.
5. Merge를 수행하고 같은 PR/head의 merge SHA를 읽는다. 별도 Issue close mutation 없이 GitHub가 해당
   PR로 leaf Issue를 auto-close했는지 읽는다.
6. 모든 사실을 `issue-release-result-v1`로 만들고 검증한다.

   `python scripts/workflow.py validate-release <release.json>`

7. 검증 통과 후에만 같은 Summary를 complete하고 terminal run metadata와 `done`을 read-back한다.

완료 기준: final implementation SHA, docs-only snapshot commit, PR head CI, merge와 Issue auto-close가 하나의
검증된 release chain이며 Summary가 마지막에 `done`이다.

## Release finding routing

CI failure나 PR feedback마다 `release-finding-v1`을 만들고
`python scripts/workflow.py validate-release-finding <finding.json>`으로 검증한다.

- `implementation-rework`: self-contained corrective Impl과 새 Review를 append한다.
- `infrastructure-retry`: 같은 head와 동일 check의 일시적 실패만 bounded retry한다.
- `context-required`: committed context를 조사하고 새 Review contract를 만든다.
- `decision-required`: blocked Decision으로 사용자에게 올린다.
- `no-action`: evidence로 실제 비적용을 증명한다.

`target_pr`, `target_head_sha`, source URL, finding ID와 idempotency key로 중복 routing을 막는다. Rework가 PR
head의 reviewed code range를 바꾸면 이전 Review·CI 결론을 재사용하지 않는다.

## 중단된 workflow 복구

### `restart-task`

Marker는 현재 Issue/branch를 가리키고 모든 dirty path가 그 Issue에만 evidence로 귀속되지만 Kanban 기록이
유실된 경우에만 하나의 Coder recovery card를 만든다. `restart-task-v1`을
`python scripts/workflow.py validate-restart <restart.json>`으로 먼저 검증한다. Mixed·unknown path, marker mismatch,
복수 recovery card는 사용자 결정으로 block한다. Recovery Impl도 정상 same-card checkpoint를 거쳐 clean
commit을 만든 뒤 정상 state derivation으로 돌아온다.

### `base-sync`

B가 A-owned public contract/domain rule 결함으로 blocked되면 A correction→checkpoint→B sync 순서를
유지한다. Canonical upstream/downstream branch, fixed/pre-sync SHA, `merge | rebase` 전략과 owner/rationale,
changed/conflicted path, affected B card, post-sync SHA와 verification을 `base-sync-v1`에 기록하고
`python scripts/workflow.py validate-base-sync <base-sync.json>`으로 검증한다.

공개된 shared branch history를 보존해야 하면 merge를 기본 후보로 삼고, unpublished local history를
정리하는 명시적 승인이 있을 때만 rebase를 선택한다. Git text conflict와 semantic conflict를 구분한다.
Authorization·consistency·API/error meaning 또는 effective behavior가 달라지는 semantic conflict는
`decision-required`로 사용자에게 올리고 독자 해결하지 않는다. Sync 뒤 affected B card가 stale하면
`build-task-graph`로 replacement/rework contract를 만들고 clean read-back 후 재개한다.

## 실패 처리

- Validator error, stale run, workspace/branch mismatch, unexpected dirty path: mutation 없이 block한다.
- Native transition 실패: task/run/event를 다시 읽고 종료된 run에서 재시도하지 않는다.
- Git commit 실패 또는 dirty post-commit: task를 완료하지 않고 같은 review run에서 복구한다.
- CI/PR finding: 분류 contract 없이 retry·dismiss·merge하지 않는다.
- External mutation 성공 응답만으로 완료하지 않고 exact target을 read-back한다.

## 보고 형식

```text
status: running | checkpoint-done | review-rework | release-done | blocked
issue/board:
phase/task/run:
head/final_implementation_sha:
validation:
native_readback:
external_readback:
blocker:
next_transition:
```
