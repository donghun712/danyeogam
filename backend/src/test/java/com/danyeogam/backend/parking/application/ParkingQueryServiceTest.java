package com.danyeogam.backend.parking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.danyeogam.backend.common.error.BusinessException;
import com.danyeogam.backend.common.error.ErrorCode;
import com.danyeogam.backend.parking.api.ParkingResponse;
import com.danyeogam.backend.parking.config.ParkingQueryProperties;
import com.danyeogam.backend.touristspot.domain.Region;
import com.danyeogam.backend.touristspot.domain.TouristSpot;
import com.danyeogam.backend.touristspot.repository.TouristSpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.test.util.ReflectionTestUtils;

class ParkingQueryServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-03T00:00:00Z"), ZoneOffset.UTC
    );
    private TouristSpotRepository repository;
    private ParkingProvider provider;
    private ParkingQueryService service;

    @BeforeEach
    void setUp() {
        repository = mock(TouristSpotRepository.class);
        provider = mock(ParkingProvider.class);
        service = new ParkingQueryService(
                repository, provider, new ParkingQueryProperties(), CLOCK
        );
        Region region = Region.province("TEST", "테스트 지역");
        TouristSpot spot = TouristSpot.fromTourApi(
                "CONTENT", "12", "관광지", region,
                new GeometryFactory(new PrecisionModel(), 4326)
                        .createPoint(new Coordinate(127.1480, 35.8242)),
                "a".repeat(64)
        );
        ReflectionTestUtils.setField(spot, "id", 7L);
        when(repository.findByIdAndActiveTrue(7L)).thenReturn(Optional.of(spot));
    }

    @Test
    void capsProviderParametersMapsNavigationAndCachesResult() {
        when(provider.findNearby(any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(new NearbyParking(
                        "KAKAO:123", "한옥마을 주차장", "전북 전주시",
                        new BigDecimal("35.816"), new BigDecimal("127.151"), 260
                )));

        ParkingResponse first = service.findNearby(7L, 50000, 100);
        ParkingResponse cached = service.findNearby(7L, 50000, 100);

        assertThat(first.temporarilyUnavailable()).isFalse();
        assertThat(first.items()).singleElement().satisfies(item -> {
            assertThat(item.publicVerified()).isFalse();
            assertThat(item.source()).isEqualTo("KAKAO_LOCAL");
            assertThat(item.navigation().coordinateType()).isEqualTo("wgs84");
            assertThat(item.fetchedAt()).isEqualTo(CLOCK.instant());
        });
        assertThat(cached).isEqualTo(first);
        verify(provider).findNearby(
                new BigDecimal("35.8242"), new BigDecimal("127.148"), 20000, 15
        );
        verifyNoMoreInteractions(provider);
    }

    @Test
    void isolatesProviderFailureAndRejectsUnknownSpot() {
        when(provider.findNearby(any(), any(), anyInt(), anyInt()))
                .thenThrow(new ParkingProviderException("unavailable", null));

        assertThat(service.findNearby(7L, null, null).temporarilyUnavailable()).isTrue();
        assertThat(service.findNearby(7L, null, null).items()).isEmpty();

        assertThatThrownBy(() -> service.findNearby(999L, null, null))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TOURIST_SPOT_NOT_FOUND)
                );
    }
}
