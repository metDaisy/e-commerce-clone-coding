# Project Manager Skill Index

이 문서는 사람이 현재 상황에 맞는 Skill을 선택하는 탐색용 index다. 실행 순서와 완료 기준은 각
`SKILL.md`, persisted schema와 validator invariant는 각 `references/` 문서가 소유한다. 이 index는
실행 계약을 복제하지 않는다.

## 시작점

| 상황 | 사용할 Skill | 경계 |
|---|---|---|
| 새 leaf Issue의 구현 아이디어·문서 영향·정책 문제를 정리 | `create-triage` | Rework graph와 Coder 실행 계약은 작성하지 않는다. |
| Business policy, 권한·오류 의미, 사용자 흐름·UI 방향에 사용자 결정 필요 | `service-planning` | 단순 source 위치·구현 범위 조사는 제외한다. |
| 승인 requirement 변경을 파생 문서나 GitHub Issue에 반영 | `sync-docs` | `current-state.md` snapshot은 갱신하지 않는다. |
| 구현 snapshot이 stale/insufficient하거나 finalization snapshot 필요 | `update-current-state` | Requirement-rework 중간의 source 확인은 제외한다. |
| 승인 입력으로 G1, requirement-rework 또는 review-rework graph 작성 | `build-task-graph` | Coder run checkpoint와 후속 lifecycle은 운영하지 않는다. |
| Impl handoff checkpoint, 다음 task, aggregate Review, Summary·release lifecycle 운영 | `controll-task-graph` | Card schema와 corrective graph는 작성하지 않는다. |

## 탐색 보조 Skill

- `codebase-memory-mcp`: 최신 index에서 symbol·dependency·call path 후보가 필요할 때 사용한다.
- `semble-search`: 정확한 위치를 모르는 behavior·문서·유사 구현을 의미 기반으로 찾을 때 사용한다.
- 두 검색 결과는 locator다. 구현 상태는 committed source/test/migration read-back으로 판정한다.
- Exact string의 모든 occurrence가 필요하면 repository literal search를 사용한다.

## 대표 routing

```text
new leaf Issue
  → create-triage
     ├─ 사용자 결정 필요 → service-planning → requirement 승인
     ├─ 파생 문서/Issue 불일치 → sync-docs
     └─ snapshot stale/insufficient → update-current-state
  → build-task-graph
     └─ 위치가 불명확할 때만 Codebase Memory / Semble
  → controll-task-graph
     ├─ Review finding의 corrective work → build-task-graph review-rework
     └─ finalization snapshot → update-current-state
```

외부 mutation은 해당 Skill의 완료 기준에 따라 read-back한 뒤 다음 Skill로 이동한다.
