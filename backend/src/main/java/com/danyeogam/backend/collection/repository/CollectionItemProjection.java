package com.danyeogam.backend.collection.repository;

import java.time.Instant;

public interface CollectionItemProjection {
    Long getTouristSpotId();
    String getName();
    String getVisitState();
    Instant getVerifiedAt();
    String getThumbnailUrl();
}
