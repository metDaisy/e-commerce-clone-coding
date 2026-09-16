# controll-task-graph 구현 계획

## 목적

생성된 Kanban graph가 Coder 구현, PM checkpoint, 단일 Reviewer의 root review, PR·CI·merge·Issue 종료까지 사실에 맞게 진행되도록 제어한다. PM은 Coder 구현이나 Reviewer의 전문 판단을 대신하지 않는다.

## 사용할 때

- Coder가 같은 card에서 PM review/checkpoint를 요청할 때
- 다음 child task를 promotion하거나 blocked/needs-input으로 전이할 때
- `G{N}-Issue{M}-Review{Q}`를 활성화하고 verdict를 routing할 때
- Reviewer finding을 `build-task-graph` rework mode로 넘길 때
- upstream correction, base-sync, interrupted workflow recovery가 발생할 때
- root review approved 뒤 PR, CI, merge, Issue auto-close를 진행할 때

## 입력

- native Kanban task/card/run/comment read-back
- current delivery branch, clean working tree, committed SHA
- Coder verification evidence
- Reviewer verdict 또는 structured finding
- GitHub Issue/PR/CI read-back
- current-state integration facts: freshness와 active workflow marker 상태

## 절차 초안

1. Coder handoff마다 acceptance·verification evidence, changed paths, commit boundary, clean tree를 확인한다.
2. PM이 same-card operational checkpoint commit을 만들고 SHA와 worktree를 read-back한 뒤 같은 child를 완료한다.
3. dependency를 만족한 다음 child만 진행시킨다. 누락된 evidence, semantic ambiguity, unrelated dirty change는 blocked 또는 needs-input으로 routing한다.
4. 모든 Impl 완료 뒤 Reviewer-assigned `G{N}-Issue{M}-Review{Q}`를 진행하고, 승인 뒤 semantic `G{N}-Issue{M}` root를 PM finalization한다. PM은 review body를 작성하지만 verdict를 대체하지 않는다.
5. changes-requested는 finding을 보존하고 `build-task-graph`의 review-rework mode로 새 contract를 만든다.
6. approved면 `final_implementation_sha`를 freeze하고, `update-current-state` 완료 뒤 PR·CI·merge·Issue auto-close read-back을 수행한다.

## exception mode

- `base-sync`: B가 A-owned defect로 blocked될 때 A rework → checkpoint → B sync → 영향받은 B card refresh를 orchestration한다. rebase/merge 선택과 semantic conflict 판단은 TBD다.
- `restart-task`: active marker가 있고 동일 Issue에만 귀속 가능한 dirty delta인데 Kanban 기록이 유실됐을 때, 하나의 Coder recovery card로 clean checkpoint를 복원한다.

## 출력

- 사실에 맞는 Kanban transition과 read-back
- checkpoint SHA와 clean-state evidence
- root review verdict의 후속 transition
- PR/CI/merge/Issue closure의 외부 상태 evidence
- blocked 또는 needs-input의 정확한 원인과 다음 결정자

## 경계

- source·test·migration을 구현하지 않는다.
- Reviewer finding을 임의로 삭제·승인·축소하지 않는다.
- semantic policy conflict를 기술적 merge conflict처럼 독자 해결하지 않는다.
- current-state schema와 updater 세부를 이 Skill에서 정의하지 않는다.

## 완료 기준

모든 상태 전이는 native 또는 external read-back으로 증명되고, 완료 보고에는 relevant SHA, verification, CI/PR/Issue 상태, clean worktree 상태가 포함되어야 한다.

## TBD

- base-sync 필요 판정과 rebase/merge 선택 기준
- Coder에게 전달할 conflict context schema
- Reviewer finding의 확정 schema
- CI failure와 PR feedback의 rework routing 기준
