package com.danyeogam.backend.identity.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "anonymous_session")
public class AnonymousSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private Actor actor;

    @Column(name = "token_hash", nullable = false, columnDefinition = "BINARY(32)")
    private byte[] tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected AnonymousSession() {
    }

    public AnonymousSession(Actor actor, byte[] tokenHash, Instant expiresAt, Instant now) {
        this.actor = Objects.requireNonNull(actor, "actor");
        if (tokenHash == null || tokenHash.length != 32) {
            throw new IllegalArgumentException("세션 토큰 해시는 32바이트여야 합니다.");
        }
        this.tokenHash = Arrays.copyOf(tokenHash, tokenHash.length);
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.lastSeenAt = Objects.requireNonNull(now, "now");
        if (!expiresAt.isAfter(now)) {
            throw new IllegalArgumentException("세션 만료 시각은 현재보다 뒤여야 합니다.");
        }
    }

    public Actor getActor() {
        return actor;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void touchIfDue(Instant now, Duration interval) {
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(interval, "interval");
        if (!now.isBefore(lastSeenAt.plus(interval))) {
            lastSeenAt = now;
        }
    }
}
