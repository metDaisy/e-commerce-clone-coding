# Coder Impl card consumption contract v3

이 문서는 `coder`가 admitted `backend-implementation-card-v2` 또는 `backend-implementation-card-v3`을 해석하고
current `backend-implementation-handoff-v2` (historical v1 read-back compatible)을 작성하는 계약을 소유한다. PM의 card authoring shape와
validation은 PM `build-task-graph/references/implementation-card-contract.md`가 소유한다. Native
Kanban이 task ID, title, assignee, status, links, workspace, run, comment와 event를 소유하며 이
schema는 그 값을 body에 복제하지 않는다.

## Coder input example

Coder는 아래와 같은 valid JSON object를 입력으로 받는다. 이 예시는 각 field가 구현과 검증에서
어떻게 결합되는지 보여준다. 모든 설명문은 한국어이며 ID, path, symbol, enum과 도구 이름은 literal을
보존한다.

```json
{
  "schema": "backend-implementation-card-v3",
  "card_type": "implementation",
  "issue": {
    "number": 138,
    "url": "https://github.com/example/repository/issues/138"
  },
  "goal": "판매자가 유효한 상품을 등록할 수 있다.",
  "effective_behavior": [
    {
      "id": "behavior-product-create",
      "outcome": "인증된 판매자의 유효한 요청은 상품을 저장하고 생성 결과를 반환한다."
    }
  ],
  "scope": ["상품 등록 application/API 동작과 직접 필요한 persistence를 구현한다."],
  "out_of_scope": ["상품 이미지 업로드와 frontend는 변경하지 않는다."],
  "implementation_context": {
    "current_behavior": ["상품 등록 application/API가 존재하지 않는다."],
    "entry_points": [
      {
        "path": "src/main/java/example/product",
        "symbol": "example.product",
        "reason": "상품 모듈의 확인된 구현 시작점이다."
      }
    ],
    "constraints": ["다른 모듈의 내부 package나 repository를 직접 참조하지 않는다."]
  },
  "contracts": {
    "actor_authorization": {
      "applicable": true,
      "rules": ["인증된 판매자만 상품을 등록한다."]
    },
    "input_output": {
      "applicable": true,
      "rules": ["이름, 가격과 재고를 입력받고 생성된 상품 표현을 반환한다."]
    },
    "state_invariants": {
      "applicable": true,
      "rules": ["성공한 요청은 판매자 소유 상품 하나를 저장한다."]
    },
    "error_semantics": {
      "applicable": true,
      "rules": ["유효하지 않은 입력은 기존 validation error contract로 거절한다."]
    },
    "api": {
      "applicable": true,
      "rules": ["승인된 상품 등록 endpoint와 HTTP contract를 구현한다."]
    },
    "persistence": {
      "applicable": true,
      "rules": ["상품 모듈 내부 persistence boundary를 사용한다."]
    },
    "transaction_consistency": {
      "applicable": true,
      "rules": ["상품 생성과 저장은 하나의 transaction 결과다."]
    },
    "event": {
      "applicable": false,
      "not_applicable_reason": "이 card는 event 발행이나 소비 계약을 변경하지 않는다."
    },
    "module_boundary": {
      "applicable": true,
      "rules": ["상품 모듈의 공개 seam과 allowedDependencies를 유지한다."]
    },
    "external_system": {
      "applicable": false,
      "not_applicable_reason": "외부 시스템 협업이 없다."
    },
    "delivery_boundary": {
      "applicable": true,
      "request_model": "application.dto.request의 생성 요청 DTO가 이름, 가격과 재고의 필수값을 Bean Validation으로 검증한다.",
      "binding_and_validation": "표현 계층은 형식이 지정된 요청 본문과 검증 어노테이션으로 검증한 전송 독립 값을 application에 전달한다.",
      "response_or_result_model": "application 결과와 표현 계층 응답을 분리하며 상태와 응답 변환은 표현 계층이 소유한다.",
      "transport_error_mapping": "형식과 validation 오류는 presentation exception flow에서 기존 validation error contract로 응답하고 업무 error는 상품 module이 소유한다.",
      "api_acceptance_ids": ["AC-PRODUCT-CREATE"],
      "required_web_scenarios": ["유효한 요청의 생성 응답", "필수값 또는 형식 오류의 거절 응답"]
    }
  },
  "acceptance_criteria": [
    {
      "id": "AC-PRODUCT-CREATE",
      "outcome": "유효한 판매자 입력으로 상품이 저장되고 생성 결과가 반환된다."
    }
  ],
  "focused_verification": [
    {
      "id": "FV-PRODUCT-CREATE",
      "acceptance_ids": ["AC-PRODUCT-CREATE"],
      "test_level": "integration",
      "runner": "gradle-mcp",
      "CHECK": {"task": "test", "tests": ["example.product.ProductCreateIntegrationTest#createsProduct"]},
      "CWD": ".",
      "EXPECT": "BUILD SUCCESSFUL",
      "required_scenarios": ["유효한 입력의 성공", "유효하지 않은 입력의 거절"]
    }
  ],
  "full_backend_verification": {
    "runner": "gradle-mcp",
    "CHECK": "test",
    "CWD": ".",
    "EXPECT": "BUILD SUCCESSFUL"
  },
  "traceability": [
    {
      "role": "goal",
      "supports": "상품 등록 목표 동작",
      "source": {
        "kind": "repository",
        "path": "docs/requirement/p2/product.md",
        "heading": "상품 등록"
      }
    }
  ]
}
```

## Admission invariants

- `schema`는 legacy `backend-implementation-card-v2` 또는 current `backend-implementation-card-v3`이고,
  `card_type`은 `implementation`이다. 새 card는 v3여야 하며 v2는 이미 생성된 historical card를 재개할 때만
  허용한다.
- Body와 모든 nested object는 closed schema이며 duplicate key와 명시되지 않은 field를 허용하지 않는다.
- `issue.number`, `issue.url`, `goal`, `effective_behavior`, `scope`, `out_of_scope`,
  `implementation_context`, `contracts`, `acceptance_criteria`, `focused_verification`,
  `full_backend_verification`, `traceability`가 존재한다.
- Body에는 baseline/planning SHA, assignee, status, dependency, workspace와 실행 후 결과가 없어야 한다.
  Coder는 해당 실행 상태를 native Kanban에서 read-back한다.
- `effective_behavior`는 requirement history나 delta가 아니라 이번 card가 완성할 현재 동작이다.
- `entry_points`는 확인된 조사 시작점이며 필수 caller/consumer 조사를 제한하는 allowlist가 아니다.
- 모든 `contracts` dimension은 존재한다. 적용되면 `applicable: true`와 비어 있지 않은
  `rules`, 적용되지 않으면 `applicable: false`와 구체적인 `not_applicable_reason`을 갖는다.
  누락을 비적용으로 해석하지 않는다.
- `acceptance_criteria.id`, `focused_verification.id`, `effective_behavior.id`는 card 안에서
  중복되지 않는다.
- 모든 acceptance ID는 하나 이상의 focused verification에 연결되고, 존재하지 않는 ID를
  참조하지 않는다.
- `test_level`은 `unit | slice | repository | integration | modulith` 중 하나인 최소 요구다. Card의
  behavior와 scenario, `CHECK.tests`의 actual FQCN/FQCN#method, `CWD: "."`, `EXPECT: "BUILD SUCCESSFUL"`은
  고정 입력이며 Coder는 이를 낮추거나 다른 selector로 바꾸지 않는다.
- `full_backend_verification`은 `runner: gradle-mcp`, `CHECK: test`, `CWD: "."`,
  `EXPECT: "BUILD SUCCESSFUL"`로 고정한다. Frontend,
  browser와 npm 검증은 이 schema 범위가 아니다.
- `traceability`는 provenance locator이지 source 문서를 다시 해석하거나 재기획하라는 지시가 아니다.
- Credential, raw tool output, prompt와 hidden reasoning은 body나 handoff에 기록하지 않는다.

## Applicability dimensions

| Field | Coder가 구현에서 해석할 의미 |
|---|---|
| `actor_authorization` | actor, 인증·인가와 거절 조건 |
| `input_output` | 입력 field 의미, 반환 값과 null/format 규칙 |
| `state_invariants` | 생성·변경·삭제 뒤 보장할 상태 |
| `error_semantics` | validation, not-found, conflict와 오류 표현 |
| `api` | endpoint, method, status와 request/response 계약 |
| `persistence` | 저장·조회·migration·constraint 의미 |
| `transaction_consistency` | transaction 경계, rollback과 consistency |
| `event` | 발행·소비 사실, payload, ordering/idempotency |
| `module_boundary` | named interface, allowed dependency와 ownership |
| `external_system` | 외부 port/adapter, timeout·failure 의미 |
| `delivery_boundary` (v3) | HTTP request DTO, framework binding, response/result, malformed input과 error mapping의 owner |

`not_applicable`은 누락된 제품 의미를 보충하라는 뜻이 아니다. 적용 여부, rules 또는 비적용 사유가
불완전하거나 구현에 새 정책 결정이 필요하면 Coder는 추측하지 않고 block한다.

## Delivery boundary (v3)

`contracts.api.applicable=true`인 v3 card는 `delivery_boundary.applicable=true`여야 한다. PM이 명시한
`request_model`, `binding_and_validation`, `response_or_result_model`, `transport_error_mapping`,
`api_acceptance_ids`, `required_web_scenarios`는 구현 checklist가 아니라 observable contract다. Coder는 다음을 확인한다.

- request DTO와 Bean Validation은 request boundary에 있고 Controller의 `@Valid`가 실제 constraint를 실행한다.
- Spring MVC가 지원하는 type conversion은 typed parameter로 위임한다. 수동 `String` parsing과 이를 감싸는
  application exception을 새로 만들지 않는다.
- application command/result는 transport-neutral이며 HTTP status, response body, `exceptionCode()` 같은
  presentation mapping helper를 소유하지 않는다.
- malformed binding은 presentation error mapping에서 카드가 정한 code로 응답하며, 업무 error는 owner module의
  공개 contract를 사용한다. consumer가 producer code/message를 복제하면 handoff 대신 block한다.
- 각 `api_acceptance_ids`는 해당 acceptance를 참조하는 `slice` 또는 `integration` test와
  `required_web_scenarios`의 관찰 결과를 가져야 한다.

legacy v2 card에 이 정보가 없더라도 Coder는 `docs/backend-development-guide.md`의 기존 규칙을 적용한다.
해당 규칙과 card의 동작을 함께 만족시킬 수 없으면 PM에 contract blocker를 올린다.

## Coder review handoff metadata

Coder는 두 검증이 통과한 뒤 `kanban_request_review`의 metadata에 다음 object를 기록한다.
Body는 수정하지 않는다.

```json
{
  "schema": "backend-implementation-handoff-v2",
  "handoff_id": "handoff-7f3d87b2",
  "source_run_id": 16,
  "implemented_behavior_ids": ["behavior-product-create"],
  "changed_paths": [
    "src/main/java/example/product/ProductService.java",
    "src/test/java/example/product/ProductRegistrationIntegrationTest.java"
  ],
  "acceptance_results": [
    {
      "acceptance_id": "AC-PRODUCT-CREATE",
      "result": "pass",
      "focused_verification_ids": ["FV-PRODUCT-CREATE"]
    }
  ],
  "focused_verification_results": [
    {
      "verification_id": "FV-PRODUCT-CREATE",
      "executor": "gradle-mcp",
      "tasks": ["test"],
      "tests": ["example.product.ProductRegistrationIntegrationTest"],
      "test_levels": ["integration"],
      "scenario_results": [
        {"scenario": "유효한 입력의 성공", "result": "pass"},
        {"scenario": "유효하지 않은 입력의 거절", "result": "pass"}
      ],
      "result": "pass"
    }
  ],
  "full_backend_verification_result": {
    "executor": "gradle-mcp",
    "task": "test",
    "result": "pass"
  },
  "documentation_impact": {
    "detected": false,
    "details": []
  },
  "implementation_convention_readback": {
    "backend_development_guide_sections": ["Module과 계층", "Presentation 구현 규칙"],
    "backend_test_guide_sections": ["Controller test"],
    "architecture_sections": [],
    "package_info_paths": [],
    "layer_ownership": [
      {"concern": "요청 형식 검증", "owner": "presentation"},
      {"concern": "상품 생성 상태 전이", "owner": "domain"}
    ],
    "checked_boundaries": ["application DTO에 HTTP annotation이 없다."]
  },
  "residual_risks": []
}
```

Handoff는 다음 coverage invariant를 모두 만족한다.

- `handoff_id`는 Coder가 review 요청 시도마다 새로 만드는 비어 있지 않은 opaque ID다. PM checkpoint와
  changes-request는 이 ID를 참조해 최신 handoff와 결합한다.
- `source_run_id`는 handoff를 만든 현재 native Coder run의 양의 정수 ID와 정확히 같다. Focused result의
  `test_levels`에는 card가 요구한 `test_level`이 반드시 포함되며 추가 실행 level은 함께 기록할 수 있다.
  Handoff의 structured pass 결과는 Coder self-verification이며 PM은 checkpoint run에서 동일한
  focused/full Gradle contract를 직접 재실행한다.
- `implemented_behavior_ids`는 card의 모든 `effective_behavior.id`를 중복 없이 정확히 한 번씩
  포함하고 존재하지 않는 ID를 포함하지 않는다.
- `acceptance_results`는 모든 `acceptance_criteria.id`를 중복 없이 정확히 한 번씩 포함하며 각
  result는 `pass`다. 각 `focused_verification_ids`는 card가 해당 acceptance에 연결한 ID와 같다.
- `focused_verification_results`는 모든 `focused_verification.id`를 중복 없이 정확히 한 번씩
  포함하고 존재하지 않는 ID를 포함하지 않는다.
- 각 focused result의 `scenario_results`는 card의 모든 `required_scenarios`를 중복 없이 정확히
  한 번씩 포함하며 모든 result가 `pass`다.
- Full backend verification result는 card와 같은 executor/task를 가리키며 `pass`다.
- Current v2 handoff는 `implementation_convention_readback`을 가져야 한다. `backend_development_guide_sections`,
  `backend_test_guide_sections`, `checked_boundaries`는 중복 없는 비어 있지 않은 목록이고, `architecture_sections`와
  `package_info_paths`는 해당하지 않으면 빈 목록이다. `package_info_paths`는 repository-relative
  `package-info.java` path만 허용한다. `layer_ownership`의 각 `concern`은 고유하고 owner는
  `presentation | application | domain | infra` 중 하나다. 이는 guide 적용의 짧은 사실 record이며 reasoning이나
  PM이 확인하지 않은 성공 주장으로 사용하지 않는다.

`changed_paths`에는 실제 task 변경의 repository-relative literal path만 기록한다. Forward slash를
사용하고 absolute path, `.`/`..` segment, glob·pathspec·option 표현과 placeholder(`...`, `*`, `**`)를
허용하지 않으며 문서 path를 포함하지 않는다. Historical v1 handoff에는 convention readback이 없을 수 있으며 read-back만 허용한다. 실행하지 못한 검증,
실패, dirty conflict 또는 미정 policy는 handoff가 아니라 `kanban_block` 대상이다.

`external_system.applicable=true`인 card는 port interface·mock test만으로 acceptance를 pass 처리할 수 없다.
Coder는 consumer port → adapter → producer public surface의 실제 경로와 unavailable/error semantics를 확인해야
한다. Adapter가 항상 empty/default/unsupported 결과를 반환하거나 producer public contract가 absent/partial이면
해당 behavior는 handoff 대상이 아니라 `PM review requested` + `kanban_block(kind="needs_input")` 대상이다.

## Changes-request input

Changes-requested rework에서 Coder는 latest native `request-changes` transition 직전 durable comment의
다음 valid JSON object만 structured rework input으로 사용한다. Native reason이나 prose summary는 이
payload를 대체하지 않는다.

```json
{
  "schema": "backend-implementation-change-request-v1",
  "source_handoff_id": "handoff-7f3d87b2",
  "source_review_run_id": 17,
  "findings": [
    {
      "finding_id": "PM-CHK-1",
      "path": "src/main/java/example/product/ProductService.java",
      "symbol": "ProductService.register",
      "observed_problem": "유효하지 않은 가격을 저장한다.",
      "expected_result": "유효하지 않은 가격을 기존 validation error contract로 거절한다.",
      "allowed_scope": ["상품 등록 validation과 직접 관련된 테스트"],
      "verification": ["FV-PRODUCT-CREATE", "full-backend"]
    }
  ]
}
```

Coder는 `source_handoff_id`를 직전 handoff ID와, `source_review_run_id`를 changes를 요청한 PM review
run의 양의 정수 native ID와 대조한다. `finding_id`는 request 안에서 고유해야 한다. 각 finding은 exact path·symbol, 관찰된
문제, 기대 결과, 허용 범위와 다시 실행할 검증을 모두 가져야 한다. 누락, ID 불일치, 일반적인 개선
요청 또는 card의 제품 의미 변경이 있으면 추측하지 않고 block한다. Valid latest request의 finding만
수정하고 card의 focused 및 full backend verification을 모두 다시 실행한다.

## Restart recovery input

모든 Impl task에는 PM-owned `backend-implementation-admission-v1` comment가 있어야 한다. 이 closed
object는 `schema`, actual `task_id`, positive Issue number인 `issue`, create-intent `workspace`,
`card_schema: backend-implementation-card-v3`, `restart`만 가진다. Coder는 native task ID와 body Issue를
comment에 대조하고 comment workspace를 dispatcher가 시작한 actual process cwd/worktree identity와
비교한다. Native `kanban_show` task가 workspace를 직접 반환한다고 가정하지 않는다.

Kanban 기록이 유실된 dirty workflow를 복구할 때에도 task body는 valid
`backend-implementation-card-v3`이어야 한다. Coder는 latest matching admission comment의 valid
`restart-task-v1`을 추가 admission input으로 읽는다. 이 object는 Issue, delivery branch, workspace,
marker/current SHA, exact `dirty_paths`, path별 attribution, 현재 `recovery_task_id`, `assignee: coder`, 단일
active recovery task, allowed scope, `implementation_card_schema: backend-implementation-card-v3`과
`required_checkpoint_schema: backend-implementation-checkpoint-v1`을 가진다.

Coder는 native task ID·workspace·Issue와 실제 dirty path가 이 metadata와 정확히 같을 때만 기존 delta를
인수한다. Body의 behavior·acceptance·verification은 계속 구현 의미를 소유하고 `restart-task-v1`은 dirty
state의 provenance와 허용 범위만 소유한다. 불일치·누락·추가 dirty path가 있으면 reset·stash·clean하지
않고 block한다. Recovery도 정상 handoff와 PM checkpoint schema를 사용한다.
