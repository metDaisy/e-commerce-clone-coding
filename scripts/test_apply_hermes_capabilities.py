"""Focused tests for portable Hermes capability policy application."""

from __future__ import annotations

import importlib.util
from pathlib import Path


_SCRIPT = Path(__file__).with_name("apply-hermes-capabilities.py")
_SPEC = importlib.util.spec_from_file_location("apply_hermes_capabilities", _SCRIPT)
assert _SPEC and _SPEC.loader
_POLICY = importlib.util.module_from_spec(_SPEC)
_SPEC.loader.exec_module(_POLICY)


def _raw(enabled_plugins=None):
    entry = {
        "skills": {"disabled": [], "additional_disabled": []},
        "tools": {"disabled_toolsets": [[], []], "platform_toolsets": {}},
        "approvals": {"mode": "smart"},
        "stt": None,
        "mcp": {"allowed_servers": [], "filters": {}},
    }
    if enabled_plugins is not None:
        entry["plugins"] = {"enabled": enabled_plugins}
    return {"version": 1, "profiles": {"project-manager": entry}}


def test_profile_expected_preserves_explicit_plugin_allowlist() -> None:
    expected = _POLICY.profile_expected(_raw(["agent-audit"]), "project-manager")

    assert expected["plugins_enabled"] == ["agent-audit"]


def test_profile_expected_leaves_unspecified_plugins_untouched() -> None:
    expected = _POLICY.profile_expected(_raw(), "project-manager")

    assert expected["plugins_enabled"] is None


def test_apply_profile_sets_explicit_plugin_allowlist() -> None:
    writes = []
    original_set_config = _POLICY.set_config
    original_get_mcp_servers = _POLICY.get_mcp_servers
    original_verify = _POLICY.verify
    try:
        _POLICY.set_config = lambda profile, key, value: writes.append((profile, key, value))
        _POLICY.get_mcp_servers = lambda _profile: {}
        _POLICY.verify = lambda _profile, _expected: None

        _POLICY.apply_profile(_raw(["agent-audit"]), "project-manager")
    finally:
        _POLICY.set_config = original_set_config
        _POLICY.get_mcp_servers = original_get_mcp_servers
        _POLICY.verify = original_verify

    assert ("project-manager", "plugins.enabled", ["agent-audit"]) in writes


if __name__ == "__main__":
    for name, test in sorted(globals().items()):
        if name.startswith("test_"):
            test()
    print("capability policy tests: passed")
