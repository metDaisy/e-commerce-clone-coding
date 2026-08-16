# P7 Offer Operations (Offer 운영·활성화 심사)

공통 응답 봉투와 예외 규칙은 [공통 API 계약](../index.md#공통-api-계약)을 따른다. P7 전체 API 목록은 [P7 Admin](p7-admin.md)을 참조한다.

Offer·Inventory의 상세 도메인 규칙은 P9가 소유하고, P7은 관리자의 운영 진입점과 관리자 차단 Offer의 재활성화 심사를 제공한다.

Offer의 현재 비활성화 정보, 상태 변경 이력, 활성화 요청 저장은 [P9 Offer 데이터 모델](../p9/p9-offer.md#2-1-데이터-모델)을 따른다.

## 1. Offer 활성·비활성

`PATCH /api/v1/admin/offers/{offerId}/status`

요청:

```json
{
  "status": "INACTIVE",
  "reasonCode": "POLICY_VIOLATION",
  "sellerMessage": "상품 설명에 허위 효능 표현이 포함되어 비활성화되었습니다."
}
```

- `ADMIN`만 호출할 수 있다.
- `status`는 `ACTIVE` 또는 `INACTIVE`만 허용한다. 보관은 삭제가 아니므로 `ARCHIVED` Offer를 다시 활성화할 수 없다.
- `ACTIVE` 전환은 Seller, CatalogProduct, ProductVariant가 모두 활성인 경우에만 허용한다.
- CatalogProduct 또는 ProductVariant가 보관되면 연결된 Offer는 모두 `INACTIVE`가 되며, 이 API로도 다시 활성화할 수 없다.
- `ADMIN`이 `INACTIVE`로 전환할 때 `reasonCode`와 판매자에게 공개할 `sellerMessage`는 필수다. 내부 감사 메모는 별도로 보관하며 판매자에게 노출하지 않는다.
- 관리자 비활성화의 `inactiveSource`는 `ADMIN`으로 기록한다. 판매자는 직접 `ACTIVE`로 변경할 수 없고 활성화 요청을 제출해야 한다.
- `reasonCode`는 판매자가 사유를 확인하고 조치할 수 있도록 자주 발생하는 4개 범주만 사용한다.

| `reasonCode` | 설명 | 대표 사례 |
|---|---|---|
| `POLICY_VIOLATION` | 플랫폼 또는 판매 정책 위반 | 금지 상품, 허위·과장 표현, 정책상 허용되지 않는 판매 방식 |
| `PRODUCT_INFORMATION_ERROR` | 상품 정보가 실제 상품과 다르거나 불완전함 | 잘못된 상품명·설명·이미지·옵션, 중복 등록, 필수 정보 누락 |
| `INTELLECTUAL_PROPERTY` | 타인의 지식재산권 침해 또는 권리자 이의 제기 | 상표·저작권 침해, 권리자 신고, 무단 브랜드 사용 |
| `SAFETY_OR_COMPLIANCE` | 법률·규제·안전·인증 또는 진품성 요건을 충족하지 못함 | 안전 인증·필수 서류 누락, 규제 상품 요건 미충족, 진품성 증빙 부족 |

구체적인 위반 내용과 판매자가 해결해야 할 조치는 `sellerMessage`에 기록한다. 내부 판단에 필요한 세부 분류는 별도 감사 메모로 보관하고, `reasonCode`를 불필요하게 세분화하지 않는다.

응답 `200`:

```json
{
  "offerId": "uuid",
  "variantId": "uuid",
  "sellerId": "uuid",
  "status": "INACTIVE",
  "inactiveSource": "ADMIN",
  "inactiveReasonCode": "POLICY_VIOLATION",
  "sellerMessage": "상품 설명에 허위 효능 표현이 포함되어 비활성화되었습니다.",
  "updatedAt": "2026-08-10T12:00:00Z"
}
```

가격 운영 수정과 재고 운영 조정의 세부 규칙은 P9 Offer·Inventory가 소유한다. P7은 다음 관리자 진입점을 제공한다.

- `PATCH /api/v1/admin/offers/{offerId}/price`
- `POST /api/v1/admin/offers/{offerId}/inventory-adjustments`

## 2. Offer 활성화 요청 심사

판매자는 관리자에 의해 비활성화된 Offer의 문제를 해결한 뒤 활성화 요청을 제출한다.

`GET /api/v1/admin/offers/activation-requests`

- `PENDING`, `APPROVED`, `REJECTED` 상태를 필터링할 수 있다.
- 기본 정렬은 `createdAt DESC, requestId DESC`다.
- 하나의 Offer에는 처리 중인 `PENDING` 요청을 하나만 둘 수 있다.
- 목록과 상세에는 Offer·Seller 정보, 관리자 비활성화 사유, 판매자의 해결 설명, 요청 시각을 포함한다.

승인:

`POST /api/v1/admin/offers/activation-requests/{requestId}/approve`

- 요청 본문은 없다. 승인 자체에 별도 사유를 요구하지 않는다.
- `PENDING` 요청만 승인할 수 있다.
- Seller, CatalogProduct, ProductVariant가 모두 활성이고 관리자가 비활성화 사유가 해결되었다고 판단한 경우에만 승인한다.
- 승인 시 Offer를 `ACTIVE`로 변경하고 처리 관리자와 처리 시각을 기록한다.

거절:

`POST /api/v1/admin/offers/activation-requests/{requestId}/reject`

```json
{
  "reasonCode": "ISSUE_NOT_RESOLVED",
  "message": "상품 설명에서 문제 문구가 아직 제거되지 않았습니다."
}
```

- `reasonCode`와 판매자에게 공개할 `message`는 필수다.
- 활성화 요청 거절의 `reasonCode`는 다음 3개만 사용한다.

| `reasonCode` | 설명 |
|---|---|
| `ISSUE_NOT_RESOLVED` | 관리자 비활성화 사유가 아직 해결되지 않음 |
| `DEPENDENCY_NOT_ACTIVE` | Seller, CatalogProduct, ProductVariant 중 하나 이상이 비활성 상태임 |
| `INSUFFICIENT_EVIDENCE` | 수정 설명이나 증빙이 활성화 판단에 충분하지 않음 |

- Offer는 `INACTIVE`로 유지하고 처리 관리자·처리 시각을 기록한다.
- 판매자는 문제를 추가로 해결한 뒤 새 활성화 요청을 제출할 수 있다.
