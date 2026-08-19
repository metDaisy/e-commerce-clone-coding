# P1 Address API

이 문서는 User가 소유하는 `Address` 데이터 모델과 주소를 조작·조회하는 API를 정의한다. 업무 정책은 [P1 User Policy](p1-policy.md), User 프로필 API는 [User API](p1-user.md), 공통 응답·예외 형식은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

Address는 User에 종속되지만 별도의 생성·수정·삭제 생명주기와 기본 배송지 불변식을 가지는 독립 리소스다. User API 응답에 주소를 중첩하지 않고 이 문서의 API로 접근한다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `Address` | User가 주문에 사용할 배송지 원본과 기본 배송지 여부 | 목록·등록·수정·삭제·기본 지정 |
| `User` | Address의 소유자와 접근 권한 기준 | [User API](p1-user.md) |
| 주문 배송지 스냅샷 | 결제 시점의 불변 배송 정보 | [P5 Order](../p5/p5-order.md) |

- P1은 Address 원본·소유권·기본 배송지 상태를 소유한다.
- P5는 Address의 현재 값을 확인한 뒤 결제 시점에 주문용 스냅샷을 만든다. 스냅샷은 Address API의 수정·삭제로 변경되지 않는다.
- 다른 User의 Address는 식별자만 알아도 조회·변경할 수 없으며, 모든 주소 API는 인증 주체의 User 소유권을 검증한다.

## 2. 데이터 모델

### 2-1. `Address`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `addressId` | UUID | 예 | 주소 식별자 |
| `userId` | UUID | 예 | 소유 User 식별자 |
| `alias` | String | 예 | 사용자가 지정하는 주소 별칭. 예: 집, 회사 |
| `recipientName` | String | 예 | 수령인 이름 |
| `recipientPhone` | String | 예 | 수령인 연락처 |
| `postalCode` | String | 예 | 우편번호 |
| `addressLine` | String | 예 | 기본 주소 본문 |
| `isPrimary` | Boolean | 예 | 기본 배송지 여부 |
| `lastUsedAt` | Instant | 아니오 | 주소를 최근 배송지로 사용한 시각. ISO-8601 UTC |
| `createdAt` | Instant | 예 | 주소 생성 시각. ISO-8601 UTC |
| `updatedAt` | Instant | 예 | 주소 마지막 변경 시각. ISO-8601 UTC |

### 2-2. 관계와 제약

- 하나의 User는 제한 없이 Address를 가진다. Address는 정확히 하나의 User에 속한다.
- 동일 User는 동일한 주소를 중복 등록할 수 없다. 주소 동일성은 Front에서 앞뒤 공백을 제거한 `postalCode`와 `addressLine` 값 조합으로 판단하며, 수령인·연락처·별칭은 판단에 포함하지 않는다.
- 한 User의 `isPrimary=true` Address는 0~1개다.
- 기본 배송지 지정은 기존 기본값 해제와 신규 기본값 지정을 하나의 트랜잭션으로 수행한다.
- 기본 배송지 삭제 시 `lastUsedAt`이 가장 최근인 Address를 기본값으로 승격한다. `lastUsedAt`이 없는 주소끼리는 `createdAt`, `addressId` 내림차순으로 선택한다. Address가 없으면 기본값도 없다.
- 첫 번째 Address를 자동으로 기본 배송지로 만들지 않는다. 등록 요청에서 `isPrimary=true`를 명시하거나 기본 지정 API를 호출해야 한다.
- 주소 목록은 공통 페이지 기반 응답을 사용한다. 기본 `page=0`, `size=20`, 최대 `size=100`이며 정렬은 `isPrimary DESC`, `lastUsedAt DESC NULLS LAST`, `createdAt DESC`, `addressId DESC`다.

### 2-3. 예외 코드

| exceptionCode | 의미 | HTTP |
|---|---|---:|
| `ADDRESS-004` | 요청한 Address를 찾을 수 없음 | 404 |
| `ADDRESS-005` | User가 동일한 주소를 이미 보유함 | 400 |
| `ADDRESS-008` | 다른 User의 Address에 접근함 | 403 |

공통 입력 검증 예외인 `INVALID_INPUT`은 [공통 API 계약](../index.md#예외-응답)을 참조한다. 주소 전용 예외 코드는 `ADDRESS-` 접두사를 사용한다.

## 3. API 정의

모든 API는 로그인한 User를 `principal`로 식별한다. `addressId`가 존재하더라도 소유 User가 다르면 접근을 허용하지 않는다.

User가 비활성화 상태면 주소를 조회하거나 변경할 수 없으며 [P1 User의 `USER-004`](p1-user.md#user-disabled-error)를 반환한다.

### 3-1. 내 주소 목록 조회

`GET /api/v1/me/addresses?page=0&size=20`

권한: 로그인 사용자

`page`는 0부터 시작하고 `size`는 기본 20, 최대 100이다. 응답은 공통 페이지 형식의 `data`, `page`, `size`, `totalElements`, `totalPages`를 사용한다.

정렬: `isPrimary DESC`, `lastUsedAt DESC NULLS LAST`, `createdAt DESC`, `addressId DESC`

#### 성공 응답: `200 OK`

```json
{
  "data": [
    {
    "id": "22222222-2222-2222-2222-222222222222",
    "userId": "11111111-1111-1111-1111-111111111111",
    "alias": "집",
    "recipientName": "홍길동",
    "recipientPhone": "01012345678",
    "postalCode": "06236",
    "addressLine": "서울특별시 강남구 테헤란로 1",
    "isPrimary": true,
    "lastUsedAt": "2026-08-17T12:00:00Z",
    "createdAt": "2026-08-16T12:00:00Z",
    "updatedAt": "2026-08-16T12:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 401 | [AUTH-001](../index.md#공통-인증-권한) | — | — | — | — |
| 403 | [`USER-004`](p1-user.md#user-disabled-error) | — | — | — | — |

### 3-2. 주소 등록

`POST /api/v1/me/addresses`

권한: 로그인 사용자

요청:

```json
{
  "alias": "집",
  "recipientName": "홍길동",
  "recipientPhone": "01012345678",
  "postalCode": "06236",
  "addressLine": "서울특별시 강남구 테헤란로 1",
  "isPrimary": true
}
```

`isPrimary`를 생략하면 `false`다. `true`이면 기존 기본 배송지를 해제하고 새 주소를 기본 배송지로 지정한다.

#### 성공 응답: `201 Created`

등록된 `Address` 전체를 반환한다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | [`INVALID_INPUT`](../index.md#예외-응답) | 필수 필드 누락·형식 오류 | 잘못된 입력값입니다. | 실패 필드와 수정 방법 | 검증 필드와 내부 원인 |
| 400 | `ADDRESS-005` | User가 동일한 주소를 이미 보유 | 이미 등록된 주소입니다. | `postalCode`, `addressLine` | User 식별자와 중복 주소 식별자 |
| 401 | [AUTH-001](../index.md#공통-인증-권한) | — | — | — | — |
| 403 | [`USER-004`](p1-user.md#user-disabled-error) | — | — | — | — |

### 3-3. 주소 수정

`PATCH /api/v1/me/addresses/{addressId}`

권한: 로그인 사용자이며 해당 Address의 소유자

요청:

```json
{
  "alias": "회사",
  "recipientName": "김길동",
  "recipientPhone": "01098765432",
  "postalCode": "06237",
  "addressLine": "서울특별시 강남구 테헤란로 2"
}
```

보낸 필드만 수정하며 `addressId`, `userId`, `createdAt`은 수정할 수 없다. `alias`는 수정할 수 있으며, `postalCode`와 `addressLine`을 함께 수정해 다른 Address와 동일해지는 경우 `ADDRESS-005`를 반환한다. 기본 배송지 여부는 [기본 배송지 지정](#3-5-기본-배송지-지정) API로 변경한다.

#### 성공 응답: `200 OK`

수정된 `Address` 전체를 반환한다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | [`INVALID_INPUT`](../index.md#예외-응답) | 수정 필드의 형식·제약조건 검증 실패 | 잘못된 입력값입니다. | 실패 필드와 수정 방법 | 검증 필드와 내부 원인 |
| 400 | `ADDRESS-005` | 수정 후 동일한 주소가 이미 존재 | 이미 등록된 주소입니다. | `postalCode`, `addressLine` | User 식별자와 중복 주소 식별자 |
| 401 | [AUTH-001](../index.md#공통-인증-권한) | — | — | — | — |
| 403 | `ADDRESS-008` | 다른 User의 Address에 접근 | 주소를 변경할 권한이 없습니다. | 없음 | 요청 User와 소유 User 식별자 |
| 403 | [`USER-004`](p1-user.md#user-disabled-error) | — | — | — | — |
| 404 | `ADDRESS-004` | Address가 존재하지 않음 | 주소를 찾을 수 없습니다. | `addressId` | 조회 조건과 requestId |

### 3-4. 주소 삭제

`DELETE /api/v1/me/addresses/{addressId}`

권한: 로그인 사용자이며 해당 Address의 소유자

#### 성공 응답: `204 No Content`

기본 배송지를 삭제하면 `lastUsedAt`이 가장 최근인 Address를 기본 배송지로 승격한다. `lastUsedAt`이 없는 주소끼리는 `createdAt`, `addressId` 내림차순으로 선택한다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 401 | [AUTH-001](../index.md#공통-인증-권한) | — | — | — | — |
| 403 | `ADDRESS-008` | 다른 User의 Address에 접근 | 주소를 삭제할 권한이 없습니다. | 없음 | 요청 User와 소유 User 식별자 |
| 403 | [`USER-004`](p1-user.md#user-disabled-error) | — | — | — | — |
| 404 | `ADDRESS-004` | Address가 존재하지 않음 | 주소를 찾을 수 없습니다. | `addressId` | 조회 조건과 requestId |

### 3-5. 기본 배송지 지정

`POST /api/v1/me/addresses/{addressId}/default`

권한: 로그인 사용자이며 해당 Address의 소유자

요청 본문: 없음

#### 성공 응답: `200 OK`

지정된 `Address` 전체를 반환한다. 기존 기본 배송지는 `isPrimary=false`가 된다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 401 | [AUTH-001](../index.md#공통-인증-권한) | — | — | — | — |
| 403 | `ADDRESS-008` | 다른 User의 Address에 접근 | 기본 배송지를 지정할 권한이 없습니다. | 없음 | 요청 User와 소유 User 식별자 |
| 403 | [`USER-004`](p1-user.md#user-disabled-error) | — | — | — | — |
| 404 | `ADDRESS-004` | Address가 존재하지 않음 | 주소를 찾을 수 없습니다. | `addressId` | 조회 조건과 requestId |
