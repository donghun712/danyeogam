package com.danyeogam.backend.geo.application;

public record ReverseAddress(
        String addressName,
        String roadAddressName,
        String depth1,
        String depth2,
        String depth3
) {
}
