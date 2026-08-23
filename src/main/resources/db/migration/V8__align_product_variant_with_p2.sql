ALTER TABLE product_variants
    DROP COLUMN sku,
    DROP COLUMN weight,
    DROP COLUMN dimensions,
    ADD COLUMN attributes JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN publication_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN archived_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE product_variants
    ADD CONSTRAINT chk_product_variants_display_name
        CHECK (length(trim(display_name)) > 0),
    ADD CONSTRAINT chk_product_variants_attributes_object
        CHECK (jsonb_typeof(attributes) = 'object'),
    ADD CONSTRAINT chk_product_variants_publication_status
        CHECK (publication_status IN ('ACTIVE', 'ARCHIVED')),
    ADD CONSTRAINT chk_product_variants_archive_consistency
        CHECK ((publication_status = 'ACTIVE' AND archived_at IS NULL) OR
               (publication_status = 'ARCHIVED' AND archived_at IS NOT NULL));

COMMENT ON TABLE product_variants IS
    'CatalogProduct 아래에서 고객이 선택하고 주문하는 실제 구매 단위.';

