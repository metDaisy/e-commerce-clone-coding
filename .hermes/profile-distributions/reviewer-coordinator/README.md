# reviewer-coordinator distribution

Install:

```text
hermes profile install ./.hermes/profile-distributions/reviewer-coordinator --name reviewer-coordinator --alias
```

The coordinator accepts only a fixed diff, requirement/architecture paths,
verification results, and reviewer findings. It emits a canonical report and
never edits the worktree. Memory, sessions, credentials, and state are not
part of this source.

This Profile intentionally has no assigned Skill; its role is defined by the
Distribution `SOUL.md` and the project handoff contract.
