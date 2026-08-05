# Project Agent Guide

## User directives

### Token-efficient tool use

- Prefer MCP tools, purpose-built connectors, repository skills, and targeted code search over broad reads, repeated searches, or reproducing content in chat.
- Narrow the target first, then read only the relevant symbol, section, or snippet. Read whole files or repository-wide output only when truly required.
- Never repeat equivalent searches across tools. Reuse results and choose the narrowest tool that can perform the next action.

### Gradle: mandatory MCP-only execution

- Run every Gradle operation, including builds and tests, exclusively through `gradle-mcp`; never use a terminal, shell, IDE terminal, wrapper command, or other fallback.
- If `gradle-mcp` is unavailable or fails due to configuration, startup, network, or connection problems, stop all Gradle-dependent work immediately and report the exact cause and required next action. Never bypass the failure.

## Project

- Amazon.com clone e-commerce system.
- Backend: Java 17, Spring Boot 3.5.16, Spring Modulith, PostgreSQL, and Flyway in a modular monolith.
- Frontend: React, TypeScript, and Vite application under `amaazon-front/`.
- Treat `docs/requirement.md` as the source for overall goals and business rules, and `docs/implementation-plan.md` as the implementation sequence.
- Documentation entry point: `docs/index.md`; current implementation snapshot: `docs/current-state.md`.
- Task-specific personal skills are indexed in `docs/skills/index.md`. Select only skills relevant to the task and read each selected `SKILL.md` in full.

## Sources of truth

1. For current behavior, prefer code, tests, and Flyway migrations.
2. For target behavior, prefer `docs/requirement.md`.
3. `docs/current-state.md` is a snapshot at its recorded Git SHA. If that SHA differs from HEAD, recheck the code.
4. Schedules and stages in `docs/implementation-plan.md` describe the intended order, not proof of completion.
5. If documentation conflicts with code, report the difference and ask which side to change; never reconcile it by assumption.

## Context-efficient discovery

- The codebase-memory project name is `e-commerce-clone-coding`.
- Use Semble first when locating an implementation by meaning or behavior:
  - For code, use Semble MCP `search`, then go directly to the returned file and line.
  - For documentation and configuration, use the CLI with `--content docs` and `--content config`, respectively. Search code and documentation separately by purpose.
  - Use Semble MCP `find_related` only to find implementations similar to a known location; similarity does not prove a call relationship.
- Verify relationships for Semble-discovered symbols with codebase-memory:
  - Use `search_graph` for the exact symbol, `trace_path` for call/data flow and impact analysis, and `get_code_snippet` only for the final target.
  - Use `get_architecture` only when a high-level view is necessary.
  - Before indexing, compare `index_status` with HEAD, inspect impact using `detect_changes`, and run `index_repository` only when required.
- Use `rg` only when every occurrence of an exact string, error message, or configuration value is required.
- Never run equivalent searches in both Semble and codebase-memory: Semble discovers candidates; codebase-memory verifies structure.
- Never pre-read the entire repository or large files. First narrow the relevant module, interface, and call path.

## Architecture rules

- Backend top-level modules are `auth`, `user`, `common`, and `global`; add future domains as separate top-level packages.
- Keep `presentation`, `application`, `domain`, and `infra` responsibilities separate within each domain module.
- Never reference another module's internal implementation packages. Use only published `@NamedInterface`s or events, and honor `package-info.java` `allowedDependencies`.
- Spring Application Events are the default inter-module communication mechanism. When synchronous lookup is essential, use a small public interface seam without creating a new cyclic dependency.
- Events represent facts that already occurred. If used like commands, document transaction coupling and failure semantics, then record the decision in an ADR.
- Keep module interfaces small; hide business rules, persistence choices, and external integration details inside implementations.
- Never pass plaintext passwords outside `auth` or through events. Never expose tokens, passwords, or OAuth secrets in logs, documentation, or test output.
- For database changes, add a new Flyway migration rather than modifying an existing one by default.

## Verification

- All Gradle work must follow the mandatory `gradle-mcp` rule above.
- For backend changes, run the nearest unit tests first. For module-seam changes, also run Spring Modulith structure verification and relevant integration tests.
- When full backend verification is needed, run `test` and, when appropriate, `jacocoTestCoverageVerification` through Gradle MCP.
- For frontend changes, run `npm run lint` and `npm run build` from `amaazon-front/`.
- Documentation-only changes may skip Gradle builds, but verify links, paths, Git SHAs, and factual consistency with the code.

## Documentation maintenance

- Update `docs/architecture.md` when architecture or module seams change.
- Update `docs/domain-glossary.md` when domain terminology or state semantics change.
- After a meaningful feature commit or implementation-stage change, use Continue's `/update` prompt to update both the date and Git SHA in `docs/current-state.md`. Never record uncommitted changes as complete.
- Record hard-to-reverse, cross-module decisions as ADRs under `docs/adr/`.
- `docs/dev-dairy.md` is the user's development journal. Never edit, summarize, or reorder it without an explicit request.
- Never duplicate code-derived class or method inventories in documentation.
