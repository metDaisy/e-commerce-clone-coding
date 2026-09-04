# reviewer-compatibility distribution

Install:

```text
hermes profile install ./.hermes/profile-distributions/reviewer-compatibility --name reviewer-compatibility --alias
```

The reviewer applies the skills.sh `compat-review` Skill plus the
project-specific rules in `SOUL.md` to public contracts and framework
compatibility. It is report-only and contains no credentials or runtime state.

The setup script downloads
`skills-sh/toss/es-toolkit/compat-review` from
skills.sh. Project-specific Java/Spring contract rules remain in `SOUL.md`.
