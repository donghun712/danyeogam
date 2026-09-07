package com.danyeogam.backend.geo.infrastructure;

import java.math.BigDecimal;
import java.util.Optional;

import com.danyeogam.backend.geo.application.GeoProvider;
import com.danyeogam.backend.geo.application.GeoProviderException;
import com.danyeogam.backend.geo.application.GeocodedPosition;
import com.danyeogam.backend.geo.application.ReverseAddress;
import com.danyeogam.backend.kakao.KakaoLocalClient;
import com.danyeogam.backend.kakao.KakaoLocalException;
import org.springframework.stereotype.Component;

@Component
public class KakaoGeoProvider implements GeoProvider {

    private final KakaoLocalClient client;

    public KakaoGeoProvider(KakaoLocalClient client) {
        this.client = client;
    }

    @Override
    public Optional<GeocodedPosition> geocode(String address) {
        try {
            return client.geocode(address)
                    .map(result -> new GeocodedPosition(result.latitude(), result.longitude()));
        } catch (KakaoLocalException exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public Optional<ReverseAddress> reverse(BigDecimal latitude, BigDecimal longitude) {
        try {
            return client.reverse(latitude, longitude)
                    .map(result -> new ReverseAddress(
                            result.addressName(),
                            result.roadAddressName(),
                            result.depth1(),
                            result.depth2(),
                            result.depth3()
                    ));
        } catch (KakaoLocalException exception) {
            throw unavailable(exception);
        }
    }

    private static GeoProviderException unavailable(KakaoLocalException exception) {
        return new GeoProviderException(
                "카카오 위치 공급자를 사용할 수 없습니다.",
                exception.isRetryable(),
                exception
        );
    }
}
