package com.danyeogam.backend.sync.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sync_run")
public class SyncRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 30)
    private SyncJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SyncRunStatus status;

    @Column(name = "started_at", nullable = false, insertable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "requested_count", nullable = false)
    private int requestedCount;

    @Column(name = "processed_count", nullable = false)
    private int processedCount;

    @Column(name = "inserted_count", nullable = false)
    private int insertedCount;

    @Column(name = "updated_count", nullable = false)
    private int updatedCount;

    @Column(name = "deactivated_count", nullable = false)
    private int deactivatedCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "request_quota_count", nullable = false)
    private int requestQuotaCount;

    @Column(length = 1000)
    private String summary;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected SyncRun() {
    }

    private SyncRun(SyncJobType jobType) {
        this.jobType = jobType;
        this.status = SyncRunStatus.RUNNING;
    }

    public static SyncRun start(SyncJobType jobType) {
        return new SyncRun(jobType);
    }

    public void recordRequested(int count) {
        requestedCount += count;
    }

    public void recordProcessed() {
        processedCount++;
    }

    public void recordInserted() {
        insertedCount++;
    }

    public void recordUpdated() {
        updatedCount++;
    }

    public void recordFailed() {
        failedCount++;
    }

    public void recordApiRequest() {
        requestQuotaCount++;
    }

    public void finish(String summary) {
        this.status = failedCount == 0
                ? SyncRunStatus.SUCCEEDED
                : insertedCount + updatedCount > 0 || processedCount > failedCount
                        ? SyncRunStatus.PARTIALLY_SUCCEEDED
                        : SyncRunStatus.FAILED;
        this.summary = limit(summary, 1000);
        this.finishedAt = Instant.now();
    }

    public void fail(String summary) {
        this.status = SyncRunStatus.FAILED;
        this.summary = limit(summary, 1000);
        this.finishedAt = Instant.now();
    }

    public Long getId() { return id; }
    public SyncJobType getJobType() { return jobType; }
    public SyncRunStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public int getRequestedCount() { return requestedCount; }
    public int getProcessedCount() { return processedCount; }
    public int getInsertedCount() { return insertedCount; }
    public int getUpdatedCount() { return updatedCount; }
    public int getDeactivatedCount() { return deactivatedCount; }
    public int getFailedCount() { return failedCount; }
    public int getRequestQuotaCount() { return requestQuotaCount; }
    public String getSummary() { return summary; }
    public Instant getCreatedAt() { return createdAt; }

    private static String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
