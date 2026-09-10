package com.danyeogam.backend.sync.application;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

import com.danyeogam.backend.geo.application.GeoProvider;
import com.danyeogam.backend.geo.application.GeoProviderException;
import com.danyeogam.backend.geo.application.GeocodedPosition;
import com.danyeogam.backend.tourapi.TouristSummary;
import com.danyeogam.backend.touristspot.domain.CoordinateSource;
import org.springframework.stereotype.Component;

@Component
class TourSpotValidator {

    private static final BigDecimal MIN_KOREA_LATITUDE = new BigDecimal("32.0");
    private static final BigDecimal MAX_KOREA_LATITUDE = new BigDecimal("39.5");
    private static final BigDecimal MIN_KOREA_LONGITUDE = new BigDecimal("124.0");
    private static final BigDecimal MAX_KOREA_LONGITUDE = new BigDecimal("132.0");

    private final GeoProvider geoProvider;

    TourSpotValidator(GeoProvider geoProvider) {
        this.geoProvider = geoProvider;
    }

    ValidatedTouristSpot validate(TouristSummary summary) {
        requireText(summary.contentId(), "CONTENT_ID_MISSING", "관광지 contentId가 없습니다.");
        requireText(summary.title(), "TITLE_MISSING", "관광지명이 없습니다.");
        requireText(summary.areaCode(), "AREA_CODE_MISSING", "지역 코드가 없습니다.");
        if (!TouristSpotSelectionPolicy.includes(summary)) {
            throw new TourSpotValidationException(
                    "CATEGORY_NOT_SELECTED",
                    "다녀감 관광지 선정 분류에 포함되지 않습니다."
            );
        }

        ResolvedCoordinate coordinate = resolveCoordinate(summary);

        String regionCode = TourSyncPersistenceService.regionCode(summary.areaCode(), summary.districtCode());
        return new ValidatedTouristSpot(
                coordinate.latitude(),
                coordinate.longitude(),
                regionCode,
                coordinate.source(),
                hash(summary, coordinate)
        );
    }

    private ResolvedCoordinate resolveCoordinate(TouristSummary summary) {
        try {
            BigDecimal latitude = coordinate(
                    summary.latitude(), "LATITUDE_INVALID", "위도가 올바르지 않습니다."
            );
            BigDecimal longitude = coordinate(
                    summary.longitude(), "LONGITUDE_INVALID", "경도가 올바르지 않습니다."
            );
            requireKoreanCoordinate(latitude, longitude, "COORDINATE_OUTSIDE_KOREA");
            return new ResolvedCoordinate(latitude, longitude, CoordinateSource.TOUR_API);
        } catch (TourSpotValidationException coordinateFailure) {
            String address = fullAddress(summary);
            if (address == null) {
                throw coordinateFailure;
            }
            return geocode(address);
        }
    }

    private ResolvedCoordinate geocode(String address) {
        Optional<GeocodedPosition> result;
        try {
            result = geoProvider.geocode(address);
        } catch (GeoProviderException exception) {
            throw new TourSpotValidationException(
                    "COORDINATE_GEOCODING_UNAVAILABLE",
                    "주소 기반 좌표 보정 서비스를 사용할 수 없습니다.",
                    exception.isRetryable()
            );
        }
        if (result.isEmpty()) {
            throw new TourSpotValidationException(
                    "COORDINATE_GEOCODING_NOT_FOUND",
                    "관광지 주소를 좌표로 변환하지 못했습니다."
            );
        }
        BigDecimal latitude = result.get().latitude().setScale(7, java.math.RoundingMode.HALF_UP);
        BigDecimal longitude = result.get().longitude().setScale(7, java.math.RoundingMode.HALF_UP);
        requireKoreanCoordinate(latitude, longitude, "GEOCODED_COORDINATE_OUTSIDE_KOREA");
        return new ResolvedCoordinate(latitude, longitude, CoordinateSource.KAKAO_GEOCODE);
    }

    private static void requireKoreanCoordinate(
            BigDecimal latitude,
            BigDecimal longitude,
            String code
    ) {
        if (latitude.compareTo(MIN_KOREA_LATITUDE) < 0 || latitude.compareTo(MAX_KOREA_LATITUDE) > 0
                || longitude.compareTo(MIN_KOREA_LONGITUDE) < 0
                || longitude.compareTo(MAX_KOREA_LONGITUDE) > 0) {
            throw new TourSpotValidationException(
                    code,
                    "대한민국 관광정보 좌표 범위를 벗어났습니다."
            );
        }
    }

    private static BigDecimal coordinate(String value, String code, String message) {
        requireText(value, code, message);
        try {
            return new BigDecimal(value).setScale(7, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            throw new TourSpotValidationException(code, message);
        }
    }

    private static void requireText(String value, String code, String message) {
        if (value == null || value.isBlank()) {
            throw new TourSpotValidationException(code, message);
        }
    }

    private static String hash(TouristSummary summary, ResolvedCoordinate coordinate) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String material = summary.rawPayload().toString();
            if (coordinate.source() == CoordinateSource.KAKAO_GEOCODE) {
                material += "|KAKAO_GEOCODE|" + coordinate.latitude().toPlainString()
                        + "|" + coordinate.longitude().toPlainString();
            }
            byte[] bytes = material.getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.");
        }
    }

    private static String fullAddress(TouristSummary summary) {
        String first = trimToNull(summary.address());
        String second = trimToNull(summary.detailAddress());
        if (first == null) {
            return second;
        }
        return second == null ? first : first + " " + second;
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record ResolvedCoordinate(
            BigDecimal latitude,
            BigDecimal longitude,
            CoordinateSource source
    ) {
    }
}
