package com.danyeogam.backend.geo.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ReverseGeoRequest(
        @NotNull @Valid GeoPositionRequest position
) {
}
