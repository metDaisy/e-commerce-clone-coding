-- P1-P8 기본 요구사항 스키마.
--
-- 모든 식별자는 UUID를 사용한다. 도메인 간 식별자는 의도적으로 데이터베이스 외래 키를
-- 생성하지 않으며, 동일 도메인 내부 관계만 외래 키로 보장한다.
-- 자세한 내용은 docs/adr/0004-schema-fk-follows-modulith-boundaries.md를 참고한다.

-- ============================================================================
-- P1: 사용자·인증
-- ============================================================================

CREATE TABLE users
(
    id            UUID PRIMARY KEY,
    name          VARCHAR(100)             NOT NULL,
    phone_number  VARCHAR(20),
    role          VARCHAR(30)              NOT NULL DEFAULT 'USER',
    point_balance INTEGER                  NOT NULL DEFAULT 0,
    is_active     BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'PRODUCT_MANAGER', 'ADMIN')),
    CONSTRAINT chk_users_point_balance CHECK (point_balance >= 0)
);

COMMENT ON COLUMN users.role IS
    'USER: 기본 구매자, PRODUCT_MANAGER: 판매자, ADMIN: 플랫폼 운영자. PRODUCT_MANAGER도 구매자 API를 사용할 수 있다.';

CREATE TABLE user_credentials
(
    user_id         UUID PRIMARY KEY,
    email           VARCHAR(255)             NOT NULL,
    password_hash   VARCHAR(255)             NOT NULL,
    violation_count INTEGER                  NOT NULL DEFAULT 0,
    until_locked    TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

COMMENT ON TABLE user_credentials IS
    '이메일·비밀번호 기반의 로컬 인증 정보만 저장한다. 소셜 회원가입 시에는 행을 생성하지 않는다.';

CREATE TABLE social_credentials
(
    id          UUID PRIMARY KEY,
    user_id     UUID                     NOT NULL,
    provider    VARCHAR(20)              NOT NULL,
    provider_id VARCHAR(255)             NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_social_credentials_provider
        CHECK (provider IN ('GOOGLE', 'NAVER', 'KAKAO', 'GITHUB'))
);

COMMENT ON TABLE social_credentials IS
    'OAuth 소셜 인증 정보. provider 열거값은 Amazon.com이 아니라 이 애플리케이션이 임의로 정의한다.';

CREATE TABLE addresses
(
    id              UUID PRIMARY KEY,
    user_id         UUID                     NOT NULL,
    recipient_name  VARCHAR(100)             NOT NULL,
    recipient_phone VARCHAR(20)              NOT NULL,
    postal_code     VARCHAR(20)              NOT NULL,
    address_line    VARCHAR(255)             NOT NULL,
    is_primary      BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE point_histories
(
    id          UUID PRIMARY KEY,
    user_id     UUID                     NOT NULL,
    amount      INTEGER                  NOT NULL,
    description VARCHAR(255)             NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE wishlists
(
    id         UUID PRIMARY KEY,
    user_id    UUID                     NOT NULL,
    variant_id UUID                     NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE refresh_tokens
(
    id         UUID PRIMARY KEY,
    user_id    UUID                     NOT NULL,
    device_id  VARCHAR(100)             NOT NULL,
    token      VARCHAR(255)             NOT NULL,
    pre_token  VARCHAR(255),
    expired_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE blacklist_tokens
(
    id         UUID PRIMARY KEY,
    token      VARCHAR(255)             NOT NULL,
    expired_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE blacklist_users
(
    id             UUID PRIMARY KEY,
    user_id        UUID                     NOT NULL,
    compromised_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

-- ============================================================================
-- P2: 카탈로그·재고·리뷰
-- ============================================================================

CREATE TABLE categories
(
    id         UUID PRIMARY KEY,
    parent_id  UUID,
    name       VARCHAR(255)             NOT NULL,
    depth      INTEGER                  NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_categories_parent
        FOREIGN KEY (parent_id) REFERENCES categories (id),
    CONSTRAINT chk_categories_depth CHECK (depth BETWEEN 1 AND 3)
);

-- catalog_products는 CatalogProduct 집합을 나타낸다.
CREATE TABLE catalog_products
(
    id                 UUID PRIMARY KEY,
    category_id        UUID                     NOT NULL,
    manager_id         UUID                     NOT NULL,
    name               VARCHAR(255)             NOT NULL,
    description        TEXT                     NOT NULL,
    brand              VARCHAR(255),
    asin               VARCHAR(50),
    gtin               VARCHAR(50),
    upc                VARCHAR(50),
    ean                VARCHAR(50),
    isbn               VARCHAR(50),
    attributes         JSONB,
    publication_status VARCHAR(20)              NOT NULL DEFAULT 'ACTIVE',
    archived_at        TIMESTAMP WITH TIME ZONE,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_catalog_products_category
        FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT chk_catalog_products_publication_status
        CHECK (publication_status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT chk_catalog_products_archive_consistency
        CHECK ((publication_status = 'ACTIVE' AND archived_at IS NULL) OR
               (publication_status = 'ARCHIVED' AND archived_at IS NOT NULL))
);

COMMENT ON TABLE catalog_products IS
    '공통 카탈로그 정보를 소유하는 CatalogProduct 집합. 판매자별 판매 조건은 offers가 소유한다.';

COMMENT ON COLUMN catalog_products.manager_id IS
    '카탈로그 상품을 등록·관리한 users.id를 논리적으로 참조한다. PRODUCT_MANAGER는 자신의 상품을 관리하고 ADMIN은 모든 상품을 관리할 수 있다.';

COMMENT ON COLUMN catalog_products.asin IS
    '선택적인 외부 상품 식별자이며 기본 키로 사용하지 않는다.';
COMMENT ON COLUMN catalog_products.gtin IS
    '선택적인 외부 상품 식별자이며 기본 키로 사용하지 않는다.';
COMMENT ON COLUMN catalog_products.upc IS
    '선택적인 외부 상품 식별자이며 기본 키로 사용하지 않는다.';
COMMENT ON COLUMN catalog_products.ean IS
    '선택적인 외부 상품 식별자이며 기본 키로 사용하지 않는다.';
COMMENT ON COLUMN catalog_products.isbn IS
    '선택적인 외부 상품 식별자이며 기본 키로 사용하지 않는다.';

CREATE TABLE product_variants
(
    id                 UUID PRIMARY KEY,
    catalog_product_id UUID                     NOT NULL,
    sku                VARCHAR(100)             NOT NULL,
    display_name       VARCHAR(255)             NOT NULL,
    weight             NUMERIC(10, 3),
    dimensions         JSONB,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_product_variants_catalog_product
        FOREIGN KEY (catalog_product_id) REFERENCES catalog_products (id)
);

COMMENT ON TABLE product_variants IS
    'CatalogProduct 아래에서 실제 구매 가능한 SKU를 나타내는 ProductVariant.';

CREATE TABLE offers
(
    id                    UUID PRIMARY KEY,
    variant_id            UUID                     NOT NULL,
    seller_id             UUID,
    base_price_amount     NUMERIC(19, 2)           NOT NULL,
    discount_price_amount NUMERIC(19, 2),
    currency              CHAR(3)                  NOT NULL,
    discount_start_at     TIMESTAMP WITH TIME ZONE,
    discount_end_at       TIMESTAMP WITH TIME ZONE,
    status                VARCHAR(20)              NOT NULL DEFAULT 'ACTIVE',
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_offers_variant
        FOREIGN KEY (variant_id) REFERENCES product_variants (id),
    CONSTRAINT chk_offers_price
        CHECK (base_price_amount >= 0 AND
               (discount_price_amount IS NULL OR
                (discount_price_amount >= 0 AND discount_price_amount <= base_price_amount))),
    CONSTRAINT chk_offers_discount_period
        CHECK (discount_start_at IS NULL OR discount_end_at IS NULL OR
               discount_start_at < discount_end_at),
    CONSTRAINT chk_offers_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

COMMENT ON TABLE offers IS
    'ProductVariant에 대한 판매자별 판매 조건을 나타내는 Offer.';

COMMENT ON COLUMN offers.seller_id IS
    'Offer 소유자인 sellers.id를 논리적으로 참조한다. NULL이면 ADMIN이 생성한 플랫폼 기본 Offer다.';

CREATE TABLE inventories
(
    id         UUID PRIMARY KEY,
    offer_id   UUID                     NOT NULL,
    quantity   INTEGER                  NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_inventories_offer UNIQUE (offer_id),
    CONSTRAINT chk_inventories_quantity CHECK (quantity >= 0),
    CONSTRAINT fk_inventories_offer
        FOREIGN KEY (offer_id) REFERENCES offers (id)
);

COMMENT ON TABLE inventories IS
    'Offer의 구매 가능 수량을 저장하는 Inventory.';

CREATE TABLE images
(
    id          UUID PRIMARY KEY,
    entity_type VARCHAR(30)              NOT NULL,
    entity_id   UUID                     NOT NULL,
    media_type  VARCHAR(20)              NOT NULL DEFAULT 'IMAGE',
    image_url   VARCHAR(1000)            NOT NULL,
    is_primary  BOOLEAN                  NOT NULL DEFAULT FALSE,
    sort_order  INTEGER                  NOT NULL DEFAULT 0,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_images_media_type CHECK (media_type = 'IMAGE'),
    CONSTRAINT chk_images_sort_order CHECK (sort_order >= 0)
);

CREATE TABLE tags
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(100)             NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE catalog_product_tags
(
    id                 UUID PRIMARY KEY,
    catalog_product_id UUID                     NOT NULL,
    tag_id             UUID                     NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_catalog_product_tags_catalog_product
        FOREIGN KEY (catalog_product_id) REFERENCES catalog_products (id) ON DELETE CASCADE,
    CONSTRAINT fk_catalog_product_tags_tag
        FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
);

CREATE TABLE reviews
(
    id         UUID PRIMARY KEY,
    variant_id UUID                     NOT NULL,
    user_id    UUID                     NOT NULL,
    rating     INTEGER                  NOT NULL,
    content    VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_reviews_variant
        FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE CASCADE,
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);

-- ============================================================================
-- P3: 장바구니
-- ============================================================================

CREATE TABLE carts
(
    id         UUID PRIMARY KEY,
    user_id    UUID                     NOT NULL,
    status     VARCHAR(20)              NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_carts_status CHECK (status IN ('ACTIVE', 'CHECKED_OUT'))
);

CREATE TABLE cart_items
(
    id         UUID PRIMARY KEY,
    cart_id    UUID                     NOT NULL,
    offer_id   UUID                     NOT NULL,
    quantity   INTEGER                  NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_cart_items_cart
        FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE,
    CONSTRAINT chk_cart_items_quantity CHECK (quantity BETWEEN 1 AND 10)
);

-- ============================================================================
-- P4: 쿠폰
-- ============================================================================

CREATE TABLE coupons
(
    id                   UUID PRIMARY KEY,
    name                 VARCHAR(255)             NOT NULL,
    discount_type        VARCHAR(20)              NOT NULL,
    discount_value       NUMERIC(19, 2)           NOT NULL,
    max_discount_amount  NUMERIC(19, 2),
    minimum_order_amount NUMERIC(19, 2)           NOT NULL DEFAULT 0,
    currency             CHAR(3)                  NOT NULL,
    total_quantity       INTEGER,
    issued_quantity      INTEGER                  NOT NULL DEFAULT 0,
    status               VARCHAR(20)              NOT NULL DEFAULT 'ACTIVE',
    valid_from           TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until          TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_coupons_discount_type
        CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    CONSTRAINT chk_coupons_discount_value CHECK (discount_value > 0),
    CONSTRAINT chk_coupons_percentage
        CHECK (discount_type <> 'PERCENTAGE' OR discount_value <= 100),
    CONSTRAINT chk_coupons_amounts
        CHECK (minimum_order_amount >= 0 AND
               (max_discount_amount IS NULL OR max_discount_amount > 0)),
    CONSTRAINT chk_coupons_quantity
        CHECK (issued_quantity >= 0 AND
               (total_quantity IS NULL OR
                (total_quantity >= 0 AND issued_quantity <= total_quantity))),
    CONSTRAINT chk_coupons_period CHECK (valid_from < valid_until),
    CONSTRAINT chk_coupons_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE user_coupons
(
    id         UUID PRIMARY KEY,
    user_id    UUID                     NOT NULL,
    coupon_id  UUID                     NOT NULL,
    status     VARCHAR(20)              NOT NULL DEFAULT 'AVAILABLE',
    used_at    TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_user_coupons_coupon
        FOREIGN KEY (coupon_id) REFERENCES coupons (id),
    CONSTRAINT chk_user_coupons_status
        CHECK (status IN ('AVAILABLE', 'USED', 'EXPIRED'))
);

-- ============================================================================
-- P5: 주문·결제·배송
-- ============================================================================

CREATE TABLE orders
(
    id                       UUID PRIMARY KEY,
    order_number             VARCHAR(40)              NOT NULL,
    user_id                  UUID                     NOT NULL,
    address_id               UUID                     NOT NULL,
    used_user_coupon_id      UUID,
    used_point_amount        INTEGER                  NOT NULL DEFAULT 0,
    status                   VARCHAR(20)              NOT NULL DEFAULT 'PENDING',
    subtotal_amount          NUMERIC(19, 2)           NOT NULL,
    discount_amount          NUMERIC(19, 2)           NOT NULL DEFAULT 0,
    shipping_fee             NUMERIC(19, 2)           NOT NULL DEFAULT 0,
    paid_amount              NUMERIC(19, 2)           NOT NULL,
    currency                 CHAR(3)                  NOT NULL,
    shipping_recipient_name  VARCHAR(100)             NOT NULL,
    shipping_recipient_phone VARCHAR(20)              NOT NULL,
    shipping_postal_code     VARCHAR(20)              NOT NULL,
    shipping_address_line    VARCHAR(255)             NOT NULL,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_orders_status CHECK (status IN ('PENDING', 'PAID', 'CANCELED')),
    CONSTRAINT chk_orders_amounts CHECK (subtotal_amount >= 0 AND discount_amount >= 0 AND
                                         shipping_fee >= 0 AND used_point_amount >= 0 AND
                                         paid_amount >= 0)
);

CREATE TABLE order_items
(
    id                   UUID PRIMARY KEY,
    order_id             UUID                     NOT NULL,
    catalog_product_id   UUID                     NOT NULL,
    variant_id           UUID                     NOT NULL,
    offer_id             UUID                     NOT NULL,
    sku                  VARCHAR(100)             NOT NULL,
    catalog_product_name VARCHAR(255)             NOT NULL,
    quantity             INTEGER                  NOT NULL,
    unit_price           NUMERIC(19, 2)           NOT NULL,
    subtotal             NUMERIC(19, 2)           NOT NULL,
    currency             CHAR(3)                  NOT NULL,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_items_amounts CHECK (unit_price >= 0 AND subtotal >= 0)
);

CREATE TABLE payment_methods
(
    id            UUID PRIMARY KEY,
    user_id       UUID                     NOT NULL,
    method_type   VARCHAR(30)              NOT NULL,
    provider      VARCHAR(100),
    masked_number VARCHAR(50),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_payment_methods_type
        CHECK (method_type IN ('CREDIT_CARD', 'KAKAO_PAY', 'BANK_TRANSFER'))
);

CREATE TABLE payments
(
    id                UUID PRIMARY KEY,
    order_id          UUID                     NOT NULL,
    payment_method_id UUID                     NOT NULL,
    transaction_id    VARCHAR(255)             NOT NULL,
    status            VARCHAR(20)              NOT NULL,
    amount            NUMERIC(19, 2)           NOT NULL,
    currency          CHAR(3)                  NOT NULL,
    paid_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_payments_order
        FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_payments_method
        FOREIGN KEY (payment_method_id) REFERENCES payment_methods (id),
    CONSTRAINT chk_payments_status CHECK (status IN ('SUCCESS', 'FAILED', 'REFUNDED')),
    CONSTRAINT chk_payments_amount CHECK (amount >= 0)
);

CREATE TABLE deliveries
(
    id                         UUID PRIMARY KEY,
    order_id                   UUID                     NOT NULL,
    status                     VARCHAR(20)              NOT NULL DEFAULT 'PREPARING',
    tracking_number            VARCHAR(100),
    delivery_status_updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at                 TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                 TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_deliveries_order
        FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT uq_deliveries_order UNIQUE (order_id),
    CONSTRAINT chk_deliveries_status
        CHECK (status IN ('PREPARING', 'SHIPPED', 'IN_TRANSIT', 'DELIVERED')),
    CONSTRAINT chk_deliveries_tracking
        CHECK (status NOT IN ('SHIPPED', 'IN_TRANSIT', 'DELIVERED') OR tracking_number IS NOT NULL)
);

-- ============================================================================
-- P6: 아웃박스·멱등 소비·사가
-- ============================================================================

CREATE TABLE outbox_events
(
    id             UUID PRIMARY KEY,
    aggregate_type VARCHAR(30)              NOT NULL,
    aggregate_id   UUID                     NOT NULL,
    event_type     VARCHAR(255)             NOT NULL,
    payload        JSONB                    NOT NULL,
    status         VARCHAR(20)              NOT NULL DEFAULT 'PENDING',
    retry_count    INTEGER                  NOT NULL DEFAULT 0,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at   TIMESTAMP WITH TIME ZONE,
    last_error     TEXT,
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT chk_outbox_retry_count CHECK (retry_count >= 0)
);

CREATE TABLE event_consumptions
(
    id           UUID PRIMARY KEY,
    event_id     UUID                     NOT NULL,
    consumer     VARCHAR(255)             NOT NULL,
    status       VARCHAR(20)              NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_event_consumptions_event
        FOREIGN KEY (event_id) REFERENCES outbox_events (id),
    CONSTRAINT chk_event_consumptions_status CHECK (status IN ('PROCESSED', 'FAILED'))
);

CREATE TABLE saga_instances
(
    id         UUID PRIMARY KEY,
    order_id   UUID                     NOT NULL,
    status     VARCHAR(30)              NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE saga_steps
(
    id           UUID PRIMARY KEY,
    saga_id      UUID                     NOT NULL,
    name         VARCHAR(100)             NOT NULL,
    status       VARCHAR(30)              NOT NULL,
    retry_count  INTEGER                  NOT NULL DEFAULT 0,
    completed_at TIMESTAMP WITH TIME ZONE,
    last_error   TEXT,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_saga_steps_saga
        FOREIGN KEY (saga_id) REFERENCES saga_instances (id) ON DELETE CASCADE,
    CONSTRAINT chk_saga_steps_retry_count CHECK (retry_count >= 0)
);

-- Spring Modulith 이벤트 발행 레지스트리.
CREATE TABLE event_publication
(
    id                     UUID PRIMARY KEY,
    completion_attempts    INTEGER                  NOT NULL,
    completion_date        TIMESTAMP WITH TIME ZONE,
    event_type             VARCHAR(255)             NOT NULL,
    last_resubmission_date TIMESTAMP WITH TIME ZONE,
    listener_id            VARCHAR(255)             NOT NULL,
    publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
    serialized_event       TEXT                     NOT NULL,
    status                 VARCHAR(255)
);

-- ============================================================================
-- P8: 판매자
-- ============================================================================

CREATE TABLE sellers
(
    id                         UUID PRIMARY KEY,
    user_id                    UUID                     NOT NULL,
    display_name               VARCHAR(255)             NOT NULL,
    business_name              VARCHAR(255)             NOT NULL,
    business_registration_hash VARCHAR(128)             NOT NULL,
    contact_email              VARCHAR(255)             NOT NULL,
    contact_phone              VARCHAR(20)              NOT NULL,
    status                     VARCHAR(20)              NOT NULL DEFAULT 'PENDING',
    reviewed_by                UUID,
    reviewed_at                TIMESTAMP WITH TIME ZONE,
    review_reason              VARCHAR(500),
    created_at                 TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                 TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_sellers_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'REJECTED', 'SUSPENDED'))
);

COMMENT ON TABLE sellers IS
    '판매자 신청·승인 프로필. 판매자 API는 PRODUCT_MANAGER 역할과 ACTIVE 상태를 모두 요구한다.';

-- ============================================================================
-- 인덱스 및 유일성 규칙
-- ============================================================================

CREATE UNIQUE INDEX uq_user_credentials_email ON user_credentials (email);
CREATE UNIQUE INDEX uq_social_credentials_provider_id
    ON social_credentials (provider_id, provider);
CREATE UNIQUE INDEX uq_users_phone_number
    ON users (phone_number) WHERE phone_number IS NOT NULL;
CREATE UNIQUE INDEX uq_addresses_one_primary
    ON addresses (user_id) WHERE is_primary = TRUE;
CREATE INDEX idx_addresses_user_id ON addresses (user_id);
CREATE INDEX idx_point_histories_user_created
    ON point_histories (user_id, created_at DESC, id DESC);
CREATE UNIQUE INDEX uq_blacklist_users_user_id ON blacklist_users (user_id);
CREATE INDEX idx_blacklist_tokens_token ON blacklist_tokens (token);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens (token);
CREATE UNIQUE INDEX uq_categories_name ON categories (name);
CREATE INDEX idx_categories_parent_depth ON categories (parent_id, depth);
CREATE INDEX idx_catalog_products_category_status
    ON catalog_products (category_id, publication_status);
CREATE INDEX idx_catalog_products_manager_status
    ON catalog_products (manager_id, publication_status);
CREATE UNIQUE INDEX uq_catalog_products_asin
    ON catalog_products (asin) WHERE asin IS NOT NULL;
CREATE UNIQUE INDEX uq_catalog_products_gtin
    ON catalog_products (gtin) WHERE gtin IS NOT NULL;
CREATE UNIQUE INDEX uq_catalog_products_upc
    ON catalog_products (upc) WHERE upc IS NOT NULL;
CREATE UNIQUE INDEX uq_catalog_products_ean
    ON catalog_products (ean) WHERE ean IS NOT NULL;
CREATE UNIQUE INDEX uq_catalog_products_isbn
    ON catalog_products (isbn) WHERE isbn IS NOT NULL;
CREATE UNIQUE INDEX uq_product_variants_sku ON product_variants (sku);
CREATE INDEX idx_product_variants_catalog_product_id ON product_variants (catalog_product_id);
CREATE INDEX idx_offers_variant_id ON offers (variant_id);
CREATE UNIQUE INDEX uq_offers_seller_variant
    ON offers (seller_id, variant_id)
    WHERE seller_id IS NOT NULL;
CREATE INDEX idx_images_entity ON images (entity_type, entity_id, sort_order);
CREATE UNIQUE INDEX uq_images_one_primary_per_entity
    ON images (entity_type, entity_id)
    WHERE is_primary = TRUE;
CREATE UNIQUE INDEX uq_images_entity_sort_order
    ON images (entity_type, entity_id, sort_order);
CREATE UNIQUE INDEX uq_tags_name ON tags (name);
CREATE UNIQUE INDEX uq_catalog_product_tags_catalog_product_tag
    ON catalog_product_tags (catalog_product_id, tag_id);
CREATE INDEX idx_reviews_variant_created
    ON reviews (variant_id, created_at DESC, id DESC);
CREATE UNIQUE INDEX uq_reviews_user_variant ON reviews (user_id, variant_id);
CREATE UNIQUE INDEX uq_wishlists_user_variant ON wishlists (user_id, variant_id);
CREATE UNIQUE INDEX uq_carts_one_active_per_user
    ON carts (user_id) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX uq_cart_items_cart_offer ON cart_items (cart_id, offer_id);
CREATE UNIQUE INDEX uq_user_coupons_user_coupon ON user_coupons (user_id, coupon_id);
CREATE UNIQUE INDEX uq_orders_order_number ON orders (order_number);
CREATE INDEX idx_orders_user_created ON orders (user_id, created_at DESC, id DESC);
CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE UNIQUE INDEX uq_payments_transaction_id ON payments (transaction_id);
CREATE INDEX idx_payments_order_id ON payments (order_id);
CREATE INDEX idx_outbox_pending ON outbox_events (status, created_at, id);
CREATE UNIQUE INDEX uq_event_consumptions_event_consumer
    ON event_consumptions (event_id, consumer);
CREATE UNIQUE INDEX uq_saga_steps_saga_name ON saga_steps (saga_id, name);
CREATE UNIQUE INDEX uq_sellers_active_application
    ON sellers (user_id)
    WHERE status IN ('PENDING', 'ACTIVE');
CREATE INDEX idx_sellers_status ON sellers (status);
CREATE INDEX idx_sellers_user_id ON sellers (user_id);
CREATE INDEX idx_offers_seller_id ON offers (seller_id);
