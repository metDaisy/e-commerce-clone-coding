# P12 Media API

이 문서는 P12의 공통 Media 모델과 업로드·완료·취소 API를 정의한다. 기본 업로드 경로는 Browser → Nginx → SeaweedFS S3이며, Spring Boot의 공개 업로드 API는 파일 바이트를 받지 않고 세션·presigned URL·완료 검증·첨부를 처리한다. 파일 정책은 [P12 Media Policy](p12-policy.md), 공통 응답과 HTTP 상태는 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

P2·P9·P10의 첨부 API는 이 문서에서 `READY`가 된 `uploadId`를 사용한다. 첨부 대상의 존재·소유권·업무 자격·공개 여부는 각 도메인의 API가 검증한다.

## 1. 데이터 모델과 API 관계

| 데이터 모델 | 책임 | 관련 API |
|---|---|---|
| `MediaUpload` | 파일 업로드 세션과 검증 상태 | 생성·완료·취소 |
| `MediaStoragePort` | object storage·업로드 URL·공개 URL·물리 삭제 추상화 | `prepareUpload`, `completeUpload`, `publicUrl`, `delete` |
| `MediaAttachment` 계약 | 완료된 Media와 업무 리소스의 연결 메타데이터 | P2·P9·P10의 연결·수정·보관 API |

- P12는 `MediaUpload`와 저장소 포트 계약을 소유한다.
- 실제 `CatalogProductMedia`, `OfferMedia`, `ReviewMediaAttachment` 모델과 업무 규칙은 각각 P2·P9·P10이 소유한다.
- `storageKey`는 저장소 내부 값이며 API 응답·이벤트·도메인 응답에 포함하지 않는다.

## 2. 데이터 모델

### 2-1. `MediaUpload`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `uploadId` | UUID | 예 | 업로드 세션 식별자 |
| `requesterUserId` | UUID | 예 | 세션을 만든 인증 사용자. 논리 참조 |
| `type` | ENUM | 예 | 현재 `IMAGE`만 허용 |
| `originalFileName` | VARCHAR(255) | 예 | 표시·감사용 이름. 경로로 사용하지 않음 |
| `contentType` | ENUM | 예 | `image/jpeg`, `image/png`, `image/webp` |
| `sizeBytes` | BIGINT | 예 | 1바이트 이상, 최대 10 MiB |
| `checksum` | SHA-256 hex | 예 | 완료 시 실제 객체와 일치해야 함 |
| `storageKey` | 불투명 문자열 | 예 | 클라이언트와 API 응답에 노출하지 않음 |
| `status` | ENUM | 예 | `INITIATED`, `READY`, `ATTACHED`, `EXPIRED`, `CANCELLED`, `FAILED` |
| `expiresAt` | TIMESTAMP | 예 | 생성 후 30분 |
| `createdAt`, `updatedAt` | TIMESTAMP | 예 | 생성·최종 변경 시각 |
| `attachedAt` | TIMESTAMP | 아니오 | `ATTACHED` 전환 시각 |

- 서버는 확장자가 아니라 실제 MIME signature와 디코딩 결과로 이미지 형식을 검증한다.
- 이미지의 가로·세로는 각각 1 이상 10,000 이하이고, 디코딩 가능한 정적 이미지여야 한다. 애니메이션 GIF·SVG·실행 파일은 거부한다.
- 공개 전 EXIF 등 원본 메타데이터를 제거하며 GPS 위치와 카메라 정보를 공개 파일에 포함하지 않는다.
- 실제 객체와 `checksum`이 다르면 `FAILED`로 전환하고 첨부를 허용하지 않는다. `FAILED` 상태 자체는 외부 상태값으로 반환하지 않고 `MEDIA-003`으로 응답한다.
- 하나의 `MediaUpload`는 하나의 업무 attachment에만 연결할 수 있다.

### 2-2. `MediaStoragePort`

`MediaStoragePort`는 P2·P9·P10에 공개하는 최소 저장소 인터페이스다. 도메인 모듈은 storage SDK, presigned URL 방식, storage key 형식을 알지 못한다. Backend의 저장소 adapter는 SeaweedFS S3에 업로드 대상을 만들고, 브라우저에는 Nginx의 same-origin 업로드 URL만 반환한다.

| 연산 | 책임 | 결과 |
|---|---|---|
| `prepareUpload(spec)` | 허용 형식·크기·checksum 조건의 일회성 업로드 대상 생성 | Nginx 업로드 URL, 만료 시각, 불투명 `storageKey` |
| `completeUpload(storageKey, expected)` | 객체 존재·MIME·크기·checksum·이미지 디코딩 검증과 공개 객체 준비 | 검증된 객체 또는 실패 |
| `publicUrl(storageKey)` | 저장 객체의 Nginx 공개 URL 생성 | 서버가 생성한 URL |
| `delete(storageKey)` | 만료·취소·보관 객체의 물리 삭제 | 성공 또는 저장소 오류 |

도메인 모듈은 포트 호출 결과만 사용하고 `storageKey`를 API·이벤트·도메인 응답으로 전달하지 않는다. Nginx는 presigned URL의 query string과 서명 대상 path·Host가 바뀌지 않도록 전달해야 하며, 업로드 경로를 캐시하지 않는다.

### 2-3. `MediaAttachment` 공통 계약

P12는 아래 연결 계약을 제공하지만, 실제 소유 모델은 P2·P9·P10의 로컬 모델이다.

| 필드 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `mediaId` | UUID | 예 | 첨부 관계 식별자 |
| `uploadId` | UUID | 예 | P12에서 `READY`가 된 업로드 식별자 |
| `ownerType` | ENUM | 예 | `CATALOG_PRODUCT`, `OFFER`, `REVIEW` |
| `ownerId` | UUID | 예 | 업무 리소스의 논리 식별자 |
| `type` | ENUM | 예 | 업로드의 `type`을 복사하며 수정 불가 |
| `url` | URI | 예 | 서버가 생성한 Nginx 공개 접근 URL |
| `sortOrder` | INTEGER | 예 | 소유 리소스 안에서 0부터 시작하는 유일한 순서 |
| `isPrimary` | BOOLEAN | 조건부 | P2·P9의 대표 이미지 여부. Review에서는 사용하지 않음 |
| `status` | ENUM | 조건부 | P2·P9의 `ACTIVE`, `ARCHIVED`. P10은 Review 공개 상태로 관리 |
| `createdAt`, `updatedAt` | TIMESTAMP | 도메인별 | 연결·최종 변경 시각. P10 첨부는 `updatedAt`을 두지 않음 |
| `archivedAt` | TIMESTAMP | 아니오 | `ARCHIVED` 전환 시각 |

P2는 CatalogProduct당 최대 20개, P9는 Offer당 최대 10개, P10은 Review당 최대 5개의 `ACTIVE` Media를 허용한다. 같은 소유 리소스 안에서 `sortOrder`는 유일해야 하며, P2·P9의 대표 Media는 최대 하나다.

## 3. API 정의

### 3-1. 업로드 세션 생성

`POST /api/v1/media/uploads`

권한: 인증된 사용자

이 API는 Media를 업무 리소스에 첨부하지 않는다. 첨부 시점에 P2·P9·P10이 소유권과 업무 자격을 다시 검증한다.

요청:

```json
{
  "type": "IMAGE",
  "originalFileName": "product-main.webp",
  "contentType": "image/webp",
  "sizeBytes": 512000,
  "checksum": "sha256-hex"
}
```

성공 응답: `201 Created`

```json
{
  "uploadId": "uuid",
  "uploadMethod": "PUT",
  "uploadUrl": "https://api.example.com/media-upload/uuid-upload-token",
  "expiresAt": "2026-08-16T12:30:00Z",
  "status": "INITIATED"
}
```

- `uploadUrl`은 Nginx의 same-origin 경로이며 30분 후 만료되는 일회성 URL이다. 브라우저는 SeaweedFS 주소를 알지 못한다.
- Nginx는 `PUT` 요청을 SeaweedFS S3로 전달하고 업로드 응답을 브라우저에 전달한다. 이 응답은 파일 수신 완료일 뿐 `READY`를 의미하지 않는다.
- URL과 presigned URL 토큰은 로그·분석 이벤트·응답 외 저장소에 기록하지 않는다.
- 반환된 `Content-Type`, `Content-Length`, checksum 조건을 바꾸면 저장소 업로드를 거부한다.
- 외부 URL, `data:` URL, HTML·스크립트 본문을 전달해 Media를 등록하는 방식은 지원하지 않는다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `MEDIA-001` | 지원하지 않는 type 또는 MIME | 지원하지 않는 이미지 형식입니다. | 허용 형식 | 입력 검증 원인 |
| 400 | `MEDIA-002` | 10 MiB 또는 이미지 차원 제한 초과 | 이미지 크기를 확인해 주세요. | 실패한 필드 | 입력값과 제한 |
| 400 | `MEDIA-003` | checksum·파일 형식 조건이 잘못됨 | 이미지 정보를 확인해 주세요. | 실패한 필드 | 검증 원인 |
| 401 | `MEDIA-004` | 인증 정보 없음·위조·만료 | 로그인이 필요합니다. | 없음 | 인증 실패 원인 |
| 503 | `MEDIA-009` | 업로드 URL 생성 불가 | 파일 업로드를 잠시 후 다시 시도해 주세요. | 없음 | 저장소 오류와 requestId |

### 3-2. 업로드 완료 검증

`POST /api/v1/media/uploads/{uploadId}/complete`

권한: 해당 업로드 세션을 만든 사용자

브라우저는 파일을 다시 보내지 않고 `uploadId`만 전달한다. 이 요청은 Nginx를 통해 Backend로 전달되는 제어 요청이다.

요청 본문: 없음

- Backend는 저장소 adapter를 통해 SeaweedFS S3의 객체 존재·메타데이터·checksum을 확인한다. 실제 MIME·크기·이미지 디코딩·EXIF 제거가 필요하면 adapter 또는 별도 내부 검증기가 객체를 검증한다. 파일 바이트는 업로드 경로에서 SeaweedFS로 직접 전달되며 Backend의 공개 HTTP 요청 본문으로 들어오지 않는다.
- 성공하면 `status = READY`로 변경하고 공개 URL을 내부적으로 준비한다. 아직 업무 리소스에는 첨부하지 않는다.
- `READY` 업로드만 P2·P9·P10의 첨부 API에서 사용할 수 있다.

성공 응답: `200 OK`

```json
{
  "uploadId": "uuid",
  "status": "READY",
  "type": "IMAGE",
  "expiresAt": "2026-08-16T12:30:00Z"
}
```

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 400 | `MEDIA-003` | MIME·checksum·디코딩 검증 실패 | 유효하지 않은 이미지 파일입니다. | 실패한 검증 항목 | 실제 검증 원인 |
| 403 | `MEDIA-005` | 다른 사용자의 업로드 세션 | 업로드를 확인할 수 없습니다. | 없음 | 요청자와 소유자 검증 결과 |
| 404 | `MEDIA-006` | 업로드 세션 없음 | 업로드를 찾을 수 없습니다. | 없음 | `uploadId`와 조회 결과 |
| 409 | `MEDIA-007` | 만료된 업로드 세션 | 만료된 업로드입니다. | `expiresAt` | 만료 원인 |
| 503 | `MEDIA-009` | 저장 객체 조회·검증 불가 | 파일을 확인할 수 없습니다. | 없음 | 저장소 오류와 requestId |

### 3-3. 업로드 취소

`DELETE /api/v1/media/uploads/{uploadId}`

권한: 해당 업로드 세션을 만든 사용자

- `INITIATED` 또는 `READY` 세션만 취소할 수 있다.
- 성공하면 `204 No Content`를 반환하고 상태를 `CANCELLED`로 바꾸며 저장 객체 삭제를 요청한다.
- `ATTACHED` 업로드는 취소하지 않는다. 물리 삭제 실패가 업무 도메인 트랜잭션을 롤백하지는 않는다.

#### 예외

| HTTP | exceptionCode | 발생 조건 | client message | details | system message |
|---:|---|---|---|---|---|
| 403 | `MEDIA-005` | 다른 사용자의 업로드 세션 | 업로드를 확인할 수 없습니다. | 없음 | 요청자와 소유자 검증 결과 |
| 404 | `MEDIA-006` | 업로드 세션 없음 | 업로드를 찾을 수 없습니다. | 없음 | `uploadId`와 조회 결과 |
| 409 | `MEDIA-007` | 만료된 업로드 세션 | 만료된 업로드입니다. | `expiresAt` | 만료 원인 |
| 409 | `MEDIA-008` | 이미 `ATTACHED`인 업로드 | 이미 사용된 업로드입니다. | 없음 | 중복 첨부 원인 |
| 503 | `MEDIA-009` | 저장 객체 삭제 요청 불가 | 업로드 취소를 잠시 후 다시 시도해 주세요. | 없음 | 저장소 오류와 requestId |

### 3-4. 업무 도메인 첨부 계약

업무 도메인은 자신의 API에서 `uploadId`를 받아 대상 검증, 첨부 모델 생성, `READY → ATTACHED` 전환을 하나의 로컬 DB 트랜잭션으로 처리한다.

P2·P9의 기본 요청 형식:

```json
{
  "uploadId": "uuid",
  "sortOrder": 0,
  "isPrimary": true
}
```

P10 Review 작성·수정은 다음처럼 업로드 세션 ID 배열을 사용한다.

```json
{
  "rating": 5,
  "content": "좋은 상품입니다.",
  "mediaUploadIds": ["uuid"]
}
```

P2·P9·P10은 임의의 `url`, `imageUrls`, 외부 URL을 첨부 입력으로 받지 않는다. 첨부 대상의 자격·최대 개수·정렬·공개·보관 예외는 각각 [P2 CatalogProduct](../p2/p2-catalog-product.md), [P9 Offer](../p9/p9-offer.md), [P10 Review](../p10/p10-review.md)가 정의한다. webhook이나 메시지 브로커를 사용하지 않는 기본 프로파일에서는 브라우저의 `/complete` 요청이 검증 시작점이다.
