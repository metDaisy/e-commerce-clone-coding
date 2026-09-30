#!/usr/bin/env python3
"""Fail-closed PM-only activation for an already materialized task graph.

This wrapper intentionally owns only the final native transitions.  It does not
create cards, edit planning bodies, or invent policy.  A failed preflight or
native transition stops the sequence before the next mutation is attempted.
"""

from __future__ import annotations

import argparse
import copy
import json
import subprocess
import sys
from pathlib import Path
from typing import Any

SCRIPT_DIR = Path(__file__).resolve().parent
SKILLS_ROOT = SCRIPT_DIR.parents[1]
BUILD_SCRIPTS = SKILLS_ROOT / "build-task-graph" / "scripts"
TRIAGE_SCRIPTS = SKILLS_ROOT / "create-triage" / "scripts"
sys.path[:0] = [str(BUILD_SCRIPTS), str(TRIAGE_SCRIPTS)]

from build_task_graph import validate as validate_implementation  # noqa: E402
from graph_contract import validate_graph, validate_native_readback  # noqa: E402
from triage import validate as validate_triage  # noqa: E402
from triage import validate_decision_alignment  # noqa: E402

PM_PROFILE = "project-manager"


def _load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def _write_report(path: Path | None, payload: dict[str, Any]) -> None:
    encoded = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
    if path is None:
        print(encoded, end="")
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(encoded, encoding="utf-8")


def _parse_show(payload: str) -> dict[str, Any]:
    parsed = json.loads(payload)
    if not isinstance(parsed, dict):
        raise ValueError("KANBAN_SHOW_NOT_OBJECT")
    task = parsed.get("task", parsed)
    if not isinstance(task, dict):
        raise ValueError("KANBAN_SHOW_MISSING_TASK")
    parents = parsed.get("parents", task.get("parents", []))
    if not isinstance(parents, list):
        raise ValueError("KANBAN_SHOW_INVALID_PARENTS")
    return {"task": task, "parents": parents}


def _run_kanban(
    hermes_bin: str,
    board: str,
    arguments: list[str],
    *,
    runner=subprocess.run,
) -> subprocess.CompletedProcess[str]:
    return runner(
        [hermes_bin, "--profile", PM_PROFILE, "kanban", "--board", board, *arguments],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )


def _show(hermes_bin: str, board: str, task_id: str, *, runner=subprocess.run) -> dict[str, Any]:
    completed = _run_kanban(hermes_bin, board, ["show", task_id, "--json"], runner=runner)
    if completed.returncode != 0:
        raise RuntimeError(f"KANBAN_SHOW_FAILED:{task_id}:{completed.returncode}")
    return _parse_show(completed.stdout)


def _body(task: dict[str, Any]) -> dict[str, Any]:
    raw = task.get("body")
    if not isinstance(raw, str):
        raise ValueError("NATIVE_BODY_NOT_TEXT")
    parsed = json.loads(raw)
    if not isinstance(parsed, dict):
        raise ValueError("NATIVE_BODY_NOT_OBJECT")
    return parsed


def _native_id_by_key(graph: dict[str, Any], readback: list[Any]) -> dict[str, str]:
    by_title: dict[str, str] = {}
    for envelope in readback:
        if not isinstance(envelope, dict):
            continue
        task = envelope.get("task", envelope)
        if isinstance(task, dict) and isinstance(task.get("title"), str) and isinstance(task.get("id"), str):
            by_title[task["title"]] = task["id"]
    result: dict[str, str] = {}
    for card in graph.get("cards", []):
        if isinstance(card, dict) and isinstance(card.get("key"), str) and isinstance(card.get("title"), str):
            task_id = by_title.get(card["title"])
            if task_id is not None:
                result[card["key"]] = task_id
    return result


def _graph_card(graph: dict[str, Any], key: str) -> dict[str, Any] | None:
    for card in graph.get("cards", []):
        if isinstance(card, dict) and card.get("key") == key:
            return card
    return None


def _read_current_graph(
    graph: dict[str, Any],
    expected_readback: list[Any],
    hermes_bin: str,
    board: str,
    *,
    runner=subprocess.run,
) -> list[dict[str, Any]]:
    ids = _native_id_by_key(graph, expected_readback)
    current: list[dict[str, Any]] = []
    for card in graph.get("cards", []):
        if not isinstance(card, dict) or not isinstance(card.get("key"), str):
            continue
        task_id = ids.get(card["key"])
        if task_id is None:
            raise ValueError(f"NATIVE_TASK_ID_MISSING:{card.get('key')}")
        current.append(_show(hermes_bin, board, task_id, runner=runner))
    return current


def _with_native_statuses(graph: dict[str, Any], readback: list[dict[str, Any]]) -> dict[str, Any]:
    observed = copy.deepcopy(graph)
    statuses: dict[str, Any] = {}
    for envelope in readback:
        task = envelope.get("task", envelope)
        if isinstance(task, dict) and isinstance(task.get("title"), str):
            statuses[task["title"]] = task.get("status")
    for card in observed.get("cards", []):
        if isinstance(card, dict) and isinstance(card.get("title"), str):
            card["status"] = statuses.get(card["title"], card.get("status"))
    return observed


def _preflight(
    graph: dict[str, Any],
    expected_readback: list[Any],
    triage_task_id: str,
    decision_task_ids: list[str],
    activation_key: str,
    hermes_bin: str,
    board: str,
    *,
    runner=subprocess.run,
) -> tuple[list[str], dict[str, Any]]:
    errors = validate_graph(graph, phase="draft", validate_implementation=validate_implementation)
    errors.extend(validate_native_readback(graph, expected_readback))
    details: dict[str, Any] = {"triage_task_id": triage_task_id, "decision_task_ids": decision_task_ids}
    try:
        triage_envelope = _show(hermes_bin, board, triage_task_id, runner=runner)
        triage_task = triage_envelope["task"]
        if triage_task.get("status") != "triage":
            errors.append("TRIAGE_NOT_NATIVE_TRIAGE")
        triage_body = _body(triage_task)
        errors.extend(validate_triage(triage_body, triage_task.get("status")))
        details["triage_status"] = triage_task.get("status")
    except (RuntimeError, ValueError, json.JSONDecodeError) as exc:
        errors.append(str(exc))
        triage_body = None

    requested_ids: set[str] = set()
    if isinstance(triage_body, dict):
        for request in triage_body.get("decision_requests", []):
            if isinstance(request, dict) and isinstance(request.get("id"), str):
                requested_ids.add(request["id"])

    decision_request_ids: set[str] = set()
    for task_id in decision_task_ids:
        try:
            envelope = _show(hermes_bin, board, task_id, runner=runner)
            task = envelope["task"]
            decision_body = _body(task)
            if task.get("status") != "running":
                errors.append(f"DECISION_NOT_RUNNING:{task_id}")
            if decision_body.get("decision_status") != "approved":
                errors.append(f"DECISION_NOT_APPROVED:{task_id}")
            if isinstance(triage_body, dict):
                errors.extend(validate_decision_alignment(triage_body, decision_body))
            request_id = decision_body.get("decision_request_id")
            if isinstance(request_id, str):
                decision_request_ids.add(request_id)
        except (RuntimeError, ValueError, json.JSONDecodeError) as exc:
            errors.append(str(exc))
    if requested_ids != decision_request_ids:
        errors.append("TRIAGE_DECISION_CARDS_MISMATCH")

    candidate = copy.deepcopy(graph)
    triage_card = next((card for card in candidate.get("cards", []) if isinstance(card, dict) and card.get("card_type") == "triage"), None)
    target = _graph_card(candidate, activation_key)
    if not isinstance(triage_card, dict):
        errors.append("GRAPH_TRIAGE_MISSING")
    elif isinstance(target, dict):
        triage_card["status"] = "done"
        target["status"] = "ready"
        errors.extend(validate_graph(candidate, phase="native", validate_implementation=validate_implementation))
    else:
        errors.append("ACTIVATION_CARD_MISSING")
    details["activation_key"] = activation_key
    return sorted(set(errors)), details


def activate(
    graph_path: Path,
    native_readback_path: Path,
    triage_task_id: str,
    decision_task_ids: list[str],
    activation_key: str,
    hermes_bin: str,
    board: str,
    *,
    apply: bool,
    runner=subprocess.run,
) -> dict[str, Any]:
    graph = _load_json(graph_path)
    readback = _load_json(native_readback_path)
    if not isinstance(graph, dict) or not isinstance(readback, list):
        return {"valid": False, "errors": ["INVALID_ACTIVATION_INPUT"], "mutated": False}
    errors, details = _preflight(
        graph,
        readback,
        triage_task_id,
        decision_task_ids,
        activation_key,
        hermes_bin,
        board,
        runner=runner,
    )
    if errors or not apply:
        return {"valid": not errors, "errors": errors, "mutated": False, "details": details}

    close_ids = [*decision_task_ids, triage_task_id]
    completed = _run_kanban(
        hermes_bin,
        board,
        ["complete", "--result", "planning graph materialized and activation verified", *close_ids],
        runner=runner,
    )
    if completed.returncode != 0:
        diagnostic = (completed.stderr or completed.stdout).strip().replace("\n", " ")
        return {
            "valid": False,
            "errors": [f"NATIVE_COMPLETE_FAILED:{completed.returncode}:{diagnostic}"],
            "mutated": True,
            "details": details,
        }
    for task_id in close_ids:
        try:
            current = _show(hermes_bin, board, task_id, runner=runner)["task"]
            if current.get("status") != "done":
                return {
                    "valid": False,
                    "errors": [f"NATIVE_COMPLETE_READBACK_FAILED:{task_id}"],
                    "mutated": True,
                    "details": details,
                }
        except (RuntimeError, ValueError, json.JSONDecodeError) as exc:
            return {"valid": False, "errors": [str(exc)], "mutated": True, "details": details}

    target_id = _native_id_by_key(graph, readback).get(activation_key)
    if target_id is None:
        return {"valid": False, "errors": ["ACTIVATION_TASK_ID_MISSING"], "mutated": True, "details": details}
    promoted = _run_kanban(hermes_bin, board, ["promote", target_id, "approved triage graph"], runner=runner)
    if promoted.returncode != 0:
        return {
            "valid": False,
            "errors": [f"NATIVE_PROMOTE_FAILED:{promoted.returncode}"],
            "mutated": True,
            "details": details,
        }
    try:
        current_readback = _read_current_graph(graph, readback, hermes_bin, board, runner=runner)
        observed_graph = _with_native_statuses(graph, current_readback)
        errors = validate_graph(observed_graph, phase="native", validate_implementation=validate_implementation)
        errors.extend(validate_native_readback(observed_graph, current_readback))
        return {"valid": not errors, "errors": sorted(set(errors)), "mutated": True, "details": details}
    except (RuntimeError, ValueError, json.JSONDecodeError) as exc:
        return {"valid": False, "errors": [str(exc)], "mutated": True, "details": details}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--board", required=True)
    parser.add_argument("--graph", required=True, type=Path)
    parser.add_argument("--native-readback", required=True, type=Path)
    parser.add_argument("--triage-task-id", required=True)
    parser.add_argument("--decision-task-id", action="append", default=[])
    parser.add_argument("--activation-key", required=True)
    parser.add_argument("--hermes-bin", default="hermes")
    parser.add_argument("--apply", action="store_true", help="perform completion and promotion after a successful preflight")
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    report = activate(
        args.graph,
        args.native_readback,
        args.triage_task_id,
        args.decision_task_id,
        args.activation_key,
        args.hermes_bin,
        args.board,
        apply=args.apply,
    )
    _write_report(args.report, report)
    return 0 if report["valid"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
