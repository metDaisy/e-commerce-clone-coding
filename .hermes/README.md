# Project-local Hermes audit

This directory contains the project-local Agent audit plugin and its runtime log.

Project plugin source is kept under `.hermes/plugins/` so it can be reviewed and
committed with the repository:

- `agent-audit/`: runtime hooks for Agent behavior and validator evidence

The personal `codex-quota` Desktop plugin remains under the active Hermes
profile and is intentionally not part of this repository.

Hermes CLI project discovery uses `.hermes/plugins/` when
`HERMES_ENABLE_PROJECT_PLUGINS=1`. The project-local plugin in this repository
is `agent-audit`; the Desktop-only personal plugin uses the active profile's
Desktop plugin door separately.

## Activation

Project-local Hermes plugins are opt-in. Start Hermes with:

```bash
HERMES_ENABLE_PROJECT_PLUGINS=true hermes --profile coder
```

For the Desktop app, enable the same environment variable before launching or
use the profile/plugin reload flow after activation.

## Runtime log

`.hermes/events.jsonl` is local runtime data and is ignored by Git. It records
compact metadata only:

- Skill lifecycle: skill name, action, provenance, and opaque correlation IDs
- Tool lifecycle: tool name, status, duration, and safe project-relative paths
- Validation trigger/result: Rule ID, validator category, generation, and pass/fail status
- Verification gate: changed paths and missing/failed Rule IDs
- Session end: completion and interruption status

Prompts, conversation history, terminal commands, raw tool arguments, raw tool
results, reasoning, credentials, and absolute paths are not persisted.

## Verification behavior

The plugin is fail-open for logging. When edited Java files reach Hermes'
verification gate, it asks the Agent to run Checkstyle, relevant tests, and
`ModularityTest` through `gradle-mcp`. If a recent `gradle-mcp` call reports a
failure, the hook gives the Agent a bounded diagnostic nudge so it can correct
the code and verify again. It does not block tool calls and it does not invoke
Gradle directly.

The project-level Skill selection and portfolio map are documented in
[`../docs/skills/index.md`](../docs/skills/index.md). The Rule contract is in
[`../docs/validator-contract.md`](../docs/validator-contract.md).
