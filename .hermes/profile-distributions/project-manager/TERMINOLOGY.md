# Project Manager 용어집

> PM workflow를 설계·검토할 때 사용하는 용어의 기준이다. 이 문서는 용어의 의미만
> 정의하며, 세부 절차는 workflow Skill, persisted field와 validator 규칙은
> `skills/write-task/references/board-contract.md`가 소유한다.

## 저장소와 스냅샷

아래 예시에서 `H0`은 구현을 조사한 clean commit, `H1`은 active workflow marker만
담은 문서 commit, `H2`는 구현 child checkpoint commit, `H3`은 종료 checkpoint 문서
commit이다.

| 용어 | 의미 | 예시 |
|---|---|---|
| clean working tree | ignored 파일을 제외하고 staged·unstaged·untracked 변경이 없는 repository 상태 | `git status --short` 출력이 없다. `H0`을 조사하기 직전과 `H3` 직후가 이에 해당한다. |
| dirty working tree | clean working tree가 아닌 repository 상태. 변경이 어떤 작업에 속하는지는 이 상태만으로 알 수 없다. | Coder가 `src/main/.../CatalogService.java`를 수정했지만 아직 commit하지 않은 상태다. |
| inspection SHA | `current-state.md` 갱신을 위해 실제로 조사한 committed implementation의 SHA | PM이 `H0`의 source·test·migration을 조사했다면 inspection SHA는 `H0`이다. |
| implementation snapshot SHA | `current-state.md`에 기록하는 inspection SHA. 이 문서의 후속 문서 commit SHA와 구분한다. | `H3`은 `docs/current-state.md`를 갱신한 문서 commit이지만, 문서에는 조사 대상인 `H2`를 snapshot SHA로 기록한다. |
| planning baseline SHA | Issue planning과 task contract가 근거로 삼는 committed repository 기준점 | `H0` 뒤에 marker-only commit `H1`을 만들고 card를 생성하면 planning baseline SHA는 `H1`이다. |
| fresh snapshot | implementation snapshot SHA 이후 implementation-covered committed 변경이 없는 상태 | snapshot SHA가 `H0`이고 `H1`이 `current-state.md`만 바꿨다면, `H1`에서 snapshot은 fresh다. |
| stale snapshot | implementation snapshot SHA 이후 implementation-covered committed 변경이 있거나, snapshot 기준을 신뢰할 수 없는 상태 | `H2`가 `src/main`을 바꿨는데 문서 snapshot SHA가 아직 `H0`이면 stale이다. |
| implementation-covered path | 구현 사실 snapshot에 영향을 주는 backend/frontend source·test, build/configuration, Flyway migration 경로 | `src/main/**`, `src/test/**`, `amaazon-front/**`, `build.gradle`, `config/**`, `gradle/**`, `settings.gradle`, `src/main/resources/db/migration/**` |

### 예시 시간선

```text
H0  clean implementation commit을 조사
    current-state snapshot SHA = H0

H1  active workflow marker만 기록한 docs commit
    planning baseline SHA = H1
    implementation snapshot SHA = H0  (H1은 구현을 바꾸지 않았으므로 fresh)

H2  child implementation checkpoint commit
    implementation snapshot SHA = H0  (H2가 구현을 바꿨으므로 stale)
    active workflow marker가 있으므로 새 Issue planning은 시작하지 않음

H3  최종 구현 H2를 조사하고 current-state를 갱신한 docs commit
    implementation snapshot SHA = H2
    active workflow marker 종료
```

## 진행과 복구

| 용어 | 의미 |
|---|---|
| active workflow marker | `docs/current-state.md`에 committed로 남기는 진행 중 implementation workflow의 repository-level 복구 표식. platform별 board·task ID는 포함하지 않는다. |
| 시작 checkpoint | delivery branch에서 active workflow marker만 기록하고 커밋한 시점 |
| 종료 checkpoint | 최종 implementation snapshot으로 `current-state.md`를 갱신하고 active workflow marker를 종료한 문서 commit |
| interrupted workflow | active workflow marker가 남아 있지만 정상 workflow 기록 또는 실행이 이어지지 않은 상태 |
| recovery | active workflow marker와 working-tree evidence를 바탕으로 중단된 동일 작업을 안전하게 재개하는 과정 |
| unattributed change | active workflow marker 또는 확인 가능한 작업 계약에 연결할 수 없는 dirty 변경 |

## 작업 단위와 review

| 용어 | 의미 |
|---|---|
| root Issue | 도메인 또는 delivery 범위의 최상위 work item |
| leaf Issue | 하위 work item이 없으며 구현 대상으로 선택할 수 있는 work item |
| delivery branch | 선택된 leaf Issue를 구현하기 위해 사용하는 branch |
| child implementation card | Coder가 수행하는 하나의 독립적으로 검증 가능한 구현 계약 |
| root-review-{n} card | 모든 관련 child implementation card가 완료된 뒤 Reviewer가 수행하는 aggregate review 계약 |
| aggregate contract | root-review card에 PM이 작성하는 child contract의 공통 불변조건·교차 API 일관성·제외 범위의 요약 |
| review question | PM이 root-review card에 작성하는 Reviewer 검토 관점 또는 확인 질문 |
| PM checkpoint | Coder의 self-verification 뒤 PM이 같은 child card에서 evidence·commit boundary·read-back을 확인하고 완료시키는 운영 gate |
| review handoff | 구현자가 완료 전 reviewer에게 task와 검증 evidence를 넘기는 lifecycle 전이 |
| rework draft | review finding에서 기계적으로 추출한 후속 작업의 초안. PM이 scope·context·AC를 보완하기 전에는 실행 계약이 아니다. |
| replacement task | stale 또는 obsolete unfinished task를 대체하기 위해 새로 만든 task |
| archived task | replacement로 더 이상 실행하지 않는 historical task. 완료 기록을 수정하거나 되돌린다는 뜻이 아니다. |

## 문서 책임

| 문서 | 소유하는 내용 |
|---|---|
| `SOUL.md` | PM identity, 권한 경계, 불확실성·보고 기본값 |
| workflow Skill | planning admission, routing, checkpoint, recovery, finalization 절차 |
| `board-contract.md` | task body field, graph 불변조건, validator 규칙 |
| `TERMINOLOGY.md` | 이 용어집의 개념적 의미 |
| `README.md` | distribution 설치·구성·문서 안내 |
