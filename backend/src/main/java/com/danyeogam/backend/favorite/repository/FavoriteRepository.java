package com.danyeogam.backend.favorite.repository;

import java.util.List;

import com.danyeogam.backend.favorite.domain.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByActorIdAndTouristSpotId(Long actorId, Long touristSpotId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    long deleteByActorIdAndTouristSpotId(Long actorId, Long touristSpotId);

    @Query(value = """
            SELECT ts.id AS touristSpotId,
                   ts.name AS name,
                   ts.thumbnail_url AS thumbnailUrl,
                   CASE WHEN v.id IS NULL THEN 'NOT_VISITED' ELSE 'VISITED' END AS visitState
            FROM tourist_spot_favorite favorite
            JOIN tourist_spot ts
              ON ts.id = favorite.tourist_spot_id
             AND ts.active = TRUE
            LEFT JOIN visit v
              ON v.tourist_spot_id = ts.id
             AND v.actor_id = :actorId
            WHERE favorite.actor_id = :actorId
            ORDER BY favorite.created_at DESC, favorite.id DESC
            """, nativeQuery = true)
    List<FavoriteItemProjection> findFavoriteItems(@Param("actorId") Long actorId);
}
