-- 다녀감 MySQL 데이터베이스 스키마
-- 대상: MySQL 8.4 / InnoDB / WGS84(SRID 4326)
-- 원본 설계: ../다녀감_백엔드_설계서.md
--
-- 이 파일은 스키마만 생성한다.
-- TourAPI 지역/관광지 데이터, 스탬프 대상 목록, API 키는 포함하지 않는다.
-- 정확한 사용자 GPS 좌표는 인증 판정 후 저장하지 않는다.

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE DATABASE IF NOT EXISTS danyeogam
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE danyeogam;

CREATE TABLE IF NOT EXISTS schema_metadata (
    version         VARCHAR(30)  NOT NULL,
    description     VARCHAR(255) NOT NULL,
    applied_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (version)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS region (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code            VARCHAR(20)     NOT NULL,
    name            VARCHAR(80)     NOT NULL,
    parent_id       BIGINT UNSIGNED NULL,
    region_level    VARCHAR(20)     NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                      ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_region_code UNIQUE (code),
    CONSTRAINT fk_region_parent
        FOREIGN KEY (parent_id) REFERENCES region (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_region_level
        CHECK (region_level IN ('PROVINCE', 'CITY_COUNTY')),
    INDEX ix_region_parent_active (parent_id, active)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS app_user (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    provider        VARCHAR(30)     NOT NULL,
    provider_user_id VARCHAR(191)   NOT NULL,
    display_name    VARCHAR(100)    NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                      ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at      DATETIME(6)     NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_app_user_provider_subject
        UNIQUE (provider, provider_user_id),
    CONSTRAINT ck_app_user_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS actor (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    actor_type      VARCHAR(20)     NOT NULL,
    user_id         BIGINT UNSIGNED NULL,
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                      ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at      DATETIME(6)     NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_actor_user UNIQUE (user_id),
    CONSTRAINT fk_actor_user
        FOREIGN KEY (user_id) REFERENCES app_user (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_actor_type
        CHECK (actor_type IN ('ANONYMOUS', 'USER')),
    CONSTRAINT ck_actor_user_consistency
        CHECK (
            (actor_type = 'ANONYMOUS' AND user_id IS NULL)
            OR (actor_type = 'USER' AND user_id IS NOT NULL)
        ),
    INDEX ix_actor_type_deleted (actor_type, deleted_at)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS anonymous_session (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    actor_id        BIGINT UNSIGNED NOT NULL,
    token_hash      BINARY(32)      NOT NULL,
    expires_at      DATETIME(6)     NOT NULL,
    last_seen_at    DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    revoked_at      DATETIME(6)     NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_anonymous_session_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_anonymous_session_actor
        FOREIGN KEY (actor_id) REFERENCES actor (id)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    INDEX ix_anonymous_session_actor (actor_id),
    INDEX ix_anonymous_session_expiry (expires_at, revoked_at)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS sync_run (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    job_type            VARCHAR(30)     NOT NULL,
    status              VARCHAR(20)     NOT NULL,
    started_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    finished_at         DATETIME(6)     NULL,
    requested_count     INT UNSIGNED    NOT NULL DEFAULT 0,
    processed_count     INT UNSIGNED    NOT NULL DEFAULT 0,
    inserted_count      INT UNSIGNED    NOT NULL DEFAULT 0,
    updated_count       INT UNSIGNED    NOT NULL DEFAULT 0,
    deactivated_count   INT UNSIGNED    NOT NULL DEFAULT 0,
    failed_count        INT UNSIGNED    NOT NULL DEFAULT 0,
    request_quota_count INT UNSIGNED    NOT NULL DEFAULT 0,
    summary             VARCHAR(1000)   NULL,
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT ck_sync_run_job_type
        CHECK (job_type IN ('INITIAL_LOAD', 'SUMMARY_SYNC', 'DETAIL_HYDRATION', 'MONTHLY_SYNC', 'RETRY_FAILED')),
    CONSTRAINT ck_sync_run_status
        CHECK (status IN ('RUNNING', 'SUCCEEDED', 'PARTIALLY_SUCCEEDED', 'FAILED', 'CANCELLED')),
    INDEX ix_sync_run_job_started (job_type, started_at),
    INDEX ix_sync_run_status_started (status, started_at)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS tourist_spot_staging (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    sync_run_id         BIGINT UNSIGNED NOT NULL,
    source              VARCHAR(30)     NOT NULL,
    source_content_id   VARCHAR(40)     NOT NULL,
    source_content_type_id VARCHAR(20)  NULL,
    name                VARCHAR(200)    NULL,
    road_address        VARCHAR(300)    NULL,
    lot_address         VARCHAR(300)    NULL,
    latitude            DECIMAL(10, 7)  NULL,
    longitude           DECIMAL(10, 7)  NULL,
    coordinate_source   VARCHAR(30)     NULL,
    coordinate_quality  VARCHAR(20)     NOT NULL DEFAULT 'UNKNOWN',
    processing_status   VARCHAR(30)     NOT NULL DEFAULT 'RECEIVED',
    data_hash            CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    raw_payload          JSON            NOT NULL,
    error_code           VARCHAR(80)     NULL,
    error_summary        VARCHAR(1000)   NULL,
    created_at           DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                          ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_staging_run_source_content
        UNIQUE (sync_run_id, source, source_content_id),
    CONSTRAINT fk_staging_sync_run
        FOREIGN KEY (sync_run_id) REFERENCES sync_run (id)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT ck_staging_latitude
        CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_staging_longitude
        CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_staging_coordinate_quality
        CHECK (coordinate_quality IN ('VERIFIED', 'SUSPECT', 'UNKNOWN')),
    CONSTRAINT ck_staging_processing_status
        CHECK (processing_status IN ('RECEIVED', 'VALIDATING', 'READY', 'PROMOTED', 'REJECTED', 'RETRY_WAIT')),
    INDEX ix_staging_status_created (processing_status, created_at),
    INDEX ix_staging_source_content (source, source_content_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS tourist_spot (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    source              VARCHAR(30)     NOT NULL,
    source_content_id   VARCHAR(40)     NOT NULL,
    source_content_type_id VARCHAR(20)  NULL,
    classification_level1 VARCHAR(20)  NULL,
    classification_level2 VARCHAR(20)  NULL,
    classification_level3 VARCHAR(20)  NULL,
    name                VARCHAR(200)    NOT NULL,
    spot_type           VARCHAR(30)     NOT NULL DEFAULT 'GENERAL',
    stamp_enabled       BOOLEAN         NOT NULL DEFAULT FALSE,
    stamp_radius_meters INT UNSIGNED    NULL,
    region_id           BIGINT UNSIGNED NOT NULL,
    road_address        VARCHAR(300)    NULL,
    lot_address         VARCHAR(300)    NULL,
    location            POINT NOT NULL SRID 4326,
    coordinate_source   VARCHAR(30)     NOT NULL,
    coordinate_quality  VARCHAR(20)     NOT NULL DEFAULT 'UNKNOWN',
    overview            MEDIUMTEXT      NULL,
    thumbnail_url       VARCHAR(1000)   NULL,
    original_image_url  VARCHAR(1000)   NULL,
    tel                 VARCHAR(100)    NULL,
    homepage_url        VARCHAR(1000)   NULL,
    source_modified_at  DATETIME(6)     NULL,
    detail_hydrated_at  DATETIME(6)     NULL,
    operating_hours     TEXT            NULL,
    closed_days         TEXT            NULL,
    parking_note        TEXT            NULL,
    parking_fee_note    TEXT            NULL,
    stroller_rental_note TEXT           NULL,
    pet_allowed_note    TEXT            NULL,
    intro_hydrated_at   DATETIME(6)     NULL,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    data_hash           CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                          ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_tourist_spot_source_content
        UNIQUE (source, source_content_id),
    CONSTRAINT fk_tourist_spot_region
        FOREIGN KEY (region_id) REFERENCES region (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_tourist_spot_type
        CHECK (spot_type IN ('STAMP_TARGET', 'GENERAL')),
    CONSTRAINT ck_tourist_spot_stamp_radius
        CHECK (stamp_radius_meters IS NULL OR stamp_radius_meters > 0),
    CONSTRAINT ck_tourist_spot_coordinate_source
        CHECK (coordinate_source IN ('TOUR_API', 'KAKAO_GEOCODE', 'MANUAL')),
    CONSTRAINT ck_tourist_spot_coordinate_quality
        CHECK (coordinate_quality IN ('VERIFIED', 'SUSPECT', 'UNKNOWN')),
    SPATIAL INDEX sx_tourist_spot_location (location),
    INDEX ix_tourist_spot_region_active (region_id, active),
    INDEX ix_tourist_spot_stamp_active (stamp_enabled, active),
    INDEX ix_tourist_spot_classification (
        source, active, source_content_type_id,
        classification_level1, classification_level2, classification_level3
    ),
    INDEX ix_tourist_spot_region_stamp_active (region_id, stamp_enabled, active),
    INDEX ix_tourist_spot_modified (source_modified_at)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS tourist_spot_image (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    tourist_spot_id     BIGINT UNSIGNED NOT NULL,
    url                 VARCHAR(1000)   NOT NULL,
    alt_text            VARCHAR(300)    NULL,
    sort_order          INT UNSIGNED    NOT NULL DEFAULT 0,
    copyright_type      VARCHAR(30)     NULL,
    source_image_id     VARCHAR(100)    NULL,
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                          ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_tourist_spot_image_spot
        FOREIGN KEY (tourist_spot_id) REFERENCES tourist_spot (id)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    INDEX ix_tourist_spot_image_order (tourist_spot_id, sort_order)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS verification_attempt (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    actor_id            BIGINT UNSIGNED NOT NULL,
    tourist_spot_id     BIGINT UNSIGNED NOT NULL,
    idempotency_key     CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    result              VARCHAR(40)     NOT NULL,
    distance_meters     DECIMAL(10, 2)  NULL,
    accuracy_meters     DECIMAL(10, 2)  NOT NULL,
    measured_at         DATETIME(6)     NOT NULL,
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_verification_actor_idempotency
        UNIQUE (actor_id, idempotency_key),
    CONSTRAINT fk_verification_actor
        FOREIGN KEY (actor_id) REFERENCES actor (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_verification_spot
        FOREIGN KEY (tourist_spot_id) REFERENCES tourist_spot (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_verification_result
        CHECK (result IN (
            'VERIFIED_NEW',
            'VERIFIED_ALREADY_ACQUIRED',
            'OUT_OF_RANGE',
            'GPS_ACCURACY_INSUFFICIENT',
            'LOCATION_STALE',
            'STAMP_DISABLED'
        )),
    CONSTRAINT ck_verification_distance
        CHECK (distance_meters IS NULL OR distance_meters >= 0),
    CONSTRAINT ck_verification_accuracy
        CHECK (accuracy_meters >= 0),
    INDEX ix_verification_actor_created (actor_id, created_at),
    INDEX ix_verification_spot_created (tourist_spot_id, created_at),
    INDEX ix_verification_result_created (result, created_at)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS visit (
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    actor_id                BIGINT UNSIGNED NOT NULL,
    tourist_spot_id         BIGINT UNSIGNED NOT NULL,
    verification_attempt_id BIGINT UNSIGNED NOT NULL,
    verified_at             DATETIME(6)     NOT NULL,
    distance_meters         DECIMAL(10, 2)  NOT NULL,
    created_at              DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_visit_actor_spot UNIQUE (actor_id, tourist_spot_id),
    CONSTRAINT uk_visit_verification_attempt UNIQUE (verification_attempt_id),
    CONSTRAINT fk_visit_actor
        FOREIGN KEY (actor_id) REFERENCES actor (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_visit_spot
        FOREIGN KEY (tourist_spot_id) REFERENCES tourist_spot (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_visit_verification_attempt
        FOREIGN KEY (verification_attempt_id) REFERENCES verification_attempt (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_visit_distance CHECK (distance_meters >= 0),
    INDEX ix_visit_actor_verified (actor_id, verified_at),
    INDEX ix_visit_spot_verified (tourist_spot_id, verified_at)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS tourist_spot_favorite (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    actor_id            BIGINT UNSIGNED NOT NULL,
    tourist_spot_id     BIGINT UNSIGNED NOT NULL,
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_favorite_actor_spot UNIQUE (actor_id, tourist_spot_id),
    CONSTRAINT fk_favorite_actor
        FOREIGN KEY (actor_id) REFERENCES actor (id)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fk_favorite_spot
        FOREIGN KEY (tourist_spot_id) REFERENCES tourist_spot (id)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    INDEX ix_favorite_actor_created (actor_id, created_at),
    INDEX ix_favorite_spot (tourist_spot_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS title_definition (
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code                    VARCHAR(50)     NOT NULL,
    name                    VARCHAR(100)    NOT NULL,
    description             VARCHAR(500)    NULL,
    scope_type              VARCHAR(20)     NOT NULL,
    region_id               BIGINT UNSIGNED NULL,
    required_visit_count    INT UNSIGNED    NOT NULL,
    display_order           INT UNSIGNED    NOT NULL DEFAULT 0,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                              ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_title_definition_code UNIQUE (code),
    CONSTRAINT fk_title_definition_region
        FOREIGN KEY (region_id) REFERENCES region (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_title_definition_scope
        CHECK (scope_type IN ('REGION', 'GLOBAL')),
    CONSTRAINT ck_title_definition_region_consistency
        CHECK (
            (scope_type = 'REGION' AND region_id IS NOT NULL)
            OR (scope_type = 'GLOBAL' AND region_id IS NULL)
        ),
    CONSTRAINT ck_title_definition_required_count
        CHECK (required_visit_count > 0),
    INDEX ix_title_definition_region_active (region_id, active, display_order)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS actor_title (
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    actor_id                BIGINT UNSIGNED NOT NULL,
    title_definition_id     BIGINT UNSIGNED NOT NULL,
    awarded_at              DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_actor_title UNIQUE (actor_id, title_definition_id),
    CONSTRAINT fk_actor_title_actor
        FOREIGN KEY (actor_id) REFERENCES actor (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_actor_title_definition
        FOREIGN KEY (title_definition_id) REFERENCES title_definition (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    INDEX ix_actor_title_awarded (actor_id, awarded_at)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS sync_error (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    sync_run_id         BIGINT UNSIGNED NOT NULL,
    staging_id          BIGINT UNSIGNED NULL,
    source              VARCHAR(30)     NULL,
    source_content_id   VARCHAR(40)     NULL,
    error_code          VARCHAR(80)     NOT NULL,
    error_summary       VARCHAR(1000)   NOT NULL,
    retryable           BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_sync_error_run
        FOREIGN KEY (sync_run_id) REFERENCES sync_run (id)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fk_sync_error_staging
        FOREIGN KEY (staging_id) REFERENCES tourist_spot_staging (id)
        ON UPDATE RESTRICT ON DELETE SET NULL,
    INDEX ix_sync_error_run_created (sync_run_id, created_at),
    INDEX ix_sync_error_retryable (retryable, created_at),
    INDEX ix_sync_error_source_content (source, source_content_id)
) ENGINE = InnoDB;

INSERT INTO schema_metadata (version, description)
VALUES ('2.0.0', 'Danyeogam schema with TourAPI classification fields')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- 의도적으로 생성하지 않은 데이터/테이블
-- 1. region 및 tourist_spot 데이터: TourAPI 연동 후 적재
-- 2. 스탬프 대상 목록: HS01, HS02, VE070100, VE070200, VE070600 선정 정책으로 적재
-- 3. GPS 이동 경로: P2이며 명시적 동의·보관정책 확정 전에는 저장 금지
-- 4. GPS 기본 반경/정확도/유효시간: DB 상수가 아니라 운영 환경변수로 주입
