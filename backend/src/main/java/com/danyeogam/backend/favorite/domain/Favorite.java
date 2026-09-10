package com.danyeogam.backend.favorite.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tourist_spot_favorite")
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "tourist_spot_id", nullable = false)
    private Long touristSpotId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Favorite() {
    }

    public Favorite(long actorId, long touristSpotId) {
        this.actorId = actorId;
        this.touristSpotId = touristSpotId;
    }
}
