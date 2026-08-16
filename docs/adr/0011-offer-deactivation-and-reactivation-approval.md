# ADR-0011: Offer 비활성화와 관리자 재활성화 승인

- Status: Accepted
- Date: 2026-08-16
- Deciders: 사용자와 Codex
- Supersedes: 없음
- Superseded by: 없음

## Context

Offer는 판매자가 직접 판매를 중단할 수도 있고, 관리자 또는 시스템이 정책·판매자 상태·Catalog 상태에 따라 비활성화할 수도 있다. 단순히 `status = INACTIVE`만 저장하면 비활성화 주체와 사유, 재활성화 권한을 구분할 수 없다.

관리자가 정책 위반으로 비활성화한 Offer를 판매자가 즉시 다시 활성화할 수 있으면 관리자 조치가 무력화된다. 반대로 판매자의 자발적 비활성화까지 관리자 승인 대상으로 만들면 정상적인 판매 운영이 불필요하게 복잡해진다.

## Decision

- Offer는 `inactiveSource`를 `SELLER`, `ADMIN`, `SYSTEM` 중 하나로 기록한다.
- 판매자 자발적 비활성화는 사유를 필수로 요구하지 않고 `SELLER_REQUEST`로 기록한다. 모든 의존 상태가 정상이면 판매자가 직접 재활성화할 수 있다.
- 관리자 비활성화는 사유 코드와 판매자 공개 안내 문구를 필수로 요구한다. 관리자 차단 Offer는 판매자가 직접 재활성화할 수 없다.
- 관리자 차단 Offer의 판매자는 문제 해결 설명을 포함한 `OfferActivationRequest`를 제출한다. 관리자가 승인해야 Offer가 `ACTIVE`가 된다.
- 관리자는 활성 Seller·CatalogProduct·ProductVariant와 해결 내용을 확인한 뒤 요청을 승인한다. 거절 시 사유를 기록하고 Offer는 `INACTIVE`로 유지한다.
- Catalog 보관이나 Seller 정지로 인한 시스템 비활성화는 원인이 해결되기 전까지 재활성화할 수 없다. 원인이 해결되어도 자동으로 활성화하지 않고 명시적인 활성화 동작을 요구한다.
- Offer 상태 변경과 활성화 요청 처리는 P9가 소유하고, 관리자 HTTP 진입점은 P7이 제공한다.

## Consequences

### Positive

- 관리자 차단을 판매자가 우회할 수 없다.
- 판매자는 비활성화 이유와 해결해야 할 조치를 확인할 수 있다.
- 판매자의 일상적인 판매 중단은 관리자 승인 없이 처리할 수 있다.
- 비활성화 이력과 승인 이력을 감사·운영 화면에서 추적할 수 있다.

### Negative

- Offer 상태 외에 비활성화 주체·사유·안내 문구와 요청 이력이 필요하다.
- 관리자용 활성화 요청 목록·승인·거절 API와 멱등성 처리가 필요하다.
- 시스템 비활성화 원인 해소 후에도 별도 활성화 절차가 필요하다.

## Follow-up

- P9 Offer 응답에 판매자용 비활성화 사유와 활성화 요청 상태를 포함한다.
- 관리자 비활성화·판매자 요청·관리자 승인·거절 이벤트와 감사 이력을 정의한다.
- Seller가 관리자 차단 Offer를 직접 활성화할 수 없는 경계 테스트를 추가한다.

## Evidence

 - [P7 Offer 운영](../requirement/p7/p7-offer.md)
- [P9 Offer 상태·보관](../requirement/p9/p9-offer.md)
- [Project Context](../../CONTEXT.md)
