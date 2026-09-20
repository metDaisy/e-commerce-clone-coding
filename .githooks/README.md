# Git hook

이 디렉터리는 이 저장소에서 버전 관리하는 Git hook 경로다.

## 활성화

각 로컬 clone에서 한 번 실행한다.

```text
git config core.hooksPath .githooks
```

`post-commit`은 커밋된 worktree를 대상으로 Codebase Memory MCP 패키지가
제공하는 공식 CLI인 `codebase-memory-mcp cli index_repository`를 `full`
모드로 동기 실행한다. 기본 프로젝트 이름은 이 저장소의 canonical
Codebase Memory 프로젝트 이름이다. 다른 index namespace가 명시적으로
필요한 경우에만 `CODEBASE_MEMORY_PROJECT_NAME`을 설정한다. 색인에 성공한
`HEAD`는 worktree의 Git metadata에
`codebase-memory/last-indexed-head`로 기록한다. 이 hook은 저장소에 graph
artifact를 저장하지 않는다.

색인은 Git commit이 아니다. 추적 대상 worktree 외부의 Codebase Memory
graph/cache를 갱신하며, freshness 파일도 `.git` metadata 아래에 저장되므로
어떤 commit에도 포함되지 않는다.

색인에 실패하면 `post-commit`이 commit을 되돌릴 수 없으므로 Git commit은
이미 생성된 상태다. 이 hook은 freshness 기록을 삭제하고 0이 아닌 종료
상태를 반환한다. 색인을 다시 성공시키고 기록된 SHA가 `HEAD`와 같아질
때까지 해당 commit을 PM checkpoint로 사용해서는 안 된다.