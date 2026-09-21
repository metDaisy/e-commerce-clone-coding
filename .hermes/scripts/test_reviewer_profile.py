import importlib.util
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
REVIEWER = ROOT / ".hermes" / "profiles" / "reviewer"
WORKFLOW = ROOT / ".hermes" / "profiles" / "project-manager" / "skills" / "run-workflow" / "scripts" / "workflow.py"


def _load_workflow():
    spec = importlib.util.spec_from_file_location("pm_workflow", WORKFLOW)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


def test_reviewer_distribution_owns_complete_runtime_contract() -> None:
    manifest = (REVIEWER / "distribution.yaml").read_text(encoding="utf-8")
    for relative in (
        "SOUL.md",
        "README.md",
        "capabilities.yaml",
        "skills/run-aggregate-review/SKILL.md",
        "skills/run-aggregate-review/README.md",
        "skills/run-aggregate-review/references/aggregate-review-contract.md",
    ):
        assert f"  - {relative}" in manifest
        assert (REVIEWER / relative).is_file()


def test_reviewer_result_example_matches_pm_consumer_validator() -> None:
    contract = (
        REVIEWER
        / "skills"
        / "run-aggregate-review"
        / "references"
        / "aggregate-review-contract.md"
    ).read_text(encoding="utf-8")
    match = re.search(r"```json\n(.*?)\n```", contract, re.DOTALL)
    assert match is not None
    result = json.loads(match.group(1))
    graph = {
        "generation": 1,
        "cards": [
            {
                "key": "review-1",
                "card_type": "review",
                "body": {"implementation_card_keys": ["impl-1"]},
            }
        ],
    }

    assert _load_workflow().validate_review_result(result, graph) == []


def test_reviewer_capabilities_are_read_focused_and_have_no_github_write() -> None:
    policy = (REVIEWER / "capabilities.yaml").read_text(encoding="utf-8")
    assert "run-aggregate-review" in policy
    assert "gradle-mcp" in policy
    assert "hermes-agent" in policy
    assert "github-mcp" not in policy
    assert "plugins:" not in policy


if __name__ == "__main__":
    test_reviewer_distribution_owns_complete_runtime_contract()
    test_reviewer_result_example_matches_pm_consumer_validator()
    test_reviewer_capabilities_are_read_focused_and_have_no_github_write()
    print("reviewer profile tests: passed")
