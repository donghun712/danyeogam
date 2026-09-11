package com.danyeogam.backend.visit.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.danyeogam.backend.title.repository.RegionTitleProgressProjection;
import com.danyeogam.backend.visit.domain.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    long countByActorId(Long actorId);

    boolean existsByActorIdAndTouristSpotId(Long actorId, Long touristSpotId);

    Optional<Visit> findByActorIdAndTouristSpotId(Long actorId, Long touristSpotId);

    @Query("""
            SELECT visit.touristSpotId
            FROM Visit visit
            WHERE visit.actorId = :actorId
              AND visit.touristSpotId IN :spotIds
            """)
    List<Long> findVisitedSpotIds(
            @Param("actorId") Long actorId,
            @Param("spotIds") Collection<Long> spotIds
    );

    @Query(value = """
            SELECT COUNT(DISTINCT COALESCE(region.parent_id, region.id))
            FROM visit visit
            JOIN tourist_spot spot ON spot.id = visit.tourist_spot_id
            JOIN region region ON region.id = spot.region_id
            WHERE visit.actor_id = :actorId
            """, nativeQuery = true)
    long countDistinctVisitedProvinces(@Param("actorId") Long actorId);

    @Query(value = """
            SELECT COUNT(DISTINCT region.id)
            FROM visit visit
            JOIN tourist_spot spot ON spot.id = visit.tourist_spot_id
            JOIN region region ON region.id = spot.region_id
            WHERE visit.actor_id = :actorId
              AND region.region_level = 'CITY_COUNTY'
            """, nativeQuery = true)
    long countDistinctVisitedCityCounties(@Param("actorId") Long actorId);

    @Query(value = """
            SELECT COUNT(DISTINCT visit.tourist_spot_id)
            FROM visit visit
            JOIN tourist_spot spot ON spot.id = visit.tourist_spot_id
            WHERE visit.actor_id = :actorId
              AND spot.classification_level2 IN (:codes)
            """, nativeQuery = true)
    long countDistinctVisitedSpotsByClassificationLevel2(
            @Param("actorId") Long actorId,
            @Param("codes") Collection<String> codes
    );

    @Query(value = """
            SELECT COUNT(DISTINCT visit.tourist_spot_id)
            FROM visit visit
            JOIN tourist_spot spot ON spot.id = visit.tourist_spot_id
            WHERE visit.actor_id = :actorId
              AND spot.classification_level3 IN (:codes)
            """, nativeQuery = true)
    long countDistinctVisitedSpotsByClassificationLevel3(
            @Param("actorId") Long actorId,
            @Param("codes") Collection<String> codes
    );

    @Query(value = """
            SELECT province.id AS regionId,
                   COUNT(DISTINCT visit.tourist_spot_id) AS visitedCount,
                   COUNT(DISTINCT spot.id) AS totalCount
            FROM region province
            LEFT JOIN region scope_region
              ON scope_region.id = province.id
              OR (scope_region.parent_id = province.id AND scope_region.active = TRUE)
            LEFT JOIN tourist_spot spot
              ON spot.region_id = scope_region.id
             AND spot.active = TRUE
             AND spot.stamp_enabled = TRUE
             AND spot.spot_type = 'STAMP_TARGET'
            LEFT JOIN visit visit
              ON visit.tourist_spot_id = spot.id
             AND visit.actor_id = :actorId
            WHERE province.region_level = 'PROVINCE'
              AND province.active = TRUE
            GROUP BY province.id
            """, nativeQuery = true)
    List<RegionTitleProgressProjection> findProvinceProgress(@Param("actorId") Long actorId);
}
