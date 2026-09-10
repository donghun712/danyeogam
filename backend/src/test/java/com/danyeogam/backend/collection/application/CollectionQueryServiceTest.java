package com.danyeogam.backend.collection.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.danyeogam.backend.collection.repository.CollectionItemProjection;
import com.danyeogam.backend.collection.repository.CollectionQueryRepository;
import com.danyeogam.backend.collection.repository.CollectionSummaryProjection;
import com.danyeogam.backend.common.error.BusinessException;
import com.danyeogam.backend.common.error.ErrorCode;
import com.danyeogam.backend.touristspot.domain.Region;
import com.danyeogam.backend.touristspot.repository.RegionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CollectionQueryServiceTest {

    private RegionRepository regionRepository;
    private CollectionQueryRepository collectionRepository;
    private CollectionQueryService service;

    @BeforeEach
    void setUp() {
        regionRepository = mock(RegionRepository.class);
        collectionRepository = mock(CollectionQueryRepository.class);
        service = new CollectionQueryService(regionRepository, collectionRepository);
    }

    @Test
    void returnsRegionCollectionAndVisitFilter() {
        Region region = Region.province("TOUR:AREA:45", "전북특별자치도");
        ReflectionTestUtils.setField(region, "id", 45L);
        when(regionRepository.findByCodeAndActiveTrue("TOUR:AREA:45"))
                .thenReturn(Optional.of(region));
        CollectionItemProjection item = mock(CollectionItemProjection.class);
        when(item.getTouristSpotId()).thenReturn(7L);
        when(item.getName()).thenReturn("경기전");
        when(item.getVisitState()).thenReturn("VISITED");
        when(item.getVerifiedAt()).thenReturn(Instant.parse("2026-09-03T00:00:00Z"));
        when(collectionRepository.findCollectionItems(42L, 45L, "VISITED"))
                .thenReturn(List.of(item));

        var result = service.getCollection(42L, "TOUR:AREA:45", "visited");

        assertThat(result.region().name()).isEqualTo("전북특별자치도");
        assertThat(result.items()).singleElement().satisfies(value -> {
            assertThat(value.touristSpotId()).isEqualTo(7L);
            assertThat(value.visitState()).isEqualTo("VISITED");
        });
    }

    @Test
    void calculatesRoundedProgressAndKeepsZeroTargetRegions() {
        CollectionSummaryProjection first = summary("45", "전북", 1L, 3L);
        CollectionSummaryProjection empty = summary("46", "전남", 0L, 0L);
        when(collectionRepository.findCollectionSummary(42L)).thenReturn(List.of(first, empty));

        var result = service.getSummary(42L);

        assertThat(result.regions()).extracting("progressPercent").containsExactly(33, 0);
    }

    @Test
    void returnsCityCountySummaryForProvinceWithoutChangingDefaultSummary() {
        Region province = Region.province("TOUR:AREA:52", "전북특별자치도");
        ReflectionTestUtils.setField(province, "id", 52L);
        when(regionRepository.findByCodeAndActiveTrue(province.getCode()))
                .thenReturn(Optional.of(province));
        CollectionSummaryProjection districtSummary =
                summary("TOUR:AREA:52:111", "전주시 완산구", 2L, 5L);
        when(collectionRepository.findCollectionSummaryByParent(42L, 52L))
                .thenReturn(List.of(districtSummary));

        var result = service.getSummary(42L, province.getCode());

        assertThat(result.regions()).singleElement().satisfies(item -> {
            assertThat(item.code()).isEqualTo("TOUR:AREA:52:111");
            assertThat(item.progressPercent()).isEqualTo(40);
        });
    }

    @Test
    void rejectsCityCountyAsSummaryParent() {
        Region province = Region.province("TOUR:AREA:52", "전북특별자치도");
        Region district = Region.cityCounty("TOUR:AREA:52:111", "전주시 완산구", province);
        when(regionRepository.findByCodeAndActiveTrue(district.getCode()))
                .thenReturn(Optional.of(district));

        assertThatThrownBy(() -> service.getSummary(42L, district.getCode()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void rejectsUnknownRegionAndStatusWithStableCodes() {
        when(regionRepository.findByCodeAndActiveTrue("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getCollection(42L, "missing", "ALL"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REGION_NOT_FOUND));
        assertThatThrownBy(() -> service.getCollection(42L, "missing", "DONE"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_COLLECTION_STATUS));
    }

    private static CollectionSummaryProjection summary(String code, String name, long visited, long total) {
        CollectionSummaryProjection item = mock(CollectionSummaryProjection.class);
        when(item.getCode()).thenReturn(code);
        when(item.getName()).thenReturn(name);
        when(item.getVisitedCount()).thenReturn(visited);
        when(item.getTotalCount()).thenReturn(total);
        return item;
    }
}
