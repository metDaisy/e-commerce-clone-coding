# Backend Impl card contract v1

이 문서는 PM이 작성하고 `implementation-coder`가 소비하는 Impl card body와 실행 handoff의
canonical schema를 소유한다. Graph identity, topology, generation과 promotion은 PM의 board
contract가 소유하며, native Kanban이 task ID, title, assignee, status, links, workspace, run,
comment와 event를 소유한다. 이 schema는 그 값을 body에 복제하지 않는다.

## Body format

Body는 아래 shape의 valid JSON object다. 모든 설명문은 한국어로 쓰고 ID, path, symbol, enum과
도구 이름은 literal을 보존한다.

```json
{
  "schema": "backend-implementation-card-v1",
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
      "required_scenarios": ["유효한 입력의 성공", "유효하지 않은 입력의 거절"]
    }
  ],
  "full_backend_verification": {
    "executor": "gradle-mcp",
    "task": "test",
    "expectation": "backend 전체 Gradle test suite가 통과한다."
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

## Required invariants

- `schema`는 정확히 `backend-implementation-card-v1`, `card_type`은 `implementation`이다.
- `issue.number`, `issue.url`, `goal`, `effective_behavior`, `scope`, `out_of_scope`,
  `implementation_context`, `contracts`, `acceptance_criteria`, `focused_verification`,
  `full_backend_verification`, `traceability`가 존재한다.
- Body에는 baseline/planning SHA, assignee, status, dependency, workspace와 실행 후 결과를 넣지
  않는다. PM admission과 native Kanban이 그 상태를 소유한다.
- `effective_behavior`는 requirement history나 delta가 아니라 이번 card가 완성할 현재 동작이다.
- `entry_points`는 PM이 직접 확인한 시작점이며 Coder의 필수 caller/consumer 조사를 제한하는
  allowlist가 아니다.
- 모든 `contracts` dimension은 존재한다. 적용되면 `applicable: true`와 비어 있지 않은
  `rules`, 적용되지 않으면 `applicable: false`와 구체적인 `not_applicable_reason`을 갖는다.
  누락을 비적용으로 해석하지 않는다.
- `acceptance_criteria.id`, `focused_verification.id`, `effective_behavior.id`는 card 안에서
  중복되지 않는다.
- 모든 acceptance ID는 하나 이상의 focused verification에 연결되고, 존재하지 않는 ID를
  참조하지 않는다.
- `test_level`은 `unit | slice | repository | integration | modulith` 중 하나다. PM은 검증할
  behavior와 scenario를 정하고, Coder는 실제 source를 조사한 뒤 정확한 test FQCN을 선택하거나
  작성한다. 아직 존재하지 않는 test command를 PM이 발명하지 않는다.
- `full_backend_verification`은 `gradle-mcp`의 backend 전체 `test` task로 고정한다. Frontend,
  browser와 npm 검증은 이 schema 범위가 아니다.
- `traceability`는 PM의 근거다. Coder에게 source 문서를 다시 해석하거나 재기획하도록 지시하지
  않는다.
- Credential, raw tool output, prompt와 hidden reasoning은 body나 handoff에 기록하지 않는다.

## Applicability dimensions

| Field | PM이 확정해 materialize할 의미 |
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

새 제품 의미가 필요한 dimension을 `not_applicable`로 숨기지 않는다. PM이 결정할 수 없는 정책은
card 생성 전에 사용자 결정으로 route한다.

## Coder review handoff metadata

Coder는 두 검증이 통과한 뒤 `kanban_request_review`의 metadata에 다음 object를 기록한다.
Body는 수정하지 않는다.

```json
{
  "schema": "backend-implementation-handoff-v1",
  "implemented_behavior_ids": ["behavior-product-create"],
  "changed_paths": ["src/main/java/example/product/...", "src/test/java/example/product/..."],
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
  "residual_risks": []
}
```

Handoff는 다음 coverage invariant를 모두 만족한다.

- `implemented_behavior_ids`는 card의 모든 `effective_behavior.id`를 중복 없이 정확히 한 번씩
  포함하고 존재하지 않는 ID를 포함하지 않는다.
- `acceptance_results`는 모든 `acceptance_criteria.id`를 중복 없이 정확히 한 번씩 포함하며 각
  result는 `pass`다. 각 `focused_verification_ids`는 card가 해당 acceptance에 연결한 ID와 같다.
- `focused_verification_results`는 모든 `focused_verification.id`를 중복 없이 정확히 한 번씩
  포함하고 존재하지 않는 ID를 포함하지 않는다.
- 각 focused result의 `scenario_results`는 card의 모든 `required_scenarios`를 중복 없이 정확히
  한 번씩 포함하며 모든 result가 `pass`다.
- Full backend verification result는 card와 같은 executor/task를 가리키며 `pass`다.

`changed_paths`에는 실제 task 변경만 기록하고 문서 path를 포함하지 않는다. 실행하지 못한 검증,
실패, dirty conflict 또는 미정 policy는 handoff가 아니라 `kanban_block` 대상이다.

## PM checkpoint evidence

PM은 native review에서 Coder handoff와 diff를 검증하고 commit한 뒤 다음 사실을 review completion
metadata 또는 durable comment에 기록한다.

```json
{
  "schema": "backend-implementation-checkpoint-v1",
  "result": "pass",
  "committed_paths": ["src/main/java/example/product/...", "src/test/java/example/product/..."],
  "commit_sha": "<40-character result commit SHA>",
  "full_backend_verification_readback": "pass",
  "clean_worktree": true
}
```

PM이 commit SHA, committed paths와 clean working tree를 read-back하기 전에는 Impl card를 `done`으로
전환하지 않는다. `request-changes`이면 같은 body를 유지하고 구체적인 finding을 Coder에게 돌려보낸다.
