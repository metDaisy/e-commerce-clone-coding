# refactor-coder distribution

Install:

```text
hermes profile install ./.hermes/profile-distributions/refactor-coder --name refactor-coder --alias
```

This Profile accepts only a fixed diff, canonical approved findings, relevant
requirements, and verification evidence. It downloads the two Java refactoring
Skills from skills.sh and does not include credentials or runtime state.

The setup script downloads these skills from skills.sh:

- `skills-sh/github/awesome-copilot/java-refactoring-extract-method`
- `skills-sh/github/awesome-copilot/java-refactoring-remove-parameter`
