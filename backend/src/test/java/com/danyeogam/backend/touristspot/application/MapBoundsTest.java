package com.danyeogam.backend.touristspot.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.danyeogam.backend.common.error.BusinessException;
import com.danyeogam.backend.common.error.ErrorCode;
import com.danyeogam.backend.touristspot.config.MapQueryProperties;
import org.junit.jupiter.api.Test;

class MapBoundsTest {

    @Test
    void buildsLongitudeLatitudeWktInCorrectOrder() {
        MapBounds bounds = bounds("37.6", "127.2", "37.4", "126.8");

        assertThat(bounds.polygonWkt()).isEqualTo(
                "POLYGON((126.8 37.4,127.2 37.4,127.2 37.6,126.8 37.6,126.8 37.4))"
        );
    }

    @Test
    void rejectsReversedAndOutOfRangeCoordinates() {
        assertThatThrownBy(() -> bounds("37.4", "127.2", "37.6", "126.8"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_BOUNDS));
        assertThatThrownBy(() -> bounds("91", "127.2", "37.4", "126.8"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsBoundsWiderThanConfiguredSpan() {
        MapQueryProperties properties = new MapQueryProperties();
        properties.setMaxLatitudeSpan(new BigDecimal("0.5"));
        properties.setMaxLongitudeSpan(new BigDecimal("0.5"));

        assertThatThrownBy(() -> bounds("38.0", "127.2", "37.0", "126.8").validateSpan(properties))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BOUNDS_TOO_WIDE));
    }

    private static MapBounds bounds(String neLat, String neLon, String swLat, String swLon) {
        return new MapBounds(
                new BigDecimal(neLat),
                new BigDecimal(neLon),
                new BigDecimal(swLat),
                new BigDecimal(swLon)
        );
    }
}
