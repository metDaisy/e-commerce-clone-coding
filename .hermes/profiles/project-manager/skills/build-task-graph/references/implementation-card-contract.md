# PM backend Impl card authoring contract v3

이 문서는 Project Manager가 `backend-implementation-card-v3` body를 작성하는 의미와 책임을 소유한다.
결정론적 field shape와 validation은 인접한 `scripts/build_task_graph.py`, 생성 예시는
`tests/fixtures/valid-new-delivery.json`이 함께 소유한다. Coder의 admission·소비 규칙과 handoff schema는
Coder `run-impl-card/references/implementation-card-contract.md`가 소유한다.

## Authoring inputs

PM은 승인된 Issue/requirement, fresh `current-state.md`, architecture와 Triage 근거를 기본 입력으로
card를 작성한다. Snapshot이 부족·충돌하거나 behavior의 정확한 시작점·구조 제약을 확정해야 할 때만
관련 committed source·test·configuration·migration을 좁게 확인한다. Requirement history나 변경 hunk를
전달하지 않고 이번 card가 완성할 현재 effective behavior를 materialize한다. 결정되지 않은 business
policy, authorization, consistency, API, event 또는 error semantics를 추측해서 채우지 않는다.

## Required body

Body는 duplicate key를 허용하지 않는 valid JSON object이며 다음 top-level field만 정확히 가진다.
Top-level과 모든 nested object는 closed schema이므로 명시되지 않은 field를 추가하지 않는다.

| Field | PM이 작성할 의미 |
|---|---|
| `schema` | 정확히 `backend-implementation-card-v3` |
| `card_type` | 정확히 `implementation` |
| `issue` | 승인된 leaf Issue의 `number`, `url` |
| `goal` | 이 card 하나가 달성할 사용자 관찰 가능 결과 |
| `effective_behavior` | history나 delta가 아닌 완성할 현재 동작과 고유 ID |
| `scope` | 구현에 포함되는 제품·기술 경계 |
| `out_of_scope` | 명시적으로 제외되는 동작과 artifact |
| `implementation_context` | 확인된 현재 동작, 조사 시작점, 구조 제약 |
| `contracts` | 모든 applicability dimension의 결정된 규칙 또는 구체적인 비적용 사유 |
| `acceptance_criteria` | 결과 중심 완료 조건과 고유 ID |
| `focused_verification` | acceptance 연결, test level, 필수 scenario와 exact `runner`/`CHECK`/`CWD`/`EXPECT` |
| `full_backend_verification` | exact `runner: gradle-mcp`, `CHECK: test`, `CWD: .`, `EXPECT: BUILD SUCCESSFUL` |
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
| `delivery_boundary` (v3) | API가 있을 때 request DTO, framework binding/validation, application result, transport error mapping과 web scenario의 owner |

새 제품 의미가 필요한 dimension을 `not_applicable`로 숨기지 않는다. PM이 승인 근거로 결정할 수 없는
정책은 card 생성 전에 사용자 결정으로 route한다.

## Delivery boundary contract (v3)

새 card는 `backend-implementation-card-v3`을 사용한다. `api.applicable=true`면
`delivery_boundary.applicable=true`가 필수이며 아래 closed object를 작성한다.

```json
{
  "applicable": true,
  "request_model": "request DTO package/type와 required field·Bean Validation owner",
  "binding_and_validation": "Spring MVC typed binding, @Valid 진입점과 application에 넘기는 transport-neutral value",
  "response_or_result_model": "presentation response와 application/public use-case result의 분리 및 owner",
  "transport_error_mapping": "malformed binding/validation의 presentation mapping과 업무 error owner",
  "api_acceptance_ids": ["API acceptance criterion ID"],
  "required_web_scenarios": ["유효한 요청", "형식 또는 validation 오류"]
}
```

- `binding_and_validation`은 “입력을 검증한다”처럼 일반적으로 쓰지 않는다. path/query/cookie/body의 type
  conversion owner와 malformed input의 response contract를 쓴다.
- `response_or_result_model`은 request DTO, presentation response와 cross-module/public use-case result를
  혼합하지 않음을 명시한다. result에 `exceptionCode()`나 HTTP helper를 넣는 설계를 기본값으로 사용하지 않는다.
- `transport_error_mapping`은 generic global mapping이 endpoint requirement의 code를 약화하지 않는지까지
  결정한다. 업무 error code/message의 owner module과 consumer 변환 여부도 적는다.
- API가 아니면 `delivery_boundary`에는 구체적인 `not_applicable_reason`을 쓴다. 기존 v2 card는 historical
  read-back만 허용하며 새 graph에 재사용하지 않는다.

## Coverage invariants

- 모든 `effective_behavior.id`, `acceptance_criteria.id`, `focused_verification.id`는 종류별로 고유하다.
- v3 API card는 `delivery_boundary`의 다섯 field, `api_acceptance_ids`와 `required_web_scenarios`를 모두
  가진다. 각 `api_acceptance_ids`는 실제 acceptance ID여야 하며, 그 acceptance를 참조하는 focused
  verification 중 하나 이상이 `slice` 또는 `integration` test level이어야 한다.
- `issue` number와 canonical HTTPS URL은 graph wrapper의 leaf Issue와 정확히 같다.
- Impl의 `effective_behavior` ID 집합은 graph에서 그 Impl에 배정한 planned behavior와 정확히 같고,
  graph 밖의 behavior를 추가하지 않는다.
- 모든 acceptance ID는 하나 이상의 focused verification에 연결되며 존재하지 않는 ID를 참조하지 않는다.
- `test_level`은 `unit | slice | repository | integration | modulith` 중 하나다.
- PM은 behavior, 최소 test level, required scenario와 현재 존재하거나 card에서 새로 만들 fully-qualified
  test selector를 정한다. `focused_verification`마다 `runner: gradle-mcp`, non-empty `CHECK.task`, 하나 이상의
  actual FQCN 또는 `FQCN#method`, `CWD: "."`, `EXPECT: "BUILD SUCCESSFUL"`를 기록한다. 존재하지 않는
  selector를 추측할 수 있으면 card를 만들지 말고 investigation으로 route한다.
- `full_backend_verification`은 `runner: gradle-mcp`, `CHECK: test`, `CWD: "."`,
  `EXPECT: "BUILD SUCCESSFUL"`로 고정한다.
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
