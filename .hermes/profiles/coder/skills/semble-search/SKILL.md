---
name: semble-search
description: Code search agent for exploring any codebase. Use for finding code by intent, locating implementations, understanding how something works, or discovering related code. Prefer over broad literal search for semantic discovery.
metadata:
  source_repository: https://github.com/MinishLab/semble
---

# Semble Search

Use Semble when the implementation location or related behavior is unclear. Use `find-related` from a promising result when a similar implementation is needed.

## Workflow

1. Search by behavior or symbol with a narrow query.
2. Choose the appropriate content scope when searching documentation or configuration.
3. Read the returned repository file and line before treating the result as evidence.
4. Use `find-related` only when a known result needs analogous code.
5. Use literal repository search when every occurrence of an exact identifier or string is required.

Semble is a locator, not proof. Confirm conclusions with the actual source, tests, module declarations, and repository rules.
