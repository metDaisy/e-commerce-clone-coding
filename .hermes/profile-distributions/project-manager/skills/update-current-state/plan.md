# update-current-state 구현 계획

## 목적

`docs/current-state.md`를 검증된 committed implementation snapshot으로 갱신한다. 이 Skill은 PM의 문서 우선 planning을 지지하지만, current-state의 세부 schema와 updater는 별도 session/branch에서 확정한다.

다른 PM Skill에는 `fresh | stale | insufficient`와 active workflow 존재 여부만 제공한다. schema,
freshness 판정, inspection 범위, draft generator, active marker field와 start/end transition의 상세는
이 Skill이 소유하며 `WORKFLOW-DESIGN.md`에 중복하지 않는다.

## 현재 합의된 설계 입력

- `current-state.md`는 platform-independent implementation snapshot과 recovery fact만 기록한다.
  Hermes board/task/profile ID나 상세 task body는 기록하지 않는다.
- implementation snapshot은 해당 snapshot SHA 뒤 backend/frontend source·test,
  build/configuration, Flyway migration의 committed 변경이 없을 때 fresh다. requirement·일반 문서·
  profile 변경만으로 snapshot이 stale해지지는 않지만, 미커밋 변경은 어떤 경우에도 planning을 막는다.
- 새 delivery workflow는 clean 확인 뒤 delivery branch를 만들고, graph 생성 전에 active workflow
  marker를 committed로 남긴다. marker의 현재 합의 field는 tracking reference, lineage, delivery branch,
  initial base SHA, initial implementation snapshot SHA, requirement locator, 짧은 작업 요약이다.
- marker의 initial SHA는 historical anchor다. base-sync가 발생해도 덮어쓰지 않는다. finalization에서만
  implementation snapshot을 갱신하고 marker를 종료한다.
- Kanban 기록이 유실된 interrupted workflow에서는 marker와 dirty delta가 동일 Issue에 명확히
  귀속될 때만 `controll-task-graph`가 `restart-task`를 만들 수 있다. 이 Skill은 marker schema와
  freshness fact를 제공하고 recovery card를 직접 만들지 않는다.
- `update-current-state`는 source/test/configuration/migration/requirement/ADR을 수정하지 않는
  docs-only maintenance task다. `sync-docs`의 파생 문서 readiness 확인 뒤에 수행한다.
- read-only draft generator는 clean inspection SHA와 이전 snapshot SHA에서 changed-path grouping,
  재조사 후보, checklist만 만들 수 있다. 구현 완료·test 통과를 주장하거나 current-state·Kanban·Git을
  변경할 수 없다.

## 사용할 때

- implementation-covered committed 변경으로 current-state snapshot이 stale일 때
- workflow finalization에서 final implementation SHA를 snapshot으로 기록할 때
- 별도 maintenance task로 현재 구현 상태를 다시 조사해야 할 때

## 입력

- clean inspection SHA와 이전 implementation snapshot SHA
- current-state schema와 freshness 규칙
- committed source/test/configuration/migration/frontend evidence
- sync-docs 완료 또는 명시적 needs-input 상태

## 절차 초안

1. clean working tree와 inspection SHA를 확인한다.
2. read-only draft generator가 있다면 changed paths·재조사 후보·checklist를 만든다.
3. schema가 요구하는 구현 사실을 committed evidence로 확인한다. 실행하지 않은 test는 통과했다고 기록하지 않는다.
4. final document를 갱신하고 implementation snapshot SHA와 docs commit SHA를 구분한다.
5. active workflow marker가 있다면 lifecycle 규칙에 맞춰 종료 또는 유지한다.
6. docs-only commit과 post-commit read-back을 수행한다.

## 출력

- 갱신된 current-state snapshot
- inspection SHA, snapshot SHA, 사실 근거
- 실행한 검증과 실행하지 않은 검증의 구분
- docs-only commit read-back

## 경계

- source/test/configuration/migration/requirement/ADR을 수정하지 않는다.
- task body·Kanban 상태·PR을 직접 정의하지 않는다.
- implementation 완료 여부를 Git history나 draft generator만으로 추정하지 않는다.

## 완료 기준

current-state의 모든 변경 사실이 clean committed evidence에 연결되고, document commit과 snapshot SHA가 혼동 없이 read-back되어야 한다.

## TBD

- current-state 최종 schema
- freshness 판정의 path 범위
- draft generator CLI와 output schema
- active workflow marker의 최종 field와 start/end transition
- inspection 범위 및 regression evidence의 최소 기준
