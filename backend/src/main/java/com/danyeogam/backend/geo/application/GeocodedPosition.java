package com.danyeogam.backend.geo.application;

import java.math.BigDecimal;

public record GeocodedPosition(
        BigDecimal latitude,
        BigDecimal longitude
) {
}
