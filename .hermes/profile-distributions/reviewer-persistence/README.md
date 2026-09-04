# reviewer-persistence distribution

Install:

```text
hermes profile install ./.hermes/profile-distributions/reviewer-persistence --name reviewer-persistence --alias
```

The reviewer uses the skills.sh `jpa-patterns` Skill plus the project-specific
rules in `SOUL.md`; `docs/testing-guide.md` remains the repository source of
truth. It reports query/fetch risks without editing code or tests and contains
no runtime state.

The setup script downloads `skills-sh/affaan-m/ecc/jpa-patterns` from skills.sh.
The project-specific query-count rules remain in `SOUL.md` because they are
repository policy, not a registry Skill.
