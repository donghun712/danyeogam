package com.danyeogam.backend.sync.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import com.danyeogam.backend.geo.application.GeoProvider;
import com.danyeogam.backend.geo.application.GeoProviderException;
import com.danyeogam.backend.geo.application.GeocodedPosition;
import com.danyeogam.backend.tourapi.TouristSummary;
import com.danyeogam.backend.touristspot.domain.CoordinateSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class TourSpotValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GeoProvider geoProvider = mock(GeoProvider.class);
    private final TourSpotValidator validator = new TourSpotValidator(geoProvider);

    @Test
    void acceptsKoreanWgs84CoordinateAndBuildsStableRegionCodeAndHash() {
        TouristSummary summary = summary("126508", "경복궁", "1", "23", "126.9769", "37.5788");

        ValidatedTouristSpot validated = validator.validate(summary);

        assertThat(validated.regionCode()).isEqualTo("TOUR:AREA:1:23");
        assertThat(validated.longitude().toPlainString()).isEqualTo("126.9769000");
        assertThat(validated.latitude().toPlainString()).isEqualTo("37.5788000");
        assertThat(validated.coordinateSource()).isEqualTo(CoordinateSource.TOUR_API);
        assertThat(validated.dataHash()).matches("[0-9a-f]{64}");
        assertThat(validator.validate(summary).dataHash()).isEqualTo(validated.dataHash());
        verifyNoInteractions(geoProvider);
    }

    @Test
    void rejectsMissingTitleAndUsesAddressToRepairInvalidCoordinate() {
        assertThatThrownBy(() -> validator.validate(
                summary("126508", null, "1", "23", "126.9769", "37.5788")
        ))
                .isInstanceOf(TourSpotValidationException.class)
                .hasMessageContaining("관광지명");

        when(geoProvider.geocode("주소")).thenReturn(Optional.of(new GeocodedPosition(
                new BigDecimal("37.5788222"), new BigDecimal("126.9769930")
        )));

        ValidatedTouristSpot repaired = validator.validate(
                summary("126508", "경복궁", "1", "23", "10", "10")
        );

        assertThat(repaired.coordinateSource()).isEqualTo(CoordinateSource.KAKAO_GEOCODE);
        assertThat(repaired.latitude()).isEqualByComparingTo("37.5788222");
        assertThat(repaired.longitude()).isEqualByComparingTo("126.9769930");
    }

    @Test
    void reportsUnavailableGeocoderWithoutExposingProviderDetails() {
        when(geoProvider.geocode("주소")).thenThrow(
                new GeoProviderException("secret provider detail", true, null)
        );

        assertThatThrownBy(() -> validator.validate(
                summary("126508", "경복궁", "1", "23", "", "")
        ))
                .isInstanceOfSatisfying(TourSpotValidationException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("COORDINATE_GEOCODING_UNAVAILABLE");
                    assertThat(exception.retryable()).isTrue();
                    assertThat(exception.getMessage()).doesNotContain("secret");
                });
    }

    private TouristSummary summary(
            String contentId,
            String title,
            String areaCode,
            String districtCode,
            String longitude,
            String latitude
    ) {
        return new TouristSummary(
                contentId, "12", title, "주소", "", areaCode, districtCode,
                longitude, latitude, "https://image.test/original.jpg",
                "https://image.test/thumb.jpg", "20260101000000", "02-0000-0000", "Type1",
                "HS", "HS01", "HS010100",
                objectMapper.createObjectNode()
                        .put("contentid", contentId)
                        .put("title", title)
                        .put("mapx", longitude)
                        .put("mapy", latitude)
        );
    }
}
