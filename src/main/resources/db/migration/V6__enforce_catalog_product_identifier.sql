ALTER TABLE catalog_products
    ADD CONSTRAINT chk_catalog_products_identifier_required
        CHECK (num_nonnulls(asin, gtin, upc, ean, isbn) >= 1);
