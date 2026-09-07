#!/usr/bin/env python3
"""Read-only validator for Amaazon board-contract-v1."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Callable, Iterable


CONTRACT_VERSION = "board-contract-v1"
TASK_ID = re.compile(r"^t_[0-9a-f]+$")
SHA = re.compile(r"^[0-9a-f]{40}$")
FQCN = re.compile(r"^(?:[a-z_][A-Za-z0-9_]*\.)+[A-Z][A-Za-z0-9_]*(?:#[A-Za-z_$][A-Za-z0-9_$]*)?$")
TASK_TYPES = {
    "reconciliation",
    "investigation",
    "implementation",
    "commit",
    "quality-review",
    "coordinator",
    "pr-create",
    "finalization",
}
TASK_STATUSES = {"triage", "todo", "ready", "running", "review", "blocked", "scheduled", "done", "archived"}
WORKSPACE_KINDS = {"dir", "scratch", "worktree"}
HUMAN_KEYS = {"claim", "outcome", "subject", "evidence", "residual_risk", "EXPECT"}


@dataclass(frozen=True)
class Finding:
    code: str
    task_id: str | None
    detail: str


def _field(body: str, name: str) -> str | None:
    match = re.search(rf"(?m)^{re.escape(name)}:\s*(.+)$", body)
    return match.group(1).strip() if match else None


def _refs(values: Iterable[object] | None) -> set[str]:
    result: set[str] = set()
    for value in values or []:
        result.add(str(value.get("id")) if isinstance(value, dict) else str(value))
    return result


def _successful_run(run: object) -> bool:
    if not isinstance(run, dict):
        return False
    values = [run.get("status"), run.get("outcome"), run.get("result")]
    normalized = {str(value).lower() for value in values if isinstance(value, str)}
    return bool(normalized & {"success", "succeeded", "passed", "completed"})


def _section(body: str, name: str) -> list[str]:
    lines = body.splitlines()
    start = next((index for index, line in enumerate(lines) if line == f"{name}:"), None)
    if start is None:
        return []
    result: list[str] = []
    for line in lines[start + 1 :]:
        if line and not line.startswith((" ", "-")) and re.match(r"^[A-Za-z_][A-Za-z0-9_-]*:", line):
            break
        result.append(line)
    return result


def _structured_entries(body: str, section: str, id_key: str) -> dict[str, dict[str, str]]:
    entries: dict[str, dict[str, str]] = {}
    current: dict[str, str] | None = None
    for line in _section(body, section):
        match = re.match(rf"^- {re.escape(id_key)}:\s*(\S+)\s*$", line)
        if match:
            current = {id_key: match.group(1)}
            entries[match.group(1)] = current
            continue
        match = re.match(r"^\s{2}([A-Za-z_][A-Za-z0-9_-]*):\s*(.+)$", line)
        if match and current is not None:
            current[match.group(1)] = match.group(2).strip()
    return entries


def _named_blocks(body: str, section: str) -> dict[str, dict[str, str]]:
    entries: dict[str, dict[str, str]] = {}
    current: dict[str, str] | None = None
    for line in _section(body, section):
        name = re.match(r"^  ([A-Za-z_][A-Za-z0-9_-]*):\s*$", line)
        if name:
            current = {}
            entries[name.group(1)] = current
            continue
        value = re.match(r"^    ([A-Za-z_][A-Za-z0-9_-]*):\s*(.+)$", line)
        if value and current is not None:
            current[value.group(1)] = value.group(2).strip()
    return entries


def _flat_block(body: str, section: str) -> dict[str, str]:
    entries: dict[str, str] = {}
    for line in _section(body, section):
        value = re.match(r"^  ([A-Za-z_][A-Za-z0-9_-]*):\s*(.+)$", line)
        if value:
            entries[value.group(1)] = value.group(2).strip()
    return entries


def _source_entries(body: str) -> list[dict[str, str] | str]:
    sources: list[dict[str, str] | str] = []
    current: dict[str, str] | None = None
    in_source = False
    for line in _section(body, "evidence"):
        if re.match(r"^  - type:\s*", line):
            current = None
            in_source = False
            continue
        legacy = re.match(r"^    source:\s*(\S.+)$", line)
        if legacy:
            sources.append(legacy.group(1).strip())
            current = None
            in_source = False
            continue
        if line == "    source:":
            current = {}
            sources.append(current)
            in_source = True
            continue
        value = re.match(r"^      ([a-z_]+):\s*(.+)$", line)
        if in_source and current is not None and value:
            current[value.group(1)] = value.group(2).strip()
        elif line.startswith("    ") and not line.startswith("      "):
            in_source = False
    return sources


def _normalize_heading(value: str) -> str:
    return re.sub(r"[`*_]", "", value).strip()


def _validate_locator(
    task_id: str,
    source: dict[str, str] | str,
    planning_sha: str | None,
    task_ids: set[str],
    source_loader: Callable[[str, str], str],
) -> list[Finding]:
    if isinstance(source, str):
        return [Finding("LOCATOR_FORMAT", task_id, "legacy source string")]
    kind = source.get("kind")
    if kind == "issue":
        url = source.get("url", "")
        if not url.startswith(("https://", "http://")):
            return [Finding("LOCATOR_FORMAT", task_id, "issue source")]
        return []
    if kind == "task":
        reference = source.get("task_id", "")
        if not TASK_ID.fullmatch(reference) or reference not in task_ids:
            return [Finding("TASK_REFERENCE_NOT_FOUND", task_id, reference)]
        return []
    if kind != "repository":
        return [Finding("LOCATOR_FORMAT", task_id, "unknown source kind")]
    path = source.get("path", "")
    heading = source.get("heading")
    line_range = source.get("lines")
    if not path or bool(heading) == bool(line_range):
        return [Finding("LOCATOR_FORMAT", task_id, "repository source")]
    line_match = re.fullmatch(r"(\d+)-(\d+)", line_range or "")
    if line_range and not line_match:
        return [Finding("LOCATOR_FORMAT", task_id, "repository line range")]
    if not planning_sha or not SHA.fullmatch(planning_sha):
        return [Finding("PLANNING_SHA_INVALID", task_id, str(planning_sha))]
    try:
        content = source_loader(planning_sha, path)
    except (FileNotFoundError, subprocess.CalledProcessError):
        return [Finding("LOCATOR_PATH_NOT_FOUND", task_id, path)]
    if heading:
        expected = _normalize_heading(heading)
        headings = {
            _normalize_heading(match.group(1))
            for match in re.finditer(r"(?m)^#{1,6}\s+(.+?)\s*$", content)
        }
        if expected not in headings:
            return [Finding("LOCATOR_NOT_FOUND", task_id, path)]
    else:
        assert line_match is not None
        start, end = int(line_match.group(1)), int(line_match.group(2))
        lines = content.splitlines()
        if start < 1 or end < start or end > len(lines) or not any(line.strip() for line in lines[start - 1 : end]):
            return [Finding("LOCATOR_NOT_FOUND", task_id, path)]
    return []


def _human_text_findings(task_id: str, title: str, body: str) -> list[Finding]:
    findings: list[Finding] = []
    if not re.search(r"[가-힣]", title):
        findings.append(Finding("HUMAN_TEXT_NOT_KOREAN", task_id, "title"))
    active_section: str | None = None
    for line in body.splitlines():
        if line in {"scope:", "out_of_scope:", "acceptance_criteria:"}:
            active_section = line[:-1]
            continue
        if line and not line.startswith((" ", "-")):
            active_section = None
        value: str | None = None
        key_match = re.match(r"^\s*([A-Za-z_][A-Za-z0-9_-]*):\s*(.+)$", line)
        if key_match and key_match.group(1) in HUMAN_KEYS:
            value = key_match.group(2)
        elif active_section in {"scope", "out_of_scope"} and line.startswith("- "):
            value = line[2:]
        machine_locator = bool(value and re.fullmatch(r"[a-z_][A-Za-z0-9_]*(?:\.[A-Za-z0-9_]+)+", value))
        if value and not machine_locator and re.search(r"[A-Za-z]{4}", value) and not re.search(r"[가-힣]", value):
            findings.append(Finding("HUMAN_TEXT_NOT_KOREAN", task_id, key_match.group(1) if key_match else active_section or "text"))
    return findings


def _verification_findings(task_id: str, body: str, task_type: str | None) -> list[Finding]:
    findings: list[Finding] = []
    workspace_path = _field(body, "workspace_path")
    checks = _structured_entries(body, "verification", "check_id")
    criteria = _structured_entries(body, "acceptance_criteria", "id")
    coverage = _structured_entries(body, "acceptance_coverage", "acceptance_id")
    if not checks:
        findings.append(Finding("CHECK_SCHEMA_INVALID", task_id, "verification.check_id missing"))
        return findings
    for check_id, check in checks.items():
        kind = check.get("kind")
        command = check.get("CHECK", "")
        expect = check.get("EXPECT", "")
        if kind not in {"behavioral", "structural", "style", "state", "manual"}:
            findings.append(Finding("CHECK_KIND_INVALID", task_id, check_id))
        if not command or not expect:
            findings.append(Finding("CHECK_SCHEMA_INVALID", task_id, check_id))
            continue
        if "process exit code 0" not in expect.lower():
            findings.append(Finding("EXPECT_SUCCESS_CONDITION_MISSING", task_id, check_id))
        if re.search(r"(?:관련|필요한|적절한|등을|확인한다|검증한다)", command):
            findings.append(Finding("CHECK_PROSE_NOT_EXECUTABLE", task_id, check_id))
        if "runner=gradle-mcp" in command:
            parts = dict(
                part.strip().split("=", 1)
                for part in command.split(";")
                if "=" in part
            )
            tests = [value.strip() for value in parts.get("tests", "").split(",") if value.strip()]
            if not parts.get("tasks") or not parts.get("CWD") or not tests or any(not FQCN.fullmatch(value) for value in tests):
                findings.append(Finding("GRADLE_CHECK_INVALID", task_id, check_id))
            if workspace_path and parts.get("CWD") != workspace_path:
                findings.append(Finding("CHECK_WORKSPACE_MISMATCH", task_id, check_id))
            if any(value.startswith("planned-") or "-" in value for value in tests):
                findings.append(Finding("GRADLE_TEST_PLACEHOLDER", task_id, check_id))
            if kind == "behavioral" and tests and all("ModularityTest" in value for value in tests):
                findings.append(Finding("CHECK_KIND_MISMATCH", task_id, check_id))
        elif re.match(r"^(?:git|gh|hermes)\s", command):
            if kind == "behavioral":
                findings.append(Finding("CHECK_KIND_MISMATCH", task_id, check_id))
            git_cwd = re.match(r"^git\s+-C\s+(\S+)", command)
            if git_cwd and workspace_path and git_cwd.group(1) != workspace_path:
                findings.append(Finding("CHECK_WORKSPACE_MISMATCH", task_id, check_id))
        else:
            findings.append(Finding("SHELL_CHECK_INVALID", task_id, check_id))
    if task_type == "implementation":
        if not criteria or not coverage:
            findings.append(Finding("IMPLEMENTATION_BEHAVIOR_COVERAGE", task_id, "criteria or coverage missing"))
        else:
            behavioral = {key for key, value in checks.items() if value.get("kind") == "behavioral" and "ModularityTest" not in value.get("CHECK", "")}
            for criterion in criteria:
                mapped = {value.strip() for value in coverage.get(criterion, {}).get("check_ids", "").split(",") if value.strip()}
                if not mapped or not mapped <= set(checks) or not mapped & behavioral:
                    findings.append(Finding("IMPLEMENTATION_BEHAVIOR_COVERAGE", task_id, criterion))
    return findings


def validate_board(
    envelopes: list[dict],
    source_loader: Callable[[str, str], str],
    *,
    phase: str = "post",
) -> list[Finding]:
    findings: list[Finding] = []
    if phase not in {"draft", "post"}:
        return [Finding("VALIDATION_PHASE_INVALID", None, phase)]
    if not isinstance(envelopes, list):
        return [Finding("INPUT_SCHEMA_INVALID", None, "board must be a list")]
    if not envelopes:
        return [Finding("EMPTY_BOARD", None, "board has no tasks")]
    malformed = [
        str(index)
        for index, item in enumerate(envelopes)
        if not isinstance(item, dict)
        or not isinstance(item.get("task"), dict)
        or not isinstance(item["task"].get("id"), str)
    ]
    if malformed:
        return [Finding("INPUT_SCHEMA_INVALID", None, ",".join(malformed))]
    all_ids = [item["task"]["id"] for item in envelopes]
    duplicate_ids = sorted({task_id for task_id in all_ids if all_ids.count(task_id) > 1})
    if duplicate_ids:
        return [Finding("DUPLICATE_TASK_ID", None, ",".join(duplicate_ids))]
    records = {item["task"]["id"]: item["task"] for item in envelopes}
    task_ids = set(records)
    parents = {item["task"]["id"]: _refs(item.get("parents")) for item in envelopes}
    children = {item["task"]["id"]: _refs(item.get("children")) for item in envelopes}

    for task_id in task_ids:
        if not TASK_ID.fullmatch(task_id):
            findings.append(Finding("TASK_ID_INVALID", task_id, task_id))
        for parent in parents[task_id]:
            if parent not in task_ids or task_id not in children.get(parent, set()):
                findings.append(Finding("DEPENDENCY_ASYMMETRIC", task_id, f"{parent}->{task_id}"))
        for child in children[task_id]:
            if child not in task_ids or task_id not in parents.get(child, set()):
                findings.append(Finding("DEPENDENCY_ASYMMETRIC", task_id, f"{task_id}->{child}"))

    memo: dict[str, int] = {}
    visiting: set[str] = set()

    def path_length(task_id: str) -> int:
        if task_id in memo:
            return memo[task_id]
        if task_id in visiting:
            findings.append(Finding("DEPENDENCY_CYCLE", task_id, task_id))
            return 0
        visiting.add(task_id)
        known_parents = [parent for parent in parents[task_id] if parent in task_ids]
        result = 0 if not known_parents else 1 + max(path_length(parent) for parent in known_parents)
        visiting.remove(task_id)
        memo[task_id] = result
        return result

    for task_id in task_ids:
        path_length(task_id)

    ancestor_memo: dict[str, set[str]] = {}

    def ancestors(task_id: str) -> set[str]:
        if task_id not in ancestor_memo:
            found: set[str] = set()
            pending = list(parents[task_id])
            while pending:
                candidate = pending.pop()
                if candidate in found or candidate not in task_ids:
                    continue
                found.add(candidate)
                pending.extend(parents[candidate])
            found.discard(task_id)
            ancestor_memo[task_id] = found
        return ancestor_memo[task_id]

    ready = [task_id for task_id, task in records.items() if task.get("status") == "ready"]
    expected_ready = 0 if phase == "draft" else 1
    if len(ready) != expected_ready:
        findings.append(Finding("READY_COUNT_INVALID", None, f"expected={expected_ready},actual={len(ready)}"))
    for task_id in ready:
        unsatisfied = [parent for parent in parents[task_id] if records.get(parent, {}).get("status") != "done"]
        if unsatisfied:
            findings.append(Finding("READY_DEPENDENCY_UNSATISFIED", task_id, ",".join(sorted(unsatisfied))))

    stale = False
    for item in envelopes:
        task = item["task"]
        task_id = task["id"]
        body = task.get("body", "")
        task_type = _field(body, "task_type")
        planning_sha = _field(body, "planning_head_sha")
        current_sha = _field(body, "current_state_sha")
        freshness = _field(body, "state_freshness")
        stale = stale or freshness == "stale" or (bool(current_sha and planning_sha) and current_sha != planning_sha)
        required = {
            "contract_version": CONTRACT_VERSION,
            "task_type": None,
            "issue": None,
            "issue_url": None,
            "current_state_sha": None,
            "planning_head_sha": None,
            "state_freshness": None,
            "decomposition_depth": None,
            "dependency_path_length": None,
            "workspace_kind": None,
            "workspace_path": None,
            "assignee": None,
        }
        for name, expected in required.items():
            value = _field(body, name)
            if value is None or (expected is not None and value != expected):
                findings.append(Finding("BODY_SCHEMA_MISSING", task_id, name))
        if task_type not in TASK_TYPES:
            findings.append(Finding("TASK_TYPE_INVALID", task_id, str(task_type)))
        if task.get("status") not in TASK_STATUSES:
            findings.append(Finding("TASK_STATUS_INVALID", task_id, "native status"))
        if phase == "draft" and task.get("status") not in {"todo", "blocked"}:
            findings.append(Finding("DRAFT_STATUS_INVALID", task_id, "draft tasks must be todo or blocked"))
        if freshness not in {"fresh", "stale"}:
            findings.append(Finding("STATE_FRESHNESS_INVALID", task_id, str(freshness)))
        expected_freshness = "stale" if current_sha and planning_sha and current_sha != planning_sha else "fresh"
        if freshness in {"fresh", "stale"} and freshness != expected_freshness:
            findings.append(Finding("STATE_FRESHNESS_MISMATCH", task_id, expected_freshness))
        if _field(body, "assignee") != task.get("assignee"):
            findings.append(Finding("ASSIGNEE_MISMATCH", task_id, "body/native assignee differ"))
        for section_name in ("evidence", "scope", "out_of_scope", "acceptance_criteria", "verification"):
            if f"{section_name}:" not in body:
                findings.append(Finding("BODY_SCHEMA_MISSING", task_id, section_name))
        evidence_types = re.findall(r"(?m)^  - type:\s*(goal|state|constraint)\s*$", body)
        evidence_sources = _source_entries(body)
        evidence_claims = re.findall(r"(?m)^    claim:\s*(.+)$", body)
        if not evidence_types or len(evidence_types) != len(evidence_sources) or len(evidence_types) != len(evidence_claims):
            findings.append(Finding("EVIDENCE_SCHEMA_INVALID", task_id, "type/source/claim cardinality"))
        if task_type == "implementation" and not {"goal", "state"} <= set(evidence_types):
            findings.append(Finding("IMPLEMENTATION_EVIDENCE_MISSING", task_id, "goal,state"))
        depth = _field(body, "decomposition_depth")
        if not depth or not depth.isdigit() or not 1 <= int(depth) <= 3:
            findings.append(Finding("DECOMPOSITION_DEPTH_INVALID", task_id, str(depth)))
        declared_path = _field(body, "dependency_path_length")
        if not declared_path or not declared_path.isdigit() or int(declared_path) != memo.get(task_id):
            findings.append(Finding("DEPENDENCY_PATH_MISMATCH", task_id, f"{declared_path}!={memo.get(task_id)}"))
        if "\\n" in body:
            findings.append(Finding("LITERAL_BACKSLASH_N", task_id, "body"))
        body_kind, body_path = _field(body, "workspace_kind"), _field(body, "workspace_path")
        if body_kind not in WORKSPACE_KINDS:
            findings.append(Finding("WORKSPACE_KIND_INVALID", task_id, str(body_kind)))
        if body_kind != task.get("workspace_kind") or body_path != task.get("workspace_path"):
            findings.append(Finding("WORKSPACE_MISMATCH", task_id, "body/native workspace differ"))
        if task.get("status") == "done" and not any(_successful_run(run) for run in item.get("runs", [])):
            findings.append(Finding("FALSE_DONE", task_id, "no successful task run"))
        for key in ("depends_on_task_id", "final_review_base_sha_producer_task_id"):
            reference = _field(body, key)
            if reference and (not TASK_ID.fullmatch(reference) or reference not in task_ids):
                findings.append(Finding("TASK_REFERENCE_INVALID", task_id, f"{key}={reference}"))
        declared_parents = {
            reference.strip()
            for reference in (_field(body, "depends_on_task_ids") or _field(body, "depends_on_task_id") or "").split(",")
            if reference.strip()
        }
        if declared_parents != parents[task_id]:
            findings.append(Finding("BODY_DEPENDENCY_MISMATCH", task_id, f"{sorted(declared_parents)}!={sorted(parents[task_id])}"))
        if task_type == "implementation" and "review_handoff_contract:" not in body:
            findings.append(Finding("REVIEW_HANDOFF_CONTRACT_MISSING", task_id, "review_handoff_contract"))
        if task_type == "implementation":
            handoff = _flat_block(body, "review_handoff_contract")
            required_handoff = {"action", "required_metadata", "reviewer_readback"}
            if not required_handoff <= set(handoff) or handoff.get("action") != "request-review":
                findings.append(Finding("REVIEW_HANDOFF_CONTRACT_INVALID", task_id, "required fields"))
        if task_type in {"quality-review", "coordinator"}:
            quality_producer = _field(body, "final_review_base_sha_producer_task_id")
            if not quality_producer:
                findings.append(Finding("QUALITY_FREEZE_PRODUCER_MISSING", task_id, "producer task id"))
            elif quality_producer not in ancestors(task_id):
                findings.append(Finding("QUALITY_FREEZE_PRODUCER_NOT_ANCESTOR", task_id, quality_producer))
            manual = _flat_block(body, "manual_review_contract")
            required_manual = {"reviewer", "subject", "evidence", "verdict_values"}
            if not required_manual <= set(manual) or manual.get("verdict_values") != "approved | request-changes | blocked":
                findings.append(Finding("MANUAL_REVIEW_CONTRACT_MISSING", task_id, "required fields"))
        if not item.get("runs") and re.search(r"(?m)^review_handoff:\s*$", body):
            findings.append(Finding("PREEXEC_REVIEW_EVIDENCE", task_id, "review_handoff"))
        if not item.get("runs") and re.search(r"(?m)^manual_review:\s*$", body):
            findings.append(Finding("PREEXEC_MANUAL_REVIEW_EVIDENCE", task_id, "manual_review"))
        bindings = _named_blocks(body, "runtime_bindings")
        placeholders = set(re.findall(r"\$\{([A-Za-z_][A-Za-z0-9_]*)\}", body))
        for placeholder in placeholders - set(bindings):
            findings.append(Finding("RUNTIME_BINDING_MISSING", task_id, placeholder))
        for name, binding in bindings.items():
            producer = binding.get("producer_task_id", "")
            evidence = binding.get("evidence", "")
            if not producer or not evidence:
                findings.append(Finding("RUNTIME_BINDING_INVALID", task_id, name))
                continue
            if producer in {"self", task_id}:
                findings.append(Finding("RUNTIME_BINDING_SELF_PRODUCER", task_id, "self"))
                continue
            if producer not in task_ids:
                findings.append(Finding("RUNTIME_BINDING_PRODUCER_NOT_FOUND", task_id, producer))
                continue
            if producer not in ancestors(task_id):
                findings.append(Finding("RUNTIME_BINDING_PRODUCER_NOT_ANCESTOR", task_id, producer))
            if name in {"pr_number", "pr_url"}:
                producer_body = records[producer].get("body", "")
                produced = _named_blocks(producer_body, "produces")
                if (
                    _field(producer_body, "task_type") != "pr-create"
                    or "gh pr create" not in producer_body
                    or not {"pr_number", "pr_url"} <= set(produced)
                ):
                    findings.append(Finding("PR_PRODUCER_CONTRACT_MISSING", task_id, producer))
        for source in evidence_sources:
            findings.extend(_validate_locator(task_id, source, planning_sha, task_ids, source_loader))
        findings.extend(_human_text_findings(task_id, task.get("title", ""), body))
        findings.extend(_verification_findings(task_id, body, task_type))

    if stale:
        if len(envelopes) != 1 or _field(envelopes[0]["task"].get("body", ""), "task_type") not in {"reconciliation", "investigation"}:
            findings.append(Finding("STALE_GRAPH_NOT_TWO_PHASE", None, "stale board must contain one reconciliation/investigation task"))

    return sorted(findings, key=lambda value: (value.task_id or "", value.code, value.detail))


def _parse_json_stream(raw: str) -> list[dict]:
    try:
        value = json.loads(raw)
        return value if isinstance(value, list) else [value]
    except json.JSONDecodeError:
        decoder = json.JSONDecoder()
        values: list[dict] = []
        position = 0
        while True:
            start = raw.find("{", position)
            if start < 0:
                break
            value, end = decoder.raw_decode(raw[start:])
            values.append(value)
            position = start + end
        if not values:
            raise
        return values


def _run(command: list[str], cwd: Path) -> str:
    return subprocess.run(command, cwd=cwd, check=True, text=True, encoding="utf-8", capture_output=True).stdout


def collect_board(profile: str, board: str, repository: Path) -> list[dict]:
    base = ["hermes", "--profile", profile, "kanban", "--board", board]
    listed = _parse_json_stream(_run(base + ["list", "--json"], repository))
    envelopes = []
    for task in listed:
        envelopes.append(json.loads(_run(base + ["show", task["id"], "--json"], repository)))
    if len(envelopes) != len(listed):
        raise RuntimeError("partial board read-back")
    return envelopes


def git_source_loader(repository: Path) -> Callable[[str, str], str]:
    def load(sha: str, path: str) -> str:
        return _run(["git", "show", f"{sha}:{path}"], repository)
    return load


def _safe_output(value: str | None) -> str | None:
    if value is None:
        return None
    sanitized = re.sub(r"https?://\S+", "<url>", value)
    sanitized = re.sub(r"[A-Za-z]:[/\\][^\s,\]]+", "<absolute-path>", sanitized)
    sanitized = re.sub(
        r"(?i)(credential|token|password|api[_-]?key|connection[_-]?string)\s*[=:]\s*\S+",
        r"\1=[REDACTED]",
        sanitized,
    )
    return sanitized[:160]


def _finding_payload(finding: Finding) -> dict[str, str | None]:
    return {
        "code": finding.code,
        "task_id": _safe_output(finding.task_id),
        "detail": _safe_output(finding.detail),
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--board")
    parser.add_argument("--profile", default="project-manager")
    parser.add_argument("--repository", type=Path, default=Path.cwd())
    parser.add_argument("--input", type=Path, help="Validate saved show-envelope JSON instead of reading Kanban")
    parser.add_argument("--phase", choices=("draft", "post"), help="Default: draft for --input, post for --board")
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args(argv)
    if not args.board and not args.input:
        parser.error("--board or --input is required")
    try:
        envelopes = json.loads(args.input.read_text(encoding="utf-8")) if args.input else collect_board(args.profile, args.board, args.repository)
        phase = args.phase or ("draft" if args.input else "post")
        findings = validate_board(envelopes, git_source_loader(args.repository), phase=phase)
    except (KeyError, TypeError, OSError, subprocess.CalledProcessError, RuntimeError, json.JSONDecodeError) as error:
        print(f"infrastructure error: {error}", file=sys.stderr)
        return 2
    report = {
        "contract_version": CONTRACT_VERSION,
        "board": args.board,
        "task_count": len(envelopes),
        "valid": not findings,
        "finding_count": len(findings),
        "phase": phase,
        "findings": [_finding_payload(finding) for finding in findings],
    }
    if args.json:
        print(json.dumps(report, ensure_ascii=False, indent=2))
    else:
        print(f"{CONTRACT_VERSION}: {'PASS' if not findings else 'FAIL'} ({len(findings)} findings)")
        for finding in findings:
            print(f"- {finding.code} [{_safe_output(finding.task_id) or 'board'}]: {_safe_output(finding.detail)}")
    return 0 if not findings else 1


if __name__ == "__main__":
    raise SystemExit(main())
