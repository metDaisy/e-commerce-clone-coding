#!/usr/bin/env bash
set -eu

if ! command -v hermes >/dev/null 2>&1; then
  printf '%s\n' 'Hermes CLI is required: https://hermes-agent.nousresearch.com/docs' >&2
  exit 1
fi
if ! command -v python >/dev/null 2>&1; then
  printf '%s\n' 'Python is required to apply the YAML capability policy.' >&2
  exit 1
fi

root_dir=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && { pwd -W 2>/dev/null || pwd; })
profiles="project-manager:project-manager coder:coder reviewer:reviewer"

cd "$root_dir"

for item in $profiles; do
  profile=${item%%:*}
  source=${item#*:}
  hermes profile install "./.hermes/profiles/$source" \
    --name "$profile" --alias --force --yes
  hermes --profile "$profile" config set terminal.cwd "$root_dir"
  hermes --profile "$profile" skills trust "$root_dir"
  hermes --profile "$profile" config set kanban.auto_decompose false
  if [ "$profile" = "reviewer" ]; then
    hermes --profile "$profile" skills reset hermes-agent --restore --yes
  fi
  if [ "$profile" = "project-manager" ]; then
    hermes --profile "$profile" config set skills.external_dirs '[]'
  fi
done

python .hermes/scripts/apply-hermes-capabilities.py \
  --policy .hermes/profiles \
  --profile project-manager \
  --profile coder \
  --profile reviewer \
  --project-root "$root_dir"

if [ -n "${HERMES_MODEL:-}" ]; then
  provider="${HERMES_PROVIDER:-custom}"
  for item in $profiles; do
    profile=${item%%:*}
    hermes --profile "$profile" config set model.provider "$provider"
    hermes --profile "$profile" config set model.default "$HERMES_MODEL"
    if [ -n "${HERMES_BASE_URL:-}" ]; then
      hermes --profile "$profile" config set model.base_url "$HERMES_BASE_URL"
    fi
  done
else
  printf '%s\n' 'Model route not configured. Run hermes model for each Profile, or set HERMES_MODEL (and optionally HERMES_PROVIDER/HERMES_BASE_URL) before rerunning.'
fi

printf '%s\n' 'Amaazon Hermes Profiles installed.'
