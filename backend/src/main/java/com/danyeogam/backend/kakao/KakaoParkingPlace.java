package com.danyeogam.backend.kakao;

import java.math.BigDecimal;

public record KakaoParkingPlace(
        String id,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer distanceMeters
) {
}
