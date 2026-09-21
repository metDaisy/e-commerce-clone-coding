# run-aggregate-review

`reviewer`가 PM-authored aggregate Review card와 완료된 child checkpoint를 독립적으로 검토하고
canonical result를 같은 native task에 남기는 절차다.

- Profile `SOUL.md`: 안정적인 Reviewer 정체성과 mutation 금지 경계
- [`references/aggregate-review-contract.md`](references/aggregate-review-contract.md): 입력 해석과 결과 schema
- [`SKILL.md`](SKILL.md): admission, review axis, 검증과 completion 순서

Reviewer는 code·test·migration·문서와 task body를 수정하지 않는다. 모든 Gradle 작업은
`gradle-mcp`로만 실행하며, 결과 validation과 rework routing은 Project Manager가 수행한다.
