package com.danyeogam.backend.collection.api;

import java.time.Instant;

public record CollectionItemResponse(
        long touristSpotId,
        String name,
        String visitState,
        Instant verifiedAt,
        String thumbnailUrl
) {
}
