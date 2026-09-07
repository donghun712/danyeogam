package com.danyeogam.backend.kakao;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_KAKAO_API_TESTS", matches = "true")
class KakaoLocalLiveTest {

    @Autowired
    private KakaoLocalClient client;

    @Test
    void geocodesReverseGeocodesAndSearchesParkingWithRealKey() {
        KakaoGeocodedAddress geocoded = client.geocode("서울특별시 종로구 사직로 161")
                .orElseThrow();
        assertThat(geocoded.latitude()).isBetween(
                new BigDecimal("33"), new BigDecimal("39")
        );
        assertThat(geocoded.longitude()).isBetween(
                new BigDecimal("124"), new BigDecimal("132")
        );

        assertThat(client.reverse(geocoded.latitude(), geocoded.longitude()))
                .isPresent();
        assertThat(client.findParking(
                geocoded.latitude(), geocoded.longitude(), 3000, 3
        )).hasSizeLessThanOrEqualTo(3);
    }
}
