---
name: implement
description: Use when coding and verifying an admitted backend Impl card.
version: 0.1.0
author: "Amaazon project, Hermes Agent"
license: MIT
platforms: [linux, macos, windows]
metadata:
  hermes:
    tags: [implementation, backend, java, testing, cleanup]
    related_skills: [run-impl-card, codebase-memory-mcp, semble-search, java-springboot, java-junit]
requires_toolsets: [file, terminal]
---

# Implement

Turn one admitted `backend-implementation-card-v1` into the smallest verified backend code change.
`run-impl-card` owns admission, Kanban transitions, blocker routing, and handoff; this Skill owns the
coding loop between admission and handoff.

## When to Use

- Use after `run-impl-card` has admitted an initial or changes-requested run.
- Use for Java/Spring backend production code, tests, configuration, migration, and directly required code artifacts.
- Do not use to reinterpret requirements, author cards, edit documentation, commit, or complete a Kanban task.

## Prerequisites

- The active native task is assigned to `implementation-coder` and is `running`.
- The immutable card and, for rework, correlated change-request passed `run-impl-card` admission.
- The worktree satisfies the initial-run or rework dirty-path rule.
- `gradle-mcp` exposes `gradle` and `query_build`. If unavailable, block instead of using Gradle through `terminal`.

## Execution Mode

- **Initial run:** implement every behavior and acceptance in the validated card.
- **Changes-requested rework:** edit only each correlated finding's exact `path`, `symbol`, and `allowed_scope`.
  Use the original card only to preserve its invariants and determine the full verification contract. Do not remap,
  improve, or simplify unrelated card code. Rerun every card-required focused verification and the full backend
  verification even when a finding names a narrower test.

## Procedure

### 1. Build the implementation map

1. Read every card entry point and nearest test, then trace definitions, callers, consumers, DTOs, mappings,
   adapters, repositories, events, and `package-info.java` boundaries with `search_files` and `read_file`.
2. Use `semble-search` only to locate uncertain code and `codebase-memory-mcp` only to inspect cross-domain or
   public-seam impact; confirm every result in repository files.
3. On an initial run, map each `effective_behavior.id` and acceptance criterion to a code seam and focused
   verification ID. On rework, map only validated findings to edit seams while retaining the card's full verification
   map.

Completion criterion: every initial-run behavior or rework finding has a concrete edit/test location, or the task is
blocked with the exact missing contract or decision.

### 2. Establish a failing test where possible

1. For a defect or changed rule, add the smallest test that reproduces the missing behavior before production code.
   During rework, add or change tests only when a validated finding requires it.
2. On an initial run, cover every card-required success, rejection, failure, boundary, and persistence scenario at
   the declared `test_level`. During rework, preserve unrelated tests and add only finding-required coverage. Reuse
   existing fixtures and test conventions instead of creating a parallel test framework.
3. Execute only the new or nearest focused test through `gradle-mcp` and confirm it fails for the expected reason.
   If the seam cannot produce a meaningful red test, record why and continue only when the card's verification
   contract still provides deterministic coverage.

Completion criterion: the behavior gap is demonstrated by a relevant failure, or the justified non-red seam and
its replacement coverage are explicit.

### 3. Implement the smallest complete change

1. Change the deepest correct seam that fixes the whole card-defined behavior on an initial run, or the exact
   finding-defined defect on rework, not a caller-specific symptom.
2. Propagate only what compilation and the confirmed contract require across callers, DTOs, mappings, adapters,
   persistence, configuration, migrations, and tests.
3. Preserve public contracts, transaction semantics, module boundaries, exception conventions, and existing style
   unless the card explicitly changes them.
4. Do not add speculative abstractions, unrelated defect repairs, broad refactors, or compatibility behavior absent
   from the card. Do not weaken or remove tests.

Completion criterion: production and test changes explain every changed path and no edit depends on an invented
product decision.

### 4. Run the tight verification loop

1. Run the nearest affected tests through `gradle-mcp`; fix root causes and repeat until green.
2. Run every card `focused_verification` with its exact tests and required scenarios through `gradle-mcp`.
3. Associate each passing focused result with all covered acceptance IDs. Do not substitute compilation or a nearby
   test for a named scenario.

Completion criterion: every focused verification and required scenario passes through `gradle-mcp`.

### 5. Perform bounded simplification

On an initial run, review only the current task diff and minimal surrounding code. On rework, review only code
changed for the validated findings and skip every unrelated simplification:

- **Reuse:** replace new duplication with an already-proven local helper when behavior is identical.
- **Quality:** remove redundant state, needless nesting, copy-paste variation, and comments that restate code.
- **Efficiency:** remove repeated work or avoidable queries only when the diff introduced them or the card requires it.
- **Altitude:** replace a shallow special case with the correct shared-seam fix when it remains inside card scope.

Do not launch a broad cleanup, change behavior, rename public contracts, or expand into unrelated files. Re-run the
affected focused tests after any simplification; revert simplification that cannot be proven behavior-preserving.

Completion criterion: no material in-scope simplification remains and focused verification is still green.

### 6. Run full backend verification

Execute the card's exact `full_backend_verification` task through `gradle-mcp` only after focused verification is
green. On failure, determine whether the cause is in scope:

- Fix an in-scope regression, then repeat affected focused verification and the full task.
- Block on an external prerequisite, policy gap, unexpected pre-existing failure, or fix requiring out-of-scope work.

Completion criterion: the full backend task passes, or the task is blocked with observed evidence and a recovery
owner. Never hand off a failed or unexecuted full verification as passing.

### 7. Self-review and return to the workflow

1. Read Git status and the complete diff. Check behavior, security, module boundaries, persistence, migration order,
   tests, and accidental generated or document files.
2. Confirm changed paths are exactly task code artifacts and every card behavior, acceptance, and scenario has
   passing evidence. Report documentation impact without editing documentation.
3. Return control to `run-impl-card` to construct the canonical handoff and request the PM checkpoint.

Completion criterion: the diff is scoped and reviewable, both verification phases pass, and no unresolved risk is
hidden. The Coder has not staged, committed, pushed, or completed the card.

## Block Instead of Guessing

Block through `run-impl-card` when implementation needs an unspecified business, authorization,
transaction, consistency, API, event, or error contract; when unexpected dirty paths appear; when the card conflicts
with repository architecture; or when required verification cannot run. Preserve the workspace and never use reset,
stash, clean, validator bypasses, or weakened assertions as recovery.

## Verification

Before returning to `run-impl-card`, verify:

- all effective behaviors and acceptance criteria map to implemented code and tests;
- all required focused scenarios passed through `gradle-mcp`;
- the card's full backend task passed through `gradle-mcp`;
- the complete diff contains no documentation or unrelated paths;
- simplification did not change the card contract;
- no commit, push, or native completion was performed.
