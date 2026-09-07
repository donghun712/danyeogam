package com.danyeogam.backend.identity.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "actor")
public class Actor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 20)
    private ActorType actorType;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Actor() {
    }

    private Actor(ActorType actorType) {
        this.actorType = actorType;
    }

    public static Actor anonymous() {
        return new Actor(ActorType.ANONYMOUS);
    }

    public Long getId() {
        return id;
    }

    public ActorType getActorType() {
        return actorType;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
