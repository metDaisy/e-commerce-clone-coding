# prototype-coder distribution

This directory is a shareable Hermes Profile Distribution source, not a runtime
profile directory. It intentionally contains no credentials, Memory, sessions,
state database, logs, or machine-specific working directory.

Install it from the repository with:

```text
hermes profile install ./.hermes/profile-distributions/prototype-coder --name prototype-coder --alias
```

After installation, set the local project working directory and model/provider
through Hermes commands for the recipient's machine.

The setup script downloads these skills from skills.sh into the target Profile:

- `skills-sh/github/awesome-copilot/java-springboot`
- `skills-sh/github/awesome-copilot/java-junit`
