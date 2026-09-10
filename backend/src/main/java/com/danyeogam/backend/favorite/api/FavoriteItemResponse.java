package com.danyeogam.backend.favorite.api;

public record FavoriteItemResponse(
        long touristSpotId,
        String name,
        String thumbnailUrl,
        String visitState
) {
}
