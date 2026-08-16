# E-commerce Domain Context

이 문서는 사용자 계정, 판매자 주체, 카탈로그 등록 요청을 설명하는 프로젝트 공통 언어를 정의한다.

## Identity, authentication, and catalog

**Credential**:
User의 신원을 증명하는 장기 인증수단이다. 로컬 이메일·비밀번호 수단인 `UserCredential`과 OAuth 공급자 식별자 수단인 `SocialCredential`을 구분한다.
_Avoid_: User 자체를 인증수단으로 취급함

**Sign-up Session**:
이메일 OTP 또는 OAuth 인증이 완료된 뒤 정식 User를 생성하기 전까지 유지되는 만료 가능한 가입 흐름이다.
_Avoid_: 미완료 가입자를 User로 취급함

**Login Session**:
한 기기에서 발급된 Access Token과 Refresh Token의 생명주기를 묶은 인증 세션이다. 역할 변경·로그아웃·계정 비활성화로 무효화될 수 있다.
_Avoid_: Access Token 하나를 전체 로그인 상태로 취급함

**Guest Token**:
신규 소셜 사용자가 추가 프로필을 제출해 가입을 완료할 때만 사용하는 단기 인증 토큰이다. 정식 User의 로그인 세션이나 Guest Cart의 소유자 식별자가 아니다.
_Avoid_: guest user, Guest Token으로 보호 API에 접근함

**User**:
로그인하고 요청을 실제로 제출하는 개인 계정이다. `userId`는 인증 주체를 식별하며, Seller와 동일한 개념이 아니다. 모든 User는 기본 구매자 역할 `USER`를 가지며 `PRODUCT_MANAGER`, `ADMIN`을 추가로 보유할 수 있다.
_Avoid_: Seller, 판매자 계정

**Address**:
User가 소유하고 직접 관리하는 배송지 리소스다. Address는 User에 종속되지만 자체 CRUD 생명주기와 기본 배송지 불변식을 가지며, User 프로필 응답에 중첩하지 않는다. 주문은 결제 시점에 Address 값을 별도 스냅샷으로 보존한다.
_Avoid_: 주문이 현재 Address를 실시간 참조함, User 프로필의 단순 문자열 주소

**Role Set**:
User가 동시에 보유하는 접근 권한의 집합이다. `USER`는 기본 구매자 역할이고, 활성 Seller가 있으면 `PRODUCT_MANAGER`를 추가하며, 플랫폼 운영자는 `ADMIN`을 추가한다. 역할 추가·삭제는 기존 역할을 교체하지 않는다. API와 이벤트 payload에서는 `USER,PRODUCT_MANAGER`처럼 쉼표로 구분한 문자열로 표현한다.
_Avoid_: single role, role replacement

**Seller**:
상품을 판매하는 사업 주체이자 User에 연결된 판매자 프로필이다. `sellerId`는 판매자 프로필을 식별하고, 권한·판매자 정보·Offer 소유자를 표현한다.
_Avoid_: seller user, 요청자

**Admin Review Item**:
여러 도메인의 관리자 심사 대상을 한 화면에서 조회하기 위한 통합 항목이다. 원본 요청의 상세 데이터와 업무 상태를 소유하지 않고, 원본 모듈과 요청 식별자·목록용 요약·심사 상태만 연결한다.
_Avoid_: P7이 모든 도메인 요청의 원본 payload를 소유함

**Seller Application Storage**:
판매자 신청은 별도 임시 요청 객체가 아니라 `PENDING` 상태의 Seller 레코드로 저장한다. 승인되면 같은 레코드를 활성 판매자 프로필로 사용하고, 거절 후 재신청하면 새 Seller 레코드를 생성해 이전 심사 이력을 보존한다.
_Avoid_: 별도 SellerApplication 임시 레코드, 승인 사유 필수

**Catalog Registration Request**:
활성 Seller가 Category, CatalogProduct 또는 ProductVariant의 등록을 관리자에게 심사 요청한 기록이다. 요청 종류와 제출 payload를 보존하며, 요청 기록은 대상 Seller(`sellerId`)와 실제 제출 User(`requestedByUserId`)를 모두 보존한다.
_Avoid_: CatalogProduct owner, sellerId만으로 표현한 요청자

**Requesting User**:
Catalog Registration Request를 인증된 세션으로 실제 제출한 User다. 기본 흐름에서는 `Seller.userId = requestedByUserId`여야 하며, 요청 본문으로 전달하지 않는다.
_Avoid_: requesterSellerId

**Offer Status History**:
Offer의 상태 변경마다 당시 상태, 비활성화 주체·사유·판매자 안내 문구, 변경 주체와 시각을 보존하는 이력이다. Offer에는 현재 상태에 필요한 비활성화 정보만 남기고, 과거 메시지는 이력에서 확인한다.

**Offer Activation Request**:
관리자에 의해 비활성화된 Offer의 문제를 판매자가 해결한 뒤 관리자에게 재활성화를 요청하는 기록이다. 요청 제출만으로 Offer가 활성화되지 않으며, 관리자 승인 후에만 `ACTIVE`가 된다.
_Avoid_: seller self-reactivation of an admin-blocked offer

**Offer Deactivation Source**:
Offer를 비활성화한 주체다. `SELLER`는 판매자 자발적 비활성화, `ADMIN`은 정책·운영 판단에 따른 차단, `SYSTEM`은 Seller·Catalog 상태에 따른 자동 비활성화를 뜻한다.

**CatalogProduct Attributes**:
카테고리와 무관하게 동적으로 저장하는 상품 속성 값이다. 최상위 JSON object와 공통 크기·깊이 제한만 적용하며, 새로운 카테고리를 제안할 때 별도의 attributes 스키마를 요구하지 않는다.

**Category**:
상품을 분류하는 계층형 공용 메타데이터다. CatalogProduct는 대표 Category 하나를 연결하고, Category는 `parentId`와 `depth`로 계층을 표현한다. 상품의 전체 분류 경로는 대표 Category에서 부모를 따라 계산한다. Seller는 기존 Category를 지정하거나 새 Category 하나를 제안할 수 있고, 생성·수정은 ADMIN이 수행한다.

**SearchKeyword**:
상품명에 없는 동의어·약어·대체 표현으로 상품을 검색하기 위한 내부 메타데이터다. 현재 기본 범위에서는 저장·관리하지 않고 심화사항으로 남긴다.

**ProductType and ItemType**:
ProductType은 플랫폼이 정의한 상품 종류이고, ItemType은 마켓플레이스 분류 체계에서 상품을 배치하는 내부 용어다. 둘 다 고객용 Browse Category와 다르며 현재 기본 범위에서는 저장·검증하지 않는다.

**Review**:
구매·배송 완료된 주문의 Offer에 귀속되는 구매 경험 평가다. 동일 User는 동일 Offer에 하나의 Review만 작성하며, 상품 자체의 공통 메타데이터가 아니라 판매자·판매 조건을 평가한다.

**Media Upload**:
업무 리소스에 연결하기 전에 저장소에 파일을 올리고 형식·크기·무결성을 검증하는 일회성 세션이다. `READY` 상태의 Upload만 CatalogProduct·Offer·Review에 첨부할 수 있고, 하나의 Upload는 하나의 Attachment에만 연결한다.
_Avoid_: 업무 리소스에 임의 URL을 직접 저장

**Media Attachment**:
완료된 Media Upload를 CatalogProduct·Offer·Review에 연결한 관계다. 첨부 대상의 권한·정렬·대표 이미지·공개·보관 규칙은 연결된 업무 도메인이 소유한다.
_Avoid_: P12가 CatalogProduct·Offer·Review의 Media 업무 규칙을 모두 소유함

**Media Storage**:
object storage·CDN·파일 삭제를 추상화한 공통 저장소 경계다. `MediaStoragePort`를 통해 도메인이 사용하며, storage key와 실제 SDK 호출은 공통 infra adapter에 숨긴다.

**Origin Storage**:
Media 파일의 원본을 보존하고 읽기·쓰기의 기준이 되는 저장소다. Edge Cache는 원본이 아니라 Origin Storage의 전달 사본이다.

**Edge Cache**:
Origin Storage에서 가져온 공개 Media를 사용자와 가까운 위치에 임시 보관해 반복 조회를 빠르게 하는 전달 계층이다.

**CDN Control Plane**:
여러 Edge Cache의 등록·건강 상태·라우팅·운영 정책을 관리하는 계층이다. Media 파일과 Media 업무 메타데이터를 소유하지 않는다.

## Cart

**Guest Cart**:
로그인하지 않은 구매자가 사용하는 임시 장바구니다. 주문 자체를 소유하지 않으며, 로그인하면 해당 User의 장바구니와 병합된다.
_Avoid_: anonymous order, guest user

**Cart Owner**:
장바구니를 변경할 수 있는 주체다. 회원 장바구니는 User이고, 비회원 장바구니는 익명 브라우저 주체다.
_Avoid_: cartId alone as ownership, guest user

**Offer Purchase Limit**:
Offer가 고객 한 명의 장바구니·주문에서 허용하는 최대 구매 수량이다. 재고의 현재 수량과는 별개의 구매 제한이며, 장바구니는 이 Offer 제한만 따른다.

## Order

**Checkout Selection**:
사용자가 이번 주문에서 선택한 Cart Item의 집합이다. 동일 사용자의 동일한 선택은 Cart Item의 정렬된 ID 조합으로 식별하며, 수량·쿠폰·포인트와는 별개의 값이다.

**Pending Order Reuse**:
동일 사용자가 동일한 Checkout Selection으로 다시 주문서 생성을 요청하면 새 주문을 만들지 않고 기존 유효한 `PENDING` 주문을 최신 요청 기준으로 갱신하는 규칙이다. 수량·현재 가격·적용 쿠폰 매핑·사용 포인트·금액 계산 결과는 최신 요청으로 덮어쓴다.
