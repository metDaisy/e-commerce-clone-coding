# ADR-0001: 요구사항을 공통 인덱스와 도메인 문서로 분리

- Status: Accepted
- Date: 2026-08-09
- Deciders: 사용자와 Codex
- Supersedes: 없음
- Superseded by: 없음

## Context

기존 `docs/requirement.md` 하나에 P1~P6의 비즈니스 규칙, API 계약, 예외, 상태 전이가 함께 들어 있어 특정 도메인의 구현 기준을 찾기 어렵다. 문서가 커질수록 한 도메인의 변경이 다른 도메인 규칙을 실수로 덮어쓸 위험도 커진다.

## Decision Drivers

- 구현자가 도메인별 기준을 빠르게 찾을 수 있어야 한다.
- 공통 API 응답·예외·인증 규칙은 문서마다 중복되지 않아야 한다.
- 요구사항과 심화사항을 같은 도메인 문서에서 함께 추적할 수 있어야 한다.
- 기존 P1~P6 구현 순서를 유지해야 한다.

## Considered Options

### Option A: 단일 `requirement.md` 유지

문서 위치는 단순하지만 파일이 계속 커지고, 도메인별 변경 범위와 책임이 불명확해진다.

### Option B: 공통 인덱스와 도메인별 문서로 분리

문서 수는 늘어나지만 공통 계약과 도메인 규칙을 분리하고, P1~P6별로 독립적으로 갱신할 수 있다.

## Decision

Option B를 선택한다.

- `docs/requirement/index.md`는 문서 인덱스와 공통 API 계약의 기준으로 사용한다.
- `p1-user-auth.md`부터 `p6-infrastructure.md`까지 도메인별 문서에 URI, 요청·응답, 예외, 요구사항, 심화사항을 기록한다.
- 기존 `docs/requirement.md`는 `docs/requirement/index.md`로 이동한 것으로 취급하고, 새 문서에는 도메인별 링크를 둔다.
- 공통 규칙은 인덱스에만 기록하고, 도메인 문서에는 해당 도메인에 특화된 규칙만 기록한다.
- `docs/index.md`를 문서 진입점으로 유지한다.

## Consequences

### Positive

- 구현 대상 도메인의 요구사항을 좁은 범위에서 확인할 수 있다.
- API 계약과 예외 형식의 중복 정의가 줄어든다.
- 이후 P2~P6을 실제 서비스 수준으로 확장할 때 해당 문서만 갱신할 수 있다.

### Negative

- 문서가 여러 파일로 나뉘어 링크 관리가 필요하다.
- 공통 규칙과 도메인 규칙의 경계를 잘못 정하면 중복이나 누락이 생길 수 있다.

### Follow-up

- 새 도메인을 추가하면 `docs/requirement/index.md`에 링크를 등록한다.
- API 계약 변경 시 공통 인덱스와 영향을 받는 도메인 문서를 함께 검토한다.
- 구현 완료 범위는 요구사항 문서가 아니라 `docs/current-state.md`에 기록한다.

## Evidence

- [요구사항 인덱스](../requirement/index.md)
- [문서 진입점](../index.md)

