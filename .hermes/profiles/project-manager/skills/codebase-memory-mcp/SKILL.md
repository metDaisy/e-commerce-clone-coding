---
name: codebase-memory-mcp
description: "PM이 behavior의 symbol·dependency·call path 후보를 찾고 현재 session에서 index freshness를 확인할 수 있을 때 사용한다."
metadata:
  source_repository: https://github.com/DeusData/codebase-memory-mcp
---

# Codebase Memory MCP

Use the configured Codebase Memory graph as a discovery accelerator, not as the sole source of truth. Confirm graph-derived conclusions with source snippets or local files before editing code or making strong claims.

## Workflow

이 Profile에서 허용된 `index_status`, `search_graph`, `trace_path`만 사용한다. 문서 입력이 충분한
일반 planning에서는 source graph 탐색을 시작하지 않는다.

1. `index_status`로 현재 project와 index freshness를 확인한다. 확인할 수 없으면 제한을 기록하고 local
   exploration으로 전환한다.
2. `search_graph`로 definition·implementation·caller 후보를 찾고 `trace_path`로 dependency/call path를
   확인한다.
3. 후보를 committed local source/test/migration에서 직접 읽는다. Graph와 checkout이 다르면 source를
   현재 사실로 취급하고 index drift를 보고한다.

## Safety and fallback

- Do not install Codebase Memory or another third-party Skill from this workflow.
- Do not call `delete_project`, ingest traces, update ADRs, or index a repository unless the user explicitly requested or approved it.
- Fall back to repository exploration when the server, project, index, or required capability is unavailable.

출처: https://github.com/DeusData/codebase-memory-mcp
