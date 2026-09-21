# coder distribution

이 디렉터리는 runtime Profile이 아닌 Git으로 공유하는 Hermes Profile Distribution 원본이다.
credential, Memory, session, state database, log, machine-specific path를 포함하지 않는다.

## 설치

```text
hermes profile install ./.hermes/profiles/coder --name coder --alias --force --yes
```

Profile 설치는 `.hermes/scripts/install-profiles.sh` 또는 `.hermes/scripts/install-profiles.ps1`를 사용한다.
Plugin 설치는 별도의 `.hermes/scripts/install-plugins.sh` 또는 `.hermes/scripts/install-plugins.ps1`를 사용한다.
설치 후 Profile 설치 스크립트가 Profile별 capability policy와 프로젝트 root를 적용한다.

## 책임

`coder`는 PM이 만든 self-contained backend Impl card를 구현하고 focused test와
backend 전체 test를 실행한 뒤 같은 card의 PM checkpoint를 요청한다. PM이 변경을 commit하고
clean working tree를 확인해야 card가 `done`이 된다. 독립 aggregate review와 승인된 refactor는
이 Profile의 책임이 아니다.

## 전속 Skill

- `run-impl-card`: Kanban task 수신부터 구현·2단계 검증·PM checkpoint까지의 순서
- `implement`: admitted Impl card를 최소 code·test 변경으로 구현하고 검증하는 coding loop
- `codebase-memory-mcp`: public API caller/callee와 cross-domain 영향 탐색
- `semble-search`: 구현 위치가 불명확할 때 의미 기반 위치 탐색

`SOUL.md`는 안정적인 Coder 정체성과 권한 경계를 소유한다. `run-impl-card`는 lifecycle procedure를,
`run-impl-card/references/implementation-card-contract.md`는 Impl card 소비 규칙과 handoff schema를 소유한다.
PM의 card authoring과 checkpoint 계약은 Project Manager Distribution이 소유한다.
`docs/backend-development-guide.md`와 `docs/backend-test-guide.md`는 repository의 backend 구현·test
convention source of truth다. Card 실행 lifecycle은 `run-impl-card`가 소유한다.
