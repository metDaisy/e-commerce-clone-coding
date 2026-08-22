ALTER TABLE catalog_products
    ADD CONSTRAINT chk_catalog_products_asin_format
        CHECK (asin IS NULL OR asin ~ '^[A-Za-z0-9]{10}$'),
    ADD CONSTRAINT chk_catalog_products_gtin_format
        CHECK (gtin IS NULL OR gtin ~ '^([0-9]{8}|[0-9]{12}|[0-9]{13}|[0-9]{14})$'),
    ADD CONSTRAINT chk_catalog_products_upc_format
        CHECK (upc IS NULL OR upc ~ '^[0-9]{12}$'),
    ADD CONSTRAINT chk_catalog_products_ean_format
        CHECK (ean IS NULL OR ean ~ '^([0-9]{8}|[0-9]{13})$'),
    ADD CONSTRAINT chk_catalog_products_isbn_format
        CHECK (isbn IS NULL OR regexp_replace(isbn, '[-[:space:]]', '', 'g')
            ~ '^([0-9]{9}[0-9Xx]|[0-9]{13})$');
