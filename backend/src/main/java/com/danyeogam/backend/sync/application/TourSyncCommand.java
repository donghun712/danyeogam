package com.danyeogam.backend.sync.application;

public record TourSyncCommand(
        String areaCode,
        int pageSize,
        int maxPages,
        boolean hydrateDetails
) {
    public TourSyncCommand {
        if (pageSize < 1 || pageSize > 1000) {
            throw new IllegalArgumentException("pageSize는 1 이상 1000 이하여야 합니다.");
        }
        if (maxPages < 1) {
            throw new IllegalArgumentException("maxPages는 1 이상이어야 합니다.");
        }
        if (areaCode != null && areaCode.isBlank()) {
            areaCode = null;
        }
    }
}
