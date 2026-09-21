#!/usr/bin/env python3
"""Read-only validators for the Project Manager runtime workflow."""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Any

SHA = re.compile(r"^[0-9a-f]{40}$")
TASK_ID = re.compile(r"^t_[0-9a-f]+$")
URL = re.compile(r"^https://[^/]+/.+")
STATUSES = {"triage", "todo", "ready", "running", "review", "blocked", "done", "archived"}
BLOCKING_VERDICTS = {"correction-required", "context-required", "decision-required"}
FINDING_VERDICTS = BLOCKING_VERDICTS | {"resolved"}


def _non_empty(value: Any) -> bool:
    return isinstance(value, str) and bool(value.strip())


def _strings(value: Any, *, allow_empty: bool = False) -> bool:
    return isinstance(value, list) and (allow_empty or bool(value)) and all(_non_empty(item) for item in value)


def _sha(value: Any) -> bool:
    return isinstance(value, str) and SHA.fullmatch(value) is not None


def _task_id(value: Any) -> bool:
    return isinstance(value, str) and TASK_ID.fullmatch(value) is not None


def _literal_repository_path(value: Any) -> bool:
    if not _non_empty(value) or "\\" in value or value.startswith(("/", "--", ":(")):
        return False
    if re.match(r"^[A-Za-z]:", value) or any(part in {"", ".", "..", "..."} for part in value.split("/")):
        return False
    return not any(token in value for token in ("*", "?", "[", "]", "{", "}"))


def _exact(value: dict[str, Any], allowed: set[str], errors: list[str], prefix: str) -> None:
    for field in sorted(set(value) - allowed):
        errors.append(f"{prefix}:{field}")


def _require(condition: bool, errors: list[str], code: str) -> None:
    if not condition:
        errors.append(code)


def _unique(values: Any) -> bool:
    return isinstance(values, list) and all(isinstance(value, str) for value in values) and len(values) == len(set(values))


def _successful_run(record: dict[str, Any]) -> bool:
    runs = record.get("runs")
    if not isinstance(runs, list) or not runs:
        return False
    latest = runs[-1]
    if not isinstance(latest, dict):
        return False
    values = {str(latest.get(name, "")).lower() for name in ("status", "outcome", "result")}
    return bool(values & {"success", "succeeded", "passed", "completed", "review_requested"})


def validate_state(graph: Any, board: Any, git: Any) -> tuple[list[str], dict[str, Any]]:
    """Validate a progressed native graph and derive one truthful next transition."""
    errors: list[str] = []
    decision: dict[str, Any] = {"phase": "invalid", "eligible_task_ids": [], "ready_task_ids": [], "allowed_transition": None}
    if not isinstance(graph, dict) or graph.get("schema") != "build-task-graph-v1":
        return ["GRAPH_SCHEMA"], decision
    cards = graph.get("cards")
    tasks = board.get("tasks") if isinstance(board, dict) else None
    if not isinstance(cards, list) or not isinstance(tasks, list):
        return ["STATE_INPUT_SCHEMA"], decision
    if not isinstance(git, dict):
        return ["GIT_READBACK_NOT_OBJECT"], decision
    _exact(git, {"branch", "head_sha", "clean_worktree"}, errors, "UNEXPECTED_GIT_FIELD")
    _require(_non_empty(git.get("branch")), errors, "MISSING_BRANCH")
    _require(_sha(git.get("head_sha")), errors, "INVALID_HEAD_SHA")
    _require(isinstance(git.get("clean_worktree"), bool), errors, "INVALID_CLEAN_WORKTREE")

    graph_records = {card.get("key"): card for card in cards if isinstance(card, dict) and _non_empty(card.get("key"))}
    board_records = {task.get("key"): task for task in tasks if isinstance(task, dict) and _non_empty(task.get("key"))}
    _require(len(graph_records) == len(cards), errors, "DUPLICATE_OR_INVALID_GRAPH_KEY")
    _require(len(board_records) == len(tasks), errors, "DUPLICATE_OR_INVALID_BOARD_KEY")
    _require(set(graph_records) == set(board_records), errors, "BOARD_MEMBERSHIP_MISMATCH")
    ready: list[str] = []
    eligible: list[str] = []
    active: list[str] = []
    order = [card.get("key") for card in cards if isinstance(card, dict)]
    for key in order:
        if key not in graph_records or key not in board_records:
            continue
        planned, actual = graph_records[key], board_records[key]
        _exact(actual, {"key", "task_id", "title", "card_type", "assignee", "workspace", "status", "parents", "runs"}, errors, f"UNEXPECTED_TASK_FIELD:{key}")
        _require(_task_id(actual.get("task_id")), errors, f"INVALID_TASK_ID:{key}")
        _require(
            _non_empty(planned.get("workspace")) and _non_empty(actual.get("workspace")),
            errors,
            f"MISSING_WORKSPACE:{key}",
        )
        for field in ("title", "card_type", "assignee", "workspace"):
            _require(actual.get(field) == planned.get(field), errors, f"TASK_{field.upper()}_MISMATCH:{key}")
        status = actual.get("status")
        _require(status in STATUSES, errors, f"INVALID_STATUS:{key}")
        parents = actual.get("parents")
        _require(_unique(parents), errors, f"INVALID_PARENTS:{key}")
        if isinstance(parents, list):
            _require(parents == planned.get("parents"), errors, f"PARENT_MISMATCH:{key}")
        if status == "done":
            _require(_successful_run(actual), errors, f"DONE_WITHOUT_SUCCESSFUL_RUN:{key}")
        if status == "ready":
            ready.append(actual["task_id"])
        if status in {"running", "review"}:
            active.append(actual["task_id"])
        if status == "todo" and isinstance(parents, list) and all(
            board_records.get(parent, {}).get("status") == "done" for parent in parents
        ):
            eligible.append(actual["task_id"])
    _require(len(ready) <= 1, errors, "MULTIPLE_READY_TASKS")
    _require(len(active) <= 1, errors, "MULTIPLE_ACTIVE_TASKS")
    ready_keys = {key for key, value in board_records.items() if value.get("task_id") in ready}
    for key in ready_keys:
        parents = board_records[key].get("parents", [])
        _require(all(board_records.get(parent, {}).get("status") == "done" for parent in parents), errors, f"READY_PARENT_NOT_DONE:{key}")
    decision["eligible_task_ids"] = eligible
    decision["ready_task_ids"] = ready
    if active:
        decision.update(phase="active", allowed_transition={"action": "inspect-active-run", "task_id": active[0]})
    elif ready:
        decision.update(phase="ready", allowed_transition={"action": "claim", "task_id": ready[0]})
    elif eligible:
        decision.update(phase="promotion", allowed_transition={"action": "promote", "task_id": eligible[0]})
    elif any(task.get("status") == "blocked" for task in board_records.values()):
        decision.update(phase="blocked", allowed_transition={"action": "resolve-blocker"})
    elif all(task.get("status") in {"done", "archived"} for task in board_records.values()):
        decision.update(phase="complete", allowed_transition=None)
    else:
        decision.update(phase="stalled", allowed_transition=None)
        errors.append("NO_EXECUTABLE_TRANSITION")
    return errors, decision


def _validate_finding(finding: Any, index: int, errors: list[str]) -> tuple[str | None, str | None]:
    if not isinstance(finding, dict):
        errors.append(f"FINDING_NOT_OBJECT:{index}")
        return None, None
    _exact(finding, {"finding_id", "verdict", "basis", "observed_fact", "evidence", "impact", "resolves"}, errors, f"UNEXPECTED_FINDING_FIELD:{index}")
    finding_id, verdict = finding.get("finding_id"), finding.get("verdict")
    _require(_non_empty(finding_id), errors, f"MISSING_FINDING_ID:{index}")
    _require(verdict in FINDING_VERDICTS, errors, f"INVALID_FINDING_VERDICT:{index}")
    for field in ("basis", "observed_fact", "impact"):
        _require(_non_empty(finding.get(field)), errors, f"MISSING_FINDING_{field.upper()}:{index}")
    _require(_strings(finding.get("evidence")), errors, f"MISSING_FINDING_EVIDENCE:{index}")
    resolves = finding.get("resolves")
    if verdict == "resolved":
        if not isinstance(resolves, dict):
            errors.append(f"MISSING_FINDING_RESOLUTION:{index}")
        else:
            _exact(resolves, {"source_review_task_id", "source_finding_id"}, errors, f"UNEXPECTED_RESOLUTION_FIELD:{index}")
            _require(_task_id(resolves.get("source_review_task_id")), errors, f"INVALID_RESOLUTION_REVIEW:{index}")
            _require(_non_empty(resolves.get("source_finding_id")), errors, f"INVALID_RESOLUTION_FINDING:{index}")
    else:
        _require(resolves is None, errors, f"BLOCKING_FINDING_HAS_RESOLUTION:{index}")
    return finding_id if isinstance(finding_id, str) else None, verdict if isinstance(verdict, str) else None


def validate_review_result(result: Any, graph: Any) -> list[str]:
    errors: list[str] = []
    if not isinstance(result, dict):
        return ["REVIEW_RESULT_NOT_OBJECT"]
    allowed = {"schema", "review_card_key", "review_task_id", "review_run_id", "generation", "result", "reviewed_checkpoints", "prior_findings", "findings"}
    _exact(result, allowed, errors, "UNEXPECTED_REVIEW_RESULT_FIELD")
    _require(result.get("schema") == "aggregate-review-result-v1", errors, "REVIEW_RESULT_SCHEMA")
    _require(_task_id(result.get("review_task_id")), errors, "INVALID_REVIEW_TASK_ID")
    _require(_non_empty(result.get("review_run_id")), errors, "MISSING_REVIEW_RUN_ID")
    _require(result.get("result") in {"approved", "changes-required", "blocked"}, errors, "INVALID_REVIEW_RESULT")
    cards = graph.get("cards", []) if isinstance(graph, dict) else []
    records = {card.get("key"): card for card in cards if isinstance(card, dict)}
    review = records.get(result.get("review_card_key"))
    _require(isinstance(review, dict) and review.get("card_type") == "review", errors, "REVIEW_CARD_NOT_FOUND")
    _require(result.get("generation") == graph.get("generation") if isinstance(graph, dict) else False, errors, "REVIEW_GENERATION_MISMATCH")
    expected_impls = set(review.get("body", {}).get("implementation_card_keys", [])) if isinstance(review, dict) and isinstance(review.get("body"), dict) else set()
    checkpoints = result.get("reviewed_checkpoints")
    checkpoint_keys: list[str] = []
    if not isinstance(checkpoints, list):
        errors.append("INVALID_REVIEWED_CHECKPOINTS")
    else:
        for index, checkpoint in enumerate(checkpoints):
            if not isinstance(checkpoint, dict):
                errors.append(f"CHECKPOINT_NOT_OBJECT:{index}")
                continue
            _exact(checkpoint, {"implementation_card_key", "implementation_task_id", "checkpoint_sha"}, errors, f"UNEXPECTED_REVIEWED_CHECKPOINT_FIELD:{index}")
            checkpoint_keys.append(str(checkpoint.get("implementation_card_key")))
            _require(_task_id(checkpoint.get("implementation_task_id")), errors, f"INVALID_CHECKPOINT_TASK_ID:{index}")
            _require(_sha(checkpoint.get("checkpoint_sha")), errors, f"INVALID_CHECKPOINT_SHA:{index}")
    _require(len(checkpoint_keys) == len(set(checkpoint_keys)) and set(checkpoint_keys) == expected_impls, errors, "REVIEW_CHECKPOINT_COVERAGE_MISMATCH")

    prior = result.get("prior_findings")
    prior_refs: set[tuple[str, str]] = set()
    if not isinstance(prior, list):
        errors.append("INVALID_PRIOR_FINDINGS")
    else:
        for index, item in enumerate(prior):
            if not isinstance(item, dict):
                errors.append(f"PRIOR_FINDING_NOT_OBJECT:{index}")
                continue
            _exact(item, {"source_review_task_id", "source_finding_id", "verdict"}, errors, f"UNEXPECTED_PRIOR_FINDING_FIELD:{index}")
            ref = (str(item.get("source_review_task_id")), str(item.get("source_finding_id")))
            prior_refs.add(ref)
            _require(_task_id(item.get("source_review_task_id")), errors, f"INVALID_PRIOR_REVIEW_TASK_ID:{index}")
            _require(_non_empty(item.get("source_finding_id")), errors, f"INVALID_PRIOR_FINDING_ID:{index}")
            _require(item.get("verdict") in BLOCKING_VERDICTS, errors, f"INVALID_PRIOR_FINDING_VERDICT:{index}")
        _require(len(prior_refs) == len(prior), errors, "DUPLICATE_PRIOR_FINDING")
    findings = result.get("findings")
    finding_ids: list[str] = []
    resolved_refs: set[tuple[str, str]] = set()
    blocking = 0
    if not isinstance(findings, list):
        errors.append("INVALID_FINDINGS")
    else:
        for index, finding in enumerate(findings):
            finding_id, verdict = _validate_finding(finding, index, errors)
            if finding_id:
                finding_ids.append(finding_id)
            if verdict in BLOCKING_VERDICTS:
                blocking += 1
            if verdict == "resolved" and isinstance(finding, dict) and isinstance(finding.get("resolves"), dict):
                resolved_refs.add((str(finding["resolves"].get("source_review_task_id")), str(finding["resolves"].get("source_finding_id"))))
    _require(len(finding_ids) == len(set(finding_ids)), errors, "DUPLICATE_FINDING_ID")
    _require(prior_refs <= resolved_refs, errors, "PRIOR_FINDING_DROPPED")
    _require(resolved_refs <= prior_refs, errors, "RESOLUTION_SOURCE_NOT_FOUND")
    if result.get("result") == "approved":
        _require(blocking == 0 and prior_refs == resolved_refs, errors, "APPROVED_WITH_BLOCKING_FINDING")
    elif result.get("result") == "changes-required":
        _require(blocking > 0, errors, "CHANGES_REQUIRED_WITHOUT_FINDING")
    return errors


def validate_summary_admission(value: Any) -> list[str]:
    errors: list[str] = []
    if not isinstance(value, dict):
        return ["SUMMARY_ADMISSION_NOT_OBJECT"]
    allowed = {"schema", "summary_task_id", "latest_review_task_id", "latest_review_run_id", "latest_review_result", "direct_parent_task_ids", "done_parent_task_ids", "unresolved_finding_ids", "final_implementation_sha", "documentation_commit_shas", "repository_head_sha", "clean_worktree"}
    _exact(value, allowed, errors, "UNEXPECTED_SUMMARY_ADMISSION_FIELD")
    _require(value.get("schema") == "summary-admission-v1", errors, "SUMMARY_ADMISSION_SCHEMA")
    for field in ("summary_task_id", "latest_review_task_id"):
        _require(_task_id(value.get(field)), errors, f"INVALID_{field.upper()}")
    _require(_non_empty(value.get("latest_review_run_id")), errors, "MISSING_LATEST_REVIEW_RUN_ID")
    _require(value.get("latest_review_result") == "approved", errors, "LATEST_REVIEW_NOT_APPROVED")
    parents, done = value.get("direct_parent_task_ids"), value.get("done_parent_task_ids")
    _require(_unique(parents) and bool(parents), errors, "INVALID_SUMMARY_PARENTS")
    _require(_unique(done) and set(done) == set(parents or []), errors, "SUMMARY_PARENT_NOT_DONE")
    _require(value.get("unresolved_finding_ids") == [], errors, "SUMMARY_HAS_UNRESOLVED_FINDINGS")
    final_sha = value.get("final_implementation_sha")
    _require(_sha(final_sha), errors, "INVALID_FINAL_IMPLEMENTATION_SHA")
    docs_shas = value.get("documentation_commit_shas")
    _require(_unique(docs_shas) and all(_sha(sha) for sha in docs_shas or []), errors, "INVALID_DOCUMENTATION_COMMIT_SHAS")
    if isinstance(docs_shas, list) and docs_shas:
        _require(final_sha not in docs_shas, errors, "DOCUMENTATION_SHA_EQUALS_IMPLEMENTATION_SHA")
        _require(value.get("repository_head_sha") == docs_shas[-1], errors, "FINAL_DOCUMENTATION_SHA_MISMATCH")
    else:
        _require(value.get("repository_head_sha") == final_sha, errors, "FINAL_IMPLEMENTATION_SHA_MISMATCH")
    _require(value.get("clean_worktree") is True, errors, "SUMMARY_WORKTREE_NOT_CLEAN")
    return errors


def validate_release(value: Any) -> list[str]:
    errors: list[str] = []
    if not isinstance(value, dict):
        return ["RELEASE_NOT_OBJECT"]
    allowed = {"schema", "issue", "delivery_branch", "default_branch", "final_implementation_sha", "snapshot", "pull_request", "ci", "merge", "issue_readback"}
    _exact(value, allowed, errors, "UNEXPECTED_RELEASE_FIELD")
    _require(value.get("schema") == "issue-release-result-v1", errors, "RELEASE_SCHEMA")
    issue = value.get("issue")
    if not isinstance(issue, dict):
        errors.append("INVALID_RELEASE_ISSUE")
        issue_number = None
    else:
        _exact(issue, {"number", "url"}, errors, "UNEXPECTED_RELEASE_ISSUE_FIELD")
        issue_number = issue.get("number")
        _require(isinstance(issue_number, int) and not isinstance(issue_number, bool) and issue_number > 0, errors, "INVALID_RELEASE_ISSUE_NUMBER")
        _require(isinstance(issue.get("url"), str) and URL.fullmatch(issue["url"]) is not None, errors, "INVALID_RELEASE_ISSUE_URL")
    for field in ("delivery_branch", "default_branch"):
        _require(_non_empty(value.get(field)), errors, f"MISSING_{field.upper()}")
    final_sha = value.get("final_implementation_sha")
    _require(_sha(final_sha), errors, "INVALID_RELEASE_IMPLEMENTATION_SHA")
    snapshot = value.get("snapshot")
    if not isinstance(snapshot, dict):
        errors.append("INVALID_RELEASE_SNAPSHOT")
    else:
        _exact(snapshot, {"inspection_sha", "docs_commit_sha", "changed_paths", "marker_removed", "clean_worktree"}, errors, "UNEXPECTED_SNAPSHOT_FIELD")
        _require(snapshot.get("inspection_sha") == final_sha, errors, "SNAPSHOT_INSPECTION_SHA_MISMATCH")
        _require(_sha(snapshot.get("docs_commit_sha")) and snapshot.get("docs_commit_sha") != final_sha, errors, "INVALID_DOCS_COMMIT_SHA")
        _require(snapshot.get("changed_paths") == ["docs/current-state.md"], errors, "SNAPSHOT_COMMIT_NOT_DOCS_ONLY")
        _require(snapshot.get("marker_removed") is True, errors, "ACTIVE_MARKER_NOT_REMOVED")
        _require(snapshot.get("clean_worktree") is True, errors, "RELEASE_WORKTREE_NOT_CLEAN")
    pr = value.get("pull_request")
    if not isinstance(pr, dict):
        errors.append("INVALID_PULL_REQUEST_READBACK")
        pr_number = pr_head = None
    else:
        _exact(pr, {"number", "url", "base_branch", "head_branch", "head_sha", "closes_issue_number"}, errors, "UNEXPECTED_PULL_REQUEST_FIELD")
        pr_number, pr_head = pr.get("number"), pr.get("head_sha")
        _require(isinstance(pr_number, int) and pr_number > 0, errors, "INVALID_PULL_REQUEST_NUMBER")
        _require(isinstance(pr.get("url"), str) and URL.fullmatch(pr["url"]) is not None, errors, "INVALID_PULL_REQUEST_URL")
        _require(pr.get("base_branch") == value.get("default_branch"), errors, "PULL_REQUEST_BASE_MISMATCH")
        _require(pr.get("head_branch") == value.get("delivery_branch"), errors, "PULL_REQUEST_HEAD_BRANCH_MISMATCH")
        _require(isinstance(snapshot, dict) and pr_head == snapshot.get("docs_commit_sha"), errors, "PULL_REQUEST_HEAD_SHA_MISMATCH")
        _require(pr.get("closes_issue_number") == issue_number, errors, "PULL_REQUEST_CLOSING_KEYWORD_MISMATCH")
    ci = value.get("ci")
    if not isinstance(ci, dict):
        errors.append("INVALID_CI_READBACK")
    else:
        _exact(ci, {"head_sha", "required_checks"}, errors, "UNEXPECTED_CI_FIELD")
        _require(ci.get("head_sha") == pr_head, errors, "CI_HEAD_SHA_MISMATCH")
        checks = ci.get("required_checks")
        _require(isinstance(checks, list) and bool(checks), errors, "MISSING_REQUIRED_CHECKS")
        if isinstance(checks, list):
            names: list[str] = []
            for index, check in enumerate(checks):
                if not isinstance(check, dict):
                    errors.append(f"CI_CHECK_NOT_OBJECT:{index}")
                    continue
                _exact(check, {"name", "conclusion"}, errors, f"UNEXPECTED_CI_CHECK_FIELD:{index}")
                names.append(str(check.get("name")))
                _require(_non_empty(check.get("name")), errors, f"MISSING_CI_CHECK_NAME:{index}")
                _require(check.get("conclusion") in {"success", "neutral"}, errors, f"CI_CHECK_NOT_SUCCESSFUL:{index}")
            _require(len(names) == len(set(names)), errors, "DUPLICATE_CI_CHECK")
    merge = value.get("merge")
    if not isinstance(merge, dict):
        errors.append("INVALID_MERGE_READBACK")
    else:
        _exact(merge, {"merged", "merge_sha", "merged_pr_number", "head_sha"}, errors, "UNEXPECTED_MERGE_FIELD")
        _require(merge.get("merged") is True, errors, "PULL_REQUEST_NOT_MERGED")
        _require(_sha(merge.get("merge_sha")), errors, "INVALID_MERGE_SHA")
        _require(merge.get("merged_pr_number") == pr_number, errors, "MERGED_PULL_REQUEST_MISMATCH")
        _require(merge.get("head_sha") == pr_head, errors, "MERGED_HEAD_SHA_MISMATCH")
    readback = value.get("issue_readback")
    if not isinstance(readback, dict):
        errors.append("INVALID_ISSUE_READBACK")
    else:
        _exact(readback, {"state", "closed_by_pr_number"}, errors, "UNEXPECTED_ISSUE_READBACK_FIELD")
        _require(readback.get("state") == "closed", errors, "ISSUE_NOT_CLOSED")
        _require(readback.get("closed_by_pr_number") == pr_number, errors, "ISSUE_NOT_AUTO_CLOSED_BY_PR")
    return errors


def validate_restart(value: Any) -> list[str]:
    errors: list[str] = []
    if not isinstance(value, dict):
        return ["RESTART_NOT_OBJECT"]
    allowed = {"schema", "issue", "delivery_branch", "workspace", "marker_baseline_sha", "marker_snapshot_sha", "current_head_sha", "dirty_paths", "path_attribution", "recovery_task_id", "assignee", "active_recovery_task_ids", "allowed_scope", "implementation_card_schema", "required_checkpoint_schema"}
    _exact(value, allowed, errors, "UNEXPECTED_RESTART_FIELD")
    _require(value.get("schema") == "restart-task-v1", errors, "RESTART_SCHEMA")
    _require(isinstance(value.get("issue"), int) and value["issue"] > 0, errors, "INVALID_RESTART_ISSUE")
    _require(_non_empty(value.get("delivery_branch")), errors, "MISSING_RESTART_BRANCH")
    _require(_non_empty(value.get("workspace")), errors, "MISSING_RESTART_WORKSPACE")
    for field in ("marker_baseline_sha", "marker_snapshot_sha", "current_head_sha"):
        _require(_sha(value.get(field)), errors, f"INVALID_{field.upper()}")
    paths = value.get("dirty_paths")
    _require(_strings(paths), errors, "MISSING_DIRTY_PATHS")
    _require(
        isinstance(paths, list)
        and len(paths) == len(set(paths))
        and all(_literal_repository_path(path) for path in paths),
        errors,
        "INVALID_DIRTY_PATHS",
    )
    attribution = value.get("path_attribution")
    if not isinstance(attribution, list):
        errors.append("INVALID_PATH_ATTRIBUTION")
    else:
        attributed: list[str] = []
        for index, item in enumerate(attribution):
            if not isinstance(item, dict):
                errors.append(f"PATH_ATTRIBUTION_NOT_OBJECT:{index}")
                continue
            _exact(item, {"path", "issue", "evidence", "attributed"}, errors, f"UNEXPECTED_PATH_ATTRIBUTION_FIELD:{index}")
            attributed.append(str(item.get("path")))
            _require(item.get("issue") == value.get("issue"), errors, f"PATH_ISSUE_MISMATCH:{index}")
            _require(_non_empty(item.get("evidence")), errors, f"MISSING_PATH_EVIDENCE:{index}")
            _require(item.get("attributed") is True, errors, f"UNATTRIBUTED_DIRTY_PATH:{index}")
        _require(attributed == (paths or []), errors, "DIRTY_PATH_ATTRIBUTION_MISMATCH")
    _require(_task_id(value.get("recovery_task_id")), errors, "INVALID_RECOVERY_TASK_ID")
    _require(value.get("assignee") == "coder", errors, "INVALID_RECOVERY_ASSIGNEE")
    active = value.get("active_recovery_task_ids")
    _require(active == [value.get("recovery_task_id")], errors, "RESTART_TASK_CARDINALITY")
    _require(_strings(value.get("allowed_scope")), errors, "MISSING_RESTART_SCOPE")
    _require(value.get("implementation_card_schema") == "backend-implementation-card-v1", errors, "INVALID_RESTART_IMPLEMENTATION_CARD")
    _require(value.get("required_checkpoint_schema") == "backend-implementation-checkpoint-v1", errors, "INVALID_RESTART_CHECKPOINT")
    return errors


def validate_implementation_admission(value: Any) -> list[str]:
    errors: list[str] = []
    if not isinstance(value, dict):
        return ["IMPLEMENTATION_ADMISSION_NOT_OBJECT"]
    allowed = {"schema", "task_id", "issue", "workspace", "card_schema", "restart"}
    _exact(value, allowed, errors, "UNEXPECTED_IMPLEMENTATION_ADMISSION_FIELD")
    _require(value.get("schema") == "backend-implementation-admission-v1", errors, "IMPLEMENTATION_ADMISSION_SCHEMA")
    _require(_task_id(value.get("task_id")), errors, "INVALID_IMPLEMENTATION_ADMISSION_TASK")
    issue = value.get("issue")
    _require(
        isinstance(issue, int) and not isinstance(issue, bool) and issue > 0,
        errors,
        "INVALID_IMPLEMENTATION_ADMISSION_ISSUE",
    )
    _require(_non_empty(value.get("workspace")), errors, "MISSING_IMPLEMENTATION_ADMISSION_WORKSPACE")
    _require(
        value.get("card_schema") == "backend-implementation-card-v1",
        errors,
        "INVALID_IMPLEMENTATION_ADMISSION_CARD_SCHEMA",
    )
    restart = value.get("restart")
    if restart is not None:
        errors.extend(f"INVALID_RESTART:{error}" for error in validate_restart(restart))
        if isinstance(restart, dict):
            _require(restart.get("recovery_task_id") == value.get("task_id"), errors, "RESTART_ADMISSION_TASK_MISMATCH")
            _require(restart.get("issue") == issue, errors, "RESTART_ADMISSION_ISSUE_MISMATCH")
            _require(
                restart.get("workspace") == value.get("workspace"),
                errors,
                "RESTART_ADMISSION_WORKSPACE_MISMATCH",
            )
    return errors


def validate_base_sync(value: Any) -> list[str]:
    errors: list[str] = []
    if not isinstance(value, dict):
        return ["BASE_SYNC_NOT_OBJECT"]
    allowed = {"schema", "upstream_issue", "downstream_issue", "upstream_branch", "downstream_branch", "upstream_fixed_sha", "downstream_pre_sync_sha", "strategy", "strategy_owner", "strategy_rationale", "changed_paths", "conflicted_paths", "semantic_impact", "decision_task_id", "affected_downstream_task_ids", "post_sync_sha", "verification", "clean_worktree"}
    _exact(value, allowed, errors, "UNEXPECTED_BASE_SYNC_FIELD")
    _require(value.get("schema") == "base-sync-v1", errors, "BASE_SYNC_SCHEMA")
    for field in ("upstream_issue", "downstream_issue"):
        _require(isinstance(value.get(field), int) and value[field] > 0, errors, f"INVALID_{field.upper()}")
    _require(value.get("upstream_issue") != value.get("downstream_issue"), errors, "BASE_SYNC_SAME_ISSUE")
    for field in ("upstream_branch", "downstream_branch", "strategy_rationale"):
        _require(_non_empty(value.get(field)), errors, f"MISSING_{field.upper()}")
    for field in ("upstream_fixed_sha", "downstream_pre_sync_sha", "post_sync_sha"):
        _require(_sha(value.get(field)), errors, f"INVALID_{field.upper()}")
    _require(value.get("strategy") in {"merge", "rebase"}, errors, "INVALID_BASE_SYNC_STRATEGY")
    _require(value.get("strategy_owner") in {"user", "project-manager"}, errors, "INVALID_BASE_SYNC_STRATEGY_OWNER")
    _require(_strings(value.get("changed_paths")), errors, "MISSING_BASE_SYNC_CHANGED_PATHS")
    _require(_unique(value.get("conflicted_paths")), errors, "INVALID_CONFLICTED_PATHS")
    semantic = value.get("semantic_impact")
    _require(semantic in {"none", "compatible", "decision-required"}, errors, "INVALID_SEMANTIC_IMPACT")
    if semantic == "decision-required":
        _require(_task_id(value.get("decision_task_id")), errors, "MISSING_BASE_SYNC_DECISION_TASK")
    else:
        _require(value.get("decision_task_id") is None, errors, "UNEXPECTED_BASE_SYNC_DECISION_TASK")
    _require(_unique(value.get("affected_downstream_task_ids")), errors, "INVALID_AFFECTED_TASKS")
    if isinstance(value.get("affected_downstream_task_ids"), list):
        _require(all(_task_id(item) for item in value["affected_downstream_task_ids"]), errors, "INVALID_AFFECTED_TASK_ID")
    verification = value.get("verification")
    if not isinstance(verification, dict):
        errors.append("INVALID_BASE_SYNC_VERIFICATION")
    else:
        _exact(verification, {"executor", "checks", "result"}, errors, "UNEXPECTED_BASE_SYNC_VERIFICATION_FIELD")
        _require(verification.get("executor") == "gradle-mcp", errors, "INVALID_BASE_SYNC_EXECUTOR")
        _require(_strings(verification.get("checks")), errors, "MISSING_BASE_SYNC_CHECKS")
        _require(verification.get("result") == "pass", errors, "BASE_SYNC_VERIFICATION_NOT_PASS")
    _require(value.get("clean_worktree") is True, errors, "BASE_SYNC_WORKTREE_NOT_CLEAN")
    return errors


def validate_release_finding(value: Any) -> list[str]:
    errors: list[str] = []
    if not isinstance(value, dict):
        return ["RELEASE_FINDING_NOT_OBJECT"]
    allowed = {"schema", "source", "source_url", "target_pr", "target_head_sha", "finding_id", "classification", "evidence", "routed_task_id", "idempotency_key"}
    _exact(value, allowed, errors, "UNEXPECTED_RELEASE_FINDING_FIELD")
    _require(value.get("schema") == "release-finding-v1", errors, "RELEASE_FINDING_SCHEMA")
    _require(value.get("source") in {"ci", "pr-review"}, errors, "INVALID_RELEASE_FINDING_SOURCE")
    _require(isinstance(value.get("source_url"), str) and URL.fullmatch(value["source_url"]) is not None, errors, "INVALID_RELEASE_FINDING_URL")
    _require(isinstance(value.get("target_pr"), int) and value["target_pr"] > 0, errors, "INVALID_RELEASE_FINDING_PR")
    _require(_sha(value.get("target_head_sha")), errors, "INVALID_RELEASE_FINDING_SHA")
    _require(_non_empty(value.get("finding_id")), errors, "MISSING_RELEASE_FINDING_ID")
    classification = value.get("classification")
    routed = {"implementation-rework", "context-required", "decision-required"}
    _require(classification in routed | {"infrastructure-retry", "no-action"}, errors, "INVALID_RELEASE_FINDING_CLASSIFICATION")
    _require(_strings(value.get("evidence")), errors, "MISSING_RELEASE_FINDING_EVIDENCE")
    if classification in routed:
        _require(_task_id(value.get("routed_task_id")), errors, "MISSING_RELEASE_FINDING_ROUTE")
    else:
        _require(value.get("routed_task_id") is None, errors, "UNEXPECTED_RELEASE_FINDING_ROUTE")
    _require(_non_empty(value.get("idempotency_key")), errors, "MISSING_RELEASE_FINDING_IDEMPOTENCY")
    return errors


def _read(path: Path) -> Any:
    def no_duplicates(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
        result: dict[str, Any] = {}
        for key, value in pairs:
            if key in result:
                raise ValueError(f"DUPLICATE_JSON_KEY:{key}")
            result[key] = value
        return result
    return json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=no_duplicates)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    state = commands.add_parser("validate-state")
    state.add_argument("graph", type=Path)
    state.add_argument("board", type=Path)
    state.add_argument("git", type=Path)
    review = commands.add_parser("validate-review-result")
    review.add_argument("result", type=Path)
    review.add_argument("graph", type=Path)
    for name in ("summary-admission", "release", "restart", "implementation-admission", "base-sync", "release-finding"):
        command = commands.add_parser(f"validate-{name}")
        command.add_argument("input", type=Path)
    args = parser.parse_args()
    try:
        decision = None
        if args.command == "validate-state":
            errors, decision = validate_state(_read(args.graph), _read(args.board), _read(args.git))
        elif args.command == "validate-review-result":
            errors = validate_review_result(_read(args.result), _read(args.graph))
        else:
            validator = {
                "validate-summary-admission": validate_summary_admission,
                "validate-release": validate_release,
                "validate-restart": validate_restart,
                "validate-implementation-admission": validate_implementation_admission,
                "validate-base-sync": validate_base_sync,
                "validate-release-finding": validate_release_finding,
            }[args.command]
            errors = validator(_read(args.input))
    except (OSError, json.JSONDecodeError, TypeError, ValueError) as exc:
        print(json.dumps({"valid": False, "errors": [f"READ_INPUT_FAILED:{exc}"]}, ensure_ascii=False))
        return 2
    payload: dict[str, Any] = {"valid": not errors, "errors": errors}
    if decision is not None:
        payload["decision"] = decision
    print(json.dumps(payload, ensure_ascii=False))
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
