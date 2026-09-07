package com.danyeogam.backend.identity.repository;

import com.danyeogam.backend.identity.domain.Actor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ActorRepository extends JpaRepository<Actor, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT actor FROM Actor actor WHERE actor.id = :id AND actor.deletedAt IS NULL")
    Optional<Actor> findActiveByIdForUpdate(@Param("id") Long id);
}
