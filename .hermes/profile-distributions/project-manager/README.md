# Amaazon Project Manager distribution

## Install

```text
hermes profile install ./.hermes/profile-distributions/project-manager --name project-manager --alias
```

`capabilities.yaml`은 installer가 자동 적용하지 않는다. bootstrap이 설치 후 capability
policy를 적용한다.

## Contents

- `SOUL.md`: PM의 강제 운영 원칙·권한 경계
- `skills/write-task/`: repository discovery preflight와 `board-contract-v3` 기반 Kanban task 작성 절차
- `skills/cross-domain-contract-planning/`: 다른 도메인의 미구현 capability를 Named Interface query, domain event projection 또는 `needs-input`으로 조율하는 contract-first 절차
- `scripts/validate_board.py`: v2 재현과 `board-contract-v3` 생성 전 draft·생성 후 native read-back을 검사하는
  read-only deterministic validator
- `scripts/test_validate_board.py`: 과거 graph 생성 결함의 regression test
- `capabilities.yaml`: PM이 사용할 Skill/toolset/MCP/GitHub tool allowlist와 승인 정책
- `distribution.yaml`: distribution-owned 파일 manifest

Project Manager는 native Kanban lifecycle과 공식 CLI fallback을 사용한다. 별도 Kanban
plugin이나 `kanban-orchestrator` Skill은 설치하지 않는다. source 구현과 독립 code review는
다른 Profile의 책임이다.

Validator는 board를 수정하지 않으며 exit code `0`일 때만 graph promotion을 허용한다.
`board-contract-v1`과 `board-contract-v2`는 기존 board 재현용으로 보존하고, 새 graph에는 v3만
사용한다. v3는 repository HEAD/planning SHA, application-covered snapshot freshness,
reconciliation 결과 handoff와 자기 자신을 증명하는 board-list CHECK 금지를 검증한다.
Semble·codebase-memory 사용 여부는 project-local `agent-audit`가 raw query/result 없이
관찰한다. Audit은 fail-open debugging evidence이며 validator나 source 확인을 대체하지 않는다.
Audit hook은 project plugin을 opt-in한 새 runtime에서만 동작한다. CLI에서는 repository
root에서 `HERMES_ENABLE_PROJECT_PLUGINS=true`로 시작하고, Desktop도 같은 환경을 상속한 새
backend/session이어야 한다. 현재 세션에 환경을 뒤늦게 설정해 소급 적용할 수 없다.

```text
python .hermes/profile-distributions/project-manager/scripts/validate_board.py --input <draft.json> --phase draft --repository <repository-root>
python .hermes/profile-distributions/project-manager/scripts/validate_board.py --board <slug> --phase draft --profile project-manager --repository <repository-root>
python .hermes/profile-distributions/project-manager/scripts/validate_board.py --board <slug> --phase post --profile project-manager --repository <repository-root>
```

Distribution에는 memory, session, state database, credential, endpoint, machine-specific
path를 포함하지 않는다.
