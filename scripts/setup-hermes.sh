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
profiles="project-manager prototype-coder reviewer-general reviewer-deep reviewer-coordinator refactor-coder"

cd "$root_dir"

install_skill() {
  profile="$1"
  identifier="$2"
  output=$(hermes --profile "$profile" skills install "$identifier" --yes 2>&1) || {
    printf '%s\n' "$output" >&2
    return 1
  }
  if [[ "$output" == *"Error:"* || "$output" == *"Installation blocked:"* ]]; then
    printf '%s\n' "$output" >&2
    return 1
  fi
  if [[ "$output" != *"Installed:"* && "$output" != *"already installed"* ]]; then
    printf '%s\n' "$output" >&2
    return 1
  fi
  printf '%s\n' "skills.sh: $identifier -> $profile"
}

for profile in $profiles; do
  hermes profile install "./.hermes/profile-distributions/$profile" \
    --name "$profile" --alias --force --yes
  hermes --profile "$profile" config set terminal.cwd "$root_dir"
done

install_skill prototype-coder skills-sh/github/awesome-copilot/java-springboot
install_skill prototype-coder skills-sh/github/awesome-copilot/java-junit

install_skill reviewer-general skills-sh/mattpocock/skills/code-review
install_skill reviewer-general skills-sh/alannkl/skills/simplify-code
install_skill reviewer-general skills-sh/toss/es-toolkit/compat-review
install_skill reviewer-deep skills-sh/mattpocock/skills/improve-codebase-architecture
install_skill reviewer-deep skills-sh/getsentry/warden/architecture-review
install_skill reviewer-deep skills-sh/jabrena/plinth/305-frameworks-spring-boot-modulith
install_skill reviewer-deep skills-sh/affaan-m/ecc/jpa-patterns
install_skill refactor-coder skills-sh/github/awesome-copilot/java-refactoring-extract-method
install_skill refactor-coder skills-sh/github/awesome-copilot/java-refactoring-remove-parameter

python scripts/apply-hermes-capabilities.py \
  --policy .hermes/profile-capabilities

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
