package com.danyeogam.backend.favorite.api;

import java.util.List;

public record FavoriteListResponse(List<FavoriteItemResponse> items) {
    public FavoriteListResponse {
        items = List.copyOf(items);
    }
}
