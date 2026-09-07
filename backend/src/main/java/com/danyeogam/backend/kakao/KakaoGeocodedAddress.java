package com.danyeogam.backend.kakao;

import java.math.BigDecimal;

public record KakaoGeocodedAddress(
        BigDecimal latitude,
        BigDecimal longitude,
        String addressName,
        String roadAddressName
) {
}
