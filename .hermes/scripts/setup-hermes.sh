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

root_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
profiles="project-manager prototype-coder implementation-coder reviewer-general reviewer-deep reviewer-coordinator refactor-coder"

cd "$root_dir"

for profile in $profiles; do
  hermes profile install "./.hermes/profile-distributions/$profile" \
    --name "$profile" --alias --force --yes
  hermes --profile "$profile" config set terminal.cwd "$root_dir"
  hermes --profile "$profile" config set kanban.auto_decompose false
  if [ "$profile" = "project-manager" ]; then
    hermes --profile "$profile" config set skills.external_dirs '[]'
  fi
done

python .hermes/scripts/apply-hermes-capabilities.py \
  --policy .hermes/profile-capabilities \
  --project-root "$root_dir"

if [ -n "${HERMES_MODEL:-}" ]; then
  provider="${HERMES_PROVIDER:-custom}"
  for profile in $profiles; do
    hermes --profile "$profile" config set model.provider "$provider"
    hermes --profile "$profile" config set model.default "$HERMES_MODEL"
    if [ -n "${HERMES_BASE_URL:-}" ]; then
      hermes --profile "$profile" config set model.base_url "$HERMES_BASE_URL"
    fi
  done
else
  printf '%s\n' 'Model route not configured. Run hermes model for each Profile, or set HERMES_MODEL (and optionally HERMES_PROVIDER/HERMES_BASE_URL) before rerunning.'
fi

printf '%s\n' 'Amaazon Hermes Profile Distributions installed.'
