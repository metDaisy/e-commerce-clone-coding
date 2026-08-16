# P1 User 문서 안내

P1은 사용자의 프로필·역할·활성 상태와 사용자가 소유하는 주소를 정의한다. 인증수단과 세션은 P11 Auth가, 관리자 운영 진입점은 P7 Admin이 담당한다.

공통 URI, 성공 응답, 예외 응답, 인증, 페이지네이션은 [공통 API 계약](../index.md#공통-api-계약)을 따른다.

## 1. 문서 목록

| 문서 | 역할 | 포함 내용 |
|---|---|---|
| [P1 Policy](p1-policy.md) | 정책 | 범위·책임, 행위자, 프로필·역할·주소 규칙, 불변식, 상태 전이, 도메인 간 규칙 |
| [User API](p1-user.md) | 데이터 모델·API | User 모델, 관계·제약, 프로필 API, 성공 응답·예외 매트릭스 |
| [Address API](p1-address.md) | 데이터 모델·API | Address 모델, User 소유권, 주소 CRUD·기본 배송지 API, 성공 응답·예외 매트릭스 |

## 2. 책임과 경계

| 책임 | 담당 도메인·모듈 | 참조 문서 |
|---|---|---|
| User 프로필·역할·활성 상태 원본 | P1 User | [P1 Policy](p1-policy.md), [User API](p1-user.md) |
| Address 원본·소유권·기본 배송지 상태 | P1 User | [P1 Policy](p1-policy.md), [Address API](p1-address.md) |
| 이메일·비밀번호·OAuth 인증수단·토큰·세션 | P11 Auth | [P11 Index](../p11/p11-index.md) |
| 관리자 역할 변경·계정 운영 진입점 | P7 Admin | [P7 Access](../p7/p7-access.md), [P7 Admin](../p7/p7-admin.md) |
| Seller 프로필과 판매자 상태 | P8 Seller | [P8 Seller Policy](../p8/p8-policy.md) |
| 주문 배송지 선택과 결제 시점 주소 스냅샷 | P5 Order | [P5 Policy](../p5/p5-policy.md), [P5 Order](../p5/p5-order.md) |
| 이벤트 Outbox 기록·재시도·운영 | P6 Infrastructure | [P6 Infrastructure](../p6/p6-infrastructure.md) |

- P1은 인증수단의 내부 모델·Repository·비밀번호·토큰을 소유하지 않는다.
- 다른 도메인은 P1의 내부 모델을 직접 참조하지 않고 공개 API·Named Interface·이벤트를 사용한다.
- P1이 발행하는 `UserRolesChangedEvent`의 사실과 payload는 P1이 정의하고, Outbox 전달·재시도는 P6, 세션 무효화 소비는 P11이 담당한다.

## 3. 문서 작성 순서

1. [P1 Policy](p1-policy.md)에서 범위·책임과 확정 업무 규칙을 정한다.
2. [User API](p1-user.md)와 [Address API](p1-address.md)에서 정책을 만족하는 리소스 모델과 API를 정의한다.
3. 각 리소스 API별 성공·P1 예외와 공통 인증 예외를 완성한다.
4. 정책 또는 리소스가 추가되면 이 문서의 목록과 책임 표를 갱신한다.

## 4. 작성 원칙

- 이 문서는 P1의 안내와 책임 경계만 작성하고, 정책·필드·API 계약을 중복하지 않는다.
- 정책 문서가 API 문서보다 우선하며, API 문서는 정책을 만족하는 구체 계약만 정의한다.
- `loginEmail`은 로컬 인증수단이 존재할 때만 P11 공개 계약에서 제공되는 값이며, P1 `User` 데이터의 소유 필드가 아니다.
- User 응답은 Address를 중첩하지 않는다. 주소 목록·상세·기본 배송지 변경은 [Address API](p1-address.md)를 사용한다.
- `Your Orders`, `Your Payments`, `Login & security`는 각각 P5·P11 문서의 기능으로 연결하고 P1에서 재정의하지 않는다.
