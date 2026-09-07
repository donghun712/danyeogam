package com.danyeogam.backend.collection.api;

import java.util.List;

public record CollectionSummaryResponse(List<CollectionSummaryItemResponse> regions) {
    public CollectionSummaryResponse {
        regions = List.copyOf(regions);
    }
}
