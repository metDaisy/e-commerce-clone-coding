# P8 SellerApplication API

이 문서는 `SellerApplication`과 `SellerApplicationReview` 모델, 판매자 신청 API를 정의한다. 업무 정책은 [P8 Seller Policy](p8-policy.md), 관리자 승인·거절은 [P7 Access](../p7/p7-access.md)를 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `SellerApplication` | User의 Seller 신청 원본과 처리 상태 | 신청 생성 |
| `SellerApplicationReview` | 승인·거절 심사 결과의 불변 이력 | P7 관리자 심사에서 기록 |
| `Seller` | 승인 결과로 연결되는 판매자 프로필 | [Seller API](p8-seller-profile.md) |

- P8은 신청 원본과 심사 이력을 소유한다.
- P7은 관리자 HTTP 진입점에서 P8의 공개 계약을 호출해 신청을 승인·거절한다.
- 신청 승인 전에는 `Seller`를 생성하지 않는다.

## 2. 데이터 모델

<a id="sellerapplication"></a>
### 2-1. `SellerApplication`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `applicationId` | UUID | 예 | 신청 식별자 |
| `userId` | UUID | 예 | 신청 User. 인증 정보에서 설정 |
| `businessName` | VARCHAR(255) | 예 | 신청 당시 제출한 회사명 |
| `contactEmail` | VARCHAR(255) | 예 | 신청 당시 제출한 연락처 이메일 |
| `contactPhone` | VARCHAR(20) | 예 | 신청 당시 제출한 연락처 전화번호 |
| `status` | ENUM | 예 | `PENDING`, `APPROVED`, `REJECTED` |
| `sellerId` | UUID | 아니오 | 승인 결과로 생성된 Seller |
| `createdAt` | TIMESTAMP | 예 | 신청 생성 시각 |
| `updatedAt` | TIMESTAMP | 예 | 신청 상태·Seller 연결의 최종 변경 시각 |

<a id="sellerapplicationreview"></a>
### 2-2. `SellerApplicationReview`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `reviewId` | UUID | 예 | 심사 이력 식별자 |
| `applicationId` | UUID | 예 | 대상 SellerApplication |
| `reviewedByUserId` | UUID | 예 | 심사를 처리한 ADMIN |
| `decision` | ENUM | 예 | `APPROVED`, `REJECTED` |
| `reasonCode` | VARCHAR(64) | 아니오 | 거절 사유 코드. 승인 시 비움 |
| `message` | VARCHAR(500) | 아니오 | 판매자에게 공개할 심사 결과 메시지 |
| `reviewedAt` | TIMESTAMP | 예 | 심사 처리 시각 |

### 2-3. 관계와 제약

- User와 SellerApplication은 `1:N`이다. User당 `PENDING` 신청은 하나만 허용한다.
- `PENDING` 신청 승인 시 `SellerApplication.sellerId`에 새 Seller의 `sellerId`를 기록한다.
- 거절된 신청은 수정·삭제하지 않으며, 재신청은 새 SellerApplication으로 저장한다.
- 처리된 심사 이력은 수정·삭제하지 않는다. 승인 시 `reasonCode`·`message`는 비우고, 거절 시 둘 다 필수다.
- `businessRegistrationNumber` 같은 증빙 원문은 MVP에서 받거나 저장하지 않는다.

## 3. API 정의

### 3-1. Seller 신청 생성

`POST /api/v1/seller/applications`

권한: 로그인한 User. `ADMIN`의 신청은 허용하지 않는다.

요청:

```json
{
  "businessName": "Example Inc.",
  "contactEmail": "seller@example.com",
  "contactPhone": "010-1234-5678"
}
```

- `userId`, `sellerId`, `applicationId`는 요청 본문으로 받지 않는다.
- 신청 당시 연락처는 심사와 승인된 Seller 프로필 생성에 사용한다.
- 생성 시 `status=PENDING`, `sellerId=null`로 저장한다.

#### 성공 응답: `201 Created`

```json
{
  "applicationId": "uuid",
  "businessName": "Example Inc.",
  "status": "PENDING",
  "createdAt": "2026-08-16T12:00:00Z"
}
```

승인·거절 후속 처리와 재신청은 [P7 Access](../p7/p7-access.md)에서 정의한다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `SELLER-005` | 회사명·연락처 검증 실패 | 신청 정보를 확인해 주세요. | 실패 필드와 수정 방법 | 내부 검증 원인과 requestId |
| 401 | [AUTH-001](../index.md#예외-응답) | — | — | — | — |
| 403 | `SELLER-006` | ADMIN 신청 또는 판매자 신청 권한 없음 | 신청할 수 없습니다. | 없음 | User 역할·신청 상태와 requestId |
| 409 | `SELLER-007` | PENDING 신청 또는 ACTIVE Seller가 이미 존재함 | 이미 처리 중인 판매자 신청이 있습니다. | `userId`를 반환하지 않음 | 충돌 User 식별자와 현재 상태 |
