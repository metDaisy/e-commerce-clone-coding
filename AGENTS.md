# Project Agent Guide

## Working rules

- Prefer MCP tools, purpose-built connectors, repository skills, and targeted code search over broad reads, repeated searches, or reproducing content in chat.
- Narrow the target first, then read only the relevant symbol, section, or snippet. Read whole files or repository-wide output only when truly required.
- Never repeat equivalent searches across tools. Reuse results and choose the narrowest tool that can perform the next action.
- Run every Gradle operation, including builds and tests, exclusively through `gradle-mcp`; never use a terminal, shell, IDE terminal, wrapper command, or other fallback.
- If `gradle-mcp` is unavailable or fails due to configuration, startup, network, or connection problems, stop all Gradle-dependent work immediately and report the exact cause and required next action. Never bypass the failure.

## Project

- Backend: Java 17, Spring Boot, Spring Modulith, PostgreSQL, Flyway; frontend: React/TypeScript/Vite under `amaazon-front/`.
- Target business rules are in `docs/requirement/index.md` and P1-P6; implementation order is tracked by the P1-P6 issue tree.
- Use `docs/index.md` as the documentation map and `docs/current-state.md` as the implementation snapshot. Select relevant skills from `docs/skills/index.md` only when needed.

## Sources of truth

1. Current behavior: code, tests, and Flyway migrations.
2. Target behavior: `docs/requirement/index.md` and its P1-P6 documents.
3. Treat `current-state.md` as current only when its SHA matches HEAD; issue status describes intent, not completion. Report conflicts instead of guessing.

## Discovery

- The codebase-memory project name is `e-commerce-clone-coding`.
- Prefer Semble and codebase-memory MCP for code discovery and relationship checks; use targeted reads only after locating the relevant symbol.
- Use Semble's `docs` or `config` content scope for documentation and configuration. Use `rg` for exact strings or non-code files.
- Never run equivalent searches in both Semble and codebase-memory: Semble discovers candidates; codebase-memory verifies structure.
- Never pre-read the repository or large files; narrow the module, interface, and call path first.

## Architecture rules

- Modules are `auth`, `user`, `product`, `common`, and `global`; keep `presentation`, `application`, `domain`, and `infra` separate.
- Cross-module access uses published `@NamedInterface`s or events and must follow `package-info.java` `allowedDependencies`; never reference internal packages.
- Events represent facts. If used as commands, document transaction coupling and failure semantics in an ADR. Keep interfaces small and hide implementation details.
- Never expose passwords, tokens, or OAuth secrets. Database changes use a new Flyway migration by default.

## Verification

- Backend: run the nearest tests; module-seam changes also require Modulith and relevant integration checks. Full validation uses Gradle MCP `check` (including Checkstyle) and, when appropriate, JaCoCo verification.
- Frontend: run `npm run lint` and `npm run build` from `amaazon-front/`. Docs-only changes need link, path, SHA, and factual checks.

## Documentation maintenance

- Update architecture/glossary when their facts change; record hard-to-reverse cross-module decisions in `docs/adr/`.
- After meaningful changes, use Continue's `/update` prompt to update `current-state.md` date and SHA. Never record uncommitted work as complete.
- `docs/dev-dairy.md` is the user's development journal. Never edit, summarize, or reorder it without an explicit request.
- Never duplicate code-derived class or method inventories in documentation.
