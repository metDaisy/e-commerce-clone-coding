# Git hook

이 디렉터리는 이 저장소에서 버전 관리하는 Git hook 경로다.

## 활성화

각 로컬 clone에서 한 번 실행한다.

```text
git config core.hooksPath .githooks
```

`post-commit`은 Python script인
`.githooks/index_codebase_memory.py`를 실행한다. 이 script가
`codebase-memory-mcp`를 MCP stdio server로 시작하고 MCP `tools/call`을
통해 `index_repository`를 `full` 모드로 호출한다. CLI subcommand는
사용하지 않는다. 기본 프로젝트 이름은 이 저장소의 canonical Codebase
Memory 프로젝트 이름이다. 다른 index namespace가 명시적으로 필요한
경우에만 `CODEBASE_MEMORY_PROJECT_NAME`을 설정한다. 색인에 성공한
`HEAD`는 worktree의 Git metadata에
`codebase-memory/last-indexed-head`로 기록한다. 이 hook은 저장소에 graph
artifact를 저장하지 않는다.

실행 환경에는 `codebase-memory-mcp`와 Python `mcp` SDK가 필요하다. Hook은 단순히
PATH의 첫 `python`을 사용하지 않는다. 먼저 명시적인 `PYTHON`, 그 다음
`HERMES_MCP_PYTHON`, Hermes 관리 venv, `python3`, `python` 순서로 `import mcp`가
성공하는 interpreter를 선택한다. 다른 Python 실행 파일을 강제해야 하면 `PYTHON`을,
기본 탐색보다 우선할 Hermes MCP interpreter를 지정하려면 `HERMES_MCP_PYTHON`을
설정한다. 명시한 `PYTHON`이 `mcp`를 import하지 못하면 hook은 다른 interpreter로
조용히 대체하지 않고 stale 원인을 보고한다.

색인은 Git commit이 아니다. 추적 대상 worktree 외부의 Codebase Memory
graph/cache를 갱신하며, freshness 파일도 `.git` metadata 아래에 저장되므로
어떤 commit에도 포함되지 않는다.

색인에 실패하면 `post-commit`이 commit을 되돌릴 수 없으므로 Git commit은
이미 생성된 상태다. 이 hook은 freshness 기록을 삭제하고 0이 아닌 종료
상태를 반환한다. 색인을 다시 성공시키고 기록된 SHA가 `HEAD`와 같아질
때까지 해당 commit을 PM checkpoint로 사용해서는 안 된다.