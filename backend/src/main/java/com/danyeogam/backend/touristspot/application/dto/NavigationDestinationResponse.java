package com.danyeogam.backend.touristspot.application.dto;

import java.math.BigDecimal;

public record NavigationDestinationResponse(
        String destinationName,
        BigDecimal latitude,
        BigDecimal longitude,
        String coordinateType
) {
}
