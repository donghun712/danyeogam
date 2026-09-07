package com.danyeogam.backend.sync.domain;

public enum SyncJobType {
    INITIAL_LOAD,
    SUMMARY_SYNC,
    DETAIL_HYDRATION,
    MONTHLY_SYNC,
    RETRY_FAILED
}
