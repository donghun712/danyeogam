package com.danyeogam.backend.parking.infrastructure;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import com.danyeogam.backend.kakao.KakaoLocalClient;
import com.danyeogam.backend.kakao.KakaoLocalException;
import com.danyeogam.backend.parking.application.NearbyParking;
import com.danyeogam.backend.parking.application.ParkingProvider;
import com.danyeogam.backend.parking.application.ParkingProviderException;
import org.springframework.stereotype.Component;

@Component
public class KakaoParkingProvider implements ParkingProvider {

    private final KakaoLocalClient client;

    public KakaoParkingProvider(KakaoLocalClient client) {
        this.client = client;
    }

    @Override
    public List<NearbyParking> findNearby(
            BigDecimal latitude,
            BigDecimal longitude,
            int radiusMeters,
            int limit
    ) {
        try {
            return client.findParking(latitude, longitude, radiusMeters, limit).stream()
                    .map(place -> new NearbyParking(
                            "KAKAO:" + place.id(),
                            place.name(),
                            place.address(),
                            place.latitude(),
                            place.longitude(),
                            place.distanceMeters()
                    ))
                    .sorted(Comparator.comparing(
                            NearbyParking::distanceMeters,
                            Comparator.nullsLast(Integer::compareTo)
                    ))
                    .limit(limit)
                    .toList();
        } catch (KakaoLocalException exception) {
            throw new ParkingProviderException("카카오 주차장 공급자를 사용할 수 없습니다.", exception);
        }
    }
}
