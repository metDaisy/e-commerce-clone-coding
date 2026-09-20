---
name: codebase-memory-mcp
description: Graph에서 symbol·dependency·call path를 찾고 실제 source로 검증합니다.
version: 1.0.0
author: Amaazon project, Hermes Agent
license: MIT
platforms: [linux, macos, windows]
metadata:
  hermes:
    tags: [codebase-memory, graph-search, impact-analysis]
    related_skills: [semble-search]
---

# Codebase Memory MCP Skill

설정된 Codebase Memory graph를 저장소 탐색의 accelerator로 사용한다. Graph는 유일한 사실의
원천이 아니므로, graph에서 얻은 결론은 편집하거나 강하게 주장하기 전에 checked-out source,
test, migration에서 확인한다.

## 사용 시점

- symbol, definition, implementation, caller/callee 위치가 불명확할 때 사용한다.
- dependency, data-flow, cross-domain 영향 또는 call path를 추적할 때 사용한다.
- 현재 index의 project와 freshness를 확인해야 할 때 사용한다.

사용하지 않는 경우:

- 문서 입력만으로 충분한 일반 planning에서는 graph 탐색을 시작하지 않는다.
- 단순한 정확한 문자열 검색에는 `search_files`와 `read_file`을 사용한다.
- index를 생성·갱신·삭제하거나 graph를 변경하는 작업에는 사용하지 않는다. 사용자의 명시적 승인이
  별도로 필요하다.

## 사전 조건

- 현재 Profile에서 `codebase-memory` MCP server가 연결되어 있어야 한다.
- 이 repository의 capability policy에서 허용된 `index_status`, `search_graph`, `trace_path`만 사용한다.
- server, project, index 또는 필요한 capability를 사용할 수 없으면 local repository 탐색으로 전환한다.

## 빠른 참조

| 목적 | 방법 |
|---|---|
| project와 index freshness 확인 | `index_status` |
| definition·implementation·caller 후보 | `search_graph` |
| dependency·call path·impact | `trace_path` |
| graph 결과 확인 | `read_file` 또는 `search_files` |
| MCP unavailable fallback | `search_files`와 `read_file` |

## 절차

1. `index_status`로 현재 project와 index freshness를 확인한다. 확인할 수 없으면 그 제한을
   기록하고 local 탐색으로 전환한다. **완료 기준:** project·freshness 상태 또는 확인 불가 사유가
   남아 있다.
2. 좁은 query로 `search_graph`를 호출해 definition·implementation·caller 후보를 찾는다.
   **완료 기준:** 후보의 symbol과 source 위치가 반환된다.
3. dependency나 call path가 질문의 범위에 포함될 때만 `trace_path`를 호출한다. 필요한 경우
   영향 범위와 test 경로도 함께 확인한다. **완료 기준:** 요청한 경로 또는 경로 부재가 명시된다.
4. graph 후보를 checked-out source·test·migration에서 직접 읽는다. Graph와 checkout이 다르면
   checkout을 현재 사실로 취급하고 index drift를 보고한다. **완료 기준:** 핵심 결론마다 실제
   파일 근거가 연결된다.
5. 작업 결과에 사용한 tool, freshness 제한, graph와 source의 차이를 기록한다. **완료 기준:**
   graph 탐색과 local source 검증이 분리되어 보고된다.

## 주의사항

- graph 결과만으로 구현 완료, API 계약, dependency 존재를 주장하지 않는다.
- `delete_project`, trace ingest, ADR 갱신, repository indexing 같은 mutation을 실행하지 않는다.
- 허용되지 않은 MCP tool을 추측하거나 우회 호출하지 않는다.
- index가 stale하거나 checkout과 다르면 source를 우선하고 drift를 숨기지 않는다.
- Codebase Memory 또는 다른 third-party Skill을 이 절차에서 설치하지 않는다.

## 검증

다음 조건을 모두 만족해야 탐색을 완료한다.

- index freshness를 확인했거나 확인할 수 없는 이유를 기록했다.
- graph에서 얻은 핵심 결과를 실제 checkout의 source·test·migration과 대조했다.
- dependency·call path를 주장할 때 `trace_path` 결과와 local source 근거를 함께 확인했다.
- server나 index가 unavailable이면 local fallback과 그 한계를 보고했다.

출처: https://github.com/DeusData/codebase-memory-mcp
