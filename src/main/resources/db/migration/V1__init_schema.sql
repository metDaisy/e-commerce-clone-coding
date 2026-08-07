DROP SCHEMA IF EXISTS public CASCADE;
CREATE SCHEMA public;

-- ============================================================================
-- TABLES
-- ============================================================================

CREATE TABLE users
(
    id            UUID PRIMARY KEY,
    name          VARCHAR(10) NOT NULL,
    phone_number  VARCHAR(11),
    role          VARCHAR(20) NOT NULL,
    point_balance INTEGER     NOT NULL DEFAULT 0,
    address       varchar(100),
    is_enabled    boolean     not null default true,
    created_at    TIMESTAMP WITH TIME ZONE,
    updated_at    TIMESTAMP WITH TIME ZONE
);

CREATE TABLE user_credentials
(
    user_id         UUID PRIMARY KEY,
    email           VARCHAR(100) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    violation_count integer      not null,
    until_locked    timestamp with time zone,
    created_at      TIMESTAMP WITH TIME ZONE,
    updated_at      TIMESTAMP WITH TIME ZONE
);
-- id/pw 에 대한 인증정보

CREATE TABLE addresses
(
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL,
    postal_code VARCHAR(20),
    city        VARCHAR(255) NOT NULL,
    is_primary  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE,
    updated_at  TIMESTAMP WITH TIME ZONE
);

CREATE TABLE point_histories
(
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL,
    amount      INTEGER      NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE
);

CREATE TABLE images
(
    id         UUID PRIMARY KEY,
    image_url  VARCHAR(1000) NOT NULL,
    status     varchar(20)   not null,
    created_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE categories
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE
);
-- 계층 카테고리

CREATE TABLE products
(
    id             UUID PRIMARY KEY,
    category_id    UUID         NOT NULL,
    manager_id     UUID         not null,
    name           VARCHAR(255) NOT NULL,
    description    TEXT,
    price          INTEGER      NOT NULL,
    sale_price     INTEGER,
    sale_start_at  TIMESTAMP WITH TIME ZONE,
    sale_end_at    TIMESTAMP WITH TIME ZONE,
    status         VARCHAR(50)  NOT NULL DEFAULT 'ON_SALE',
    stock_quantity INTEGER      NOT NULL DEFAULT 0,
    view_count     INTEGER      NOT NULL DEFAULT 0,
    is_time_sale   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP WITH TIME ZONE,
    updated_at     TIMESTAMP WITH TIME ZONE
);

CREATE TABLE tags
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE product_tags
(
    id         UUID primary key,
    product_id UUID NOT NULL,
    tag_id     UUID NOT NULL,
    created_at timestamp with time zone
);

CREATE TABLE reviews
(
    id         UUID PRIMARY KEY,
    product_id UUID    NOT NULL,
    user_id    UUID    NOT NULL,
    rating     INTEGER NOT NULL,
    content    TEXT,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE wishlists
(
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL,
    product_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE coupons
(
    id             UUID PRIMARY KEY,
    name           VARCHAR(255)             NOT NULL,
    discount_type  VARCHAR(50)              NOT NULL,
    discount_value INTEGER                  NOT NULL,
    valid_from     TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE
);

CREATE TABLE user_coupons
(
    id         UUID PRIMARY KEY,
    user_id    UUID    NOT NULL,
    coupon_id  UUID    NOT NULL,
    is_used    BOOLEAN NOT NULL DEFAULT FALSE,
    used_at    TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE carts
(
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE
);
-- 장바구니

CREATE TABLE cart_items
(
    id         UUID PRIMARY KEY,
    cart_id    UUID    NOT NULL,
    product_id UUID    NOT NULL,
    quantity   INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE orders
(
    id                  UUID PRIMARY KEY,
    user_id             UUID        NOT NULL,
    address_id          UUID        NOT NULL,
    used_user_coupon_id UUID,
    used_point_amount   INTEGER     NOT NULL DEFAULT 0,
    status              VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    original_amount     INTEGER     NOT NULL,
    discount_amount     INTEGER     NOT NULL DEFAULT 0,
    final_amount        INTEGER     NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE
);

CREATE TABLE order_items
(
    id         UUID PRIMARY KEY,
    order_id   UUID    NOT NULL,
    product_id UUID    NOT NULL,
    quantity   INTEGER NOT NULL,
    price      INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE deliveries
(
    id              UUID PRIMARY KEY,
    order_id        UUID        NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'PREPARING',
    tracking_number VARCHAR(100),
    created_at      TIMESTAMP WITH TIME ZONE,
    updated_at      TIMESTAMP WITH TIME ZONE
);

CREATE TABLE payment_methods
(
    id            UUID PRIMARY KEY,
    user_id       UUID        NOT NULL,
    method_type   VARCHAR(50) NOT NULL,
    provider      VARCHAR(100),
    masked_number VARCHAR(50),
    created_at    TIMESTAMP WITH TIME ZONE,
    updated_at    TIMESTAMP WITH TIME ZONE
);

CREATE TABLE payments
(
    id                UUID PRIMARY KEY,
    order_id          UUID        NOT NULL,
    payment_method_id UUID,
    status            VARCHAR(50) NOT NULL,
    amount            INTEGER     NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE,
    updated_at        TIMESTAMP WITH TIME ZONE
);

CREATE TABLE event_publication
(
    id                     UUID PRIMARY KEY,
    completion_attempts    INTEGER                  NOT NULL,
    completion_date        TIMESTAMP WITH TIME ZONE,
    event_type             VARCHAR(255)             NOT NULL,
    last_resubmission_date TIMESTAMP WITH TIME ZONE,
    listener_id            VARCHAR(255)             NOT NULL,
    publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
    serialized_event       VARCHAR(255)             NOT NULL,
    status                 VARCHAR(255)
);

CREATE TABLE refresh_tokens
(
    id         UUID primary key,
    user_id    UUID                     not null,
    device_id  varchar(100)             not null,
    token      varchar(36)              not null,
    pre_token  varchar(36),
    expired_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone
);

CREATE TABLE blacklist_tokens
(
    id         UUID primary key,
    token      varchar(36)              not null,
    expired_at timestamp with time zone not null,
    created_at timestamp with time zone
);

CREATE TABLE blacklist_users
(
    id             UUID primary key,
    user_id        UUID                     not null,
    compromised_at timestamp with time zone not null,
    created_at     timestamp with time zone
);

CREATE TABLE social_credentials
(
    id          UUID primary key,
    user_id     UUID         not null,
    provider    varchar(20)  not null,
    provider_id varchar(128) not null,
    created_at  timestamp with time zone
);

-- ============================================================================
-- CONSTRAINTS
-- ============================================================================

-- social_credentials
ALTER TABLE social_credentials
    ADD CONSTRAINT chk_social_credentials_provider CHECK (provider in ('GOOGLE', 'NAVER', 'GITHUB', 'KAKAO'));

-- users
ALTER TABLE users
    ADD CONSTRAINT chk_users_point_balance CHECK (point_balance >= 0);

ALTER TABLE users
    ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN', 'PRODUCT_MANAGER'));

-- products
ALTER TABLE products
    ADD CONSTRAINT chk_products_status CHECK (status IN ('ON_SALE', 'SOLD_OUT', 'RESTOCK_SCHEDULED'));
alter table products
    add constraint fk_products_categories_id foreign key (category_id) REFERENCES categories (id);

-- product_tags
alter table product_tags
    add constraint fk_product_tags_product_id foreign key (product_id) references products (id) on DELETE cascade;
alter table product_tags
    add constraint fk_product_tags_tag_id foreign key (tag_id) references tags (id) on delete cascade;

-- reviews
ALTER TABLE reviews
    ADD CONSTRAINT chk_reviews_rating CHECK (rating >= 1 AND rating <= 5);

-- coupons
ALTER TABLE coupons
    ADD CONSTRAINT chk_coupons_discount_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT'));

ALTER TABLE tags
    ADD CONSTRAINT uq_tags_name UNIQUE (name);

-- cart_items
ALTER TABLE cart_items
    ADD CONSTRAINT chk_cart_items_quantity CHECK (quantity > 0);

-- orders
ALTER TABLE orders
    ADD CONSTRAINT chk_orders_status CHECK (status IN
                                            ('PENDING', 'PAID', 'PREPARING', 'SHIPPED', 'DELIVERED',
                                             'CANCELED'));

-- order_items
ALTER TABLE order_items
    ADD CONSTRAINT chk_order_items_quantity CHECK (quantity > 0);

-- deliveries
ALTER TABLE deliveries
    ADD CONSTRAINT chk_deliveries_status CHECK (status IN ('PREPARING', 'SHIPPED', 'DELIVERED'));

-- delivery order unique
ALTER TABLE deliveries
    ADD CONSTRAINT uq_deliveries_order_id UNIQUE (order_id);

-- payment_methods
ALTER TABLE payment_methods
    ADD CONSTRAINT chk_payment_methods_type CHECK (method_type IN ('CARD', 'KAKAOPAY', 'NAVERPAY', 'TRANSFER'));

-- payments
ALTER TABLE payments
    ADD CONSTRAINT chk_payments_status CHECK (status IN ('COMPLETED', 'FAILED', 'REFUNDED'));

-- categories
alter table categories
    add constraint uq_categories_name unique (name);

-- ============================================================================
-- INDEXES
-- ============================================================================

-- social_credentials
CREATE UNIQUE INDEX idx_social_credentials_provider ON social_credentials (provider_id, provider);

-- blacklist_tokens
CREATE INDEX idx_blacklist_tokens_token ON blacklist_tokens (token);

-- blacklist_users
CREATE UNIQUE INDEX idx_blacklist_users_user_id ON blacklist_users (user_id);

-- refresh_tokens
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens (token);

-- user_credentials
CREATE UNIQUE INDEX idx_user_credentials_email ON user_credentials (email);

-- users
CREATE UNIQUE INDEX idx_users_phone_number ON users (phone_number) WHERE is_enabled = true;

-- wishlists
CREATE UNIQUE INDEX idx_wishlists_user_product ON wishlists (user_id, product_id);

-- user_coupons
CREATE UNIQUE INDEX idx_user_coupons_user_coupon ON user_coupons (user_id, coupon_id);

-- cart_items
CREATE UNIQUE INDEX idx_cart_items_unique ON cart_items (cart_id, product_id);

-- products
CREATE INDEX idx_products_category_id ON products (category_id);

-- reviews
CREATE INDEX idx_reviews_product_id ON reviews (product_id);
CREATE UNIQUE INDEX idx_reviews_user_product ON reviews (user_id, product_id);

-- orders
CREATE INDEX idx_orders_user_id ON orders (user_id);

-- order_items
CREATE INDEX idx_order_items_order_id ON order_items (order_id);

-- payments
CREATE INDEX idx_payments_order_id ON payments (order_id);
