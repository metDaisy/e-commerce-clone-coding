# reviewer distribution

이 디렉터리는 runtime Profile이 아닌 Git으로 공유하는 Hermes Profile Distribution 원본이다.
Credential, Memory, session, state database, log와 machine-specific path는 포함하지 않는다.

## 설치

```text
hermes profile install ./.hermes/profiles/reviewer --name reviewer --alias --force --yes
```

일반 설치는 `.hermes/scripts/install-profiles.sh` 또는 `.hermes/scripts/install-profiles.ps1`를 사용한다.
설치 스크립트가 repository root, Kanban 설정과 Profile별 capability policy를 적용하고 read-back한다.

## 책임

`reviewer`는 모든 Coder checkpoint가 완료된 뒤 PM-authored `aggregate-review-card-v1`을 독립적으로
검토한다. Card의 complete behavior와 aggregate acceptance, child checkpoint를 고정하고 committed
source·test·migration을 직접 확인한다. Spec, maintainability, persistence, architecture,
evolution·compatibility를 검토한 뒤 terminal run metadata에 `aggregate-review-result-v1`을 남긴다.

Reviewer는 파일을 수정하거나 commit하지 않는다. Finding의 rework contract 작성, 다음 task promotion,
Summary와 release는 Project Manager 책임이다.

## 전속 Skill

- `run-aggregate-review`: task admission, evidence 고정, 다섯 review axis, 검증과 native handoff 절차
- `codebase-memory-mcp`: symbol·dependency·call path 후보 탐색
- `semble-search`: 정확한 구현 위치를 모를 때 의미 기반 후보 탐색

`SOUL.md`는 안정적인 Reviewer 정체성과 권한 경계를 소유한다.
`run-aggregate-review`는 실행 순서를,
`run-aggregate-review/references/aggregate-review-contract.md`는 Review card 소비와 결과 작성 계약을
소유한다. PM Distribution은 card authoring, 결과 validation과 finding routing을 소유한다.
