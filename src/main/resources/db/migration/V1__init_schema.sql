-- ------------------------------------------------------------------------------
-- 1. User & Auth Module
-- ------------------------------------------------------------------------------

CREATE TABLE users
(
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    phone_number  VARCHAR(50),
    role          VARCHAR(50)  NOT NULL, -- USER, ADMIN, PRODUCT_MANAGER
    point_balance INTEGER      NOT NULL    DEFAULT 0,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_credentials
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    login_type VARCHAR(50)  NOT NULL, -- LOCAL, OAUTH
    login_id   VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255),
    provider   VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE addresses
(
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    zip_code     VARCHAR(20)  NOT NULL,
    address_line VARCHAR(500) NOT NULL,
    is_default   BOOLEAN      NOT NULL    DEFAULT FALSE,
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE point_histories
(
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    amount      INTEGER      NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------------------------
-- 2. Common / Media Module
-- ------------------------------------------------------------------------------

CREATE TABLE images
(
    id          BIGSERIAL PRIMARY KEY,
    target_type VARCHAR(50)   NOT NULL, -- PRODUCT, REVIEW 등 다형성 타겟
    target_id   BIGINT        NOT NULL,
    image_url   VARCHAR(1000) NOT NULL,
    sort_order  INTEGER       NOT NULL   DEFAULT 0,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------------------------
-- 3. Catalog & Inventory Module
-- ------------------------------------------------------------------------------

CREATE TABLE categories
(
    id         BIGSERIAL PRIMARY KEY,
    parent_id  BIGINT,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products
(
    id             BIGSERIAL PRIMARY KEY,
    category_id    BIGINT       NOT NULL,
    manager_id     BIGINT,
    name           VARCHAR(255) NOT NULL,
    description    TEXT,
    price          INTEGER      NOT NULL,
    sale_price     INTEGER,
    sale_start_at  TIMESTAMP WITH TIME ZONE,
    sale_end_at    TIMESTAMP WITH TIME ZONE,
    status         VARCHAR(50)  NOT NULL    DEFAULT 'ON_SALE', -- ON_SALE, SOLD_OUT, RESTOCK_SCHEDULED
    stock_quantity INTEGER      NOT NULL    DEFAULT 0,
    view_count     INTEGER      NOT NULL    DEFAULT 0,
    is_time_sale   BOOLEAN      NOT NULL    DEFAULT FALSE,
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tags
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE product_tags
(
    product_id BIGINT NOT NULL,
    tag_id     BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (product_id, tag_id)
);

CREATE TABLE reviews
(
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT  NOT NULL,
    user_id    BIGINT  NOT NULL,
    rating     INTEGER NOT NULL, -- CHECK: 1~5
    content    TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE wishlists
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------------------------
-- 4. Coupon Module
-- ------------------------------------------------------------------------------

CREATE TABLE coupons
(
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(255)             NOT NULL,
    discount_type  VARCHAR(50)              NOT NULL, -- PERCENTAGE, FIXED_AMOUNT
    discount_value INTEGER                  NOT NULL,
    valid_from     TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_coupons
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT  NOT NULL,
    coupon_id  BIGINT  NOT NULL,
    is_used    BOOLEAN NOT NULL         DEFAULT FALSE,
    used_at    TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------------------------
-- 5. Cart Module
-- ------------------------------------------------------------------------------

CREATE TABLE carts
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL UNIQUE, -- FK 추가: carts.user_id -> users.id
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cart_items
(
    id         BIGSERIAL PRIMARY KEY,
    cart_id    BIGINT  NOT NULL,
    product_id BIGINT  NOT NULL,
    quantity   INTEGER NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------------------------
-- 6. Order & Delivery Module
-- ------------------------------------------------------------------------------

CREATE TABLE orders
(
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT      NOT NULL,
    address_id          BIGINT      NOT NULL,
    used_user_coupon_id BIGINT,                                     -- FK 추가: orders.used_user_coupon_id -> user_coupons.id
    used_point_amount   INTEGER     NOT NULL     DEFAULT 0,
    status              VARCHAR(50) NOT NULL     DEFAULT 'PENDING', -- PENDING, PAID, PREPARING, SHIPPED, DELIVERED, CANCELED
    original_amount     INTEGER     NOT NULL,
    discount_amount     INTEGER     NOT NULL     DEFAULT 0,
    final_amount        INTEGER     NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items
(
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT  NOT NULL,
    product_id BIGINT  NOT NULL,
    quantity   INTEGER NOT NULL CHECK (quantity > 0),
    price      INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE deliveries
(
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT      NOT NULL UNIQUE,                  -- FK 추가: deliveries.order_id -> orders.id
    status          VARCHAR(50) NOT NULL     DEFAULT 'PREPARING', -- PREPARING, SHIPPED, DELIVERED
    tracking_number VARCHAR(100),
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------------------------
-- 7. Payment Module
-- ------------------------------------------------------------------------------

CREATE TABLE payment_methods
(
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT      NOT NULL,
    method_type   VARCHAR(50) NOT NULL, -- CARD, KAKAOPAY, NAVERPAY, TRANSFER
    provider      VARCHAR(100),
    masked_number VARCHAR(50),
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payments
(
    id                BIGSERIAL PRIMARY KEY,
    order_id          BIGINT      NOT NULL,
    payment_method_id BIGINT,               -- FK 추가: payments.payment_method_id -> payment_methods.id
    status            VARCHAR(50) NOT NULL, -- COMPLETED, FAILED, REFUNDED
    amount            INTEGER     NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------------------------
-- 8. Outbox (Transaction)
-- ------------------------------------------------------------------------------

CREATE TABLE outbox
(
    id             BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id   BIGINT       NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        TEXT         NOT NULL,
    status         VARCHAR(50)  NOT NULL    DEFAULT 'PENDING', -- PENDING, PUBLISHED
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 9. Constraints & Indexes (Table-by-Table)
-- ==============================================================================

---------------------------------------------------------------------------------
-- [1] users - No additional constraints needed beyond PK and NOT NULL
---------------------------------------------------------------------------------

---------------------------------------------------------------------------------
-- [2] user_credentials
---------------------------------------------------------------------------------
ALTER TABLE user_credentials
    ADD CONSTRAINT fk_user_credentials_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
CREATE UNIQUE INDEX idx_user_credentials_login_id ON user_credentials (login_id);

---------------------------------------------------------------------------------
-- [3] addresses
---------------------------------------------------------------------------------
ALTER TABLE addresses
    ADD CONSTRAINT fk_addresses_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

---------------------------------------------------------------------------------
-- [4] point_histories
---------------------------------------------------------------------------------
ALTER TABLE point_histories
    ADD CONSTRAINT fk_point_histories_user_id FOREIGN KEY (user_id) REFERENCES users (id);

---------------------------------------------------------------------------------
-- [5] images
---------------------------------------------------------------------------------
CREATE INDEX idx_images_target ON images (target_type, target_id);

---------------------------------------------------------------------------------
-- [6] categories
---------------------------------------------------------------------------------
ALTER TABLE categories
    ADD CONSTRAINT fk_categories_parent_id FOREIGN KEY (parent_id) REFERENCES categories (id) ON DELETE SET NULL;

---------------------------------------------------------------------------------
-- [7] products
---------------------------------------------------------------------------------
ALTER TABLE products
    ADD CONSTRAINT fk_products_category_id FOREIGN KEY (category_id) REFERENCES categories (id);
ALTER TABLE products
    ADD CONSTRAINT fk_products_manager_id FOREIGN KEY (manager_id) REFERENCES users (id) ON DELETE SET NULL;
ALTER TABLE products
    ADD CONSTRAINT chk_products_status CHECK (status IN ('ON_SALE', 'SOLD_OUT', 'RESTOCK_SCHEDULED'));

---------------------------------------------------------------------------------
-- [8] tags - No additional constraints needed beyond PK, UNIQUE(name), and NOT NULL
---------------------------------------------------------------------------------

---------------------------------------------------------------------------------
-- [9] product_tags
---------------------------------------------------------------------------------
ALTER TABLE product_tags
    ADD CONSTRAINT fk_product_tags_product_id FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE;
