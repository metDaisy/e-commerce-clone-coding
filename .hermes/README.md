# Project-local Hermes plugins

This directory contains the project-local Agent audit plugin and its runtime log.

Project plugin source is kept under `.hermes/plugins/` so it can be reviewed and
committed with the repository:

- `agent-audit/`: runtime hooks for Agent behavior and validator evidence

The personal `codex-quota` Desktop plugin remains under the active Hermes
profile and is intentionally not part of this repository.

Hermes CLI project discovery uses `.hermes/plugins/` when
`HERMES_ENABLE_PROJECT_PLUGINS=1`. The Desktop-only personal plugin uses the
active profile's Desktop plugin door separately.

## Activation

Project-local Hermes plugins are opt-in. Start Hermes with:

```bash
HERMES_ENABLE_PROJECT_PLUGINS=true hermes --profile implementation-coder
```

For the Desktop app, enable the same environment variable before launching or
use the profile/plugin reload flow after activation.

## Capability policy

The operational Profiles use repository-managed capability policies. Each
Profile Distribution keeps its policy in `profiles/<source-name>/capabilities.yaml`,
including its Skill/toolset/MCP/GitHub write allowlists. Each Profile must
declare its complete `skills.allowed`, `tools.allowed_toolsets`, and
`mcp.allowed_servers`/`mcp.filters` set. These files contain no endpoints or
credentials.

The common `.hermes/scripts/apply-hermes-capabilities.py` compiles the declared
Skill and built-in toolset allowlists into Hermes' native deny-list settings,
disables configured MCP servers outside the declared allowlist, applies exact
MCP `tools.include` filters, and reads the effective state back:

```bash
python .hermes/scripts/apply-hermes-capabilities.py \
  --policy .hermes/profiles \
  --profile project-manager \
  --profile implementation-coder \
  --project-root .
```

MCP servers must already be configured in the local Profile. Servers not listed
for a Profile are disabled; missing allowed servers are reported but are not
created because their endpoints are machine-local configuration.

## Runtime log

`.hermes/events.jsonl` is local runtime data and is ignored by Git. It records
compact metadata only:

- Skill lifecycle: skill name, action, provenance, and opaque correlation IDs
- Tool lifecycle: tool name, status, duration, and safe project-relative paths
- Validation trigger/result: Rule ID, validator category, generation, and pass/fail status
- Verification gate: changed paths and missing/failed Rule IDs
- Workflow deviation: direct Gradle use or verification that is missing, failed, or uncorrelated
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
