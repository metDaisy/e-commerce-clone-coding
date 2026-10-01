#!/usr/bin/env python3
"""Create and validate backend implementation card JSON drafts."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
from pathlib import Path
from typing import Any

from contract_common import is_canonical_https_url
from graph_contract import requirement_diff, validate_graph, validate_native_readback

LEGACY_CARD_SCHEMA = "backend-implementation-card-v2"
CURRENT_CARD_SCHEMA = "backend-implementation-card-v3"
BASE_CONTRACT_DIMENSIONS = (
    "actor_authorization",
    "input_output",
    "state_invariants",
    "error_semantics",
    "api",
    "persistence",
    "transaction_consistency",
    "event",
    "module_boundary",
    "external_system",
)
CONTRACT_DIMENSIONS = BASE_CONTRACT_DIMENSIONS + ("delivery_boundary",)
TEST_LEVELS = {"unit", "slice", "repository", "integration", "modulith"}
FORBIDDEN_FIELDS = {
    "baseline_sha",
    "planning_sha",
    "assignee",
    "status",
    "dependency",
    "dependencies",
    "depends_on",
    "depends_on_card_keys",
    "workspace",
    "run",
    "runs",
    "result",
    "results",
}
FULL_BACKEND_FIELDS = {"runner", "CHECK", "CWD", "EXPECT"}
CARD_FIELDS = {
    "schema",
    "card_type",
    "issue",
    "goal",
    "effective_behavior",
    "scope",
    "out_of_scope",
    "implementation_context",
    "contracts",
    "acceptance_criteria",
    "focused_verification",
    "full_backend_verification",
    "traceability",
}
HANGUL = re.compile(r"[가-힣]")
ALPHABETIC = re.compile(r"[A-Za-z가-힣]")



def _non_empty(value: Any) -> bool:
    return isinstance(value, str) and bool(value.strip())


def _strings(value: Any) -> bool:
    return isinstance(value, list) and bool(value) and all(_non_empty(item) for item in value)


def _error(errors: list[str], condition: bool, code: str) -> None:
    if not condition:
        errors.append(code)


def _unexpected_fields(value: dict[str, Any], allowed: set[str], errors: list[str], prefix: str) -> None:
    for field in sorted(set(value) - allowed):
        errors.append(f"{prefix}:{field}")


def _validate_korean(value: Any, errors: list[str], field: str) -> None:
    if not _non_empty(value):
        return
    hangul_count = len(HANGUL.findall(value))
    alphabetic_count = len(ALPHABETIC.findall(value))
    if hangul_count < 4 or hangul_count * 5 < alphabetic_count:
        errors.append(f"DESCRIPTION_NOT_KOREAN:{field}")


def _validate_korean_strings(value: Any, errors: list[str], field: str) -> None:
    if isinstance(value, list):
        for index, item in enumerate(value):
            _validate_korean(item, errors, f"{field}:{index}")


def _append_forbidden_fields(value: Any, errors: list[str]) -> None:
    if isinstance(value, dict):
        for key, child in value.items():
            if key in FORBIDDEN_FIELDS:
                code = f"FORBIDDEN_FIELD:{key}"
                if code not in errors:
                    errors.append(code)
            _append_forbidden_fields(child, errors)
    elif isinstance(value, list):
        for child in value:
            _append_forbidden_fields(child, errors)


def template(issue: int, issue_url: str) -> dict[str, Any]:
    """Create a fact-only card scaffold; PM authors all behavior and evidence."""
    return {
        "schema": CURRENT_CARD_SCHEMA,
        "card_type": "implementation",
        "issue": {"number": issue, "url": issue_url},
        "goal": "",
        "effective_behavior": [],
        "scope": [],
        "out_of_scope": [],
        "implementation_context": {
            "current_behavior": [],
            "entry_points": [],
            "constraints": [],
        },
        "contracts": {
            field: {"applicable": False, "not_applicable_reason": ""}
            for field in CONTRACT_DIMENSIONS
        },
        "acceptance_criteria": [],
        "focused_verification": [],
        "full_backend_verification": {
            "runner": "gradle-mcp",
            "CHECK": "test",
            "CWD": ".",
            "EXPECT": "BUILD SUCCESSFUL",
        },
        "traceability": [],
    }


def _validate_identified_outcomes(
    entries: Any,
    errors: list[str],
    *,
    missing_code: str,
    item_code: str,
    duplicate_code: str,
) -> set[str]:
    if not isinstance(entries, list) or not entries:
        errors.append(missing_code)
        return set()
    identifiers: set[str] = set()
    for index, entry in enumerate(entries):
        if not isinstance(entry, dict):
            errors.append(f"{item_code}_NOT_OBJECT:{index}")
            continue
        _unexpected_fields(entry, {"id", "outcome"}, errors, f"UNEXPECTED_{item_code}_FIELD:{index}")
        identifier = entry.get("id")
        _error(errors, _non_empty(identifier), f"MISSING_{item_code}_ID:{index}")
        _error(errors, _non_empty(entry.get("outcome")), f"MISSING_{item_code}_OUTCOME:{index}")
        _validate_korean(entry.get("outcome"), errors, f"{item_code.lower()}:{index}:outcome")
        if isinstance(identifier, str) and identifier:
            if identifier in identifiers:
                errors.append(f"{duplicate_code}:{identifier}")
            identifiers.add(identifier)
    return identifiers


def _validate_implementation_context(value: Any, errors: list[str]) -> None:
    if not isinstance(value, dict):
        errors.append("MISSING_IMPLEMENTATION_CONTEXT")
        return
    _unexpected_fields(
        value,
        {"current_behavior", "entry_points", "constraints"},
        errors,
        "UNEXPECTED_CONTEXT_FIELD",
    )
    _error(errors, _strings(value.get("current_behavior")), "MISSING_CURRENT_BEHAVIOR")
    _error(errors, _strings(value.get("constraints")), "MISSING_IMPLEMENTATION_CONSTRAINTS")
    _validate_korean_strings(value.get("current_behavior"), errors, "current_behavior")
    _validate_korean_strings(value.get("constraints"), errors, "constraints")
    entries = value.get("entry_points")
    _error(errors, isinstance(entries, list) and bool(entries), "MISSING_ENTRY_POINTS")
    if isinstance(entries, list):
        for index, entry in enumerate(entries):
            if not isinstance(entry, dict):
                errors.append(f"ENTRY_POINT_NOT_OBJECT:{index}")
                continue
            _unexpected_fields(
                entry,
                {"path", "symbol", "reason"},
                errors,
                f"UNEXPECTED_ENTRY_POINT_FIELD:{index}",
            )
            for field in ("path", "symbol", "reason"):
                _error(errors, _non_empty(entry.get(field)), f"MISSING_ENTRY_POINT_{field.upper()}:{index}")
            _validate_korean(entry.get("reason"), errors, f"entry_points:{index}:reason")


def _validate_delivery_boundary(value: Any, api_applicable: bool, errors: list[str]) -> None:
    if not isinstance(value, dict):
        errors.append("MISSING_CONTRACT:delivery_boundary")
        return
    applicable = value.get("applicable")
    if not isinstance(applicable, bool):
        errors.append("INVALID_CONTRACT_APPLICABILITY:delivery_boundary")
        return
    if applicable:
        fields = {
            "applicable",
            "request_model",
            "binding_and_validation",
            "response_or_result_model",
            "transport_error_mapping",
            "api_acceptance_ids",
            "required_web_scenarios",
        }
        _unexpected_fields(value, fields, errors, "UNEXPECTED_DELIVERY_BOUNDARY_FIELD")
        for field in (
            "request_model",
            "binding_and_validation",
            "response_or_result_model",
            "transport_error_mapping",
        ):
            _error(errors, _non_empty(value.get(field)), f"MISSING_DELIVERY_BOUNDARY_{field.upper()}")
            _validate_korean(value.get(field), errors, f"delivery_boundary:{field}")
        _error(errors, _strings(value.get("required_web_scenarios")), "MISSING_DELIVERY_BOUNDARY_REQUIRED_WEB_SCENARIOS")
        _validate_korean_strings(value.get("required_web_scenarios"), errors, "delivery_boundary:required_web_scenarios")
        if isinstance(value.get("required_web_scenarios"), list) and len(value["required_web_scenarios"]) != len(set(value["required_web_scenarios"])):
            errors.append("DUPLICATE_DELIVERY_BOUNDARY_REQUIRED_WEB_SCENARIO")
        acceptance_ids = value.get("api_acceptance_ids")
        _error(errors, _strings(acceptance_ids), "MISSING_DELIVERY_BOUNDARY_API_ACCEPTANCE_IDS")
        if isinstance(acceptance_ids, list) and all(isinstance(item, str) for item in acceptance_ids) and len(acceptance_ids) != len(set(acceptance_ids)):
            errors.append("DUPLICATE_DELIVERY_BOUNDARY_API_ACCEPTANCE_ID")
    else:
        _unexpected_fields(value, {"applicable", "not_applicable_reason"}, errors, "UNEXPECTED_DELIVERY_BOUNDARY_FIELD")
        _error(errors, _non_empty(value.get("not_applicable_reason")), "MISSING_NOT_APPLICABLE_REASON:delivery_boundary")
        _validate_korean(value.get("not_applicable_reason"), errors, "delivery_boundary:not_applicable_reason")
    if api_applicable != applicable:
        errors.append("API_DELIVERY_BOUNDARY_APPLICABILITY_MISMATCH")


def _validate_contracts(value: Any, schema: Any, errors: list[str]) -> None:
    if not isinstance(value, dict):
        errors.append("MISSING_CONTRACTS")
        return
    dimensions = CONTRACT_DIMENSIONS if schema == CURRENT_CARD_SCHEMA else BASE_CONTRACT_DIMENSIONS
    for field in sorted(set(value) - set(dimensions)):
        errors.append(f"UNEXPECTED_CONTRACT:{field}")
    api_contract = value.get("api")
    api_applicable = isinstance(api_contract, dict) and api_contract.get("applicable") is True
    for field in dimensions:
        contract = value.get(field)
        if field == "delivery_boundary":
            _validate_delivery_boundary(contract, api_applicable, errors)
            continue
        if not isinstance(contract, dict):
            errors.append(f"MISSING_CONTRACT:{field}")
            continue
        _unexpected_fields(
            contract,
            {"applicable", "rules", "not_applicable_reason"},
            errors,
            f"UNEXPECTED_CONTRACT_FIELD:{field}",
        )
        applicable = contract.get("applicable")
        if not isinstance(applicable, bool):
            errors.append(f"INVALID_CONTRACT_APPLICABILITY:{field}")
        elif applicable:
            _error(errors, _strings(contract.get("rules")), f"MISSING_CONTRACT_RULES:{field}")
            _validate_korean_strings(contract.get("rules"), errors, f"contract:{field}")
            _error(errors, "not_applicable_reason" not in contract, f"CONFLICTING_CONTRACT:{field}")
        else:
            _error(
                errors,
                _non_empty(contract.get("not_applicable_reason")),
                f"MISSING_NOT_APPLICABLE_REASON:{field}",
            )
            _validate_korean(
                contract.get("not_applicable_reason"),
                errors,
                f"contracts:{field}:not_applicable_reason",
            )
            _error(errors, "rules" not in contract, f"CONFLICTING_CONTRACT:{field}")


def _validate_delivery_boundary_coverage(
    contracts: Any,
    acceptance_ids: set[str],
    verifications: Any,
    errors: list[str],
) -> None:
    if not isinstance(contracts, dict):
        return
    boundary = contracts.get("delivery_boundary")
    if not isinstance(boundary, dict) or boundary.get("applicable") is not True:
        return
    api_acceptance_ids = boundary.get("api_acceptance_ids")
    if not isinstance(api_acceptance_ids, list):
        return
    for acceptance_id in api_acceptance_ids:
        if acceptance_id not in acceptance_ids:
            errors.append(f"UNKNOWN_DELIVERY_BOUNDARY_ACCEPTANCE_ID:{acceptance_id}")
            continue
        has_web_level = isinstance(verifications, list) and any(
            isinstance(item, dict)
            and acceptance_id in item.get("acceptance_ids", [])
            and isinstance(item.get("test_level"), str)
            and item.get("test_level") in {"slice", "integration"}
            for item in verifications
        )
        _error(errors, has_web_level, f"DELIVERY_BOUNDARY_ACCEPTANCE_WITHOUT_WEB_LEVEL:{acceptance_id}")


def _validate_focused_verification(
    value: Any, acceptance_ids: set[str], errors: list[str]
) -> None:
    if not isinstance(value, list) or not value:
        errors.append("MISSING_FOCUSED_VERIFICATION")
        for acceptance_id in sorted(acceptance_ids):
            errors.append(f"UNCOVERED_ACCEPTANCE:{acceptance_id}")
        return
    verification_ids: set[str] = set()
    covered: set[str] = set()
    for index, verification in enumerate(value):
        if not isinstance(verification, dict):
            errors.append(f"VERIFICATION_NOT_OBJECT:{index}")
            continue
        _unexpected_fields(
            verification,
            {"id", "acceptance_ids", "test_level", "required_scenarios", "runner", "CHECK", "CWD", "EXPECT"},
            errors,
            f"UNEXPECTED_VERIFICATION_FIELD:{index}",
        )
        identifier = verification.get("id")
        label = identifier if _non_empty(identifier) else str(index)
        _error(errors, _non_empty(identifier), f"MISSING_VERIFICATION_ID:{index}")
        if isinstance(identifier, str) and identifier:
            if identifier in verification_ids:
                errors.append(f"DUPLICATE_VERIFICATION_ID:{identifier}")
            verification_ids.add(identifier)
        references = verification.get("acceptance_ids")
        _error(errors, _strings(references), f"MISSING_ACCEPTANCE_IDS:{label}")
        if isinstance(references, list):
            seen_references: set[str] = set()
            for acceptance_id in references:
                if not isinstance(acceptance_id, str):
                    continue
                if acceptance_id in seen_references:
                    errors.append(f"DUPLICATE_ACCEPTANCE_REFERENCE:{label}:{acceptance_id}")
                seen_references.add(acceptance_id)
                if acceptance_id not in acceptance_ids:
                    errors.append(f"UNKNOWN_ACCEPTANCE_ID:{label}:{acceptance_id}")
                else:
                    covered.add(acceptance_id)
        test_level = verification.get("test_level")
        _error(errors, isinstance(test_level, str) and test_level in TEST_LEVELS, f"INVALID_TEST_LEVEL:{label}")
        _error(errors, verification.get("runner") == "gradle-mcp", f"INVALID_VERIFICATION_RUNNER:{label}")
        check = verification.get("CHECK")
        _error(errors, isinstance(check, dict), f"INVALID_VERIFICATION_CHECK:{label}")
        if isinstance(check, dict):
            _unexpected_fields(check, {"task", "tests"}, errors, f"UNEXPECTED_VERIFICATION_CHECK_FIELD:{label}")
            _error(errors, _non_empty(check.get("task")), f"MISSING_VERIFICATION_TASK:{label}")
            tests = check.get("tests")
            _error(errors, _strings(tests), f"MISSING_VERIFICATION_TESTS:{label}")
            if isinstance(tests, list):
                _error(errors, len(tests) == len(set(tests)), f"DUPLICATE_VERIFICATION_TEST:{label}")
                _error(errors, all(re.fullmatch(r"[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)+(?:#[A-Za-z_$][\w$]*)?", test or "") for test in tests), f"INVALID_VERIFICATION_TEST:{label}")
        _error(errors, verification.get("CWD") == ".", f"INVALID_VERIFICATION_CWD:{label}")
        _error(errors, verification.get("EXPECT") == "BUILD SUCCESSFUL", f"INVALID_VERIFICATION_EXPECT:{label}")
        scenarios = verification.get("required_scenarios")
        _error(errors, _strings(scenarios), f"MISSING_REQUIRED_SCENARIOS:{label}")
        _validate_korean_strings(scenarios, errors, f"focused_verification:{label}:required_scenarios")
        if isinstance(scenarios, list) and all(isinstance(item, str) for item in scenarios) and len(scenarios) != len(set(scenarios)):
            errors.append(f"DUPLICATE_REQUIRED_SCENARIO:{label}")
    for acceptance_id in sorted(acceptance_ids - covered):
        errors.append(f"UNCOVERED_ACCEPTANCE:{acceptance_id}")


def _validate_full_backend(value: Any, errors: list[str]) -> None:
    if not isinstance(value, dict):
        errors.append("MISSING_FULL_BACKEND_VERIFICATION")
        return
    for field in sorted(set(value) - FULL_BACKEND_FIELDS):
        errors.append(f"UNEXPECTED_FULL_BACKEND_FIELD:{field}")
    _error(errors, value.get("runner") == "gradle-mcp", "INVALID_FULL_BACKEND_RUNNER")
    _error(errors, value.get("CHECK") == "test", "INVALID_FULL_BACKEND_CHECK")
    _error(errors, value.get("CWD") == ".", "INVALID_FULL_BACKEND_CWD")
    _error(errors, value.get("EXPECT") == "BUILD SUCCESSFUL", "INVALID_FULL_BACKEND_EXPECT")


def _validate_locator(value: Any, errors: list[str], index: int) -> None:
    if not isinstance(value, dict):
        errors.append(f"TRACE_SOURCE_NOT_OBJECT:{index}")
        return
    kind = value.get("kind")
    if kind == "repository":
        _unexpected_fields(
            value,
            {"kind", "path", "heading"},
            errors,
            f"UNEXPECTED_TRACE_SOURCE_FIELD:{index}",
        )
        _error(errors, _non_empty(value.get("path")), f"MISSING_TRACE_PATH:{index}")
        _error(errors, _non_empty(value.get("heading")), f"MISSING_TRACE_HEADING:{index}")
    else:
        errors.append(f"INVALID_TRACE_KIND:{index}")


def _validate_traceability(value: Any, errors: list[str]) -> None:
    if not isinstance(value, list) or not value:
        errors.append("MISSING_TRACEABILITY")
        return
    for index, entry in enumerate(value):
        if not isinstance(entry, dict):
            errors.append(f"TRACE_NOT_OBJECT:{index}")
            continue
        _unexpected_fields(
            entry,
            {"role", "supports", "source"},
            errors,
            f"UNEXPECTED_TRACE_FIELD:{index}",
        )
        _error(errors, _non_empty(entry.get("role")), f"MISSING_TRACE_ROLE:{index}")
        _error(errors, _non_empty(entry.get("supports")), f"MISSING_TRACE_SUPPORTS:{index}")
        _validate_korean(entry.get("supports"), errors, f"traceability:{index}:supports")
        _validate_locator(entry.get("source"), errors, index)


def validate(card: Any) -> list[str]:
    """Return deterministic card contract failures without mutating Git or Kanban."""
    errors: list[str] = []
    if not isinstance(card, dict):
        return ["CARD_NOT_OBJECT"]
    _append_forbidden_fields(card, errors)
    for field in sorted(set(card) - CARD_FIELDS):
        errors.append(f"UNEXPECTED_CARD_FIELD:{field}")
    schema = card.get("schema")
    _error(errors, schema in {LEGACY_CARD_SCHEMA, CURRENT_CARD_SCHEMA}, "SCHEMA_VERSION")
    _error(errors, card.get("card_type") == "implementation", "INVALID_CARD_TYPE")
    issue = card.get("issue")
    if not isinstance(issue, dict):
        errors.append("INVALID_ISSUE")
    else:
        _unexpected_fields(issue, {"number", "url"}, errors, "UNEXPECTED_ISSUE_FIELD")
        issue_number = issue.get("number")
        _error(
            errors,
            isinstance(issue_number, int) and not isinstance(issue_number, bool) and issue_number > 0,
            "INVALID_ISSUE_NUMBER",
        )
        issue_url = issue.get("url")
        _error(
            errors,
            is_canonical_https_url(issue_url),
            "INVALID_ISSUE_URL",
        )
    _error(errors, _non_empty(card.get("goal")), "MISSING_GOAL")
    _validate_korean(card.get("goal"), errors, "goal")
    _validate_identified_outcomes(
        card.get("effective_behavior"),
        errors,
        missing_code="MISSING_EFFECTIVE_BEHAVIOR",
        item_code="BEHAVIOR",
        duplicate_code="DUPLICATE_BEHAVIOR_ID",
    )
    _error(errors, _strings(card.get("scope")), "MISSING_SCOPE")
    _error(errors, _strings(card.get("out_of_scope")), "MISSING_OUT_OF_SCOPE")
    _validate_korean_strings(card.get("scope"), errors, "scope")
    _validate_korean_strings(card.get("out_of_scope"), errors, "out_of_scope")
    _validate_implementation_context(card.get("implementation_context"), errors)
    _validate_contracts(card.get("contracts"), schema, errors)
    acceptance_ids = _validate_identified_outcomes(
        card.get("acceptance_criteria"),
        errors,
        missing_code="MISSING_ACCEPTANCE_CRITERIA",
        item_code="ACCEPTANCE",
        duplicate_code="DUPLICATE_ACCEPTANCE_ID",
    )
    if schema == CURRENT_CARD_SCHEMA:
        _validate_delivery_boundary_coverage(card.get("contracts"), acceptance_ids, card.get("focused_verification"), errors)
    _validate_focused_verification(card.get("focused_verification"), acceptance_ids, errors)
    _validate_full_backend(card.get("full_backend_verification"), errors)
    _validate_traceability(card.get("traceability"), errors)
    return errors


def _temp_path(path: Path) -> Path:
    if path.is_absolute():
        raise ValueError("output path must be relative to this project's .temp directory")
    temp_root = (Path.cwd() / ".temp").resolve()
    destination = (Path.cwd() / path).resolve()
    if destination == temp_root or temp_root not in destination.parents:
        raise ValueError("output path must be relative to this project's .temp directory")
    return destination


def write_json(path: Path, payload: Any) -> None:
    destination = _temp_path(path)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def _read_json(path: Path) -> Any:
    def reject_duplicate_keys(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
        value: dict[str, Any] = {}
        for key, item in pairs:
            if key in value:
                raise ValueError(f"DUPLICATE_JSON_KEY:{key}")
            value[key] = item
        return value

    return json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=reject_duplicate_keys)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)
    template_parser = subparsers.add_parser("template", help="write a backend implementation card draft")
    template_parser.add_argument("--issue", required=True, type=int)
    template_parser.add_argument("--issue-url", required=True)
    template_parser.add_argument("--output", required=True, type=Path)
    validate_parser = subparsers.add_parser("validate", help="validate a backend implementation card")
    validate_parser.add_argument("card", type=Path)
    graph_parser = subparsers.add_parser("validate-graph", help="validate a task graph wrapper")
    graph_parser.add_argument("graph", type=Path)
    graph_parser.add_argument("--phase", choices=("draft", "native"), default="draft")
    graph_parser.add_argument("--source-review-result", type=Path)
    native_parser = subparsers.add_parser("validate-native-readback", help="compare graph wrapper with full Kanban show read-back")
    native_parser.add_argument("graph", type=Path)
    native_parser.add_argument("readback", type=Path)
    native_parser.add_argument("--source-review-result", type=Path)
    diff_parser = subparsers.add_parser("requirement-diff", help="write a read-only requirement comparison")
    diff_parser.add_argument("--base", required=True)
    diff_parser.add_argument("--revised", required=True)
    diff_parser.add_argument("--path", action="append", required=True, dest="paths")
    diff_parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()

    if args.command == "template":
        write_json(args.output, template(args.issue, args.issue_url))
        return 0
    if args.command == "requirement-diff":
        try:
            write_json(args.output, requirement_diff(Path.cwd(), args.base, args.revised, args.paths))
        except (OSError, subprocess.CalledProcessError, ValueError) as exc:
            print(json.dumps({"valid": False, "errors": [f"REQUIREMENT_DIFF_FAILED:{exc}"]}, ensure_ascii=False))
            return 2
        return 0
    source = args.graph if args.command in {"validate-graph", "validate-native-readback"} else args.card
    try:
        payload = _read_json(source)
    except (OSError, json.JSONDecodeError, TypeError, ValueError) as exc:
        print(json.dumps({"valid": False, "errors": [f"READ_INPUT_FAILED:{exc}"]}, ensure_ascii=False))
        return 2
    source_review_result = None
    if args.command in {"validate-graph", "validate-native-readback"} and args.source_review_result is not None:
        try:
            source_review_result = _read_json(args.source_review_result)
        except (OSError, json.JSONDecodeError, TypeError, ValueError) as exc:
            print(json.dumps({"valid": False, "errors": [f"READ_INPUT_FAILED:{exc}"]}, ensure_ascii=False))
            return 2
    if args.command == "validate-native-readback":
        try:
            readback = _read_json(args.readback)
        except (OSError, json.JSONDecodeError, TypeError, ValueError) as exc:
            print(json.dumps({"valid": False, "errors": [f"READ_INPUT_FAILED:{exc}"]}, ensure_ascii=False))
            return 2
        errors = validate_graph(payload, phase="native", validate_implementation=validate, source_review_result=source_review_result)
        errors.extend(validate_native_readback(payload, readback))
    elif args.command == "validate-graph":
        errors = validate_graph(payload, phase=args.phase, validate_implementation=validate, source_review_result=source_review_result)
    else:
        errors = validate(payload)
    print(json.dumps({"valid": not errors, "errors": errors}, ensure_ascii=False))
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
