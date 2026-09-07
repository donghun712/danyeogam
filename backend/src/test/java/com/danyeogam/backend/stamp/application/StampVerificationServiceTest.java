package com.danyeogam.backend.stamp.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.danyeogam.backend.common.error.BusinessException;
import com.danyeogam.backend.common.error.ErrorCode;
import com.danyeogam.backend.identity.domain.Actor;
import com.danyeogam.backend.identity.repository.ActorRepository;
import com.danyeogam.backend.stamp.api.GpsPositionRequest;
import com.danyeogam.backend.stamp.api.StampVerificationRequest;
import com.danyeogam.backend.stamp.config.StampVerificationProperties;
import com.danyeogam.backend.stamp.domain.VerificationAttempt;
import com.danyeogam.backend.stamp.domain.VerificationResult;
import com.danyeogam.backend.stamp.repository.VerificationAttemptRepository;
import com.danyeogam.backend.touristspot.domain.SpotType;
import com.danyeogam.backend.touristspot.domain.TouristSpot;
import com.danyeogam.backend.touristspot.repository.TouristSpotRepository;
import com.danyeogam.backend.visit.domain.Visit;
import com.danyeogam.backend.visit.repository.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.test.util.ReflectionTestUtils;

class StampVerificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-03T00:00:00Z");
    private static final String KEY = "550e8400-e29b-41d4-a716-446655440000";

    private ActorRepository actorRepository;
    private TouristSpotRepository spotRepository;
    private VerificationAttemptRepository attemptRepository;
    private VisitRepository visitRepository;
    private StampVerificationService service;
    private TouristSpot spot;

    @BeforeEach
    void setUp() {
        actorRepository = mock(ActorRepository.class);
        spotRepository = mock(TouristSpotRepository.class);
        attemptRepository = mock(VerificationAttemptRepository.class);
        visitRepository = mock(VisitRepository.class);
        StampVerificationProperties properties = new StampVerificationProperties();
        properties.setDefaultRadiusMeters(100);
        properties.setMaxAccuracyMeters(new BigDecimal("50"));
        properties.setMaxLocationAge(java.time.Duration.ofMinutes(2));
        service = new StampVerificationService(
                actorRepository, spotRepository, attemptRepository, visitRepository,
                properties, new GeoDistanceCalculator(), Clock.fixed(NOW, ZoneOffset.UTC)
        );
        when(actorRepository.findActiveByIdForUpdate(42L)).thenReturn(Optional.of(Actor.anonymous()));
        when(attemptRepository.findByActorIdAndIdempotencyKey(42L, KEY)).thenReturn(Optional.empty());
        when(attemptRepository.countByActorIdAndCreatedAtGreaterThanEqual(any(), any())).thenReturn(0L);
        when(attemptRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            VerificationAttempt attempt = invocation.getArgument(0);
            ReflectionTestUtils.setField(attempt, "id", 9L);
            return attempt;
        });
        when(visitRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(visitRepository.findByActorIdAndTouristSpotId(42L, 7L)).thenReturn(Optional.empty());

        spot = mock(TouristSpot.class);
        when(spot.getId()).thenReturn(7L);
        when(spot.isActive()).thenReturn(true);
        when(spot.isStampEnabled()).thenReturn(true);
        when(spot.getSpotType()).thenReturn(SpotType.STAMP_TARGET);
        when(spot.getLocation()).thenReturn(new GeometryFactory(new PrecisionModel(), 4326)
                .createPoint(new Coordinate(127.0, 37.0)));
        when(spotRepository.findById(7L)).thenReturn(Optional.of(spot));
    }

    @Test
    void savesAttemptAndVisitAtomicallyInsideRadius() {
        var result = service.verify(42L, KEY, request("37.0", "127.0", "5", NOW));

        assertThat(result.status()).isEqualTo("VERIFIED_NEW");
        assertThat(result.visitState()).isEqualTo("VISITED");
        assertThat(result.collectionChanged()).isTrue();
        verify(attemptRepository).saveAndFlush(any(VerificationAttempt.class));
        verify(visitRepository).saveAndFlush(any(Visit.class));
    }

    @Test
    void returnsNormalBusinessStatusesWithoutCreatingVisit() {
        var inaccurate = service.verify(42L, KEY, request("37.0", "127.0", "51", NOW));
        assertThat(inaccurate.status()).isEqualTo("GPS_ACCURACY_INSUFFICIENT");

        resetAttemptKey("550e8400-e29b-41d4-a716-446655440001");
        var stale = service.verify(42L, "550e8400-e29b-41d4-a716-446655440001",
                request("37.0", "127.0", "5", NOW.minusSeconds(121)));
        assertThat(stale.status()).isEqualTo("LOCATION_STALE");

        resetAttemptKey("550e8400-e29b-41d4-a716-446655440002");
        var outside = service.verify(42L, "550e8400-e29b-41d4-a716-446655440002",
                request("37.01", "127.0", "5", NOW));
        assertThat(outside.status()).isEqualTo("OUT_OF_RANGE");
        assertThat(outside.distanceMeters()).isGreaterThan(new BigDecimal("100"));
        verify(visitRepository, never()).saveAndFlush(any(Visit.class));
    }

    @Test
    void returnsStoredResultForIdempotentReplayAndRejectsDifferentSpot() {
        VerificationAttempt stored = new VerificationAttempt(
                42L, 7L, KEY, VerificationResult.OUT_OF_RANGE,
                new BigDecimal("120.00"), new BigDecimal("5.00"), NOW
        );
        when(attemptRepository.findByActorIdAndIdempotencyKey(42L, KEY))
                .thenReturn(Optional.of(stored));

        assertThat(service.verify(42L, KEY, request("37.0", "127.0", "5", NOW)).status())
                .isEqualTo("OUT_OF_RANGE");

        StampVerificationRequest otherSpot = new StampVerificationRequest(8L,
                new GpsPositionRequest(new BigDecimal("37"), new BigDecimal("127"),
                        new BigDecimal("5"), NOW));
        assertThatThrownBy(() -> service.verify(42L, KEY, otherSpot))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_CONFLICT));
    }

    @Test
    void rateLimitsOnlyNewKeysAfterLockAndReplayCheck() {
        when(attemptRepository.countByActorIdAndCreatedAtGreaterThanEqual(any(), any())).thenReturn(5L);

        assertThatThrownBy(() -> service.verify(42L, KEY, request("37", "127", "5", NOW)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.TOO_MANY_REQUESTS));
    }

    @Test
    void rejectsMissingActorAndMalformedIdempotencyKey() {
        when(actorRepository.findActiveByIdForUpdate(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.verify(42L, KEY, request("37", "127", "5", NOW)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.AUTHENTICATION_REQUIRED));
        assertThatThrownBy(() -> service.verify(42L, "bad", request("37", "127", "5", NOW)))
                .isInstanceOf(BusinessException.class);
    }

    private void resetAttemptKey(String key) {
        when(attemptRepository.findByActorIdAndIdempotencyKey(42L, key)).thenReturn(Optional.empty());
    }

    private static StampVerificationRequest request(
            String latitude, String longitude, String accuracy, Instant measuredAt
    ) {
        return new StampVerificationRequest(7L, new GpsPositionRequest(
                new BigDecimal(latitude), new BigDecimal(longitude),
                new BigDecimal(accuracy), measuredAt
        ));
    }
}
