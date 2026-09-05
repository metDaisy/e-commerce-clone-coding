# Amaazon Project Manager distribution

Install:

```text
hermes profile install ./.hermes/profile-distributions/project-manager --name project-manager --alias
```

Project Manager에는 별도 Kanban Skill을 설치하지 않습니다. Hermes runtime의 built-in
Kanban toolset과 lifecycle guidance가 기본 제공됩니다.

Project Manager는 GitHub Issue의 대략적인 범위와 요구사항 문서의 상세 계약,
`current-state.md`, committed code/test/migration을 대조하여 Issue 전체의 근거 있는
Kanban task graph를 생성합니다. 각 task에 담당 Profile, dependency, evidence,
scope, acceptance criteria, verification을 기록하고 첫 번째 task만 `ready`로 둡니다.

Issue 시작 시 고정 Quality Reviewer task와 commit/finalization task도 graph에
생성합니다. 구현 task의 Coder–Spec Reviewer 반복은 Hermes Kanban의 same-card review
lifecycle에 맡기고, 구현 task가 승인되면 commit task가 Project Manager에게 라우팅됩니다.
Quality Reviewer는 정해진 순서와 동일한 final HEAD를 사용합니다.

Project Manager는 Kanban과 GitHub Issue/PR lifecycle을 관리합니다. Coder, Reviewer,
Coordinator의 역할을 대신하지 않으며, 완료 선언 대신 Kanban run, verifier, CI, commit,
push, PR 상태와 working tree를 확인합니다.

## Capability boundary

Project Manager는 다음 외부 작업을 수행할 수 있습니다.

- Kanban task 생성·수정·할당·dependency 연결·상태 전환
- 요구사항과 검증 결과를 반영한 GitHub Issue 업데이트
- 승인된 변경의 commit 및 push
- PR 생성·업데이트·comment reply·merge
- 최종 code SHA를 기준으로 `current-state.md` 갱신 및 docs commit

Source 구현, 일반적인 code review, 요구사항 임의 변경, 검증 결과 조작은 수행하지
않습니다. GitHub PR/Issue의 authoritative 상태는 Project Manager만 읽고 씁니다.

전속 Skill이 있는 Profile은 bootstrap이 skills.sh에서 설치합니다. Project Manager의
Kanban orchestration은 Hermes runtime에 내장된 기능과 Kanban guidance를 사용하므로,
현재 별도 `kanban-orchestrator` Skill을 설치하지 않습니다. Memory, sessions, state
database, credentials, endpoint와 machine-specific path는 Distribution에 포함하지 않습니다.
