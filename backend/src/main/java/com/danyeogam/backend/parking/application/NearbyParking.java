package com.danyeogam.backend.parking.application;

import java.math.BigDecimal;

public record NearbyParking(
        String id,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer distanceMeters
) {
}
