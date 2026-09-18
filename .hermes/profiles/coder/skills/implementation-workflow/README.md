# implementation-workflow

`implementation-coder`가 PM-authored backend Impl card를 구현하고 두 단계 검증을 거쳐 같은
card의 PM checkpoint를 요청하는 절차다.

- [`references/execution-contract.md`](references/execution-contract.md): PM/Coder 권한과 lifecycle
- [`references/implementation-card-contract.md`](references/implementation-card-contract.md): body와 handoff schema
- [`SKILL.md`](SKILL.md): Coder 실행 순서

검증은 focused tests 다음 backend 전체 Gradle test 순서이며, 모든 Gradle 작업은
`gradle-mcp`로 실행한다.