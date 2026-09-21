# reviewer distribution

이 디렉터리는 runtime Profile이 아닌 Git으로 공유하는 Hermes Profile Distribution 원본이다.
Credential, Memory, session, state database, log와 machine-specific path는 포함하지 않는다.

## 설치

```text
hermes profile install ./.hermes/profiles/reviewer --name reviewer --alias --force --yes
```

일반 설치는 `.hermes/scripts/install-profiles.sh` 또는 `.hermes/scripts/install-profiles.ps1`를 사용한다.
설치 스크립트가 repository root, Kanban 설정과 Profile별 capability policy를 적용하고 read-back한다.

## 책임

`reviewer`는 모든 Coder checkpoint가 완료된 뒤 PM-authored `aggregate-review-card-v1`을 독립적으로
검토한다. `run-review`가 card, checkpoint와 commit range를 고정하고 다섯 leaf Skill의 판단을 분리해
수집한다. 중복 finding만 통합한 뒤 terminal run metadata에 `aggregate-review-result-v1`을 남긴다.

Reviewer는 파일을 수정하거나 commit하지 않는다. Finding의 rework contract 작성, 다음 task promotion,
Summary와 release는 Project Manager 책임이다.

## 전속 Skill

- `run-review`: task admission, immutable evidence, 축 실행, 검증, 통합과 native handoff
- `review-spec`: frozen behavior와 acceptance 충족 여부
- `review-maintainability`: concrete change cost, shallow abstraction과 speculative generality
- `review-persistence`: transaction, query/fetch, database와 migration evidence
- `review-architecture`: requirement 영향 closure의 module, interface와 Spring Modulith 경계
- `review-evolution-compatibility`: public/persisted contract와 rollout compatibility
- `improve-codebase-architecture`: architecture 축의 scoped deep-module 분석 helper
- `codebase-memory-mcp`: symbol·dependency·call path 후보 탐색
- `semble-search`: 정확한 구현 위치를 모를 때 의미 기반 후보 탐색

`SOUL.md`는 안정적인 Reviewer 정체성과 권한 경계를 소유한다.
`run-review`는 lifecycle과 aggregation을, 각 `review-*` Skill은 축별 evidence rubric을 소유한다.
`run-review/references/aggregate-review-contract.md`는 Review card 소비와 최종 결과 계약을,
`axis-result-contract.md`는 leaf invocation packet과 결과·통합 규칙을 소유한다.
`code-discovery-guide.md`는 leaf가 exact source, Semble과 Codebase Memory를 선택하는 조건과 source 확인을
소유한다. `review-rubric-provenance.md`는 참고한 범용 Skill에서 채택·제외한 원칙을 기록하지만 runtime
절차는 소유하지 않는다. PM Distribution은 card authoring, 결과 validation과 finding routing을 소유한다.

Regular aggregate Review의 architecture 범위는 현재 requirement의 영향 closure다. 전체 codebase 분석은
새 module·대규모 integration 전, 반복 architecture finding, major milestone/upgrade 또는 정기 health
checkpoint에 PM이 별도 architecture-audit task로 승인할 때만 수행한다.
