package com.danyeogam.backend.geo.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.danyeogam.backend.common.error.BusinessException;
import com.danyeogam.backend.common.error.ErrorCode;
import com.danyeogam.backend.geo.api.AddressRegionResponse;
import com.danyeogam.backend.geo.api.ReverseGeoResponse;
import com.danyeogam.backend.geo.config.GeoQueryProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
public class GeoQueryService {

    private static final String SOURCE = "KAKAO_LOCAL";
    private static final int CACHE_COORDINATE_SCALE = 4;

    private final GeoProvider provider;
    private final GeoQueryProperties properties;
    private final Clock clock;
    private final ConcurrentMap<CoordinateKey, CachedAddress> cache = new ConcurrentHashMap<>();

    public GeoQueryService(GeoProvider provider, GeoQueryProperties properties, Clock clock) {
        this.provider = provider;
        this.properties = properties;
        this.clock = clock;
    }

    public ReverseGeoResponse reverse(BigDecimal latitude, BigDecimal longitude) {
        CoordinateKey key = new CoordinateKey(normalize(latitude), normalize(longitude));
        Instant now = clock.instant();
        CachedAddress cached = cache.get(key);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.response();
        }

        try {
            ReverseGeoResponse response = response(provider.reverse(latitude, longitude));
            put(key, response, now.plus(properties.getReverseCacheTtl()));
            return response;
        } catch (GeoProviderException exception) {
            throw new BusinessException(ErrorCode.GEO_PROVIDER_UNAVAILABLE);
        }
    }

    private ReverseGeoResponse response(Optional<ReverseAddress> result) {
        if (result.isEmpty()) {
            return new ReverseGeoResponse(null, null, null, SOURCE);
        }
        ReverseAddress address = result.get();
        AddressRegionResponse region = allBlank(address.depth1(), address.depth2(), address.depth3())
                ? null
                : new AddressRegionResponse(address.depth1(), address.depth2(), address.depth3());
        return new ReverseGeoResponse(
                address.addressName(),
                address.roadAddressName(),
                region,
                SOURCE
        );
    }

    private void put(CoordinateKey key, ReverseGeoResponse response, Instant expiresAt) {
        if (cache.size() >= properties.getReverseCacheMaxEntries()) {
            cache.clear();
        }
        cache.put(key, new CachedAddress(response, expiresAt));
    }

    private static BigDecimal normalize(BigDecimal value) {
        return value.setScale(CACHE_COORDINATE_SCALE, RoundingMode.HALF_UP);
    }

    private static boolean allBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private record CoordinateKey(BigDecimal latitude, BigDecimal longitude) {
    }

    private record CachedAddress(ReverseGeoResponse response, Instant expiresAt) {
    }
}
