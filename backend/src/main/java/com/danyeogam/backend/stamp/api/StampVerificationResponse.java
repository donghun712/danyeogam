package com.danyeogam.backend.stamp.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record StampVerificationResponse(
        String status,
        long touristSpotId,
        BigDecimal distanceMeters,
        Instant verifiedAt,
        String visitState,
        boolean collectionChanged,
        List<Long> newTitleIds
) {
    public StampVerificationResponse {
        newTitleIds = List.copyOf(newTitleIds);
    }
}
