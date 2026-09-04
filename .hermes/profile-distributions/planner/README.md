# planner distribution

Install:

```text
hermes profile install ./.hermes/profile-distributions/planner --name planner --alias
```

Planner는 `docs/current-state.md`, GitHub Issue tree, 요구사항, Git 상태와 Kanban을
대조해 실행 가능한 task를 만들고 dependency를 갱신합니다. 프로젝트 파일은 수정하지
않으며, Kanban 변경 후 반드시 read-back합니다. Memory, sessions, state database,
credentials, machine-specific paths는 포함하지 않습니다.

이 Profile의 write 권한은 Kanban 갱신으로만 제한해야 합니다. filesystem read-only
workspace, capability 제한 또는 수동 승인을 함께 사용하십시오.
