#!/usr/bin/env python3
"""그래프 초안과 requirement-rework 비교 산출물을 검증·생성한다."""

from __future__ import annotations

import hashlib
import json
import re
import subprocess
from pathlib import Path
from typing import Any, Callable

GRAPH_SCHEMA = "build-task-graph-v1"
CARD_SCHEMAS = {
    "review": "aggregate-review-card-v1",
    "summary": "issue-summary-card-v1",
    "decision": "policy-decision-card-v1",
}
TITLE = re.compile(r"^G(?P<generation>[1-9]\d*)-Issue(?P<issue>[1-9]\d*)-(?P<kind>Triage|Impl[1-9]\d*|Review[1-9]\d*|Summary|Decision[1-9]\d*)$")
SHA = re.compile(r"^[0-9a-f]{40}$")
STATES = {"implemented", "partial", "absent", "unknown"}
DISPOSITIONS = {"inherited", "planned", "blocked"}
CARD_TYPES = {"triage", "implementation", "review", "summary", "decision"}
STATUSES = {"running", "todo", "ready", "blocked", "done", "archived"}
TITLE_TYPES = {
    "Triage": "triage",
    "Summary": "summary",
}


def _non_empty(value: Any) -> bool:
    return isinstance(value, str) and bool(value.strip())


def _strings(value: Any) -> bool:
    return isinstance(value, list) and bool(value) and all(_non_empty(item) for item in value)


def _error(errors: list[str], condition: bool, code: str) -> None:
    if not condition:
        errors.append(code)


def _exact_fields(value: dict[str, Any], allowed: set[str], errors: list[str], prefix: str) -> None:
    for field in sorted(set(value) - allowed):
        errors.append(f"{prefix}:{field}")


def _validate_locator(locator: Any, errors: list[str], prefix: str) -> None:
    if not isinstance(locator, dict):
        errors.append(f"{prefix}_NOT_OBJECT")
        return
    _exact_fields(locator, {"path", "heading", "revision"}, errors, f"UNEXPECTED_{prefix}_FIELD")
    _error(errors, _non_empty(locator.get("path")), f"MISSING_{prefix}_PATH")
    _error(errors, _non_empty(locator.get("heading")), f"MISSING_{prefix}_HEADING")
    _error(errors, isinstance(locator.get("revision"), str) and bool(SHA.fullmatch(locator["revision"])), f"INVALID_{prefix}_REVISION")


def _validate_body(card: dict[str, Any], behavior_ids: set[str], errors: list[str]) -> None:
    key, card_type, body = card.get("key"), card.get("card_type"), card.get("body")
    if card_type == "triage":
        _error(errors, body is None, f"TRIAGE_BODY_MUST_BE_EXTERNAL:{key}")
        return
    if card_type == "implementation":
        _error(errors, isinstance(body, dict), f"MISSING_IMPLEMENTATION_BODY:{key}")
        if isinstance(body, dict):
            _error(errors, body.get("schema") == "backend-implementation-card-v1", f"IMPLEMENTATION_SCHEMA:{key}")
        return
    if not isinstance(body, dict):
        errors.append(f"MISSING_CARD_BODY:{key}")
        return
    _error(errors, body.get("schema") == CARD_SCHEMAS.get(card_type), f"CARD_BODY_SCHEMA:{key}")
    if card_type == "review":
        allowed = {"schema", "effective_behavior_ids", "implementation_card_keys", "inherited_behavior_ids", "aggregate_acceptance"}
        _exact_fields(body, allowed, errors, f"UNEXPECTED_REVIEW_BODY_FIELD:{key}")
        _error(errors, _strings(body.get("effective_behavior_ids")), f"MISSING_REVIEW_BEHAVIORS:{key}")
        _error(errors, isinstance(body.get("implementation_card_keys"), list), f"MISSING_REVIEW_IMPLS:{key}")
        _error(errors, isinstance(body.get("inherited_behavior_ids"), list), f"MISSING_REVIEW_INHERITED:{key}")
        _error(errors, _strings(body.get("aggregate_acceptance")), f"MISSING_REVIEW_ACCEPTANCE:{key}")
    elif card_type == "summary":
        allowed = {"schema", "effective_behavior_ids", "aggregate_acceptance", "finalization_checks"}
        _exact_fields(body, allowed, errors, f"UNEXPECTED_SUMMARY_BODY_FIELD:{key}")
        _error(errors, _strings(body.get("effective_behavior_ids")), f"MISSING_SUMMARY_BEHAVIORS:{key}")
        _error(errors, _strings(body.get("aggregate_acceptance")), f"MISSING_SUMMARY_ACCEPTANCE:{key}")
        _error(errors, _strings(body.get("finalization_checks")), f"MISSING_FINALIZATION_CHECKS:{key}")
    elif card_type == "decision":
        allowed = {"schema", "source_review_key", "finding_ids", "question", "decision_owner"}
        _exact_fields(body, allowed, errors, f"UNEXPECTED_DECISION_BODY_FIELD:{key}")
        _error(errors, _non_empty(body.get("source_review_key")), f"MISSING_DECISION_REVIEW:{key}")
        _error(errors, _strings(body.get("finding_ids")), f"MISSING_DECISION_FINDINGS:{key}")
        _error(errors, _non_empty(body.get("question")), f"MISSING_DECISION_QUESTION:{key}")
        _error(errors, body.get("decision_owner") == "user", f"INVALID_DECISION_OWNER:{key}")
    referenced = body.get("effective_behavior_ids", [])
    if isinstance(referenced, list):
        for behavior_id in referenced:
            if behavior_id not in behavior_ids:
                errors.append(f"UNKNOWN_BEHAVIOR:{key}:{behavior_id}")


def validate_graph(graph: Any, *, phase: str = "draft", validate_implementation: Callable[[Any], list[str]] | None = None) -> list[str]:
    """결정론적으로 graph wrapper와 persisted card body를 검증한다."""
    if phase not in {"draft", "native"}:
        return ["INVALID_PHASE"]
    errors: list[str] = []
    if not isinstance(graph, dict):
        return ["GRAPH_NOT_OBJECT"]
    allowed = {"schema", "mode", "issue", "generation", "requirement_basis", "revised_requirement", "lineage", "source_review", "behaviors", "cards", "ready_candidate", "archived_card_keys"}
    _exact_fields(graph, allowed, errors, "UNEXPECTED_GRAPH_FIELD")
    _error(errors, graph.get("schema") == GRAPH_SCHEMA, "GRAPH_SCHEMA")
    mode = graph.get("mode")
    _error(errors, mode in {"new", "requirement-rework", "review-rework"}, "INVALID_MODE")
    issue = graph.get("issue")
    issue_number = issue.get("number") if isinstance(issue, dict) else None
    if isinstance(issue, dict):
        _exact_fields(issue, {"number", "url"}, errors, "UNEXPECTED_ISSUE_FIELD")
    _error(errors, isinstance(issue_number, int) and not isinstance(issue_number, bool) and issue_number > 0, "INVALID_ISSUE")
    issue_url = issue.get("url") if isinstance(issue, dict) else None
    _error(errors, isinstance(issue_url, str) and bool(re.fullmatch(r"https://[^/]+/.+", issue_url)), "INVALID_ISSUE_URL")
    generation = graph.get("generation")
    _error(errors, isinstance(generation, int) and not isinstance(generation, bool) and generation > 0, "INVALID_GENERATION")
    if mode == "new":
        _error(errors, generation == 1, "NEW_REQUIRES_GENERATION_1")
        _error(errors, graph.get("revised_requirement") is None, "NEW_FORBIDS_REVISED_REQUIREMENT")
        _error(errors, graph.get("lineage") is None, "NEW_FORBIDS_LINEAGE")
        _error(errors, graph.get("source_review") is None, "NEW_FORBIDS_SOURCE_REVIEW")
    elif mode == "requirement-rework":
        _error(errors, isinstance(generation, int) and generation > 1, "REWORK_REQUIRES_NEXT_GENERATION")
        _validate_locator(graph.get("revised_requirement"), errors, "REVISED_REQUIREMENT")
        lineage = graph.get("lineage")
        if not isinstance(lineage, dict):
            errors.append("MISSING_REWORK_LINEAGE")
        else:
            _exact_fields(lineage, {"prior_generation", "prior_summary", "archived_unfinished_keys"}, errors, "UNEXPECTED_LINEAGE_FIELD")
            _error(errors, isinstance(generation, int) and lineage.get("prior_generation") == generation - 1, "REWORK_GENERATION_DISCONTINUITY")
            _error(errors, _non_empty(lineage.get("prior_summary")), "MISSING_PRIOR_SUMMARY")
            _error(errors, isinstance(lineage.get("archived_unfinished_keys"), list), "MISSING_ARCHIVED_LINEAGE")
        _error(errors, graph.get("source_review") is None, "REQUIREMENT_REWORK_FORBIDS_SOURCE_REVIEW")
    else:
        _error(errors, graph.get("revised_requirement") is None, "REVIEW_REWORK_FORBIDS_REQUIREMENT_REVISION")
        lineage = graph.get("lineage")
        if not isinstance(lineage, dict):
            errors.append("MISSING_REVIEW_REWORK_LINEAGE")
        else:
            _exact_fields(lineage, {"prior_generation", "prior_summary", "archived_unfinished_keys"}, errors, "UNEXPECTED_LINEAGE_FIELD")
            _error(errors, lineage.get("prior_generation") == generation, "REVIEW_REWORK_GENERATION_MISMATCH")
            _error(errors, _non_empty(lineage.get("prior_summary")), "MISSING_PRIOR_SUMMARY")
        source_review = graph.get("source_review")
        if not isinstance(source_review, dict):
            errors.append("MISSING_SOURCE_REVIEW")
        else:
            _exact_fields(source_review, {"review_key", "dispositions", "idempotency"}, errors, "UNEXPECTED_SOURCE_REVIEW_FIELD")
            _error(errors, _non_empty(source_review.get("review_key")), "MISSING_SOURCE_REVIEW_KEY")
            dispositions = source_review.get("dispositions")
            _error(errors, isinstance(dispositions, list) and bool(dispositions), "MISSING_SOURCE_DISPOSITIONS")
            finding_ids: set[str] = set()
            if isinstance(dispositions, list):
                for index, disposition in enumerate(dispositions):
                    if not isinstance(disposition, dict):
                        errors.append(f"SOURCE_DISPOSITION_NOT_OBJECT:{index}")
                        continue
                    _exact_fields(disposition, {"finding_id", "verdict", "appended_card_keys"}, errors, f"UNEXPECTED_SOURCE_DISPOSITION_FIELD:{index}")
                    finding_id = disposition.get("finding_id")
                    _error(errors, _non_empty(finding_id), f"MISSING_SOURCE_FINDING_ID:{index}")
                    if finding_id in finding_ids:
                        errors.append(f"DUPLICATE_SOURCE_FINDING_ID:{finding_id}")
                    if isinstance(finding_id, str):
                        finding_ids.add(finding_id)
                    _error(errors, disposition.get("verdict") in {"correction-required", "context-required", "decision-required"}, f"INVALID_SOURCE_VERDICT:{finding_id}")
                    _error(errors, _strings(disposition.get("appended_card_keys")), f"MISSING_DISPOSITION_CARDS:{finding_id}")
            idempotency = source_review.get("idempotency")
            _error(errors, isinstance(idempotency, dict) and bool(idempotency), "MISSING_IDEMPOTENCY_MAP")
            if isinstance(idempotency, dict):
                _error(errors, all(_non_empty(key) and _non_empty(value) for key, value in idempotency.items()), "INVALID_IDEMPOTENCY_MAP")
                _error(errors, len(set(idempotency.values())) == len(idempotency), "DUPLICATE_IDEMPOTENCY_KEY")
    _validate_locator(graph.get("requirement_basis"), errors, "REQUIREMENT_BASIS")

    behaviors = graph.get("behaviors")
    behavior_ids: set[str] = set()
    if not isinstance(behaviors, list) or not behaviors:
        errors.append("MISSING_BEHAVIORS")
    else:
        for index, behavior in enumerate(behaviors):
            if not isinstance(behavior, dict):
                errors.append(f"BEHAVIOR_NOT_OBJECT:{index}")
                continue
            _exact_fields(behavior, {"id", "outcome", "state", "disposition", "evidence", "implementation_card_key"}, errors, f"UNEXPECTED_BEHAVIOR_FIELD:{index}")
            identifier = behavior.get("id")
            _error(errors, _non_empty(identifier), f"MISSING_BEHAVIOR_ID:{index}")
            if identifier in behavior_ids:
                errors.append(f"DUPLICATE_BEHAVIOR_ID:{identifier}")
            if isinstance(identifier, str):
                behavior_ids.add(identifier)
            state, disposition = behavior.get("state"), behavior.get("disposition")
            _error(errors, state in STATES, f"INVALID_BEHAVIOR_STATE:{identifier}")
            _error(errors, disposition in DISPOSITIONS, f"INVALID_BEHAVIOR_DISPOSITION:{identifier}")
            _error(errors, _non_empty(behavior.get("outcome")), f"MISSING_BEHAVIOR_OUTCOME:{identifier}")
            evidence = behavior.get("evidence")
            if state == "implemented":
                _error(errors, disposition == "inherited", f"IMPLEMENTED_NOT_INHERITED:{identifier}")
                _error(errors, _strings(evidence), f"MISSING_INHERITED_EVIDENCE:{identifier}")
            elif state in {"partial", "absent"}:
                _error(errors, disposition == "planned", f"WORK_NOT_PLANNED:{identifier}")
                _error(errors, _non_empty(behavior.get("implementation_card_key")), f"MISSING_BEHAVIOR_IMPL:{identifier}")
            elif state == "unknown":
                _error(errors, disposition == "blocked", f"UNKNOWN_NOT_BLOCKED:{identifier}")

    cards = graph.get("cards")
    if not isinstance(cards, list) or not cards:
        return errors + ["MISSING_CARDS"]
    records: dict[str, dict[str, Any]] = {}
    titles: set[str] = set()
    for index, card in enumerate(cards):
        if not isinstance(card, dict):
            errors.append(f"CARD_NOT_OBJECT:{index}")
            continue
        _exact_fields(card, {"key", "title", "card_type", "assignee", "status", "parents", "body"}, errors, f"UNEXPECTED_CARD_FIELD:{index}")
        key = card.get("key")
        if not _non_empty(key):
            errors.append(f"MISSING_CARD_KEY:{index}")
            continue
        if key in records:
            errors.append(f"DUPLICATE_CARD_KEY:{key}")
        records[key] = card
        match = TITLE.fullmatch(str(card.get("title", "")))
        title = card.get("title")
        if title in titles:
            errors.append(f"DUPLICATE_CARD_TITLE:{title}")
        if isinstance(title, str):
            titles.add(title)
        if not match:
            errors.append(f"INVALID_TITLE:{key}")
        elif int(match.group("generation")) != generation or int(match.group("issue")) != issue_number:
            errors.append(f"TITLE_LINEAGE_MISMATCH:{key}")
        elif TITLE_TYPES.get(match.group("kind"), "implementation" if match.group("kind").startswith("Impl") else "review" if match.group("kind").startswith("Review") else "decision") != card.get("card_type"):
            errors.append(f"TITLE_CARD_TYPE_MISMATCH:{key}")
        _error(errors, card.get("card_type") in CARD_TYPES, f"INVALID_CARD_TYPE:{key}")
        _error(errors, _non_empty(card.get("assignee")), f"MISSING_ASSIGNEE:{key}")
        _error(errors, card.get("status") in STATUSES, f"INVALID_CARD_STATUS:{key}")
        _error(errors, isinstance(card.get("parents"), list), f"INVALID_PARENTS:{key}")
        _validate_body(card, behavior_ids, errors)
        if card.get("card_type") == "implementation" and isinstance(card.get("body"), dict) and validate_implementation:
            errors.extend(f"IMPLEMENTATION:{key}:{failure}" for failure in validate_implementation(card["body"]))

    triages = [key for key, card in records.items() if card.get("card_type") == "triage"]
    summaries = [key for key, card in records.items() if card.get("card_type") == "summary"]
    reviews = [key for key, card in records.items() if card.get("card_type") == "review"]
    _error(errors, len(triages) == 1, "TRIAGE_COUNT")
    _error(errors, len(summaries) == 1, "SUMMARY_COUNT")
    _error(errors, bool(reviews), "REVIEW_COUNT")
    for key, card in records.items():
        for parent in card.get("parents", []) if isinstance(card.get("parents"), list) else []:
            if parent not in records:
                errors.append(f"UNKNOWN_PARENT:{key}:{parent}")
            if parent == key:
                errors.append(f"SELF_PARENT:{key}")
    visiting: set[str] = set()
    visited: set[str] = set()

    def visit(key: str) -> None:
        if key in visiting:
            errors.append(f"DEPENDENCY_CYCLE:{key}")
            return
        if key in visited:
            return
        visiting.add(key)
        for parent in records[key].get("parents", []):
            if parent in records:
                visit(parent)
        visiting.remove(key)
        visited.add(key)

    for key in records:
        visit(key)
    if triages:
        _error(errors, records[triages[0]].get("parents") == [], "TRIAGE_HAS_PARENT")
    if summaries:
        summary_parents = set(records[summaries[0]].get("parents", []))
        required_parents = {key for key, card in records.items() if card.get("card_type") in {"implementation", "review", "decision"}}
        _error(errors, required_parents == summary_parents, "SUMMARY_MEMBERSHIP_MISMATCH")
    for key in reviews:
        _error(errors, bool(records[key].get("parents")), f"REVIEW_WITHOUT_PARENT:{key}")
    for key, card in records.items():
        parent_types = {records[parent].get("card_type") for parent in card.get("parents", []) if parent in records}
        card_type = card.get("card_type")
        if card_type == "implementation":
            _error(errors, bool(parent_types) and parent_types <= {"triage", "implementation", "review", "decision"}, f"INVALID_IMPL_PARENTS:{key}")
        elif card_type == "review":
            allowed_review_parents = {"implementation", "review", "decision"}
            if mode in {"new", "requirement-rework"}:
                allowed_review_parents.add("triage")
            _error(errors, bool(parent_types) and parent_types <= allowed_review_parents, f"INVALID_REVIEW_PARENTS:{key}")
        elif card_type == "decision":
            _error(errors, "review" in parent_types, f"DECISION_WITHOUT_REVIEW_PARENT:{key}")
    for impl_key, impl in records.items():
        if impl.get("card_type") != "implementation":
            continue
        child_reviews = [key for key in reviews if impl_key in records[key].get("parents", [])]
        _error(errors, bool(child_reviews), f"IMPLEMENTATION_WITHOUT_REVIEW_CHILD:{impl_key}")

    implemented_ids = {
        behavior.get("id")
        for behavior in behaviors or []
        if isinstance(behavior, dict) and behavior.get("state") == "implemented"
    }
    for behavior in behaviors or []:
        if not isinstance(behavior, dict) or behavior.get("state") not in {"partial", "absent"}:
            continue
        behavior_id = behavior.get("id")
        impl_key = behavior.get("implementation_card_key")
        impl = records.get(impl_key)
        _error(errors, impl is not None and impl.get("card_type") == "implementation", f"BEHAVIOR_IMPL_NOT_FOUND:{behavior_id}")
        if impl and isinstance(impl.get("body"), dict):
            outcomes = {
                item.get("id"): item.get("outcome")
                for item in impl["body"].get("effective_behavior", [])
                if isinstance(item, dict)
            }
            _error(errors, outcomes.get(behavior_id) == behavior.get("outcome"), f"BEHAVIOR_IMPL_MISMATCH:{behavior_id}")
    implementation_keys = {key for key, card in records.items() if card.get("card_type") == "implementation"}
    planned_impl_keys = {
        behavior.get("implementation_card_key")
        for behavior in behaviors or []
        if isinstance(behavior, dict) and behavior.get("disposition") == "planned"
    }
    if mode in {"new", "requirement-rework"}:
        _error(errors, implementation_keys == planned_impl_keys, "IMPLEMENTATION_BEHAVIOR_COVERAGE")
    numbered_reviews = [
        (int(match.group("kind")[6:]), key)
        for key in reviews
        if (match := TITLE.fullmatch(str(records[key].get("title", "")))) and match.group("kind").startswith("Review")
    ]
    latest_review = max(numbered_reviews, default=(0, None))[1]
    for key in reviews:
        body = records[key].get("body")
        if not isinstance(body, dict):
            continue
        for impl_key in body.get("implementation_card_keys", []):
            _error(errors, impl_key in records and records[impl_key].get("card_type") == "implementation", f"REVIEW_IMPL_NOT_FOUND:{key}:{impl_key}")
        for behavior_id in body.get("inherited_behavior_ids", []):
            _error(errors, behavior_id in implemented_ids, f"REVIEW_INHERITED_BEHAVIOR_INVALID:{key}:{behavior_id}")
        _error(errors, set(body.get("effective_behavior_ids", [])) == behavior_ids, f"REVIEW_BEHAVIOR_COVERAGE:{key}")
    if latest_review:
        latest_body = records[latest_review].get("body", {})
        _error(errors, set(latest_body.get("implementation_card_keys", [])) == implementation_keys, "LATEST_REVIEW_IMPL_COVERAGE")
        _error(errors, set(latest_body.get("inherited_behavior_ids", [])) == implemented_ids, "LATEST_REVIEW_INHERITED_COVERAGE")
    if summaries:
        summary_body = records[summaries[0]].get("body", {})
        if isinstance(summary_body, dict):
            _error(errors, set(summary_body.get("effective_behavior_ids", [])) == behavior_ids, "SUMMARY_BEHAVIOR_COVERAGE")

    ready_candidate = graph.get("ready_candidate")
    source_review = graph.get("source_review") if isinstance(graph.get("source_review"), dict) else {}
    dispositions = source_review.get("dispositions", []) if isinstance(source_review, dict) else []
    appended = {
        key
        for disposition in dispositions
        if isinstance(disposition, dict)
        for key in disposition.get("appended_card_keys", [])
    }
    appended_types = {records[key].get("card_type") for key in appended if key in records}
    decision_only = mode == "review-rework" and bool(appended) and appended_types == {"decision"}
    if mode == "review-rework":
        _error(errors, (ready_candidate is None) == decision_only, "REVIEW_REWORK_ACTIVATION_MISMATCH")
        _error(errors, set(source_review.get("idempotency", {})) == appended, "IDEMPOTENCY_CARD_COVERAGE")
        for disposition in dispositions:
            if not isinstance(disposition, dict):
                continue
            keys = disposition.get("appended_card_keys", [])
            types = {records[key].get("card_type") for key in keys if key in records}
            verdict = disposition.get("verdict")
            expected = {"implementation"} if verdict == "correction-required" else {"review"} if verdict == "context-required" else {"decision"}
            _error(errors, bool(types & expected), f"SOURCE_VERDICT_CARD_TYPE:{disposition.get('finding_id')}")
            if verdict == "decision-required":
                for key in keys:
                    card = records.get(key, {})
                    if card.get("card_type") != "decision" or not isinstance(card.get("body"), dict):
                        continue
                    _error(errors, card["body"].get("source_review_key") == source_review.get("review_key"), f"DECISION_SOURCE_REVIEW_MISMATCH:{key}")
                    _error(errors, disposition.get("finding_id") in card["body"].get("finding_ids", []), f"DECISION_SOURCE_FINDING_MISMATCH:{key}")
    _error(errors, decision_only or ready_candidate in records, "READY_CANDIDATE_NOT_FOUND")
    if ready_candidate in records and triages:
        candidate = records[ready_candidate]
        _error(errors, candidate.get("card_type") in {"implementation", "review"}, "READY_CANDIDATE_TYPE")
        if mode == "review-rework":
            source_key = source_review.get("review_key")
            _error(errors, source_key in candidate.get("parents", []), "READY_CANDIDATE_NOT_SOURCE_REVIEW_CHILD")
            _error(errors, ready_candidate in appended, "READY_CANDIDATE_NOT_APPENDED")
        else:
            _error(errors, candidate.get("parents") == [triages[0]], "READY_CANDIDATE_PARENT_SET")
            if planned_impl_keys:
                _error(errors, candidate.get("card_type") == "implementation", "READY_CANDIDATE_NOT_IMPL")
            else:
                _error(errors, candidate.get("card_type") == "review", "NO_IMPL_REQUIRES_REVIEW_CANDIDATE")
    ready = [key for key, card in records.items() if card.get("status") == "ready"]
    if phase == "draft":
        _error(errors, not ready, "DRAFT_HAS_READY_CARD")
        if triages:
            expected_triage = "done" if mode == "review-rework" else "running"
            _error(errors, records[triages[0]].get("status") == expected_triage, "DRAFT_TRIAGE_STATUS")
        source_key = source_review.get("review_key")
        for key, card in records.items():
            if key in triages or card.get("status") == "archived":
                continue
            historical = mode == "review-rework" and key not in appended and key != summaries[0]
            if historical:
                _error(errors, card.get("status") == "done", f"HISTORICAL_CARD_NOT_DONE:{key}")
                continue
            expected = {"todo", "blocked"} if card.get("card_type") == "decision" else {"todo"}
            _error(errors, card.get("status") in expected, f"DRAFT_ACTIVE_STATUS:{key}")
        if mode == "review-rework" and source_key in records:
            _error(errors, records[source_key].get("status") == "done", "SOURCE_REVIEW_NOT_DONE")
    else:
        expected_ready = [] if decision_only else [ready_candidate]
        _error(errors, ready == expected_ready, "NATIVE_READY_COUNT_OR_ID")
        if triages:
            _error(errors, records[triages[0]].get("status") == "done", "NATIVE_TRIAGE_NOT_DONE")
        source_key = source_review.get("review_key")
        for key, card in records.items():
            historical = mode == "review-rework" and key not in appended and key != summaries[0]
            if key in triages or key == ready_candidate or card.get("status") == "archived" or historical:
                if historical:
                    _error(errors, card.get("status") == "done", f"HISTORICAL_CARD_NOT_DONE:{key}")
                continue
            expected = {"todo", "blocked"} if card.get("card_type") == "decision" else {"todo"}
            _error(errors, card.get("status") in expected, f"NATIVE_ACTIVE_STATUS:{key}")
        if decision_only:
            _error(errors, not ready, "DECISION_ONLY_HAS_READY_CARD")
    if mode == "review-rework" and isinstance(graph.get("source_review"), dict):
        source_key = source_review.get("review_key")
        _error(errors, source_key in records and records[source_key].get("card_type") == "review", "SOURCE_REVIEW_NOT_FOUND")
        for key in appended:
            _error(errors, key in records, f"APPENDED_CARD_NOT_FOUND:{key}")
    archived = graph.get("archived_card_keys")
    _error(errors, isinstance(archived, list), "INVALID_ARCHIVED_CARD_KEYS")
    if isinstance(archived, list):
        for key in archived:
            card = records.get(key)
            _error(errors, card is not None, f"ARCHIVED_CARD_NOT_FOUND:{key}")
            if card:
                _error(errors, card.get("status") == "archived", f"ARCHIVE_STATUS_MISMATCH:{key}")
                _error(errors, card.get("card_type") in {"implementation", "review", "summary"}, f"ARCHIVE_TYPE_FORBIDDEN:{key}")
        for key, card in records.items():
            if card.get("status") == "archived" and key not in archived:
                errors.append(f"ARCHIVED_CARD_UNDECLARED:{key}")
    return errors


def _git(repository: Path, *args: str) -> str:
    return subprocess.run(["git", *args], cwd=repository, check=True, text=True, encoding="utf-8", capture_output=True).stdout


def requirement_diff(repository: Path, base: str, revised: str, paths: list[str]) -> dict[str, Any]:
    """Git을 변경하지 않고 stable hunk ID를 가진 비교 산출물을 만든다."""
    for revision in (base, revised):
        _git(repository, "rev-parse", "--verify", f"{revision}^{{commit}}")
    command = ["diff", "--find-renames", "--find-copies", "--unified=0", "--no-ext-diff", base, revised, "--", *paths]
    raw = _git(repository, *command)
    hunks: list[dict[str, Any]] = []
    old_path = new_path = ""
    current: dict[str, Any] | None = None
    for line in raw.splitlines():
        if line.startswith("diff --git "):
            parts = line.split(" ", 3)
            old_path, new_path = parts[2][2:], parts[3][2:]
            current = None
        elif line.startswith("rename from ") or line.startswith("copy from "):
            old_path = line.split(" ", 2)[2]
        elif line.startswith("rename to ") or line.startswith("copy to "):
            new_path = line.split(" ", 2)[2]
        elif line.startswith("@@ "):
            match = re.match(r"@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@", line)
            if not match:
                continue
            current = {
                "old_path": old_path,
                "new_path": new_path,
                "old_start": int(match.group(1)),
                "old_count": int(match.group(2) or "1"),
                "new_start": int(match.group(3)),
                "new_count": int(match.group(4) or "1"),
                "lines": [],
            }
            hunks.append(current)
        elif current is not None and line.startswith(("+", "-")) and not line.startswith(("+++", "---")):
            current["lines"].append(line)
    for hunk in hunks:
        canonical = json.dumps(hunk, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
        hunk["id"] = "h_" + hashlib.sha256(canonical.encode("utf-8")).hexdigest()[:16]
    return {
        "schema": "requirement-rework-comparison-v1",
        "provenance": {"base_revision": base, "revised_revision": revised, "paths": paths, "git_args": command},
        "hunks": hunks,
        "behavior_deltas": [],
        "behavior_delta_status": "human-review-required",
    }
