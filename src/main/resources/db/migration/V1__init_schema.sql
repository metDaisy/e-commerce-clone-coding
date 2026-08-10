-- P1-P8 basic requirement schema.
--
-- All identifiers use UUID. Cross-domain identifiers are intentionally kept
-- without database foreign keys; only relationships within the same domain
-- are enforced here (see docs/adr/0004-schema-fk-follows-modulith-boundaries.md).

-- ============================================================================
-- P1: User & Auth
-- ============================================================================

CREATE TABLE users
(
    id            UUID PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    phone_number  VARCHAR(20),
    role          VARCHAR(30)  NOT NULL DEFAULT 'USER',
    point_balance INTEGER      NOT NULL DEFAULT 0,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'PRODUCT_MANAGER', 'ADMIN')),
    CONSTRAINT chk_users_point_balance CHECK (point_balance >= 0)
);

COMMENT ON COLUMN users.role IS
    'USER: buyer by default, PRODUCT_MANAGER: seller, ADMIN: platform operator. PRODUCT_MANAGER can also use buyer APIs.';

CREATE TABLE user_credentials
(
    user_id         UUID PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    violation_count INTEGER      NOT NULL DEFAULT 0,
    until_locked    TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

COMMENT ON TABLE user_credentials IS
    'Local email/password credentials only. Social signup does not create a row.';

CREATE TABLE social_credentials
(
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL,
    provider    VARCHAR(20)  NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_social_credentials_provider
        CHECK (provider IN ('GOOGLE', 'NAVER', 'KAKAO', 'GITHUB'))
);

COMMENT ON TABLE social_credentials IS
    'OAuth identity. The provider enum is defined by this application, not by Amazon.com.';

CREATE TABLE addresses
(
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL,
    recipient_name  VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(20)  NOT NULL,
    postal_code     VARCHAR(20)  NOT NULL,
    address_line    VARCHAR(255) NOT NULL,
    is_primary      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE point_histories
(
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL,
    amount      INTEGER      NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE wishlists
(
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL,
    product_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE refresh_tokens
(
    id         UUID PRIMARY KEY,
    user_id    UUID         NOT NULL,
    device_id  VARCHAR(100) NOT NULL,
    token      VARCHAR(255) NOT NULL,
    pre_token  VARCHAR(255),
    expired_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE blacklist_tokens
(
    id         UUID PRIMARY KEY,
    token      VARCHAR(255) NOT NULL,
    expired_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE blacklist_users
(
    id             UUID PRIMARY KEY,
    user_id        UUID NOT NULL,
    compromised_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

-- ============================================================================
-- P2: Catalog, Inventory & Review
-- ============================================================================

CREATE TABLE categories
(
    id         UUID PRIMARY KEY,
    parent_id  UUID,
    name       VARCHAR(255) NOT NULL,
    depth      INTEGER      NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_categories_parent
        FOREIGN KEY (parent_id) REFERENCES categories (id),
    CONSTRAINT chk_categories_depth CHECK (depth BETWEEN 1 AND 3)
);

-- products represents the CatalogProduct aggregate.
CREATE TABLE products
(
    id                 UUID PRIMARY KEY,
    category_id        UUID         NOT NULL,
    manager_id         UUID         NOT NULL,
    name               VARCHAR(255) NOT NULL,
    description        TEXT         NOT NULL,
    brand              VARCHAR(255),
    asin               VARCHAR(50),
    gtin               VARCHAR(50),
    upc                VARCHAR(50),
    ean                VARCHAR(50),
    isbn               VARCHAR(50),
    attributes         JSONB,
    publication_status VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    archived_at        TIMESTAMP WITH TIME ZONE,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT chk_products_publication_status
        CHECK (publication_status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT chk_products_archive_consistency
        CHECK ((publication_status = 'ACTIVE' AND archived_at IS NULL) OR
               (publication_status = 'ARCHIVED' AND archived_at IS NOT NULL))
);

COMMENT ON COLUMN products.manager_id IS
    'Logical users.id of the user who registered/manages the catalog product; PRODUCT_MANAGER owns its products and ADMIN can manage all products.';

COMMENT ON COLUMN products.asin IS
    'Optional external product identifier; not used as the primary key.';
COMMENT ON COLUMN products.gtin IS
    'Optional external product identifier; not used as the primary key.';
COMMENT ON COLUMN products.upc IS
    'Optional external product identifier; not used as the primary key.';
COMMENT ON COLUMN products.ean IS
    'Optional external product identifier; not used as the primary key.';
COMMENT ON COLUMN products.isbn IS
    'Optional external product identifier; not used as the primary key.';

CREATE TABLE product_variants
(
    id           UUID PRIMARY KEY,
    product_id   UUID         NOT NULL,
    sku          VARCHAR(100) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    weight       NUMERIC(10, 3),
    dimensions   JSONB,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_product_variants_product
        FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE offers
(
    id                    UUID PRIMARY KEY,
    variant_id            UUID           NOT NULL,
    seller_id             UUID,
    base_price_amount     NUMERIC(19, 2) NOT NULL,
    discount_price_amount NUMERIC(19, 2),
    currency              CHAR(3)        NOT NULL,
    discount_start_at     TIMESTAMP WITH TIME ZONE,
    discount_end_at       TIMESTAMP WITH TIME ZONE,
    status                VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
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

COMMENT ON COLUMN offers.seller_id IS
    'Logical seller_profiles.id of the Offer owner. NULL means the platform default Offer created by ADMIN.';

CREATE TABLE inventories
(
    id         UUID PRIMARY KEY,
    offer_id   UUID      NOT NULL,
    quantity   INTEGER   NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_inventories_offer UNIQUE (offer_id),
    CONSTRAINT chk_inventories_quantity CHECK (quantity >= 0),
    CONSTRAINT fk_inventories_offer
        FOREIGN KEY (offer_id) REFERENCES offers (id)
);

CREATE TABLE images
(
    id          UUID PRIMARY KEY,
    entity_type VARCHAR(30)   NOT NULL,
    entity_id   UUID          NOT NULL,
    media_type  VARCHAR(20)   NOT NULL DEFAULT 'IMAGE',
    image_url   VARCHAR(1000) NOT NULL,
    is_primary  BOOLEAN       NOT NULL DEFAULT FALSE,
    sort_order  INTEGER       NOT NULL DEFAULT 0,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_images_media_type CHECK (media_type = 'IMAGE'),
    CONSTRAINT chk_images_sort_order CHECK (sort_order >= 0)
);

CREATE TABLE tags
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE product_tags
(
    id         UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    tag_id     UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_product_tags_product
        FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_product_tags_tag
        FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
);

CREATE TABLE reviews
(
    id         UUID PRIMARY KEY,
    product_id UUID         NOT NULL,
    user_id    UUID         NOT NULL,
    rating     INTEGER      NOT NULL,
    content    VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);

-- ============================================================================
-- P3: Cart
-- ============================================================================

CREATE TABLE carts
(
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_carts_status CHECK (status IN ('ACTIVE', 'CHECKED_OUT'))
);

CREATE TABLE cart_items
(
    id         UUID PRIMARY KEY,
    cart_id    UUID NOT NULL,
    offer_id   UUID NOT NULL,
    quantity   INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_cart_items_cart
        FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE,
    CONSTRAINT chk_cart_items_quantity CHECK (quantity BETWEEN 1 AND 10)
);

-- ============================================================================
-- P4: Coupon
-- ============================================================================

CREATE TABLE coupons
(
    id                     UUID PRIMARY KEY,
    name                   VARCHAR(255)     NOT NULL,
    discount_type          VARCHAR(20)      NOT NULL,
    discount_value         NUMERIC(19, 2)   NOT NULL,
    max_discount_amount    NUMERIC(19, 2),
    minimum_order_amount   NUMERIC(19, 2)   NOT NULL DEFAULT 0,
    applicable_category_id UUID,
    total_quantity         INTEGER,
    issued_quantity        INTEGER          NOT NULL DEFAULT 0,
    status                 VARCHAR(20)      NOT NULL DEFAULT 'ACTIVE',
    valid_from             TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until            TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL,
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
               (total_quantity IS NULL OR (total_quantity >= 0 AND issued_quantity <= total_quantity))),
    CONSTRAINT chk_coupons_period CHECK (valid_from < valid_until),
    CONSTRAINT chk_coupons_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE user_coupons
(
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL,
    coupon_id  UUID        NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    used_at    TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_user_coupons_coupon
        FOREIGN KEY (coupon_id) REFERENCES coupons (id),
    CONSTRAINT chk_user_coupons_status
        CHECK (status IN ('AVAILABLE', 'USED', 'EXPIRED'))
);

-- ============================================================================
-- P5: Order, Payment & Delivery
-- ============================================================================

CREATE TABLE orders
(
    id                      UUID PRIMARY KEY,
    order_number            VARCHAR(40)      NOT NULL,
    user_id                 UUID             NOT NULL,
    address_id              UUID             NOT NULL,
    used_user_coupon_id     UUID,
    used_point_amount       NUMERIC(19, 2)   NOT NULL DEFAULT 0,
    status                  VARCHAR(20)      NOT NULL DEFAULT 'PENDING',
    subtotal_amount         NUMERIC(19, 2)   NOT NULL,
    discount_amount         NUMERIC(19, 2)   NOT NULL DEFAULT 0,
    shipping_fee            NUMERIC(19, 2)   NOT NULL DEFAULT 0,
    paid_amount             NUMERIC(19, 2)   NOT NULL,
    currency                CHAR(3)          NOT NULL,
    shipping_recipient_name VARCHAR(100)     NOT NULL,
    shipping_recipient_phone VARCHAR(20)     NOT NULL,
    shipping_postal_code    VARCHAR(20)      NOT NULL,
    shipping_address_line   VARCHAR(255)     NOT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_orders_status CHECK (status IN ('PENDING', 'PAID', 'CANCELED')),
    CONSTRAINT chk_orders_amounts CHECK (subtotal_amount >= 0 AND discount_amount >= 0 AND
                                         shipping_fee >= 0 AND used_point_amount >= 0 AND
                                         paid_amount >= 0)
);

CREATE TABLE order_items
(
    id             UUID PRIMARY KEY,
    order_id       UUID           NOT NULL,
    product_id     UUID           NOT NULL,
    variant_id     UUID           NOT NULL,
    offer_id       UUID           NOT NULL,
    sku            VARCHAR(100)   NOT NULL,
    product_name   VARCHAR(255)   NOT NULL,
    quantity       INTEGER        NOT NULL,
    unit_price     NUMERIC(19, 2) NOT NULL,
    subtotal       NUMERIC(19, 2) NOT NULL,
    currency       CHAR(3)        NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_items_amounts CHECK (unit_price >= 0 AND subtotal >= 0)
);

CREATE TABLE payment_methods
(
    id            UUID PRIMARY KEY,
    user_id       UUID        NOT NULL,
    method_type   VARCHAR(30) NOT NULL,
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
    order_id          UUID           NOT NULL,
    payment_method_id UUID           NOT NULL,
    transaction_id    VARCHAR(255)   NOT NULL,
    status            VARCHAR(20)    NOT NULL,
    amount            NUMERIC(19, 2) NOT NULL,
    currency          CHAR(3)        NOT NULL,
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
    order_id                   UUID        NOT NULL,
    status                     VARCHAR(20) NOT NULL DEFAULT 'PREPARING',
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
-- P6: Outbox, Idempotent Consumption & Saga
-- ============================================================================

CREATE TABLE outbox_events
(
    id             UUID PRIMARY KEY,
    aggregate_type VARCHAR(30)  NOT NULL,
    aggregate_id   UUID         NOT NULL,
    event_type     VARCHAR(255) NOT NULL,
    payload        JSONB        NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count    INTEGER      NOT NULL DEFAULT 0,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at   TIMESTAMP WITH TIME ZONE,
    last_error     TEXT,
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT chk_outbox_retry_count CHECK (retry_count >= 0)
);

CREATE TABLE event_consumptions
(
    id           UUID PRIMARY KEY,
    event_id     UUID        NOT NULL,
    consumer     VARCHAR(255) NOT NULL,
    status       VARCHAR(20) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_event_consumptions_event
        FOREIGN KEY (event_id) REFERENCES outbox_events (id),
    CONSTRAINT chk_event_consumptions_status CHECK (status IN ('PROCESSED', 'FAILED'))
);

CREATE TABLE saga_instances
(
    id         UUID PRIMARY KEY,
    order_id   UUID        NOT NULL,
    status     VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE saga_steps
(
    id             UUID PRIMARY KEY,
    saga_id        UUID        NOT NULL,
    name           VARCHAR(100) NOT NULL,
    status         VARCHAR(30) NOT NULL,
    retry_count    INTEGER     NOT NULL DEFAULT 0,
    completed_at   TIMESTAMP WITH TIME ZONE,
    last_error     TEXT,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_saga_steps_saga
        FOREIGN KEY (saga_id) REFERENCES saga_instances (id) ON DELETE CASCADE,
    CONSTRAINT chk_saga_steps_retry_count CHECK (retry_count >= 0)
);

-- Spring Modulith's event publication registry.
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
-- P8: Seller
-- ============================================================================

CREATE TABLE seller_profiles
(
    id                              UUID PRIMARY KEY,
    user_id                         UUID         NOT NULL,
    display_name                    VARCHAR(255) NOT NULL,
    business_name                   VARCHAR(255) NOT NULL,
    business_registration_hash     VARCHAR(128) NOT NULL,
    contact_email                   VARCHAR(255) NOT NULL,
    contact_phone                   VARCHAR(20)  NOT NULL,
    status                          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reviewed_by                     UUID,
    reviewed_at                     TIMESTAMP WITH TIME ZONE,
    review_reason                   VARCHAR(500),
    created_at                      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_seller_profiles_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'REJECTED', 'SUSPENDED'))
);

COMMENT ON TABLE seller_profiles IS
    'Seller application and approval profile. Seller APIs require both role PRODUCT_MANAGER and status ACTIVE.';

-- ============================================================================
-- Indexes and uniqueness rules
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
CREATE INDEX idx_products_category_status
    ON products (category_id, publication_status);
CREATE INDEX idx_products_manager_status
    ON products (manager_id, publication_status);
CREATE UNIQUE INDEX uq_products_asin
    ON products (asin) WHERE asin IS NOT NULL;
CREATE UNIQUE INDEX uq_products_gtin
    ON products (gtin) WHERE gtin IS NOT NULL;
CREATE UNIQUE INDEX uq_products_upc
    ON products (upc) WHERE upc IS NOT NULL;
CREATE UNIQUE INDEX uq_products_ean
    ON products (ean) WHERE ean IS NOT NULL;
CREATE UNIQUE INDEX uq_products_isbn
    ON products (isbn) WHERE isbn IS NOT NULL;
CREATE UNIQUE INDEX uq_product_variants_sku ON product_variants (sku);
CREATE INDEX idx_product_variants_product_id ON product_variants (product_id);
CREATE INDEX idx_offers_variant_id ON offers (variant_id);
CREATE INDEX idx_images_entity ON images (entity_type, entity_id, sort_order);
CREATE UNIQUE INDEX uq_tags_name ON tags (name);
CREATE UNIQUE INDEX uq_product_tags_product_tag ON product_tags (product_id, tag_id);
CREATE INDEX idx_reviews_product_created
    ON reviews (product_id, created_at DESC, id DESC);
CREATE UNIQUE INDEX uq_reviews_user_product ON reviews (user_id, product_id);
CREATE UNIQUE INDEX uq_wishlists_user_product ON wishlists (user_id, product_id);
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
CREATE UNIQUE INDEX uq_seller_profiles_active_application
    ON seller_profiles (user_id)
    WHERE status IN ('PENDING', 'ACTIVE');
CREATE INDEX idx_seller_profiles_status ON seller_profiles (status);
CREATE INDEX idx_seller_profiles_user_id ON seller_profiles (user_id);
CREATE INDEX idx_offers_seller_id ON offers (seller_id);
