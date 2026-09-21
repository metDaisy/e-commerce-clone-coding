import importlib.util
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
REVIEWER = ROOT / ".hermes" / "profiles" / "reviewer"
WORKFLOW = ROOT / ".hermes" / "profiles" / "project-manager" / "skills" / "run-workflow" / "scripts" / "workflow.py"

REVIEW_SKILLS = (
    "run-review",
    "review-spec",
    "review-maintainability",
    "review-persistence",
    "review-architecture",
    "review-evolution-compatibility",
    "improve-codebase-architecture",
)


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
        "skills/run-review/SKILL.md",
        "skills/run-review/README.md",
        "skills/run-review/references/aggregate-review-contract.md",
        "skills/run-review/references/axis-result-contract.md",
        "skills/run-review/references/code-discovery-guide.md",
        "skills/run-review/references/review-rubric-provenance.md",
        "skills/review-spec/SKILL.md",
        "skills/review-maintainability/SKILL.md",
        "skills/review-persistence/SKILL.md",
        "skills/review-architecture/SKILL.md",
        "skills/review-evolution-compatibility/SKILL.md",
        "skills/improve-codebase-architecture/SKILL.md",
    ):
        assert f"  - {relative}" in manifest
        assert (REVIEWER / relative).is_file()

    assert "run-aggregate-review" not in manifest
    assert not (REVIEWER / "skills" / "run-aggregate-review" / "SKILL.md").exists()


def test_reviewer_skills_have_valid_identity_and_capability_edges() -> None:
    policy = (REVIEWER / "capabilities.yaml").read_text(encoding="utf-8")
    manifest = (REVIEWER / "distribution.yaml").read_text(encoding="utf-8")

    for name in REVIEW_SKILLS:
        skill = (REVIEWER / "skills" / name / "SKILL.md").read_text(encoding="utf-8")
        assert skill.startswith("---\n")
        assert f"name: {name}\n" in skill
        description = re.search(r'^description: "([^"]+)"$', skill, re.MULTILINE)
        assert description is not None
        assert len(description.group(1)) <= 60
        assert f"    - {name}\n" in policy
        assert f"  - skills/{name}/SKILL.md\n" in manifest

    root = (REVIEWER / "skills" / "run-review" / "SKILL.md").read_text(encoding="utf-8")
    for leaf in REVIEW_SKILLS[1:6]:
        assert leaf in root
    assert "finding은 입력으로 전달하지 않는다" in root
    assert "Cross-cutting security baseline" in root
    assert "auto-fix하지 않는다" in root
    assert "`skill_view`로 정확한 leaf Skill 이름을 로드한다" in root
    assert "invocation packet" in root
    assert "다섯 raw result가 모두 닫힌 뒤에만" in root


def test_reviewer_result_example_matches_pm_consumer_validator() -> None:
    contract = (
        REVIEWER
        / "skills"
        / "run-review"
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

    assert _load_workflow().validate_review_result(result, graph, result["prior_findings"]) == []


def test_reviewer_capabilities_are_read_focused_and_have_no_github_write() -> None:
    policy = (REVIEWER / "capabilities.yaml").read_text(encoding="utf-8")
    for name in REVIEW_SKILLS:
        assert name in policy
    assert "run-aggregate-review" not in policy
    assert "gradle-mcp" in policy
    assert "hermes-agent" in policy
    assert "github-mcp" not in policy
    assert "plugins:" not in policy


def test_architecture_review_is_requirement_scoped_and_uses_deepening_helper() -> None:
    architecture = (
        REVIEWER / "skills" / "review-architecture" / "SKILL.md"
    ).read_text(encoding="utf-8")
    helper = (
        REVIEWER / "skills" / "improve-codebase-architecture" / "SKILL.md"
    ).read_text(encoding="utf-8")

    assert "improve-codebase-architecture" in architecture
    assert "requirement 영향 closure" in architecture
    assert "repository-audit" in architecture
    assert "별도 architecture-audit task" in architecture
    assert "ApplicationModules.verify()" in architecture
    assert "allowedDependencies" in architecture
    assert "checkpoint" in helper
    assert "repository-audit" in helper
    assert "Aggregate Review 중 스스로 이 mode로 전환하지 않는다" in helper


def test_axis_rubrics_embed_selected_review_guidance_without_runtime_dependency() -> None:
    spec = (REVIEWER / "skills" / "review-spec" / "SKILL.md").read_text(encoding="utf-8")
    maintainability = (
        REVIEWER / "skills" / "review-maintainability" / "SKILL.md"
    ).read_text(encoding="utf-8")
    persistence = (
        REVIEWER / "skills" / "review-persistence" / "SKILL.md"
    ).read_text(encoding="utf-8")
    architecture = (
        REVIEWER / "skills" / "review-architecture" / "SKILL.md"
    ).read_text(encoding="utf-8")
    evolution = (
        REVIEWER / "skills" / "review-evolution-compatibility" / "SKILL.md"
    ).read_text(encoding="utf-8")
    provenance = (
        REVIEWER
        / "skills"
        / "run-review"
        / "references"
        / "review-rubric-provenance.md"
    ).read_text(encoding="utf-8")

    assert "red-capable scenario" in spec
    assert "observable behavior" in spec
    for term in ("Reuse", "Quality", "Efficiency", "Altitude", "confidence", "RISKY"):
        assert term in maintainability
    for term in ("save/find/delete", "zero-query", "non-overlapping", "QueryInspector"):
        assert term in persistence
    for term in ("named-interface query", "fact event/projection", "Internal entity"):
        assert term in architecture
    for term in ("producer/new consumer", "versioning rule", "Internal JPA entity"):
        assert term in evolution
    for source in (
        "reviewer-quality-gates",
        "requesting-code-review",
        "simplify-code",
        "cross-domain-contract-planning",
    ):
        assert source in provenance
    assert "runtime dependency가 아니며" in provenance
    assert "dogfood" not in provenance.lower()
    assert "dogfood" not in (REVIEWER / "capabilities.yaml").read_text(encoding="utf-8").lower()


def test_leaf_skills_choose_discovery_tool_by_question_and_confirm_source() -> None:
    guide = (
        REVIEWER
        / "skills"
        / "run-review"
        / "references"
        / "code-discovery-guide.md"
    ).read_text(encoding="utf-8")
    axis_contract = (
        REVIEWER
        / "skills"
        / "run-review"
        / "references"
        / "axis-result-contract.md"
    ).read_text(encoding="utf-8")

    for term in (
        "Source-direct branch",
        "Semble branch",
        "Codebase Memory branch",
        "같은 질문을 Semble과 Codebase Memory 양쪽에 보내 비교하지 않는다",
        "index_status",
        "search_graph",
        "trace_path",
        "checked-out source",
    ):
        assert term in guide
    assert "code-discovery-guide.md" in axis_contract
    assert "같은 질문을 두 discovery tool에 반복하지 않는다" in axis_contract

    for leaf in REVIEW_SKILLS[1:6]:
        content = (REVIEWER / "skills" / leaf / "SKILL.md").read_text(encoding="utf-8")
        assert "## Code discovery 선택" in content
        assert "code-discovery-guide.md" in content
        assert "Semble branch" in content
        assert "Codebase Memory branch" in content
        assert "source" in content.lower()


if __name__ == "__main__":
    test_reviewer_distribution_owns_complete_runtime_contract()
    test_reviewer_skills_have_valid_identity_and_capability_edges()
    test_reviewer_result_example_matches_pm_consumer_validator()
    test_reviewer_capabilities_are_read_focused_and_have_no_github_write()
    test_architecture_review_is_requirement_scoped_and_uses_deepening_helper()
    test_axis_rubrics_embed_selected_review_guidance_without_runtime_dependency()
    test_leaf_skills_choose_discovery_tool_by_question_and_confirm_source()
    print("reviewer profile tests: passed")
