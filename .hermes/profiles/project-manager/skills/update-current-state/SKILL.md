---
name: update-current-state
description: "Committed implementation 변경으로 current-state snapshot이 stale하거나 finalization에서 새 snapshot을 기록할 때 사용한다."
version: 0.1.0
license: MIT
metadata:
  hermes:
    tags: [project-management, current-state, snapshot, recovery]
    related_skills: [sync-docs, build-task-graph, controll-task-graph]
---

# Current state 갱신

`docs/current-state.md`를 platform-independent committed implementation snapshot으로 관리한다.
Kanban/Profile 상세를 기록하지 않으며 source·test·configuration·migration·requirement·ADR을 수정하지 않는다.

## Freshness 판정

- `fresh`: recorded snapshot SHA 이후 backend/frontend source·test, build/configuration 또는 Flyway
  migration의 committed 변경이 없고 worktree가 clean하다.
- `stale`: 위 implementation-covered path에 committed drift가 있다.
- `insufficient`: recorded SHA, ancestry, path 범위 또는 clean state를 확인할 수 없다.

Requirement·일반 문서·Profile 변경만으로 snapshot이 stale해지지는 않는다. 미커밋 변경이 있으면
snapshot을 fresh로 판정하지 않는다. `insufficient`는 graph authoring blocker다.

## 절차

1. Clean worktree, inspection HEAD, 이전 snapshot SHA와 ancestry를 read-back한다.
2. 두 SHA 사이 changed paths를 implementation-covered path로 분류한다.
3. `fresh`이면 근거와 active workflow marker 상태만 반환한다.
4. `stale`이면 committed source/test/configuration/migration evidence로 영향 section을 재조사한다. 실행하지 않은 test는 통과로 기록하지 않는다.
5. Snapshot을 갱신할 때 implementation snapshot SHA와 이후 docs commit SHA를 구분한다.
6. Finalization에서만 active workflow marker를 종료하고 docs-only commit을 read-back한다.

완료 기준: `fresh | stale | insufficient`, inspection/snapshot SHA, drift paths, 실행·미실행 검증과 marker
상태가 확인된다. 문서를 갱신했다면 모든 변경 사실이 committed evidence에 연결되고 docs commit을 read-back했다.

Requirement-rework 중간에는 snapshot을 갱신하지 않는다. `build-task-graph`가 현재 committed source를
직접 확인하며, 최종 갱신은 Summary finalization 뒤 수행한다.
