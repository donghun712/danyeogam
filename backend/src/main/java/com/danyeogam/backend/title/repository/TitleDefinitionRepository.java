package com.danyeogam.backend.title.repository;

import java.util.List;

import com.danyeogam.backend.title.domain.TitleDefinition;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TitleDefinitionRepository extends JpaRepository<TitleDefinition, Long> {

    @EntityGraph(attributePaths = "region")
    List<TitleDefinition> findAllByActiveTrueOrderByDisplayOrderAscIdAsc();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE title_definition definition
            LEFT JOIN region region ON region.id = definition.region_id
            SET definition.active = CASE
                WHEN region.active = TRUE AND region.region_level = 'PROVINCE' THEN TRUE
                ELSE FALSE
            END
            WHERE definition.condition_type = 'REGION_PROGRESS_PERCENT'
            """, nativeQuery = true)
    int alignRegionTitleActivity();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
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
                required_progress_percent = VALUES(required_progress_percent),
                display_order = VALUES(display_order),
                active = TRUE
            """, nativeQuery = true)
    int upsertActiveRegionTitles();
}
