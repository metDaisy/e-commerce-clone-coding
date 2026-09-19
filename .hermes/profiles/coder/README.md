# implementation-coder distribution

이 디렉터리는 runtime Profile이 아닌 Git으로 공유하는 Hermes Profile Distribution 원본이다.
credential, Memory, session, state database, log, machine-specific path를 포함하지 않는다.

## 설치

```text
hermes profile install ./.hermes/profiles/coder --name implementation-coder --alias --force --yes
```

전체 Profile bootstrap은 `.hermes/scripts/setup-hermes.sh` 또는 `.hermes/scripts/setup-hermes.ps1`를 사용한다.
설치 후 bootstrap이 Profile별 capability policy와 프로젝트 root를 적용한다.

## 책임

`implementation-coder`는 PM이 만든 self-contained backend Impl card를 구현하고 focused test와
backend 전체 test를 실행한 뒤 같은 card의 PM checkpoint를 요청한다. PM이 변경을 commit하고
clean working tree를 확인해야 card가 `done`이 된다. 독립 aggregate review와 승인된 refactor는
이 Profile의 책임이 아니다.

## 전속 Skill

- `implementation-workflow`: Kanban task 수신부터 구현·2단계 검증·PM checkpoint까지의 순서
- `implement-backend-card`: admitted Impl card를 최소 code·test 변경으로 구현하고 검증하는 coding loop
- `codebase-memory-mcp`: public API caller/callee와 cross-domain 영향 탐색
- `semble-search`: 구현 위치가 불명확할 때 의미 기반 위치 탐색
- `java-springboot`, `java-junit`: Java/Spring Boot 구현·테스트 보조

`implementation-workflow/references/`는 PM/Coder execution contract와 Impl card schema를 소유한다.
`docs/testing-guide.md`와 `docs/agent-workflow.md`는 repository의 테스트·작업 절차 source of truth다.
