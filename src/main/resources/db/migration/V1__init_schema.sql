DROP SCHEMA IF EXISTS public CASCADE;
CREATE SCHEMA public;

CREATE TABLE users
(
    id            UUID PRIMARY KEY,
    name          VARCHAR(10) NOT NULL,
    phone_number  VARCHAR(11),
    role          VARCHAR(20) NOT NULL,
    point_balance INTEGER     NOT NULL DEFAULT 0,
    created_at    TIMESTAMP WITH TIME ZONE,
    updated_at    TIMESTAMP WITH TIME ZONE
);

CREATE TABLE user_credentials
(
    id                UUID PRIMARY KEY,
    user_id           UUID         NOT NULL,
    email             VARCHAR(100) NOT NULL,
    password          VARCHAR(255) NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE,
    updated_at        TIMESTAMP WITH TIME ZONE
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
    product_id UUID NOT NULL,
    tag_id     UUID NOT NULL
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

create table refresh_tokens
(
    id UUID primary key,
    user_id UUID not null,
    device_id varchar(100) not null,
    token varchar(36) not null,
    pre_token varchar(36),
    expired_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone
);

-- ------------------------------------------------------------------------------
-- 3. Unique Constraints & Indexes
-- ------------------------------------------------------------------------------
create index idx_refresh_tokens_user_id on refresh_tokens (user_id);

CREATE UNIQUE INDEX idx_user_credentials_user_id ON user_credentials (user_id);

ALTER TABLE tags
    ADD CONSTRAINT uq_tags_name UNIQUE (name);

CREATE UNIQUE INDEX idx_wishlists_user_product ON wishlists (user_id, product_id);

CREATE UNIQUE INDEX idx_users_name ON users (name);
CREATE UNIQUE INDEX idx_users_phone_number ON users (phone_number);

CREATE UNIQUE INDEX idx_user_coupons_user_coupon ON user_coupons (user_id, coupon_id);

CREATE UNIQUE INDEX idx_cart_items_unique ON cart_items (cart_id, product_id);

ALTER TABLE deliveries
    ADD CONSTRAINT uq_deliveries_order_id UNIQUE (order_id);

CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_reviews_product_id ON reviews (product_id);
CREATE UNIQUE INDEX idx_reviews_user_product ON reviews (user_id, product_id);
CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_payments_order_id ON payments (order_id);


-- ------------------------------------------------------------------------------
-- 4. Check Constraints
-- ------------------------------------------------------------------------------

ALTER TABLE users
    ADD CONSTRAINT chk_users_point_balance CHECK (point_balance >= 0);

ALTER TABLE users
    ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN', 'PRODUCT_MANAGER'));

ALTER TABLE products
    ADD CONSTRAINT chk_products_status CHECK (status IN ('ON_SALE', 'SOLD_OUT', 'RESTOCK_SCHEDULED'));

ALTER TABLE reviews
    ADD CONSTRAINT chk_reviews_rating CHECK (rating >= 1 AND rating <= 5);

ALTER TABLE coupons
    ADD CONSTRAINT chk_coupons_discount_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT'));

ALTER TABLE cart_items
    ADD CONSTRAINT chk_cart_items_quantity CHECK (quantity > 0);

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_status CHECK (status IN
                                            ('PENDING', 'PAID', 'PREPARING', 'SHIPPED', 'DELIVERED',
                                             'CANCELED'));

ALTER TABLE order_items
    ADD CONSTRAINT chk_order_items_quantity CHECK (quantity > 0);

ALTER TABLE deliveries
    ADD CONSTRAINT chk_deliveries_status CHECK (status IN ('PREPARING', 'SHIPPED', 'DELIVERED'));

ALTER TABLE payment_methods
    ADD CONSTRAINT chk_payment_methods_type CHECK (method_type IN ('CARD', 'KAKAOPAY', 'NAVERPAY', 'TRANSFER'));

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_status CHECK (status IN ('COMPLETED', 'FAILED', 'REFUNDED'));
