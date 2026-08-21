ALTER TABLE addresses
    ADD COLUMN alias VARCHAR(100);

UPDATE addresses
SET alias = '주소'
WHERE alias IS NULL;

ALTER TABLE addresses
    ALTER COLUMN alias SET NOT NULL;

CREATE UNIQUE INDEX uq_addresses_user_postal_address
    ON addresses (user_id, postal_code, address_line);

ALTER TABLE addresses
    ADD COLUMN last_used_at TIMESTAMPTZ;
