"""Focused tests for portable Hermes capability policy application."""

from __future__ import annotations

import importlib.util
from pathlib import Path


_SCRIPT = Path(__file__).parent / "apply-hermes-capabilities.py"
_SPEC = importlib.util.spec_from_file_location("apply_hermes_capabilities", _SCRIPT)
assert _SPEC and _SPEC.loader
_POLICY = importlib.util.module_from_spec(_SPEC)
_SPEC.loader.exec_module(_POLICY)


def _raw(
    enabled_plugins=None,
    allowed_skills=None,
    allowed_toolsets=None,
    allowed_servers=None,
    filters=None,
):
    entry = {
        "skills": {"allowed": allowed_skills or ["hermes-agent", "write-task"]},
        "tools": {
            "allowed_toolsets": allowed_toolsets or ["terminal", "file"],
            "platform_toolsets": {},
        },
        "approvals": {"mode": "smart"},
        "stt": None,
        "mcp": {
            "allowed_servers": allowed_servers or [],
            "filters": filters or {},
        },
    }
    if enabled_plugins is not None:
        entry["plugins"] = {"enabled": enabled_plugins}
    return {"version": 1, "profiles": {"project-manager": entry}}


def test_profile_expected_preserves_explicit_plugin_allowlist() -> None:
    expected = _POLICY.profile_expected(_raw(["agent-audit"]), "project-manager")

    assert expected["plugins_enabled"] == ["agent-audit"]
    assert expected["skills_allowed"] == ["hermes-agent", "write-task"]
    assert expected["toolsets_allowed"] == ["terminal", "file"]


def test_profile_expected_leaves_unspecified_plugins_untouched() -> None:
    expected = _POLICY.profile_expected(_raw(), "project-manager")

    assert expected["plugins_enabled"] is None


def test_apply_profile_sets_explicit_plugin_allowlist() -> None:
    writes = []
    original_set_config = _POLICY.set_config
    original_get_mcp_servers = _POLICY.get_mcp_servers
    original_verify = _POLICY.verify
    original_discover_skills = _POLICY.discover_skill_names
    original_discover_toolsets = _POLICY.discover_toolsets
    try:
        _POLICY.set_config = lambda profile, key, value, **_kwargs: writes.append((profile, key, value))
        _POLICY.get_mcp_servers = lambda _profile: {}
        _POLICY.verify = lambda _profile, _expected, _project_root=None: None

        _POLICY.discover_skill_names = lambda _profile, _cwd=None: {
            "hermes-agent",
            "write-task",
            "rss-feeds",
        }
        _POLICY.discover_toolsets = lambda _profile: {
            "terminal",
            "file",
            "web",
        }
        _POLICY.apply_profile(_raw(["agent-audit"]), "project-manager")
    finally:
        _POLICY.set_config = original_set_config
        _POLICY.get_mcp_servers = original_get_mcp_servers
        _POLICY.verify = original_verify
        _POLICY.discover_skill_names = original_discover_skills
        _POLICY.discover_toolsets = original_discover_toolsets

    assert ("project-manager", "plugins.enabled", ["agent-audit"]) in writes
    assert (
        "project-manager",
        "skills.disabled",
        ["rss-feeds"],
    ) in writes
    assert (
        "project-manager",
        "agent.disabled_toolsets",
        ["web"],
    ) in writes
    assert ("project-manager", "platform_toolsets", {}) in writes


def test_parse_skill_list_rejects_truncated_names() -> None:
    output = "│ very-long-skill… │ category │ builtin │ builtin │ enabled │"

    try:
        _POLICY.parse_skill_names(output)
    except ValueError as exc:
        assert "truncated" in str(exc)
    else:
        raise AssertionError("truncated skill names must fail closed")


def test_parse_toolset_list_reads_builtin_and_plugin_toolsets() -> None:
    output = """Built-in toolsets (cli):
  ✓ enabled  terminal  shell
  ✗ disabled web  browser
Plugin toolsets (cli):
  ✓ enabled  kanban  board
"""

    assert _POLICY.parse_toolset_names(output) == {"terminal", "web", "kanban"}


def test_verify_requires_empty_platform_toolsets_when_policy_has_none() -> None:
    reads = []
    original_get_json = _POLICY.get_json_config
    original_discover_skills = _POLICY.discover_skill_names
    original_discover_toolsets = _POLICY.discover_toolsets
    original_get_mcp = _POLICY.get_mcp_servers
    try:
        def get_json(_profile, key):
            reads.append(key)
            return {
                "skills.disabled": [],
                "agent.disabled_toolsets": [],
                "platform_toolsets": {},
                "approvals.mode": "smart",
            }[key]

        _POLICY.get_json_config = get_json
        _POLICY.discover_skill_names = lambda _profile, _cwd=None: {"hermes-agent"}
        _POLICY.discover_toolsets = lambda _profile: {"terminal"}
        _POLICY.get_mcp_servers = lambda _profile: {}
        _POLICY.verify(
            "coder",
            {
                "skills_allowed": ["hermes-agent"],
                "skills_disabled": [],
                "plugins_enabled": None,
                "toolsets_allowed": ["terminal"],
                "disabled_toolsets": [],
                "platform_toolsets": {},
                "approval": "smart",
                "stt_enabled": None,
                "allowed_servers": [],
                "filters": {},
            },
        )
    finally:
        _POLICY.get_json_config = original_get_json
        _POLICY.discover_skill_names = original_discover_skills
        _POLICY.discover_toolsets = original_discover_toolsets
        _POLICY.get_mcp_servers = original_get_mcp

    assert "platform_toolsets" in reads


def test_profile_expected_requires_mcp_include_filter_for_every_server() -> None:
    raw = _raw(allowed_servers=["codebase-memory"])

    try:
        _POLICY.profile_expected(raw, "project-manager")
    except ValueError as exc:
        assert "every allowed MCP server" in str(exc)
    else:
        raise AssertionError("unfiltered MCP server must fail")


def test_repository_policies_use_explicit_allowlists() -> None:
    root = Path(__file__).parents[2]
    profiles = ["project-manager", "coder"]
    raw = _POLICY.load_policy(root / ".hermes" / "profiles", profiles)

    for profile in profiles:
        expected = _POLICY.profile_expected(raw, profile)
        assert expected["skills_allowed"]
        assert expected["toolsets_allowed"]


def test_profile_installers_reference_current_profile_sources() -> None:
    scripts = Path(__file__).parent
    for name in ("install-profiles.sh", "install-profiles.ps1"):
        text = (scripts / name).read_text(encoding="utf-8")
        assert "profile-distributions" not in text
        assert "profile-capabilities" not in text
        assert ".hermes/profiles" in text.replace("\\", "/")


def test_profile_expected_requires_explicit_allowlists() -> None:
    raw = _raw()
    del raw["profiles"]["project-manager"]["skills"]["allowed"]

    try:
        _POLICY.profile_expected(raw, "project-manager")
    except ValueError as exc:
        assert "skills.allowed" in str(exc)
    else:
        raise AssertionError("missing Skill allowlist must fail")


def test_common_skills_are_project_owned() -> None:
    root = Path(__file__).parents[2]
    common = root / ".hermes" / "skills"
    for name in ("semble-search", "codebase-memory-mcp"):
        assert (common / name / "SKILL.md").is_file()
        for distribution in ("project-manager", "coder"):
            manifest = (common.parent / "profiles" / distribution / "distribution.yaml").read_text(
                encoding="utf-8"
            )
            assert f"skills/{name}/SKILL.md" not in manifest


def test_distribution_owns_runtime_assets_not_regression_tests() -> None:
    manifest = (Path(__file__).parents[2] / ".hermes" / "profiles" / "project-manager" / "distribution.yaml").read_text(
        encoding="utf-8"
    )

    assert "skills/service-planning/plan.md" not in manifest
    assert "skills/sync-docs/plan.md" not in manifest
    assert "skills/service-planning/README.md" not in manifest
    assert "skills/sync-docs/README.md" not in manifest
    assert "skills/create-triage/scripts/test_triage.py" not in manifest
    assert "skills/build-task-graph/tests/test_build_task_graph.py" not in manifest
    assert "skills/run-workflow/tests/test_workflow.py" not in manifest
    assert "skills/update-current-state/tests/test_current_state.py" not in manifest
    assert "skills/build-task-graph/tests/native_e2e.py" in manifest
    assert "skills/build-task-graph/tests/fixtures/valid-graph-draft.json" in manifest


if __name__ == "__main__":
    for name, test in sorted(globals().items()):
        if name.startswith("test_"):
            test()
    print("capability policy tests: passed")
