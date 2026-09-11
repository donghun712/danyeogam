package com.danyeogam.backend.title.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.danyeogam.backend.touristspot.domain.Region;
import com.danyeogam.backend.touristspot.domain.RegionLevel;
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
@Table(name = "title_definition")
public class TitleDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 20)
    private TitleScopeType scopeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false, length = 40)
    private TitleConditionType conditionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_region_level", length = 20)
    private RegionLevel targetRegionLevel;

    @Column(name = "classification_level")
    private Byte classificationLevel;

    @Column(name = "classification_codes", length = 255)
    private String classificationCodes;

    @Column(name = "required_visit_count")
    private Integer requiredVisitCount;

    @Column(name = "required_progress_percent", precision = 5, scale = 2)
    private BigDecimal requiredProgressPercent;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected TitleDefinition() {
    }

    private TitleDefinition(
            String code,
            String name,
            String description,
            TitleScopeType scopeType,
            Region region,
            TitleConditionType conditionType,
            RegionLevel targetRegionLevel,
            Integer classificationLevel,
            String classificationCodes,
            Integer requiredVisitCount,
            BigDecimal requiredProgressPercent,
            int displayOrder
    ) {
        this.code = requireText(code, "code");
        this.name = requireText(name, "name");
        this.description = blankToNull(description);
        this.scopeType = Objects.requireNonNull(scopeType, "scopeType");
        this.region = region;
        this.conditionType = Objects.requireNonNull(conditionType, "conditionType");
        this.targetRegionLevel = targetRegionLevel;
        this.classificationLevel = classificationLevel == null
                ? null
                : classificationLevel.byteValue();
        this.classificationCodes = blankToNull(classificationCodes);
        this.requiredVisitCount = requiredVisitCount;
        this.requiredProgressPercent = requiredProgressPercent;
        this.displayOrder = displayOrder;
    }

    public static TitleDefinition visitCount(
            String code, String name, String description, int requiredCount, int displayOrder
    ) {
        return new TitleDefinition(
                code, name, description, TitleScopeType.GLOBAL, null,
                TitleConditionType.VISIT_COUNT, null, null, null,
                requiredCount, null, displayOrder
        );
    }

    public static TitleDefinition distinctRegionCount(
            String code,
            String name,
            String description,
            RegionLevel regionLevel,
            int requiredCount,
            int displayOrder
    ) {
        return new TitleDefinition(
                code, name, description, TitleScopeType.GLOBAL, null,
                TitleConditionType.DISTINCT_REGION_COUNT,
                Objects.requireNonNull(regionLevel, "regionLevel"), null, null,
                requiredCount, null, displayOrder
        );
    }

    public static TitleDefinition classificationVisitCount(
            String code,
            String name,
            String description,
            int classificationLevel,
            List<String> classificationCodes,
            int requiredCount,
            int displayOrder
    ) {
        if (classificationLevel != 2 && classificationLevel != 3) {
            throw new IllegalArgumentException("분류 단계는 2 또는 3이어야 합니다.");
        }
        if (classificationCodes == null || classificationCodes.isEmpty()
                || classificationCodes.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("분류 코드는 하나 이상 필요합니다.");
        }
        return new TitleDefinition(
                code, name, description, TitleScopeType.GLOBAL, null,
                TitleConditionType.CLASSIFICATION_VISIT_COUNT, null,
                classificationLevel, String.join(",", classificationCodes),
                requiredCount, null, displayOrder
        );
    }

    public static TitleDefinition regionProgress(
            String code,
            String name,
            String description,
            Region region,
            BigDecimal requiredPercent,
            int displayOrder
    ) {
        return new TitleDefinition(
                code, name, description, TitleScopeType.REGION,
                Objects.requireNonNull(region, "region"),
                TitleConditionType.REGION_PROGRESS_PERCENT, null, null, null,
                null, requiredPercent, displayOrder
        );
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public TitleScopeType getScopeType() { return scopeType; }
    public Region getRegion() { return region; }
    public TitleConditionType getConditionType() { return conditionType; }
    public RegionLevel getTargetRegionLevel() { return targetRegionLevel; }
    public Integer getClassificationLevel() {
        return classificationLevel == null ? null : classificationLevel.intValue();
    }
    public Integer getRequiredVisitCount() { return requiredVisitCount; }
    public BigDecimal getRequiredProgressPercent() { return requiredProgressPercent; }
    public int getDisplayOrder() { return displayOrder; }
    public boolean isActive() { return active; }

    public List<String> getClassificationCodes() {
        if (classificationCodes == null) {
            return List.of();
        }
        return Arrays.stream(classificationCodes.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
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
