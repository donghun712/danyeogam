package com.danyeogam.backend.stamp.api;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record GpsPositionRequest(
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        @NotNull @DecimalMin("0.0") @DecimalMax("1000000.0") BigDecimal accuracyMeters,
        @NotNull Instant measuredAt
) {
}
