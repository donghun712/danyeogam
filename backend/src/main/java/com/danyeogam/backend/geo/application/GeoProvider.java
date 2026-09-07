package com.danyeogam.backend.geo.application;

import java.math.BigDecimal;
import java.util.Optional;

public interface GeoProvider {

    Optional<GeocodedPosition> geocode(String address);

    Optional<ReverseAddress> reverse(BigDecimal latitude, BigDecimal longitude);
}
