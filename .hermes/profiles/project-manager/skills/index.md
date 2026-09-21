# Project Manager Skill Index

이 문서는 사람이 현재 상황에 맞는 Skill을 선택하는 탐색용 index다. 실행 순서와 완료 기준은 각
`SKILL.md`, persisted schema와 validator invariant는 각 `references/` 문서가 소유한다. 이 index는
실행 계약을 복제하지 않는다.

## 시작점

| Trigger | Load | Pass | Return only when / next |
|---|---|---|---|
| Issue delivery·재개·checkpoint·Review·release lifecycle | `run-workflow` | Issue/branch, actual task/run, graph/native/Git read-back | 현재 transition이 read-back되거나 durable blocker가 기록됨; 필요한 branch Skill로 이동 |
| 새 leaf Issue의 계획·문서 영향·정책 문제 정리 | `create-triage` | Issue identity, approved requirement locator, planning/current-state SHA | frozen triage digest와 open graph gate; `build-task-graph` |
| 사용자 policy·권한·오류 의미·UI 결정 필요 | `service-planning` | 결정 질문, evidence, 영향받는 requirement/Issue | 사용자 승인과 requirement read-back; 호출한 triage/workflow로 반환 |
| 승인 requirement 변경의 파생 문서·Issue 동기화 | `sync-docs` | 승인 revision, impact matrix, target locators | mutation/read-back 완료; 호출한 workflow로 반환 |
| stale/insufficient 또는 final implementation snapshot | `update-current-state` | invocation mode, inspection/final implementation SHA | `fresh | stale | insufficient`와 docs read-back; 호출한 workflow로 반환 |
| G1, requirement-rework, review-rework graph authoring | `build-task-graph` | mode와 frozen triage 또는 canonical Review result | native validation과 serial single-ready frontier; `run-workflow` |

## 탐색 보조 Skill

- `codebase-memory-mcp`: 최신 index에서 symbol·dependency·call path 후보가 필요할 때 사용한다.
- `semble-search`: 정확한 위치를 모르는 behavior·문서·유사 구현을 의미 기반으로 찾을 때 사용한다.
- 두 검색 결과는 locator다. 구현 상태는 committed source/test/migration read-back으로 판정한다.
- Exact string의 모든 occurrence가 필요하면 repository literal search를 사용한다.

## 대표 routing

```text
run-workflow (root)
  ├─ admission: clean repository + fresh current-state + leaf Issue
  │  └─ snapshot stale/insufficient → update-current-state → admission 재개
  ├─ create-triage
  │  ├─ 사용자 결정 필요 → service-planning → requirement 승인
  │  └─ 파생 문서/Issue 불일치 → sync-docs
  ├─ build-task-graph
  │  └─ 위치가 불명확할 때만 Codebase Memory / Semble
  ├─ Impl checkpoint와 aggregate Review 반복
  │  └─ Review finding의 corrective work → build-task-graph review-rework
  └─ Summary admission → update-current-state → PR·CI·merge → Summary done
```

외부 mutation은 해당 Skill의 완료 기준에 따라 read-back한 뒤 다음 Skill로 이동한다.
