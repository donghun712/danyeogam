package com.danyeogam.backend.stamp.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import com.danyeogam.backend.common.error.BusinessException;
import com.danyeogam.backend.common.error.ErrorCode;
import com.danyeogam.backend.identity.repository.ActorRepository;
import com.danyeogam.backend.stamp.api.GpsPositionRequest;
import com.danyeogam.backend.stamp.api.StampVerificationRequest;
import com.danyeogam.backend.stamp.api.StampVerificationResponse;
import com.danyeogam.backend.stamp.config.StampVerificationProperties;
import com.danyeogam.backend.stamp.domain.VerificationAttempt;
import com.danyeogam.backend.stamp.domain.VerificationResult;
import com.danyeogam.backend.stamp.repository.VerificationAttemptRepository;
import com.danyeogam.backend.touristspot.domain.SpotType;
import com.danyeogam.backend.touristspot.domain.TouristSpot;
import com.danyeogam.backend.touristspot.repository.TouristSpotRepository;
import com.danyeogam.backend.visit.domain.Visit;
import com.danyeogam.backend.visit.repository.VisitRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!test")
public class StampVerificationService {

    private static final Instant MYSQL_MIN_INSTANT = Instant.parse("1000-01-01T00:00:00Z");
    private static final Instant MYSQL_MAX_INSTANT = Instant.parse("9999-12-31T23:59:59Z");

    private final ActorRepository actorRepository;
    private final TouristSpotRepository spotRepository;
    private final VerificationAttemptRepository attemptRepository;
    private final VisitRepository visitRepository;
    private final StampVerificationProperties properties;
    private final GeoDistanceCalculator distanceCalculator;
    private final Clock clock;

    public StampVerificationService(
            ActorRepository actorRepository,
            TouristSpotRepository spotRepository,
            VerificationAttemptRepository attemptRepository,
            VisitRepository visitRepository,
            StampVerificationProperties properties,
            GeoDistanceCalculator distanceCalculator,
            Clock clock
    ) {
        this.actorRepository = actorRepository;
        this.spotRepository = spotRepository;
        this.attemptRepository = attemptRepository;
        this.visitRepository = visitRepository;
        this.properties = properties;
        this.distanceCalculator = distanceCalculator;
        this.clock = clock;
    }

    @Transactional
    public StampVerificationResponse verify(
            long actorId,
            String rawIdempotencyKey,
            StampVerificationRequest request
    ) {
        String idempotencyKey = normalizeIdempotencyKey(rawIdempotencyKey);
        Instant now = clock.instant();
        actorRepository.findActiveByIdForUpdate(actorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED));

        Optional<VerificationAttempt> replay = attemptRepository
                .findByActorIdAndIdempotencyKey(actorId, idempotencyKey);
        if (replay.isPresent()) {
            if (!replay.get().getTouristSpotId().equals(request.touristSpotId())) {
                throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
            }
            return responseForStored(actorId, replay.get());
        }

        long recentAttempts = attemptRepository.countByActorIdAndCreatedAtGreaterThanEqual(
                actorId, now.minusSeconds(60)
        );
        if (recentAttempts >= properties.getMaxAttemptsPerMinute()) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }

        TouristSpot spot = spotRepository.findById(request.touristSpotId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TOURIST_SPOT_NOT_FOUND));
        GpsPositionRequest position = request.position();
        if (position.measuredAt().isBefore(MYSQL_MIN_INSTANT)
                || position.measuredAt().isAfter(MYSQL_MAX_INSTANT)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        BigDecimal accuracy = position.accuracyMeters().setScale(2, RoundingMode.HALF_UP);

        if (position.accuracyMeters().compareTo(properties.getMaxAccuracyMeters()) > 0) {
            return recordResult(actorId, spot.getId(), idempotencyKey,
                    VerificationResult.GPS_ACCURACY_INSUFFICIENT, null, accuracy,
                    position.measuredAt(), null);
        }
        if (position.measuredAt().isBefore(now.minus(properties.getMaxLocationAge()))
                || position.measuredAt().isAfter(now.plus(properties.getMaxFutureSkew()))) {
            return recordResult(actorId, spot.getId(), idempotencyKey,
                    VerificationResult.LOCATION_STALE, null, accuracy,
                    position.measuredAt(), null);
        }
        if (!spot.isActive() || !spot.isStampEnabled() || spot.getSpotType() != SpotType.STAMP_TARGET) {
            return recordResult(actorId, spot.getId(), idempotencyKey,
                    VerificationResult.STAMP_DISABLED, null, accuracy,
                    position.measuredAt(), null);
        }

        BigDecimal distance = distanceCalculator.meters(
                position.latitude(), position.longitude(),
                spot.getLocation().getY(), spot.getLocation().getX()
        );
        int radiusMeters = spot.getStampRadiusMeters() == null
                ? properties.getDefaultRadiusMeters()
                : spot.getStampRadiusMeters();
        if (distance.compareTo(BigDecimal.valueOf(radiusMeters)) > 0) {
            return recordResult(actorId, spot.getId(), idempotencyKey,
                    VerificationResult.OUT_OF_RANGE, distance, accuracy,
                    position.measuredAt(), null);
        }

        Optional<Visit> existingVisit = visitRepository.findByActorIdAndTouristSpotId(
                actorId, spot.getId()
        );
        if (existingVisit.isPresent()) {
            return recordResult(actorId, spot.getId(), idempotencyKey,
                    VerificationResult.VERIFIED_ALREADY_ACQUIRED, distance, accuracy,
                    position.measuredAt(), existingVisit.get());
        }

        VerificationAttempt attempt = saveAttempt(
                actorId, spot.getId(), idempotencyKey, VerificationResult.VERIFIED_NEW,
                distance, accuracy, position.measuredAt()
        );
        Visit visit = visitRepository.saveAndFlush(new Visit(
                actorId, spot.getId(), attempt.getId(), now, distance
        ));
        return response(VerificationResult.VERIFIED_NEW, spot.getId(), distance,
                visit.getVerifiedAt(), true, true);
    }

    private StampVerificationResponse recordResult(
            long actorId,
            long spotId,
            String key,
            VerificationResult result,
            BigDecimal distance,
            BigDecimal accuracy,
            Instant measuredAt,
            Visit knownVisit
    ) {
        saveAttempt(actorId, spotId, key, result, distance, accuracy, measuredAt);
        Visit visit = knownVisit != null ? knownVisit
                : visitRepository.findByActorIdAndTouristSpotId(actorId, spotId).orElse(null);
        return response(result, spotId, distance,
                result == VerificationResult.VERIFIED_ALREADY_ACQUIRED && visit != null
                        ? visit.getVerifiedAt() : null,
                visit != null,
                false);
    }

    private VerificationAttempt saveAttempt(
            long actorId,
            long spotId,
            String key,
            VerificationResult result,
            BigDecimal distance,
            BigDecimal accuracy,
            Instant measuredAt
    ) {
        return attemptRepository.saveAndFlush(new VerificationAttempt(
                actorId, spotId, key, result, distance, accuracy, measuredAt
        ));
    }

    private StampVerificationResponse responseForStored(
            long actorId,
            VerificationAttempt attempt
    ) {
        Visit visit = visitRepository.findByActorIdAndTouristSpotId(
                actorId, attempt.getTouristSpotId()
        ).orElse(null);
        boolean changed = attempt.getResult() == VerificationResult.VERIFIED_NEW;
        Instant verifiedAt = changed || attempt.getResult() == VerificationResult.VERIFIED_ALREADY_ACQUIRED
                ? visit == null ? null : visit.getVerifiedAt()
                : null;
        return response(attempt.getResult(), attempt.getTouristSpotId(),
                attempt.getDistanceMeters(), verifiedAt, visit != null, changed);
    }

    private static StampVerificationResponse response(
            VerificationResult result,
            long spotId,
            BigDecimal distance,
            Instant verifiedAt,
            boolean visited,
            boolean collectionChanged
    ) {
        return new StampVerificationResponse(
                result.name(), spotId, distance, verifiedAt,
                visited ? "VISITED" : "NOT_VISITED",
                collectionChanged,
                List.of()
        );
    }

    static String normalizeIdempotencyKey(String rawKey) {
        if (rawKey == null || rawKey.length() != 36) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        try {
            String normalized = UUID.fromString(rawKey).toString().toLowerCase(Locale.ROOT);
            if (!normalized.equals(rawKey.toLowerCase(Locale.ROOT))) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
            return normalized;
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}
