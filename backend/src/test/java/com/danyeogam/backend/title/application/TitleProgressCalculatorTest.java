package com.danyeogam.backend.title.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import com.danyeogam.backend.title.domain.TitleDefinition;
import com.danyeogam.backend.title.repository.RegionTitleProgressProjection;
import com.danyeogam.backend.touristspot.domain.Region;
import com.danyeogam.backend.touristspot.domain.RegionLevel;
import com.danyeogam.backend.visit.repository.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TitleProgressCalculatorTest {

    private VisitRepository visitRepository;
    private TitleProgressCalculator calculator;

    @BeforeEach
    void setUp() {
        visitRepository = mock(VisitRepository.class);
        calculator = new TitleProgressCalculator(visitRepository);
    }

    @Test
    void calculatesVisitRegionAndClassificationConditionsFromDatabaseCounts() {
        TitleDefinition visits = withId(TitleDefinition.visitCount(
                "VISITS", "방문", "방문", 10, 1
        ), 1L);
        TitleDefinition provinces = withId(TitleDefinition.distinctRegionCount(
                "PROVINCES", "광역", "광역", RegionLevel.PROVINCE, 8, 2
        ), 2L);
        TitleDefinition districts = withId(TitleDefinition.distinctRegionCount(
                "DISTRICTS", "시군구", "시군구", RegionLevel.CITY_COUNTY, 10, 3
        ), 3L);
        TitleDefinition history = withId(TitleDefinition.classificationVisitCount(
                "HISTORY", "역사", "역사", 2, List.of("HS01"), 15, 4
        ), 4L);
        TitleDefinition culture = withId(TitleDefinition.classificationVisitCount(
                "CULTURE", "문화", "문화", 3,
                List.of("VE070100", "VE070200", "VE070600"), 8, 5
        ), 5L);

        when(visitRepository.countByActorId(42L)).thenReturn(10L);
        when(visitRepository.countDistinctVisitedProvinces(42L)).thenReturn(8L);
        when(visitRepository.countDistinctVisitedCityCounties(42L)).thenReturn(9L);
        when(visitRepository.countDistinctVisitedSpotsByClassificationLevel2(
                42L, List.of("HS01")
        )).thenReturn(15L);
        when(visitRepository.countDistinctVisitedSpotsByClassificationLevel3(
                42L, List.of("VE070100", "VE070200", "VE070600")
        )).thenReturn(7L);

        var result = calculator.calculate(
                42L, List.of(visits, provinces, districts, history, culture)
        );

        assertThat(result.get(1L).achieved()).isTrue();
        assertThat(result.get(2L).achieved()).isTrue();
        assertThat(result.get(3L).achieved()).isFalse();
        assertThat(result.get(4L).achieved()).isTrue();
        assertThat(result.get(5L).achieved()).isFalse();
    }

    @Test
    void usesTheSameRoundedProvinceProgressAsCollectionSummary() {
        Region province = Region.province("TOUR:AREA:52", "전북특별자치도");
        ReflectionTestUtils.setField(province, "id", 52L);
        TitleDefinition regional = withId(TitleDefinition.regionProgress(
                "REGION_MASTER:TOUR:AREA:52", "전북특별자치도 터줏대감", "지역",
                province, new BigDecimal("20.00"), 100
        ), 6L);
        RegionTitleProgressProjection progress = mock(RegionTitleProgressProjection.class);
        when(progress.getRegionId()).thenReturn(52L);
        when(progress.getVisitedCount()).thenReturn(1L);
        when(progress.getTotalCount()).thenReturn(5L);
        when(visitRepository.findProvinceProgress(42L)).thenReturn(List.of(progress));

        var result = calculator.calculate(42L, List.of(regional)).get(6L);

        assertThat(result.currentValue()).isEqualTo(20);
        assertThat(result.targetValue()).isEqualTo(20);
        assertThat(result.unit()).isEqualTo(TitleProgressUnit.PERCENT);
        assertThat(result.achieved()).isTrue();
    }

    private static TitleDefinition withId(TitleDefinition definition, long id) {
        ReflectionTestUtils.setField(definition, "id", id);
        return definition;
    }
}
