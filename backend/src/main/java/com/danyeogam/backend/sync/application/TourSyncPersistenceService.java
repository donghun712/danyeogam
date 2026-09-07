package com.danyeogam.backend.sync.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import com.danyeogam.backend.sync.domain.SyncErrorRecord;
import com.danyeogam.backend.sync.domain.SyncRun;
import com.danyeogam.backend.sync.domain.TouristSpotStaging;
import com.danyeogam.backend.sync.repository.SyncErrorRepository;
import com.danyeogam.backend.sync.repository.SyncRunRepository;
import com.danyeogam.backend.sync.repository.TouristSpotStagingRepository;
import com.danyeogam.backend.stamp.config.StampVerificationProperties;
import com.danyeogam.backend.tourapi.TourRegionCode;
import com.danyeogam.backend.tourapi.TouristDetail;
import com.danyeogam.backend.tourapi.TouristImage;
import com.danyeogam.backend.tourapi.TouristSummary;
import com.danyeogam.backend.touristspot.domain.Region;
import com.danyeogam.backend.touristspot.domain.SpotType;
import com.danyeogam.backend.touristspot.domain.TouristSpot;
import com.danyeogam.backend.touristspot.domain.TouristSpotImage;
import com.danyeogam.backend.touristspot.repository.RegionRepository;
import com.danyeogam.backend.touristspot.repository.TouristSpotImageRepository;
import com.danyeogam.backend.touristspot.repository.TouristSpotRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!test")
class TourSyncPersistenceService {

    private static final DateTimeFormatter TOUR_TIME = DateTimeFormatter.ofPattern("uuuuMMddHHmmss");
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final GeometryFactory WGS84 = new GeometryFactory(new PrecisionModel(), 4326);

    private final SyncRunRepository syncRunRepository;
    private final TouristSpotStagingRepository stagingRepository;
    private final SyncErrorRepository errorRepository;
    private final RegionRepository regionRepository;
    private final TouristSpotRepository spotRepository;
    private final TouristSpotImageRepository imageRepository;
    private final TourSpotValidator validator;
    private final TourContentSanitizer sanitizer;
    private final StampVerificationProperties stampProperties;

    TourSyncPersistenceService(
            SyncRunRepository syncRunRepository,
            TouristSpotStagingRepository stagingRepository,
            SyncErrorRepository errorRepository,
            RegionRepository regionRepository,
            TouristSpotRepository spotRepository,
            TouristSpotImageRepository imageRepository,
            TourSpotValidator validator,
            TourContentSanitizer sanitizer,
            StampVerificationProperties stampProperties
    ) {
        this.syncRunRepository = syncRunRepository;
        this.stagingRepository = stagingRepository;
        this.errorRepository = errorRepository;
        this.regionRepository = regionRepository;
        this.spotRepository = spotRepository;
        this.imageRepository = imageRepository;
        this.validator = validator;
        this.sanitizer = sanitizer;
        this.stampProperties = stampProperties;
    }

    @Transactional
    void upsertProvinces(List<TourRegionCode> provinces) {
        for (TourRegionCode item : provinces) {
            String code = provinceCode(item.code());
            Region region = regionRepository.findByCode(code)
                    .orElseGet(() -> Region.province(code, item.name()));
            region.refresh(item.name());
            regionRepository.save(region);
        }
    }

    @Transactional
    void upsertDistricts(String areaCode, List<TourRegionCode> districts) {
        Region province = regionRepository.findByCode(provinceCode(areaCode))
                .orElseThrow(() -> new IllegalStateException("상위 지역이 없습니다: " + areaCode));
        for (TourRegionCode item : districts) {
            String code = regionCode(areaCode, item.code());
            Region region = regionRepository.findByCode(code)
                    .orElseGet(() -> Region.cityCounty(code, item.name(), province));
            region.refresh(item.name());
            regionRepository.save(region);
        }
    }

    @Transactional
    StagingResult stage(long syncRunId, TouristSummary summary) {
        if (summary.contentId() == null || summary.contentId().isBlank()) {
            recordUnstagedError(syncRunId, "CONTENT_ID_MISSING", "관광지 contentId가 없어 staging에 저장할 수 없습니다.");
            return StagingResult.rejected(0);
        }

        SyncRun run = syncRunRepository.getReferenceById(syncRunId);
        TouristSpotStaging staging = TouristSpotStaging.receive(
                run,
                summary.contentId(),
                summary.contentTypeId(),
                summary.title(),
                summary.address(),
                summary.detailAddress(),
                decimalOrNull(summary.latitude()),
                decimalOrNull(summary.longitude()),
                summary.rawPayload()
        );
        stagingRepository.save(staging);

        try {
            ValidatedTouristSpot validated = validator.validate(summary);
            staging.markReady(
                    validated.dataHash(),
                    validated.latitude(),
                    validated.longitude(),
                    validated.coordinateSource()
            );
            return StagingResult.accepted(staging.getId(), validated);
        } catch (TourSpotValidationException exception) {
            if (exception.retryable()) {
                staging.retryLater(exception.code(), exception.getMessage());
            } else {
                staging.reject(exception.code(), exception.getMessage());
            }
            errorRepository.save(new SyncErrorRecord(
                    run,
                    staging,
                    summary.contentId(),
                    exception.code(),
                    exception.getMessage(),
                    exception.retryable()
            ));
            return StagingResult.rejected(staging.getId());
        }
    }

    @Transactional(readOnly = true)
    boolean isUnchanged(String sourceContentId, String dataHash) {
        return spotRepository.findBySourceAndSourceContentId("TOUR_API", sourceContentId)
                .map(spot -> spot.getDataHash().equals(dataHash)
                        && spot.getSpotType() == SpotType.STAMP_TARGET
                        && spot.isStampEnabled())
                .orElse(false);
    }

    @Transactional
    void markPromoted(long stagingId) {
        stagingRepository.findById(stagingId).orElseThrow().markPromoted();
    }

    @Transactional
    ImportOutcome promote(
            long stagingId,
            TouristSummary summary,
            ValidatedTouristSpot validated,
            TouristDetail detail,
            List<TouristImage> images
    ) {
        TouristSpotStaging staging = stagingRepository.findById(stagingId).orElseThrow();
        Region region = regionRepository.findByCode(validated.regionCode())
                .orElseThrow(() -> new TourSpotValidationException(
                        "REGION_NOT_FOUND",
                        "관광지 지역 코드를 DB에서 찾을 수 없습니다."
                ));

        Optional<TouristSpot> existing = spotRepository.findBySourceAndSourceContentId(
                "TOUR_API",
                summary.contentId()
        );
        TouristSpot spot = existing.orElseGet(() -> TouristSpot.fromTourApi(
                summary.contentId(),
                summary.contentTypeId(),
                summary.title(),
                region,
                point(validated.longitude(), validated.latitude()),
                validated.coordinateSource(),
                validated.dataHash()
        ));
        spot.refreshSummary(
                summary.contentTypeId(),
                summary.title(),
                region,
                summary.address(),
                summary.detailAddress(),
                point(validated.longitude(), validated.latitude()),
                validated.coordinateSource(),
                summary.firstThumbnailUrl(),
                summary.firstImageUrl(),
                truncate(summary.telephone(), 100),
                tourTime(summary.modifiedTime()),
                validated.dataHash()
        );

        if (detail != null) {
            spot.hydrateDetail(
                    sanitizer.plainText(detail.overview()),
                    sanitizer.homepageUrl(detail.homepage()),
                    truncate(detail.telephone(), 100),
                    detail.firstThumbnailUrl(),
                    detail.firstImageUrl(),
                    Instant.now()
            );
        }
        spot.enableStampTarget(stampProperties.getDefaultRadiusMeters());
        spotRepository.saveAndFlush(spot);

        if (images != null) {
            imageRepository.deleteAllByTouristSpotId(spot.getId());
            int order = 0;
            for (TouristImage image : images) {
                if (image.originalUrl() == null || image.originalUrl().isBlank()) {
                    continue;
                }
                imageRepository.save(new TouristSpotImage(
                        spot,
                        image.originalUrl(),
                        sanitizer.plainText(image.imageName()),
                        order++,
                        image.copyrightType(),
                        image.serialNumber()
                ));
            }
            if (order == 0 && summary.firstImageUrl() != null && !summary.firstImageUrl().isBlank()) {
                imageRepository.save(new TouristSpotImage(
                        spot,
                        summary.firstImageUrl(),
                        summary.title(),
                        0,
                        summary.copyrightType(),
                        "representative:" + summary.contentId()
                ));
            }
        }

        staging.markPromoted();
        return existing.isPresent() ? ImportOutcome.UPDATED : ImportOutcome.INSERTED;
    }

    @Transactional
    void recordItemError(
            long syncRunId,
            long stagingId,
            String sourceContentId,
            String errorCode,
            String message,
            boolean retryable,
            boolean retryLater
    ) {
        SyncRun run = syncRunRepository.getReferenceById(syncRunId);
        TouristSpotStaging staging = stagingRepository.findById(stagingId).orElseThrow();
        if (retryLater) {
            staging.retryLater(errorCode, message);
        }
        errorRepository.save(new SyncErrorRecord(
                run,
                staging,
                sourceContentId,
                errorCode,
                message,
                retryable
        ));
    }

    @Transactional
    void recordUnstagedError(long syncRunId, String errorCode, String message) {
        SyncRun run = syncRunRepository.getReferenceById(syncRunId);
        errorRepository.save(new SyncErrorRecord(run, null, null, errorCode, message, false));
    }

    static String regionCode(String areaCode, String districtCode) {
        if (districtCode == null || districtCode.isBlank()) {
            return provinceCode(areaCode);
        }
        return provinceCode(areaCode) + ":" + districtCode;
    }

    private static String provinceCode(String areaCode) {
        return "TOUR:AREA:" + areaCode;
    }

    private static BigDecimal decimalOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value).setScale(7, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static org.locationtech.jts.geom.Point point(BigDecimal longitude, BigDecimal latitude) {
        return WGS84.createPoint(new Coordinate(longitude.doubleValue(), latitude.doubleValue()));
    }

    private static Instant tourTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, TOUR_TIME).atZone(KOREA).toInstant();
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
