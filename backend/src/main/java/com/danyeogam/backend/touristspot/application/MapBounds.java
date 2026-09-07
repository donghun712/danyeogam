package com.danyeogam.backend.touristspot.application;

import java.math.BigDecimal;

import com.danyeogam.backend.common.error.BusinessException;
import com.danyeogam.backend.common.error.ErrorCode;
import com.danyeogam.backend.touristspot.config.MapQueryProperties;

public record MapBounds(
        BigDecimal northEastLatitude,
        BigDecimal northEastLongitude,
        BigDecimal southWestLatitude,
        BigDecimal southWestLongitude
) {
    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");

    public MapBounds {
        if (northEastLatitude == null || northEastLongitude == null
                || southWestLatitude == null || southWestLongitude == null) {
            throw new BusinessException(ErrorCode.INVALID_BOUNDS);
        }
        if (!between(northEastLatitude, MIN_LATITUDE, MAX_LATITUDE)
                || !between(southWestLatitude, MIN_LATITUDE, MAX_LATITUDE)
                || !between(northEastLongitude, MIN_LONGITUDE, MAX_LONGITUDE)
                || !between(southWestLongitude, MIN_LONGITUDE, MAX_LONGITUDE)
                || southWestLatitude.compareTo(northEastLatitude) >= 0
                || southWestLongitude.compareTo(northEastLongitude) >= 0) {
            throw new BusinessException(ErrorCode.INVALID_BOUNDS);
        }
    }

    public void validateSpan(MapQueryProperties properties) {
        BigDecimal latitudeSpan = northEastLatitude.subtract(southWestLatitude);
        BigDecimal longitudeSpan = northEastLongitude.subtract(southWestLongitude);
        if (latitudeSpan.compareTo(properties.getMaxLatitudeSpan()) > 0
                || longitudeSpan.compareTo(properties.getMaxLongitudeSpan()) > 0) {
            throw new BusinessException(ErrorCode.BOUNDS_TOO_WIDE);
        }
    }

    public String polygonWkt() {
        String swLat = southWestLatitude.toPlainString();
        String swLon = southWestLongitude.toPlainString();
        String neLat = northEastLatitude.toPlainString();
        String neLon = northEastLongitude.toPlainString();
        return "POLYGON((" + swLon + " " + swLat + ","
                + neLon + " " + swLat + ","
                + neLon + " " + neLat + ","
                + swLon + " " + neLat + ","
                + swLon + " " + swLat + "))";
    }

    private static boolean between(BigDecimal value, BigDecimal min, BigDecimal max) {
        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }
}
