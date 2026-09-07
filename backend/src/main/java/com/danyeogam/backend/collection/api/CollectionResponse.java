package com.danyeogam.backend.collection.api;

import java.util.List;

public record CollectionResponse(
        CollectionRegionResponse region,
        List<CollectionItemResponse> items
) {
    public CollectionResponse {
        items = List.copyOf(items);
    }
}
