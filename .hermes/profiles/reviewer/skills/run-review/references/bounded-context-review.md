# Bounded-context aggregate Review 실행 계약

이 문서는 `run-review`의 aggregate Review를 local model context window 안에서 재현 가능하게 수행하는 실행 절차를 소유한다. Kanban card와 final `aggregate-review-result-v2` schema는 변경하지 않는다.

## 목적

Aggregate Review는 하나의 native Review card와 하나의 terminal result를 유지한다. 그러나 다섯 review axis를 하나의 긴 worker conversation에 누적하지 않는다. 각 axis는 **one axis per session** 원칙으로 실행한다.

Root Reviewer는 `delegate_task`로 매 axis를 **fresh isolated axis session**에 하나씩 위임한다. Local llama.cpp 모델을 사용할 때는 동시에 여러 axis를 실행하지 않고 순차 실행한다. 이는 GPU 병렬성보다 context 예산과 결과 추적성을 우선한다.

## Compact immutable packet

Admission이 닫힌 뒤 Root는 다음 사실만 포함한 compact immutable packet을 만든다.

- review task/run ID와 generation
- `baseline_sha`, `reviewed_head_sha`, ordered checkpoint key/task/SHA
- effective behavior ID, aggregate acceptance, scope exclusions
- 해당 axis의 requirement/repository-rule/architecture locator
- prior finding identity와 해당 axis의 필요한 evidence locator

다음은 packet이나 child prompt에 넣지 않는다.

- 전체 `kanban_show` envelope, parent body, comments, events 또는 run history
- 전체 `git diff`, full file contents, raw tool output, Gradle output, reasoning
- 다른 axis의 conclusion, finding, observation
- Coder 또는 PM의 결론

Child는 fixed base/head에서 필요한 changed paths와 source를 직접 확인한다. Root가 source 후보를 전달해야 하면 axis와 직접 관련된 locator만 전달한다.

## Axis dispatch and durable handoff

1. Root는 `review-spec`, `review-maintainability`, `review-persistence`, `review-architecture`, `review-evolution-compatibility` 순서로 child를 하나씩 dispatch한다.
2. Child는 자신의 leaf Skill을 load하고, read-only source discovery와 해당 axis 판단만 수행한다. Kanban mutation, Gradle execution, source edit, commit과 다른 axis 재검토는 금지한다.
3. Child의 final response는 `axis-result-contract.md`의 형식만 포함한다. Evidence는 exact locator와 test/validator identity를 사용하며 raw diff나 raw tool output을 복사하지 않는다.
4. Root는 scope identity와 필수 field를 검증한 뒤 다음 compact comment를 현재 Review task에 한 번 남긴다. 이 comment는 final result가 아닌 durable intermediate evidence다.

```json
{
  "schema": "aggregate-review-axis-v1",
  "review_task_id": "t_a1b2c3",
  "review_run_id": 21,
  "axis": "spec",
  "scope_identity": {
    "baseline_sha": "0123456789abcdef0123456789abcdef01234567",
    "reviewed_head_sha": "89abcdef0123456789abcdef0123456789abcdef"
  },
  "applicability": "reviewed",
  "evidence": ["src/main/java/example/CartService.java:42"],
  "verification_requested": ["CartServiceTest#addsItem"],
  "finding_ids": [],
  "conclusion": "요구사항 behavior와 현재 source/test mapping을 확인했다.",
  "blocker": null
}
```

`evidence`, `verification_requested`, `finding_ids`와 `conclusion`은 concise locator/summary만 가진다. Card comment에는 raw diff, raw tool output, prompt, reasoning 또는 credential을 기록하지 않는다.

## Resume and failure

- Root가 죽거나 context capacity가 부족해지면 PM은 `aggregate-review-axis-v1` comments를 read-back한다.
- 동일한 `baseline_sha`, `reviewed_head_sha`, task/run scope에서 이미 닫힌 axis는 재실행하지 않는다.
- 아직 없는 axis만 새 fresh isolated axis session으로 실행한다.
- Child가 malformed result를 반환하면 Root가 추측으로 보완하지 않고 같은 axis를 새 session에서 한 번만 다시 실행한다.
- delegation capability, immutable scope, source, tool 또는 model context가 부족하면 `PM review requested` comment와 native `kanban_block(kind="needs_input")`으로 중단한다.
- 정상 axis 경계는 `blocked` 상태 전환 사유가 아니다. `blocked`는 PM 조치나 외부 prerequisite가 필요한 실제 문제에만 사용한다.

## Root aggregation

모든 durable axis comment가 같은 immutable scope를 가리키고 모두 닫힌 뒤에만 Root는 security baseline과 root-owned Gradle verification을 수행한다. Canonical `aggregate-review-result-v2`에는 기존 exact top-level field 집합만 사용한다. Axis comment의 상세를 result metadata에 재복사하지 않는다.
