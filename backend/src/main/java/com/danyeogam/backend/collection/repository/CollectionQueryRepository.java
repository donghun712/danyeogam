package com.danyeogam.backend.collection.repository;

import java.util.List;

import com.danyeogam.backend.touristspot.domain.TouristSpot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollectionQueryRepository extends JpaRepository<TouristSpot, Long> {

    @Query(value = """
            SELECT ts.id AS touristSpotId,
                   ts.name AS name,
                   CASE WHEN v.id IS NULL THEN 'NOT_VISITED' ELSE 'VISITED' END AS visitState,
                   v.verified_at AS verifiedAt,
                   ts.thumbnail_url AS thumbnailUrl
            FROM tourist_spot ts
            JOIN region spot_region ON spot_region.id = ts.region_id
            LEFT JOIN visit v
              ON v.tourist_spot_id = ts.id
             AND v.actor_id = :actorId
            WHERE ts.active = TRUE
              AND ts.stamp_enabled = TRUE
              AND ts.spot_type = 'STAMP_TARGET'
              AND (spot_region.id = :regionId OR spot_region.parent_id = :regionId)
              AND (
                    :status = 'ALL'
                    OR (:status = 'VISITED' AND v.id IS NOT NULL)
                    OR (:status = 'NOT_VISITED' AND v.id IS NULL)
                  )
            ORDER BY ts.name, ts.id
            """, nativeQuery = true)
    List<CollectionItemProjection> findCollectionItems(
            @Param("actorId") Long actorId,
            @Param("regionId") Long regionId,
            @Param("status") String status
    );

    @Query(value = """
            SELECT province.code AS code,
                   province.name AS name,
                   COUNT(DISTINCT v.tourist_spot_id) AS visitedCount,
                   COUNT(DISTINCT ts.id) AS totalCount
            FROM region province
            LEFT JOIN region scope_region
              ON scope_region.id = province.id
              OR (scope_region.parent_id = province.id AND scope_region.active = TRUE)
            LEFT JOIN tourist_spot ts
              ON ts.region_id = scope_region.id
             AND ts.active = TRUE
             AND ts.stamp_enabled = TRUE
             AND ts.spot_type = 'STAMP_TARGET'
            LEFT JOIN visit v
              ON v.tourist_spot_id = ts.id
             AND v.actor_id = :actorId
            WHERE province.parent_id IS NULL
              AND province.active = TRUE
            GROUP BY province.id, province.code, province.name
            ORDER BY province.name, province.id
            """, nativeQuery = true)
    List<CollectionSummaryProjection> findCollectionSummary(@Param("actorId") Long actorId);
}
