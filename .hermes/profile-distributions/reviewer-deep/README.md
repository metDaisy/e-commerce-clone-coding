# reviewer-deep distribution

Install:

```text
hermes profile install ./.hermes/profile-distributions/reviewer-deep --name reviewer-deep --alias
```

한 번의 세션에서는 `focus=architecture` 또는 `focus=persistence` 하나만 실행합니다.
Architecture와 Persistence는 동일한 final diff를 독립적으로 확인하고 Coordinator가
결과를 통합합니다. 이 Profile은 report-only입니다.

setup script는 다음 skills.sh Skill을 설치합니다.

- `skills-sh/mattpocock/skills/improve-codebase-architecture`
- `skills-sh/getsentry/warden/architecture-review`
- `skills-sh/jabrena/plinth/305-frameworks-spring-boot-modulith`
- `skills-sh/affaan-m/ecc/jpa-patterns`
