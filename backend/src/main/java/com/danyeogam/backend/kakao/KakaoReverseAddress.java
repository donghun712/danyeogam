package com.danyeogam.backend.kakao;

public record KakaoReverseAddress(
        String addressName,
        String roadAddressName,
        String depth1,
        String depth2,
        String depth3
) {
}
