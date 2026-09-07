package com.danyeogam.backend.parking.application;

import java.math.BigDecimal;
import java.util.List;

public interface ParkingProvider {

    List<NearbyParking> findNearby(
            BigDecimal latitude,
            BigDecimal longitude,
            int radiusMeters,
            int limit
    );
}
