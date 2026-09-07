package com.danyeogam.backend.sync.domain;

public enum SyncRunStatus {
    RUNNING,
    SUCCEEDED,
    PARTIALLY_SUCCEEDED,
    FAILED,
    CANCELLED
}
