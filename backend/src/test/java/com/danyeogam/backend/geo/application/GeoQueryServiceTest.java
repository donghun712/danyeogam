package com.danyeogam.backend.geo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.danyeogam.backend.common.error.BusinessException;
import com.danyeogam.backend.common.error.ErrorCode;
import com.danyeogam.backend.geo.api.ReverseGeoResponse;
import com.danyeogam.backend.geo.config.GeoQueryProperties;
import org.junit.jupiter.api.Test;

class GeoQueryServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-03T00:00:00Z"), ZoneOffset.UTC
    );

    @Test
    void returnsNormalizedAddressAndCachesNearbyCoordinate() {
        GeoProvider provider = mock(GeoProvider.class);
        BigDecimal latitude = new BigDecimal("35.82421");
        BigDecimal longitude = new BigDecimal("127.14801");
        when(provider.reverse(latitude, longitude)).thenReturn(Optional.of(new ReverseAddress(
                "전북 전주시 완산구", "전북 전주시 완산구 태조로 1",
                "전북특별자치도", "전주시 완산구", "풍남동"
        )));
        GeoQueryService service = new GeoQueryService(provider, new GeoQueryProperties(), CLOCK);

        ReverseGeoResponse first = service.reverse(latitude, longitude);
        ReverseGeoResponse cached = service.reverse(
                new BigDecimal("35.82424"), new BigDecimal("127.14804")
        );

        assertThat(first.source()).isEqualTo("KAKAO_LOCAL");
        assertThat(first.region().depth1()).isEqualTo("전북특별자치도");
        assertThat(cached).isEqualTo(first);
        verify(provider).reverse(latitude, longitude);
        verifyNoMoreInteractions(provider);
    }

    @Test
    void emptyProviderResultIsValidAndProviderFailureBecomes503BusinessError() {
        GeoProvider provider = mock(GeoProvider.class);
        BigDecimal latitude = new BigDecimal("35.8");
        BigDecimal longitude = new BigDecimal("127.1");
        when(provider.reverse(latitude, longitude)).thenReturn(Optional.empty());
        GeoQueryService service = new GeoQueryService(provider, new GeoQueryProperties(), CLOCK);

        assertThat(service.reverse(latitude, longitude).addressName()).isNull();

        GeoProvider failingProvider = mock(GeoProvider.class);
        when(failingProvider.reverse(latitude, longitude)).thenThrow(
                new GeoProviderException("unavailable", true, null)
        );
        GeoQueryService failingService = new GeoQueryService(
                failingProvider, new GeoQueryProperties(), CLOCK
        );
        assertThatThrownBy(() -> failingService.reverse(latitude, longitude))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GEO_PROVIDER_UNAVAILABLE)
                );
    }
}
