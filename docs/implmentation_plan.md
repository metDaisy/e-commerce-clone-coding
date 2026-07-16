# [Goal Description]
Amazon 클론 이커머스 백엔드 시스템 구축.
단일 Spring Boot 애플리케이션 안에서 도메인 간 결합도를 낮춘 **모듈러 모놀리스(Modular Monolith)** 아키텍처를 채택하며, 향후 MSA 전환을 대비해 **Outbox 패턴과 Saga 패턴(보상 트랜잭션)**을 스프링 내부 이벤트 메커니즘으로 모의 구현합니다.

## 📅 12일 압축 개발 일정 (12-Day Sprint Milestone)
기존 14일 일정을 12일로 압축하여, 코어 로직의 동작 보장에 집중합니다.
* **Day 1~2**: P1 기반 시스템 (User/Auth) 설계 및 API 구현
* **Day 3~4**: P2 전시 도메인 (Catalog/Inventory) 구현
* **Day 5~6**: P3 & P4 쇼핑 준비 (Cart/Coupon) 구현
* **Day 7~9**: P5 핵심 결제 트랜잭션 (Order/Payment/Delivery) 구현
* **Day 10~11**: P6 아키텍처 고도화 (Infrastructure/Outbox/Saga)
* **Day 12**: E2E 통합 테스트 및 치명적 버그 수정

---

## 📌 도메인별 세부 기능 명세 (P1 ~ P6)

### [P1] User & Auth 도메인 (기반 시스템)
사용자의 개인정보와 인증 메커니즘을 분리하여 확장성을 확보합니다.
* **사용자(users)**: 회원 기본 정보(이름, 연락처), 권한(USER, ADMIN, PRODUCT_MANAGER), 주소록(addresses) 관리
* **인증(user_credentials)**: 로컬 로그인 및 OAuth2(구글, 카카오) 소셜 로그인 동시 지원
* **관심상품(wishlists)**: 사용자가 특정 상품을 '찜'하는 기능
* **포인트(point_histories)**: 현금처럼 결제에 사용할 수 있는 사용자 포인트 시스템의 잔액 관리 및 변동 내역 추적

### [P2] Catalog & Inventory 도메인 (전시 시스템)
구매할 대상이 되는 상품과 카테고리를 관리합니다.
* **카테고리(categories)**: 대분류/중분류 등 계층형 구조
* **상품(products)**: 상품 기본 정보, 정상가, 상태(판매중, 품절 등), 다대다 태그 매핑(tags)
* **다형성 이미지(images)**: 상품 썸네일, 리뷰 첨부사진 등 다양한 도메인에서 공통으로 사용할 수 있는 이미지 테이블
* **리뷰(reviews)**: 상품에 대한 사용자의 평점 및 다중 이미지 리뷰 기능
* **재고 및 타임세일**: 실시간 재고(stock_quantity) 관리 및 지정된 기간 내 할인(sale_price) 적용 로직

### [P3] Cart 도메인 (장바구니)
유저가 결제 전 상품을 모아두는 기능입니다.
* **장바구니(carts/cart_items)**: 유저 1명당 1개의 활성화된 카트를 부여하고, 담긴 상품의 수량 조절 및 삭제 기능

### [P4] Coupon 도메인 (혜택 시스템)
결제 금액을 할인해 주는 수단입니다.
* **쿠폰 마스터(coupons)**: 어드민이 생성하는 쿠폰의 기본 속성(정률/정액 할인, 유효기간)
* **유저 쿠폰(user_coupons)**: 사용자가 발급받아 소유한 쿠폰. 스케줄러를 통한 만료일 검사 로직 적용

### [P5] Order & Payment 도메인 (핵심 트랜잭션)
장바구니의 물건을 실제 돈(또는 포인트/쿠폰)을 지불하고 구매하는 가장 중요한 단계입니다.
* **주문(orders)**: 주문 상태(PENDING, PAID 등) 관리. 장바구니 데이터를 읽어와 원가 계산 후, 쿠폰과 포인트를 복합 적용하여 **최종 결제 금액**을 산출하는 핵심 비즈니스 로직
* **배송(deliveries)**: 결제 완료 후 트리거되어 운송장 번호 및 배송 상태(PREPARING 등) 트래킹
* **결제(payment_methods/payments)**: 다형성(Strategy Pattern) 구조를 이용한 결제 수단 추상화 및 PG사 연동을 가정한 모의 결제 트랜잭션 기록

### [P6] Infrastructure (Outbox & Saga)
모놀리스의 결합도를 끊어내기 위한 아키텍처 패턴을 구현합니다.
* **Outbox 테이블**: 주문 트랜잭션 발생 시 같은 DB 테이블 내에 '이벤트 JSON'을 기록
* **Polling 스케줄러**: Spring `@Scheduled`를 이용해 발행되지 않은 이벤트를 긁어와 비동기 발행
* **Saga 패턴 (보상 트랜잭션)**: 재고가 부족하거나 결제가 실패했을 때 이벤트를 수신받아 이전에 생성되었던 주문(Order)을 자동으로 '취소(CANCELED)' 시키는 로직

---

## 🛠 아키텍처 및 기술 스택
- **언어 및 프레임워크**: Java 17, Spring Boot 3.5.15
- **설계 구조**: Spring Modulith를 통한 도메인 패키지 격리 적용
- **데이터베이스**: PostgreSQL 16 (Testcontainers 연동)
- **CI/CD**: GitHub Actions (Jacoco 커버리지 업로드 및 Test Reporter), Codecov, CodeRabbit 연동
- **API 및 통신**: REST API 및 타임세일 재고 브로드캐스팅용 WebSocket

---
