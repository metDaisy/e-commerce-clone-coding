---
name: write-task
description: Use when the Project Manager creates or repairs an evidence-backed Amaazon Kanban task graph.
version: 0.6.0
author: leee, Hermes Agent
license: MIT
platforms: [linux, macos, windows]
metadata:
  hermes:
    tags: [planning, kanban, requirements, verification]
    related_skills: [cross-domain-contract-planning, semble-search, codebase-memory-mcp]
---

# Write Task

> **Legacy v3 — current PM distribution에서 사용하지 않는다.** Backend Impl card는
> `build-task-graph`와 `.hermes/profiles/coder/skills/run-impl-card/references/`
> contract를 따른다. 이 Skill과 v3 validator는 migration 참고용이며 새 graph의 authority가 아니다.

This archived recipe documents the v3 graph authoring path. It is not invoked by the current Project
Manager distribution and must not author or repair current tasks.

## Authority map

- `SOUL.md` owns PM identity, mutation limits, routing, approval, failure handling, and the Issue lifecycle.
- [`references/board-contract.md`](references/board-contract.md) owns only the archived v3 fixture and
  validator field set.
- `cross-domain-contract-planning` owns only a missing or uncertain public seam between modules.
- This Skill owns committed-evidence discovery and the ordered authoring work that turns that evidence
  into a validated graph.

Do not copy lifecycle policy into this Skill, or redefine a contract field and validator rule here.
If this Skill and the contract disagree, apply the contract; if the procedure and SOUL disagree,
apply SOUL and record the discrepancy for a separate documentation change.

Load `SOUL.md` and `board-contract.md` before a graph mutation. Use native `kanban_*` tools when
available; otherwise inspect `hermes kanban --help` and use the official CLI. Select the board
explicitly for every read or mutation. If the required skill, contract, or official surface is
unavailable, preserve the evidence and stop before mutation.

## 1. Establish the planning snapshot

Read the Issue, `AGENTS.md`, `docs/index.md`, `docs/current-state.md`, applicable requirements,
architecture/ADR documents, existing board, repository `HEAD`, and Git status. Requirements define
the target; committed code, tests, configuration, and migrations define the current behavior.

Use `planning_head_sha` for all planning evidence. Read a dirty tracked source from that Git object;
untracked and working-tree changes are execution-safety inputs, not planning evidence. Preserve a
conflict or insufficient source as `unknowns`, `blocked`, or `needs-input`.

**Completion:** the Issue outcome, literal planning SHA, snapshot input, relevant module boundaries,
and unresolved questions are known before discovery begins.

## 2. Build the evidence matrix

For every observable Issue behavior, extract affected domain terms, state transitions, public
contracts, and persistence concerns. Give each search question a local short `query_id`; never put
the raw query in Kanban or audit records.

1. Call codebase-memory `index_status`; when usable, use `search_graph` for known symbols/modules
   and `trace_path` for callers, callees, dependencies, or cross-module paths.
2. Use Semble `search` for behavior whose location remains unknown. Expand a promising source/test
   result with `find_related`; keep unrelated domains outside the search scope.
3. Treat MCP output as a locator. Read the returned production source, tests, `package-info.java`,
   configuration, JPA mapping, and Flyway migration from `planning_head_sha`.
4. Classify each behavior in a private temporary matrix:

   ```text
   requirement_id | status=implemented|partial|absent|unknown
   confirmed_source_locators | confirmed_test_locators | constraint_locators | task_decision
   ```

`absent` requires a searched entry point/contract and implementation with no satisfying committed
behavior. `unknown` means the evidence is insufficient; route it to investigation or `needs-input`.
Create implementation work only for confirmed `partial` or `absent` behavior. Cite implemented
behavior in the planning report instead.

For broad cross-module work, use both MCPs before creation: Semble finds behavior and codebase-memory
checks relationships. If either is unavailable, continue safe read-only planning with literal search
and committed-file reads; record only the limitation and the resulting `unknowns`. MCP configuration
or reindexing is a separate approved change. `agent-audit` observations are debugging evidence, never
a substitute for source confirmation or the board validator.

**Completion:** every behavior is classified with committed source/test/constraint locators or an
explicit unknown; no task decision depends only on an Issue, search result, or `current-state.md`.

## 3. Choose the smallest justified graph

Apply the freshness and stale-board branch from `board-contract.md`. A stale branch contains only
the reconciliation/investigation task required by that contract; generate downstream work only after
its durable result establishes a gap.

Give each live leaf one owner, one coherent outcome, explicit scope/out-of-scope, and decisive
verification. A functional implementation task maps the requirement `goal`, confirmed production
`state`, and relevant test surface. When production code is absent, use the nearest confirmed module
seam, schema, and test surface; describe the searched absence in `state`.

If a consumer needs another module's absent, partial, or uncertain public capability, load
`cross-domain-contract-planning`. The consumer depends on its contract decision, not automatically on
the producer's complete implementation. Create producer work only when that decision requires it.

Select `dir`, `scratch`, or an authorized worktree before `create`. A task body receives the native
raw workspace kind/path after read-back; selector prefixes never become persisted paths.

**Completion:** every planned task has a justified outcome and owner, every cross-domain prerequisite
has a contract decision or `needs-input`, and a workspace selection exists before task creation.

## 4. Author the draft contract

Build a zero-ready `show --json`-shaped draft. Apply the body schema, structured evidence source
forms, CHECK/EXPECT rules, review handoff, runtime bindings, Korean human-readable text, and secret
redaction requirements from `board-contract.md`.

Use one executable operation per `CHECK`. For Gradle work, state the intended `gradle-mcp` request and
actual Java FQCNs; do not substitute a shell Gradle command. Persist only source-backed,
repository-relative decisions and literal IDs after their native read-back.

**Completion:** `validate_board.py --input <draft.json> --phase draft` exits `0` from the repository
root. A finding or infrastructure failure leaves the draft uncreated and is routed with its sanitized
rule/check and next action.

## 5. Create, link, and prove the native graph

Create prerequisite tasks before dependents that need their task IDs. After each create, read the
native envelope; then write only its actual `t_<hex>` ID into a dependent body. Link parent/child IDs,
read back both envelopes, and calculate declared dependency-path lengths from the native parent graph.

Native creation may promote a dependency-free task. Accept that native state, ensure the contract's
single intended executable ready leaf, and never report an invented zero-ready native phase. Immediately
before each validator gate, confirm repository `HEAD` still equals every task's `planning_head_sha`.

Run the native post gate from the repository root:

```text
python .hermes/profiles/project-manager/scripts/validate_board.py --board <slug> --phase post --profile project-manager --repository <repository-root>
```

**Completion:** every live envelope and link is read back, the post validator exits `0`, and exactly
the contract-permitted ready state is present. Only then report graph creation.

## 6. Route evidence, not prose

Follow `SOUL.md` for same-card review, commit/freeze, quality review, PR, and finalization. Follow
`board-contract.md` for executed review metadata and durable result handoffs. Missing evidence,
wrong workspace identity, stale planning identity, or validator findings route to the contract's
blocked/reconciliation/archive path; never weaken the contract or patch Kanban storage directly.

Keep credentials, tokens, connection strings, raw tool output, prompts, and reasoning out of task
bodies, comments, metadata, and audit evidence.
