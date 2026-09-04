# reviewer-maintainability distribution

Install:

```text
hermes profile install ./.hermes/profile-distributions/reviewer-maintainability --name reviewer-maintainability --alias
```

This reviewer is report-only. It uses the skills.sh `simplify-code` review procedure
with reuse, quality, efficiency, and altitude findings limited to the requested
diff. It contains no credentials or runtime state.

The setup script downloads `skills-sh/alannkl/skills/simplify-code` from
skills.sh into the target Profile.
