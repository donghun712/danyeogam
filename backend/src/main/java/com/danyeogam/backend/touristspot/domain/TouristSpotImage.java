package com.danyeogam.backend.touristspot.domain;

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
@Table(name = "tourist_spot_image")
public class TouristSpotImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tourist_spot_id", nullable = false)
    private TouristSpot touristSpot;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(name = "alt_text", length = 300)
    private String altText;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "copyright_type", length = 30)
    private String copyrightType;

    @Column(name = "source_image_id", length = 100)
    private String sourceImageId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected TouristSpotImage() {
    }

    public TouristSpotImage(
            TouristSpot touristSpot,
            String url,
            String altText,
            int sortOrder,
            String copyrightType,
            String sourceImageId
    ) {
        this.touristSpot = Objects.requireNonNull(touristSpot, "touristSpot");
        this.url = requireText(url, "url");
        if (sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder는 0 이상이어야 합니다.");
        }
        this.altText = altText;
        this.sortOrder = sortOrder;
        this.copyrightType = copyrightType;
        this.sourceImageId = sourceImageId;
    }

    public Long getId() {
        return id;
    }

    public TouristSpot getTouristSpot() {
        return touristSpot;
    }

    public String getUrl() {
        return url;
    }

    public String getAltText() {
        return altText;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getCopyrightType() {
        return copyrightType;
    }

    public String getSourceImageId() {
        return sourceImageId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + "는 비어 있을 수 없습니다.");
        }
        return value;
    }
}
