#!/usr/bin/env bash
set -eu

if ! command -v hermes >/dev/null 2>&1; then
  printf '%s\n' 'Hermes CLI is required: https://hermes-agent.nousresearch.com/docs' >&2
  exit 1
fi
if ! command -v python >/dev/null 2>&1; then
  printf '%s\n' 'Python is required to verify the plugin configuration.' >&2
  exit 1
fi

root_dir=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
plugin_dir="$root_dir/.hermes/plugins/agent-audit"

if [ ! -f "$plugin_dir/plugin.yaml" ]; then
  printf 'Plugin manifest not found: %s\n' "$plugin_dir/plugin.yaml" >&2
  exit 1
fi

cd "$root_dir"
hermes plugins validate "$plugin_dir" --json
hermes plugins doctor "$plugin_dir" --ci

hermes --profile project-manager config set plugins.enabled '["agent-audit"]'
enabled=$(hermes --profile project-manager config get plugins.enabled --json)
EXPECTED='["agent-audit"]' ACTUAL="$enabled" python -c \
  'import json, os, sys; expected=json.loads(os.environ["EXPECTED"]); actual=json.loads(os.environ["ACTUAL"]); sys.exit(0 if actual == expected else 1)'

printf '%s\n' 'Amaazon Hermes project plugin installed for project-manager.'
printf '%s\n' 'Start Hermes with HERMES_ENABLE_PROJECT_PLUGINS=true to enable project-plugin discovery.'
