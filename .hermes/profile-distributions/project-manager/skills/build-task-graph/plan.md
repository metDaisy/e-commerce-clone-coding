# build-task-graph 구현 계획

## 목적

`build-task-graph`는 승인된 Issue/requirement와 검증된 현재 구현 상태를 수동 native
Kanban graph로 바꾼다. PM은 graph authoring·promotion을 소유하고, native Kanban은 상태,
ID, dependency, assignee, run, comment, evidence를 소유한다. Coder는 과거 generation이나
delta history가 아니라 자기 card의 완전한 현재 behavior contract만 구현한다.

## 확정된 설계

- mode는 `new`, `requirement-rework`, `review-rework`다.
- Kanban decomposer는 사용하지 않는다.
- card는 PM이 하나씩 `todo`로 생성하고 native read-back한다. 모든 생성·link·body 검증 후
  첫 eligible Impl 하나만 `ready`로 만든다. atomic graph-create는 요구하지 않는다.
- semantic Issue root는 `G{N}-Issue{M}-Summary` finalization child다. Triage는 별도 planning card다.

```text
Triage; initial Impl → Review1/Summary; Review → corrective Impl or Decision;
corrective Impl → Review{Q+1}/Summary; every Review/Decision → Summary
```

- Summary는 immutable finalization contract를 보존한다. membership은 native direct parent read-back으로
  계산하며 Summary의 `done`은 aggregate Review와 PM finalization checkpoint 뒤에만 가능하다.
- title은 `G{N}-Issue{M}-Triage`, `G{N}-Issue{M}-Impl{P}`, `G{N}-Issue{M}-Review{Q}`,
  `G{N}-Issue{M}-Decision{R}`, `G{N}-Issue{M}-Summary`를 쓴다.
  `rework`라는 문자열을 title에 넣지 않는다.
- `requirement-rework`은 aggregate Review 승인 전 active generation에만 허용한다. 승인 후
  requirement 변경은 새 Issue와 `new` graph를 만든다.
- `done` card의 body, ID, run, comment, checkpoint evidence는 immutable history다. 미완료
  obsolete Impl/Review/root만 archive한다. affected running task는 `requirement superseded`라는
  사실로 block하고 worker termination을 read-back한 후 archive한다.
- requirement rework는 active graph의 `requirement_basis_sha`와 새 approved revision 사이의
  deterministic Git diff를 출발점으로 한다. diff hunk는 evidence일 뿐 task boundary가 아니다.
- rework root는 delta가 아니라 T2 effective behavior model을 기록한다. 예: `A′ = A + AA`.
  기존 completed behavior는 새 effective behavior를 실제 source/test evidence로 재확인한
  경우에만 inherited evidence로 쓴다.
- PM은 behavior마다 `implemented`, `partial`, `absent`, `unknown`을 결정한다. `partial`/
  `absent`에만 새 Impl을 만든다. Impl의 goal은 언제나 A′ 전체이지 AA 단독이 아니다.
- Codebase Memory `index_status → search_graph → trace_path`와 Semble은 candidate locator다.
  PM은 committed source/test/migration을 직접 read-back하여 state evidence를 기록한다.
- source evidence가 부족한 것은 정상 investigation이다. policy, authorization, consistency,
  error semantics, ownership, external prerequisite를 조사만으로 결정할 수 없을 때만 native
  `blocked` 및 사용자 결정 route를 사용한다.
- `current-state.md`는 requirement rework 중간에 갱신하지 않는다.

## Contract and tool rollout

`references/board-contract.md` v5가 persisted body/draft schema와 validator invariant의 유일한
source of truth다. `SKILL.md`는 ordered procedure만 보유한다.

1. `scripts/task_graph.py`를 구현한다.
   - `new template|validate`
   - `requirement-rework diff|template|validate`
   - `review-rework template|validate`는 완료된 Review metadata의 per-finding verdict를 검증한다.
2. `requirement-rework diff`는 read-only Git helper다. `.temp/requirement-rework/.../comparison.json`
   에 stable hunk ID와 provenance만 만든다. behavior delta를 자동으로 확정하지 않는다.
3. v5 validator는 title/generation, behavior disposition, inherited evidence, native topology,
   single ready-promotion candidate, archive restriction을 deterministic하게 검증한다.
4. v5 valid/invalid new·requirement-rework fixture와 helper CLI test를 추가한다.
5. PM distribution documentation (`README`, `WORKFLOW-DESIGN`, `TERMINOLOGY`)을 v5 contract와
   align한다. `SOUL.md`에는 procedure/schema를 중복하지 않는다.

## 아직 결정하지 않은 사항

아래는 **현재 Skill/board contract의 확정 규칙이 아니다.** 구현 전 이 문서에서 사용자와
결정하고, 결정된 항목만 board contract와 helper/fixture에 옮긴다.

### 1. `review-rework`의 실행 계약 — 확정

- Reviewer는 done Review completion metadata의 `findings[]`에 canonical finding을 기록한다.
  PM은 creator-session terminal wake-up 또는 recovery에서 task/run/metadata를 read-back한다.
- finding verdict는 `correction-required`, `context-required`, `decision-required`, `resolved`다.
  PM은 observation을 routing input으로 사용하지 않는다.
- corrective Impl은 requirement delta가 아니라 existing effective behavior A의 self-contained correction이다.
- Decision은 같은 G의 native `blocked` card다. 사용자 결정이 완료 contract를 무효화할 때만
  `requirement-rework` G{N+1}로 전환한다.
- old task body/link/history를 바꾸지 않고 append-only topology와 idempotency key/read-back으로 recovery한다.

### 2. Requirement-rework persisted schema의 상세

- Summary/Impl/Review/Decision이 실제로 보존할 최소 field와 JSON version naming.
- inherited completed behavior의 evidence shape와 source/test 재확인 규칙의 machine-checkable
  최소 요건.
- behavior catalog key의 namespace·normalization·card별 reference 형태. key는 comparison
  candidate이며 impact verdict가 아니라는 원칙만 확정됐다.

### 3. Native Kanban mutation/read-back의 정확한 surface

- task create, parent link, `todo → ready`, running worker stop/block/archive가 현재 Hermes
  native API에서 각각 어떤 action/response/state semantics를 갖는지.
- running task requirement supersession 시 worker termination 확인 시점과 archive 허용 조건.
- Summary의 `todo → ready → running → done` 전이와 Review done-event PM resume을 UI/dispatcher와
  함께 실제 E2E로 검증할 방법.

### 4. v5 helper와 validator의 범위

- `task_graph.py`가 authoring scaffold만 만들지, board-contract 전체 JSON을 strict validate할지.
- `comparison.json`의 stable hunk ID algorithm, required provenance, rename/copy handling,
  artifact retention path.
- `implemented` behavior만 있는 graph를 허용할지, aggregate Review의 mandatory check가
  무엇인지, 그리고 "exactly one ready"를 draft-time 또는 native read-back-time 어디서
  검증할지.
- v4 helper/fixtures를 v5로 migrate/rename하는 순서와 backward compatibility 정책.

### 5. Codebase Memory operating evidence

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
