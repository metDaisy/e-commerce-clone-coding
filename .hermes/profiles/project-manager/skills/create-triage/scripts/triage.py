#!/usr/bin/env python3
"""Create and validate JSON contracts for Project Manager triage cards."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path
from typing import Any

DOCUMENTS = ("requirement", "architecture", "ADR", "glossary", "ERD", "index")
DECISIONS = {"update", "no-change", "not-applicable", "blocked"}
STATUSES = {"triage", "todo", "ready", "running", "blocked", "done"}
PLANNING_STATES = {"planning", "frozen"}
FINDING_STATUSES = {"open", "resolved"}
DECISION_STATUSES = {"pending", "approved", "rejected", "deferred"}
SHA = re.compile(r"^[0-9a-f]{40}$")
TASK_ID = re.compile(r"^t_[0-9a-f]+$")


def _non_empty_string(value: Any) -> bool:
    return isinstance(value, str) and bool(value.strip())


def _string_list(value: Any) -> bool:
    return isinstance(value, list) and bool(value) and all(_non_empty_string(item) for item in value)


def _optional_string_list(value: Any) -> bool:
    return isinstance(value, list) and all(_non_empty_string(item) for item in value)


def _error(errors: list[str], condition: bool, code: str) -> None:
    if not condition:
        errors.append(code)


def template(
    issue_number: int,
    title: str,
    issue_url: str,
    planning_sha: str,
    current_state_sha: str,
    requirement_locators: list[str],
    current_state_locators: list[str],
) -> dict[str, Any]:
    """Return a draft whose only pre-filled context is supplied by the PM."""
    impacts = {
        document: {
            "decision": "pending",
            "locators": requirement_locators if document == "requirement" else [],
            "reason": None,
            "follow_up": None,
        }
        for document in DOCUMENTS
    }
    return {
        "schema": "triage-v1",
        "planning_state": "planning",
        "frozen_digest": None,
        "issue": {"number": issue_number, "title": title, "url": issue_url},
        "planning_baseline_sha": planning_sha,
        "current_state": {
            "snapshot_sha": current_state_sha,
            "freshness": "fresh",
            "usage": "read-only",
            "locators": current_state_locators,
        },
        "goal": None,
        "scope": [],
        "out_of_scope": [],
        "current_behavior": None,
        "desired_behavior": None,
        "implementation_idea": None,
        "candidate_task_slices": [],
        "candidate_dependencies": [],
        "verification_direction": [],
        "document_impact": impacts,
        "policy_findings": [],
        "decision_requests": [],
        "blocker": None,
        "build_task_graph": {
            "allowed": False,
            "provenance_only": True,
            "coder_reads_triage": False,
        },
    }


def validate(body: Any, status: str | None = None) -> list[str]:
    """Return deterministic contract errors without reading or mutating Kanban."""
    errors: list[str] = []
    if not isinstance(body, dict):
        return ["BODY_NOT_OBJECT"]

    _error(errors, body.get("schema") == "triage-v1", "SCHEMA_VERSION")
    planning_state = body.get("planning_state")
    _error(errors, planning_state in PLANNING_STATES, "INVALID_PLANNING_STATE")
    frozen_digest = body.get("frozen_digest")
    if planning_state == "planning":
        _error(errors, frozen_digest is None, "PLANNING_BODY_WITH_FROZEN_DIGEST")
    if planning_state == "frozen":
        _error(errors, isinstance(frozen_digest, str) and frozen_digest == _body_digest(body), "FROZEN_BODY_DIGEST_MISMATCH")
    issue = body.get("issue")
    _error(errors, isinstance(issue, dict), "INVALID_ISSUE")
    if isinstance(issue, dict):
        _error(errors, isinstance(issue.get("number"), int) and issue["number"] > 0, "INVALID_ISSUE_NUMBER")
        _error(errors, _non_empty_string(issue.get("title")), "MISSING_ISSUE_TITLE")
        if isinstance(issue.get("number"), int):
            _error(errors, issue.get("title") == f"G1-Issue{issue['number']}-Triage", "INVALID_ISSUE_TITLE")
        _error(errors, _non_empty_string(issue.get("url")), "MISSING_ISSUE_URL")
    _error(errors, isinstance(body.get("planning_baseline_sha"), str) and SHA.fullmatch(body["planning_baseline_sha"]) is not None, "INVALID_PLANNING_SHA")

    current_state = body.get("current_state")
    _error(errors, isinstance(current_state, dict), "INVALID_CURRENT_STATE")
    if isinstance(current_state, dict):
        _error(errors, isinstance(current_state.get("snapshot_sha"), str) and SHA.fullmatch(current_state["snapshot_sha"]) is not None, "INVALID_CURRENT_STATE_SHA")
        _error(errors, current_state.get("freshness") == "fresh", "CURRENT_STATE_NOT_FRESH")
        _error(errors, current_state.get("usage") == "read-only", "CURRENT_STATE_NOT_READ_ONLY")
        _error(errors, _string_list(current_state.get("locators")), "MISSING_CURRENT_STATE_LOCATOR")

    _error(errors, _non_empty_string(body.get("goal")), "MISSING_GOAL")
    for field in ("scope", "out_of_scope", "candidate_task_slices", "verification_direction"):
        _error(errors, _string_list(body.get(field)), f"MISSING_{field.upper()}")
    for field in ("current_behavior", "desired_behavior", "implementation_idea"):
        _error(errors, _non_empty_string(body.get(field)), f"MISSING_{field.upper()}")
    _error(errors, isinstance(body.get("candidate_dependencies"), list), "INVALID_CANDIDATE_DEPENDENCIES")

    impacts = body.get("document_impact")
    _error(errors, isinstance(impacts, dict), "INVALID_DOCUMENT_IMPACT")
    if isinstance(impacts, dict):
        for document in DOCUMENTS:
            entry = impacts.get(document)
            _error(errors, isinstance(entry, dict), f"MISSING_DOCUMENT:{document}")
            if not isinstance(entry, dict):
                continue
            decision = entry.get("decision")
            _error(errors, decision in DECISIONS, f"INVALID_DOCUMENT_DECISION:{document}")
            locators = entry.get("locators")
            _error(errors, _optional_string_list(locators), f"INVALID_DOCUMENT_LOCATORS:{document}")
            if decision in {"update", "no-change", "blocked"}:
                _error(errors, _string_list(locators), f"MISSING_DOCUMENT_LOCATOR:{document}")
            _error(errors, _non_empty_string(entry.get("reason")), f"MISSING_DOCUMENT_REASON:{document}")
            if decision == "update":
                _error(errors, _non_empty_string(entry.get("follow_up")), f"MISSING_DOCUMENT_FOLLOW_UP:{document}")

    policy_findings = body.get("policy_findings")
    _error(errors, isinstance(policy_findings, list), "INVALID_POLICY_FINDINGS")
    open_findings = False
    if isinstance(policy_findings, list):
        for index, finding in enumerate(policy_findings):
            prefix = f"POLICY_FINDING:{index}"
            _error(errors, isinstance(finding, dict), f"INVALID_{prefix}")
            if not isinstance(finding, dict):
                continue
            _error(errors, _non_empty_string(finding.get("id")), f"MISSING_{prefix}_ID")
            _error(errors, _non_empty_string(finding.get("problem")), f"MISSING_{prefix}_PROBLEM")
            _error(errors, _string_list(finding.get("evidence")), f"MISSING_{prefix}_EVIDENCE")
            finding_status = finding.get("status")
            _error(errors, finding_status in FINDING_STATUSES, f"INVALID_{prefix}_STATUS")
            open_findings = open_findings or finding_status == "open"

    decision_requests = body.get("decision_requests")
    _error(errors, isinstance(decision_requests, list), "INVALID_DECISION_REQUESTS")
    unresolved_requests = False
    if isinstance(decision_requests, list):
        for index, request in enumerate(decision_requests):
            prefix = f"DECISION_REQUEST:{index}"
            _error(errors, isinstance(request, dict), f"INVALID_{prefix}")
            if not isinstance(request, dict):
                continue
            for field in ("id", "problem", "why", "decision_owner"):
                _error(errors, _non_empty_string(request.get(field)), f"MISSING_{prefix}_{field.upper()}")
            _error(errors, _string_list(request.get("evidence")), f"MISSING_{prefix}_EVIDENCE")
            options = request.get("options")
            _error(errors, isinstance(options, list) and bool(options), f"MISSING_{prefix}_OPTIONS")
            if isinstance(options, list):
                for option_index, option in enumerate(options):
                    option_prefix = f"{prefix}_OPTION:{option_index}"
                    _error(errors, isinstance(option, dict), f"INVALID_{option_prefix}")
                    if isinstance(option, dict):
                        for field in ("id", "choice", "impact"):
                            _error(errors, _non_empty_string(option.get(field)), f"MISSING_{option_prefix}_{field.upper()}")
            decision_status = request.get("decision_status")
            _error(errors, decision_status in DECISION_STATUSES, f"INVALID_{prefix}_STATUS")
            unresolved_requests = unresolved_requests or decision_status in {"pending", "deferred"}
            if decision_status == "approved":
                _error(errors, _non_empty_string(request.get("approved_change")), f"MISSING_{prefix}_APPROVED_CHANGE")
    blocker = body.get("blocker")
    _error(errors, blocker is None or isinstance(blocker, dict), "INVALID_BLOCKER")

    handoff = body.get("build_task_graph")
    _error(errors, isinstance(handoff, dict), "INVALID_GRAPH_HANDOFF")
    if isinstance(handoff, dict):
        allowed = handoff.get("allowed")
        _error(errors, isinstance(allowed, bool), "INVALID_GRAPH_GATE")
        _error(errors, handoff.get("provenance_only") is True, "INVALID_PROVENANCE_BOUNDARY")
        _error(errors, handoff.get("coder_reads_triage") is False, "INVALID_CODER_BOUNDARY")
        blocked_documents = isinstance(impacts, dict) and any(
            isinstance(impacts.get(document), dict) and impacts[document].get("decision") == "blocked"
            for document in DOCUMENTS
        )
        if allowed is True:
            _error(errors, planning_state == "frozen", "OPEN_GRAPH_GATE_WITHOUT_FROZEN_PLAN")
            if status is not None:
                _error(errors, status in {"running", "done"}, "OPEN_GRAPH_GATE_IN_INVALID_STATUS")
            _error(errors, blocker is None, "OPEN_GRAPH_GATE_WITH_BLOCKER")
            _error(errors, not open_findings, "OPEN_GRAPH_GATE_WITH_POLICY_FINDING")
            _error(errors, not unresolved_requests, "OPEN_GRAPH_GATE_WITH_DECISION_REQUEST")
            _error(errors, not blocked_documents, "OPEN_GRAPH_GATE_WITH_BLOCKED_DOCUMENT")
        if planning_state == "frozen":
            _error(errors, allowed is True, "FROZEN_PLAN_WITH_CLOSED_GRAPH_GATE")
        if status == "blocked":
            _error(errors, allowed is False, "BLOCKED_GRAPH_GATE")
            _error(errors, planning_state == "planning", "BLOCKED_FROZEN_PLAN")
            _error(errors, isinstance(blocker, dict) and _non_empty_string(blocker.get("kind")), "MISSING_BLOCKER")
            _error(errors, open_findings, "BLOCKED_WITHOUT_OPEN_POLICY_FINDING")
            _error(errors, unresolved_requests, "BLOCKED_WITHOUT_DECISION_REQUEST")
        if status == "done":
            _error(errors, allowed is True, "DONE_GRAPH_GATE")
            _error(errors, blocker is None, "DONE_WITH_BLOCKER")
    if status is not None:
        _error(errors, status in STATUSES, "INVALID_STATUS")
    return errors


def _read_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def _project_temp_output(path: Path) -> Path:
    if path.is_absolute() or not path.parts or path.parts[0] != ".temp":
        raise ValueError("output path must be relative to this project's .temp directory")
    temp_root = (Path.cwd() / ".temp").resolve()
    destination = (Path.cwd() / path).resolve()
    if destination != temp_root and temp_root not in destination.parents:
        raise ValueError("output path must stay inside this project's .temp directory")
    return destination


def _body_digest(body: dict[str, Any]) -> str:
    payload = dict(body)
    payload.pop("frozen_digest", None)
    canonical = json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


def freeze(body: dict[str, Any]) -> dict[str, Any]:
    """Return a frozen copy with an integrity digest and an open graph gate."""
    frozen = json.loads(json.dumps(body, ensure_ascii=False))
    frozen["planning_state"] = "frozen"
    frozen["frozen_digest"] = None
    handoff = frozen.get("build_task_graph")
    if isinstance(handoff, dict):
        handoff["allowed"] = True
    frozen["frozen_digest"] = _body_digest(frozen)
    return frozen


def write_json(path: Path, payload: Any) -> None:
    destination = _project_temp_output(path)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def _parse_show_json(output: str) -> dict[str, Any]:
    parsed = json.loads(output)
    if not isinstance(parsed, dict):
        raise ValueError("kanban show did not return a JSON object")
    task = parsed.get("task", parsed)
    if not isinstance(task, dict):
        raise ValueError("kanban show response has no task object")
    return task


def read_card(hermes_bin: str, board: str, task_id: str) -> dict[str, Any]:
    """Read one card using only Hermes' public CLI surface."""
    completed = subprocess.run(
        [hermes_bin, "kanban", "--board", board, "show", task_id, "--json"],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )
    if completed.returncode != 0:
        raise RuntimeError(f"kanban show failed (exit {completed.returncode})")
    return _parse_show_json(completed.stdout)


def validate_card(task: dict[str, Any], requested_task_id: str, expected_status: str | None, expected_assignee: str | None) -> list[str]:
    errors: list[str] = []
    actual_id = task.get("id")
    _error(errors, isinstance(actual_id, str) and TASK_ID.fullmatch(actual_id) is not None, "INVALID_NATIVE_TASK_ID")
    _error(errors, actual_id == requested_task_id, "TASK_ID_MISMATCH")
    status = task.get("status")
    _error(errors, isinstance(status, str) and status in STATUSES, "INVALID_NATIVE_STATUS")
    if expected_status is not None:
        _error(errors, status == expected_status, "STATUS_MISMATCH")
    if expected_assignee is not None:
        _error(errors, task.get("assignee") == expected_assignee, "ASSIGNEE_MISMATCH")
    body = task.get("body")
    if not isinstance(body, str):
        errors.append("MISSING_NATIVE_BODY")
    else:
        try:
            errors.extend(validate(json.loads(body), status if isinstance(status, str) else None))
        except json.JSONDecodeError:
            errors.append("BODY_NOT_JSON")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)

    template_parser = subparsers.add_parser("template", help="write a JSON triage draft")
    template_parser.add_argument("--issue", required=True, type=int)
    template_parser.add_argument("--title", required=True)
    template_parser.add_argument("--issue-url", required=True)
    template_parser.add_argument("--planning-sha", required=True)
    template_parser.add_argument("--current-state-sha", required=True)
    template_parser.add_argument("--requirement-locator", action="append", default=[])
    template_parser.add_argument("--current-state-locator", action="append", required=True)
    template_parser.add_argument("--output", required=True, type=Path)

    validate_parser = subparsers.add_parser("validate", help="validate a JSON body file")
    validate_parser.add_argument("body", type=Path)
    validate_parser.add_argument("--status", choices=sorted(STATUSES))

    freeze_parser = subparsers.add_parser("freeze", help="freeze a validated planning body")
    freeze_parser.add_argument("body", type=Path)
    freeze_parser.add_argument("--output", required=True, type=Path)

    card_parser = subparsers.add_parser("validate-card", help="read and validate one Kanban card")
    card_parser.add_argument("--task-id", required=True)
    card_parser.add_argument("--board", required=True)
    card_parser.add_argument("--expected-status", choices=sorted(STATUSES))
    card_parser.add_argument("--expected-assignee")
    card_parser.add_argument("--hermes-bin", default="hermes")
    card_parser.add_argument("--report", type=Path)

    args = parser.parse_args()
    if args.command == "template":
        write_json(
            args.output,
            template(
                args.issue,
                args.title,
                args.issue_url,
                args.planning_sha,
                args.current_state_sha,
                args.requirement_locator,
                args.current_state_locator,
            ),
        )
        return 0
    if args.command == "validate":
        errors = validate(_read_json(args.body), args.status)
        print(json.dumps({"valid": not errors, "errors": errors}, ensure_ascii=False))
        return 1 if errors else 0
    if args.command == "freeze":
        frozen = freeze(_read_json(args.body))
        errors = validate(frozen, "running")
        print(json.dumps({"valid": not errors, "errors": errors}, ensure_ascii=False))
        if errors:
            return 1
        write_json(args.output, frozen)
        return 0

    task = read_card(args.hermes_bin, args.board, args.task_id)
    errors = validate_card(task, args.task_id, args.expected_status, args.expected_assignee)
    report = {"valid": not errors, "task_id": args.task_id, "errors": errors}
    if args.report:
        write_json(args.report, report)
    print(json.dumps(report, ensure_ascii=False))
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
