# Aggregate Review 소비·결과 계약 v1

이 문서는 `reviewer`가 PM-authored `aggregate-review-card-v1`을 해석하고
`aggregate-review-result-v1`을 작성하는 계약을 소유한다. PM은 graph/card authoring, result의
결정론적 validation과 finding routing을 소유한다. Native Kanban task·run·event·comment와 Git read-back이
identity와 실행 사실의 원본이며 body나 result가 이를 대신하지 않는다.

## 입력 계약

Reviewer는 dispatcher가 시작한 active run에서 다음을 read-back한다.

1. Assignee가 `reviewer`이고 status가 `running`인 actual aggregate Review task
2. Closed body fields: `schema`, `effective_behavior_ids`, `implementation_card_keys`,
   `inherited_behavior_ids`, `aggregate_acceptance`
3. 각 implementation key의 immutable `backend-implementation-card-v1`, done task ID와 latest terminal
   `backend-implementation-checkpoint-v1`
4. Prior Review의 unresolved blocking finding과 후속 corrective/context/decision evidence
5. Actual workspace/cwd, branch, clean worktree, current HEAD와 checkpoint commit history

Body의 schema는 `aggregate-review-card-v1`이다. Effective behavior와 aggregate acceptance는 목표 계약이며,
implementation/inherited 목록은 evidence coverage를 고정한다. Native task ID, status, run과 workspace를
body에서 추론하거나 복제값으로 대체하지 않는다.

Admission invariant:

- Body implementation key마다 done task와 유효한 40자리 checkpoint SHA가 정확히 하나 있다.
- Checkpoint는 현재 review branch history에 존재하고 final reviewed state에 포함된다.
- Prior blocking finding은 source Review task ID, finding ID와 원래 verdict로 추적된다.
- Repository가 clean하고 current run/task/workspace가 dispatcher read-back과 일치한다.

하나라도 실패하면 result를 만들지 않고 native task를 block한다.

## Result schema

Reviewer는 latest terminal run metadata에 다음 closed object를 기록한다.

```json
{
  "schema": "aggregate-review-result-v1",
  "review_card_key": "review-1",
  "review_task_id": "t_a1b2c3",
  "review_run_id": "run-reviewer-21",
  "generation": 1,
  "result": "changes-required",
  "reviewed_checkpoints": [
    {
      "implementation_card_key": "impl-1",
      "implementation_task_id": "t_d4e5f6",
      "checkpoint_sha": "0123456789abcdef0123456789abcdef01234567"
    }
  ],
  "prior_findings": [],
  "findings": [
    {
      "finding_id": "REV-1",
      "verdict": "correction-required",
      "basis": "aggregate acceptance: 중복 주문 요청은 한 번만 반영된다.",
      "observed_fact": "동일 idempotency key의 동시 요청이 별도 주문 두 건을 저장한다.",
      "evidence": [
        "src/main/java/example/order/OrderService.java:42",
        "OrderConcurrencyIntegrationTest#sameKeyCreatesOneOrder failed"
      ],
      "impact": "재시도 또는 동시 요청에서 중복 주문과 이중 결제가 발생할 수 있다.",
      "resolves": null
    }
  ]
}
```

Top-level field 의미:

- `review_card_key`, `review_task_id`, `review_run_id`, `generation`은 actual card/run과 일치한다.
- `result`는 `approved | changes-required | blocked`다. 정상 review에서 blocking finding이 있으면
  `changes-required`, 없으면 `approved`다. Admission/검증 prerequisite가 없으면 incomplete `blocked`
  result를 완료 metadata로 남기지 말고 native task를 block한다.
- `reviewed_checkpoints`는 body의 모든 implementation key를 중복 없이 정확히 한 번 포함한다.
- `prior_findings`는 이전 Review의 아직 해결을 증명해야 하는 blocking finding 전체다.
- `findings`는 이번 Review의 새 finding과 prior finding resolution이다. Finding이 없으면 빈 배열이다.

`prior_findings` item은 `source_review_task_id`, `source_finding_id`, `verdict`만 가지며 verdict는
`correction-required | context-required | decision-required`다.

## Finding 계약

Finding은 `finding_id`, `verdict`, `basis`, `observed_fact`, non-empty `evidence`, `impact`, `resolves`를
가진다. ID는 현재 result 안에서 고유하다.

| verdict | 사용 조건 | PM의 후속 routing |
|---|---|---|
| `correction-required` | 기존 effective behavior를 만족하도록 code/test 수정 필요 | Corrective Impl |
| `context-required` | 승인된 source/context 확인 없이는 결론 불가 | Next aggregate Review |
| `decision-required` | 새 business policy·authorization·consistency·error/public contract 결정 필요 | User-owned blocked Decision |
| `resolved` | Prior blocking finding이 committed evidence로 해소됨 | Prior finding closure |

`resolved`만 `resolves` object를 가지며 `source_review_task_id`, `source_finding_id`를 정확히 가리킨다.
다른 verdict의 `resolves`는 null이다. 모든 prior finding은 현재 findings에서 정확히 한 번 resolved되어야
`approved`가 가능하다. 존재하지 않는 prior finding을 resolve하거나 unresolved finding을 누락하지 않는다.

`basis`는 card acceptance나 repository rule/architecture contract를 특정한다. `observed_fact`는 재현 가능한
현재 사실만 기술한다. `evidence`는 exact `path:line`, symbol, test/validator identity와 checkpoint SHA 중
판정을 재현하는 locator를 사용한다. `impact`는 영향을 받는 behavior, data, security, caller 또는
maintenance cost를 설명한다. 단순 선호와 측정되지 않은 가능성은 blocking finding이 아니다.

## Result invariant

- `approved`: 새 blocking finding이 없고 prior finding 전체가 explicit `resolved`다.
- `changes-required`: 하나 이상의 blocking finding이 있다.
- 모든 body implementation key와 result checkpoint key가 정확히 일치한다.
- Review는 Spec, maintainability, persistence, architecture, evolution·compatibility 다섯 축을 모두
  수행하거나 근거와 함께 not-applicable로 판정한 뒤에만 완료한다.
- Runtime metadata에는 raw tool output, prompt, reasoning, credential 또는 불필요한 개인정보를 넣지 않는다.

PM은 result와 graph를 자신의 `validate-review-result` validator로 검증하고 native run/task를 read-back한
뒤에만 rework 또는 Summary로 전환한다.