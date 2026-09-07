package com.danyeogam.backend.touristspot.domain;

import java.time.Instant;
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
import jakarta.persistence.Table;

@Entity
@Table(name = "region")
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Region parent;

    @Enumerated(EnumType.STRING)
    @Column(name = "region_level", nullable = false, length = 20)
    private RegionLevel level;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected Region() {
    }

    private Region(String code, String name, Region parent, RegionLevel level) {
        this.code = requireText(code, "code");
        this.name = requireText(name, "name");
        this.parent = parent;
        this.level = Objects.requireNonNull(level, "level");
        if (level == RegionLevel.PROVINCE && parent != null) {
            throw new IllegalArgumentException("광역 지역은 상위 지역을 가질 수 없습니다.");
        }
        if (level == RegionLevel.CITY_COUNTY && parent == null) {
            throw new IllegalArgumentException("시군구 지역에는 상위 지역이 필요합니다.");
        }
    }

    public static Region province(String code, String name) {
        return new Region(code, name, null, RegionLevel.PROVINCE);
    }

    public static Region cityCounty(String code, String name, Region parent) {
        return new Region(code, name, Objects.requireNonNull(parent, "parent"), RegionLevel.CITY_COUNTY);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Region getParent() {
        return parent;
    }

    public RegionLevel getLevel() {
        return level;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void refresh(String name) {
        this.name = requireText(name, "name");
        this.active = true;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + "는 비어 있을 수 없습니다.");
        }
        return value;
    }
}
