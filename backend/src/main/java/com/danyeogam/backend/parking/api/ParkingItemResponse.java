package com.danyeogam.backend.parking.api;

import java.time.Instant;

import com.danyeogam.backend.touristspot.application.dto.NavigationDestinationResponse;
import com.danyeogam.backend.touristspot.application.dto.PositionResponse;

public record ParkingItemResponse(
        String id,
        String name,
        String address,
        PositionResponse position,
        Integer distanceMeters,
        boolean publicVerified,
        String source,
        NavigationDestinationResponse navigation,
        Instant fetchedAt
) {
}
