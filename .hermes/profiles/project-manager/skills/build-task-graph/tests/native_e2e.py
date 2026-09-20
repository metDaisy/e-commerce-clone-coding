#!/usr/bin/env python3
"""격리 board에서 build-task-graph native lifecycle을 검증한다."""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
import uuid
from pathlib import Path
from typing import Any

SKILL_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SKILL_DIR / "scripts"))
from build_task_graph import validate  # noqa: E402
from graph_contract import validate_graph  # noqa: E402


class NativeE2E:
    def __init__(self, profile: str, repository: Path) -> None:
        self.profile = profile
        self.repository = repository
        self.board = f"btg-e2e-{uuid.uuid4().hex[:8]}"

    def run(self, *args: str, json_output: bool = False) -> Any:
        completed = subprocess.run(
            ["hermes", "-p", self.profile, "kanban", *args],
            cwd=self.repository,
            check=False,
            text=True,
            encoding="utf-8",
            capture_output=True,
        )
        if completed.returncode != 0:
            raise RuntimeError(f"kanban command failed ({completed.returncode}): {completed.stderr or completed.stdout}")
        return json.loads(completed.stdout) if json_output else completed.stdout.strip()

    def board_run(self, *args: str, json_output: bool = False) -> Any:
        return self.run("--board", self.board, *args, json_output=json_output)

    @staticmethod
    def task_id(payload: dict[str, Any]) -> str:
        task = payload.get("task", payload)
        value = task.get("id")
        if not isinstance(value, str):
            raise RuntimeError("create response has no task id")
        return value

    def create(
        self,
        title: str,
        assignee: str,
        body: dict[str, Any] | str,
        *,
        parents: list[str] | None = None,
        idempotency_key: str | None = None,
    ) -> str:
        body_text = json.dumps(body, ensure_ascii=False) if isinstance(body, dict) else body
        args = ["create", title, "--assignee", assignee, "--body", body_text, "--workspace", "scratch"]
        for parent in parents or []:
            args.extend(["--parent", parent])
        if idempotency_key:
            args.extend(["--idempotency-key", idempotency_key])
        args.append("--json")
        return self.task_id(self.board_run(*args, json_output=True))

    def show(self, task_id: str) -> dict[str, Any]:
        return self.board_run("show", task_id, "--json", json_output=True)

    @staticmethod
    def status(envelope: dict[str, Any]) -> str:
        return str(envelope["task"]["status"])

    def graph_wrapper(
        self,
        fixture: dict[str, Any],
        ids: dict[str, str],
    ) -> dict[str, Any]:
        reverse = {task_id: key for key, task_id in ids.items()}
        graph = json.loads(json.dumps(fixture))
        records = {card["key"]: card for card in graph["cards"]}
        for key, task_id in ids.items():
            envelope = self.show(task_id)
            task = envelope["task"]
            record = records[key]
            record["status"] = task["status"]
            record["assignee"] = task["assignee"]
            native_parents = [parent["id"] if isinstance(parent, dict) else parent for parent in envelope["parents"]]
            record["parents"] = [reverse[parent] for parent in native_parents]
            if key != "triage":
                record["body"] = json.loads(task["body"])
        return graph

    def execute(self) -> dict[str, Any]:
        fixture = json.loads((SKILL_DIR / "tests" / "fixtures" / "valid-graph-draft.json").read_text(encoding="utf-8"))
        cards = {card["key"]: card for card in fixture["cards"]}
        board_created = False
        try:
            self.run("boards", "create", self.board, "--name", "build-task-graph native E2E")
            board_created = True
            triage = self.create(cards["triage"]["title"], cards["triage"]["assignee"], "frozen triage")
            self.board_run("claim", triage, "--ttl", "120")
            impl = self.create(cards["impl-1"]["title"], cards["impl-1"]["assignee"], cards["impl-1"]["body"], parents=[triage])
            review1 = self.create(cards["review-1"]["title"], cards["review-1"]["assignee"], cards["review-1"]["body"], parents=[impl])
            summary = self.create(cards["summary"]["title"], cards["summary"]["assignee"], cards["summary"]["body"], parents=[impl, review1])
            ids = {"triage": triage, "impl-1": impl, "review-1": review1, "summary": summary}

            draft_errors = validate_graph(self.graph_wrapper(fixture, ids), phase="draft", validate_implementation=validate)
            if draft_errors:
                raise RuntimeError(f"draft graph invalid: {draft_errors}")
            self.board_run("complete", "--force", triage, "--result", "graph read-back verified")
            native_errors = validate_graph(self.graph_wrapper(fixture, ids), phase="native", validate_implementation=validate)
            if native_errors:
                raise RuntimeError(f"native graph invalid: {native_errors}")

            self.board_run("claim", impl, "--ttl", "120")
            self.board_run("complete", "--force", impl, "--result", "implementation checkpoint verified")
            self.board_run("claim", review1, "--ttl", "120")
            finding_metadata = {
                "findings": [
                    {
                        "finding_id": "F-1",
                        "verdict": "correction-required",
                        "basis": "behavior-product-create",
                        "observed_fact": "회귀 시나리오 보강이 필요하다.",
                        "evidence": ["test:missing-regression"],
                        "impact": "aggregate acceptance",
                    }
                ]
            }
            self.board_run(
                "complete",
                "--force",
                review1,
                "--result",
                "correction required",
                "--metadata",
                json.dumps(finding_metadata, ensure_ascii=False),
            )
            review_envelope = self.show(review1)
            latest_metadata = review_envelope["runs"][-1]["metadata"]
            if latest_metadata != finding_metadata:
                raise RuntimeError("review finding metadata read-back mismatch")
            if not any(event.get("kind") == "completed" for event in review_envelope["events"]):
                raise RuntimeError("review completion event was not read back")

            corrective_body = json.loads(json.dumps(cards["impl-1"]["body"]))
            corrective_body["goal"] = "기존 상품 등록 동작의 회귀 시나리오를 보강한다."
            corrective = self.create(
                "G1-Issue138-Impl2",
                "implementation-coder",
                corrective_body,
                parents=[review1],
                idempotency_key="review-1:F-1:correction",
            )
            recovered = self.create(
                "G1-Issue138-Impl2",
                "implementation-coder",
                corrective_body,
                parents=[review1],
                idempotency_key="review-1:F-1:correction",
            )
            if recovered != corrective:
                raise RuntimeError("idempotent recovery created a duplicate card")
            review2_body = json.loads(json.dumps(cards["review-1"]["body"]))
            review2_body["implementation_card_keys"] = ["impl-1", "impl-2"]
            review2 = self.create(
                "G1-Issue138-Review2",
                "reviewer",
                review2_body,
                parents=[corrective],
                idempotency_key="review-1:F-1:review",
            )
            self.board_run("link", corrective, summary)
            self.board_run("link", review2, summary)
            if self.status(self.show(summary)) != "todo":
                raise RuntimeError("Summary remained dispatchable after corrective parents were linked")

            review_graph = json.loads(json.dumps(fixture))
            review_graph["mode"] = "review-rework"
            review_graph["lineage"] = {
                "prior_generation": 1,
                "prior_summary": summary,
                "archived_unfinished_keys": [],
            }
            review_graph["source_review"] = {
                "review_key": "review-1",
                "dispositions": [
                    {
                        "finding_id": "F-1",
                        "verdict": "correction-required",
                        "appended_card_keys": ["impl-2", "review-2"],
                    }
                ],
                "idempotency": {
                    "impl-2": "review-1:F-1:correction",
                    "review-2": "review-1:F-1:review",
                },
            }
            review_graph["behaviors"][0]["implementation_card_key"] = "impl-2"
            review_graph["ready_candidate"] = "impl-2"
            summary_card = next(card for card in review_graph["cards"] if card["key"] == "summary")
            summary_card["parents"] = ["impl-1", "review-1", "impl-2", "review-2"]
            review_graph["cards"].insert(
                -1,
                {
                    "key": "impl-2",
                    "title": "G1-Issue138-Impl2",
                    "card_type": "implementation",
                    "assignee": "implementation-coder",
                    "status": "ready",
                    "parents": ["review-1"],
                    "body": corrective_body,
                },
            )
            review_graph["cards"].insert(
                -1,
                {
                    "key": "review-2",
                    "title": "G1-Issue138-Review2",
                    "card_type": "review",
                    "assignee": "reviewer",
                    "status": "todo",
                    "parents": ["impl-2"],
                    "body": review2_body,
                },
            )
            rework_ids = {**ids, "impl-2": corrective, "review-2": review2}
            rework_errors = validate_graph(
                self.graph_wrapper(review_graph, rework_ids),
                phase="native",
                validate_implementation=validate,
            )
            if rework_errors:
                raise RuntimeError(f"review-rework graph invalid: {rework_errors}")

            self.board_run("claim", corrective, "--ttl", "120")
            self.board_run("complete", "--force", corrective, "--result", "correction verified")
            self.board_run("claim", review2, "--ttl", "120")
            resolution_metadata = {
                "findings": [
                    {
                        "finding_id": "F-2",
                        "basis": "Review2 재검사",
                        "observed_fact": "F-1의 corrective implementation이 확인되었다.",
                        "evidence": ["native corrective run completion"],
                        "impact": "F-1은 더 이상 blocking이 아니다.",
                        "verdict": "resolved",
                        "resolves": {"source_review_id": review1, "finding_id": "F-1"},
                    }
                ]
            }
            self.board_run(
                "complete",
                "--force",
                review2,
                "--result",
                "all findings resolved",
                "--metadata",
                json.dumps(resolution_metadata, ensure_ascii=False),
            )
            if self.show(review2)["runs"][-1]["metadata"] != resolution_metadata:
                raise RuntimeError("review finding resolution provenance read-back mismatch")
            if self.status(self.show(summary)) != "ready":
                raise RuntimeError("Summary was not promoted after every direct parent completed")

            obsolete = self.create("G1-Issue138-Impl99", "implementation-coder", corrective_body)
            self.board_run("claim", obsolete, "--ttl", "120")
            self.board_run("block", obsolete, "requirement superseded", "--kind", "dependency")
            blocked = self.show(obsolete)
            last_run = blocked["runs"][-1]
            if self.status(blocked) not in {"todo", "blocked"} or last_run["ended_at"] is None or last_run["outcome"] != "blocked":
                raise RuntimeError("running worker termination was not read back before archive")
            self.board_run("archive", obsolete)
            if self.status(self.show(obsolete)) != "archived":
                raise RuntimeError("obsolete card archive was not read back")

            return {
                "draft_validation": "pass",
                "native_validation": "pass",
                "triage_promotion": "pass",
                "review_rework_native_validation": "pass",
                "review_completion_event_and_finding_readback": "pass",
                "idempotent_recovery": "pass",
                "summary_admission_frontier": "pass",
                "running_worker_termination_and_archive": "pass",
            }
        finally:
            if board_created:
                self.run("boards", "rm", "--delete", self.board)
                boards = self.run("boards", "list", "--json", "--all", json_output=True)
                slugs = {board.get("slug") for board in boards if isinstance(board, dict)}
                if self.board in slugs:
                    raise RuntimeError("E2E board cleanup read-back failed")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--profile", default="project-manager")
    parser.add_argument("--repository", type=Path, default=Path.cwd())
    args = parser.parse_args()
    try:
        result = NativeE2E(args.profile, args.repository.resolve()).execute()
    except (OSError, RuntimeError, TypeError, KeyError, subprocess.SubprocessError, json.JSONDecodeError) as exc:
        print(json.dumps({"valid": False, "error": str(exc)}, ensure_ascii=False))
        return 1
    print(json.dumps({"valid": True, "checks": result}, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
