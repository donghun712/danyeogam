package com.danyeogam.backend.title.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.danyeogam.backend.title.domain.TitleConditionType;
import com.danyeogam.backend.title.domain.TitleDefinition;
import com.danyeogam.backend.title.repository.RegionTitleProgressProjection;
import com.danyeogam.backend.touristspot.domain.RegionLevel;
import com.danyeogam.backend.visit.repository.VisitRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class TitleProgressCalculator {

    private final VisitRepository visitRepository;

    public TitleProgressCalculator(VisitRepository visitRepository) {
        this.visitRepository = visitRepository;
    }

    public Map<Long, TitleProgress> calculate(long actorId, List<TitleDefinition> definitions) {
        Map<Long, TitleProgress> result = new HashMap<>();
        Long totalVisits = null;
        Long provinceCount = null;
        Long cityCountyCount = null;
        Map<Long, RegionTitleProgressProjection> provinceProgress = null;

        for (TitleDefinition definition : definitions) {
            TitleProgress progress;
            if (definition.getConditionType() == TitleConditionType.VISIT_COUNT) {
                if (totalVisits == null) {
                    totalVisits = visitRepository.countByActorId(actorId);
                }
                progress = new TitleProgress(
                        totalVisits,
                        requiredCount(definition),
                        TitleProgressUnit.VISITS
                );
            } else if (definition.getConditionType() == TitleConditionType.DISTINCT_REGION_COUNT) {
                if (definition.getTargetRegionLevel() == RegionLevel.PROVINCE) {
                    if (provinceCount == null) {
                        provinceCount = visitRepository.countDistinctVisitedProvinces(actorId);
                    }
                    progress = new TitleProgress(
                            provinceCount,
                            requiredCount(definition),
                            TitleProgressUnit.REGIONS
                    );
                } else if (definition.getTargetRegionLevel() == RegionLevel.CITY_COUNTY) {
                    if (cityCountyCount == null) {
                        cityCountyCount = visitRepository.countDistinctVisitedCityCounties(actorId);
                    }
                    progress = new TitleProgress(
                            cityCountyCount,
                            requiredCount(definition),
                            TitleProgressUnit.REGIONS
                    );
                } else {
                    throw invalidDefinition(definition);
                }
            } else if (definition.getConditionType() == TitleConditionType.CLASSIFICATION_VISIT_COUNT) {
                List<String> codes = definition.getClassificationCodes();
                if (codes.isEmpty()) {
                    throw invalidDefinition(definition);
                }
                long count = switch (Objects.requireNonNull(definition.getClassificationLevel())) {
                    case 2 -> visitRepository.countDistinctVisitedSpotsByClassificationLevel2(actorId, codes);
                    case 3 -> visitRepository.countDistinctVisitedSpotsByClassificationLevel3(actorId, codes);
                    default -> throw invalidDefinition(definition);
                };
                progress = new TitleProgress(
                        count,
                        requiredCount(definition),
                        TitleProgressUnit.VISITS
                );
            } else if (definition.getConditionType() == TitleConditionType.REGION_PROGRESS_PERCENT) {
                if (definition.getRegion() == null) {
                    throw invalidDefinition(definition);
                }
                if (provinceProgress == null) {
                    provinceProgress = new HashMap<>();
                    for (RegionTitleProgressProjection item : visitRepository.findProvinceProgress(actorId)) {
                        provinceProgress.put(item.getRegionId(), item);
                    }
                }
                RegionTitleProgressProjection item = provinceProgress.get(definition.getRegion().getId());
                long currentPercent = item == null
                        ? 0
                        : roundedPercent(item.getVisitedCount(), item.getTotalCount());
                progress = new TitleProgress(
                        currentPercent,
                        requiredPercent(definition),
                        TitleProgressUnit.PERCENT
                );
            } else {
                throw invalidDefinition(definition);
            }
            result.put(definition.getId(), progress);
        }
        return Map.copyOf(result);
    }

    private static long roundedPercent(long visited, long total) {
        if (total == 0) {
            return 0;
        }
        return BigDecimal.valueOf(visited)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private static long requiredCount(TitleDefinition definition) {
        if (definition.getRequiredVisitCount() == null) {
            throw invalidDefinition(definition);
        }
        return definition.getRequiredVisitCount();
    }

    private static long requiredPercent(TitleDefinition definition) {
        if (definition.getRequiredProgressPercent() == null) {
            throw invalidDefinition(definition);
        }
        return definition.getRequiredProgressPercent().longValueExact();
    }

    private static IllegalStateException invalidDefinition(TitleDefinition definition) {
        return new IllegalStateException("칭호 정의가 올바르지 않습니다: " + definition.getCode());
    }
}
