#!/usr/bin/env python3
"""Index the committed worktree through the Codebase Memory MCP server."""

from __future__ import annotations

import argparse
import asyncio
import os
import shutil
import sys
from pathlib import Path

from mcp import ClientSession
from mcp.client.stdio import StdioServerParameters, stdio_client


async def index_repository(repo_root: Path, project_name: str) -> dict[str, object]:
    command = shutil.which("codebase-memory-mcp")
    if command is None:
        raise RuntimeError("codebase-memory-mcp executable was not found on PATH")

    server = StdioServerParameters(
        command=command,
        args=[],
        cwd=repo_root,
        env=os.environ.copy(),
    )
    async with stdio_client(server) as (read_stream, write_stream):
        async with ClientSession(read_stream, write_stream) as session:
            await session.initialize()
            result = await session.call_tool(
                "index_repository",
                {
                    "repo_path": str(repo_root),
                    "mode": "full",
                    "name": project_name,
                },
            )

    if result.is_error:
        raise RuntimeError("Codebase Memory MCP index_repository returned an error")

    structured = result.structured_content
    if not isinstance(structured, dict) or structured.get("status") != "indexed":
        raise RuntimeError(
            "Codebase Memory MCP index_repository did not return status=indexed"
        )
    return structured


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo-root", type=Path, required=True)
    parser.add_argument("--project-name", required=True)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        result = asyncio.run(index_repository(args.repo_root.resolve(), args.project_name))
    except Exception as error:  # noqa: BLE001 - hook must report MCP failures clearly
        print(f"post-commit: MCP indexing failed: {error}", file=sys.stderr)
        return 1

    print(
        "post-commit: Codebase Memory MCP indexed "
        f"project={result.get('project')} nodes={result.get('nodes')} "
        f"edges={result.get('edges')} status={result.get('status')}",
        file=sys.stderr,
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
