# Agent Profile Workflow

## 목적

이 문서는 `renewal/harness` 브랜치에서 Profile Distribution으로 Prototype Coder,
독립 Reviewer Profiles, Feedback 기반 Refactor Coder를 운영하는 방법을 정의한다.

프로젝트 공통 규칙은 `AGENTS.md`, 작업 절차는 `docs/agent-workflow.md`,
구조 원칙은 `docs/architecture.md`와 ADR을 따른다. 이 문서는 Agent Profile의
역할과 인수인계 계약만 정의하며 기존 프로젝트 규칙을 대체하지 않는다.

## 기본 원칙

- Project Manager, Coder, Reviewer, Coordinator는 서로 다른 책임을 가진 Hermes Profile을 사용한다.
- 각 Profile의 `SOUL.md`, Memory, Session, `state.db`, 설정과 Skill 상태는 분리한다.
- 프로젝트 규칙과 설계의 확정 사실은 Memory가 아니라 저장소 문서와 코드에 둔다.
- Profile 간에는 Memory를 공유하지 않고 diff, 요구사항 경로, 검증 결과,
  구조화된 finding만 명시적으로 전달한다.
- Profile별 배포 원본은 저장소의 `.hermes/profile-distributions/<profile>/`에
  버전 관리한다.
- 배포 원본의 `SOUL.md`는 설치 시 각 사용자의 Hermes Profile로 복사하고,
  전속 Skill은 `setup-hermes.*`가 skills.sh에서 보안 스캔 후 내려받는다.
  개인 runtime 경로를 저장소에 기록하거나 공유하지 않는다.
- Profile은 파일 시스템 격리가 아니다. Reviewer의 수정 방지는 capability 제한,
  수동 승인, read-only workspace 또는 OS/sandbox 권한으로 보완한다.

## Distribution source와 runtime

프로젝트의 `.hermes/`는 project-local Plugin과 Profile Distribution source를
구분한다. `.hermes/profile-distributions/`는 Git으로 공유하는 배포 원본이고,
실제 Profile runtime은 각 사용자의 `HERMES_HOME` 아래에 생성된다.

```text
repository/.hermes/profile-distributions/reviewer-general/
  distribution.yaml
  SOUL.md
  README.md                 # skills.sh identifiers and install notes

$HERMES_HOME/profiles/reviewer-general/
  SOUL.md
  skills/
  memories/
  sessions/
  state.db
```

다른 사용자는 clone 후 다음과 같이 local Distribution을 설치한다.

```text
hermes profile install ./.hermes/profile-distributions/<profile> --name <profile> --alias
```

위 명령은 Profile Distribution 자체를 설치한다. 전속 Skill까지 설치하려면
저장소 root에서 `setup-hermes.*`를 실행해야 한다. Hermes Distribution manifest는
registry Skill ID를 자동 설치하는 필드를 제공하지 않으므로, bootstrap이
`hermes --profile <profile> skills install skills-sh/...`를 명시적으로 실행한다.

전체 Profile을 한 번에 설치할 때는 운영체제에 맞는 bootstrap script를 사용한다.

```text
# Windows PowerShell
./scripts/setup-hermes.ps1

# Bash / Git Bash
bash ./scripts/setup-hermes.sh
```

스크립트는 local Distribution만 설치·갱신하고 각 사용자의 credential, Memory,
Session, `state.db`는 보존한다. 설치 후 각 Profile의 runtime `terminal.cwd`를 clone된
프로젝트 root로 설정하여 `AGENTS.md`와 프로젝트 context가 fresh session에 주입되도록
한다. 이 절대 경로는 Distribution에 저장하지 않는다.
모델 endpoint도 credential과 함께 배포하지 않는다. 각 사용자는 자신의 provider/model을
`hermes model`로 설정하거나 `HERMES_MODEL`과 선택적인 `HERMES_PROVIDER`,
`HERMES_BASE_URL`을 지정해 script를 재실행한다.

Distribution에는 credential, Memory, Session, `state.db`, 로그와 machine-specific
absolute path를 포함하지 않는다. 프로젝트 공통 규칙은 기존 `AGENTS.md`가
담당하며, Profile 역할은 각 Distribution의 `SOUL.md`가 담당한다. 루트에
`HERMES.md`를 추가해 `AGENTS.md`를 대체하지 않는다.

## Profile 구성

| Profile | 책임 | 전속 Skill 또는 도구 | 주요 산출물 |
|---|---|---|---|
| `project-manager` | Issue 시작·task graph·Kanban·commit·push·PR·merge·Issue 종료 | Kanban orchestration, GitHub write | 근거 있는 task graph와 lifecycle 상태 |
| `prototype-coder` | 최소 동작 Vertical Slice 구현 | `java-springboot`, `java-junit` | 실행 가능한 최소 구현과 범위가 명확한 diff |
| `reviewer-general` | Spec·Maintainability·Compatibility를 focus별 독립 검토 | `code-review`, `simplify-code`, `compat-review` | 축별 evidence finding |
| `reviewer-deep` | Architecture·Persistence를 focus별 독립 검토 | `improve-codebase-architecture`, `architecture-review`, `305-frameworks-spring-boot-modulith`, `jpa-patterns` | 구조·query evidence finding |
| `reviewer-coordinator` | 독립 결과 취합과 우선순위 결정 | 전용 검토 Skill 없음 | 중복 제거된 최종 review report |
| `refactor-coder` | 승인된 finding만 최소 범위로 수정 | finding에 따라 `java-refactoring-*` 등 선택 | 수정 diff와 회귀 검증 결과 |

`codebase-design`은 `reviewer-deep`의 architecture focus가 사용하는 설계 어휘로
취급한다. 두 Reviewer Profile의 Skill은 모두 skills.sh에서 설치하지만, 한 세션에서는
반드시 하나의 `focus`만 실행한다.

`requesting-code-review`와 `simplify-code`는 자동 수정·검증·commit 흐름을 포함할
수 있으므로 순수 read-only Reviewer로 사용할 때는 dry-run과 수정 금지를 명시한다.
프로젝트의 Gradle 실행 규칙이 항상 우선한다.

## Hard Rules와 Soft Rules

### Prototype에서도 반드시 지키는 Hard Rules

- `AGENTS.md`와 프로젝트 구조·보안 규칙
- 공개 API의 최소 계약과 데이터 손상 방지
- 명백한 Spring Modulith 모듈 경계 위반 금지
- 기존 migration을 수정하지 않고 새 migration 사용
- credential·token·password·API key 비노출
- 기존 미커밋 변경 보존
- Gradle 작업은 반드시 `gradle-mcp`로 실행

### Reviewer가 판단하는 Soft Rules

- 중복 제거와 코드 단순화
- 추상화 수준과 Design Pattern 도입 여부
- Deep Module과 seam 개선
- query count와 fetch 전략
- 클래스 의존성·책임 분산
- 향후 확장 지점과 API 호환성

Soft Rule은 위반 자체를 자동 실패로 보지 않는다. 실제 영향, 변경 비용,
근거와 confidence를 함께 보고한다.

## 실행 흐름

```text
project-manager
  → current-state.md + 요구사항 문서 + GitHub Issue tree + Git 상태 확인
  → 현재 Issue의 모든 근거 있는 task graph와 dependency 작성
  → 고정 Quality Reviewer/commit/finalization task 생성
  → 첫 번째 leaf task 하나만 ready promotion
prototype-coder
  → task claim + 최소 구현/테스트
  → 결정론적 검증
reviewer-general(focus=spec)
  → changes-requested면 coder 수정 후 재검증·재리뷰
  → approve면 implementation task done
project-manager
  → commit task 수행 + push + 다음 leaf task ready promotion

[Issue의 모든 leaf task 완료]
quality-maintainability → quality-persistence
  → quality-architecture → quality-compatibility
  → 동일 final_review_base_sha를 직렬 검증
reviewer-coordinator
  → canonical findings와 conflicts 취합
refactor-coder
  → 승인된 finding만 수정 + 결정론적 검증
  → 영향 받은 축만 targeted re-review
project-manager
  → 최종 code commit + push + PR 생성
  → CodeRabbit feedback task/CI failure task 처리
  → current-state.md를 최종 code SHA 기준으로 갱신
  → 별도 docs commit + push + clean tree 확인
  → Issue start/target date 갱신 + PR merge + Issue 종료
```

Prototype은 구조 품질을 일부 미루는 단계이지, 프로젝트의 Hard Rule을 무시하는
단계가 아니다. Reviewer의 독립성은 Profile 수가 아니라 각 focus를 새 세션·동일
fixed diff·독립 Memory로 실행하는 방식으로 보장한다.

## Project Manager와 Kanban 계약

Project Manager의 입력은 `current-state.md`, 그 문서의 기준 SHA, 커밋된 코드·테스트·설정·
Flyway, GitHub Issue tree, 요구사항·ADR, 현재 Kanban이다. 기준 SHA가 HEAD와 다르거나
dirty worktree가 있으면 stale/unknown으로 기록하고, 미커밋 변경을 완료된 범위로
계산하지 않는다.

Project Manager는 Coder·Reviewer의 source/test/migration 구현을 대신하지 않는다. 다만
승인된 lifecycle 결과를 반영하는 commit·push, 최종 `current-state.md` 갱신, GitHub
Issue/PR과 merge는 Project Manager의 책임이다. Kanban에는 다음을 기록한다.

- `github_issue`, domain/parent issue, 현재 상태 기준 SHA
- task 생성 근거: 원문을 직접 확인한 repository-relative 경로와 heading 또는 line
  range, 근거 유형(`goal`, `state`, `constraint`), 그리고 해당 근거가 뒷받침하는
  task 판단
- task scope와 `out_of_scope`
- 성공·거절·경계 조건을 포함한 acceptance criteria
- 필요한 deterministic verification과 예상 결과
- 실제 실행 dependency와 unknown/needs-input

Issue 부모는 추적용 epic으로 두고 실행 dependency는 leaf task 사이에만 연결한다.
근거 없는 task를 생성하지 않는다. 모든 task에는 `evidence`를 기록한다.
`evidence`는 검색 결과의 요약이나 URL이 아니라 직접 읽은 원문의 repository-relative
path와 heading 또는 line range를 사용하며, 해당 자료가 task의 어떤 판단을
뒷받침하는지 함께 설명한다. 근거 유형은 목표를 증명하는 `goal`, 현재 구현의 gap
또는 선행 조건을 증명하는 `state`, 구조·API·DB 제약을 증명하는 `constraint`로
구분한다. 구현 task에는 최소한 `goal` 근거와 `state` 근거를 각각 하나씩 기록한다.
일반적인 구현 Issue task는 요구사항 문서와 `current-state.md`를 우선 근거로 삼고,
구조·API·DB 제약이 있으면 관련 ADR, architecture 문서, 코드·테스트·migration의
정확한 위치를 `constraint` 근거로 추가한다.

Task graph 생성과 실행 지시를 구분한다. Issue를 구현 대상으로 활성화할 때
Project Manager는 현재 Issue 범위에서 근거가 충분한 최소 구현 task graph를 먼저 만들고,
알려진 task를 한 번의 planning pass에서 생성·보정한다. 각 task에는 담당 Profile과
실제 dependency를 기록한다. dependency가 충족되지 않은 task는 `todo` 또는
`blocked`로 두고, 현재 순서상 첫 번째 실행 가능한 leaf task 하나만 `ready`로
promotion한다. 명시적인 병렬 실행 근거가 없는 한 여러 task를 동시에 `ready`로
만들지 않는다. Coder의 검증과 implementation commit 이후 Project Manager가 완료 상태와
dependency를 read-back하고 다음 leaf task 하나를 `ready`로 promotion한다.

범위나 dependency의 근거가 부족한 추측성 downstream task는 미리 생성하지 않고
다음 Project Manager 실행으로 미룬다. 기존 task가 있으면 중복 생성보다 상태 read-back과
보정을 우선한다. Coder는 Project Manager가 `ready`로 promotion한 task 하나만 claim하고,
scope를 재정의하지 않는다. 불명확한 요구사항이나 stale state는 추측하지 않고
Kanban을 `blocked` 또는 `needs-input`으로 되돌린다.

## 복잡도와 불필요한 로직 판단 기준

줄 수, `for`문 개수, 클래스 개수, 생성자 파라미터 수는 자동 위반 기준이 아니라
검토 신호다. Maintainability focus가 finding으로 올리려면 다음 중 하나를 실제
`path:line`과 함께 입증한다.

- 작은 정책 변경이 여러 unrelated 위치로 전파되는 change amplification
- 삭제해도 caller에 복잡도가 돌아오지 않는 pass-through/shallow abstraction
- 하나의 Module이 서로 무관한 여러 변경 이유를 갖는 낮은 응집도
- caller가 내부 workflow나 implementation detail을 조합해야 하는 인터페이스
- 삭제해도 observable behavior가 변하지 않는 dead/redundant logic

불필요한 로직은 도달 불가능한 branch, 사용되지 않는 계산, 중복 fetch/filter/sort,
이미 보장된 invariant를 의미 없이 반복하는 guard처럼 삭제 전후 동작을 설명할 수
있을 때만 보고한다. 취향·style-only 지적은 제외하고, 근거가 약하면 open question으로
낮춘다.

## Persistence 판단 기준

Persistence focus는 query count와 query behavior를 분리한다. 기존 `QueryInspector`,
`BaseRepositoryTest`, `ensureQueryCount(...)`를 사용해 fresh entity, association
traversal, N+1, pagination content/count를 측정한다. 추가로 row volume, selectivity,
projection/over-fetch, join cardinality, index, sort, lock, transaction scope와
필요한 execution plan을 확인한다. “query 1개가 항상 최선”이라고 판단하지 않으며,
측정하지 않은 우려는 suspicion/open question으로 보고한다.

## Commit·current-state·Issue 날짜 규칙

Reviewer A의 Spec 승인이 끝나면 implementation task는 done이 되고, Project Manager가
연결된 commit task를 수행하여 task 단위 implementation commit을 만들고 push한다. Issue의
모든 implementation task가 끝난 뒤 Quality Reviewer를 직렬 실행한다.
Coordinator가 승인한 후 Refactor Coder의 수정은 별도 follow-up code commit으로 남긴다.
기능·API·persistence·architecture에 영향이 있으면 해당 focus만 targeted re-review한다.

최종 code/refactor commit 이후 `current-state.md`를 재생성하고 별도 `docs:` commit으로
저장한다. 문서의 기준 SHA는 문서 commit이 아니라 직전 최종 code commit이다. 진행 중인
Issue는 Kanban이 상태를 표현하고 `current-state.md`에는 반영하지 않는다.

Issue `start`는 해당 Issue의 최초 implementation commit 날짜, `target`은 최종
code/refactor commit 날짜다. Project Manager·reviewer·current-state 문서·Issue metadata
commit은 날짜 계산에서 제외한다. 날짜를 갱신한 뒤 Issue와 Kanban을 다시 읽어 확인한다.

## Quality·CI·CodeRabbit gate

Quality Reviewer task는 `maintainability → persistence → architecture → compatibility`
순서로 직렬 실행한다. 첫 Quality Reviewer 시작 전에 `final_review_base_sha`를 고정하고,
각 Reviewer는 동일 SHA와 clean working tree를 확인한다. Reviewer는 코드를 수정하지
않으며, Coordinator가 통합한 승인 finding만 Refactor Coder가 수정한다.

GitHub CI는 필수 gate다. Build 또는 test/CI가 실패하면 원인별로 쪼개지 않고 실패한
run 하나당 하나의 `ci-failure` task를 만든다. task에는 run URL, failed job/step,
오류 요약과 재검증 방법을 남긴다.

CodeRabbit은 advisory reviewer다. review가 한도 초과 또는 미실행이어도 deterministic
verification과 GitHub CI가 통과하면 진행할 수 있지만, 미실행을 승인으로 해석하지 않는다.
Actionable comment 하나마다 `refactor-coder` 담당 PR feedback task 하나를 만들고, 반영
여부와 근거를 해당 comment에 한글로 reply한다. 필요한 경우 `@coderabbitai review`를
명시적으로 요청한다. PR 본문에는 실제 `Closes #<issue-number>`가 있어야 한다.

## Finding 계약

모든 Reviewer는 다음 필드를 사용한다.

- `severity`: `BLOCKER`, `HIGH`, `MEDIUM`, `LOW`
- `confidence`: `high`, `medium`, `low`
- `path`: 저장소 상대 경로
- `line`: 가능한 경우 시작 행
- `category`: 담당 검토 축
- `finding`: 문제 설명
- `evidence`: 코드·테스트·문서 근거
- `impact`: 사용자·데이터·변경 영향
- `recommendation`: 최소 수정 방향
- `blocking`: Refactor 전 필수 수정 여부

근거 없는 일반론, 단순 취향, 이미 도구가 결정하는 style 지적은 최종 finding에서
제외한다. Reviewer 간 결론이 충돌하면 Coordinator가 임의로 숨기지 않고 충돌을
기록한다.

Coordinator는 Reviewer가 반환한 Markdown 또는 JSON을 다음 canonical 객체로
정규화한다. 원래 Reviewer, 원문 finding, 정규화 이유를 함께 보존한다.

```json
{
  "severity": "BLOCKER|HIGH|MEDIUM|LOW",
  "confidence": "high|medium|low",
  "path": "repository/relative/path",
  "line": 1,
  "category": "spec|maintainability|architecture|persistence|compatibility",
  "finding": "problem statement",
  "evidence": ["path:line or command result"],
  "impact": "user, data, or change impact",
  "recommendation": "smallest safe next action",
  "blocking": true,
  "source_reviewer": "reviewer-general",
  "conflicts": []
}
```

`source_reviewer`가 없는 finding은 confidence를 임의로 높이지 않고 open question으로
분리한다. 중복은 동일 원인·동일 위치일 때만 묶고, severity나 recommendation이
다르면 원문과 충돌을 모두 보존한다.

## Persistence 검증 원칙

`reviewer-deep`의 persistence focus는 다음 프로젝트 자산을 결정론적 근거로 사용한다.

- `docs/testing-guide.md`
- `QueryInspector`
- `BaseRepositoryTest`
- `ensureQueryCount(...)`

Repository 작업별 예상 query count, fresh entity 조회, association 접근,
페이지별 결과 분리와 count/content query를 검증한다. Query count가 낮다는
사실만으로 최적화를 단정하지 않으며, over-fetch, 인덱스와 실행 계획은 별도
근거로 판단한다.

## 인수인계와 Memory 경계

Refactor Coder에게 전달하는 것은 다음뿐이다.

- 대상 base commit 또는 명확한 diff
- 관련 요구사항과 기준 문서 경로
- Reviewer finding
- 실제 테스트·lint·build·query 검증 결과
- 사용자가 승인한 수정 범위

Coder의 작업 Memory, Reviewer의 과거 판단, 추측과 완료 선언은 인수인계하지
않는다. 현재 사실은 다시 source, test, migration과 문서에서 확인한다.
