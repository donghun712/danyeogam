package com.danyeogam.backend.geo.api;

public record ReverseGeoResponse(
        String addressName,
        String roadAddressName,
        AddressRegionResponse region,
        String source
) {
}
