package com.danyeogam.backend.touristspot.application;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.danyeogam.backend.common.error.BusinessException;
import com.danyeogam.backend.common.error.ErrorCode;
import com.danyeogam.backend.favorite.repository.FavoriteRepository;
import com.danyeogam.backend.touristspot.application.dto.NavigationDestinationResponse;
import com.danyeogam.backend.touristspot.application.dto.FacilityInfoResponse;
import com.danyeogam.backend.touristspot.application.dto.FacilityStatusResponse;
import com.danyeogam.backend.touristspot.application.dto.OperatingInfoResponse;
import com.danyeogam.backend.touristspot.application.dto.PositionResponse;
import com.danyeogam.backend.touristspot.application.dto.RegionListData;
import com.danyeogam.backend.touristspot.application.dto.RegionResponse;
import com.danyeogam.backend.touristspot.application.dto.TouristSpotAddressResponse;
import com.danyeogam.backend.touristspot.application.dto.TouristSpotDetailResponse;
import com.danyeogam.backend.touristspot.application.dto.TouristSpotImageResponse;
import com.danyeogam.backend.touristspot.application.dto.TouristSpotMapData;
import com.danyeogam.backend.touristspot.application.dto.TouristSpotMapItemResponse;
import com.danyeogam.backend.touristspot.config.MapQueryProperties;
import com.danyeogam.backend.touristspot.domain.Region;
import com.danyeogam.backend.touristspot.domain.RegionLevel;
import com.danyeogam.backend.touristspot.domain.SpotType;
import com.danyeogam.backend.touristspot.domain.TouristSpot;
import com.danyeogam.backend.touristspot.domain.TouristSpotImage;
import com.danyeogam.backend.touristspot.repository.RegionRepository;
import com.danyeogam.backend.touristspot.repository.TouristSpotImageRepository;
import com.danyeogam.backend.touristspot.repository.TouristSpotMapProjection;
import com.danyeogam.backend.touristspot.repository.TouristSpotRepository;
import com.danyeogam.backend.visit.repository.VisitRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!test")
public class TouristSpotQueryService {

    private static final String VISIT_STATE_UNKNOWN = "UNKNOWN";
    private static final String VISIT_STATE_NOT_VISITED = "NOT_VISITED";
    private static final String VISIT_STATE_VISITED = "VISITED";

    private final RegionRepository regionRepository;
    private final TouristSpotRepository spotRepository;
    private final TouristSpotImageRepository imageRepository;
    private final VisitRepository visitRepository;
    private final FavoriteRepository favoriteRepository;
    private final MapQueryProperties properties;

    public TouristSpotQueryService(
            RegionRepository regionRepository,
            TouristSpotRepository spotRepository,
            TouristSpotImageRepository imageRepository,
            VisitRepository visitRepository,
            FavoriteRepository favoriteRepository,
            MapQueryProperties properties
    ) {
        this.regionRepository = regionRepository;
        this.spotRepository = spotRepository;
        this.imageRepository = imageRepository;
        this.visitRepository = visitRepository;
        this.favoriteRepository = favoriteRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public RegionListData getRegions() {
        List<RegionResponse> provinces = regionRepository
                .findAllByParentIsNullAndActiveTrueOrderByNameAsc()
                .stream()
                .map(province -> regionResponse(
                        province,
                        regionRepository.findAllByParentIdAndActiveTrueOrderByNameAsc(province.getId())
                                .stream()
                                .map(child -> regionResponse(child, List.of()))
                                .toList()
                ))
                .toList();
        return new RegionListData(provinces);
    }

    @Transactional(readOnly = true)
    public TouristSpotMapData getMapSpots(MapBounds bounds, String typesCsv) {
        return getMapSpots(bounds, typesCsv, null);
    }

    @Transactional(readOnly = true)
    public TouristSpotMapData getMapSpots(MapBounds bounds, String typesCsv, Long actorId) {
        bounds.validateSpan(properties);
        Set<SpotType> types = parseTypes(typesCsv);
        int queryLimit = properties.getMaxResults() + 1;
        List<TouristSpotMapProjection> projections = types.isEmpty()
                ? spotRepository.findActiveWithinBounds(bounds.polygonWkt(), queryLimit)
                : spotRepository.findActiveWithinBoundsAndTypes(
                        bounds.polygonWkt(),
                        types.stream().map(Enum::name).sorted().toList(),
                        queryLimit
                );
        if (projections.size() > properties.getMaxResults()) {
            throw new BusinessException(ErrorCode.BOUNDS_TOO_DENSE);
        }

        Set<Long> visitedSpotIds = findVisitedSpotIds(actorId, projections);

        List<TouristSpotMapItemResponse> items = projections.stream()
                .map(item -> new TouristSpotMapItemResponse(
                        item.getId(),
                        item.getName(),
                        new PositionResponse(item.getLatitude(), item.getLongitude()),
                        item.getSpotType(),
                        Boolean.TRUE.equals(item.getStampEnabled()),
                        visitState(actorId, item.getId(), visitedSpotIds),
                        blankToNull(item.getThumbnailUrl())
                ))
                .toList();
        return new TouristSpotMapData(items);
    }

    @Transactional(readOnly = true)
    public TouristSpotDetailResponse getDetail(long spotId) {
        return getDetail(spotId, null);
    }

    @Transactional(readOnly = true)
    public TouristSpotDetailResponse getDetail(long spotId, Long actorId) {
        TouristSpot spot = spotRepository.findByIdAndActiveTrue(spotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOURIST_SPOT_NOT_FOUND));
        List<TouristSpotImageResponse> images = imageRepository
                .findAllByTouristSpotIdOrderBySortOrderAscIdAsc(spotId)
                .stream()
                .map(this::imageResponse)
                .toList();
        if (images.isEmpty() && hasText(spot.getOriginalImageUrl())) {
            images = List.of(new TouristSpotImageResponse(
                    spot.getOriginalImageUrl(),
                    spot.getName(),
                    null
            ));
        }

        BigDecimal latitude = BigDecimal.valueOf(spot.getLocation().getY());
        BigDecimal longitude = BigDecimal.valueOf(spot.getLocation().getX());
        return new TouristSpotDetailResponse(
                spot.getId(),
                spot.getName(),
                spot.getSpotType().name(),
                spot.isStampEnabled(),
                actorId == null
                        ? VISIT_STATE_UNKNOWN
                        : visitRepository.existsByActorIdAndTouristSpotId(actorId, spotId)
                                ? VISIT_STATE_VISITED
                                : VISIT_STATE_NOT_VISITED,
                new TouristSpotAddressResponse(
                        blankToNull(spot.getRoadAddress()),
                        blankToNull(spot.getLotAddress())
                ),
                new PositionResponse(latitude, longitude),
                blankToNull(spot.getOverview()),
                images,
                blankToNull(spot.getTel()),
                blankToNull(spot.getHomepageUrl()),
                new NavigationDestinationResponse(
                        spot.getName(),
                        latitude,
                        longitude,
                        "wgs84"
                ),
                "한국관광공사 TourAPI",
                spot.getDetailHydratedAt() == null ? "PARTIAL" : "COMPLETE",
                spot.getUpdatedAt() != null ? spot.getUpdatedAt() : spot.getSourceModifiedAt(),
                resolveProvinceRegionCode(spot.getRegion()),
                operatingInfo(spot),
                facilityInfo(spot),
                actorId != null && favoriteRepository
                        .existsByActorIdAndTouristSpotId(actorId, spotId)
        );
    }

    private static OperatingInfoResponse operatingInfo(TouristSpot spot) {
        if (spot.getIntroHydratedAt() == null) {
            return null;
        }
        String hours = blankToNull(spot.getOperatingHours());
        String closedDays = blankToNull(spot.getClosedDays());
        return hours == null && closedDays == null
                ? null
                : new OperatingInfoResponse(hours, closedDays);
    }

    private static FacilityInfoResponse facilityInfo(TouristSpot spot) {
        if (spot.getIntroHydratedAt() == null) {
            return null;
        }
        return new FacilityInfoResponse(
                facilityStatus(spot.getParkingNote()),
                blankToNull(spot.getParkingFeeNote()),
                facilityStatus(spot.getStrollerRentalNote()),
                facilityStatus(spot.getPetAllowedNote())
        );
    }

    private static FacilityStatusResponse facilityStatus(String rawNote) {
        String note = blankToNull(rawNote);
        if (note == null) {
            return new FacilityStatusResponse("UNKNOWN", null);
        }
        String normalized = note.stripLeading();
        if (normalized.startsWith("불가능") || normalized.startsWith("불가")) {
            return new FacilityStatusResponse("UNAVAILABLE", note);
        }
        if (normalized.startsWith("가능")) {
            return new FacilityStatusResponse("AVAILABLE", note);
        }
        return new FacilityStatusResponse("UNKNOWN", note);
    }

    private static String resolveProvinceRegionCode(Region region) {
        Set<String> visitedCodes = new HashSet<>();
        for (Region current = region; current != null; current = current.getParent()) {
            if (!visitedCodes.add(current.getCode())) {
                break;
            }
            if (current.getLevel() == RegionLevel.PROVINCE) {
                return current.getCode();
            }
        }
        throw new IllegalStateException("관광지의 상위 광역 지역을 찾을 수 없습니다.");
    }

    private RegionResponse regionResponse(Region region, List<RegionResponse> children) {
        return new RegionResponse(
                region.getId(),
                region.getCode(),
                region.getName(),
                region.getLevel().name(),
                children
        );
    }

    private TouristSpotImageResponse imageResponse(TouristSpotImage image) {
        return new TouristSpotImageResponse(
                image.getUrl(),
                blankToNull(image.getAltText()),
                blankToNull(image.getCopyrightType())
        );
    }

    private Set<Long> findVisitedSpotIds(
            Long actorId,
            List<TouristSpotMapProjection> projections
    ) {
        if (actorId == null || projections.isEmpty()) {
            return Set.of();
        }
        List<Long> spotIds = projections.stream()
                .map(TouristSpotMapProjection::getId)
                .toList();
        return new HashSet<>(visitRepository.findVisitedSpotIds(actorId, spotIds));
    }

    private static String visitState(Long actorId, Long spotId, Set<Long> visitedSpotIds) {
        if (actorId == null) {
            return VISIT_STATE_UNKNOWN;
        }
        return visitedSpotIds.contains(spotId)
                ? VISIT_STATE_VISITED
                : VISIT_STATE_NOT_VISITED;
    }

    private static Set<SpotType> parseTypes(String typesCsv) {
        if (!hasText(typesCsv)) {
            return Set.of();
        }
        EnumSet<SpotType> types = EnumSet.noneOf(SpotType.class);
        for (String raw : typesCsv.split(",")) {
            if (raw.isBlank()) {
                continue;
            }
            try {
                types.add(SpotType.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                throw new BusinessException(ErrorCode.INVALID_SPOT_TYPE);
            }
        }
        return types;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String blankToNull(String value) {
        return hasText(value) ? value : null;
    }
}
