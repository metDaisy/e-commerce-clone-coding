#!/usr/bin/env python3
"""Apply the repository's portable Hermes capability policy.

The policy deliberately omits MCP endpoints and credentials. Existing
machine-local MCP server definitions are filtered in place; servers not listed
for a Profile are disabled rather than removed.
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path
from typing import Any

import yaml


PROFILES = (
    "project-manager",
    "prototype-coder",
    "reviewer-general",
    "reviewer-deep",
    "reviewer-coordinator",
    "refactor-coder",
)


def load_yaml(path: Path) -> dict[str, Any]:
    """Load one YAML mapping and reject malformed policy documents."""
    with path.open(encoding="utf-8") as stream:
        value = yaml.safe_load(stream)
    if not isinstance(value, dict):
        raise ValueError(f"{path}: policy must be a mapping")
    return value


def load_policy(policy_path: Path) -> dict[str, Any]:
    """Load either the legacy aggregate policy or split policy directory."""
    if policy_path.is_file():
        return load_yaml(policy_path)
    if not policy_path.is_dir():
        raise ValueError(f"policy path does not exist: {policy_path}")

    common = load_yaml(policy_path / "common.yaml")
    if common.get("version") != 1:
        raise ValueError("common capability policy version must be 1")
    common_skills = common.get("disabled_skills", [])
    common_toolsets = common.get("disabled_toolsets", [])
    common_mcp_tools = common.get("mcp_tools", {})
    if not isinstance(common_mcp_tools, dict):
        raise ValueError("common.yaml: mcp_tools must be a mapping")

    profiles: dict[str, dict[str, Any]] = {}
    for profile in PROFILES:
        profile_path = policy_path / f"{profile}.yaml"
        entry = load_yaml(profile_path)
        if entry.get("profile") != profile:
            raise ValueError(f"{profile_path}: profile name does not match filename")

        skills = entry.get("skills", {}) or {}
        tools = entry.get("tools", {}) or {}
        approvals = entry.get("approvals", {}) or {}
        mcp = entry.get("mcp", {}) or {}
        filters: dict[str, Any] = {}
        for server, reference in (mcp.get("filters", {}) or {}).items():
            if isinstance(reference, str):
                try:
                    filters[server] = common_mcp_tools[reference]
                except KeyError as exc:
                    raise ValueError(
                        f"{profile_path}: unknown MCP tool reference {reference}"
                    ) from exc
            elif isinstance(reference, dict):
                filters[server] = reference
            else:
                raise ValueError(f"{profile_path}: invalid MCP filter for {server}")

        profiles[profile] = {
            "skills": {
                "disabled": common_skills,
                "additional_disabled": skills.get("additional_disabled", []),
            },
            "tools": {
                "disabled_toolsets": [
                    common_toolsets,
                    tools.get("additional_disabled_toolsets", []),
                ]
            },
            "approvals": {"mode": approvals.get("mode")},
            "mcp": {
                "allowed_servers": mcp.get("allowed_servers", []),
                "filters": filters,
            },
        }

    return {"version": common["version"], "profiles": profiles}


def flatten(value: Any) -> list[str]:
    """Flatten YAML anchor-expanded lists and reject non-string values."""
    if not isinstance(value, list):
        raise ValueError("expected a YAML list")
    result: list[str] = []
    for item in value:
        if isinstance(item, list):
            result.extend(flatten(item))
        elif isinstance(item, str) and item:
            result.append(item)
        else:
            raise ValueError(f"expected non-empty string list item, got {item!r}")
    if len(result) != len(set(result)):
        raise ValueError("duplicate entries are not allowed")
    return result


def hermes(profile: str, *args: str) -> str:
    command = ["hermes", "--profile", profile, *args]
    completed = subprocess.run(
        command,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )
    if completed.returncode != 0:
        raise RuntimeError(
            f"Hermes command failed for {profile}: {' '.join(args)} "
            f"(exit {completed.returncode})"
        )
    return completed.stdout


def set_config(profile: str, key: str, value: Any) -> None:
    # String-typed Hermes settings (for example approvals.mode) must remain
    # plain scalars. Structured values are passed as compact JSON/YAML literals.
    encoded = value if isinstance(value, str) else json.dumps(
        value, ensure_ascii=False, separators=(",", ":")
    )
    hermes(profile, "config", "set", key, encoded)


def get_mcp_servers(profile: str) -> dict[str, dict[str, Any]]:
    command = ["hermes", "--profile", profile, "config", "get", "mcp_servers", "--json"]
    completed = subprocess.run(
        command,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )
    if completed.returncode != 0:
        missing_key = "Config key not set: mcp_servers"
        if missing_key in completed.stdout or missing_key in completed.stderr:
            return {}
        raise RuntimeError(
            f"Hermes command failed for {profile}: config get mcp_servers "
            f"(exit {completed.returncode})"
        )
    value = json.loads(completed.stdout)
    if value is None:
        return {}
    if not isinstance(value, dict):
        raise ValueError(f"{profile}: mcp_servers must be a mapping")
    return value


def verify(profile: str, expected: dict[str, Any]) -> None:
    disabled_skills = json.loads(
        hermes(profile, "config", "get", "skills.disabled", "--json")
    )
    if disabled_skills != expected["skills_disabled"]:
        raise RuntimeError(f"{profile}: skills.disabled read-back mismatch")

    disabled_toolsets = json.loads(
        hermes(profile, "config", "get", "agent.disabled_toolsets", "--json")
    )
    if disabled_toolsets != expected["disabled_toolsets"]:
        raise RuntimeError(f"{profile}: agent.disabled_toolsets read-back mismatch")

    approval = json.loads(
        hermes(profile, "config", "get", "approvals.mode", "--json")
    )
    if approval != expected["approval"]:
        raise RuntimeError(f"{profile}: approvals.mode read-back mismatch")

    servers = get_mcp_servers(profile)
    allowed = set(expected["allowed_servers"])
    for name, server in servers.items():
        if not isinstance(server, dict):
            raise RuntimeError(f"{profile}: MCP server {name!r} is not a mapping")
        if bool(server.get("enabled", False)) != (name in allowed):
            raise RuntimeError(f"{profile}: MCP enabled state mismatch for {name}")

    for name, filter_config in expected["filters"].items():
        if name not in servers:
            continue
        actual = servers[name].get("tools") or {}
        expected_include = filter_config.get("include")
        if expected_include is not None and actual.get("include") != expected_include:
            raise RuntimeError(f"{profile}: MCP include filter mismatch for {name}")


def profile_expected(raw: dict[str, Any], profile: str) -> dict[str, Any]:
    try:
        entry = raw["profiles"][profile]
        skills = entry["skills"]
        tools = entry["tools"]
        mcp = entry["mcp"]
        approval = entry["approvals"]["mode"]
    except (KeyError, TypeError) as exc:
        raise ValueError(f"invalid policy structure for {profile}") from exc

    disabled_skills = flatten(skills["disabled"])
    disabled_skills.extend(flatten(skills.get("additional_disabled", [])))
    if len(disabled_skills) != len(set(disabled_skills)):
        raise ValueError(f"{profile}: duplicate disabled Skill")
    if "hermes-agent" in disabled_skills:
        raise ValueError(f"{profile}: hermes-agent cannot be disabled")

    disabled_toolsets = flatten(tools["disabled_toolsets"])
    allowed_servers = flatten(mcp.get("allowed_servers", []))
    filters = mcp.get("filters", {}) or {}
    if not isinstance(filters, dict):
        raise ValueError(f"{profile}: mcp.filters must be a mapping")

    normalized_filters: dict[str, dict[str, list[str]]] = {}
    for server, filter_config in filters.items():
        if server not in allowed_servers:
            raise ValueError(f"{profile}: filter for non-allowed MCP server {server}")
        if not isinstance(filter_config, dict):
            raise ValueError(f"{profile}: invalid MCP filter for {server}")
        normalized: dict[str, list[str]] = {}
        for mode in ("include", "exclude"):
            if mode in filter_config:
                normalized[mode] = flatten(filter_config[mode])
        normalized_filters[server] = normalized

    return {
        "skills_disabled": disabled_skills,
        "disabled_toolsets": disabled_toolsets,
        "approval": approval,
        "allowed_servers": allowed_servers,
        "filters": normalized_filters,
    }


def apply_profile(raw: dict[str, Any], profile: str) -> None:
    expected = profile_expected(raw, profile)
    set_config(profile, "skills.disabled", expected["skills_disabled"])
    set_config(profile, "agent.disabled_toolsets", expected["disabled_toolsets"])
    set_config(profile, "approvals.mode", expected["approval"])

    servers = get_mcp_servers(profile)
    allowed = set(expected["allowed_servers"])
    for name in servers:
        set_config(profile, f"mcp_servers.{name}.enabled", name in allowed)

    for name, filter_config in expected["filters"].items():
        if name not in servers:
            print(f"warning: {profile}: MCP server {name} is not configured locally", file=sys.stderr)
            continue
        if "include" in filter_config:
            set_config(profile, f"mcp_servers.{name}.tools.include", filter_config["include"])
            # Make the allowlist authoritative if an older exclude list exists.
            set_config(profile, f"mcp_servers.{name}.tools.exclude", [])
        elif "exclude" in filter_config:
            set_config(profile, f"mcp_servers.{name}.tools.exclude", filter_config["exclude"])

    verify(profile, expected)
    present_allowed = sorted(set(servers) & allowed)
    print(
        f"{profile}: policy applied; skills.disabled={len(expected['skills_disabled'])}, "
        f"disabled_toolsets={len(expected['disabled_toolsets'])}, "
        f"mcp={','.join(present_allowed) if present_allowed else 'none'}"
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--policy", required=True, type=Path)
    parser.add_argument("--profile", choices=PROFILES, action="append")
    args = parser.parse_args()

    try:
        raw = load_policy(args.policy)
        if raw.get("version") != 1:
            raise ValueError("policy version must be 1")
        profiles = args.profile or list(PROFILES)
        for profile in profiles:
            apply_profile(raw, profile)
    except (OSError, ValueError, json.JSONDecodeError, RuntimeError) as exc:
        print(f"capability policy failed: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
