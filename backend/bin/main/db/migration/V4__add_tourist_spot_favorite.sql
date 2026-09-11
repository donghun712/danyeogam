CREATE TABLE tourist_spot_favorite (
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
