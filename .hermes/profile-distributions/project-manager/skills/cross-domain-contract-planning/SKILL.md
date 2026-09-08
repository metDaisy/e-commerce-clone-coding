---
name: cross-domain-contract-planning
description: Use when a task needs another domain's absent or changing capability. Decide and freeze the smallest cross-domain contract before scheduling consumer and producer implementation.
version: 1.0.0
author: leee, Hermes Agent
license: MIT
platforms: [linux, macos, windows]
metadata:
  hermes:
    tags: [planning, kanban, domain-boundary, named-interface, domain-event, contract]
    related_skills: [write-task]
---

# Cross-Domain Contract Planning

Use when a consumer-domain feature requires a capability owned by another domain and the
producer implementation or its public contract is absent, partial, or uncertain. This Skill
coordinates the seam; it does not implement either domain.

`SOUL.md` remains authoritative for PM lifecycle, routing, and mutation policy.
`write-task` remains authoritative for general evidence discovery and graph authoring.
`board-contract.md` remains authoritative for persisted task fields and validator rules.
This Skill defines only the cross-domain contract decision, its handoff, and the
contract-specific follow-up graph.

## Core rule

Do **not** automatically make the consumer wait for the producer's complete implementation.
First determine whether a small, stable integration contract is sufficient. The required
precondition is a verified contract decision, not necessarily completed producer code.

Use `blocked` only when a product or architecture decision is genuinely unavailable. Do not use
`blocked` as shorthand for “producer code does not exist yet.”

## Discovery and decision

Before creating or changing a graph:

1. Read the consumer requirement and identify the exact capability it needs; do not name a
   producer entity merely because it has a similar name.
2. Read the consumer and producer modules' `package-info.java`, existing public
   `@NamedInterface`s, events, tests, and relevant ADRs from `planning_head_sha`.
3. Confirm whether the capability is already published, partial, absent, or conflicting. An
   internal entity, repository, controller, or schema is not a public cross-module contract.
4. Select one integration mode and record why it satisfies the consumer:
   - `named-interface-query`: the consumer needs a current answer and synchronous consistency.
   - `domain-event-projection`: the consumer can tolerate explicit eventual consistency and owns a
     local read model/projection.
   - `needs-input`: ownership, consistency, authorization, or business semantics cannot be
     decided from committed evidence.
5. For `named-interface-query` or `domain-event-projection`, create a dedicated contract-decision
   task before dependent implementation cards. Use `task_type: investigation`; its title and scope
   must explicitly state that its deliverable is a frozen cross-domain contract, not generic research.

## Contract-decision task

The task must produce a `comment.cross_domain_contract` handoff with all fields below:

```text
consumer_module
producer_module
capability
integration_mode: named-interface-query | domain-event-projection
contract_owner
public_surface
request_or_event
response_or_projection
authorization_boundary
not_found_and_error_semantics
consistency_and_transaction_semantics
ordering_paging_or_idempotency
versioning_or_evolution_rule
producer_implementation_follow_up
consumer_implementation_follow_up
verification_plan
adr_decision: required | not-required, with reason
```

Rules:

- `public_surface` names a small producer `@NamedInterface` contract or a fact-named domain event.
  It must never expose a producer JPA entity, repository, or internal package.
- An event states a fact already occurred. Do not disguise a command as an event. Record delivery,
  duplicate/idempotency, ordering, and projection rebuild behavior when relevant.
- A query contract states ownership/authorization, missing-data behavior, and result semantics.
  Add pagination/sorting only when the consumer needs them.
- Treat the contract as source-level design evidence until its producer and consumer tests exist;
  do not claim the integration is implemented merely because the decision task is done.
- Evaluate an ADR only when the choice is hard to reverse, surprising without context, and involves
  a real trade-off. Otherwise record the rationale in the task comment.

## Kanban graph rules

Use the smallest graph justified by the decision:

```text
contract decision
  ├─ producer contract implementation
  └─ consumer implementation
       └─ integration/module verification
```

- The producer and consumer implementation tasks may proceed in parallel after the contract task is
  `done`, provided their scopes do not overlap and each is independently testable.
- A consumer may use a fake adapter or contract fixture while the producer is unfinished. Its
  acceptance criteria must require replacement with the real module seam and an integration/module
  verification before the Issue is finalized.
- If only one side is needed for the Issue, create only that side plus the verification necessary to
  prove the public seam. Do not pre-create speculative producer work.
- If `needs-input` is selected, create one explicitly blocked decision task. State the unresolved
  question and the person/role able to decide it; do not fabricate an event or API.
- A consumer task must depend on the contract-decision task, not on unrelated producer implementation
  merely because that implementation happens to be in the same domain.

## Evidence and verification

Each contract-decision task cites:

- `goal`: the consumer requirement;
- `state`: committed producer/consumer public seam evidence or confirmed absence;
- `constraint`: both modules' `package-info.java` and any relevant ADR.

Its verification is a manual architecture review that reads the durable contract comment and confirms:

1. the chosen mode fits the required consistency;
2. the public surface respects Modulith boundaries;
3. the owner and follow-up tasks are explicit;
4. the contract contains observable test criteria; and
5. no internal type crosses the module boundary.

Producer and consumer implementation cards must each have focused behavioral tests. Add a
Spring Modulith structure test and an integration/module test when the public seam or event wiring
changes. All Gradle verification uses `gradle-mcp`.

## Completion routing

After the contract-decision task completes, read back the comment and generated task ID. Create or
promote only the producer/consumer cards justified by that decision. If the decision identifies no
remaining gap, record the evidence and do not create implementation work. If committed evidence
contradicts the decision, return to `needs-input` rather than silently changing the contract.
