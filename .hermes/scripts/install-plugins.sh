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

default_home=$(python -c 'from hermes_constants import get_hermes_home; print(get_hermes_home())')
profile_root=$(python -c 'from pathlib import Path; from hermes_constants import get_hermes_home; print(Path(get_hermes_home()).parent.as_posix())')
root_dir=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd -W)
plugin_dir="$root_dir/.hermes/plugins/agent-audit"
hook_command="python $plugin_dir/shell_hook.py"
hook_value=$(HOOK_COMMAND="$hook_command" python -c 'import json, os; print(json.dumps([{"command": os.environ["HOOK_COMMAND"]}]))')

approve_hooks() {
  local home="$1"
  HERMES_HOME="$home" HERMES_ACCEPT_HOOKS=1 python -c \
    'from agent.shell_hooks import register_from_config; from hermes_cli.config import load_config; register_from_config(load_config(), accept_hooks=False)'
}

if [ ! -f "$plugin_dir/plugin.yaml" ]; then
  printf 'Plugin manifest not found: %s\n' "$plugin_dir/plugin.yaml" >&2
  exit 1
fi

cd "$root_dir"
hermes plugins validate "$plugin_dir" --json
hermes plugins doctor "$plugin_dir" --ci

for event in post_tool_call on_skill_lifecycle pre_verify on_session_end; do
  hermes config set "hooks.$event" "$hook_value"
done
approve_hooks "$default_home"

for profile in project-manager coder reviewer; do
  hermes --profile "$profile" config set plugins.enabled '["agent-audit"]'
  enabled=$(hermes --profile "$profile" config get plugins.enabled --json)
  EXPECTED='["agent-audit"]' ACTUAL="$enabled" python -c \
    'import json, os, sys; expected=json.loads(os.environ["EXPECTED"]); actual=json.loads(os.environ["ACTUAL"]); sys.exit(0 if actual == expected else 1)'
  for event in post_tool_call on_skill_lifecycle pre_verify on_session_end; do
    hermes --profile "$profile" config set "hooks.$event" "$hook_value"
  done
  approve_hooks "$profile_root/$profile"
done

printf '%s\n' 'Amaazon Hermes project plugin installed for project-manager, coder, reviewer, and the default Desktop profile.'
printf '%s\n' 'Restart Hermes Desktop after installation so the shell hooks register.'
