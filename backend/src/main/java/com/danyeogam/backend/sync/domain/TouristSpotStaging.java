package com.danyeogam.backend.sync.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import com.danyeogam.backend.touristspot.domain.CoordinateQuality;
import com.danyeogam.backend.touristspot.domain.CoordinateSource;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tourist_spot_staging")
public class TouristSpotStaging {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sync_run_id", nullable = false)
    private SyncRun syncRun;

    @Column(nullable = false, length = 30)
    private String source;

    @Column(name = "source_content_id", nullable = false, length = 40)
    private String sourceContentId;

    @Column(name = "source_content_type_id", length = 20)
    private String sourceContentTypeId;

    @Column(length = 200)
    private String name;

    @Column(name = "road_address", length = 300)
    private String roadAddress;

    @Column(name = "lot_address", length = 300)
    private String lotAddress;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "coordinate_source", length = 30)
    private CoordinateSource coordinateSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "coordinate_quality", nullable = false, length = 20)
    private CoordinateQuality coordinateQuality = CoordinateQuality.UNKNOWN;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 30)
    private StagingProcessingStatus processingStatus = StagingProcessingStatus.RECEIVED;

    @Column(name = "data_hash", length = 64, columnDefinition = "CHAR(64)")
    private String dataHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", nullable = false, columnDefinition = "JSON")
    private JsonNode rawPayload;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "error_summary", length = 1000)
    private String errorSummary;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected TouristSpotStaging() {
    }

    private TouristSpotStaging(
            SyncRun syncRun,
            String sourceContentId,
            String sourceContentTypeId,
            String name,
            String roadAddress,
            String lotAddress,
            BigDecimal latitude,
            BigDecimal longitude,
            JsonNode rawPayload
    ) {
        this.syncRun = Objects.requireNonNull(syncRun, "syncRun");
        this.source = "TOUR_API";
        this.sourceContentId = requireText(sourceContentId, "sourceContentId");
        this.sourceContentTypeId = sourceContentTypeId;
        this.name = name;
        this.roadAddress = roadAddress;
        this.lotAddress = lotAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.rawPayload = Objects.requireNonNull(rawPayload, "rawPayload").deepCopy();
    }

    public static TouristSpotStaging receive(
            SyncRun syncRun,
            String sourceContentId,
            String sourceContentTypeId,
            String name,
            String roadAddress,
            String lotAddress,
            BigDecimal latitude,
            BigDecimal longitude,
            JsonNode rawPayload
    ) {
        return new TouristSpotStaging(
                syncRun,
                sourceContentId,
                sourceContentTypeId,
                name,
                roadAddress,
                lotAddress,
                latitude,
                longitude,
                rawPayload
        );
    }

    public void markReady(
            String dataHash,
            BigDecimal latitude,
            BigDecimal longitude,
            CoordinateSource coordinateSource
    ) {
        this.processingStatus = StagingProcessingStatus.READY;
        this.latitude = Objects.requireNonNull(latitude, "latitude");
        this.longitude = Objects.requireNonNull(longitude, "longitude");
        this.coordinateSource = Objects.requireNonNull(coordinateSource, "coordinateSource");
        this.coordinateQuality = CoordinateQuality.VERIFIED;
        this.dataHash = dataHash;
        this.errorCode = null;
        this.errorSummary = null;
    }

    public void markPromoted() {
        this.processingStatus = StagingProcessingStatus.PROMOTED;
    }

    public void reject(String errorCode, String errorSummary) {
        this.processingStatus = StagingProcessingStatus.REJECTED;
        this.errorCode = limit(errorCode, 80);
        this.errorSummary = limit(errorSummary, 1000);
    }

    public void retryLater(String errorCode, String errorSummary) {
        this.processingStatus = StagingProcessingStatus.RETRY_WAIT;
        this.errorCode = limit(errorCode, 80);
        this.errorSummary = limit(errorSummary, 1000);
    }

    public Long getId() { return id; }
    public SyncRun getSyncRun() { return syncRun; }
    public String getSource() { return source; }
    public String getSourceContentId() { return sourceContentId; }
    public String getSourceContentTypeId() { return sourceContentTypeId; }
    public String getName() { return name; }
    public String getRoadAddress() { return roadAddress; }
    public String getLotAddress() { return lotAddress; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public CoordinateSource getCoordinateSource() { return coordinateSource; }
    public CoordinateQuality getCoordinateQuality() { return coordinateQuality; }
    public StagingProcessingStatus getProcessingStatus() { return processingStatus; }
    public String getDataHash() { return dataHash; }
    public JsonNode getRawPayload() { return rawPayload.deepCopy(); }
    public String getErrorCode() { return errorCode; }
    public String getErrorSummary() { return errorSummary; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + "는 비어 있을 수 없습니다.");
        }
        return value;
    }

    private static String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
