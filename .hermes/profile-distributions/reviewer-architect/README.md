# reviewer-architect distribution

Install:

```text
hermes profile install ./.hermes/profile-distributions/reviewer-architect --name reviewer-architect --alias
```

The reviewer evaluates recent changes, ADRs, package-info boundaries, and
Spring Modulith structure. It produces report candidates only; HTML reports
must be written outside the repository. No credentials or runtime state is
included.

The setup script downloads these skills from skills.sh:

- `skills-sh/mattpocock/skills/improve-codebase-architecture`
- `skills-sh/getsentry/warden/architecture-review`
- `skills-sh/jabrena/plinth/305-frameworks-spring-boot-modulith`
