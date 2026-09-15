ALTER TABLE tourist_spot_image
    ADD COLUMN source_provider VARCHAR(200) NULL AFTER source_image_id,
    ADD COLUMN source_page_url VARCHAR(1000) NULL AFTER source_provider,
    ADD COLUMN license_url VARCHAR(1000) NULL AFTER source_page_url,
    ADD COLUMN attribution VARCHAR(500) NULL AFTER license_url,
    ADD COLUMN verified_at DATE NULL AFTER attribution;
