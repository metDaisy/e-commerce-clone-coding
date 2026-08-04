# E-Commerce Domain Requirements (기능 명세서)

> 문서 성격: P1~P6의 **목표 비즈니스 규칙**을 정의한다. 현재 구현 여부는
> [current-state.md](current-state.md), 전체 문서 지도는 [index.md](index.md)를 확인한다.
> 코드·DB와 충돌하는 규칙은 임의로 맞추지 않고 결정 후 함께 갱신한다.

본 문서는 **Amazon.com 클론 코딩** 프로젝트의 각 도메인(P1~P6)에서 구현해야 할 구체적인 비즈니스 룰, 제약 조건, API 요구사항, 유효성 검증 규칙 및 엣지 케이스를 정의합니다.

> **참고**: 본 프로젝트는 Spring Modulith 기반 모듈러 모놀리스 아키텍처를 채택하며, 도메인 간 통신은 Spring Application Event를 통해 이루어집니다. 다른 모듈의 Bean을 직접 주입받는 것은 금지합니다.

---

## [P1] User & Auth (사용자 및 인증)

### 1. 회원가입 및 프로필 관리 (Users)

#### 1-1. 회원가입
- **필수 입력 필드**: 이름(name), 이메일(email), 비밀번호(password)
- **이메일 유효성 검증**: RFC 5322 형식 준수, DB 수준 `UNIQUE` 제약조건으로 중복 가입 방지
- **비밀번호 정책**:
  - 최소 8자 이상
  - 영문 대소문자, 숫자, 특수문자 중 최소 2종류 이상 조합
  - Bcrypt 단방향 암호화 적용 (cost factor: 10 이상)
- **가입 완료 시**: `USER` 권한 자동 부여, 포인트 잔액 0으로 초기화
- **에러 케이스**:
  - `EMAIL_ALREADY_EXISTS` (409): 이미 가입된 이메일로 재가입 시도
  - `INVALID_PASSWORD_FORMAT` (400): 비밀번호 정책 미충족

#### 1-2. 프로필 조회 및 수정
- **조회 가능 필드**: 이름, 이메일, 연락처(phone), 가입일, 포인트 잔액
- **수정 가능 필드**: 이름, 연락처
- **이메일 변경**: 별도 API로 분리하며, 변경 시 기존 비밀번호 재확인 필수
- **접근 제어**: 본인의 프로필만 조회/수정 가능 (userId 일치 검증)

#### 1-3. 계정 비활성화 (논리적 삭제)
- 회원 탈퇴 시 `is_active=false` 처리 (물리적 삭제 금지, 주문 이력 보존 목적)
- 비활성화된 계정으로 로그인 시도 시 `ACCOUNT_DEACTIVATED` (403) 에러 반환
- 비활성화 후 90일 경과 시 개인정보 마스킹 처리 (배치 스케줄러)

---

### 2. 인증 및 토큰 관리 (Auth & Credentials)

#### 2-1. 로컬 로그인 (Form Login)
- **인증 방식**: 이메일 + 비밀번호 기반 Spring Security Form Authentication
- **성공 시 발급**:
  - Access Token (JWT, HttpOnly 쿠키, 만료: 30분)
  - Refresh Token (JWT, HttpOnly 쿠키, 만료: 7일)
- **JWT Claims**: userId(sub), role, deviceId, issuedAt, expiration
- **에러 케이스**:
  - `BAD_CREDENTIALS` (401): 이메일 또는 비밀번호 불일치
  - `ACCOUNT_DEACTIVATED` (403): 비활성화된 계정
  - `ACCOUNT_LOCKED` (423): 연속 로그인 실패 5회 초과 시 30분간 잠금

#### 2-2. OAuth2 소셜 로그인
- **지원 프로바이더**: Google, Naver, Kakao, GitHub
- **인증 플로우**:
  1. 사용자가 소셜 로그인 버튼 클릭 → OAuth2 Authorization Code 요청
  2. 콜백 수신 → `OAuth2Provider` enum을 통해 프로바이더별 사용자 속성을 표준화된 형식으로 정규화
  3. `provider + providerId` 조합으로 기존 `SocialCredential` 조회
  4. **기존 사용자**: JWT 토큰 발급 후 메인 페이지 리다이렉트
  5. **신규 사용자**: 게스트 토큰(Guest Token) 발급 후 회원가입 연동 페이지 리다이렉트
- **게스트 토큰**: 소셜 인증 정보(provider, providerId)를 Claims에 포함한 임시 JWT (만료: 10분)
- **계정 연동**: 폼 로그인 성공 시 게스트 토큰이 존재하면, 해당 소셜 계정을 기존 `users` 레코드에 `SocialCredential`로 연결
- **`social_credentials` 테이블 제약조건**: `(provider, provider_id)` 조합에 UNIQUE 인덱스

#### 2-3. 토큰 갱신 및 로그아웃
- **토큰 갱신**: Refresh Token이 유효할 경우 새 Access Token 재발급
- **로그아웃**:
  - Access Token을 `BlacklistRegistry` (Caffeine 캐시)에 등록하여 즉시 무효화
  - Refresh Token 쿠키 삭제
- **전체 기기 로그아웃**: 특정 userId의 모든 토큰을 사용자 단위로 블랙리스트 등록 (issuedAt 기준)
- **JWT 인증 필터** (`JwtAuthenticationFilter`):
  - 모든 요청에서 JWT 추출 → 서명 검증 → 블랙리스트 확인 → SecurityContext 설정
  - 블랙리스트에 등록된 토큰 사용 시 `BLACKLISTED_TOKEN` (401)

#### 2-4. 다중 인증 수단 관리
- `users` : `user_credentials` = 1:0..1 관계
- `users` : `social_credentials` = 1:N 관계
- 하나의 계정에 로컬 + 다수의 소셜 로그인 수단을 동시에 연결 가능
- 소셜 계정 연결 해제 API 제공 (단, 마지막 인증 수단은 해제 불가)

---

### 3. 권한 기반 접근 제어 (Role-based Authorization)

| 권한 | 설명 | 접근 가능 범위 |
|------|------|---------------|
| `USER` | 일반 고객 | 본인 장바구니, 주문, 주소록, 위시리스트, 리뷰 작성 |
| `PRODUCT_MANAGER` | 상품 관리자 | 상품 CRUD, 재고 관리, 타임세일 설정 |
| `ADMIN` | 최고 관리자 | 전사 쿠폰 발행, 전체 주문/유저 조회, 권한 변경 |

- **리소스 소유권 검증**: `USER` 권한의 모든 API에서 요청자의 userId와 리소스 소유자의 userId 일치 여부를 반드시 검증
- **권한 변경**: `ADMIN`만 다른 사용자의 권한을 변경 가능하며, 본인의 `ADMIN` 권한은 해제 불가
- **Spring Security 적용**: `@PreAuthorize`, `@Secured` 또는 SecurityConfig에서 URL 패턴 기반으로 권한 제어

---

### 4. 주소록 관리 (Addresses)

#### 4-1. CRUD 규칙
- **등록 제한**: 사용자당 최대 5개 (초과 시 `ADDRESS_LIMIT_EXCEEDED` (400))
- **필수 입력 필드**: 수령인 이름(recipient_name), 연락처(phone), 우편번호(zip_code), 기본 주소(address_line1)
- **선택 입력 필드**: 상세 주소(address_line2), 배송 메모(delivery_note)
- **주소 라벨**: "집", "회사" 등 사용자 정의 별칭(label) 지원

#### 4-2. 기본 배송지 관리
- `is_default=true` 설정 시 해당 사용자의 기존 기본 배송지를 **동일 트랜잭션** 내에서 `is_default=false`로 변경 (원자성 보장)
- 기본 배송지가 삭제될 경우, 가장 최근에 등록된 주소를 자동으로 새 기본 배송지로 승격
- 주소가 1개만 남은 상태에서는 해당 주소가 자동으로 기본 배송지로 설정
- **주문에서의 참조**: 주문 시점에 선택된 주소의 스냅샷을 주문 테이블에 별도 저장 (이후 주소 변경이 기존 주문에 영향을 미치지 않도록)

---

### 5. 포인트 시스템 (Point & Point Histories)

#### 5-1. 적립 규칙
- **주문 완료 적립**: 배송 완료(`DELIVERED`) 상태 도달 시 최종 결제 금액의 1% 포인트 자동 적립
- **리뷰 작성 적립**: 텍스트 리뷰 50P, 이미지 포함 리뷰 100P (상품당 1회)
- **관리자 수동 지급**: `ADMIN` 권한으로 특정 사용자에게 포인트를 직접 지급/차감 가능

#### 5-2. 사용 규칙
- **최소 사용 단위**: 100P 이상부터 사용 가능
- **최대 사용 제한**: 1회 주문 시 최종 결제 금액의 50%까지만 포인트로 결제 가능
- **유효기간**: 적립일로부터 1년 (만료 대상 포인트 일괄 차감 배치 스케줄러)

#### 5-3. 데이터 무결성
- **히스토리 불변성 (Immutability)**: `point_histories` 테이블의 레코드는 INSERT만 허용, UPDATE/DELETE 금지
  - 적립: `change_amount > 0`, 사용: `change_amount < 0`
  - 취소 시: 반대 부호의 새 레코드 INSERT (사유: `CANCEL_REFUND` 등)
- **음수 잔액 방지**: `users.point_balance` 컬럼에 DB CHECK 제약조건 (`point_balance >= 0`) 적용
  - 잔액 부족 시 `INSUFFICIENT_POINT` (400) 에러 발생 및 트랜잭션 롤백
- **동시성 제어**: 동일 사용자의 포인트 차감 요청이 동시에 들어올 경우 이중 차감 방지
  - `SELECT ... FOR UPDATE` (비관적 락) 적용하여 직렬화
  - 또는 Optimistic Lock (`@Version` 컬럼) 적용 후 재시도 로직

---

### 6. 관심 상품 (Wishlists)

#### 6-1. 토글 방식 동작
- `POST /api/wishlists/{productId}`: 미등록 상태면 추가, 이미 등록 상태면 삭제 (멱등성)
- DB 레벨 `(user_id, product_id)` UNIQUE 인덱스로 중복 방지

#### 6-2. 조회
- 페이지네이션 지원 (기본 20건, Cursor 기반 or Offset 기반)
- 각 위시리스트 아이템에 상품 요약 정보(이름, 가격, 썸네일, 재고 상태) 포함
- 품절된 상품은 목록에서 시각적으로 구분 표시 (삭제하지 않음)

#### 6-3. 알림 연동 (향후 확장)
- 위시리스트에 담긴 상품의 가격이 하락하거나 타임세일 진입 시 알림 이벤트 발행 가능하도록 설계

---

## [P2] Catalog & Inventory (전시 및 재고)

### 1. 카테고리 관리 (Categories)

#### 1-1. 계층형 구조
- **최대 3단계** 계층 지원: 대분류 → 중분류 → 소분류
  - 예: `전자제품` → `컴퓨터/노트북` → `노트북`
- `parent_id` 자기참조 FK로 트리 구조 구현
- **루트 카테고리**: `parent_id = NULL`

#### 1-2. API 요구사항
- **전체 카테고리 트리 조회**: 중첩(nested) JSON 형태로 전체 트리를 한 번의 API 호출로 반환
- **카테고리별 상품 목록**: 특정 카테고리 선택 시 하위 카테고리의 상품까지 포함하여 조회
- **카테고리 CRUD**: `ADMIN` 권한 전용
  - 삭제 시: 하위 카테고리 또는 연결된 상품이 존재하면 `CATEGORY_HAS_CHILDREN` (409) 에러

---

### 2. 상품 관리 (Products)

#### 2-1. 상품 등록
- **권한**: `PRODUCT_MANAGER` 또는 `ADMIN`
- **필수 필드**: 상품명(name), 설명(description), 정상가(price), 카테고리(category_id), 초기 재고(stock_quantity)
- **선택 필드**: 태그(tags), 브랜드(brand), 무게(weight), 치수(dimensions)
- **상품 상태**: `ACTIVE` (판매중), `INACTIVE` (비활성화), `SOLD_OUT` (품절)
  - 재고가 0이 되면 자동으로 `SOLD_OUT`으로 전환
  - 재고가 보충되면 `ACTIVE`로 복원

#### 2-2. 상품 수정 및 삭제
- **수정**: 상품 기본 정보, 가격, 재고 수량 등 개별 필드 수정 가능
- **논리적 삭제**: `is_deleted=true` 처리. 기존 주문 이력에서 참조되는 상품 정보는 보존
- **삭제된 상품**: 상품 목록 API에서 제외, 단 주문 상세 조회 시에는 "(삭제된 상품)" 표기와 함께 표시

#### 2-3. 상품 조회 및 검색
- **상품 상세 조회**: 상품 기본 정보 + 이미지 목록 + 현재 적용 가격 (타임세일 여부 반영) + 리뷰 요약 (평균 평점, 총 리뷰 수) + 재고 상태
- **상품 목록 조회** (페이지네이션 필수):
  - **정렬 옵션**: 최신순, 가격 낮은순, 가격 높은순, 평점순, 리뷰 많은순, 판매량순
  - **필터링**: 카테고리, 가격 범위(min/max), 평점(N점 이상), 재고 있음(in-stock only), 타임세일 진행 중
- **태그 기반 검색**: `product_tags` 다대다 매핑을 통해 특정 태그(들)를 가진 상품 필터링
- **키워드 검색**: 상품명, 설명, 브랜드에 대한 LIKE 검색 (향후 Elasticsearch 전환 고려)

#### 2-4. 다형성 이미지 (Images)
- **공통 이미지 테이블**: `entity_type` (PRODUCT, REVIEW 등) + `entity_id`로 다형성 참조
- **이미지 정렬**: `sort_order` 필드로 표시 순서 관리
- **상품 이미지 구분**: 메인 썸네일(1장 필수), 추가 이미지(최대 8장)
- **이미지 저장**: 파일 시스템 또는 S3 호환 스토리지에 저장, DB에는 URL 경로만 기록
- **이미지 리사이징**: 업로드 시 원본, 중간(목록용), 소형(썸네일용) 3가지 사이즈 생성 (향후 확장)

---

### 3. 타임세일 및 재고 (Inventory & Time Sale)

#### 3-1. 타임세일 로직
- **적용 조건**: `sale_start_at <= 현재시각 <= sale_end_at` 이면 `sale_price` 적용, 그 외에는 `price` 적용
- **할인율 표시**: API 응답에 원래 가격 대비 할인율(%) 계산하여 포함
- **타임세일 설정**: `PRODUCT_MANAGER` 또는 `ADMIN`만 가능
  - `sale_price`는 반드시 `price`보다 작아야 함 (유효성 검증)
  - `sale_end_at`은 반드시 `sale_start_at`보다 이후여야 함
  - 이미 진행 중인 타임세일이 있는 상품에 대해 새 타임세일 설정 시 `SALE_ALREADY_ACTIVE` (409) 에러

#### 3-2. 재고 관리
- **재고 차감**: 결제 완료(`PAID`) 시점에 `stock_quantity`를 차감
- **재고 복원**: 주문 취소 또는 Saga 보상 트랜잭션 발생 시 차감된 수량만큼 복원
- **동시성 제어**: 인기 상품 동시 결제 시 재고가 0 미만으로 떨어지지 않도록 제어
  - 낙관적 락: `@Version` 컬럼 + 재시도 로직 (최대 3회)
  - 또는 비관적 락: `SELECT ... FOR UPDATE`
  - DB CHECK 제약조건: `stock_quantity >= 0`

#### 3-3. 실시간 재고 브로드캐스팅 (WebSocket)
- 재고 차감 또는 `SOLD_OUT` 전환 시 해당 상품 페이지를 구독 중인 클라이언트에게 WebSocket으로 실시간 알림
- **구독 채널**: `/topic/products/{productId}/stock`
- **메시지 페이로드**: `{ productId, stockQuantity, status, salePrice(nullable) }`

---

### 4. 리뷰 (Reviews)

#### 4-1. 작성 권한 및 규칙
- **작성 조건**: 해당 상품을 구매하고 배송 완료(`DELIVERED`) 상태에 도달한 사용자만 작성 가능
  - 미구매 또는 미배송 완료 시 `REVIEW_NOT_ELIGIBLE` (403)
- **1상품 1리뷰**: 동일 상품에 대해 사용자당 1개의 리뷰만 작성 가능
  - `(user_id, product_id)` UNIQUE 제약조건
  - 중복 작성 시도 시 `REVIEW_ALREADY_EXISTS` (409)
- **평점**: 1~5 정수 (필수), 텍스트 리뷰 (선택, 최대 2000자), 이미지 (선택, 최대 5장)

#### 4-2. 리뷰 CRUD
- **수정**: 작성자 본인만 수정 가능, 평점 및 텍스트 변경 가능
- **삭제**: 작성자 본인 또는 `ADMIN`만 삭제 가능 (논리적 삭제)
- **조회**: 특정 상품의 리뷰 목록 (페이지네이션, 최신순/평점순 정렬)

#### 4-3. 리뷰 통계
- 상품별 평균 평점과 총 리뷰 수를 `products` 테이블에 비정규화하여 캐싱
  - `avg_rating` (소수점 첫째 자리), `review_count`
  - 리뷰 등록/수정/삭제 시 이벤트를 발행하여 비동기적으로 재계산

#### 4-4. 리뷰 도움됨 투표 (향후 확장)
- "이 리뷰가 도움이 되었나요?" 기능을 위한 `review_votes` 테이블 설계 고려
- 도움됨 수 기준 정렬 옵션 제공

---

## [P3] Cart (장바구니)

### 1. 장바구니 관리 (Cart & Cart Items)

#### 1-1. 기본 규칙
- **1유저 1카트**: 사용자당 하나의 활성화된 장바구니(`status=ACTIVE`)만 존재
- 최초 상품 추가 시 카트가 없으면 자동 생성
- **담기 수량 제한**: 단일 상품 최대 10개, 장바구니 전체 아이템 종류 최대 50개
  - 초과 시 `CART_ITEM_LIMIT_EXCEEDED` (400)

#### 1-2. 상품 추가/수정/삭제
- **추가**: 이미 담긴 상품을 다시 추가하면 수량을 누적 (동일 상품 중복 row 생성 금지)
- **수량 변경**: 최소 1, 최대 10. 0으로 설정 시 해당 아이템 삭제와 동일하게 처리
- **개별 삭제**: 특정 cart_item 삭제
- **전체 비우기**: 장바구니의 모든 아이템 일괄 삭제

#### 1-3. 동적 상태 검증
- **장바구니 조회 시점**마다 실시간으로 다음 사항을 검증하여 응답에 반영:
  - 상품 가격 변동 여부 → 변동 시 `priceChanged: true` 플래그 + 이전 가격 / 현재 가격 표시
  - 상품 품절 여부 → 품절 시 `outOfStock: true` 플래그 (결제 진행 불가 표시)
  - 상품 삭제 여부 → 삭제된 상품은 `unavailable: true` 플래그
  - 타임세일 적용 여부 → 타임세일 진행 중이면 할인가로 자동 반영
- **결제 시점 최종 검증**: 장바구니 → 주문 전환 시 위 검증을 한 번 더 수행하고, 문제가 있으면 주문 생성 차단

#### 1-4. 장바구니 요약 정보
- **API 응답에 포함할 정보**:
  - 아이템 목록 (상품명, 썸네일, 수량, 단가, 소계)
  - 상품 총액 (= Σ 각 아이템의 `단가 × 수량`)
  - 적용 가능한 쿠폰 목록 (선택 사항, P4 연동 후)
  - 예상 배송비 (무료 배송 기준 금액 미달 시 표시)

#### 1-5. 결제 후 처리
- 결제 성공 시 주문에 포함된 cart_items는 장바구니에서 자동 삭제
- 부분 결제 (장바구니의 일부 상품만 선택하여 결제) 지원

---

## [P4] Coupon (쿠폰 및 혜택)

### 1. 쿠폰 마스터 (Coupons)

#### 1-1. 쿠폰 생성 (`ADMIN` 전용)
- **필수 필드**: 쿠폰명(name), 할인 타입(discount_type), 할인 값(discount_value), 유효기간(valid_from ~ valid_until)
- **할인 타입**:
  - `PERCENTAGE` (정률 할인): 예) 10% 할인 → `discount_value = 10`
    - 반드시 **최대 할인 한도** (`max_discount_amount`) 설정 필수. 예) 최대 5,000원
  - `FIXED_AMOUNT` (정액 할인): 예) 3,000원 할인 → `discount_value = 3000`
- **적용 조건**:
  - `minimum_order_amount`: 최소 주문 금액 (미달 시 쿠폰 적용 불가)
  - `applicable_category_id` (선택): 특정 카테고리 상품에만 적용 가능한 쿠폰
- **발급 수량 관리**:
  - `total_quantity`: 전체 발급 가능 수량 (NULL이면 무제한)
  - `issued_quantity`: 현재까지 발급된 수량
  - `issued_quantity >= total_quantity` 이면 `COUPON_EXHAUSTED` (409)

#### 1-2. 쿠폰 유효성 검증
- 유효기간 외(`valid_from` 이전 또는 `valid_until` 이후)의 쿠폰은 발급 및 사용 불가
- 비활성화(`is_active=false`)된 쿠폰은 신규 발급 불가 (기발급 분은 유효기간 내 사용 가능)

---

### 2. 사용자 발급 쿠폰 (User Coupons)

#### 2-1. 쿠폰 발급
- `POST /api/coupons/{couponId}/claim`: 사용자가 쿠폰을 발급받아 보유
- **중복 발급 방지**: `(user_id, coupon_id)` UNIQUE 제약조건 → `COUPON_ALREADY_CLAIMED` (409)
- 발급 시 `issued_quantity` 원자적 증가 (동시성 제어 필수)

#### 2-2. 쿠폰 상태 관리
| 상태 | 조건 |
|------|------|
| `AVAILABLE` | 미사용 + 유효기간 이내 |
| `USED` | 주문에서 사용 완료 (`used_at` 기록, `order_id` 연결) |
| `EXPIRED` | 유효기간 만료 (배치 스케줄러에 의해 자동 전환) |

- **사용 취소**: 주문 취소 시 사용된 쿠폰을 `AVAILABLE`로 복원 (단, 유효기간이 아직 남아있는 경우에만)

#### 2-3. 만료 스케줄러
- `@Scheduled(cron = "0 0 * * * *")`: 매 정시마다 실행
- `valid_until < 현재시각 AND status = AVAILABLE` 인 사용자 쿠폰을 일괄 `EXPIRED` 처리
- 배치 사이즈: 1000건 단위 벌크 업데이트

#### 2-4. 내 쿠폰 조회
- **필터**: 사용 가능한 쿠폰, 사용 완료 쿠폰, 만료 쿠폰 구분 조회
- **정렬**: 만료 임박순 기본
- 각 쿠폰에 할인 조건 요약 (할인율/금액, 최소 주문 금액, 최대 할인 한도, 잔여 유효기간) 포함

---

## [P5] Order, Payment, Delivery (결제 트랜잭션)

### 1. 주문 생성 및 금액 산출 (Orders)

#### 1-1. 주문 생성 플로우
```
장바구니 → [상품/재고 검증] → 주문 임시 생성(PENDING) → [금액 산출] → 결제 시도 → 성공 시 PAID → 배송 생성
                                                                             → 실패 시 CANCELED
```

#### 1-2. 주문 요청 페이로드
- `cartItemIds[]`: 결제할 장바구니 아이템 ID 목록 (부분 결제 지원)
- `addressId`: 배송지 주소 ID
- `userCouponId` (선택): 적용할 사용자 쿠폰 ID (1주문 1쿠폰)
- `pointAmount` (선택): 사용할 포인트 금액
- `paymentMethodId`: 결제 수단 ID

#### 1-3. 금액 산출 로직 (핵심 비즈니스 로직)
```
① 상품 총액 = Σ (각 아이템의 적용가격 × 수량)
    - 적용가격 = 타임세일 기간이면 sale_price, 아니면 price
② 쿠폰 할인액 계산:
    - PERCENTAGE: min(상품총액 × discount_value / 100, max_discount_amount)
    - FIXED_AMOUNT: discount_value
    - 단, 상품총액 < minimum_order_amount 이면 쿠폰 적용 불가 에러
③ 포인트 사용액 검증:
    - point_amount > 사용자 보유 포인트이면 INSUFFICIENT_POINT 에러
    - point_amount > (상품총액 - 쿠폰할인액) × 50% 이면 POINT_LIMIT_EXCEEDED 에러
④ 최종 결제 금액 = 상품총액 - 쿠폰할인액 - 포인트사용액
    - 최종 결제 금액 < 0 인 경우 0으로 처리 (전액 할인)
```

#### 1-4. 주문 상태 머신
| 상태 | 설명 | 전이 가능 상태 |
|------|------|---------------|
| `PENDING` | 주문 임시 생성, 결제 대기 | → `PAID`, `CANCELED` |
| `PAID` | 결제 완료 | → `CANCELED` (전액 환불) |
| `CANCELED` | 주문 취소됨 | 최종 상태 (전이 불가) |

- 잘못된 상태 전이 시도 시 `INVALID_ORDER_STATUS_TRANSITION` (400)

#### 1-5. 주문 취소
- `PENDING` 상태: 즉시 취소 가능
- `PAID` 상태: 환불 프로세스 트리거
  - 사용된 쿠폰 복원 (유효기간 내인 경우)
  - 사용된 포인트 복원
  - 차감된 재고 복원
  - 결제 취소 (PG사 환불 API 모의 호출)
- 배송이 `SHIPPED` 이상이면 취소 불가 → `ORDER_CANNOT_BE_CANCELED` (409)

#### 1-6. 주문 조회
- **주문 목록**: 내 주문 내역 페이지네이션 조회 (최신순 정렬)
- **주문 상세**: 주문 아이템 목록, 배송 주소 스냅샷, 적용된 쿠폰/포인트 정보, 결제 정보, 배송 상태
- **재주문**: 이전 주문의 상품들을 한 번에 장바구니에 다시 담는 기능

---

### 2. 결제 연동 (Payments)

#### 2-1. 결제 수단 관리 (Payment Methods)
- **다형성 설계**: Strategy 패턴을 적용하여 결제 수단별 구현을 인터페이스 뒤에 캡슐화
- **지원 결제 수단**:
  - `CREDIT_CARD`: 신용/체크카드 결제
  - `KAKAO_PAY`: 카카오페이 간편결제
  - `BANK_TRANSFER`: 계좌이체
- 사용자별 결제 수단 등록/삭제 API 제공
- 민감 정보(카드 번호 등)는 마스킹 처리하여 저장 (예: `**** **** **** 1234`)

#### 2-2. 모의(Mock) PG사 연동
- 실제 PG사 API 대신 Mock 객체를 통해 결제 승인/취소 시뮬레이션
- **결제 승인**: 80% 확률 성공, 20% 확률 실패 (의도적 실패 시나리오)
  - 실패 시 `PAYMENT_DECLINED` 에러 코드 + 실패 사유 반환
- **결제 취소(환불)**: 95% 확률 성공
- 모든 결제 트랜잭션은 `payments` 테이블에 기록:
  - `transaction_id` (PG사 거래 번호), `status` (SUCCESS/FAILED/REFUNDED), `amount`, `method_type`, `created_at`

#### 2-3. 결제 후 처리
- 결제 성공 시:
  1. 주문 상태 `PENDING` → `PAID` 전환
  2. 재고 차감 이벤트 발행
  3. 쿠폰 사용 처리
  4. 포인트 차감 처리
  5. 장바구니 정리 (결제된 아이템 삭제)
  6. 배송 엔티티 생성 (`PREPARING` 상태)
- 결제 실패 시:
  1. 주문 상태 `PENDING` → `CANCELED` 전환
  2. 모든 예약된 리소스(쿠폰, 포인트) 원복

---

### 3. 배송 트래킹 (Deliveries)

#### 3-1. 배송 생성
- 결제 완료(`PAID`) 시 자동으로 `deliveries` 엔티티가 1:1로 생성
- 초기 상태: `PREPARING` (상품 준비 중)

#### 3-2. 배송 상태 머신
| 상태 | 설명 | 전이 가능 상태 |
|------|------|---------------|
| `PREPARING` | 상품 준비 중 | → `SHIPPED` |
| `SHIPPED` | 배송 출발 (운송장 번호 부여) | → `IN_TRANSIT` |
| `IN_TRANSIT` | 배송 중 | → `DELIVERED` |
| `DELIVERED` | 배송 완료 | 최종 상태 |

- 상태 전이 시 `delivery_status_updated_at` 타임스탬프 갱신
- `SHIPPED` 전이 시 운송장 번호(`tracking_number`) 필수 기록
- 잘못된 상태 전이 시도 시 `INVALID_DELIVERY_STATUS_TRANSITION` (400)

#### 3-3. 배송 완료 후 트리거
- `DELIVERED` 상태 도달 시 다음 이벤트를 비동기 발행:
  - 포인트 적립 이벤트 (최종 결제 금액의 1%)
  - 리뷰 작성 가능 상태 활성화

---

## [P6] Infrastructure (Outbox & Saga Pattern)

### 1. Outbox 메시징

#### 1-1. Outbox 테이블 구조
| 컬럼 | 설명 |
|------|------|
| `id` | PK (UUID) |
| `aggregate_type` | 이벤트 발생 애그리거트 (예: `ORDER`, `PAYMENT`) |
| `aggregate_id` | 애그리거트 ID |
| `event_type` | 이벤트 타입 (예: `OrderCreatedEvent`, `PaymentCompletedEvent`) |
| `payload` | 이벤트 JSON 페이로드 |
| `status` | `PENDING` / `PUBLISHED` / `FAILED` |
| `created_at` | 생성 시각 |
| `published_at` | 발행 완료 시각 |
| `retry_count` | 재시도 횟수 |

#### 1-2. 이벤트 기록 규칙
- 비즈니스 트랜잭션과 **동일한 DB 트랜잭션** 내에서 Outbox 레코드를 INSERT
  - 예: 주문 생성 + Outbox INSERT = 하나의 트랜잭션
- 이를 통해 "비즈니스 데이터는 저장됐지만 이벤트는 유실" 되는 상황을 원천 차단
- 다른 모듈의 Bean을 직접 주입받아 호출하는 것을 금지 (Spring Modulith 모듈 경계 준수)

---

### 2. 스케줄러 폴링 (Polling Publisher)

#### 2-1. 폴링 메커니즘
- `@Scheduled(fixedDelay = 5000)`: 5초 간격으로 미처리 이벤트 폴링
- 조회 조건: `status = PENDING AND retry_count < 5`
- 배치 사이즈: 최대 100건 단위로 처리

#### 2-2. 이벤트 발행 플로우
```
1. PENDING 이벤트 조회 (배치)
2. 각 이벤트에 대해 Spring ApplicationEventPublisher.publishEvent() 호출
3. 발행 성공 시: status → PUBLISHED, published_at 기록
4. 발행 실패 시: retry_count++, 5회 초과 시 status → FAILED (알림/로깅)
```

#### 2-3. 멱등성 보장
- 이벤트 소비자(Consumer)는 `event_id`를 기반으로 중복 처리를 방지
- 이미 처리된 이벤트 ID는 무시하도록 구현

---

### 3. Saga (보상 트랜잭션)

#### 3-1. 주문-결제-재고 Saga 시나리오
```
정상 플로우:
  OrderCreatedEvent → 결제 처리 → PaymentCompletedEvent → 재고 차감 → StockDeductedEvent → 주문 확정

보상 플로우 (재고 부족):
  StockDeductionFailedEvent 발행
    → 결제 모듈: 환불 API 호출 (PaymentRefundedEvent 발행)
    → 주문 모듈: 주문 상태 CANCELED 전환 (OrderCanceledEvent 발행)
    → 쿠폰 모듈: 사용된 쿠폰 복원
    → 포인트 모듈: 사용된 포인트 복원

보상 플로우 (결제 실패):
  PaymentFailedEvent 발행
    → 주문 모듈: 주문 상태 CANCELED 전환
```

#### 3-2. 보상 트랜잭션 원칙
- 각 보상 단계는 독립적인 트랜잭션으로 실행
- 보상 자체가 실패할 경우 재시도 + 로깅 (최대 3회)
- 3회 재시도 후에도 실패 시 `COMPENSATION_FAILED` 상태로 기록하고 관리자 알림

#### 3-3. 이벤트 목록

| 이벤트 | 발행 주체 | 소비 주체 | 설명 |
|--------|----------|----------|------|
| `OrderCreatedEvent` | Order | Payment | 주문 생성 → 결제 요청 |
| `PaymentCompletedEvent` | Payment | Inventory | 결제 성공 → 재고 차감 요청 |
| `PaymentFailedEvent` | Payment | Order | 결제 실패 → 주문 취소 |
| `StockDeductedEvent` | Inventory | Order | 재고 차감 성공 → 주문 확정 |
| `StockDeductionFailedEvent` | Inventory | Payment, Order | 재고 부족 → 보상 트랜잭션 |
| `PaymentRefundedEvent` | Payment | Order | 환불 완료 알림 |
| `OrderCanceledEvent` | Order | Coupon, Point | 주문 취소 → 쿠폰/포인트 복원 |
| `DeliveryCompletedEvent` | Delivery | Point, Review | 배송 완료 → 포인트 적립, 리뷰 작성 활성화 |

---

## 공통 기술 요구사항

### API 응답 형식
```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "INSUFFICIENT_POINT",
    "message": "포인트 잔액이 부족합니다.",
    "details": { "required": 5000, "available": 3000 }
  }
}
```

### 페이지네이션 응답 형식
```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "hasNext": true
  }
}
```

### 공통 유효성 검증
- 모든 요청 DTO에 Bean Validation (`@Valid`, `@NotNull`, `@Size` 등) 적용
- 유효성 검증 실패 시 `VALIDATION_ERROR` (400) + 필드별 에러 메시지 목록 반환

### 감사 로깅 (Audit)
- 모든 엔티티에 `created_at`, `updated_at` 자동 기록 (`@EnableJpaAuditing`)
- 주요 변경 이력 (주문 상태 변경, 포인트 변동, 배송 상태 변경) 은 별도 히스토리 테이블에 기록
