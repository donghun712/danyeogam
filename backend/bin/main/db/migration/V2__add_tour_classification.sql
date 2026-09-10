ALTER TABLE tourist_spot
    ADD COLUMN classification_level1 VARCHAR(20) NULL AFTER source_content_type_id,
    ADD COLUMN classification_level2 VARCHAR(20) NULL AFTER classification_level1,
    ADD COLUMN classification_level3 VARCHAR(20) NULL AFTER classification_level2,
    ADD INDEX ix_tourist_spot_classification (
        source, active, source_content_type_id,
        classification_level1, classification_level2, classification_level3
    );
