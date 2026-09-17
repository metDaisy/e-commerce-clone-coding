#!/usr/bin/env python3
"""Create and validate JSON contracts for Project Manager triage cards."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path
from typing import Any

DOCUMENTS = ("requirement", "architecture", "ADR", "glossary", "ERD", "index")
DECISIONS = {"update", "no-change", "not-applicable", "blocked"}
STATUSES = {"triage", "todo", "ready", "running", "blocked", "done"}
SHA = re.compile(r"^[0-9a-f]{40}$")
TASK_ID = re.compile(r"^t_[0-9a-f]+$")


def _non_empty_string(value: Any) -> bool:
    return isinstance(value, str) and bool(value.strip())


def _string_list(value: Any) -> bool:
    return isinstance(value, list) and bool(value) and all(_non_empty_string(item) for item in value)


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
    issue = body.get("issue")
    _error(errors, isinstance(issue, dict), "INVALID_ISSUE")
    if isinstance(issue, dict):
        _error(errors, isinstance(issue.get("number"), int) and issue["number"] > 0, "INVALID_ISSUE_NUMBER")
        _error(errors, _non_empty_string(issue.get("title")), "MISSING_ISSUE_TITLE")
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
            _error(errors, _non_empty_string(entry.get("reason")), f"MISSING_DOCUMENT_REASON:{document}")
            if decision == "update":
                _error(errors, _non_empty_string(entry.get("follow_up")), f"MISSING_DOCUMENT_FOLLOW_UP:{document}")

    for field in ("policy_findings", "decision_requests"):
        _error(errors, isinstance(body.get(field), list), f"INVALID_{field.upper()}")
    blocker = body.get("blocker")
    _error(errors, blocker is None or isinstance(blocker, dict), "INVALID_BLOCKER")

    handoff = body.get("build_task_graph")
    _error(errors, isinstance(handoff, dict), "INVALID_GRAPH_HANDOFF")
    if isinstance(handoff, dict):
        allowed = handoff.get("allowed")
        _error(errors, isinstance(allowed, bool), "INVALID_GRAPH_GATE")
        _error(errors, handoff.get("provenance_only") is True, "INVALID_PROVENANCE_BOUNDARY")
        _error(errors, handoff.get("coder_reads_triage") is False, "INVALID_CODER_BOUNDARY")
        if status == "blocked":
            _error(errors, allowed is False, "BLOCKED_GRAPH_GATE")
            _error(errors, isinstance(blocker, dict) and _non_empty_string(blocker.get("kind")), "MISSING_BLOCKER")
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
    return (Path.cwd() / path).resolve()


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

    task = read_card(args.hermes_bin, args.board, args.task_id)
    errors = validate_card(task, args.task_id, args.expected_status, args.expected_assignee)
    report = {"valid": not errors, "task_id": args.task_id, "errors": errors}
    if args.report:
        write_json(args.report, report)
    print(json.dumps(report, ensure_ascii=False))
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
