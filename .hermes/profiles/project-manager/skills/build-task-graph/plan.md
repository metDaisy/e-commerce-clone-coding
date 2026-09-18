# build-task-graph 미구현·TBD

확정된 mode, topology, card 책임, ordered procedure는 [`SKILL.md`](SKILL.md)와
[`references/board-contract.md`](references/board-contract.md)가 소유한다. 이 문서는 아직
구현하지 않았거나 구현 전에 결정해야 하는 항목만 기록한다.

## 현재 구현 기준선

- `scripts/build_task_graph.py`와 fixture는 v4 `new-delivery` template/validate만 지원한다.
- `requirement-rework`, `review-rework`, v5 persisted schema와 native Kanban E2E는 미구현이다.

## 구현 backlog

1. 기존 `scripts/build_task_graph.py`를 v5 helper로 migration한다.
   - `new template|validate`
   - `requirement-rework diff|template|validate`
   - `review-rework template|validate`: 완료된 Review metadata의 per-finding verdict를 검증
2. `requirement-rework diff` read-only Git helper를 구현한다. `.temp/requirement-rework/.../comparison.json`에
   stable hunk ID와 provenance만 기록하며 behavior delta를 자동 확정하지 않는다.
3. v5 validator와 valid/invalid fixture·CLI tests를 추가한다. title/generation, behavior disposition,
   inherited evidence, native topology, single ready candidate, archive restriction을 검증한다.
4. native Kanban mutation/read-back과 PM creator-session/recovery를 실제 E2E로 검증한다.

## 결정이 필요한 TBD

아래는 **현재 Skill/board contract의 확정 규칙이 아니다.** 결정된 항목만 contract와
helper/fixture에 옮긴다.

### 1. Persisted schema 상세

- Summary/Review/Decision이 실제로 보존할 최소 field와 JSON version naming.
- Backend Impl body와 Coder handoff/PM checkpoint는 `backend-implementation-card-v1`,
  `backend-implementation-handoff-v1`, `backend-implementation-checkpoint-v1`로 확정됐다. v5
  helper와 fixture를 이 canonical contract로 migration해야 한다.
- inherited completed behavior의 evidence shape와 source/test 재확인 규칙의 machine-checkable
  최소 요건.
- behavior catalog key의 namespace·normalization·card별 reference 형태. key는 comparison
  candidate이며 impact verdict가 아니라는 원칙만 확정됐다.

### 2. Native Kanban mutation/read-back의 정확한 surface

- task create, parent link, `todo → ready`, running worker stop/block/archive가 현재 Hermes
  native API에서 각각 어떤 action/response/state semantics를 갖는지.
- running task requirement supersession 시 worker termination 확인 시점과 archive 허용 조건.
- Summary의 `todo → ready → running → done` 전이와 Review done-event PM resume을 UI/dispatcher와
  함께 실제 E2E로 검증할 방법.

### 3. v5 helper와 validator의 범위

- `build_task_graph.py`가 authoring scaffold만 만들지, board-contract 전체 JSON을 strict validate할지.
- `comparison.json`의 stable hunk ID algorithm, required provenance, rename/copy handling,
  artifact retention path.
- `implemented` behavior만 있는 graph를 허용할지, aggregate Review의 mandatory check가
  무엇인지, 그리고 "exactly one ready"를 draft-time 또는 native read-back-time 어디서
  검증할지.
- v4 helper/fixtures를 v5로 migrate/rename하는 순서와 backward compatibility 정책.

### 4. Codebase Memory operating evidence

- PM fresh session에서 `codebase-memory-mcp` domain tools가 실제로 노출되는지의 E2E 기준.
- index freshness/rebuild authority, 실패 시 Semble + committed-tree fallback의 bounded
  procedure와 card에 남길 sanitized evidence 형태.

## 검증 기준

- Python helper unit/CLI fixture tests가 통과한다.
- valid/invalid rework fixture가 title, generation, root/review topology, inherited evidence,
  requirement-basis/revision, one eligible Impl rule을 각각 판정한다.
- native Kanban mutation은 draft validation보다 별도다. native card create/link/promotion을
  수행한 뒤에는 task body, ID, assignee, `todo` status, link, single `ready` task를 read-back한다.
- Codebase Memory 설치 여부는 `hermes mcp list`만으로 확정하지 않는다. PM fresh session에서
  `mcp__codebase_memory_mcp__...` domain tool registration 또는 실제 call을 확인한다.
