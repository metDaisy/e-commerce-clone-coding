---
name: codebase-memory-mcp
description: Use when a configured codebase-memory-mcp server can assist with graph-backed code discovery, architecture orientation, symbol lookup, callers and callees, dependency or data-flow tracing, impact analysis, unfamiliar modules, or an explicit Codebase Memory request.
metadata:
  source_repository: https://github.com/DeusData/codebase-memory-mcp
---

# Codebase Memory MCP

Use the configured Codebase Memory graph as a discovery accelerator, not as the sole source of truth. Confirm graph-derived conclusions with source snippets or local files before editing code or making strong claims.

## Workflow

1. Discover the Codebase Memory tools exposed by the current MCP client; clients may prefix or rename tool namespaces.
2. Call `list_projects` when available and use the exact indexed project name. If the repository is not indexed, continue with local exploration or ask before calling `index_repository`.
3. Before branch-sensitive or edit-sensitive conclusions, use `index_status` or `detect_changes` when available. If freshness cannot be established, disclose that limitation and verify locally.
4. Use `get_architecture` once for orientation in an unfamiliar repository or subsystem.
5. Use `search_graph` for definitions, implementations, routes, classes, interfaces, callers, and related symbols.
6. Use `search_code` or repository text search for literals, config keys, test identifiers, error messages, and non-code files.
7. Confirm graph findings with `get_code_snippet` or local source.
8. Use `trace_path` for callers, callees, dependency paths, data flow, cross-service paths, and impact analysis.
9. Use `get_graph_schema` before `query_graph`; reserve custom queries for multi-hop or aggregate questions.
10. When graph and checked-out source disagree, treat source as current and report likely index drift.

## Safety and fallback

- Do not install Codebase Memory or another third-party Skill from this workflow.
- Do not call `delete_project`, ingest traces, update ADRs, or index a repository unless the user explicitly requested or approved it.
- Fall back to repository exploration when the server, project, index, or required capability is unavailable.

출처: https://github.com/DeusData/codebase-memory-mcp
