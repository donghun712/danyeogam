package com.danyeogam.backend.sync.application;

import java.math.BigDecimal;

import com.danyeogam.backend.touristspot.domain.CoordinateSource;

record ValidatedTouristSpot(
        BigDecimal latitude,
        BigDecimal longitude,
        String regionCode,
        CoordinateSource coordinateSource,
        String dataHash
) {
}
