package com.danyeogam.backend.sync.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.danyeogam.backend.sync.domain.SyncRun;
import com.danyeogam.backend.sync.repository.SyncRunRepository;
import com.danyeogam.backend.tourapi.TourApiClient;
import com.danyeogam.backend.tourapi.TourApiPage;
import com.danyeogam.backend.tourapi.TourRegionCode;
import com.danyeogam.backend.tourapi.TouristDetail;
import com.danyeogam.backend.tourapi.TouristImage;
import com.danyeogam.backend.tourapi.TouristIntro;
import com.danyeogam.backend.tourapi.TouristSummary;
import com.danyeogam.backend.touristspot.domain.CoordinateSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TourSyncServiceTest {

    @Test
    void orchestratesRegionSummaryDetailImageAndPromotion() {
        TourApiClient client = mock(TourApiClient.class);
        TourSyncPersistenceService persistence = mock(TourSyncPersistenceService.class);
        SyncRunRepository runRepository = mock(SyncRunRepository.class);
        TouristSummary summary = summary();
        ValidatedTouristSpot validated = new ValidatedTouristSpot(
                new java.math.BigDecimal("37.5788000"),
                new java.math.BigDecimal("126.9769000"),
                "TOUR:AREA:1:23",
                CoordinateSource.TOUR_API,
                "a".repeat(64)
        );
        TouristDetail detail = new TouristDetail(
                "126508", "경복궁", "서울", "", "126.9769", "37.5788",
                "소개", "https://example.com", "02-0000-0000", "original", "thumb",
                "20260101000000", "Type1"
        );
        TouristImage image = new TouristImage(
                "126508", "전경", "https://image.test/original.jpg",
                "https://image.test/thumb.jpg", "1", "Type1"
        );
        TouristIntro intro = new TouristIntro(
                "126508", "12", "09:00~18:00", "매주 화요일",
                "가능", null, "없음", ""
        );

        when(runRepository.save(any(SyncRun.class))).thenAnswer(invocation -> {
            SyncRun run = invocation.getArgument(0);
            if (run.getId() == null) {
                ReflectionTestUtils.setField(run, "id", 1L);
            }
            return run;
        });
        when(client.getLegalDongCodes(isNull(), eq(1), eq(100)))
                .thenReturn(new TourApiPage<>(1, 1, 100, List.of(new TourRegionCode("1", "서울특별시"))));
        when(client.getAreaBasedList(
                anyInt(), anyInt(), eq("1"), any(), any(), any(), nullable(String.class)
        )).thenReturn(new TourApiPage<>(1, 0, 10, List.of()));
        when(client.getAreaBasedList(1, 10, "1", "12", "HS", "HS01", null))
                .thenReturn(new TourApiPage<>(1, 1, 10, List.of(summary)));
        when(client.getLegalDongCodes("1", 1, 1000))
                .thenReturn(new TourApiPage<>(1, 1, 1000, List.of(new TourRegionCode("23", "종로구"))));
        when(persistence.stage(1L, summary)).thenReturn(StagingResult.accepted(11L, validated));
        when(persistence.isUnchanged(summary, validated.dataHash())).thenReturn(false);
        when(client.getCommonDetail("126508")).thenReturn(Optional.of(detail));
        when(client.getImages("126508", 1, 100))
                .thenReturn(new TourApiPage<>(1, 1, 100, List.of(image)));
        when(client.getIntroDetail("126508", "12")).thenReturn(Optional.of(intro));
        when(persistence.promote(11L, summary, validated, detail, intro, List.of(image)))
                .thenReturn(ImportOutcome.INSERTED);

        TourSyncResult result = new TourSyncService(client, persistence, runRepository)
                .synchronize(new TourSyncCommand("1", 10, 1, true));

        assertThat(result.requestedCount()).isEqualTo(1);
        assertThat(result.processedCount()).isEqualTo(1);
        assertThat(result.insertedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isZero();
        assertThat(result.apiRequestCount()).isEqualTo(13);
        verify(persistence).upsertProvinces(List.of(new TourRegionCode("1", "서울특별시")));
        verify(persistence).upsertDistricts("1", List.of(new TourRegionCode("23", "종로구")));
        verify(persistence).updateClassificationIfPresent(summary);
        verify(persistence, never()).deactivateOutsideSelectionPolicy();
    }

    @Test
    void backfillsOnlyIntroWhenUnchangedSpotHasNoIntroData() {
        TourApiClient client = mock(TourApiClient.class);
        TourSyncPersistenceService persistence = mock(TourSyncPersistenceService.class);
        SyncRunRepository runRepository = mock(SyncRunRepository.class);
        TouristSummary summary = summary();
        ValidatedTouristSpot validated = new ValidatedTouristSpot(
                new java.math.BigDecimal("37.5788000"),
                new java.math.BigDecimal("126.9769000"),
                "TOUR:AREA:1:23", CoordinateSource.TOUR_API, "a".repeat(64)
        );
        TouristIntro intro = new TouristIntro(
                "126508", "12", "상시 개방", "연중무휴", "가능", null, null, null
        );
        when(runRepository.save(any(SyncRun.class))).thenAnswer(invocation -> {
            SyncRun run = invocation.getArgument(0);
            if (run.getId() == null) {
                ReflectionTestUtils.setField(run, "id", 1L);
            }
            return run;
        });
        when(client.getLegalDongCodes(isNull(), eq(1), eq(100)))
                .thenReturn(new TourApiPage<>(1, 1, 100, List.of(new TourRegionCode("1", "서울특별시"))));
        when(client.getAreaBasedList(
                anyInt(), anyInt(), eq("1"), any(), any(), any(), nullable(String.class)
        )).thenReturn(new TourApiPage<>(1, 0, 10, List.of()));
        when(client.getAreaBasedList(1, 10, "1", "12", "HS", "HS01", null))
                .thenReturn(new TourApiPage<>(1, 1, 10, List.of(summary)));
        when(client.getLegalDongCodes("1", 1, 1000))
                .thenReturn(new TourApiPage<>(1, 1, 1000, List.of(new TourRegionCode("23", "종로구"))));
        when(persistence.stage(1L, summary)).thenReturn(StagingResult.accepted(11L, validated));
        when(persistence.isUnchanged(summary, validated.dataHash())).thenReturn(true);
        when(persistence.needsIntroHydration(summary)).thenReturn(true);
        when(client.getIntroDetail("126508", "12")).thenReturn(Optional.of(intro));
        when(persistence.promote(11L, summary, validated, null, intro, null))
                .thenReturn(ImportOutcome.UPDATED);

        TourSyncResult result = new TourSyncService(client, persistence, runRepository)
                .synchronize(new TourSyncCommand("1", 10, 1, true));

        assertThat(result.updatedCount()).isEqualTo(1);
        verify(client, never()).getCommonDetail(any());
        verify(client, never()).getImages(any(), anyInt(), anyInt());
        verify(client).getIntroDetail("126508", "12");
    }

    @Test
    void deactivatesExcludedSpotsOnlyAfterCompleteNationalSync() {
        TourApiClient client = mock(TourApiClient.class);
        TourSyncPersistenceService persistence = mock(TourSyncPersistenceService.class);
        SyncRunRepository runRepository = mock(SyncRunRepository.class);
        when(runRepository.save(any(SyncRun.class))).thenAnswer(invocation -> {
            SyncRun run = invocation.getArgument(0);
            if (run.getId() == null) {
                ReflectionTestUtils.setField(run, "id", 1L);
            }
            return run;
        });
        when(client.getLegalDongCodes(isNull(), eq(1), eq(100)))
                .thenReturn(new TourApiPage<>(1, 1, 100, List.of(new TourRegionCode("11", "서울특별시"))));
        when(client.getAreaBasedList(
                anyInt(), anyInt(), isNull(), any(), any(), any(), nullable(String.class)
        )).thenReturn(new TourApiPage<>(1, 0, 1000, List.of()));
        when(persistence.deactivateOutsideSelectionPolicy()).thenReturn(11_448);

        TourSyncResult result = new TourSyncService(client, persistence, runRepository)
                .synchronize(new TourSyncCommand(null, 1000, 3, false));

        assertThat(result.deactivatedCount()).isEqualTo(11_448);
        assertThat(result.apiRequestCount()).isEqualTo(9);
        verify(persistence).deactivateOutsideSelectionPolicy();
    }

    private static TouristSummary summary() {
        return new TouristSummary(
                "126508", "12", "경복궁", "서울", "", "1", "23",
                "126.9769", "37.5788", "original", "thumb", "20260101000000",
                "02-0000-0000", "Type1",
                "HS", "HS01", "HS010100",
                new ObjectMapper().createObjectNode().put("contentid", "126508")
        );
    }
}
