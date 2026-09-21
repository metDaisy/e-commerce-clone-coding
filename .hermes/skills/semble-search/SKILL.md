---
name: semble-search
description: 코드·문서·설정 위치가 불명확할 때 의미 기반 검색을 수행합니다.
version: 1.0.0
author: Amaazon project, Hermes Agent
license: MIT
platforms: [linux, macos, windows]
metadata:
  hermes:
    tags: [semantic-search, repository, code-discovery]
    related_skills: [codebase-memory-mcp]
---

# Semble 검색 Skill

Semble을 사용해 코드·문서·설정에서 동작이나 심볼을 기준으로 후보 위치를 찾는다. 검색 결과는
위치를 찾기 위한 locator이며 사실의 증거가 아니므로, 실제 checkout의 source·test·migration을
읽어 결론을 확인한다.

## 사용 시점

- 구현 위치나 관련 동작을 정확히 모를 때 사용한다.
- 코드·문서·설정의 의미 기반 후보를 찾을 때 사용한다.
- 이미 찾은 결과와 유사한 구현을 찾아야 할 때 `find_related`를 사용한다.
- 승인된 requirement와 fresh `current-state`만으로 계획을 세울 수 있으면 source 검색을 시작하지 않는다.

사용하지 않는 경우:

- 정확한 문자열의 모든 occurrence가 필요한 경우에는 repository literal search를 사용한다.
- 검색 결과만으로 구현 상태나 변경 완료를 판정하지 않는다.

## 사전 조건

- 현재 Profile에서 `semble-mcp`의 `search`와 `find_related`가 허용되어 있으면 MCP를 우선 사용한다.
- MCP를 사용할 수 없고 사용자가 CLI fallback을 승인한 경우에만 `terminal`로 local `semble`을 실행한다.
- CLI 설치가 필요하면 먼저 승인을 받고 `terminal`에서 `uvx --from "semble[mcp]==0.5.3" semble ...`을 실행한다.

## 빠른 참조

| 목적 | 방법 |
|---|---|
| 코드·동작 후보 찾기 | Semble MCP의 `search` |
| 문서 검색 | `search`에 `content=docs` 사용 |
| 설정 검색 | `search`에 `content=config` 사용 |
| 전체 범위 검색 | `search`에 `content=all` 사용 |
| 유사 구현 찾기 | Semble MCP의 `find_related` |
| CLI fallback | `terminal(command="semble search ...")` |

## 절차

1. 검색 필요성을 판정한다. requirement와 현재 상태만으로 충분하면 검색을 생략하고 그 근거를
   기록한다. 검색이 필요하면 질문을 동작·심볼·문서 주제로 좁힌다. **완료 기준:** 검색 여부와
   이유가 명확하다.
2. 현재 Profile의 Semble MCP `search`를 좁은 query와 적절한 content 범위로 호출한다. **완료
   기준:** 후보의 file path와 line 정보가 반환된다.
3. 반환된 path와 line으로 실제 repository 파일을 읽는다. 검색 결과를 다시 검색하지 말고
   `read_file` 또는 필요한 repository 탐색으로 해당 source·test·설정을 확인한다. **완료 기준:**
   최소 하나의 후보를 실제 checkout에서 확인했다.
4. 유사 구현이 필요할 때만 확인된 결과의 path와 line으로 `find_related`를 호출한다. **완료
   기준:** 유사 결과도 실제 파일을 읽고 관련성을 확인했다.
5. 결론에 locator와 source 검증 결과를 구분해 기록한다. Graph·검색 index와 checkout이 다르면
   checkout을 현재 사실로 취급하고 index drift를 보고한다. **완료 기준:** 검증된 파일과 검색의
   한계가 함께 남아 있다.

## 주의사항

- 검색 결과의 설명이나 snippet만 복사해 구현 사실로 사용하지 않는다.
- 같은 query를 반복 호출하지 않는다. 반환된 위치로 바로 이동한다.
- MCP가 없다는 이유로 Skill이나 외부 패키지를 자동 설치하지 않는다.
- 승인 requirement와 fresh current-state가 충분한 planning에서 불필요한 source 탐색을 시작하지 않는다.
- CLI fallback은 MCP보다 우선하지 않으며, 사용자 승인 없이 설치·실행하지 않는다.

## 검증

다음 조건을 모두 만족해야 검색 작업을 완료한다.

- 반환된 모든 핵심 후보를 실제 repository source·test·설정에서 확인했다.
- 검색 결과와 checkout 사이의 불일치를 기록했다.
- 검색 결과만으로 구현 완료나 요구사항 충족을 주장하지 않았다.
