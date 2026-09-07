package com.danyeogam.backend.sync.domain;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "sync_error")
public class SyncErrorRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sync_run_id", nullable = false)
    private SyncRun syncRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staging_id")
    private TouristSpotStaging staging;

    @Column(length = 30)
    private String source;

    @Column(name = "source_content_id", length = 40)
    private String sourceContentId;

    @Column(name = "error_code", nullable = false, length = 80)
    private String errorCode;

    @Column(name = "error_summary", nullable = false, length = 1000)
    private String errorSummary;

    @Column(nullable = false)
    private boolean retryable;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected SyncErrorRecord() {
    }

    public SyncErrorRecord(
            SyncRun syncRun,
            TouristSpotStaging staging,
            String sourceContentId,
            String errorCode,
            String errorSummary,
            boolean retryable
    ) {
        this.syncRun = Objects.requireNonNull(syncRun, "syncRun");
        this.staging = staging;
        this.source = "TOUR_API";
        this.sourceContentId = sourceContentId;
        this.errorCode = limit(Objects.requireNonNull(errorCode, "errorCode"), 80);
        this.errorSummary = limit(Objects.requireNonNull(errorSummary, "errorSummary"), 1000);
        this.retryable = retryable;
    }

    public Long getId() { return id; }
    public SyncRun getSyncRun() { return syncRun; }
    public TouristSpotStaging getStaging() { return staging; }
    public String getSource() { return source; }
    public String getSourceContentId() { return sourceContentId; }
    public String getErrorCode() { return errorCode; }
    public String getErrorSummary() { return errorSummary; }
    public boolean isRetryable() { return retryable; }
    public Instant getCreatedAt() { return createdAt; }

    private static String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
