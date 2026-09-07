package com.danyeogam.backend.visit.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "visit")
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "tourist_spot_id", nullable = false)
    private Long touristSpotId;

    @Column(name = "verification_attempt_id", nullable = false)
    private Long verificationAttemptId;

    @Column(name = "verified_at", nullable = false)
    private Instant verifiedAt;

    @Column(name = "distance_meters", nullable = false, precision = 10, scale = 2)
    private BigDecimal distanceMeters;

    protected Visit() {
    }

    public Visit(
            long actorId,
            long touristSpotId,
            long verificationAttemptId,
            Instant verifiedAt,
            BigDecimal distanceMeters
    ) {
        this.actorId = actorId;
        this.touristSpotId = touristSpotId;
        this.verificationAttemptId = verificationAttemptId;
        this.verifiedAt = verifiedAt;
        this.distanceMeters = distanceMeters;
    }

    public Long getTouristSpotId() { return touristSpotId; }
    public Instant getVerifiedAt() { return verifiedAt; }
}
