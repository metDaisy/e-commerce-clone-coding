# PM 구현 checkpoint 계약

이 문서는 Project Manager가 Coder의 same-card review handoff를 검수하고 changes-request 또는 checkpoint
completion으로 전환하는 권한과 field contract를 소유한다. 실행 순서는
[`../SKILL.md`](../SKILL.md), 결정론적 validation은 [`../scripts/checkpoint.py`](../scripts/checkpoint.py)가 소유한다.
Coder의 구현·검증·handoff 생성 절차는 Coder `run-impl-card` 계약이 소유한다.

## PM 입력과 권한

PM은 dispatcher가 시작한 active native review run에서 다음 입력을 read-back한다.

1. immutable `backend-implementation-card-v1` body
2. latest Coder run의 `backend-implementation-handoff-v1` metadata
3. 실제 workspace, branch, Git status와 complete diff
4. focused 및 full backend verification evidence

PM은 handoff와 diff를 검수하고, changes를 요청하거나 검증된 path만 commit하고 card를 완료할 수 있다.
PM은 Coder 대신 production code를 수정하거나 card의 제품 의미를 보정하지 않는다.

## Checkpoint review

PM은 다음을 확인한다.

- handoff가 card의 모든 behavior, acceptance, verification과 required scenario를 정확히 한 번씩 포함한다.
- actual dirty paths가 `handoff.changed_paths`와 같고 문서나 설명할 수 없는 path가 없다.
- diff가 card scope, security, module boundary, persistence와 migration 규칙을 만족한다.
- focused verification과 `gradle-mcp` backend `test` 결과가 모두 `pass`다.

구현 수정이 필요하면 commit하지 않고 changes-request로 route한다. Policy 또는 contract 결정이 필요하면
사용자 owner에게 block한다.

## Changes-request contract

PM은 native `request-changes` 직전에 다음 valid JSON object를 durable comment로 기록한다. Native reason은
finding ID와 짧은 요약만 가지며 이 object를 대체하지 않는다.

```json
{
  "schema": "backend-implementation-change-request-v1",
  "source_handoff_id": "handoff-7f3d87b2",
  "source_review_run_id": "run-review-17",
  "findings": [
    {
      "finding_id": "PM-CHK-1",
      "path": "src/main/java/example/product/ProductService.java",
      "symbol": "ProductService.register",
      "observed_problem": "유효하지 않은 가격을 저장한다.",
      "expected_result": "유효하지 않은 가격을 기존 validation error contract로 거절한다.",
      "allowed_scope": ["상품 등록 validation과 직접 관련된 테스트"],
      "verification": ["FV-PRODUCT-CREATE", "backend 전체 test"]
    }
  ]
}
```

`source_handoff_id`는 검토한 latest handoff와 같고 `source_review_run_id`는 active PM review run과 같다.
`finding_id`는 request 안에서 고유하다. 각 finding은 exact path·symbol, 관찰된 문제, 기대 결과, 허용
범위와 다시 실행할 검증을 모두 가진다. 일반적인 개선 요청이나 card의 제품 의미를 바꾸는 요청은
허용하지 않는다. `scripts/checkpoint.py change-request` validation이 통과한 뒤에만 comment와 native
`request-changes`를 수행한다.

## Checkpoint completion contract

PM은 검증된 path만 stage·commit하고 result commit과 clean worktree를 read-back한 뒤 다음 metadata를
작성한다.

```json
{
  "schema": "backend-implementation-checkpoint-v1",
  "source_handoff_id": "handoff-7f3d87b2",
  "result": "pass",
  "committed_paths": ["src/main/java/example/product/...", "src/test/java/example/product/..."],
  "commit_sha": "<40-character result commit SHA>",
  "full_backend_verification_readback": "pass",
  "clean_worktree": true
}
```

`source_handoff_id`는 검수한 handoff와 같고 `committed_paths`는 그 handoff의 `changed_paths`와 중복 없이
정확히 같아야 한다. Full backend evidence, 40-character commit SHA와 clean worktree를 read-back하기
전에는 completion metadata를 작성하거나 task를 `done`으로 전환하지 않는다.

`scripts/checkpoint.py checkpoint` validation 후 `kanban_complete`를 호출하고 task `done`, closing PM run
metadata, result SHA, committed paths와 clean Git state를 다시 read-back한다.

## Lifecycle boundary

```text
ready → running(Coder) → review(PM) → done
                          └→ request-changes → ready 또는 dependency-gated todo
review → blocked → resumed PM review phase
```

PM checkpoint는 independent aggregate Review를 대체하지 않는다.
