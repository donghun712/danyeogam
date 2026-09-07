package com.danyeogam.backend.touristspot.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "tourist_spot")
public class TouristSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String source;

    @Column(name = "source_content_id", nullable = false, length = 40)
    private String sourceContentId;

    @Column(name = "source_content_type_id", length = 20)
    private String sourceContentTypeId;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "spot_type", nullable = false, length = 30)
    private SpotType spotType = SpotType.GENERAL;

    @Column(name = "stamp_enabled", nullable = false)
    private boolean stampEnabled;

    @Column(name = "stamp_radius_meters")
    private Integer stampRadiusMeters;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(name = "road_address", length = 300)
    private String roadAddress;

    @Column(name = "lot_address", length = 300)
    private String lotAddress;

    @JdbcTypeCode(SqlTypes.GEOMETRY)
    @Column(nullable = false, columnDefinition = "POINT SRID 4326")
    private Point location;

    @Enumerated(EnumType.STRING)
    @Column(name = "coordinate_source", nullable = false, length = 30)
    private CoordinateSource coordinateSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "coordinate_quality", nullable = false, length = 20)
    private CoordinateQuality coordinateQuality = CoordinateQuality.UNKNOWN;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String overview;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "original_image_url", length = 1000)
    private String originalImageUrl;

    @Column(length = 100)
    private String tel;

    @Column(name = "homepage_url", length = 1000)
    private String homepageUrl;

    @Column(name = "source_modified_at")
    private Instant sourceModifiedAt;

    @Column(name = "detail_hydrated_at")
    private Instant detailHydratedAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "data_hash", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String dataHash;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "touristSpot", fetch = FetchType.LAZY)
    private List<TouristSpotImage> images = new ArrayList<>();

    protected TouristSpot() {
    }

    private TouristSpot(
            String sourceContentId,
            String sourceContentTypeId,
            String name,
            Region region,
            Point location,
            CoordinateSource coordinateSource,
            String dataHash
    ) {
        this.source = "TOUR_API";
        this.sourceContentId = requireText(sourceContentId, "sourceContentId");
        this.sourceContentTypeId = sourceContentTypeId;
        this.name = requireText(name, "name");
        this.region = Objects.requireNonNull(region, "region");
        this.location = requireWgs84(location);
        this.coordinateSource = Objects.requireNonNull(coordinateSource, "coordinateSource");
        this.dataHash = requireHash(dataHash);
    }

    public static TouristSpot fromTourApi(
            String sourceContentId,
            String sourceContentTypeId,
            String name,
            Region region,
            Point location,
            String dataHash
    ) {
        return fromTourApi(
                sourceContentId, sourceContentTypeId, name, region, location,
                CoordinateSource.TOUR_API, dataHash
        );
    }

    public static TouristSpot fromTourApi(
            String sourceContentId,
            String sourceContentTypeId,
            String name,
            Region region,
            Point location,
            CoordinateSource coordinateSource,
            String dataHash
    ) {
        return new TouristSpot(
                sourceContentId, sourceContentTypeId, name, region, location,
                coordinateSource, dataHash
        );
    }

    public Long getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getSourceContentId() {
        return sourceContentId;
    }

    public String getSourceContentTypeId() {
        return sourceContentTypeId;
    }

    public String getName() {
        return name;
    }

    public SpotType getSpotType() {
        return spotType;
    }

    public boolean isStampEnabled() {
        return stampEnabled;
    }

    public Integer getStampRadiusMeters() {
        return stampRadiusMeters;
    }

    public Region getRegion() {
        return region;
    }

    public String getRoadAddress() {
        return roadAddress;
    }

    public String getLotAddress() {
        return lotAddress;
    }

    public Point getLocation() {
        return location;
    }

    public CoordinateSource getCoordinateSource() {
        return coordinateSource;
    }

    public CoordinateQuality getCoordinateQuality() {
        return coordinateQuality;
    }

    public String getOverview() {
        return overview;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getOriginalImageUrl() {
        return originalImageUrl;
    }

    public String getTel() {
        return tel;
    }

    public String getHomepageUrl() {
        return homepageUrl;
    }

    public Instant getSourceModifiedAt() {
        return sourceModifiedAt;
    }

    public Instant getDetailHydratedAt() {
        return detailHydratedAt;
    }

    public boolean isActive() {
        return active;
    }

    public String getDataHash() {
        return dataHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<TouristSpotImage> getImages() {
        return Collections.unmodifiableList(images);
    }

    public void refreshSummary(
            String sourceContentTypeId,
            String name,
            Region region,
            String roadAddress,
            String lotAddress,
            Point location,
            String thumbnailUrl,
            String originalImageUrl,
            String telephone,
            Instant sourceModifiedAt,
            String dataHash
    ) {
        refreshSummary(
                sourceContentTypeId, name, region, roadAddress, lotAddress, location,
                CoordinateSource.TOUR_API, thumbnailUrl, originalImageUrl, telephone,
                sourceModifiedAt, dataHash
        );
    }

    public void refreshSummary(
            String sourceContentTypeId,
            String name,
            Region region,
            String roadAddress,
            String lotAddress,
            Point location,
            CoordinateSource coordinateSource,
            String thumbnailUrl,
            String originalImageUrl,
            String telephone,
            Instant sourceModifiedAt,
            String dataHash
    ) {
        this.sourceContentTypeId = blankToNull(sourceContentTypeId);
        this.name = requireText(name, "name");
        this.region = Objects.requireNonNull(region, "region");
        this.roadAddress = blankToNull(roadAddress);
        this.lotAddress = blankToNull(lotAddress);
        this.location = requireWgs84(location);
        this.coordinateSource = Objects.requireNonNull(coordinateSource, "coordinateSource");
        this.coordinateQuality = CoordinateQuality.VERIFIED;
        this.thumbnailUrl = blankToNull(thumbnailUrl);
        this.originalImageUrl = blankToNull(originalImageUrl);
        this.tel = blankToNull(telephone);
        this.sourceModifiedAt = sourceModifiedAt;
        this.dataHash = requireHash(dataHash);
        this.active = true;
    }

    public void hydrateDetail(
            String overview,
            String homepageUrl,
            String telephone,
            String thumbnailUrl,
            String originalImageUrl,
            Instant hydratedAt
    ) {
        this.overview = blankToNull(overview);
        this.homepageUrl = blankToNull(homepageUrl);
        if (telephone != null && !telephone.isBlank()) {
            this.tel = telephone;
        }
        if (thumbnailUrl != null && !thumbnailUrl.isBlank()) {
            this.thumbnailUrl = thumbnailUrl;
        }
        if (originalImageUrl != null && !originalImageUrl.isBlank()) {
            this.originalImageUrl = originalImageUrl;
        }
        this.detailHydratedAt = Objects.requireNonNull(hydratedAt, "hydratedAt");
    }

    public void enableStampTarget(int radiusMeters) {
        if (radiusMeters < 1) {
            throw new IllegalArgumentException("스탬프 인증 반경은 1m 이상이어야 합니다.");
        }
        this.spotType = SpotType.STAMP_TARGET;
        this.stampEnabled = true;
        this.stampRadiusMeters = radiusMeters;
    }

    private static Point requireWgs84(Point point) {
        Objects.requireNonNull(point, "location");
        if (point.getSRID() != 4326) {
            throw new IllegalArgumentException("관광지 좌표의 SRID는 4326이어야 합니다.");
        }
        return point;
    }

    private static String requireHash(String value) {
        if (value == null || !value.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("dataHash는 64자리 16진수여야 합니다.");
        }
        return value.toLowerCase();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + "는 비어 있을 수 없습니다.");
        }
        return value;
    }
}
