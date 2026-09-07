package com.danyeogam.backend.touristspot.application.dto;

import java.time.Instant;
import java.util.List;

public record TouristSpotDetailResponse(
        long id,
        String name,
        String type,
        boolean stampEnabled,
        String visitState,
        TouristSpotAddressResponse address,
        PositionResponse position,
        String overview,
        List<TouristSpotImageResponse> images,
        String telephone,
        String homepageUrl,
        NavigationDestinationResponse navigation,
        String dataSource,
        String dataQuality,
        Instant lastSyncedAt
) {
    public TouristSpotDetailResponse {
        images = List.copyOf(images);
    }
}
