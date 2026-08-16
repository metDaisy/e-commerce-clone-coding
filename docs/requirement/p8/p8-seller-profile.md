# P8 Seller API

이 문서는 승인된 `Seller` 프로필과 상태 이력, 프로필 조회·수정 API를 정의한다. 업무 정책은 [P8 Seller Policy](p8-policy.md), 관리자 상태 변경은 [P7 Access](../p7/p7-access.md)를 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `Seller` | 승인된 판매자 프로필과 현재 상태 | 프로필 조회·수정 |
| `SellerStatusHistory` | Seller 정지·재활성화의 불변 이력 | P7 관리자 상태 변경에서 기록 |
| `User` | Seller 소유 User와 역할 집합 | P1 User 원본 참조 |

- Seller는 P8이 소유하고 User의 공개 식별자만 논리적으로 참조한다.
- `Seller.sellerId`와 `User.userId`는 별도 식별자이며, `Seller.userId`는 유일하다.
- Seller 상태 변경과 `PRODUCT_MANAGER` 역할 변경은 P7의 관리자 흐름을 따른다.

## 2. 데이터 모델

### 2-1. `Seller`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `sellerId` | UUID | 예 | Seller 프로필 식별자 |
| `userId` | UUID | 예 | 소유 User. Seller 사이에서 유일 |
| `displayName` | VARCHAR(255) | 예 | 고객에게 표시할 판매자명 |
| `businessName` | VARCHAR(255) | 예 | 사업자명 |
| `contactEmail` | VARCHAR(255) | 예 | 판매자 연락처 이메일 |
| `contactPhone` | VARCHAR(20) | 예 | 판매자 연락처 전화번호 |
| `status` | ENUM | 예 | `ACTIVE`, `SUSPENDED` |
| `createdAt` | TIMESTAMP | 예 | Seller 생성 시각 |
| `updatedAt` | TIMESTAMP | 예 | 최종 변경 시각 |

- Seller는 SellerApplication 승인 시 `ACTIVE`로 생성한다.
- Seller에는 신청·심사 원본과 민감한 신청 증빙 원문을 저장하지 않는다.

### 2-2. `SellerStatusHistory`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `historyId` | UUID | 예 | 상태 이력 식별자 |
| `sellerId` | UUID | 예 | 상태가 변경된 Seller |
| `fromStatus` | ENUM | 예 | 변경 전 상태. `ACTIVE`, `SUSPENDED` |
| `toStatus` | ENUM | 예 | 변경 후 상태. `ACTIVE`, `SUSPENDED` |
| `changedByUserId` | UUID | 예 | 상태 변경을 처리한 ADMIN |
| `reasonCode` | VARCHAR(64) | 아니오 | 상태 변경 사유 코드 |
| `message` | VARCHAR(500) | 아니오 | 판매자에게 공개할 안내 메시지 |
| `changedAt` | TIMESTAMP | 예 | 상태 변경 시각 |

### 2-3. 관계와 제약

- User와 Seller는 `1:0..1`이며 `Seller.userId`에는 유일성 제약을 둔다.
- Seller 상태는 `ACTIVE ↔ SUSPENDED`만 허용한다. Seller에는 `INACTIVE` 상태를 사용하지 않는다.
- SellerStatusHistory는 상태 변경마다 추가하고 수정·삭제하지 않는다.
- Seller가 `SUSPENDED`가 되면 P9가 `ARCHIVED`가 아닌 Offer를 `INACTIVE`로 변경한다. Offer·Inventory는 Seller API가 삭제하지 않는다.

## 3. API 정의

### 3-1. Seller 프로필 조회

`GET /api/v1/seller/profile`

권한: 인증된 Seller 소유자. Seller 상태가 `ACTIVE` 또는 `SUSPENDED`이면 허용한다. `PRODUCT_MANAGER` 역할은 요구하지 않는다.

#### 성공 응답: `200 OK`

```json
{
  "sellerId": "uuid",
  "displayName": "Example Store",
  "businessName": "Example Inc.",
  "contactEmail": "seller@example.com",
  "contactPhone": "010-1234-5678",
  "status": "SUSPENDED",
  "updatedAt": "2026-08-16T12:00:00Z"
}
```

- `Seller.userId`와 인증 User의 `userId`가 일치해야 한다.
- 조회는 읽기 전용이며 Seller 상태·역할 집합을 변경하지 않는다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | `SELLER-003` | 다른 User의 Seller 조회 | 판매자 정보를 조회할 수 없습니다. | 없음 | 요청 User와 Seller 식별자 |
| 404 | `SELLER-001` | 승인된 Seller가 없음 | 판매자 승인이 필요합니다. | 없음 | User와 Seller 조회 결과 |

### 3-2. Seller 프로필 수정

`PATCH /api/v1/seller/profile`

권한: `PRODUCT_MANAGER` + `Seller.status=ACTIVE`인 Seller 소유자

요청:

```json
{
  "displayName": "Updated Store",
  "contactPhone": "010-9876-5432"
}
```

- `displayName`, `contactEmail`, `contactPhone`만 수정할 수 있다.
- `sellerId`, `userId`, `businessName`, `status`는 요청으로 수정할 수 없다.

#### 성공 응답: `200 OK`

```json
{
  "sellerId": "uuid",
  "displayName": "Updated Store",
  "businessName": "Example Inc.",
  "contactEmail": "seller@example.com",
  "contactPhone": "010-9876-5432",
  "status": "ACTIVE",
  "updatedAt": "2026-08-16T12:05:00Z"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `SELLER-005` | 프로필 필드 검증 실패 | 프로필 정보를 확인해 주세요. | 실패 필드와 수정 방법 | 내부 검증 원인과 requestId |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | [SELLER-001](p8-seller-profile.md) | — | — | — | — |
| 403 | [SELLER-002](p8-seller-profile.md) | — | — | — | — |
| 404 | `SELLER-004` | Seller가 존재하지 않음 | 판매자 정보를 찾을 수 없습니다. | 없음 | Seller 식별자와 조회 원인 |
