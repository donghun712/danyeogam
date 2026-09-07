package com.danyeogam.backend.identity.repository;

import java.time.Instant;
import java.util.Optional;

import com.danyeogam.backend.identity.domain.AnonymousSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnonymousSessionRepository extends JpaRepository<AnonymousSession, Long> {

    @Query("""
            SELECT session
            FROM AnonymousSession session
            JOIN FETCH session.actor actor
            WHERE session.tokenHash = :tokenHash
              AND session.revokedAt IS NULL
              AND session.expiresAt > :now
              AND actor.deletedAt IS NULL
            """)
    Optional<AnonymousSession> findValid(
            @Param("tokenHash") byte[] tokenHash,
            @Param("now") Instant now
    );
}
