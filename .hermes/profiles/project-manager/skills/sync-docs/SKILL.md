---
name: sync-docs
description: "승인 requirement 변경 뒤 파생 문서 또는 GitHub Issue tracker를 동기화하고 외부 mutation을 read-back할 때 사용한다."
version: 0.1.0
license: MIT
metadata:
  hermes:
    tags: [project-management, documentation, github, synchronization]
    related_skills: [service-planning, build-task-graph]
---

# 문서와 tracker 동기화

승인 requirement를 기준으로 `current-state.md`를 제외한 파생 문서와 delivery tracker를 동기화한다.
Triage는 영향만 분류하고 실제 mutation은 이 Skill이 소유한다.

## Mode

- `derived-docs`: architecture, ADR, glossary, ERD, index와 testing/validator 문서를 동기화한다.
- `issue-tracker`: GitHub Issue tree의 scope, body, status와 dependency를 동기화하고 read-back한다.

둘 다 필요하면 `derived-docs`를 먼저 완료한다.

## 사용 조건

- 사용자 결정이 requirement에 반영됐다.
- `create-triage` 또는 `build-task-graph`가 파생 문서·Issue 불일치를 찾았다.
- Graph authoring 또는 finalization 전에 documentation readiness 확인이 필요하다.

## 절차

1. 승인 requirement locator와 선택한 mode의 영향 대상을 inventory한다.
2. 각 대상을 `no-change | update | needs-input`으로 분류하고 근거를 기록한다.
3. Requirement가 제공하지 않는 architecture·policy 의미는 사용자에게 escalation한다.
4. 승인된 변경만 적용한다. `issue-tracker` mutation은 정확한 Issue를 다시 읽어 scope, status와 dependency를 확인한다.
5. 변경된 대상과 변경하지 않은 대상, blocker와 후속 Skill을 보고한다.

완료 기준: 모든 영향 대상이 승인 requirement와 일치하거나, 남은 불일치의 결정자·영향·다음
행동이 명시되고 external mutation은 read-back되었다.

`current-state.md`, requirement policy, source·test·migration은 수정하지 않는다. Snapshot 갱신 필요는
`snapshot-refresh-required`로 root `run-workflow`에 반환한다.
