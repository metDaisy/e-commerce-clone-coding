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

위 명령은 Profile Distribution 자체를 설치한다. 전속 외부 Skill까지 설치하려면
저장소 root에서 `setup-hermes.*`를 실행해야 한다. 각 capability policy의
`skills.external` mapping이 skills.sh 저장소와 Skill 이름을 정의하며,
공통 `apply-hermes-capabilities.py`가 누락된 항목만 다음 대상에 복사한다.

```text
repository/.hermes/skills/<skill-name>/SKILL.md
```

이 디렉터리는 외부 다운로드 캐시이므로 Git에서 무시한다. Built-in Skill과
Profile Distribution에 포함된 custom Skill은 `skills.allowed`에만 선언한다.
따라서 Skill을 capability에 추가할 때는 먼저 내장/배포 Skill인지 확인하고,
그 외 Skill만 `skills.external`에 source를 추가한다.

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

## 책임 소유권

이 문서는 Profile topology와 high-level handoff만 설명한다. 실행 시점의 PM 정책은
`.hermes/profile-distributions/project-manager/SOUL.md`, graph authoring은
`.hermes/profile-distributions/project-manager/skills/write-task/SKILL.md`, 저장 task
schema와 validator는 `.hermes/profile-distributions/project-manager/skills/write-task/references/board-contract.md`가
각각 단일 source of truth다.

프로젝트 공통 hard rule은 `AGENTS.md`와 `docs/agent-workflow.md`가 소유한다. Reviewer의
focus별 판단은 각 Reviewer Profile의 SOUL/Skill이, finding canonicalization과 conflict
보존은 `.hermes/profile-distributions/reviewer-coordinator/SOUL.md`가 소유하며, 이 문서에서는
재정의하지 않는다.

## High-level lifecycle

```text
project-manager
  → Issue·요구사항·현재 상태를 확인하고 graph authoring Skill로 graph 생성
  → 첫 번째 실행 가능한 leaf 하나만 ready
prototype-coder
  → task claim + 최소 구현/테스트 + 결정론적 검증
reviewer-general(focus=spec)
  → same-card review와 재검증
project-manager
  → 승인된 결과를 commit/push하고 다음 leaf를 routing

[Issue의 모든 implementation leaf 완료]
quality review → coordinator → 승인된 refactor → targeted re-review
project-manager
  → 최종 code commit + PR/CI gate + current-state 갱신
  → docs commit + PR merge + Issue 종료
```

세부 순서, review freeze, CI/PR 처리와 완료 조건은 PM SOUL과 해당 Skill/contract를
따른다. 이 문서는 Profile 간 흐름만 보여준다.

## Project Manager와 Kanban 계약

PM과 Kanban의 상세 입력·task-body·evidence·acceptance·verification·dependency 계약은
`.hermes/profile-distributions/project-manager/skills/write-task/SKILL.md`와
`.hermes/profile-distributions/project-manager/skills/write-task/references/board-contract.md`가 소유한다.
PM의 lifecycle 상태 전이와 routing만
`.hermes/profile-distributions/project-manager/SOUL.md`가 소유한다. 이 문서에서는
필드나 validator 규칙을 재기록하지 않는다.

Profile 간 handoff는 Kanban을 authoritative surface로 사용한다. 직접 메시지는 알림일
뿐이며 scope·acceptance·dependency·verification을 전달하는 계약이 아니다.

## Handoff 원칙

Profile 간 durable handoff는 Kanban의 task·run·comment·metadata를 사용한다. 실제
전달 field와 verification 결과는 `board-contract.md`, 각 Profile의 전문 계약과 PM SOUL을
따른다. Memory·추측·완료 선언을 handoff 근거로 사용하지 않는다.
