# Review code discovery guide

Leaf Review Skill은 이 가이드를 **위치나 관계가 불명확할 때만** 읽는다. 목표는 search volume이 아니라
판정에 필요한 source evidence다. 이미 packet에 정확한 path/symbol이 있으면 source-direct branch부터
시작한다.

## 선택 표

| 질문 | 첫 branch | 이유 |
|---|---|---|
| Exact path와 symbol이 이미 있다 | Source direct | 가장 짧고 current checkout을 직접 확인한다. |
| “이 behavior/policy를 어디서 구현하는가?” | Semble | 의미·의도 기반 후보 탐색에 적합하다. |
| Known symbol의 caller/callee/dependency/consumer는 무엇인가? | Codebase Memory | graph 관계와 impact closure에 적합하다. |
| Exact string, annotation, config key, test ID, migration/table/field는 어디 있는가? | Local literal | 정확한 occurrence가 필요하다. |
| Semble 후보의 caller·dependency 경로가 새로 궁금해졌다 | Codebase Memory 후속 | 첫 검색과 다른 관계 질문이다. |
| Graph가 가리킨 구현의 현재 내용은 무엇인가? | Source direct 후속 | graph claim을 checkout에서 확정한다. |

같은 질문을 Semble과 Codebase Memory 양쪽에 보내 비교하지 않는다. 첫 결과가 좁거나 stale하면 아래
fallback 순서로 전환하고 axis result에 이유를 남긴다.

## Source-direct branch

1. `read_file`로 packet의 exact path와 line 주변을 읽는다.
2. Exact symbol/string의 모든 occurrence가 필요할 때만 `search_files`를 사용한다.
3. Caller·dependency 관계가 source 몇 곳으로 명확하면 직접 추적하고 graph를 호출하지 않는다.

완료 기준: 판정에 필요한 symbol, branch, caller/test가 current checkout에서 확인됐거나 다음 discovery
질문이 한 문장으로 특정됐다.

## Semble branch — 의미와 위치

다음 상황에서 `semble-search` Skill을 로드한 뒤 MCP `search`를 사용한다.

- requirement/domain behavior의 구현 위치를 모른다.
- 같은 policy나 responsibility를 수행하는 sibling/utility를 찾는다.
- 관련 architecture 문서나 configuration의 이름을 모른다.

Procedure:

1. Behavior와 domain term을 포함한 한 개의 focused query로 시작한다.
2. Code는 기본 content, 문서는 docs, configuration은 config scope로 좁힌다.
3. 상위 후보의 path와 line을 `read_file`로 열어 실제 responsibility와 contract를 확인한다.
4. Known location과 유사한 implementation이 필요한 별도 질문에서만 `find_related`를 사용한다.
5. 결과가 비어 있으면 동의어 하나 또는 더 넓은 scope로 한 번 재시도하고, 이후 local search로 fallback한다.

완료 기준: 후보가 exact source/test/doc locator로 확정됐거나 “검색 결과 없음 + 사용한 query/scope”가
미검증 evidence로 기록됐다.

## Codebase Memory branch — 관계와 영향

다음 상황에서 `codebase-memory-mcp` Skill을 로드하고 index freshness를 확인한 뒤 사용한다.

- Known symbol의 caller/callee 또는 dependency path가 필요하다.
- Module/event/interface의 producer→consumer 경로와 impact closure가 필요하다.
- 여러 hop의 data flow, cross-module path 또는 blast radius가 필요하다.

Procedure:

1. `index_status`가 허용되면 current repository와 index freshness를 확인한다. Branch/HEAD 일치 여부를
   확정할 수 없으면 graph를 discovery hint로만 취급한다.
2. Definition·implementation·related symbol 후보는 `search_graph`로 좁힌다. Known symbol 이름과 path를
   함께 주고 결과 limit를 둔다.
3. Caller/callee, dependency, data-flow와 cross-module 관계는 `trace_path`로 확인한다. Test coverage가
   질문일 때만 test path를 포함한다.
4. Returned symbol/path를 `read_file`로 열어 imports, call, annotation, event 또는 mapping을 현재 checkout에서
   확인한다.
5. Graph와 source가 다르면 source를 현재 사실로 사용하고 index drift를 axis result에 기록한다.

완료 기준: 필요한 관계가 graph와 checked-out source 양쪽에서 확인됐거나 freshness/coverage 한계가
`unverified_or_blocker`에 기록됐다.

## Fallback과 기록

- Semble/MCP가 unavailable해도 exact source와 local search로 안전하게 완료할 수 있으면 Review를 계속한다.
- 필요한 multi-hop 관계를 local evidence로 확정할 수 없을 때만 해당 axis를 `blocked`로 판정한다.
- `evidence_examined`에는 최종 source locator를 기록한다. Discovery query나 graph result 자체를 defect의
  유일한 evidence로 사용하지 않는다.
- Raw tool output, prompt와 index internals는 Kanban result metadata에 저장하지 않는다.