package com.danyeogam.backend.parking.api;

import java.util.List;

public record ParkingResponse(
        List<ParkingItemResponse> items,
        boolean temporarilyUnavailable
) {
    public ParkingResponse {
        items = List.copyOf(items);
    }
}
