# reviewer-spec distribution

This directory is the shareable source for the `reviewer-spec` Hermes
Profile. Install it with:

```text
hermes profile install ./.hermes/profile-distributions/reviewer-spec --name reviewer-spec --alias
```

The reviewer receives project rules, supplied requirement paths, the diff, and
verification output. It must report findings only; filesystem write actions
remain outside the review workflow. This source contains no credentials,
Memory, sessions, state database, or machine-specific paths.

The setup script downloads `skills-sh/mattpocock/skills/code-review` from
skills.sh into the target Profile.
