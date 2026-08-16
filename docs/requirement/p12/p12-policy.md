# P12 Media Policy

이 문서는 P12의 범위·책임과 API에 독립적인 Media 업무 정책을 정의한다. 데이터 모델과 HTTP 계약은 [P12 Media API](p12-media.md)를 따른다.

## 1. 범위와 책임

### 범위

- 인증 사용자의 이미지 업로드 세션과 저장소 계약
- 파일 형식·크기·차원·checksum·디코딩·메타데이터 검증
- `MediaUpload`의 준비·완료·취소·만료 생명주기
- P2·P9·P10이 사용하는 `READY` 업로드와 첨부 연결 계약
- 공개 URL, 캐시, 보관 파일과 고아 파일의 정리 규칙

### 책임

| 책임 | 담당 | 참조 |
|---|---|---|
| 업로드 세션·파일 검증·`READY` 상태 | P12 | [P12 Media API](p12-media.md) |
| 저장소 포트와 adapter 경계 | `common`·infra adapter | [`MediaStoragePort`](p12-media.md#2-2-mediastorageport) |
| CatalogProduct Media 대상·대표·정렬·보관 | P2 | [P2 CatalogProduct](../p2/p2-catalog-product.md) |
| Offer Media 대상·대표·정렬·보관 | P9 | [P9 Offer](../p9/p9-offer.md) |
| Review 첨부 자격·최대 개수·공개 | P10 | [P10 Review](../p10/p10-review.md) |
| Outbox 전달·업무 Saga | P6 | [P6 Infrastructure](../p6/p6-infrastructure.md) |

P12는 다른 도메인의 업무 애그리거트·Repository·권한 모델을 소유하거나 직접 참조하지 않는다. P2·P9·P10은 P12의 공개 계약만 사용한다.

## 2. 용어와 행위자

| 용어·행위자 | 의미와 책임 |
|---|---|
| `MediaUpload` | 파일을 업무 리소스에 연결하기 전에 저장·검증하는 일회성 업로드 세션 |
| `MediaAttachment` | 완료된 Media와 업무 리소스를 연결한 관계와 표시 메타데이터. 연결 도메인이 소유 |
| `MediaStoragePort` | object storage·공개 URL·물리 삭제를 추상화한 공통 포트 |
| `publicUrl` | 서버가 허용된 Nginx 공개 경로로 생성한 접근 URL |
| 업로더 | 업로드 세션을 만든 인증 사용자. 세션 확인·완료·취소 권한을 가짐 |
| 소유 도메인 | Media를 연결할 CatalogProduct·Offer·Review의 존재·소유권·업무 자격을 검증하는 P2·P9·P10 |
| 저장소 adapter | SeaweedFS S3 SDK·HTTP 구현을 `MediaStoragePort` 뒤에 숨기는 infra 구현 |

## 3. 핵심 업무 규칙

- 현재 Media type은 `IMAGE`만 허용하고 `image/jpeg`, `image/png`, `image/webp`만 지원한다.
- 파일 크기는 1바이트 이상 10 MiB 이하이고, 이미지 가로·세로는 각각 1 이상 10,000 이하이어야 한다.
- 확장자가 아니라 MIME signature, 실제 디코딩 결과, checksum으로 파일을 검증한다. 애니메이션 GIF·SVG·실행 파일은 거부한다.
- 업로드는 `세션 생성 → 저장소 업로드 → 완료 검증 → 업무 첨부`의 두 단계 흐름이다. 세션 생성만으로 업무 리소스에 연결되지 않는다.
- 브라우저는 Nginx의 same-origin 업로드 URL로 파일을 보내며 SeaweedFS S3의 내부 주소·bucket·storage key를 알지 못한다.
- Spring Boot의 공개 API는 파일 업로드 본문을 받지 않는다. 업로드 세션 생성과 완료 검증 요청만 처리하고, 실제 파일 전송은 Nginx가 SeaweedFS S3로 전달한다. 필요한 깊은 파일 검증은 저장소 adapter 또는 별도 내부 검증기가 수행한다.
- 업로드 세션은 생성 후 30분 동안만 유효하다. `READY` 업로드도 만료 전까지 첨부하지 않으면 `EXPIRED`가 된다.
- 하나의 `MediaUpload`는 하나의 `MediaAttachment`에만 연결한다. 같은 `uploadId`의 재사용은 거부한다.
- 첨부 시점에 P2·P9·P10이 대상 존재·소유권·권한·최대 개수·정렬·대표 지정 규칙을 다시 검증한다.
- 클라이언트가 보낸 URL을 저장하지 않는다. 공개 URL은 서버가 허용된 CDN 도메인으로 생성한다.
- `storageKey`, presigned URL 토큰, 원본 파일, EXIF 원문은 API 응답·일반 로그·도메인 이벤트에 포함하지 않는다.
- 파일 바이트를 교체하지 않는다. 이미지 교체는 새 업로드·새 첨부·새 `mediaId`를 만들어 URL을 변경한다.

## 4. 불변식과 상태 전이

### 불변식

- `MediaUpload.requesterUserId`만 해당 세션을 완료·취소할 수 있다.
- 실제 객체와 선언된 MIME·크기·checksum이 일치하지 않으면 `READY`가 될 수 없다.
- `READY`가 아닌 업로드는 P2·P9·P10의 첨부 API에서 사용할 수 없다.
- `ATTACHED`, `CANCELLED`, `FAILED`, `EXPIRED` 업로드는 다시 `READY`로 전환하지 않는다.
- `storageKey`는 불투명 값이며 업무 도메인과 외부 클라이언트에 공개하지 않는다.
- Media attachment의 `sortOrder`·대표 이미지·최대 개수·공개 여부는 연결 도메인이 소유한다.

### `MediaUpload` 상태 전이

| 현재 상태 | 사건·조건 | 다음 상태 | 처리 주체 |
|---|---|---|---|
| 없음 | 업로드 세션 생성 | `INITIATED` | P12 |
| `INITIATED` | 객체 업로드 완료 후 검증 성공 | `READY` | P12 |
| `INITIATED`·`READY` | 소유자가 취소 | `CANCELLED` | P12 |
| `INITIATED`·`READY` | `expiresAt` 경과 | `EXPIRED` | 만료 정리 작업 |
| `INITIATED` | 객체 검증 실패 | `FAILED` | P12 |
| `READY` | 업무 도메인 첨부 성공 | `ATTACHED` | P2·P9·P10 + P12 |
| `ATTACHED`·`CANCELLED`·`FAILED`·`EXPIRED` | 재완료·재첨부·재활성화 요청 | 유지 | 요청 거절 |

`MediaAttachment`의 `ACTIVE → ARCHIVED` 전환은 P2·P9·P10이 수행한다. `ARCHIVED`는 공개 조회에서 제외되지만 실제 파일 삭제와 동일한 상태가 아니다.

## 5. 보관·물리 삭제·예외 소유권

- P2·P9·P10의 Media 보관은 물리 삭제가 아닌 `ARCHIVED` 전환이다.
- `INITIATED`·`READY` 세션이 만료되면 고아 저장 객체를 삭제 대상으로 등록한다.
- `ARCHIVED` 파일은 보관 후 30일 동안 복구 감사 목적으로 유지한 뒤 물리 삭제한다. 삭제 실패는 5회까지 재시도하고 이후 운영 알림 대상으로 기록한다.
- 물리 삭제 실패는 CatalogProduct·Offer·Review의 업무 트랜잭션을 롤백하지 않으며 Media 상태를 다시 `ACTIVE`로 바꾸지 않는다.
- P12는 업로드·저장소 오류를 소유한다. 첨부 대상 미존재·소유권·작성 자격·도메인별 최대 개수·대표 충돌은 P2·P9·P10이 소유한다.

| 외부 도메인 | P12 계약 사용 | 외부 도메인이 소유하는 규칙 |
|---|---|---|
| P2 Catalog | `READY` `uploadId`, `publicUrl` | CatalogProduct 존재·ADMIN 권한·20개 제한·대표·정렬·보관 |
| P9 Offer | `READY` `uploadId`, `publicUrl` | Offer 소유 Seller·10개 제한·대표·정렬·보관 |
| P10 Review | `READY` `uploadId`, `publicUrl` | Review 작성 자격·5개 제한·정렬·Review 공개·숨김 |

외부 도메인이 P12 예외를 사용자용 예외로 변환해야 하는 경우, 원본 P12 코드 대신 자신의 도메인 코드와 메시지를 사용한다.

## 6. 최소 저장소·네트워크 프로파일

P12의 기본 프로파일은 Nginx를 외부 진입점으로 사용하고, SeaweedFS S3를 실제 파일의 Origin Storage로 사용한다. Spring Boot는 제어 경로에만 있고 파일 바이트 경로에는 포함되지 않는다.

```text
제어 경로:
Browser ──→ Nginx ──→ Spring Boot
              세션 생성·완료 검증·첨부

파일 경로:
Browser ──→ Nginx ──→ SeaweedFS S3
              PUT 업로드

공개 조회:
Browser ──→ Nginx ──→ SeaweedFS S3
              GET·HEAD, 선택적 Nginx 캐시
```

- SeaweedFS S3는 실제 파일과 checksum 검증 대상을 저장한다.
- Nginx는 업로드 `PUT`을 캐시하지 않고 SeaweedFS S3로 전달한다. 공개 `GET`·`HEAD`는 필요할 때만 Nginx 디스크 캐시를 사용한다.
- Backend가 발급하는 `uploadUrl`은 Nginx의 공개 주소다. Nginx는 presigned URL의 서명 대상 query string·path·Host를 변경하지 않아야 한다.
- 브라우저는 SeaweedFS의 내부 주소·bucket·storage key·access key를 알지 못한다.
- `POST /complete`는 파일을 다시 보내지 않는 작은 제어 요청이다. 기본 프로파일은 webhook·메시지 브로커를 사용하지 않는다.
- Nginx·SeaweedFS·Backend는 Docker 네트워크로 연결하고, SeaweedFS 데이터 디렉터리는 영속 볼륨에 연결한다. Nginx 업로드 경로에는 크기 제한·timeout·캐시 금지를 적용한다.
- Nginx는 인증·Media 업무 권한·checksum 정책을 소유하지 않는다. 해당 검증은 Backend와 저장소 adapter가 수행한다.

## 7. P6와의 경계

Media 보관·물리 삭제 실패는 주문·결제·재고의 업무 보상이 아니므로 P6 Saga로 모델링하지 않는다. P2·P9·P10이 Media 변경 사실을 다른 모듈에 전달해야 할 때만 P6 Outbox를 전달 기반으로 사용할 수 있으며, 이벤트의 업무 의미와 원본은 해당 도메인이 소유한다.

따라서 P6 요구사항 문서에는 Media 모델이나 Media 전용 Saga를 추가하지 않는다.
