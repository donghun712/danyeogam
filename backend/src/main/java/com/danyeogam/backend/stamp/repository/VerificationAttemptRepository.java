package com.danyeogam.backend.stamp.repository;

import java.time.Instant;
import java.util.Optional;

import com.danyeogam.backend.stamp.domain.VerificationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationAttemptRepository extends JpaRepository<VerificationAttempt, Long> {

    Optional<VerificationAttempt> findByActorIdAndIdempotencyKey(Long actorId, String idempotencyKey);

    long countByActorIdAndCreatedAtGreaterThanEqual(Long actorId, Instant since);
}
