# E-commerce-clone-coding

[![CI Pipeline](https://github.com/metDaisy/e-commerce-clone-coding/actions/workflows/ci.yml/badge.svg)](https://github.com/metDaisy/e-commerce-clone-coding/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/metDaisy/e-commerce-clone-coding/graph/badge.svg?token=qUz12GJ8C4)](https://codecov.io/gh/metDaisy/e-commerce-clone-coding)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.16-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue.svg)](https://www.postgresql.org/)

요구사항과 구현 문서는 [문서 인덱스](./docs/index.md)에서 확인할 수 있습니다. 기본 데이터베이스 스키마는 [`V1__init_schema.sql`](./src/main/resources/db/migration/V1__init_schema.sql)에 정의되어 있습니다.

## 기본 스키마

심화사항을 제외한 P1~P8 요구사항의 테이블을 영역별로 나누었습니다. 모든 식별자는 UUID입니다. 모듈 간 식별자에는 DB FK를 만들지 않고 애플리케이션 계약으로 검증합니다.

<details>
<summary>P1 User & Auth</summary>

```mermaid
erDiagram
    users {
        UUID id PK
        VARCHAR name
        VARCHAR phone_number
        VARCHAR role
        INTEGER point_balance
        BOOLEAN is_active
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }
    user_credentials {
        UUID user_id PK
        VARCHAR email UK
        VARCHAR password
        INTEGER violation_count
        TIMESTAMPTZ until_locked
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }
    social_credentials {
        UUID id PK
        UUID user_id
        VARCHAR provider
        VARCHAR provider_id
        TIMESTAMPTZ created_at
    }
    addresses {
        UUID id PK
        UUID user_id
        VARCHAR recipient_name
        VARCHAR recipient_phone
        VARCHAR postal_code
        VARCHAR address_line
        BOOLEAN is_primary
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }
    point_histories {
        UUID id PK
        UUID user_id
        INTEGER amount
        VARCHAR description
        TIMESTAMPTZ created_at
    }
    wishlists {
        UUID id PK
        UUID user_id
        UUID product_id
        TIMESTAMPTZ created_at
    }
    refresh_tokens {
        UUID id PK
        UUID user_id
        VARCHAR device_id
        VARCHAR token
        VARCHAR pre_token
        TIMESTAMPTZ expired_at
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }
    blacklist_tokens {
        UUID id PK
        VARCHAR token
        TIMESTAMPTZ expired_at
        TIMESTAMPTZ created_at
    }
    blacklist_users {
        UUID id PK
        UUID user_id
        TIMESTAMPTZ compromised_at
        TIMESTAMPTZ created_at
    }

    users ||--o| user_credentials : local_login
    users ||--o{ social_credentials : social_login
    users ||--o{ addresses : owns
    users ||--o{ point_histories : records
    users ||--o{ wishlists : likes
```
</details>

<details>
<summary>P2 Catalog & Inventory</summary>

```mermaid
erDiagram
    categories {
        UUID id PK
        UUID parent_id FK
        VARCHAR name
        INTEGER depth "root=1, maximum=3"
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }
    products {
        UUID id PK
        UUID category_id FK
        UUID manager_id
        VARCHAR name
        TEXT description
        VARCHAR brand
        VARCHAR asin
        VARCHAR gtin
        VARCHAR upc
        VARCHAR ean
        VARCHAR isbn
        JSONB attributes
        VARCHAR publication_status
        TIMESTAMPTZ archived_at
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }
    product_variants {
        UUID id PK
        UUID product_id FK
        VARCHAR sku UK
        VARCHAR display_name
        NUMERIC weight
        JSONB dimensions
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }
    offers {
        UUID id PK
        UUID variant_id FK
        UUID seller_id
        NUMERIC base_price_amount
        NUMERIC discount_price_amount
        CHAR currency
        TIMESTAMPTZ discount_start_at
        TIMESTAMPTZ discount_end_at
        VARCHAR status
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }
    inventories {
        UUID id PK
        UUID offer_id FK
        INTEGER quantity
        TIMESTAMPTZ updated_at
    }
    images {
        UUID id PK
        VARCHAR entity_type
        UUID entity_id
        VARCHAR media_type
        VARCHAR image_url
        BOOLEAN is_primary
        INTEGER sort_order
        TIMESTAMPTZ created_at
    }
    tags {
        UUID id PK
        VARCHAR name UK
        TIMESTAMPTZ created_at
    }
    product_tags {
        UUID id PK
        UUID product_id FK
        UUID tag_id FK
        TIMESTAMPTZ created_at
    }
    reviews {
        UUID id PK
        UUID product_id
        UUID user_id
        INTEGER rating
        VARCHAR content
        TIMESTAMPTZ created_at
    }

    categories o|--o{ categories : parent_of
    categories ||--o{ products : contains
    products ||--o{ product_variants : has
    product_variants ||--o{ offers : has
    offers ||--|| inventories : stocks
    products ||--o{ product_tags : tagged
    tags ||--o{ product_tags : assigned
```
</details>

<details>
<summary>P3 Cart</summary>

```mermaid
erDiagram
    carts {
        UUID id PK
        UUID user_id
        VARCHAR status
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }
    cart_items {
        UUID id PK
        UUID cart_id FK
        UUID offer_id
        INTEGER quantity
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    carts ||--o{ cart_items : contains
```
</details>

<details>
<summary>P4 Coupon</summary>

```mermaid
erDiagram
    coupons {
        UUID id PK
        VARCHAR name
        VARCHAR discount_type
        NUMERIC discount_value
        NUMERIC max_discount_amount
        NUMERIC minimum_order_amount
        UUID applicable_category_id
        INTEGER total_quantity
        INTEGER issued_quantity
        VARCHAR status
        TIMESTAMPTZ valid_from
        TIMESTAMPTZ valid_until
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }
    user_coupons {
        UUID id PK
        UUID user_id
        UUID coupon_id FK
        VARCHAR status
        TIMESTAMPTZ used_at
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    coupons ||--o{ user_coupons : issued
```
</details>

<details>
<summary>P5 Order, Payment & Delivery</summary>

```mermaid
erDiagram
    orders {
        UUID id PK
        VARCHAR order_number UK
        UUID user_id
        UUID address_id
        UUID used_user_coupon_id
        NUMERIC used_point_amount
        VARCHAR status
        NUMERIC subtotal_amount
        NUMERIC discount_amount
        NUMERIC shipping_fee
        NUMERIC paid_amount
        CHAR currency
        VARCHAR shipping_recipient_name
        VARCHAR shipping_recipient_phone
        VARCHAR shipping_postal_code
        VARCHAR shipping_address_line
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }
    order_items {
        UUID id PK
        UUID order_id FK
        UUID product_id
        UUID variant_id
        UUID offer_id
        VARCHAR sku
        VARCHAR product_name
        INTEGER quantity
        NUMERIC unit_price
        NUMERIC subtotal
        CHAR currency
        TIMESTAMPTZ created_at
    }
    payment_methods {
        UUID id PK
        UUID user_id
        VARCHAR method_type
        VARCHAR provider
        VARCHAR masked_number
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }
    payments {
        UUID id PK
        UUID order_id FK
        UUID payment_method_id FK
        VARCHAR transaction_id UK
        VARCHAR status
        NUMERIC amount
        CHAR currency
        TIMESTAMPTZ paid_at
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }
    deliveries {
        UUID id PK
        UUID order_id FK
        VARCHAR status
        VARCHAR tracking_number
        TIMESTAMPTZ delivery_status_updated_at
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    orders ||--o{ order_items : contains
    orders ||--o{ payments : pays
    payment_methods ||--o{ payments : used_for
    orders ||--o| deliveries : ships
```
</details>

<details>
<summary>P6 Outbox & Saga</summary>

```mermaid
erDiagram
    outbox_events {
        UUID id PK
        VARCHAR aggregate_type
        UUID aggregate_id
        VARCHAR event_type
        JSONB payload
        VARCHAR status
        INTEGER retry_count
        TIMESTAMPTZ created_at
        TIMESTAMPTZ published_at
        TEXT last_error
    }
    event_consumptions {
        UUID id PK
        UUID event_id FK
        VARCHAR consumer
        VARCHAR status
        TIMESTAMPTZ processed_at
        TIMESTAMPTZ created_at
    }
    saga_instances {
        UUID id PK
        UUID order_id
        VARCHAR status
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }
    saga_steps {
        UUID id PK
        UUID saga_id FK
        VARCHAR name
        VARCHAR status
        INTEGER retry_count
        TIMESTAMPTZ completed_at
        TEXT last_error
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }
    event_publication {
        UUID id PK
        INTEGER completion_attempts
        TIMESTAMPTZ completion_date
        VARCHAR event_type
        TIMESTAMPTZ last_resubmission_date
        VARCHAR listener_id
        TIMESTAMPTZ publication_date
        TEXT serialized_event
        VARCHAR status
    }

    outbox_events ||--o{ event_consumptions : consumed_by
    saga_instances ||--o{ saga_steps : contains
```
</details>

<details>
<summary>P7 Admin & Operations</summary>

P7은 별도 업무 데이터를 소유하지 않습니다. P2~P6의 공개 application interface를 통해 관리자 전용 기능과 운영·재처리를 제공합니다.
</details>

<details>
<summary>P8 Seller & Marketplace</summary>

```mermaid
erDiagram
    seller_profiles {
        UUID id PK
        UUID user_id
        VARCHAR display_name
        VARCHAR business_name
        VARCHAR business_registration_hash
        VARCHAR contact_email
        VARCHAR contact_phone
        VARCHAR status
        UUID reviewed_by
        TIMESTAMPTZ reviewed_at
        VARCHAR review_reason
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

```
</details>
`products.manager_id`는 상품을 등록·관리한 `users.id`를 가리키는 모듈 간 논리 참조입니다. `offers.seller_id`는 Offer를 소유한 P8의 `seller_profiles.id`를 가리키며, 플랫폼 기본 Offer에서는 `NULL`입니다. 두 값 모두 모듈 간 논리 참조이므로 DB FK는 생성하지 않습니다.
