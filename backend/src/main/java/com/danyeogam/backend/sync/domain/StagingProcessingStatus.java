package com.danyeogam.backend.sync.domain;

public enum StagingProcessingStatus {
    RECEIVED,
    VALIDATING,
    READY,
    PROMOTED,
    REJECTED,
    RETRY_WAIT
}
