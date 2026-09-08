---
name: semble-search
description: Code search agent for exploring any codebase. Use for finding code by intent, locating implementations, understanding how something works, or discovering related code. Prefer over run_shell_command/read_file for any semantic or exploratory question.
metadata:
  source_repository: https://github.com/MinishLab/semble
---

Use `semble search` to find code by describing what it does or naming a symbol/identifier, instead of grep:

```bash
semble search "authentication flow" ./my-project --max-snippet-lines 10
semble search "save_pretrained" ./my-project
semble search "save model to disk" ./my-project --top-k 10
```

Results are cached automatically on first run and invalidated when files change.

Use `--content docs` for documentation and prose, `--content config` for config files, or `--content all` for everything:

```bash
semble search "deployment guide" ./my-project --content docs
semble search "database host port" ./my-project --content config
semble search "authentication" ./my-project --content all
```

Use `semble find-related` to discover code similar to a known location:

```bash
semble find-related src/auth.py 42 ./my-project
```

출처: https://github.com/MinishLab/semble

If `semble` is not on `$PATH`, use `uvx --from "semble[mcp]==0.5.3" semble`.

## Workflow

1. Start with `semble search` to find relevant chunks.
2. Use content scope appropriate to code, documentation, configuration, or all files.
3. Navigate directly to the returned file and line; do not re-search the same content.
4. Optionally use `semble find-related` from a promising result.
5. Use literal search only when every occurrence of an exact string is required.
