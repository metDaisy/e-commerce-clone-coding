# P12 Media 문서 안내

P12는 상품·판매 조건·리뷰에 공통으로 사용되는 Media 업로드와 저장 계약을 정의한다. 업로드 파일의 검증·저장·공개 URL은 P12가 소유하고, Media를 연결한 업무 대상의 존재·소유권·권한·공개 여부는 P2 Catalog, P9 Offer, P10 Review가 각각 소유한다. 기본 인프라 경로는 Browser → Nginx → SeaweedFS S3이며, Spring Boot의 공개 업로드 API는 파일 바이트가 아닌 업로드 세션과 검증 결과를 처리한다.

공통 URI, 성공 응답, 페이지네이션, 인증, 오류 응답 형식은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

## 1. 문서 목록

| 문서 | 역할 | 포함 내용 |
|---|---|---|
| [P12 Media Policy](p12-policy.md) | 정책 | 범위·책임, 용어, 파일·업로드 규칙, 상태 전이, 보관·삭제, 도메인 경계 |
| [P12 Media API](p12-media.md) | 데이터 모델·API | `MediaUpload`, `MediaStoragePort`, 첨부 계약, 업로드 API, 검증·예외 매트릭스 |

P12는 CatalogProduct·Offer·Review 애그리거트나 해당 도메인의 Media attachment Repository를 소유하지 않는다. 각 업무 도메인은 공개 계약으로 P12의 `READY` 업로드를 확인한 뒤 자신의 로컬 첨부 모델을 만든다.

## 2. 책임과 경계

| 책임 | 담당 | 참조 |
|---|---|---|
| 업로드 세션·파일 검증·업로드 상태 | P12·`common` | [P12 Media API](p12-media.md) |
| 저장소 포트와 저장소 adapter 경계 | `common` | [`MediaStoragePort`](p12-media.md#2-2-mediastorageport) |
| 외부 진입점·업로드 reverse proxy·공개 파일 전달 | Nginx | P12 인프라 프로파일 |
| 실제 파일 Origin Storage | SeaweedFS S3 | P12 인프라 프로파일 |
| CatalogProduct Media 대상·대표·정렬·보관 | P2 Catalog | [P2 CatalogProduct](../p2/p2-catalog-product.md) |
| Offer Media 대상·대표·정렬·보관 | P9 Offer | [P9 Offer](../p9/p9-offer.md) |
| Review 첨부 자격·최대 개수·정렬·노출 | P10 Review | [P10 Review](../p10/p10-review.md) |
| Outbox 전달과 업무 Saga | P6 Infrastructure | [P6 Infrastructure](../p6/p6-infrastructure.md) |

- P2·P9·P10은 storage SDK, presigned URL 생성 방식, storage key 형식을 알지 못한다.
- 브라우저는 SeaweedFS의 내부 주소·bucket·storage key를 알지 못하고 Nginx의 공개 주소만 사용한다.
- Nginx는 업로드 `PUT`을 캐시하지 않고 SeaweedFS S3로 전달한다. 공개 `GET`·`HEAD` 캐시는 선택 사항이다.
- P12는 첨부 대상의 존재·소유권·업무 자격을 판단하지 않는다.
- 다른 도메인의 내부 모델·Repository를 직접 참조하지 않고 `uploadId`와 공개 응답 계약만 사용한다.
- P6는 Media 모델이나 Media 전용 보상 Saga를 소유하지 않는다. 다른 도메인에 사실을 전달해야 할 때만 Outbox를 전달 수단으로 사용한다.
- webhook과 메시지 브로커는 기본 업로드 완료 경로에 포함하지 않는다. 브라우저의 `/complete` 제어 요청으로 업로드 검증을 완료한다.

## 3. 문서 작성 순서

1. [P12 Media Policy](p12-policy.md)에서 범위·책임·불변식·상태 전이를 확정한다.
2. [P12 Media API](p12-media.md)에서 정책을 만족하는 모델·저장소 포트·업로드 API를 정의한다.
3. P2·P9·P10의 Media API에서 각 업무 대상에 대한 첨부·조회·보관 계약을 정의한다.
4. P12와 업무 도메인 사이의 상태·예외 매핑이 일치하는지 확인한다.

## 4. 작성 원칙

- 정책 문서는 API나 ORM 구현보다 오래 유지되는 파일·업로드·보관 규칙을 작성한다.
- P12 문서는 공통 파일 계약을 정의하고 업무 도메인의 자격·표시 규칙을 중복하지 않는다.
- `storageKey`, presigned URL 토큰, 원본 파일 내용과 EXIF는 외부 API·일반 로그·도메인 이벤트에 노출하지 않는다.
- 구현 결과가 달라질 수 있는 선택 표현 대신 기본 동작과 실패 조건을 하나로 확정한다.
- 문서 간 규칙이 충돌하면 P12 Policy를 기준으로 P12 API 문서를 수정하고, 업무 도메인의 대상 규칙은 해당 도메인 Policy를 기준으로 수정한다.
