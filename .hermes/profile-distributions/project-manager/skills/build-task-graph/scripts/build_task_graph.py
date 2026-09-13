#!/usr/bin/env python3
"""Generate and validate v0.1 Project Manager task-graph JSON drafts."""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any

CARD_KEY = re.compile(r"^[a-z0-9][a-z0-9-]*$")
SHA = re.compile(r"^[0-9a-f]{40}$")
EXECUTORS = {"gradle-mcp", "npm", "terminal", "browser"}
CHECK_KINDS = {"behavioral", "structural", "style", "state"}
APPLICABILITY_FIELDS = (
    "actor_authorization",
    "field_semantics",
    "state_invariants",
    "error_contract",
    "api_contract",
    "persistence",
    "ui_flow",
    "module_boundary",
)


def _non_empty(value: Any) -> bool:
    return isinstance(value, str) and bool(value.strip())


def _strings(value: Any) -> bool:
    return isinstance(value, list) and bool(value) and all(_non_empty(item) for item in value)


def _error(errors: list[str], condition: bool, code: str) -> None:
    if not condition:
        errors.append(code)


def template(issue: int, title: str, issue_url: str, planning_sha: str, current_state_sha: str) -> dict[str, Any]:
    """Create a fact-only graph scaffold; PM fills cards from approved inputs."""
    return {
        "schema": "build-task-graph-draft-v4",
        "mode": "new-delivery",
        "issue": {"number": issue, "title": title, "url": issue_url},
        "planning": {
            "baseline_sha": planning_sha,
            "current_state_snapshot_sha": current_state_sha,
            "state_freshness": "fresh",
        },
        "cards": [],
    }


def _validate_locator(locator: Any, errors: list[str], prefix: str) -> None:
    if not isinstance(locator, dict):
        errors.append(f"{prefix}:NOT_OBJECT")
        return
    kind = locator.get("kind")
    if kind == "repository":
        _error(errors, _non_empty(locator.get("path")), f"{prefix}:MISSING_PATH")
        heading = locator.get("heading")
        lines = locator.get("lines")
        _error(errors, _non_empty(heading) != _non_empty(lines), f"{prefix}:LOCATOR_REGION")
    elif kind == "issue":
        _error(errors, _non_empty(locator.get("url")), f"{prefix}:MISSING_URL")
        _error(errors, _non_empty(locator.get("heading")), f"{prefix}:MISSING_HEADING")
    elif kind == "task":
        _error(errors, _non_empty(locator.get("task_id")), f"{prefix}:MISSING_TASK_ID")
    else:
        errors.append(f"{prefix}:INVALID_KIND")


def _validate_applicability(value: Any, errors: list[str], field: str) -> None:
    if not isinstance(value, dict) or not isinstance(value.get("applicable"), bool):
        errors.append(f"INVALID_APPLICABILITY:{field}")
        return
    if value["applicable"]:
        _error(errors, _strings(value.get("details")), f"MISSING_DETAILS:{field}")
        _error(errors, "not_applicable_reason" not in value, f"CONFLICTING_APPLICABILITY:{field}")
    else:
        _error(errors, _non_empty(value.get("not_applicable_reason")), f"MISSING_NOT_APPLICABLE_REASON:{field}")
        _error(errors, "details" not in value, f"CONFLICTING_APPLICABILITY:{field}")


def _validate_common(body: Any, draft: dict[str, Any], errors: list[str], card_key: str, expected_assignee: Any) -> None:
    prefix = f"CARD:{card_key}"
    if not isinstance(body, dict):
        errors.append(f"{prefix}:BODY_NOT_OBJECT")
        return
    _error(errors, body.get("schema") == "build-task-card-v4", f"{prefix}:SCHEMA")
    _error(errors, body.get("assignee") == expected_assignee, f"{prefix}:ASSIGNEE_MISMATCH")
    issue = body.get("issue")
    _error(errors, issue == draft.get("issue"), f"{prefix}:ISSUE_MISMATCH")
    planning = body.get("planning")
    _error(errors, planning == draft.get("planning"), f"{prefix}:PLANNING_MISMATCH")
    _error(errors, _strings(body.get("scope")), f"{prefix}:MISSING_SCOPE")
    _error(errors, _strings(body.get("out_of_scope")), f"{prefix}:MISSING_OUT_OF_SCOPE")
    evidence = body.get("evidence")
    _error(errors, isinstance(evidence, list) and bool(evidence), f"{prefix}:MISSING_EVIDENCE")
    if isinstance(evidence, list):
        roles: set[str] = set()
        for index, entry in enumerate(evidence):
            if not isinstance(entry, dict):
                errors.append(f"{prefix}:EVIDENCE_NOT_OBJECT:{index}")
                continue
            role = entry.get("role")
            _error(errors, role in {"goal", "state", "constraint"}, f"{prefix}:INVALID_EVIDENCE_ROLE:{index}")
            if isinstance(role, str):
                roles.add(role)
            _error(errors, _non_empty(entry.get("supports")), f"{prefix}:MISSING_EVIDENCE_SUPPORT:{index}")
            _validate_locator(entry.get("source"), errors, f"{prefix}:LOCATOR:{index}")
        _error(errors, "goal" in roles, f"{prefix}:MISSING_GOAL_EVIDENCE")
        _error(errors, "state" in roles, f"{prefix}:MISSING_STATE_EVIDENCE")


def _validate_verification(body: dict[str, Any], errors: list[str], card_key: str) -> None:
    prefix = f"CARD:{card_key}"
    checks = body.get("verification")
    _error(errors, isinstance(checks, list) and bool(checks), f"{prefix}:MISSING_VERIFICATION")
    check_kinds: dict[str, str] = {}
    if isinstance(checks, list):
        for index, check in enumerate(checks):
            if not isinstance(check, dict):
                errors.append(f"{prefix}:CHECK_NOT_OBJECT:{index}")
                continue
            check_id = check.get("id")
            _error(errors, _non_empty(check_id), f"{prefix}:MISSING_CHECK_ID:{index}")
            if isinstance(check_id, str):
                if check_id in check_kinds:
                    errors.append(f"{prefix}:DUPLICATE_CHECK_ID:{check_id}")
                check_kinds[check_id] = str(check.get("kind"))
            _error(errors, check.get("kind") in CHECK_KINDS, f"{prefix}:INVALID_CHECK_KIND:{index}")
            execution = check.get("execution")
            if not isinstance(execution, dict):
                errors.append(f"{prefix}:INVALID_EXECUTION:{index}")
            else:
                _error(errors, execution.get("executor") in EXECUTORS, f"{prefix}:INVALID_EXECUTOR:{index}")
                for field in ("operation", "target", "cwd"):
                    _error(errors, _non_empty(execution.get(field)), f"{prefix}:MISSING_EXECUTION_{field.upper()}:{index}")
                _error(errors, isinstance(execution.get("request"), dict), f"{prefix}:INVALID_EXECUTION_REQUEST:{index}")
            expectation = check.get("expectation")
            if not isinstance(expectation, dict):
                errors.append(f"{prefix}:INVALID_EXPECTATION:{index}")
            else:
                _error(errors, _non_empty(expectation.get("success_condition")), f"{prefix}:MISSING_SUCCESS_CONDITION:{index}")
                _error(errors, _non_empty(expectation.get("evidence")), f"{prefix}:MISSING_EXPECTATION_EVIDENCE:{index}")

    criteria = body.get("acceptance_criteria")
    _error(errors, isinstance(criteria, list) and bool(criteria), f"{prefix}:MISSING_ACCEPTANCE")
    acceptance_ids: set[str] = set()
    if isinstance(criteria, list):
        for index, criterion in enumerate(criteria):
            if not isinstance(criterion, dict):
                errors.append(f"{prefix}:AC_NOT_OBJECT:{index}")
                continue
            acceptance_id = criterion.get("id")
            _error(errors, _non_empty(acceptance_id), f"{prefix}:MISSING_AC_ID:{index}")
            if isinstance(acceptance_id, str):
                if acceptance_id in acceptance_ids:
                    errors.append(f"{prefix}:DUPLICATE_AC_ID:{acceptance_id}")
                acceptance_ids.add(acceptance_id)
            _error(errors, _non_empty(criterion.get("outcome")), f"{prefix}:MISSING_AC_OUTCOME:{index}")

    coverage = body.get("acceptance_coverage")
    _error(errors, isinstance(coverage, list), f"{prefix}:INVALID_ACCEPTANCE_COVERAGE")
    covered: set[str] = set()
    if isinstance(coverage, list):
        for index, entry in enumerate(coverage):
            if not isinstance(entry, dict):
                errors.append(f"{prefix}:COVERAGE_NOT_OBJECT:{index}")
                continue
            acceptance_id = entry.get("acceptance_id")
            _error(errors, acceptance_id in acceptance_ids, f"{prefix}:UNKNOWN_COVERAGE_AC:{index}")
            if isinstance(acceptance_id, str):
                covered.add(acceptance_id)
            check_ids = entry.get("check_ids")
            _error(errors, _strings(check_ids), f"{prefix}:MISSING_COVERAGE_CHECKS:{index}")
            if isinstance(check_ids, list):
                for check_id in check_ids:
                    _error(errors, check_id in check_kinds, f"{prefix}:UNKNOWN_COVERAGE_CHECK:{check_id}")
                    _error(errors, check_kinds.get(check_id) == "behavioral", f"{prefix}:NON_BEHAVIORAL_COVERAGE:{check_id}")
    for acceptance_id in acceptance_ids:
        _error(errors, acceptance_id in covered, f"{prefix}:UNCOVERED_ACCEPTANCE:{acceptance_id}")

    manual = body.get("manual_verification", [])
    _error(errors, isinstance(manual, list), f"{prefix}:INVALID_MANUAL_VERIFICATION")
    if isinstance(manual, list):
        for index, entry in enumerate(manual):
            if not isinstance(entry, dict):
                errors.append(f"{prefix}:MANUAL_NOT_OBJECT:{index}")
                continue
            _error(errors, _non_empty(entry.get("id")), f"{prefix}:MISSING_MANUAL_ID:{index}")
            _error(errors, _strings(entry.get("procedure")), f"{prefix}:MISSING_MANUAL_PROCEDURE:{index}")
            _error(errors, _non_empty(entry.get("expected_result")), f"{prefix}:MISSING_MANUAL_EXPECTATION:{index}")
            _error(errors, _non_empty(entry.get("deferral_reason")), f"{prefix}:MISSING_MANUAL_DEFERRAL:{index}")


def _validate_child(card: dict[str, Any], draft: dict[str, Any], errors: list[str]) -> None:
    key = str(card.get("card_key"))
    body = card.get("body")
    _validate_common(body, draft, errors, key, card.get("assignee"))
    if not isinstance(body, dict):
        return
    _error(errors, body.get("card_type") == "implementation", f"CARD:{key}:INVALID_CARD_TYPE")
    implementation = body.get("implementation")
    if not isinstance(implementation, dict):
        errors.append(f"CARD:{key}:INVALID_IMPLEMENTATION")
    else:
        _error(errors, _strings(implementation.get("observable_behavior")), f"CARD:{key}:MISSING_OBSERVABLE_BEHAVIOR")
        for field in APPLICABILITY_FIELDS:
            _validate_applicability(implementation.get(field), errors, f"{key}:{field}")
    _validate_verification(body, errors, key)
    handoff = body.get("review_handoff_contract")
    _error(errors, isinstance(handoff, dict), f"CARD:{key}:INVALID_HANDOFF")
    if isinstance(handoff, dict):
        _error(errors, handoff.get("action") == "request-review", f"CARD:{key}:INVALID_HANDOFF_ACTION")
        _error(errors, handoff.get("reviewer") == "project-manager", f"CARD:{key}:INVALID_HANDOFF_REVIEWER")
        _error(errors, set(handoff.get("required_metadata", [])) == {"verified_sha", "check_results", "changed_paths", "residual_risk"}, f"CARD:{key}:INVALID_HANDOFF_METADATA")
        _error(errors, set(handoff.get("reviewer_readback", [])) == {"show", "runs", "comments"}, f"CARD:{key}:INVALID_HANDOFF_READBACK")


def _validate_root(card: dict[str, Any], draft: dict[str, Any], child_keys: set[str], errors: list[str]) -> None:
    key = str(card.get("card_key"))
    body = card.get("body")
    _validate_common(body, draft, errors, key, card.get("assignee"))
    if not isinstance(body, dict):
        return
    _error(errors, body.get("card_type") == "root-review", f"CARD:{key}:INVALID_CARD_TYPE")
    targets = body.get("target_children")
    _error(errors, isinstance(targets, list) and set(targets) == child_keys and len(targets) == len(child_keys), f"CARD:{key}:INVALID_TARGET_CHILDREN")
    for field in ("aggregate_contract", "cross_boundary_invariants", "aggregate_exclusions", "child_evidence_readback", "review_questions"):
        _error(errors, _strings(body.get(field)), f"CARD:{key}:MISSING_{field.upper()}")
    _error(errors, body.get("verdict_protocol") == ["approved", "changes-requested", "needs-input"], f"CARD:{key}:INVALID_VERDICT_PROTOCOL")


def _has_cycle(dependencies: dict[str, list[str]]) -> bool:
    visiting: set[str] = set()
    visited: set[str] = set()

    def visit(key: str) -> bool:
        if key in visiting:
            return True
        if key in visited:
            return False
        visiting.add(key)
        if any(visit(parent) for parent in dependencies[key] if parent in dependencies):
            return True
        visiting.remove(key)
        visited.add(key)
        return False

    return any(visit(key) for key in dependencies)


def validate(draft: Any) -> list[str]:
    """Return deterministic contract failures without mutating Git or Kanban."""
    errors: list[str] = []
    if not isinstance(draft, dict):
        return ["DRAFT_NOT_OBJECT"]
    _error(errors, draft.get("schema") == "build-task-graph-draft-v4", "SCHEMA_VERSION")
    _error(errors, draft.get("mode") == "new-delivery", "UNSUPPORTED_MODE")
    issue = draft.get("issue")
    _error(errors, isinstance(issue, dict), "INVALID_ISSUE")
    if isinstance(issue, dict):
        _error(errors, isinstance(issue.get("number"), int) and issue["number"] > 0, "INVALID_ISSUE_NUMBER")
        _error(errors, _non_empty(issue.get("title")), "MISSING_ISSUE_TITLE")
        _error(errors, _non_empty(issue.get("url")), "MISSING_ISSUE_URL")
    planning = draft.get("planning")
    _error(errors, isinstance(planning, dict), "INVALID_PLANNING")
    if isinstance(planning, dict):
        _error(errors, isinstance(planning.get("baseline_sha"), str) and SHA.fullmatch(planning["baseline_sha"]) is not None, "INVALID_BASELINE_SHA")
        _error(errors, isinstance(planning.get("current_state_snapshot_sha"), str) and SHA.fullmatch(planning["current_state_snapshot_sha"]) is not None, "INVALID_CURRENT_STATE_SHA")
        _error(errors, planning.get("state_freshness") == "fresh", "CURRENT_STATE_NOT_FRESH")

    cards = draft.get("cards")
    _error(errors, isinstance(cards, list) and bool(cards), "MISSING_CARDS")
    if not isinstance(cards, list):
        return errors
    keys: set[str] = set()
    normalized: list[dict[str, Any]] = []
    for index, card in enumerate(cards):
        if not isinstance(card, dict):
            errors.append(f"CARD_NOT_OBJECT:{index}")
            continue
        normalized.append(card)
        key = card.get("card_key")
        _error(errors, isinstance(key, str) and CARD_KEY.fullmatch(key) is not None, f"INVALID_CARD_KEY:{index}")
        if isinstance(key, str):
            if key in keys:
                errors.append(f"DUPLICATE_CARD_KEY:{key}")
            keys.add(key)
        _error(errors, _non_empty(card.get("title")), f"MISSING_CARD_TITLE:{index}")
        _error(errors, _non_empty(card.get("assignee")), f"MISSING_CARD_ASSIGNEE:{index}")
        _error(errors, isinstance(card.get("depends_on_card_keys"), list), f"INVALID_DEPENDENCIES:{index}")

    children = [card for card in normalized if isinstance(card.get("body"), dict) and card["body"].get("card_type") == "implementation"]
    roots = [card for card in normalized if isinstance(card.get("body"), dict) and card["body"].get("card_type") == "root-review"]
    _error(errors, bool(children), "MISSING_IMPLEMENTATION_CHILD")
    _error(errors, len(roots) == 1, "ROOT_COUNT_INVALID")
    child_keys = {str(card.get("card_key")) for card in children}
    dependencies: dict[str, list[str]] = {}
    for card in normalized:
        key = str(card.get("card_key"))
        raw_dependencies = card.get("depends_on_card_keys", [])
        deps = raw_dependencies if isinstance(raw_dependencies, list) else []
        dependencies[key] = [dependency for dependency in deps if isinstance(dependency, str)]
        for dependency in deps:
            _error(errors, dependency in keys, f"UNKNOWN_DEPENDENCY:{key}:{dependency}")

        if isinstance(card.get("body"), dict) and card["body"].get("card_type") == "implementation":
            _error(errors, card.get("assignee") == "implementation-coder", f"INVALID_CHILD_ASSIGNEE:{key}")
            _validate_child(card, draft, errors)
            _error(errors, all(dependency in child_keys for dependency in deps), f"CHILD_DEPENDS_ON_NON_CHILD:{key}")
        elif isinstance(card.get("body"), dict) and card["body"].get("card_type") == "root-review":
            _error(errors, card.get("assignee") == "reviewer-general", f"INVALID_ROOT_ASSIGNEE:{key}")
            _validate_root(card, draft, child_keys, errors)
            _error(errors, set(deps) == child_keys and len(deps) == len(child_keys), f"ROOT_DEPENDENCIES_INVALID:{key}")
        else:
            errors.append(f"UNKNOWN_CARD_TYPE:{key}")

    _error(errors, not _has_cycle(dependencies), "DEPENDENCY_CYCLE")
    eligible_children = [card for card in children if not card.get("depends_on_card_keys")]
    _error(errors, len(eligible_children) == 1, "ELIGIBLE_CHILD_COUNT_INVALID")
    return errors


def _temp_path(path: Path) -> Path:
    if path.is_absolute() or not path.parts or path.parts[0] != ".temp":
        raise ValueError("output path must be relative to this project's .temp directory")
    return (Path.cwd() / path).resolve()


def write_json(path: Path, payload: Any) -> None:
    destination = _temp_path(path)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)
    template_parser = subparsers.add_parser("template", help="write a new-delivery graph draft")
    template_parser.add_argument("--issue", required=True, type=int)
    template_parser.add_argument("--title", required=True)
    template_parser.add_argument("--issue-url", required=True)
    template_parser.add_argument("--planning-sha", required=True)
    template_parser.add_argument("--current-state-sha", required=True)
    template_parser.add_argument("--output", required=True, type=Path)
    validate_parser = subparsers.add_parser("validate", help="validate a new-delivery graph draft")
    validate_parser.add_argument("draft", type=Path)
    args = parser.parse_args()

    if args.command == "template":
        write_json(args.output, template(args.issue, args.title, args.issue_url, args.planning_sha, args.current_state_sha))
        return 0
    try:
        draft = json.loads(args.draft.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        print(json.dumps({"valid": False, "errors": [f"READ_DRAFT_FAILED:{exc}"]}, ensure_ascii=False))
        return 2
    errors = validate(draft)
    print(json.dumps({"valid": not errors, "errors": errors}, ensure_ascii=False))
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
