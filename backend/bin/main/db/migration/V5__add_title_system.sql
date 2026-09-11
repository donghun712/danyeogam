ALTER TABLE title_definition
    DROP CHECK ck_title_definition_required_count,
    MODIFY COLUMN required_visit_count INT UNSIGNED NULL,
    ADD COLUMN condition_type VARCHAR(40) NOT NULL DEFAULT 'VISIT_COUNT' AFTER region_id,
    ADD COLUMN target_region_level VARCHAR(20) NULL AFTER condition_type,
    ADD COLUMN classification_level TINYINT UNSIGNED NULL AFTER target_region_level,
    ADD COLUMN classification_codes VARCHAR(255) NULL AFTER classification_level,
    ADD COLUMN required_progress_percent DECIMAL(5, 2) NULL AFTER required_visit_count,
    ADD CONSTRAINT ck_title_definition_condition_type
        CHECK (condition_type IN (
            'VISIT_COUNT',
            'DISTINCT_REGION_COUNT',
            'CLASSIFICATION_VISIT_COUNT',
            'REGION_PROGRESS_PERCENT'
        )),
    ADD CONSTRAINT ck_title_definition_target_region_level
        CHECK (target_region_level IS NULL OR target_region_level IN ('PROVINCE', 'CITY_COUNTY')),
    ADD CONSTRAINT ck_title_definition_classification_level
        CHECK (classification_level IS NULL OR classification_level IN (2, 3)),
    ADD CONSTRAINT ck_title_definition_condition_values
        CHECK (
            (condition_type = 'VISIT_COUNT'
                AND scope_type = 'GLOBAL'
                AND required_visit_count > 0
                AND target_region_level IS NULL
                AND classification_level IS NULL
                AND classification_codes IS NULL
                AND required_progress_percent IS NULL)
            OR
            (condition_type = 'DISTINCT_REGION_COUNT'
                AND scope_type = 'GLOBAL'
                AND required_visit_count > 0
                AND target_region_level IS NOT NULL
                AND classification_level IS NULL
                AND classification_codes IS NULL
                AND required_progress_percent IS NULL)
            OR
            (condition_type = 'CLASSIFICATION_VISIT_COUNT'
                AND scope_type = 'GLOBAL'
                AND required_visit_count > 0
                AND target_region_level IS NULL
                AND classification_level IS NOT NULL
                AND classification_codes IS NOT NULL
                AND required_progress_percent IS NULL)
            OR
            (condition_type = 'REGION_PROGRESS_PERCENT'
                AND scope_type = 'REGION'
                AND required_visit_count IS NULL
                AND target_region_level IS NULL
                AND classification_level IS NULL
                AND classification_codes IS NULL
                AND required_progress_percent > 0
                AND required_progress_percent <= 100)
        );

ALTER TABLE actor_title
    ADD COLUMN verification_attempt_id BIGINT UNSIGNED NULL AFTER title_definition_id,
    ADD CONSTRAINT fk_actor_title_verification_attempt
        FOREIGN KEY (verification_attempt_id) REFERENCES verification_attempt (id)
        ON UPDATE RESTRICT ON DELETE SET NULL,
    ADD INDEX ix_actor_title_verification_attempt (actor_id, verification_attempt_id);

INSERT INTO title_definition (
    code, name, description, scope_type, region_id,
    condition_type, target_region_level, classification_level, classification_codes,
    required_visit_count, required_progress_percent, display_order, active
) VALUES
    ('FIRST_STEP', '첫 걸음', '첫 번째 스탬프를 획득하세요.', 'GLOBAL', NULL,
     'VISIT_COUNT', NULL, NULL, NULL, 1, NULL, 1, TRUE),
    ('TRAVEL_RECORDER', '여행 기록가', '서로 다른 관광지에서 스탬프 10개를 획득하세요.', 'GLOBAL', NULL,
     'VISIT_COUNT', NULL, NULL, NULL, 10, NULL, 2, TRUE),
    ('DANYEOGAM_RECORDER', '다녀감 기록가', '서로 다른 관광지에서 스탬프 50개를 획득하세요.', 'GLOBAL', NULL,
     'VISIT_COUNT', NULL, NULL, NULL, 50, NULL, 3, TRUE),
    ('EIGHT_PROVINCE_TRAVELER', '팔도 여행자', '서로 다른 광역지역 8곳에서 스탬프를 획득하세요.', 'GLOBAL', NULL,
     'DISTINCT_REGION_COUNT', 'PROVINCE', NULL, NULL, 8, NULL, 4, TRUE),
    ('EVERY_CORNER_EXPLORER', '구석구석 탐험가', '서로 다른 시군구 10곳에서 스탬프를 획득하세요.', 'GLOBAL', NULL,
     'DISTINCT_REGION_COUNT', 'CITY_COUNTY', NULL, NULL, 10, NULL, 5, TRUE),
    ('HISTORY_FOOTPRINT', '역사 발자국', '역사유적 관광지 15곳에서 스탬프를 획득하세요.', 'GLOBAL', NULL,
     'CLASSIFICATION_VISIT_COUNT', NULL, 2, 'HS01', 15, NULL, 6, TRUE),
    ('CULTURE_COLLECTOR', '문화 수집가', '박물관·기념관·미술관 및 화랑 8곳에서 스탬프를 획득하세요.', 'GLOBAL', NULL,
     'CLASSIFICATION_VISIT_COUNT', NULL, 3, 'VE070100,VE070200,VE070600', 8, NULL, 7, TRUE)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    scope_type = VALUES(scope_type),
    region_id = VALUES(region_id),
    condition_type = VALUES(condition_type),
    target_region_level = VALUES(target_region_level),
    classification_level = VALUES(classification_level),
    classification_codes = VALUES(classification_codes),
    required_visit_count = VALUES(required_visit_count),
    required_progress_percent = VALUES(required_progress_percent),
    display_order = VALUES(display_order),
    active = VALUES(active);

INSERT INTO title_definition (
    code, name, description, scope_type, region_id,
    condition_type, target_region_level, classification_level, classification_codes,
    required_visit_count, required_progress_percent, display_order, active
)
SELECT CONCAT('REGION_MASTER:', region.code),
       CONCAT(region.name, ' 터줏대감'),
       CONCAT(region.name, ' 스탬프 진행률 20%를 달성하세요.'),
       'REGION', region.id,
       'REGION_PROGRESS_PERCENT', NULL, NULL, NULL,
       NULL, 20.00, 100 + region.id, TRUE
FROM region
WHERE region.region_level = 'PROVINCE'
  AND region.active = TRUE
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    region_id = VALUES(region_id),
    condition_type = VALUES(condition_type),
    required_visit_count = VALUES(required_visit_count),
    required_progress_percent = VALUES(required_progress_percent),
    display_order = VALUES(display_order),
    active = VALUES(active);

UPDATE schema_metadata
SET version = '1.4.0',
    description = 'Title conditions, regional title definitions, and idempotent title awards';
