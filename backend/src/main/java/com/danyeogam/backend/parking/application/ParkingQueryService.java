package com.danyeogam.backend.parking.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.danyeogam.backend.common.error.BusinessException;
import com.danyeogam.backend.common.error.ErrorCode;
import com.danyeogam.backend.parking.api.ParkingItemResponse;
import com.danyeogam.backend.parking.api.ParkingResponse;
import com.danyeogam.backend.parking.config.ParkingQueryProperties;
import com.danyeogam.backend.touristspot.application.dto.NavigationDestinationResponse;
import com.danyeogam.backend.touristspot.application.dto.PositionResponse;
import com.danyeogam.backend.touristspot.domain.TouristSpot;
import com.danyeogam.backend.touristspot.repository.TouristSpotRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
public class ParkingQueryService {

    private static final String SOURCE = "KAKAO_LOCAL";

    private final TouristSpotRepository spotRepository;
    private final ParkingProvider provider;
    private final ParkingQueryProperties properties;
    private final Clock clock;
    private final ConcurrentMap<ParkingCacheKey, CachedParking> cache = new ConcurrentHashMap<>();

    public ParkingQueryService(
            TouristSpotRepository spotRepository,
            ParkingProvider provider,
            ParkingQueryProperties properties,
            Clock clock
    ) {
        this.spotRepository = spotRepository;
        this.provider = provider;
        this.properties = properties;
        this.clock = clock;
    }

    public ParkingResponse findNearby(long spotId, Integer requestedRadius, Integer requestedLimit) {
        TouristSpot spot = spotRepository.findByIdAndActiveTrue(spotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOURIST_SPOT_NOT_FOUND));
        int radius = bounded(requestedRadius, properties.getDefaultRadiusMeters(), properties.getMaxRadiusMeters());
        int limit = bounded(requestedLimit, properties.getDefaultLimit(), properties.getMaxLimit());
        ParkingCacheKey key = new ParkingCacheKey(spotId, radius, limit);
        Instant now = clock.instant();
        CachedParking cached = cache.get(key);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.response();
        }

        BigDecimal latitude = BigDecimal.valueOf(spot.getLocation().getY());
        BigDecimal longitude = BigDecimal.valueOf(spot.getLocation().getX());
        try {
            List<ParkingItemResponse> items = provider.findNearby(
                    latitude, longitude, radius, limit
            ).stream().map(item -> response(item, now)).toList();
            ParkingResponse response = new ParkingResponse(items, false);
            put(key, response, now.plus(properties.getCacheTtl()));
            return response;
        } catch (ParkingProviderException exception) {
            return new ParkingResponse(List.of(), true);
        }
    }

    private static ParkingItemResponse response(NearbyParking item, Instant fetchedAt) {
        PositionResponse position = new PositionResponse(item.latitude(), item.longitude());
        return new ParkingItemResponse(
                item.id(),
                item.name(),
                item.address(),
                position,
                item.distanceMeters(),
                false,
                SOURCE,
                new NavigationDestinationResponse(
                        item.name(), item.latitude(), item.longitude(), "wgs84"
                ),
                fetchedAt
        );
    }

    private void put(ParkingCacheKey key, ParkingResponse response, Instant expiresAt) {
        if (cache.size() >= properties.getCacheMaxEntries()) {
            cache.clear();
        }
        cache.put(key, new CachedParking(response, expiresAt));
    }

    private static int bounded(Integer requested, int defaultValue, int maximum) {
        int value = requested == null ? defaultValue : requested;
        if (value < 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return Math.min(value, maximum);
    }

    private record ParkingCacheKey(long spotId, int radiusMeters, int limit) {
    }

    private record CachedParking(ParkingResponse response, Instant expiresAt) {
    }
}
