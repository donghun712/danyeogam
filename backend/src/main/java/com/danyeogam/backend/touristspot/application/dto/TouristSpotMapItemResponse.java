package com.danyeogam.backend.touristspot.application.dto;

public record TouristSpotMapItemResponse(
        long id,
        String name,
        PositionResponse position,
        String type,
        boolean stampEnabled,
        String visitState,
        String thumbnailUrl
) {
}
