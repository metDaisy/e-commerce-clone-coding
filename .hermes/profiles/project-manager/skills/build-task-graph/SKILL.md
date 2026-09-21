---
name: build-task-graph
description: "승인된 Issue에서 검증 가능한 Kanban graph를 작성한다."
version: 0.7.0
author: "Amaazon project, Hermes Agent"
license: MIT
platforms: [linux, macos, windows]
metadata:
  hermes:
    tags: [project-management, kanban, task-graph, requirements, verification]
    related_skills: [create-triage, service-planning, sync-docs, run-workflow, codebase-memory-mcp, semble-search]
requires_toolsets: [kanban]
---

# Task graph 작성

승인된 Issue 계약을 native Kanban graph로 변환한다. PM은 graph membership, self-contained card,
최초 activation과 rework mutation을 소유한다. Native Kanban은 task ID, assignee, status, link, run,
comment와 event를 소유한다.

모든 mode에서 [`references/board-contract.md`](references/board-contract.md)를 읽는다. Backend Impl을
작성할 때만 [`references/implementation-card-contract.md`](references/implementation-card-contract.md)를
추가로 읽는다. 전자는 persisted graph의 schema·invariant, 후자는 Impl body의 단일 기준이다.

## 사용 조건

- `new`: 승인된 leaf Issue의 G1 graph가 필요하다.
- `requirement-rework`: aggregate Review 승인 전에 승인 requirement revision이 active contract를
  바꿔 G{N+1} superseding graph가 필요하다.
- `review-rework`: 완료된 aggregate Review의 structured finding에 같은 generation의 corrective Impl,
  context re-review 또는 blocked Decision이 필요하다.

aggregate Review 승인 뒤 requirement가 바뀌면 기존 graph를 수정하지 않고 새 Issue의 `new`를
사용한다.

## 조건부 Skill routing

다음 조건에서 해당 Skill을 읽고 그 완료 기준을 충족할 때까지 graph 작성을 중단한다.

- 새 leaf Issue의 G1 planning record가 없으면 `create-triage`를 사용한다. G{N+1} rework Triage는 이
  Skill이 작성한다.
- Policy·authorization·consistency·오류 의미·UI 의미에 사용자 결정이 남으면
  `service-planning`을 사용하고 Triage를 `blocked`로 유지한다.
- 승인 requirement와 파생 문서 또는 Issue가 불일치하면 `sync-docs`를 사용하고 mutation을
  read-back한다.
- `current-state.md`가 fresh하지 않거나 판정 근거가 부족하면 graph 작성을 중단하고
  `snapshot-refresh-required` blocker를 root `run-workflow`에 반환한다. Requirement-rework 중간에는
  snapshot 대신 committed source를 직접 확인한다.
- Behavior locator가 불명확할 때만 `codebase-memory-mcp` 또는 `semble-search`를 사용한다. 결과는
  후보이며 committed source/test/migration read-back이 구현 상태의 근거다.
- 최초 activation 뒤 checkpoint, 후속 promotion, aggregate Review, Summary와 release lifecycle은
  `run-workflow`에 인계한다. Review finding이 graph mutation을 요구하면 이 Skill의
  `review-rework`로 돌아온다.

## 공통 작성 절차

1. **입력과 mode를 고정한다.** Leaf Issue, 승인 requirement, planning baseline, workflow marker,
   current-state, 기존 native card를 읽는다. 완료 기준: mode와 requirement basis가 하나로 확정되고
   누락 입력은 owner에게 routing되었다.
2. **Behavior를 분류한다.** 각 effective behavior를 `implemented | partial | absent | unknown`으로
   판정한다. `implemented`는 새 contract를 계속 충족하는 source-backed evidence가 있어야 하며,
   `partial`/`absent`만 Impl을 만든다. 완료 기준: 모든 behavior에 state, disposition과 근거가 있다.
3. **Draft를 검증한다.** `.temp/task-graphs/<issue>/graph.json`을 작성하고 `validate-graph --phase
   draft`를 실행한다. Backend Impl은 `template`로 시작해 승인된 사실만 채우고 `validate`한다.
   완료 기준: validator finding이 없다.
4. **Native graph를 생성한다.** Card와 link를 하나씩 만들며 body, ID, assignee, status와 parent를
   각각 read-back한다. 각 Impl에는 PM-owned comment로 `backend-implementation-admission-v1`을 남긴다.
   이 closed object는 `schema`, actual `task_id`, positive wrapper Issue number인 `issue`, exact `workspace`,
   `card_schema: backend-implementation-card-v1`, `restart: null`만 가지며 Coder가 native show에서 읽는
   durable execution intent다. `run-workflow/scripts/workflow.py validate-implementation-admission`으로
   comment payload를 검증한다. Wrapper를 read-back 값으로 갱신한다. 완료 기준: 신규 execution card가
   dispatchable하지 않고 wrapper와 native 상태가 일치하며 admission comment가 exact task에 존재한다.
5. **Activation을 검증한다.** Mode별 activation을 실행한 뒤 `validate-graph --phase native`를
   실행한다. 완료 기준: 정확히 하나의 activation target만 `ready`이거나, unresolved Decision-only
   graph에는 `ready` card가 없다.

Built-in decomposer는 이 계약을 표현하지 못하므로 사용하지 않는다.

## `new` 절차

1. `create-triage`가 freeze한 `running` G1 Triage와 handoff를 읽는다. 완료 기준:
   `build_task_graph.allowed: true`이고 미해결 policy finding이 없다.
2. Impl, Review1, Summary body와 topology를 작성한다. Impl은 history delta가 아니라 현재 effective
   behavior 전체를 구현하는 self-contained `backend-implementation-card-v1`이다.
3. Planned behavior가 있으면 첫 Impl을 Triage child로 둔다. Planned behavior가 없으면 전체
   inherited behavior를 재검증할 Review1을 Triage child이자 activation target으로 둔다.
4. 공통 작성 절차를 완료하고 Triage를 완료한다.

완료 기준: Triage는 `done`이고 첫 Impl 또는 no-Impl Review 하나만 `ready`다.

## `requirement-rework` 절차

1. 승인 revised requirement와 파생 문서/Issue read-back을 확인한다. `requirement-diff`로
   basis→revised 비교 산출물을 만든 뒤 각 hunk를 behavior delta evidence로 해석한다. Helper가
   behavior 결론을 자동 확정하지 않는다.
2. Affected running card를 실제 supersession 사유로 block하고 worker 종료를 read-back한다. `done`
   history는 보존한다.
3. Prior Summary, archived unfinished work, inherited done evidence, revised requirement와 complete
   effective behavior를 가진 G{N+1} draft를 작성한다.
4. 새 graph를 완전히 read-back한 뒤 obsolete unfinished Impl/Review/Summary만 archive한다. Planned
   behavior가 있으면 첫 Impl, 없으면 aggregate Review를 Triage child와 activation target으로 둔다.
5. 공통 작성 절차를 완료하고 새 Triage를 완료한다.

완료 기준: G{N+1}만 dispatchable하며 모든 desired behavior가 inherited, planned 또는 blocked다.

## `review-rework` 절차

1. Done Review의 terminal completion event, latest run metadata, body, children과 Summary direct parents를
   read-back한다. Run metadata의 canonical `findings`만 routing input으로 사용한다.
2. `correction-required`는 기존 effective behavior를 충족하는 Impl, `context-required`는 approved
   source를 재확인하는 Review, `decision-required`는 같은 G의 blocked Decision으로 materialize한다.
3. Completed card/body/link를 그대로 두고 source Review→새 work→next Review/Summary를 append한다.
   모든 blocking finding의 후속 contract가 정해진 뒤 next Review를 만든다.
4. `source_review`에 review key, finding별 verdict·appended card와 card별 idempotency key를 기록한다.
   Native child와 Summary parents를 read-back해 이미 존재하는 work를 재생성하지 않는다.
5. Appended Impl이 있으면 첫 Impl을 activation target으로 둔다. Context-only rework이면 next Review를
   activation target으로 둔다. Unresolved Decision-only rework이면 `ready_candidate: null`로 두고
   Decision을 `blocked`로 유지한다. Triage와 source history는 `done` 상태를 유지한다.
6. 공통 작성 절차의 draft/native 검증을 완료한다. Review-rework에서는 Triage completion을 다시
   실행하지 않는다.

완료 기준: 모든 finding에 disposition이 있고, historical card는 immutable·`done`, append된 graph가
검증되며 ready card는 최대 하나다.

## Helper

다음 명령은 `terminal`로 Skill directory에서 실행한다.

```text
python scripts/build_task_graph.py template --issue <number> --issue-url <url> --output .temp/task-graphs/<issue>/impl-1.json
python scripts/build_task_graph.py validate .temp/task-graphs/<issue>/impl-1.json
python scripts/build_task_graph.py validate-graph .temp/task-graphs/<issue>/graph.json --phase draft
python scripts/build_task_graph.py requirement-diff --base <sha> --revised <sha> --path <requirement-path> --output .temp/requirement-rework/<issue>/comparison.json
python scripts/build_task_graph.py validate-graph .temp/task-graphs/<issue>/graph.json --phase native
python tests/native_e2e.py --profile project-manager
```

Validator는 schema·topology 일관성을 증명한다. Native status/link/ID는 read-back으로, behavior 완료는
checkpoint와 Reviewer evidence로 별도 증명한다.

## 보고

한국어로 mode, generation/root, requirement basis/revision, behavior classification, inherited evidence,
created/archived cards, native read-back, activation target, blocker와 next transition을 보고한다. 근거 없이
behavior를 implemented, card를 done, graph를 dispatchable이라고 보고하지 않는다.
