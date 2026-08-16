# [P?] [Resource] API

<!--
이 파일을 실제 리소스명으로 변경한다.
예: resource.md -> review.md, order.md, offer.md
이 문서는 하나의 핵심 리소스에 대한 데이터 모델과 API를 담당한다.
-->

이 문서는 `[Resource]` 데이터 모델과 해당 리소스를 조작·조회하는 API를 정의한다. 업무 정책은 [P? Policy](p?-policy.md), 공통 응답·예외 형식은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `[Resource]` | [핵심 리소스의 데이터와 상태] | 조회·생성·수정·[기타] |
| `[ResourceChild]` | [하위 리소스 또는 첨부] | [관련 API] |

- 이 문서가 소유하는 모델과 외부 도메인 모델을 구분한다.
- 외부 도메인의 모델은 식별자·공개 계약만 참조하고 필드를 복제하지 않는다.
- 정책에 정의된 불변식이 어느 모델과 API에서 검증되는지 연결한다.

## 2. 데이터 모델

### 2-1. [Resource]

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `[resourceId]` | UUID | 예 | 리소스 식별자 |
| `[field]` | [타입] | [예/아니오] | [업무 의미와 제약] |

### 2-2. 관계와 제약

- [유일성·참조·상태 제약]
- [하위 모델 최대 개수·정렬·삭제 규칙]
- [트랜잭션과 멱등성 규칙]

## 3. API 정의

각 API는 URI, 권한, 요청 JSON, 성공 상태·응답 JSON, 가능한 모든 예외를 작성한다. 성공 응답은 도메인별 Response DTO를 직접 반환하며 공통 성공 봉투를 사용하지 않는다.

### 3-1. [리소스] 조회

`GET /api/v1/[resources]/{resourceId}`

권한: [공개 또는 역할]

#### 성공 응답: `200 OK`

```json
{
  "resourceId": "uuid",
  "field": "value"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 404 | `[DOMAIN]-001` | 리소스가 존재하지 않음 | [추상화된 안내 문구] | 없음 | [내부 원인과 요청 식별자] |

### 3-2. [리소스] 생성

`POST /api/v1/[resources]`

권한: [역할]

요청:

```json
{
  "field": "value"
}
```

#### 성공 응답: `201 Created`

```json
{
  "resourceId": "uuid",
  "field": "value"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `[DOMAIN]-002` | 요청 필드 검증 실패 | [추상화된 안내 문구] | 실패 필드와 수정 방법 | 실제 입력값과 내부 검증 원인 |
| 409 | `[DOMAIN]-003` | 중복 또는 현재 상태와 충돌 | [추상화된 안내 문구] | [필요 시] | 내부 충돌 원인과 식별자 |

### 3-3. [리소스] 수정

`PATCH /api/v1/[resources]/{resourceId}`

권한: [역할]

요청:

```json
{
  "field": "updated-value"
}
```

#### 성공 응답: `200 OK`

```json
{
  "resourceId": "uuid",
  "field": "updated-value"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 403 | `[DOMAIN]-004` | 요청자가 리소스 권한을 갖지 않음 | [추상화된 안내 문구] | 없음 | 권한 검증 원인과 식별자 |
| 404 | `[DOMAIN]-001` | 리소스가 존재하지 않음 | [추상화된 안내 문구] | 없음 | 리소스 조회 원인과 식별자 |
