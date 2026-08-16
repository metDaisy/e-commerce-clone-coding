# P7 Access API

이 문서는 관리자용 역할·Seller 신청·Seller 상태 API를 정의한다. 업무 정책은 [P7 Policy](p7-policy.md), 공통 관리자 권한과 예외는 [P7 Admin API](p7-admin.md)를 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| User role set | P1 User가 사용자의 역할 집합과 변경 이벤트를 소유한다. | 역할 추가·삭제 |
| `SellerApplication` | P8 Seller가 판매자 신청 원본과 `PENDING`·`APPROVED`·`REJECTED` 상태를 소유한다. | 판매자 신청 승인·거절 |
| `SellerApplicationReview` | P8 Seller가 관리자의 승인·거절 이력을 소유한다. | 판매자 신청 승인·거절 |
| `Seller` | P8 Seller가 승인된 판매자 프로필과 `ACTIVE`·`SUSPENDED` 상태를 소유한다. | 판매자 정지·재활성화 |
| `SellerStatusHistory` | P8 Seller가 Seller 상태 변경 이력을 소유한다. | 판매자 정지·재활성화 |
| `UserRolesChangedEvent` | P1 User가 역할 변경 사실을 발행하고 P11 Auth가 세션을 무효화한다. | 역할 추가·삭제의 후속 처리 |

- P7은 위 모델의 원본을 소유하지 않고 공개 application interface를 호출한다.
- Seller 신청 승인 시 P8이 Seller를 생성하고 P1이 `PRODUCT_MANAGER` 역할을 추가한다.
- 실제 역할 집합 변경이 성공하면 역할 변경 이벤트를 기록하고 대상 User의 모든 로그인 세션을 무효화한다.

## 2. 데이터 모델

### 2-1. User role set

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `userId` | UUID | 예 | 역할 변경 대상 User |
| `roles` | String | 예 | 쉼표로 구분한 가산형 역할 집합. 예: `USER,PRODUCT_MANAGER` |
| `updatedAt` | TIMESTAMP | 예 | 역할 집합 변경 시각 |

역할 원본 필드와 `UserRolesChangedEvent` payload는 [P1 User](../p1/p1-user.md)를 따른다.

### 2-2. 관리자 심사·상태 이력 참조

| 모델 | 주요 필드 | 소유 도메인 |
|---|---|---|
| `SellerApplication` | `applicationId`, `userId`, `businessName`, `contactEmail`, `contactPhone`, `status`, `sellerId` | P8 |
| `SellerApplicationReview` | `applicationId`, `reviewedByUserId`, `decision`, `reasonCode`, `message`, `reviewedAt` | P8 |
| `SellerStatusHistory` | `sellerId`, `fromStatus`, `toStatus`, `changedByUserId`, `reasonCode`, `message`, `changedAt` | P8 |

### 2-3. 관계와 제약

- `SellerApplication`은 User당 `PENDING` 하나만 허용한다.
- 승인된 신청만 Seller와 `PRODUCT_MANAGER` 역할을 생성한다.
- 처리된 신청은 다시 처리하지 않으며, 재신청은 새로운 `SellerApplication`으로 저장한다.
- Seller 정지 시 Seller의 변경 API와 Offer 변경을 차단하고, Seller 재활성화가 Offer를 자동 활성화하지 않는다.
- 역할 집합이 실제로 변경될 때만 이벤트와 세션 무효화를 수행한다.
- 세션 무효화가 최초 처리에서 실패하면 역할 변경을 성공으로 반환하지 않는다.

## 3. API 정의

### 3-1. 사용자 역할 추가

`POST /api/v1/admin/users/{userId}/roles`

권한: `ADMIN`

요청:

```json
{
  "role": "PRODUCT_MANAGER"
}
```

`role`은 `PRODUCT_MANAGER` 또는 `ADMIN`만 허용한다. `PRODUCT_MANAGER`는 `ACTIVE` Seller를 가진 User에게만 추가한다.

#### 성공 응답: `200 OK`

```json
{
  "userId": "uuid",
  "roles": "USER,PRODUCT_MANAGER",
  "updatedAt": "2026-08-16T12:00:00Z"
}
```

#### 예외

공통 예외 `AUTH-001`, `ADMIN-001`, `SYSTEM-001`은 [P7 Admin API](p7-admin.md#4-공통-예외)를 따른다.

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 404 | [USER-001](../p1/p1-user.md#3-1-내-프로필-조회) | — | — | — | — |
| 409 | `ADMIN-005` | 이미 역할을 보유함 | 이미 부여된 역할입니다. | role | 대상 User와 역할 |
| 409 | `ADMIN-006` | `PRODUCT_MANAGER` 대상 Seller가 활성 아님 | 판매자 상태를 확인해 주세요. | 없음 | Seller 상태와 User |
| 400 | [ADMIN-003](p7-admin.md#4-공통-예외) | — | — | — | — |

### 3-2. 사용자 역할 삭제

`DELETE /api/v1/admin/users/{userId}/roles/{role}`

권한: `ADMIN`

성공 응답: `204 No Content`

역할 삭제 후 P1은 `UserRolesChangedEvent`를 기록하고 P11 Auth는 대상 User의 Access Token·Refresh Token을 포함한 모든 세션을 무효화한다. 대상 User는 다시 로그인해야 한다.

#### 예외

공통 예외 `AUTH-001`, `ADMIN-001`, `ADMIN-002`, `SYSTEM-001`은 [P7 Admin API](p7-admin.md#4-공통-예외)를 따른다.

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 404 | [USER-001](../p1/p1-user.md#3-1-내-프로필-조회) | — | — | — | — |
| 409 | `ADMIN-007` | 보유하지 않은 역할 삭제 | 현재 부여되지 않은 역할입니다. | role | 대상 User와 역할 |
| 409 | `ADMIN-008` | 기본 `USER` 역할 삭제 | 기본 역할은 삭제할 수 없습니다. | 없음 | 대상 User와 역할 |
| 403 | [ADMIN-002](p7-admin.md#4-공통-예외) | — | — | — | — |

### 3-3. Seller 신청 승인·거절

`PATCH /api/v1/admin/seller-applications/{applicationId}/status`

권한: `ADMIN`

승인 요청:

```json
{
  "status": "APPROVED"
}
```

거절 요청:

```json
{
  "status": "REJECTED",
  "reasonCode": "INFORMATION_INSUFFICIENT",
  "message": "판매자 신청을 승인하기에 필요한 정보가 충분하지 않습니다."
}
```

승인에는 사유를 요구하지 않는다. 거절에는 `reasonCode`와 판매자에게 공개할 `message`를 요구한다. 아래 `reasonCode`는 심사 업무 사유이며 API 예외 코드가 아니다.

| `reasonCode` | 설명 |
|---|---|
| `INFORMATION_INSUFFICIENT` | 신청 정보가 승인 판단에 충분하지 않음 |
| `ELIGIBILITY_NOT_MET` | 플랫폼의 판매자 등록 요건을 충족하지 않음 |

#### 성공 응답: `200 OK`

```json
{
  "applicationId": "uuid",
  "status": "APPROVED",
  "sellerId": "uuid",
  "processedByUserId": "uuid",
  "processedAt": "2026-08-16T12:00:00Z"
}
```

승인 시 P8은 Seller를 `ACTIVE`로 생성하고, P1은 기존 `USER` 역할을 유지한 채 `PRODUCT_MANAGER`를 추가한다. 거절 시 Seller를 생성하지 않는다.

#### 예외

공통 예외 `AUTH-001`, `ADMIN-001`, `SYSTEM-001`은 [P7 Admin API](p7-admin.md#4-공통-예외)를 따른다.

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 404 | `ADMIN-009` | 신청이 존재하지 않음 | 판매자 신청을 찾을 수 없습니다. | 없음 | 신청 식별자 |
| 409 | `ADMIN-010` | `PENDING`이 아닌 신청 처리 | 이미 처리된 신청입니다. | status | 신청 상태와 식별자 |
| 400 | `ADMIN-011` | 거절 사유 또는 메시지 누락 | 거절 사유를 입력해 주세요. | 실패 필드 | 내부 검증 원인 |
| 409 | `ADMIN-012` | 승인 결과 Seller 중복 | 판매자 등록 상태를 확인해 주세요. | 없음 | User와 Seller 식별자 |

### 3-4. Seller 정지·재활성화

`PATCH /api/v1/admin/sellers/{sellerId}/status`

권한: `ADMIN`

요청:

```json
{
  "status": "SUSPENDED",
  "reasonCode": "POLICY_VIOLATION",
  "message": "판매 정책 위반으로 판매자 기능이 정지되었습니다."
}
```

`ACTIVE ↔ SUSPENDED` 전환을 허용한다. 상태 변경 시 P8이 `SellerStatusHistory`에 이전·이후 상태, 관리자, 사유, 시각을 기록한다. `SUSPENDED` 전환은 Seller의 Offer를 `INACTIVE`로 만들지만, 재활성화 시 Offer를 자동 복구하지 않는다.

#### 성공 응답: `200 OK`

```json
{
  "sellerId": "uuid",
  "status": "SUSPENDED",
  "updatedAt": "2026-08-16T12:00:00Z"
}
```

#### 예외

공통 예외 `AUTH-001`, `ADMIN-001`, `SYSTEM-001`은 [P7 Admin API](p7-admin.md#4-공통-예외)를 따른다.

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 404 | `ADMIN-013` | Seller가 존재하지 않음 | 판매자를 찾을 수 없습니다. | 없음 | Seller 식별자 |
| 409 | `ADMIN-014` | 현재 상태에서 전환할 수 없음 | 현재 판매자 상태에서는 처리할 수 없습니다. | status | Seller 상태와 요청 상태 |
| 400 | `ADMIN-015` | 상태·사유 필드 검증 실패 | 판매자 상태 변경 요청을 확인해 주세요. | 실패 필드 | 입력값과 검증 원인 |
