package com.danyeogam.backend.touristspot.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.danyeogam.backend.common.error.BusinessException;
import com.danyeogam.backend.common.error.ErrorCode;
import com.danyeogam.backend.touristspot.application.dto.TouristSpotDetailResponse;
import com.danyeogam.backend.touristspot.application.dto.TouristSpotMapData;
import com.danyeogam.backend.touristspot.config.MapQueryProperties;
import com.danyeogam.backend.touristspot.domain.Region;
import com.danyeogam.backend.touristspot.domain.TouristSpot;
import com.danyeogam.backend.touristspot.domain.TouristSpotImage;
import com.danyeogam.backend.touristspot.repository.RegionRepository;
import com.danyeogam.backend.touristspot.repository.TouristSpotImageRepository;
import com.danyeogam.backend.touristspot.repository.TouristSpotMapProjection;
import com.danyeogam.backend.touristspot.repository.TouristSpotRepository;
import com.danyeogam.backend.visit.repository.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.test.util.ReflectionTestUtils;

class TouristSpotQueryServiceTest {

    private RegionRepository regionRepository;
    private TouristSpotRepository spotRepository;
    private TouristSpotImageRepository imageRepository;
    private VisitRepository visitRepository;
    private MapQueryProperties properties;
    private TouristSpotQueryService service;

    @BeforeEach
    void setUp() {
        regionRepository = mock(RegionRepository.class);
        spotRepository = mock(TouristSpotRepository.class);
        imageRepository = mock(TouristSpotImageRepository.class);
        visitRepository = mock(VisitRepository.class);
        properties = new MapQueryProperties();
        service = new TouristSpotQueryService(
                regionRepository, spotRepository, imageRepository, visitRepository, properties
        );
    }

    @Test
    void mapsLightweightBoundsProjectionWithoutOverview() {
        TouristSpotMapProjection projection = mock(TouristSpotMapProjection.class);
        when(projection.getId()).thenReturn(7L);
        when(projection.getName()).thenReturn("경복궁");
        when(projection.getSpotType()).thenReturn("GENERAL");
        when(projection.getStampEnabled()).thenReturn(false);
        when(projection.getLatitude()).thenReturn(new BigDecimal("37.5788"));
        when(projection.getLongitude()).thenReturn(new BigDecimal("126.9769"));
        when(projection.getThumbnailUrl()).thenReturn("https://image.test/thumb.jpg");
        MapBounds bounds = bounds();
        when(spotRepository.findActiveWithinBoundsAndTypes(
                bounds.polygonWkt(), List.of("GENERAL"), 1001
        )).thenReturn(List.of(projection));

        TouristSpotMapData data = service.getMapSpots(bounds, "general");

        assertThat(data.items()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(7L);
            assertThat(item.visitState()).isEqualTo("UNKNOWN");
            assertThat(item.position().longitude()).isEqualByComparingTo("126.9769");
        });
        verify(spotRepository).findActiveWithinBoundsAndTypes(
                bounds.polygonWkt(), List.of("GENERAL"), 1001
        );
    }

    @Test
    void returnsActiveRegionHierarchy() {
        Region province = Region.province("TOUR:AREA:1", "서울특별시");
        Region district = Region.cityCounty("TOUR:AREA:1:23", "종로구", province);
        ReflectionTestUtils.setField(province, "id", 1L);
        ReflectionTestUtils.setField(district, "id", 2L);
        when(regionRepository.findAllByParentIsNullAndActiveTrueOrderByNameAsc())
                .thenReturn(List.of(province));
        when(regionRepository.findAllByParentIdAndActiveTrueOrderByNameAsc(1L))
                .thenReturn(List.of(district));

        var result = service.getRegions();

        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.name()).isEqualTo("서울특별시");
            assertThat(item.children()).singleElement()
                    .satisfies(child -> assertThat(child.name()).isEqualTo("종로구"));
        });
    }

    @Test
    void resolvesVisitedStateInOneBatchForAuthenticatedActor() {
        TouristSpotMapProjection projection = mock(TouristSpotMapProjection.class);
        when(projection.getId()).thenReturn(7L);
        when(projection.getName()).thenReturn("경복궁");
        when(projection.getSpotType()).thenReturn("STAMP_TARGET");
        when(projection.getLatitude()).thenReturn(new BigDecimal("37.5788"));
        when(projection.getLongitude()).thenReturn(new BigDecimal("126.9769"));
        when(spotRepository.findActiveWithinBounds(bounds().polygonWkt(), 1001))
                .thenReturn(List.of(projection));
        when(visitRepository.findVisitedSpotIds(42L, List.of(7L))).thenReturn(List.of(7L));

        TouristSpotMapData data = service.getMapSpots(bounds(), null, 42L);

        assertThat(data.items()).singleElement()
                .satisfies(item -> assertThat(item.visitState()).isEqualTo("VISITED"));
        verify(visitRepository).findVisitedSpotIds(42L, List.of(7L));
    }

    @Test
    void rejectsUnknownTypeAndMoreThanConfiguredResultLimit() {
        assertThatThrownBy(() -> service.getMapSpots(bounds(), "UNKNOWN_TYPE"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_SPOT_TYPE));

        properties.setMaxResults(1);
        when(spotRepository.findActiveWithinBounds(bounds().polygonWkt(), 2))
                .thenReturn(List.of(mock(TouristSpotMapProjection.class), mock(TouristSpotMapProjection.class)));
        assertThatThrownBy(() -> service.getMapSpots(bounds(), null))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BOUNDS_TOO_DENSE));
    }

    @Test
    void returnsDetailedTouristSpotWithImagesAndNavigation() {
        Region region = Region.province("TOUR:AREA:1", "서울특별시");
        TouristSpot spot = TouristSpot.fromTourApi(
                "126508", "12", "경복궁", region, point(126.9769, 37.5788), "a".repeat(64)
        );
        spot.refreshSummary(
                "12", "경복궁", region, "서울특별시 종로구", null,
                point(126.9769, 37.5788), "thumb", "original", "02-0000-0000",
                Instant.parse("2026-01-01T00:00:00Z"), "a".repeat(64)
        );
        spot.hydrateDetail(
                "조선 왕조의 법궁", "https://example.com", "02-0000-0000",
                "thumb", "original", Instant.parse("2026-01-02T00:00:00Z")
        );
        ReflectionTestUtils.setField(spot, "id", 7L);
        TouristSpotImage image = new TouristSpotImage(
                spot, "https://image.test/original.jpg", "경복궁 전경", 0, "Type1", "1"
        );
        when(spotRepository.findByIdAndActiveTrue(7L)).thenReturn(Optional.of(spot));
        when(imageRepository.findAllByTouristSpotIdOrderBySortOrderAscIdAsc(7L)).thenReturn(List.of(image));

        TouristSpotDetailResponse detail = service.getDetail(7L);

        assertThat(detail.dataQuality()).isEqualTo("COMPLETE");
        assertThat(detail.regionCode()).isEqualTo("TOUR:AREA:1");
        assertThat(detail.images()).singleElement().satisfies(result -> {
            assertThat(result.alt()).isEqualTo("경복궁 전경");
            assertThat(result.copyrightType()).isEqualTo("Type1");
        });
        assertThat(detail.navigation().coordinateType()).isEqualTo("wgs84");
        assertThat(detail.position().latitude()).isEqualByComparingTo("37.5788");
    }

    @Test
    void returnsStableNotFoundCode() {
        when(spotRepository.findByIdAndActiveTrue(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(999L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TOURIST_SPOT_NOT_FOUND));
    }

    @Test
    void returnsNotVisitedForAuthenticatedActorWithoutVisit() {
        Region region = Region.province("TOUR:AREA:1", "서울특별시");
        TouristSpot spot = TouristSpot.fromTourApi(
                "126508", "12", "경복궁", region, point(126.9769, 37.5788), "a".repeat(64)
        );
        ReflectionTestUtils.setField(spot, "id", 7L);
        when(spotRepository.findByIdAndActiveTrue(7L)).thenReturn(Optional.of(spot));
        when(imageRepository.findAllByTouristSpotIdOrderBySortOrderAscIdAsc(7L)).thenReturn(List.of());
        when(visitRepository.existsByActorIdAndTouristSpotId(42L, 7L)).thenReturn(false);

        TouristSpotDetailResponse detail = service.getDetail(7L, 42L);

        assertThat(detail.visitState()).isEqualTo("NOT_VISITED");
    }

    @Test
    void returnsProvinceCodeForCityCountySpot() {
        Region province = Region.province("TOUR:AREA:45", "전북특별자치도");
        Region district = Region.cityCounty("TOUR:AREA:45:111", "전주시", province);
        TouristSpot spot = TouristSpot.fromTourApi(
                "REGION-1", "12", "전주 관광지", district, point(127.148, 35.8242), "a".repeat(64)
        );
        ReflectionTestUtils.setField(spot, "id", 7L);
        when(spotRepository.findByIdAndActiveTrue(7L)).thenReturn(Optional.of(spot));
        when(imageRepository.findAllByTouristSpotIdOrderBySortOrderAscIdAsc(7L)).thenReturn(List.of());

        assertThat(service.getDetail(7L).regionCode())
                .isEqualTo(province.getCode())
                .matches("^TOUR:AREA:[0-9]+$");
        assertThat(service.getDetail(7L, 42L).regionCode()).isEqualTo(province.getCode());
    }

    @Test
    void rejectsMissingProvinceAndCyclicHierarchyInsteadOfReturningDistrictCode() {
        Region province = Region.province("TOUR:AREA:45", "전북특별자치도");
        Region district = Region.cityCounty("TOUR:AREA:45:111", "전주시", province);
        TouristSpot spot = TouristSpot.fromTourApi(
                "REGION-2", "12", "전주 관광지", district, point(127.148, 35.8242), "a".repeat(64)
        );
        ReflectionTestUtils.setField(spot, "id", 7L);
        when(spotRepository.findByIdAndActiveTrue(7L)).thenReturn(Optional.of(spot));
        when(imageRepository.findAllByTouristSpotIdOrderBySortOrderAscIdAsc(7L)).thenReturn(List.of());

        ReflectionTestUtils.setField(district, "parent", null);
        assertThatThrownBy(() -> service.getDetail(7L)).isInstanceOf(IllegalStateException.class);
        ReflectionTestUtils.setField(district, "parent", district);
        assertThatThrownBy(() -> service.getDetail(7L)).isInstanceOf(IllegalStateException.class);
    }

    private static MapBounds bounds() {
        return new MapBounds(
                new BigDecimal("37.7"), new BigDecimal("127.2"),
                new BigDecimal("37.4"), new BigDecimal("126.8")
        );
    }

    private static org.locationtech.jts.geom.Point point(double longitude, double latitude) {
        return new GeometryFactory(new PrecisionModel(), 4326)
                .createPoint(new Coordinate(longitude, latitude));
    }
}
