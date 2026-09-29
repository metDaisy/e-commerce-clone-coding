-- P3 Cart ownership/lifecycle and invariant alignment.
ALTER TABLE carts
    ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE carts
    ADD COLUMN expires_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE carts
    DROP CONSTRAINT chk_carts_status;

ALTER TABLE carts
    DROP COLUMN status;

ALTER TABLE carts
    ADD CONSTRAINT chk_carts_owner_lifecycle
        CHECK ((user_id IS NOT NULL AND expires_at IS NULL)
            OR (user_id IS NULL AND expires_at IS NOT NULL));

CREATE UNIQUE INDEX uq_carts_member_user_id ON carts (user_id)
    WHERE user_id IS NOT NULL;

ALTER TABLE cart_items
    DROP CONSTRAINT chk_cart_items_quantity;

ALTER TABLE cart_items
    ADD CONSTRAINT chk_cart_items_quantity CHECK (quantity BETWEEN 1 AND 1000);

DROP INDEX uq_cart_items_cart_offer;

ALTER TABLE cart_items
    ADD CONSTRAINT uq_cart_items_cart_offer UNIQUE (cart_id, offer_id);
