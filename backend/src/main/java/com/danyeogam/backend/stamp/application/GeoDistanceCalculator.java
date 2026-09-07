package com.danyeogam.backend.stamp.application;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class GeoDistanceCalculator {

    private static final double EARTH_RADIUS_METERS = 6_371_008.8;

    public BigDecimal meters(
            BigDecimal fromLatitude,
            BigDecimal fromLongitude,
            double toLatitude,
            double toLongitude
    ) {
        double lat1 = Math.toRadians(fromLatitude.doubleValue());
        double lat2 = Math.toRadians(toLatitude);
        double deltaLat = lat2 - lat1;
        double deltaLon = Math.toRadians(toLongitude - fromLongitude.doubleValue());
        double sinLat = Math.sin(deltaLat / 2.0);
        double sinLon = Math.sin(deltaLon / 2.0);
        double a = sinLat * sinLat
                + Math.cos(lat1) * Math.cos(lat2) * sinLon * sinLon;
        a = Math.max(0.0, Math.min(1.0, a));
        double distance = 2.0 * EARTH_RADIUS_METERS
                * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP);
    }
}
