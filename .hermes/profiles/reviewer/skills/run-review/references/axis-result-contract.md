# Review axis 결과 계약 v1

이 계약은 `run-review`가 다섯 leaf Review Skill에 동일한 immutable 범위를 전달하고 결과를 빠짐없이
수집하기 위한 내부 계약이다. Axis result는 intermediate evidence이며 native Kanban metadata에 그대로
저장하지 않는다. 최종 저장 계약은 `aggregate-review-result-v1`이다.

## 공통 입력

각 leaf Skill은 다음 값을 동일하게 받는다.

- review task/run ID와 generation
- `aggregate-review-card-v1`의 effective behavior, aggregate acceptance와 scope exclusion
- fixed base, reviewed HEAD, ordered checkpoint task/key/SHA
- reviewed commit range와 changed paths
- 관련 requirement, repository rule, architecture/ADR locator
- 해당 축이 독립적으로 확인할 source/test/configuration/migration 후보
- prior finding 중 현재 축이 재검토해야 할 identity와 원래 evidence

다른 축의 finding이나 결론은 입력하지 않는다. 위치 후보는 사실이 아니며 leaf가 source에서 확정한다.

## Invocation packet

Root는 매 leaf 실행 직전에 공통 입력을 복사해 axis-specific packet을 만든다. Packet에는 `axis`, immutable
scope identity와 위 공통 입력만 허용한다. 다른 axis result, aggregate verdict와 구현자의 해석은 넣지
않는다. Leaf는 packet identity를 결과의 `scope_identity`에 그대로 반환하고, 추가로 조사한 path와 symbol은
`scope_examined` 또는 `evidence_examined`에 기록한다.

Leaf가 위치를 조사할 때는
[`code-discovery-guide.md`](code-discovery-guide.md)의 source-direct, Semble, Codebase Memory branch 중
질문에 맞는 하나를 선택한다. 한 tool의 결과가 새 관계 질문을 만들었을 때만 다른 branch로 이어가며,
같은 질문을 두 discovery tool에 반복하지 않는다.

## 결과 형식

```text
axis: spec | maintainability | persistence | architecture | evolution-compatibility
scope_identity:
applicability: reviewed | not-applicable | blocked
scope_examined:
evidence_examined:
verification_requested_or_run:
findings:
non_blocking_observations:
unverified_or_blocker:
conclusion_basis:
```

- `reviewed`: 축을 실제로 검토했다. Finding이 없으면 어떤 evidence와 질문을 확인했는지 기록한다.
- `not-applicable`: changed behavior와 영향 closure에 해당 concern이 없음을 path/call/dependency evidence로
  입증했다. 조사하지 않았다는 뜻으로 사용하지 않는다.
- `blocked`: 필요한 source, contract, tool 또는 environment가 없어 결론을 낼 수 없다.
- `non_blocking_observations`에는 preference, 측정 전 가설, unrelated pre-existing debt와 후속 audit 후보만
  둔다. 이것만으로 aggregate verdict를 변경하지 않는다.

Finding 후보는 axis, basis, observed fact, evidence, impact, suggested verdict를 가진다. 정확한 위치나
재현 가능한 validator가 없는 설계 선호는 finding이 아니다.

## Root 통합 규칙

1. 다섯 axis identity가 같은 immutable review 범위를 가리키는지 확인한다.
2. 하나라도 누락되거나 `blocked`면 native task를 block하고 final result를 완료하지 않는다.
3. `basis + observed fact + affected behavior`가 같은 중복만 합친다.
4. 서로 다른 축의 독립 위험과 충돌하는 판정은 근거와 함께 보존한다.
5. Leaf가 요청한 deterministic verification은 root가 모아 실행하고 실제 결과를 final evidence에 연결한다.
6. Axis name이나 intermediate observation을 추가하기 위해 `aggregate-review-result-v1` schema를 확장하지
   않는다. PM과 합의한 wire contract만 사용한다.