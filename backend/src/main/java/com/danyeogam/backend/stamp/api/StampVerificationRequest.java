package com.danyeogam.backend.stamp.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StampVerificationRequest(
        @NotNull @Positive Long touristSpotId,
        @NotNull @Valid GpsPositionRequest position
) {
}
