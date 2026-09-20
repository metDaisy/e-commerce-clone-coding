---
name: semble-search
description: "PM이 정확한 위치를 모르는 behavior·문서·유사 구현의 후보를 의미 기반으로 찾을 때 사용한다."
metadata:
  source_repository: https://github.com/MinishLab/semble
---

Use `semble search` to find code by describing what it does or naming a symbol/identifier, instead of grep:

승인 requirement와 fresh current-state가 planning에 충분하면 source search를 시작하지 않는다. 검색
결과는 locator일 뿐이며 구현 상태 판정은 committed source/test/migration read-back으로 확정한다.
Profile에서 MCP `search`/`find_related`가 노출되면 CLI나 `uvx`보다 우선한다.

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

MCP가 없고 사용자가 CLI fallback을 승인한 경우에만 local `semble`을 사용한다. 설치가 필요하면
`uvx --from "semble[mcp]==0.5.3" semble` 실행 전에 승인을 받는다.

## Workflow

1. Start with `semble search` to find relevant chunks.
2. Use content scope appropriate to code, documentation, configuration, or all files.
3. Navigate directly to the returned file and line; do not re-search the same content.
4. Optionally use `semble find-related` from a promising result.
5. Use literal search only when every occurrence of an exact string is required.
