# Local Skills Index

This document is a catalog of personal local skills installed in `$AGENT_SKILLS`. It provides names and conditions for use without injecting detailed instructions into every conversation.

## Usage Rules

1. Only select skills that clearly match the current task.
2. If selected, read the entire `SKILL.md` for that skill and apply the instructions before starting the task.
3. If multiple skills overlap, use the minimum combination necessary to satisfy the task.
4. Skills are operational procedures. The source of truth for project facts is the code and `docs/`.
5. If the local path does not exist or the environment is different, do not assume the skill is a hard dependency; report possible alternatives.

## Catalog

| Skill | When to Read | Location |
|---|---|---|
| `caveman` | When keeping internal reasoning and user responses short but complete. | `$AGENT_SKILLS/caveman/SKILL.md` |
| `codebase-design` | When designing or improving module interfaces, seams, adapters, dependency directions, and test surfaces. | `$AGENT_SKILLS/codebase-design/SKILL.md` |
| `improve-codebase-architecture` | When investigating candidates for structural improvement across the codebase and comparing them with visual reports. | `$AGENT_SKILLS/improve-codebase-architecture/SKILL.md` |
| `java-springboot` | When implementing or reviewing Spring Boot configuration, layers, transactions, dependency injection, and web/data features. | `$AGENT_SKILLS/java-springboot/SKILL.md` |
| `java-junit` | When writing or reviewing JUnit 5 unit tests, parameterized tests, test structure, and assertions. | `$AGENT_SKILLS/java-junit/SKILL.md` |
| `java-refactoring-extract-method` | When performing refactoring to extract cohesive logic from long Java methods into separate methods. | `$AGENT_SKILLS/java-refactoring-extract-method/SKILL.md` |
| `java-refactoring-remove-parameter` | When safely removing unused or unnecessary Java parameters and verifying the impact on call sites. | `$AGENT_SKILLS/java-refactoring-remove-parameter/SKILL.md` |
| `agent-browser` | When website navigation, form input, browser automation, screenshots, or web application QA is required. | `$AGENT_SKILLS/agent-browser/SKILL.md` |
| `semble` | When locating code implementations by meaning or behavior using semantic search. | `$AGENT_SKILLS/semble/SKILL.md` |
| `codebase-memory` | When verifying relationships for symbols, tracing paths, or understanding the knowledge graph of the codebase. | `$AGENT_SKILLS/codebase-memory/SKILL.md` |

## Maintenance

- Update this table only when skills are added, removed, or their descriptions change in `$AGENT_SKILLS`.
- Do not copy the body of a skill into this repository. Separate the change cycles of personal skills and project documentation.
