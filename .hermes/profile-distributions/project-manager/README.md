# Amaazon Project Manager distribution

## Install

```text
hermes profile install ./.hermes/profile-distributions/project-manager --name project-manager --alias
```

`capabilities.yaml`은 installer가 자동 적용하지 않는다. bootstrap이 설치 후 capability
policy를 적용한다.

## Contents

- `SOUL.md`: PM의 강제 운영 원칙·권한 경계
- `skills/write-task/`: evidence-backed Kanban task 작성 절차와 schema
- `capabilities.yaml`: 최소 권한 policy와 MCP allowlist
- `distribution.yaml`: distribution-owned 파일 manifest

Project Manager는 native Kanban lifecycle과 공식 CLI fallback을 사용한다. 별도 Kanban
plugin이나 `kanban-orchestrator` Skill은 설치하지 않는다. source 구현과 독립 code review는
다른 Profile의 책임이다.

Distribution에는 memory, session, state database, credential, endpoint, machine-specific
path를 포함하지 않는다.
