# P7 Index (Admin & Operations)

P7은 관리자와 운영자의 관리 기능 및 운영 API 진입점을 정의한다. 실제 도메인 데이터와 상태의 소유권은 각 도메인에 남겨두고, P7은 권한 검증과 관리 작업의 조합을 담당한다.

## 1. 범위

- 사용자 권한 변경
- Category·CatalogProduct 관리
- Offer 활성·비활성
- 판매자 신청과 Catalog 등록 요청 심사
- 여러 도메인의 관리자 심사 통합 조회
- 쿠폰 관리
- 배송 운영
- 이벤트·Saga 운영 상태 조회

## 2. 문서 구성

- [P7 Admin & Operations](p7-admin.md): 범위, 공통 규칙, 전체 API 목록과 공통 예외
- [P7 Access & Seller Review](p7-access.md): 사용자 역할 변경, 세션 무효화, 판매자 신청 심사
- [P7 Catalog Administration](p7-catalog.md): Catalog 관리 문서 모음
- [P7 Category Administration](p7-category.md): Category 생성·수정
- [P7 CatalogProduct Administration](p7-catalog-products.md): CatalogProduct·ProductVariant 관리자 목록
- [P7 Catalog Registration Review](p7-catalog-requests.md): 판매자 Catalog 등록 요청 심사
- [P7 Offer Operations](p7-offer.md): Offer 활성·비활성, 활성화 요청 심사, 가격·재고 운영 진입점
- [P7 Operations](p7-operations.md): 쿠폰·배송·Outbox·Saga 운영

## 3. 도메인 경계

모든 P7 API는 `ADMIN` 권한을 기본으로 요구한다. P7은 P2·P4·P6·P8·P9·P10의 공개 application interface를 사용하며 각 도메인의 내부 구현을 직접 참조하지 않는다.

공통 응답 형식과 HTTP 예외 규칙은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.
