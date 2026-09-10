package com.danyeogam.backend.sync.application;

import com.danyeogam.backend.sync.domain.SyncRunStatus;

public record TourSyncResult(
        long syncRunId,
        SyncRunStatus status,
        int requestedCount,
        int processedCount,
        int insertedCount,
        int updatedCount,
        int deactivatedCount,
        int failedCount,
        int apiRequestCount
) {
}
