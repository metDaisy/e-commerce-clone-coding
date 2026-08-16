# P7 Access & Seller Review (권한·판매자 심사)

공통 응답 봉투와 예외 규칙은 [공통 API 계약](../index.md#공통-api-계약)을 따른다. P7 전체 API 목록은 [P7 Admin](p7-admin.md)을 참조한다.

## 1. 사용자 권한 변경

`POST /api/v1/admin/users/{userId}/roles`

요청:

```json
{
  "role": "PRODUCT_MANAGER"
}
```

- 추가 대상 역할은 `PRODUCT_MANAGER`, `ADMIN`이다. `USER`는 가입 시 자동 부여되는 기본 역할이므로 이 API로 추가하지 않는다.
- `PRODUCT_MANAGER`는 활성 `Seller`를 가진 판매자에게만 추가할 수 있다.
- 판매자 신청 승인 시 `Seller.status`를 `ACTIVE`로 변경하고 사용자의 역할 집합에 `PRODUCT_MANAGER`를 추가한다. 기존 `USER` 역할은 유지한다.
- 판매자 정지 또는 승인 취소 시 판매자 API를 차단하고 역할 집합에서 `PRODUCT_MANAGER`만 제거한다. `USER` 역할은 유지한다.
- 역할이 이미 추가되어 있으면 `ROLE_ALREADY_ASSIGNED`를 반환한다.
- `DELETE /api/v1/admin/users/{userId}/roles/{role}`는 `PRODUCT_MANAGER` 또는 `ADMIN`만 삭제할 수 있다.
- 역할 삭제 성공 시 본문 없이 `204 No Content`를 반환한다.
- 요청자는 자신의 `ADMIN` 역할을 해제할 수 없고, `USER` 역할 삭제 요청은 거부한다.
- 대상 사용자가 없으면 `USER_NOT_FOUND`를 반환한다.
- 실제 역할 집합 변경이 성공하면 대상 사용자의 모든 로그인 세션을 즉시 무효화한다. 모든 기기의 Access Token과 Refresh Token을 포함하며, 세션을 물리적으로 삭제하거나 사용 금지하는 방식은 P11 Auth가 결정한다.
- 역할 집합 변경 전에 발급된 Access Token은 만료 시각이 남아 있어도 더 이상 사용할 수 없고, Refresh Token으로 갱신할 수도 없다. 대상 사용자는 다음 요청에서 `401 INVALID_TOKEN`을 받고 새로 로그인해야 한다.
- 역할 집합 변경 성공 시 P1 User가 `UserRolesChangedEvent`를 생성한다. 이벤트는 Outbox에 역할 집합 변경과 같은 트랜잭션으로 기록하며, P11 Auth가 이벤트를 소비해 세션을 무효화한다.
- 최초 이벤트 소비와 세션 무효화는 권한 변경 처리 중 동기적으로 완료되어야 한다. 세션 무효화에 실패하면 역할 변경도 성공으로 처리하지 않는다.
- 역할 변경 성공 응답은 역할 변경, 이벤트 기록, 최초 세션 무효화가 모두 완료된 경우에만 반환한다. 커밋 이후 장애나 재전달은 P6 재시도 규칙을 따르며, 소비자는 `eventId` 기준으로 멱등 처리한다.

성공 응답 `200`:

```json
{
  "userId": "uuid",
  "roles": ["USER", "PRODUCT_MANAGER"],
  "updatedAt": "2026-08-09T12:00:00Z"
}
```

## 2. 판매자 신청 관리

판매자 신청은 P8 Seller가 소유한다. 신청 시 `Seller` 레코드를 `PENDING`으로 저장하고, P7은 저장된 레코드를 관리자 심사 대상으로 조회·변경한다. 상세 데이터 모델은 [P8 Seller 데이터 모델](../p8/p8-seller.md#2-1-데이터-모델)을 따른다.

`PATCH /api/v1/admin/seller-applications/{sellerId}/status`

승인 요청:

```json
{
  "status": "ACTIVE"
}
```

- `PENDING → ACTIVE` 또는 `PENDING → REJECTED` 전환을 허용한다.
- `ACTIVE` 승인 요청에는 별도 사유를 요구하지 않는다.
- 승인 시 `reviewedByUserId`와 `reviewedAt`만 기록하고 `reviewReasonCode`, `reviewMessage`는 비워 둔다.

거절 요청:

```json
{
  "status": "REJECTED",
  "reasonCode": "DOCUMENTS_INVALID_OR_INCOMPLETE",
  "message": "제출한 사업자 증빙 서류를 확인할 수 없거나 필수 정보가 누락되었습니다."
}
```

| `reasonCode` | 설명 |
|---|---|
| `DOCUMENTS_INVALID_OR_INCOMPLETE` | 제출 서류가 유효하지 않거나 필수 서류가 누락됨 |
| `INFORMATION_MISMATCH` | 신청 정보와 증빙 서류의 이름·주소·사업자 정보가 일치하지 않음 |
| `ELIGIBILITY_NOT_MET` | 판매자 등록 또는 판매 자격 요건을 충족하지 않음 |

- 거절 시 `reviewReasonCode`와 `reviewMessage`를 저장한다.
- `ACTIVE → SUSPENDED` 전환으로 판매자 Offer 등록·수정을 차단한다.
- 승인·정지 이력과 처리 관리자를 기록한다.
- 거절된 레코드는 이력으로 보존하고, 재신청은 새로운 `Seller` 레코드로 저장한다. 동일 User에게 `PENDING` 또는 `ACTIVE` 레코드는 하나만 허용한다.

