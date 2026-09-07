package com.danyeogam.backend.touristspot.application.dto;

import java.math.BigDecimal;

public record PositionResponse(
        BigDecimal latitude,
        BigDecimal longitude
) {
}
