package com.danyeogam.backend.collection.api;

public record CollectionSummaryItemResponse(
        String code,
        String name,
        long visitedCount,
        long totalCount,
        int progressPercent
) {
}
