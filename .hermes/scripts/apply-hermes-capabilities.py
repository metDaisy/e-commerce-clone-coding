#!/usr/bin/env python3
"""Apply strict, repository-owned Hermes Profile capability policies.

The policy is an allowlist for Skills, built-in toolsets, MCP servers, and MCP
server tools. Hermes stores Skills and built-in toolsets as deny lists, so this
script discovers the installed inventory and compiles the complement into the
native configuration. MCP endpoints and credentials remain machine-local.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path
from typing import Any

import yaml


PROFILES = (
    "project-manager",
    "coder",
    "reviewer",
)

_SKILL_NAME = re.compile(r"^[A-Za-z0-9][A-Za-z0-9_-]*$")
_TOOLSET_LINE = re.compile(
    r"^\s*[✓✗]\s+(?:enabled|disabled)\s+([A-Za-z0-9][A-Za-z0-9_-]*)\s+"
)
# Hermes Kanban workers receive this toolset from the dispatcher rather than
# from the ordinary CLI tool inventory. It must still be declared explicitly.
RUNTIME_TOOLSETS = {"kanban"}


def load_yaml(path: Path) -> dict[str, Any]:
    """Load one YAML mapping and reject malformed policy documents."""
    with path.open(encoding="utf-8") as stream:
        value = yaml.safe_load(stream)
    if not isinstance(value, dict):
        raise ValueError(f"{path}: policy must be a mapping")
    return value


def load_policy(policy_path: Path, profiles: list[str]) -> dict[str, Any]:
    """Load shared MCP definitions and strict per-Profile allowlists."""
    if policy_path.is_file():
        return load_yaml(policy_path)
    if not policy_path.is_dir():
        raise ValueError(f"policy path does not exist: {policy_path}")

    common_path = policy_path / "common.yaml"
    common = load_yaml(common_path) if common_path.is_file() else {"version": 1, "mcp_tools": {}}
    if common.get("version") != 1:
        raise ValueError("common capability policy version must be 1")
    common_mcp_tools = common.get("mcp_tools", {})
    if not isinstance(common_mcp_tools, dict):
        raise ValueError("common.yaml: mcp_tools must be a mapping")

    loaded_profiles: dict[str, dict[str, Any]] = {}
    for profile in profiles:
        profile_path = policy_path / f"{profile}.yaml"
        if not profile_path.is_file():
            matches = []
            for candidate in policy_path.glob("*/capabilities.yaml"):
                if load_yaml(candidate).get("profile") == profile:
                    matches.append(candidate)
            if len(matches) != 1:
                raise ValueError(
                    f"{policy_path}: expected one capabilities.yaml for {profile}, found {len(matches)}"
                )
            profile_path = matches[0]
        entry = load_yaml(profile_path)
        if entry.get("profile") != profile:
            raise ValueError(f"{profile_path}: profile name does not match filename")

        skills = entry.get("skills") or {}
        tools = entry.get("tools") or {}
        approvals = entry.get("approvals") or {}
        stt = entry.get("stt")
        mcp = entry.get("mcp") or {}
        profile_mcp_tools = entry.get("mcp_tools") or {}
        if "allowed" not in skills or "additional_disabled" in skills:
            raise ValueError(
                f"{profile_path}: skills.allowed is required; legacy disabled Skill keys are not supported"
            )
        if "allowed_toolsets" not in tools or "additional_disabled_toolsets" in tools:
            raise ValueError(
                f"{profile_path}: tools.allowed_toolsets is required; legacy disabled toolset keys are not supported"
            )
        if not isinstance(profile_mcp_tools, dict):
            raise ValueError(f"{profile_path}: mcp_tools must be a mapping")
        mcp_tools = {**common_mcp_tools, **profile_mcp_tools}

        filters: dict[str, Any] = {}
        for server, reference in (mcp.get("filters", {}) or {}).items():
            if isinstance(reference, str):
                try:
                    filters[server] = mcp_tools[reference]
                except KeyError as exc:
                    raise ValueError(
                        f"{profile_path}: unknown MCP tool reference {reference}"
                    ) from exc
            elif isinstance(reference, dict):
                filters[server] = reference
            else:
                raise ValueError(f"{profile_path}: invalid MCP filter for {server}")

        external_skills = skills.get("external", {}) or {}
        if not isinstance(external_skills, dict):
            raise ValueError(f"{profile_path}: skills.external must be a mapping")
        for name, source in external_skills.items():
            if not isinstance(name, str) or not _SKILL_NAME.fullmatch(name):
                raise ValueError(f"{profile_path}: invalid external Skill name {name!r}")
            if name not in skills.get("allowed", []):
                raise ValueError(
                    f"{profile_path}: external Skill {name!r} must be listed in skills.allowed"
                )
            if not isinstance(source, str) or not source:
                raise ValueError(f"{profile_path}: external Skill {name!r} requires a source")

        loaded_profiles[profile] = {
            "skills": {
                "allowed": skills.get("allowed"),
                "external": external_skills,
            },
            "plugins": entry.get("plugins"),
            "tools": {
                "allowed_toolsets": tools.get("allowed_toolsets"),
                "platform_toolsets": tools.get("platform_toolsets", {}),
            },
            "approvals": {"mode": approvals.get("mode")},
            "stt": stt,
            "mcp": {
                "allowed_servers": mcp.get("allowed_servers", []),
                "filters": filters,
            },
        }

    return {"version": common["version"], "profiles": loaded_profiles}


def flatten(value: Any, field: str = "list") -> list[str]:
    """Flatten YAML anchor-expanded lists and reject invalid values."""
    if not isinstance(value, list):
        raise ValueError(f"{field} must be a YAML list")
    result: list[str] = []
    for item in value:
        if isinstance(item, list):
            result.extend(flatten(item, field))
        elif isinstance(item, str) and item:
            result.append(item)
        else:
            raise ValueError(f"{field} requires non-empty string items, got {item!r}")
    if len(result) != len(set(result)):
        raise ValueError(f"{field} contains duplicate entries")
    return result


def parse_skill_names(output: str) -> set[str]:
    """Parse the non-truncated names from ``hermes skills list``."""
    names: set[str] = set()
    for line in output.splitlines():
        if "│" not in line:
            continue
        cells = [cell.strip() for cell in line.split("│")]
        if len(cells) < 6:
            continue
        name = cells[1]
        if name == "Name":
            continue
        if "…" in name or "..." in name:
            raise ValueError("Hermes returned truncated Skill names; increase terminal width")
        if _SKILL_NAME.fullmatch(name):
            names.add(name)
    if not names:
        raise ValueError("Hermes returned no installed Skill names")
    return names


def parse_toolset_names(output: str) -> set[str]:
    """Parse built-in and plugin toolset names from ``hermes tools list``."""
    names = {match.group(1) for line in output.splitlines() if (match := _TOOLSET_LINE.match(line))}
    if not names:
        raise ValueError("Hermes returned no built-in or plugin toolset names")
    return names


def hermes(profile: str, *args: str, cwd: Path | None = None) -> str:
    """Run a profile-scoped Hermes command with enough table width to avoid truncation."""
    command = ["hermes", "--profile", profile, *args]
    environment = os.environ.copy()
    environment["COLUMNS"] = "1000"
    environment.setdefault("TERM", "dumb")
    completed = subprocess.run(
        command,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        env=environment,
        cwd=cwd,
        check=False,
    )
    if completed.returncode != 0:
        raise RuntimeError(
            f"Hermes command failed for {profile}: {' '.join(args)} "
            f"(exit {completed.returncode})"
        )
    return completed.stdout


def discover_skill_names(profile: str, cwd: Path | None = None) -> set[str]:
    return parse_skill_names(
        hermes(profile, "skills", "list", "--source", "all", cwd=cwd)
    )


def discover_toolsets(profile: str) -> set[str]:
    return parse_toolset_names(hermes(profile, "tools", "list", "--platform", "cli"))


def set_config(profile: str, key: str, value: Any, *, force: bool = False) -> None:
    # String-typed settings remain plain scalars; lists/maps use compact JSON.
    encoded = value if isinstance(value, str) else json.dumps(
        value, ensure_ascii=False, separators=(",", ":")
    )
    args = ["config", "set"]
    if force:
        args.append("--force")
    hermes(profile, *args, key, encoded)


def get_json_config(profile: str, key: str) -> Any:
    return json.loads(hermes(profile, "config", "get", key, "--json"))


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


def verify(profile: str, expected: dict[str, Any], project_root: Path | None = None) -> None:
    disabled_skills = get_json_config(profile, "skills.disabled")
    if disabled_skills != expected["skills_disabled"]:
        raise RuntimeError(f"{profile}: skills.disabled read-back mismatch")
    actual_skills = discover_skill_names(profile, project_root)
    if actual_skills - set(disabled_skills) != set(expected["skills_allowed"]):
        raise RuntimeError(f"{profile}: effective Skill allowlist read-back mismatch")

    disabled_toolsets = get_json_config(profile, "agent.disabled_toolsets")
    if disabled_toolsets != expected["disabled_toolsets"]:
        raise RuntimeError(f"{profile}: agent.disabled_toolsets read-back mismatch")
    actual_toolsets = discover_toolsets(profile)
    effective_toolsets = (actual_toolsets - set(disabled_toolsets)) | (
        set(expected["toolsets_allowed"]) & RUNTIME_TOOLSETS
    )
    if effective_toolsets != set(expected["toolsets_allowed"]):
        raise RuntimeError(f"{profile}: effective toolset allowlist read-back mismatch")

    if expected["plugins_enabled"] is not None:
        enabled_plugins = get_json_config(profile, "plugins.enabled")
        if enabled_plugins != expected["plugins_enabled"]:
            raise RuntimeError(f"{profile}: plugins.enabled read-back mismatch")

    actual_platform_toolsets = get_json_config(profile, "platform_toolsets")
    if actual_platform_toolsets != expected["platform_toolsets"]:
        raise RuntimeError(f"{profile}: platform_toolsets read-back mismatch")

    if get_json_config(profile, "approvals.mode") != expected["approval"]:
        raise RuntimeError(f"{profile}: approvals.mode read-back mismatch")
    if expected["stt_enabled"] is not None and get_json_config(profile, "stt.enabled") != expected["stt_enabled"]:
        raise RuntimeError(f"{profile}: stt.enabled read-back mismatch")

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
        if actual.get("include") != filter_config.get("include"):
            raise RuntimeError(f"{profile}: MCP include filter mismatch for {name}")
        if actual.get("exclude", []) != []:
            raise RuntimeError(f"{profile}: MCP exclude filter is not empty for {name}")


def profile_expected(
    raw: dict[str, Any],
    profile: str,
    available_skills: set[str] | None = None,
    available_toolsets: set[str] | None = None,
) -> dict[str, Any]:
    try:
        entry = raw["profiles"][profile]
        skills = entry["skills"]
        tools = entry["tools"]
        mcp = entry["mcp"]
        approval = entry["approvals"]["mode"]
        stt = entry.get("stt")
    except (KeyError, TypeError) as exc:
        raise ValueError(f"invalid policy structure for {profile}") from exc

    skills_allowed = flatten(skills.get("allowed"), f"{profile}.skills.allowed")
    if "hermes-agent" not in skills_allowed:
        raise ValueError(f"{profile}: hermes-agent must be in skills.allowed")
    if available_skills is not None:
        missing = sorted(set(skills_allowed) - available_skills)
        if missing:
            raise ValueError(f"{profile}: allowed Skills are not installed: {missing}")
    skills_disabled = sorted((available_skills or set()) - set(skills_allowed))

    toolsets_allowed = flatten(tools.get("allowed_toolsets"), f"{profile}.tools.allowed_toolsets")
    if available_toolsets is not None:
        missing = sorted(set(toolsets_allowed) - available_toolsets - RUNTIME_TOOLSETS)
        if missing:
            raise ValueError(f"{profile}: allowed toolsets are unavailable: {missing}")
    disabled_toolsets = sorted((available_toolsets or set()) - set(toolsets_allowed))

    plugins = entry.get("plugins")
    if plugins is not None and not isinstance(plugins, dict):
        raise ValueError(f"{profile}: plugins must be a mapping")
    plugins_enabled = None
    if plugins is not None:
        plugins_enabled = flatten(plugins.get("enabled", []), f"{profile}.plugins.enabled")

    platform_toolsets_raw = tools.get("platform_toolsets", {}) or {}
    if not isinstance(platform_toolsets_raw, dict):
        raise ValueError(f"{profile}: tools.platform_toolsets must be a mapping")
    platform_toolsets: dict[str, list[str]] = {}
    for platform, toolsets in platform_toolsets_raw.items():
        platform_toolsets[platform] = flatten(toolsets, f"{profile}.tools.platform_toolsets.{platform}")
        overlap = set(platform_toolsets[platform]) & set(disabled_toolsets)
        if overlap:
            raise ValueError(f"{profile}: platform toolsets are not allowed: {sorted(overlap)}")

    stt_enabled = None
    if stt is not None:
        if not isinstance(stt, dict) or not isinstance(stt.get("enabled"), bool):
            raise ValueError(f"{profile}: stt.enabled must be a boolean")
        stt_enabled = stt["enabled"]

    allowed_servers = flatten(mcp.get("allowed_servers", []), f"{profile}.mcp.allowed_servers")
    filters = mcp.get("filters", {}) or {}
    if not isinstance(filters, dict):
        raise ValueError(f"{profile}: mcp.filters must be a mapping")
    if set(filters) != set(allowed_servers):
        raise ValueError(f"{profile}: every allowed MCP server requires an exact tools.include filter")

    normalized_filters: dict[str, dict[str, list[str]]] = {}
    for server, filter_config in filters.items():
        if not isinstance(filter_config, dict) or "include" not in filter_config:
            raise ValueError(f"{profile}: MCP filter for {server} must use tools.include")
        normalized = {"include": flatten(filter_config["include"], f"{profile}.mcp.{server}.include")}
        if "exclude" in filter_config and flatten(filter_config["exclude"], f"{profile}.mcp.{server}.exclude"):
            raise ValueError(f"{profile}: MCP exclude filters are not allowed in strict mode")
        normalized_filters[server] = normalized

    return {
        "skills_allowed": skills_allowed,
        "external_skills": dict(skills.get("external", {}) or {}),
        "skills_disabled": skills_disabled,
        "plugins_enabled": plugins_enabled,
        "toolsets_allowed": toolsets_allowed,
        "disabled_toolsets": disabled_toolsets,
        "platform_toolsets": platform_toolsets,
        "approval": approval,
        "stt_enabled": stt_enabled,
        "allowed_servers": allowed_servers,
        "filters": normalized_filters,
    }


def install_external_skill(name: str, source: str, project_root: Path) -> None:
    """Install one skills.sh Skill into the repository's Hermes Skill tree."""
    target = project_root / ".hermes" / "skills" / name / "SKILL.md"
    if target.is_file():
        return

    npx = shutil.which("npx") or shutil.which("npx.cmd")
    if not npx:
        raise RuntimeError(
            f"external Skill {name!r} is missing and npx is unavailable; "
            "install Node.js/npm before applying capabilities"
        )

    command = [
        npx,
        "--yes",
        "skills",
        "add",
        source,
        "--skill",
        name,
        "--agent",
        "hermes-agent",
        "--copy",
        "--yes",
    ]
    completed = subprocess.run(
        command,
        cwd=project_root,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )
    if completed.returncode != 0:
        detail = (completed.stdout + completed.stderr).strip()
        raise RuntimeError(
            f"skills.sh installation failed for {name!r} from {source!r}: {detail}"
        )
    if not target.is_file():
        raise RuntimeError(
            f"skills.sh reported success for {name!r}, but {target} was not created"
        )
    print(f"skills.sh: {name} <- {source} -> {target}")


def ensure_external_skills(
    raw: dict[str, Any], profile: str, project_root: Path
) -> set[str]:
    """Make declared external Skills available before compiling the allowlist."""
    external = raw["profiles"][profile]["skills"].get("external", {})
    available = discover_skill_names(profile, project_root)
    for name, source in external.items():
        target = project_root / ".hermes" / "skills" / name / "SKILL.md"
        if not target.is_file():
            install_external_skill(name, source, project_root)
            available = discover_skill_names(profile, project_root)
    return available


def apply_profile(raw: dict[str, Any], profile: str, project_root: Path | None = None) -> None:
    project_root = (project_root or Path.cwd()).resolve()
    available_skills = ensure_external_skills(raw, profile, project_root)
    available_toolsets = discover_toolsets(profile)
    expected = profile_expected(raw, profile, available_skills, available_toolsets)

    set_config(profile, "skills.disabled", expected["skills_disabled"])
    if expected["plugins_enabled"] is not None:
        set_config(profile, "plugins.enabled", expected["plugins_enabled"])
    set_config(profile, "agent.disabled_toolsets", expected["disabled_toolsets"])
    set_config(profile, "platform_toolsets", expected["platform_toolsets"], force=True)
    set_config(profile, "approvals.mode", expected["approval"])
    if expected["stt_enabled"] is not None:
        set_config(profile, "stt.enabled", expected["stt_enabled"])

    servers = get_mcp_servers(profile)
    allowed = set(expected["allowed_servers"])
    for name in servers:
        set_config(profile, f"mcp_servers.{name}.enabled", name in allowed)

    for name, filter_config in expected["filters"].items():
        if name not in servers:
            print(f"warning: {profile}: MCP server {name} is not configured locally", file=sys.stderr)
            continue
        set_config(profile, f"mcp_servers.{name}.tools.include", filter_config["include"])
        set_config(profile, f"mcp_servers.{name}.tools.exclude", [])

    verify(profile, expected, project_root)
    present_allowed = sorted(set(servers) & allowed)
    print(
        f"{profile}: strict policy applied; allowed_skills={len(expected['skills_allowed'])}, "
        f"allowed_toolsets={len(expected['toolsets_allowed'])}, "
        f"plugins={len(expected['plugins_enabled']) if expected['plugins_enabled'] is not None else 'unchanged'}, "
        f"mcp={','.join(present_allowed) if present_allowed else 'none'}"
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--policy", required=True, type=Path)
    parser.add_argument("--profile", choices=PROFILES, action="append")
    parser.add_argument("--project-root", type=Path, help="repository root for project-local Skills")
    args = parser.parse_args()

    try:
        profiles = args.profile or list(PROFILES)
        raw = load_policy(args.policy, profiles)
        if raw.get("version") != 1:
            raise ValueError("policy version must be 1")
        for profile in profiles:
            apply_profile(raw, profile, args.project_root)
    except (OSError, ValueError, json.JSONDecodeError, RuntimeError) as exc:
        print(f"capability policy failed: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
