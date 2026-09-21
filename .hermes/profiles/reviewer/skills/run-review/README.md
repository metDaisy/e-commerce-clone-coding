# run-review

`reviewer`가 PM-authored aggregate Review card와 완료된 child checkpoint를 고정하고 다섯 축의
독립 판정을 통합해 canonical result를 같은 native task에 남기는 root 절차다.

- Profile `SOUL.md`: 안정적인 Reviewer 정체성과 mutation 금지 경계
- [`references/aggregate-review-contract.md`](references/aggregate-review-contract.md): 입력과 최종 결과 schema
- [`references/axis-result-contract.md`](references/axis-result-contract.md): leaf Skill 공통 입출력과 통합 규칙
- [`references/code-discovery-guide.md`](references/code-discovery-guide.md): exact source, Semble과 Codebase
  Memory 선택·fallback·source 확인 규칙
- [`references/review-rubric-provenance.md`](references/review-rubric-provenance.md): 참고한 범용 Skill에서
  채택·제외한 원칙과 runtime dependency 경계
- [`SKILL.md`](SKILL.md): admission, 축 실행, 검증, 집계와 native completion

축별 판단은 `review-spec`, `review-maintainability`, `review-persistence`, `review-architecture`,
`review-evolution-compatibility`가 소유한다. Reviewer는 code·test·migration·문서와 task body를 수정하지
않고 모든 Gradle 작업을 `gradle-mcp`로만 실행한다.