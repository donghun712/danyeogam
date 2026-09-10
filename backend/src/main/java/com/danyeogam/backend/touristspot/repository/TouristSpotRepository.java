package com.danyeogam.backend.touristspot.repository;

import java.util.Optional;
import java.util.List;

import com.danyeogam.backend.touristspot.domain.TouristSpot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

public interface TouristSpotRepository extends JpaRepository<TouristSpot, Long> {

    Optional<TouristSpot> findBySourceAndSourceContentId(String source, String sourceContentId);

    Optional<TouristSpot> findByIdAndActiveTrue(Long id);

    long countBySource(String source);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE tourist_spot
            SET active = FALSE,
                spot_type = 'GENERAL',
                stamp_enabled = FALSE,
                stamp_radius_meters = NULL
            WHERE source = 'TOUR_API'
              AND active = TRUE
              AND NOT (
                    (source_content_type_id = '12'
                     AND COALESCE(classification_level1, '') = 'HS'
                     AND COALESCE(classification_level2, '') IN ('HS01', 'HS02'))
                    OR
                    (source_content_type_id IN ('12', '14')
                     AND COALESCE(classification_level1, '') = 'VE'
                     AND COALESCE(classification_level2, '') = 'VE07'
                     AND COALESCE(classification_level3, '') IN ('VE070100', 'VE070200', 'VE070600'))
                  )
            """, nativeQuery = true)
    int deactivateOutsideSelectionPolicy();

    @Query(value = """
            SELECT ts.id AS id,
                   ts.name AS name,
                   ts.spot_type AS spotType,
                   ts.stamp_enabled AS stampEnabled,
                   ROUND(ST_Latitude(ts.location), 7) AS latitude,
                   ROUND(ST_Longitude(ts.location), 7) AS longitude,
                   ts.thumbnail_url AS thumbnailUrl
            FROM tourist_spot ts FORCE INDEX (sx_tourist_spot_location)
            WHERE ts.active = TRUE
              AND MBRContains(
                    ST_GeomFromText(:polygonWkt, 4326, 'axis-order=long-lat'),
                    ts.location
                  )
            ORDER BY ts.id
            LIMIT :limit
            """, nativeQuery = true)
    List<TouristSpotMapProjection> findActiveWithinBounds(
            @Param("polygonWkt") String polygonWkt,
            @Param("limit") int limit
    );

    @Query(value = """
            SELECT ts.id AS id,
                   ts.name AS name,
                   ts.spot_type AS spotType,
                   ts.stamp_enabled AS stampEnabled,
                   ROUND(ST_Latitude(ts.location), 7) AS latitude,
                   ROUND(ST_Longitude(ts.location), 7) AS longitude,
                   ts.thumbnail_url AS thumbnailUrl
            FROM tourist_spot ts FORCE INDEX (sx_tourist_spot_location)
            WHERE ts.active = TRUE
              AND ts.spot_type IN (:types)
              AND MBRContains(
                    ST_GeomFromText(:polygonWkt, 4326, 'axis-order=long-lat'),
                    ts.location
                  )
            ORDER BY ts.id
            LIMIT :limit
            """, nativeQuery = true)
    List<TouristSpotMapProjection> findActiveWithinBoundsAndTypes(
            @Param("polygonWkt") String polygonWkt,
            @Param("types") List<String> types,
            @Param("limit") int limit
    );
}
