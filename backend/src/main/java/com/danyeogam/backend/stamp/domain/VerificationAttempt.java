package com.danyeogam.backend.stamp.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "verification_attempt")
public class VerificationAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "tourist_spot_id", nullable = false)
    private Long touristSpotId;

    @Column(name = "idempotency_key", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private VerificationResult result;

    @Column(name = "distance_meters", precision = 10, scale = 2)
    private BigDecimal distanceMeters;

    @Column(name = "accuracy_meters", nullable = false, precision = 10, scale = 2)
    private BigDecimal accuracyMeters;

    @Column(name = "measured_at", nullable = false)
    private Instant measuredAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected VerificationAttempt() {
    }

    public VerificationAttempt(
            long actorId,
            long touristSpotId,
            String idempotencyKey,
            VerificationResult result,
            BigDecimal distanceMeters,
            BigDecimal accuracyMeters,
            Instant measuredAt
    ) {
        this.actorId = actorId;
        this.touristSpotId = touristSpotId;
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        this.result = Objects.requireNonNull(result, "result");
        this.distanceMeters = distanceMeters;
        this.accuracyMeters = Objects.requireNonNull(accuracyMeters, "accuracyMeters");
        this.measuredAt = Objects.requireNonNull(measuredAt, "measuredAt");
    }

    public Long getId() { return id; }
    public Long getActorId() { return actorId; }
    public Long getTouristSpotId() { return touristSpotId; }
    public VerificationResult getResult() { return result; }
    public BigDecimal getDistanceMeters() { return distanceMeters; }
}
