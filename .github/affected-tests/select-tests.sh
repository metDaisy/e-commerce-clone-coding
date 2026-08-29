#!/usr/bin/env bash

set -euo pipefail

: "${JAVA_BASE_PACKAGE:?JAVA_BASE_PACKAGE is required}"

emit_full_suite() {
  local base="$JAVA_BASE_PACKAGE"
  echo "auth_patterns=[\"$base.auth.*\"]" >> "$GITHUB_OUTPUT"
  echo "user_patterns=[\"$base.user.*\"]" >> "$GITHUB_OUTPUT"
  echo "address_patterns=[\"$base.address.*\"]" >> "$GITHUB_OUTPUT"
  echo "catalog_patterns=[\"$base.catalog.*\"]" >> "$GITHUB_OUTPUT"
  echo "seller_patterns=[\"$base.seller.*\"]" >> "$GITHUB_OUTPUT"
  echo "global_patterns=[\"$base.global.*\"]" >> "$GITHUB_OUTPUT"
  echo "application_patterns=[\"$base.AmaazonApplicationTests\"]" >> "$GITHUB_OUTPUT"
  echo "modularity_patterns=[\"$base.ModularityTest\"]" >> "$GITHUB_OUTPUT"
}

emit_skipped() {
  for domain in auth user address catalog seller global application modularity; do
    echo "${domain}_patterns=[]" >> "$GITHUB_OUTPUT"
  done
}

if [[ "${FULL_SUITE:-false}" == 'true' ]]; then
  emit_full_suite
  exit 0
fi

: "${BASE_SHA:?BASE_SHA is required}"

set -o pipefail
./gradlew \
  -I .github/affected-tests/init.gradle \
  affectedTest \
  --explain \
  --explain-format=json \
  -PaffectedTestsBaseRef="$BASE_SHA" \
  --console=plain 2>&1 | tee affected-tests.log

selection_json="$(sed -n '/^{.*}$/p' affected-tests.log | tail -n 1)"
if [[ -z "$selection_json" ]]; then
  echo 'Could not find affected-tests JSON output.' >&2
  exit 1
fi
printf '%s\n' "$selection_json" | jq -e . > affected-tests.json

outcome="$(jq -r '.outcome.kind // .action.name // empty' affected-tests.json)"
if [[ "$outcome" == 'FULL_SUITE' ]]; then
  emit_full_suite
  exit 0
fi

if [[ "$outcome" == 'SKIPPED' ]]; then
  emit_skipped
  exit 0
fi

if [[ "$outcome" != 'SELECTED' ]]; then
  echo "Unsupported affected-tests outcome: $outcome" >&2
  exit 1
fi

selected_count="$(jq -r '.outcome.selectedClassCount // 0' affected-tests.json)"
if ! [[ "$selected_count" =~ ^[0-9]+$ ]] || (( selected_count == 0 )); then
  echo 'Affected-tests returned SELECTED without any test classes; escalating to the full suite.' >&2
  emit_full_suite
  exit 0
fi

unrouted_classes="$(jq -c --arg base "$JAVA_BASE_PACKAGE" '
  [.modules[]?.testClasses[]?
    | select(
        (startswith($base + ".auth.") or
         startswith($base + ".user.") or
         startswith($base + ".address.") or
         startswith($base + ".catalog.") or
         startswith($base + ".seller.") or
         startswith($base + ".global.") or
         . == ($base + ".AmaazonApplicationTests") or
         . == ($base + ".ModularityTest"))
        | not)]
' affected-tests.json)"
if [[ "$unrouted_classes" != '[]' ]]; then
  echo "Affected tests outside the CI routing map; escalating to the full suite: $unrouted_classes" >&2
  emit_full_suite
  exit 0
fi

for domain in auth user address catalog seller global; do
  patterns="$(jq -c --arg prefix "$JAVA_BASE_PACKAGE.$domain." \
    '[.modules[]?.testClasses[]? | select(startswith($prefix))] | unique' \
    affected-tests.json)"
  echo "${domain}_patterns=$patterns" >> "$GITHUB_OUTPUT"
done

echo "application_patterns=$(jq -c --arg test "$JAVA_BASE_PACKAGE.AmaazonApplicationTests" '[.modules[]?.testClasses[]? | select(. == $test)] | unique' affected-tests.json)" >> "$GITHUB_OUTPUT"
echo "modularity_patterns=$(jq -c --arg test "$JAVA_BASE_PACKAGE.ModularityTest" '[.modules[]?.testClasses[]? | select(. == $test)] | unique' affected-tests.json)" >> "$GITHUB_OUTPUT"
