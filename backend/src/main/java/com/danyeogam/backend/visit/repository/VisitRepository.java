package com.danyeogam.backend.visit.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.danyeogam.backend.visit.domain.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitRepository extends JpaRepository<Visit, Long> {

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
}
