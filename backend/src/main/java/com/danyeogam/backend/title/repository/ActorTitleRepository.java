package com.danyeogam.backend.title.repository;

import java.util.List;

import com.danyeogam.backend.title.domain.ActorTitle;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActorTitleRepository extends JpaRepository<ActorTitle, Long> {

    @EntityGraph(attributePaths = "titleDefinition")
    List<ActorTitle> findAllByActorId(Long actorId);

    @Query("""
            SELECT award.titleDefinition.id
            FROM ActorTitle award
            WHERE award.actorId = :actorId
            """)
    List<Long> findTitleDefinitionIdsByActorId(@Param("actorId") Long actorId);

    @Query("""
            SELECT award.titleDefinition.id
            FROM ActorTitle award
            WHERE award.actorId = :actorId
              AND award.verificationAttemptId = :attemptId
            ORDER BY award.titleDefinition.displayOrder, award.titleDefinition.id
            """)
    List<Long> findTitleDefinitionIdsByActorIdAndVerificationAttemptId(
            @Param("actorId") Long actorId,
            @Param("attemptId") Long attemptId
    );
}
