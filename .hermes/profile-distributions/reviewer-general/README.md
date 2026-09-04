# reviewer-general distribution

Install:

```text
hermes profile install ./.hermes/profile-distributions/reviewer-general --name reviewer-general --alias
```

한 번의 세션에서는 `focus=spec`, `focus=maintainability`, `focus=compatibility` 중
하나만 실행합니다. 각 세션은 동일한 fixed diff를 독립적으로 확인하고 결과를 Coordinator에
전달합니다. 이 Profile은 report-only입니다.

setup script는 다음 skills.sh Skill을 설치합니다.

- `skills-sh/mattpocock/skills/code-review`
- `skills-sh/alannkl/skills/simplify-code`
- `skills-sh/toss/es-toolkit/compat-review`
