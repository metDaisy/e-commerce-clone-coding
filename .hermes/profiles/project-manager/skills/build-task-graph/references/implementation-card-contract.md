# PM backend Impl card authoring contract v1

이 문서는 Project Manager가 `backend-implementation-card-v1` body를 작성하는 의미와 책임을 소유한다.
결정론적 field shape와 validation은 인접한 `scripts/build_task_graph.py`, 생성 예시는
`tests/fixtures/valid-new-delivery.json`이 함께 소유한다. Coder의 admission·소비 규칙과 handoff schema는
Coder `run-impl-card/references/implementation-card-contract.md`가 소유한다.

## Authoring inputs

PM은 승인된 Issue/requirement, 현재 committed source·test·configuration·migration, architecture와 Triage
근거를 읽고 card를 작성한다. Requirement history나 변경 hunk를 전달하지 않고 이번 card가 완성할
현재 effective behavior를 materialize한다. 결정되지 않은 business policy, authorization, consistency,
API, event 또는 error semantics를 추측해서 채우지 않는다.

## Required body

Body는 valid JSON object이며 다음 top-level field를 모두 가진다.

| Field | PM이 작성할 의미 |
|---|---|
| `schema` | 정확히 `backend-implementation-card-v1` |
| `card_type` | 정확히 `implementation` |
| `issue` | 승인된 leaf Issue의 `number`, `url` |
| `goal` | 이 card 하나가 달성할 사용자 관찰 가능 결과 |
| `effective_behavior` | history나 delta가 아닌 완성할 현재 동작과 고유 ID |
| `scope` | 구현에 포함되는 제품·기술 경계 |
| `out_of_scope` | 명시적으로 제외되는 동작과 artifact |
| `implementation_context` | 확인된 현재 동작, 조사 시작점, 구조 제약 |
| `contracts` | 모든 applicability dimension의 결정된 규칙 또는 구체적인 비적용 사유 |
| `acceptance_criteria` | 결과 중심 완료 조건과 고유 ID |
| `focused_verification` | acceptance 연결, test level, 필수 scenario |
| `full_backend_verification` | `gradle-mcp` backend `test` |
| `traceability` | PM이 확인한 requirement/repository 근거 locator |

`implementation_context.entry_points`는 PM이 확인한 조사 시작점이며 Coder의 caller/consumer 조사나 수정
범위를 제한하는 allowlist가 아니다.

## Contract dimensions

모든 dimension이 존재해야 한다. 적용되면 `applicable: true`와 비어 있지 않은 `rules`, 적용되지 않으면
`applicable: false`와 구체적인 `not_applicable_reason`을 작성한다.

| Dimension | PM이 확정할 의미 |
|---|---|
| `actor_authorization` | actor, 인증·인가와 거절 조건 |
| `input_output` | 입력 field 의미, 반환 값, null/format 규칙 |
| `state_invariants` | 생성·변경·삭제 뒤 보장할 상태 |
| `error_semantics` | validation, not-found, conflict와 오류 표현 |
| `api` | endpoint, method, status, request/response 계약 |
| `persistence` | 저장·조회·migration·constraint 의미 |
| `transaction_consistency` | transaction 경계, rollback, consistency |
| `event` | 발행·소비 사실, payload, ordering/idempotency |
| `module_boundary` | named interface, allowed dependency, ownership |
| `external_system` | 외부 port/adapter, timeout·failure 의미 |

새 제품 의미가 필요한 dimension을 `not_applicable`로 숨기지 않는다. PM이 승인 근거로 결정할 수 없는
정책은 card 생성 전에 사용자 결정으로 route한다.

## Coverage invariants

- 모든 `effective_behavior.id`, `acceptance_criteria.id`, `focused_verification.id`는 종류별로 고유하다.
- 모든 acceptance ID는 하나 이상의 focused verification에 연결되며 존재하지 않는 ID를 참조하지 않는다.
- `test_level`은 `unit | slice | repository | integration | modulith` 중 하나다.
- PM은 behavior, test level과 required scenario를 정한다. 실제 test FQCN은 Coder가 source 조사 후
  선택하거나 작성하므로 PM이 존재하지 않는 test command를 발명하지 않는다.
- `full_backend_verification`은 executor `gradle-mcp`, task `test`로 고정한다.
- Body에 baseline/planning SHA, assignee, status, dependency, workspace, run 또는 실행 결과를 복제하지
  않는다. 이 값은 PM admission과 native Kanban이 소유한다.
- Credential, raw tool output, prompt와 hidden reasoning을 기록하지 않는다.

## Authoring procedure

1. `scripts/build_task_graph.py template`로 `.temp` 아래 fact-only scaffold를 생성한다.
2. 승인 근거와 직접 확인한 repository 사실만으로 모든 field를 작성한다.
3. `scripts/build_task_graph.py validate`를 실행해 shape와 coverage invariant를 확인한다.
4. Native task를 생성한 뒤 body, assignee, status와 links를 read-back한다.

Validator 통과는 schema 증거일 뿐 semantic completeness를 보장하지 않는다. PM은 card가 다른 문서의
재해석 없이 실행 가능한 self-contained contract인지 별도로 검수한다.
