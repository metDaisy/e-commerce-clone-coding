# 프로젝트 로컬 Hermes 구성

이 디렉터리는 저장소에서 관리하는 Hermes Profile Distribution, 프로젝트 플러그인,
bootstrap 스크립트를 포함한다. Runtime Profile의 credential, Memory, session,
state database, log 또는 machine-specific path는 이 저장소에 포함하지 않는다.

## 현재 Profile

현재 저장소에는 다음 세 개의 Profile Distribution이 구현되어 있다.

| Runtime Profile | Distribution 원본 | 역할 | 전속 Skill |
|---|---|---|---|
| `project-manager` | `.hermes/profiles/project-manager/` | 승인된 requirement에서 task graph를 만들고 Coder·Review·release lifecycle을 조율 | `service-planning`, `create-triage`, `build-task-graph`, `run-workflow`, `sync-docs`, `update-current-state`, `codebase-memory-mcp`, `semble-search` |
| `coder` | `.hermes/profiles/coder/` | PM이 승인한 backend Impl card를 구현하고 검증 | `run-impl-card`, `implement`, `codebase-memory-mcp`, `semble-search` |
| `reviewer` | `.hermes/profiles/reviewer/` | 완료된 implementation checkpoint를 다섯 독립 축으로 검토 | `run-review`, `review-spec`, `review-maintainability`, `review-persistence`, `review-architecture`, `review-evolution-compatibility`, `improve-codebase-architecture`, `codebase-memory-mcp`, `semble-search` |

Profile별 실제 허용 Skill·toolset·MCP 목록의 기준은 각 Distribution의
`capabilities.yaml`이다. 상세한 역할과 lifecycle은 각
Profile의 `README.md`, `SOUL.md`, Skill 문서를 따른다.

## 공통 Skill

세 Profile이 함께 사용하는 `semble-search`와 `codebase-memory-mcp`는 중복 없이
`.hermes/skills/`에 하나씩 둔다. Profile 설치 스크립트는 repository root를 세 Profile에서
trust하고 capability policy의 allowlist를 read-back하므로 별도의 공통 Skill 설치 스크립트는
필요하지 않다. Profile 전용 Skill은 각 Distribution의 `profiles/<name>/skills/`에 둔다.

## 설치와 bootstrap

Profile 설치와 프로젝트 plugin 설치를 별도 스크립트로 분리했다.

### Profile 설치

```bash
# Linux/macOS 또는 Bash 환경
bash .hermes/scripts/install-profiles.sh

# Windows PowerShell
.\.hermes\scripts\install-profiles.ps1
```

Profile 설치 스크립트는 다음을 수행한다.

- `.hermes/profiles/project-manager`를 `project-manager`로 설치한다.
- `.hermes/profiles/coder`를 `coder`로 설치한다.
- `.hermes/profiles/reviewer`를 `reviewer`로 설치한다.
- 세 Profile의 `terminal.cwd`를 현재 repository root로 설정한다.
- 세 Profile이 repository root의 `.hermes/skills/`를 탐색할 수 있도록 trust한다.
- 세 Profile에서 `kanban.auto_decompose`를 `false`로 설정한다.
- 새 `reviewer` Profile에 policy가 요구하는 bundled `hermes-agent` Skill을 복원한다.
- `project-manager`의 중복 외부 Skill 탐색 경로를 비운다.
- `.hermes/scripts/apply-hermes-capabilities.py`로 capability policy를 적용하고 read-back한다.
- `HERMES_MODEL`이 있으면 세 Profile에 model route를 적용한다. 이때 `HERMES_PROVIDER`와
  `HERMES_BASE_URL`은 선택 사항이며, 없으면 각 Profile에서 `hermes model`로 설정해야 한다.

### Plugin 설치

```bash
# Linux/macOS 또는 Bash 환경
bash .hermes/scripts/install-plugins.sh

# Windows PowerShell
.\.hermes\scripts\install-plugins.ps1
```

Plugin 설치 스크립트는 repository-owned `agent-audit`를 Hermes Plugin Validate와
Plugin Doctor로 검증하고, `project-manager` Profile의 `plugins.enabled`를 read-back한다.
Project-local plugin discovery는 프로세스 시작 환경에서 별도로 opt-in해야 한다.

Project-local plugin은 opt-in이다. Hermes CLI를 실행할 때 다음 환경 변수를 켠다.

```bash
HERMES_ENABLE_PROJECT_PLUGINS=true hermes --profile coder
```

Desktop 앱에서는 실행 전에 같은 환경 변수를 설정하거나 Profile/plugin reload 흐름을
사용한다.

## Capability policy

각 Distribution은 `profiles/<source-name>/capabilities.yaml`에 완전한 allowlist를
선언한다. 이 파일에는 Skill, built-in toolset, MCP server/tool, GitHub write 권한이
기록되지만 endpoint와 credential은 기록하지 않는다.

현재 MCP allowlist는 다음과 같다.

| Profile | 허용 MCP server |
|---|---|
| `project-manager` | `codebase-memory`, `semble-mcp`, `github-mcp`, `gradle-mcp` |
| `coder` | `semble-mcp`, `codebase-memory`, `gradle-mcp` |
| `reviewer` | `semble-mcp`, `codebase-memory`, `gradle-mcp` |

공통 적용 스크립트는 Hermes가 내부적으로 사용하는 deny-list를 allowlist의 여집합으로
계산하고, 설정된 MCP server 중 허용되지 않은 server를 비활성화하며, 각 허용 server에
정확한 `tools.include` filter를 적용한다. 적용 후 유효한 설정을 다시 읽어 검증한다.

```bash
python .hermes/scripts/apply-hermes-capabilities.py \
  --policy .hermes/profiles \
  --profile project-manager \
  --profile coder \
  --profile reviewer \
  --project-root .
```

MCP server endpoint는 각 컴퓨터의 local Profile에 미리 설정되어 있어야 한다. 허용 목록에
있지만 local Profile에 없는 server는 경고만 표시하며, machine-local endpoint를 알 수
없으므로 자동으로 만들지 않는다.

## 프로젝트 플러그인과 runtime log

프로젝트 플러그인 원본은 검토·커밋할 수 있도록 `.hermes/plugins/`에 둔다.

- `agent-audit/`: Agent lifecycle과 validator evidence를 관찰하는 runtime hook

`agent-audit`는 `project-manager` capability policy에서 활성화하도록 선언되어 있다.
플러그인의 구현과 회귀 테스트는 [`plugins/agent-audit/README.md`](plugins/agent-audit/README.md)를
기준으로 한다.

`.hermes/events.jsonl`은 Git에서 무시되는 local runtime data이며 compact metadata만
기록한다.

- Skill lifecycle: Skill 이름, action, provenance, opaque correlation ID
- Tool lifecycle: tool 이름, 상태, 소요 시간, 안전한 project-relative path
- Validation trigger/result: Rule ID, validator category, generation, pass/fail
- Verification gate: 변경 path와 누락·실패한 Rule ID
- Workflow deviation: 직접 Gradle 사용 또는 누락·실패·비연결 verification
- Session end: 완료·중단 상태

Prompt, conversation history, terminal command, raw tool argument/result, reasoning,
credential, absolute path는 저장하지 않는다.

## Verification 동작

로깅은 fail-open이다. Java 파일이 수정된 뒤 Hermes verification gate에 도달하면
Agent에게 `gradle-mcp`를 통한 Checkstyle, 관련 test, `ModularityTest` 실행을 요청한다.
최근 `gradle-mcp` 호출이 실패한 경우 hook은 수정 후 재검증하도록 제한된 diagnostic
nudge를 제공한다. 이 플러그인은 tool call을 차단하지 않으며 Gradle을 직접 실행하지 않는다.

관련 회귀 테스트는 repository Python interpreter로 실행한다.

```bash
python .hermes/plugins/agent-audit/test_agent_audit.py
python .hermes/scripts/test_apply_hermes_capabilities.py
python .hermes/scripts/test_setup_hermes.py
```

플러그인 discovery나 manifest 동작을 변경한 경우에는 관련 Hermes Plugin Doctor/runtime
검사도 수행한다. 정적 문서와 회귀 테스트가 plugin runtime 활성화를 의미하지는 않는다.
